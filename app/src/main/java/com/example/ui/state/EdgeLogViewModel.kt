package com.example.ui.state

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.friendlyFirestoreMessage
import com.example.data.model.AssetClass
import com.example.data.model.DailyJournalRecord
import com.example.data.model.DateRangeFilter
import com.example.data.model.JournalMood
import com.example.data.model.MarketRegime
import com.example.data.model.PlaybookRecord
import com.example.data.model.TradeOutcome
import com.example.data.model.TradeRecord
import com.example.data.model.TradeSide
import com.example.data.model.TradeSortOption
import com.example.data.model.TradingAccountRecord
import com.example.data.preferences.AppPreferences
import com.example.data.repository.TradingRepository
import com.example.domain.DailySummary
import com.example.domain.DateUtils
import com.example.domain.PerformanceSummary
import com.example.domain.TradeAnalyticsEngine
import com.example.domain.YearMonthKey
import com.example.ui.theme.AppThemeMode
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JournalFilterState(
    val dateRange: DateRangeFilter = DateRangeFilter.ALL,
    val assetClass: AssetClass? = null,
    val side: TradeSide? = null,
    val outcome: TradeOutcome? = null,
    val searchQuery: String = "",
    val sortOption: TradeSortOption = TradeSortOption.NEWEST,
    val calendarMonth: YearMonthKey = YearMonthKey.current(),
    val selectedDateIso: String = DateUtils.todayIso()
)

data class EdgeLogUiState(
    val isLoading: Boolean = true,
    val syncErrorMessage: String? = null,
    val userDisplayName: String? = null,
    val userEmail: String? = null,
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val accounts: List<TradingAccountRecord> = emptyList(),
    val selectedAccountId: String = AppPreferences.ALL_ACCOUNTS_ID,
    val playbooks: List<PlaybookRecord> = emptyList(),
    val allTradesUnfiltered: List<TradeRecord> = emptyList(),
    val allAccountTrades: List<TradeRecord> = emptyList(),
    val scopedTrades: List<TradeRecord> = emptyList(),
    val filteredTradesList: List<TradeRecord> = emptyList(),
    val allJournals: List<DailyJournalRecord> = emptyList(),
    val journals: Map<String, DailyJournalRecord> = emptyMap(),
    val summary: PerformanceSummary = PerformanceSummary(),
    val calendarDailyMap: Map<String, DailySummary> = emptyMap(),
    val filters: JournalFilterState = JournalFilterState(),
    val hasSampleData: Boolean = false
) {
    val activeAccount: TradingAccountRecord?
        get() = accounts.firstOrNull { it.id == selectedAccountId }

    val activeAccountLabel: String
        get() = activeAccount?.name ?: if (accounts.size <= 1) {
            accounts.firstOrNull()?.name ?: "Primary Account"
        } else {
            "All Accounts"
        }

    val defaultWriteAccountId: String
        get() = activeAccount?.id ?: accounts.firstOrNull()?.id.orEmpty()

    val recentSymbols: List<String>
        get() = allTradesUnfiltered
            .sortedByDescending { it.entryTimeEpochMs }
            .map { it.symbol.trim().uppercase() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(8)

    fun tradesForDate(dateIso: String): List<TradeRecord> =
        allAccountTrades.filter { it.effectiveDateIso == dateIso }
            .sortedByDescending { it.entryTimeEpochMs }
}

private sealed interface CoreStreamState {
    data object Loading : CoreStreamState
    data class Success(
        val accounts: List<TradingAccountRecord>,
        val playbooks: List<PlaybookRecord>,
        val trades: List<TradeRecord>,
        val journals: List<DailyJournalRecord>
    ) : CoreStreamState

    data class Error(val message: String) : CoreStreamState
}

@OptIn(ExperimentalCoroutinesApi::class)
class EdgeLogViewModel(
    private val repository: TradingRepository,
    private val currentUserId: String,
    private val userDisplayName: String?,
    private val userEmail: String?
) : ViewModel() {

    private val filterState = MutableStateFlow(JournalFilterState())
    private val retryTrigger = MutableStateFlow(0)
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultAccount()
                .onFailure { err ->
                    postMessage(friendlyFirestoreMessage(err, "Unable to initialize default trading account."))
                }
        }
    }

    private val coreStreamFlow = retryTrigger.flatMapLatest {
        combine(
            repository.observeAccounts(currentUserId),
            repository.observePlaybooks(currentUserId),
            repository.observeTrades(currentUserId),
            repository.observeJournals(currentUserId)
        ) { accounts, playbooks, trades, journals ->
            CoreStreamState.Success(
                accounts = accounts,
                playbooks = playbooks,
                trades = trades,
                journals = journals
            ) as CoreStreamState
        }.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            emit(CoreStreamState.Error(friendlyFirestoreMessage(rootError, "Unable to sync cloud trading journal.")))
        }
    }

    val uiState: StateFlow<EdgeLogUiState> = combine(
        coreStreamFlow,
        repository.themeMode,
        repository.selectedAccountId,
        filterState
    ) { coreState, themeMode, selectedAccId, filters ->
        when (coreState) {
            is CoreStreamState.Loading -> EdgeLogUiState(
                isLoading = true,
                userDisplayName = userDisplayName,
                userEmail = userEmail,
                themeMode = themeMode,
                filters = filters
            )

            is CoreStreamState.Error -> EdgeLogUiState(
                isLoading = false,
                syncErrorMessage = coreState.message,
                userDisplayName = userDisplayName,
                userEmail = userEmail,
                themeMode = themeMode,
                filters = filters
            )

            is CoreStreamState.Success -> {
                val effectiveAccountId = if (
                    selectedAccId != AppPreferences.ALL_ACCOUNTS_ID &&
                    coreState.accounts.none { it.id == selectedAccId }
                ) {
                    AppPreferences.ALL_ACCOUNTS_ID
                } else {
                    selectedAccId
                }

                val accountTrades = if (effectiveAccountId == AppPreferences.ALL_ACCOUNTS_ID) {
                    coreState.trades
                } else {
                    coreState.trades.filter { it.accountId == effectiveAccountId }
                }

                val startingEquity = if (effectiveAccountId == AppPreferences.ALL_ACCOUNTS_ID) {
                    coreState.accounts.sumOf { it.startingBalance }
                } else {
                    coreState.accounts.firstOrNull { it.id == effectiveAccountId }?.startingBalance ?: 25_000.0
                }

                val cutoffMs: Long? = when (filters.dateRange) {
                    DateRangeFilter.ALL -> null
                    DateRangeFilter.YTD -> DateUtils.startOfYearEpoch()
                    else -> filters.dateRange.days?.let { DateUtils.daysAgoEpoch(it) }
                }

                val scopedTrades = accountTrades.filter { trade ->
                    val inRange = cutoffMs == null || trade.effectiveDateEpochMs >= cutoffMs
                    val matchesAsset = filters.assetClass == null || trade.assetClassEnum == filters.assetClass
                    val matchesSide = filters.side == null || trade.sideEnum == filters.side
                    inRange && matchesAsset && matchesSide
                }

                val query = filters.searchQuery.trim().lowercase()
                val playbookNameById = coreState.playbooks.associate { it.id to it.name.lowercase() }
                val filteredList = scopedTrades
                    .filter { trade ->
                        val matchesOutcome = filters.outcome == null || trade.outcome == filters.outcome
                        val matchesQuery = query.isEmpty() ||
                            trade.symbol.lowercase().contains(query) ||
                            trade.notes.lowercase().contains(query) ||
                            trade.mistakeTags.any { it.lowercase().contains(query) } ||
                            trade.setupTags.any { it.lowercase().contains(query) } ||
                            (trade.playbookId?.let { playbookNameById[it]?.contains(query) } == true)
                        matchesOutcome && matchesQuery
                    }
                    .let { list ->
                        when (filters.sortOption) {
                            TradeSortOption.NEWEST -> list.sortedByDescending { it.entryTimeEpochMs }
                            TradeSortOption.OLDEST -> list.sortedBy { it.entryTimeEpochMs }
                            TradeSortOption.HIGHEST_PNL -> list.sortedByDescending { it.netPnl }
                            TradeSortOption.LOWEST_PNL -> list.sortedBy { it.netPnl }
                            TradeSortOption.BEST_R -> list.sortedByDescending { it.rMultiple ?: Double.NEGATIVE_INFINITY }
                        }
                    }

                val summary = TradeAnalyticsEngine.analyze(
                    trades = scopedTrades,
                    startingEquity = startingEquity,
                    playbooks = coreState.playbooks
                )

                val calendarFullSummary = TradeAnalyticsEngine.analyze(
                    trades = accountTrades,
                    startingEquity = startingEquity,
                    playbooks = emptyList()
                )

                val activeJournalAccountId = if (effectiveAccountId != AppPreferences.ALL_ACCOUNTS_ID) {
                    effectiveAccountId
                } else {
                    coreState.accounts.firstOrNull()?.id.orEmpty()
                }

                val journalsByDate = coreState.journals
                    .filter { it.accountId == activeJournalAccountId }
                    .associateBy { it.dateIso }

                val hasDemo = coreState.trades.any { it.demoRecord } ||
                    coreState.playbooks.any { it.demoRecord } ||
                    coreState.journals.any { it.demoRecord }

                EdgeLogUiState(
                    isLoading = false,
                    syncErrorMessage = null,
                    userDisplayName = userDisplayName,
                    userEmail = userEmail,
                    themeMode = themeMode,
                    accounts = coreState.accounts,
                    selectedAccountId = effectiveAccountId,
                    playbooks = coreState.playbooks,
                    allTradesUnfiltered = coreState.trades,
                    allAccountTrades = accountTrades,
                    scopedTrades = scopedTrades,
                    filteredTradesList = filteredList,
                    allJournals = coreState.journals,
                    journals = journalsByDate,
                    summary = summary,
                    calendarDailyMap = calendarFullSummary.dailyPnlMap,
                    filters = filters,
                    hasSampleData = hasDemo
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = EdgeLogUiState(
            userDisplayName = userDisplayName,
            userEmail = userEmail
        )
    )

    fun retryCloudSync() {
        retryTrigger.update { it + 1 }
        viewModelScope.launch { repository.ensureDefaultAccount() }
    }

    fun dismissSnackbar() {
        _snackbarMessage.value = null
    }

    private fun postMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    // Filters
    fun selectAccount(accountId: String) {
        viewModelScope.launch { repository.setSelectedAccountId(accountId) }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setDateRange(range: DateRangeFilter) {
        filterState.update { it.copy(dateRange = range) }
    }

    fun setAssetClassFilter(assetClass: AssetClass?) {
        filterState.update { it.copy(assetClass = assetClass) }
    }

    fun setSideFilter(side: TradeSide?) {
        filterState.update { it.copy(side = side) }
    }

    fun setOutcomeFilter(outcome: TradeOutcome?) {
        filterState.update { it.copy(outcome = outcome) }
    }

    fun setSearchQuery(query: String) {
        filterState.update { it.copy(searchQuery = query) }
    }

    fun setSortOption(sort: TradeSortOption) {
        filterState.update { it.copy(sortOption = sort) }
    }

    fun clearTradeFilters() {
        filterState.update {
            it.copy(
                dateRange = DateRangeFilter.ALL,
                assetClass = null,
                side = null,
                outcome = null,
                searchQuery = ""
            )
        }
    }

    fun prevCalendarMonth() {
        filterState.update { it.copy(calendarMonth = it.calendarMonth.prev()) }
    }

    fun nextCalendarMonth() {
        filterState.update { it.copy(calendarMonth = it.calendarMonth.next()) }
    }

    fun selectCalendarDate(dateIso: String) {
        filterState.update { it.copy(selectedDateIso = dateIso) }
    }

    // Accounts CRUD
    fun saveAccount(
        id: String = "",
        name: String,
        broker: String,
        startingBalance: Double
    ) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) return
        viewModelScope.launch {
            repository.saveAccount(
                TradingAccountRecord(
                    id = id,
                    name = cleanName,
                    broker = broker.trim().ifEmpty { "Manual" },
                    startingBalance = startingBalance.coerceAtLeast(0.0)
                )
            ).onSuccess {
                postMessage(if (id.isBlank()) "Account \"$cleanName\" synced to cloud" else "Account updated")
            }.onFailure { err ->
                postMessage(friendlyFirestoreMessage(err, "Could not save trading account."))
            }
        }
    }

    fun deleteAccount(accountId: String) {
        viewModelScope.launch {
            if (uiState.value.accounts.size <= 1) {
                postMessage("At least one trading account must remain.")
                return@launch
            }
            repository.deleteAccount(accountId)
                .onSuccess { postMessage("Trading account removed") }
                .onFailure { err -> postMessage(friendlyFirestoreMessage(err, "Failed to delete account.")) }
        }
    }

    // Playbooks CRUD
    fun savePlaybook(playbook: PlaybookRecord) {
        viewModelScope.launch {
            repository.savePlaybook(playbook)
                .onSuccess {
                    postMessage(if (playbook.id.isBlank()) "Playbook \"${playbook.name}\" synced" else "Playbook updated")
                }
                .onFailure { err ->
                    postMessage(friendlyFirestoreMessage(err, "Could not save playbook."))
                }
        }
    }

    fun deletePlaybook(id: String) {
        viewModelScope.launch {
            repository.deletePlaybook(id)
                .onSuccess { postMessage("Playbook deleted") }
                .onFailure { err -> postMessage(friendlyFirestoreMessage(err, "Could not delete playbook.")) }
        }
    }

    // Trades CRUD
    fun saveTrade(trade: TradeRecord, onComplete: (String) -> Unit = {}) {
        viewModelScope.launch {
            repository.saveTrade(trade)
                .onSuccess { savedId ->
                    postMessage(if (trade.id.isBlank()) "Logged ${trade.symbol} trade to cloud" else "Updated ${trade.symbol} trade")
                    onComplete(savedId)
                }
                .onFailure { err ->
                    postMessage(friendlyFirestoreMessage(err, "Could not save trade."))
                }
        }
    }

    fun deleteTrade(tradeId: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteTrade(tradeId)
                .onSuccess {
                    postMessage("Trade deleted")
                    onDeleted()
                }
                .onFailure { err ->
                    postMessage(friendlyFirestoreMessage(err, "Could not delete trade."))
                }
        }
    }

    fun copyAndReturnChartImage(uri: Uri, onCopied: (String) -> Unit) {
        viewModelScope.launch {
            repository.copyChartImageToPrivateStorage(uri)
                .onSuccess { path -> onCopied(path) }
                .onFailure { postMessage("Could not attach screenshot: ${it.localizedMessage}") }
        }
    }

    fun attachChartToExistingTrade(tradeId: String, uri: Uri) {
        viewModelScope.launch {
            repository.copyChartImageToPrivateStorage(uri)
                .onSuccess { path ->
                    repository.updateTradeChartPath(tradeId, path)
                        .onSuccess { postMessage("Chart screenshot attached") }
                        .onFailure { err -> postMessage(friendlyFirestoreMessage(err, "Failed to save chart attachment.")) }
                }
                .onFailure { postMessage("Failed to attach chart image") }
        }
    }

    fun removeChartFromTrade(tradeId: String) {
        viewModelScope.launch {
            repository.updateTradeChartPath(tradeId, null)
                .onSuccess { postMessage("Chart screenshot removed") }
                .onFailure { err -> postMessage(friendlyFirestoreMessage(err, "Could not remove chart screenshot.")) }
        }
    }

    // Daily Journal
    fun saveDailyJournal(
        dateIso: String,
        preMarketPlan: String,
        postMarketReview: String,
        lessonsLearned: String,
        mood: JournalMood,
        marketRegime: MarketRegime,
        followedPlan: Boolean
    ) {
        val accountId = uiState.value.defaultWriteAccountId
        viewModelScope.launch {
            repository.saveDailyJournal(
                DailyJournalRecord(
                    accountId = accountId,
                    dateIso = dateIso,
                    preMarketPlan = preMarketPlan.trim(),
                    postMarketReview = postMarketReview.trim(),
                    lessonsLearned = lessonsLearned.trim(),
                    mood = mood.name,
                    marketRegime = marketRegime.name,
                    followedPlan = followedPlan,
                    demoRecord = false
                )
            ).onSuccess {
                postMessage("Daily journal synced for ${DateUtils.formatIsoDisplay(dateIso)}")
            }.onFailure { err ->
                postMessage(friendlyFirestoreMessage(err, "Could not save daily journal."))
            }
        }
    }

    // CSV Import / Export
    fun importCsvFromUri(uri: Uri) {
        val targetAccId = uiState.value.defaultWriteAccountId
        viewModelScope.launch {
            repository.importCsvFromUri(uri, targetAccId)
                .onSuccess { result ->
                    if (result.trades.isNotEmpty()) {
                        val skippedText = if (result.skippedRows > 0) " (${result.skippedRows} skipped)" else ""
                        postMessage("Imported ${result.trades.size} trades to cloud$skippedText")
                    } else {
                        postMessage(result.errors.firstOrNull() ?: "No valid trade rows found in CSV.")
                    }
                }
                .onFailure { err ->
                    postMessage(friendlyFirestoreMessage(err, "CSV import failed."))
                }
        }
    }

    fun importCsvFromText(rawCsv: String, onImported: () -> Unit = {}) {
        val targetAccId = uiState.value.defaultWriteAccountId
        viewModelScope.launch {
            repository.importCsvText(rawCsv, targetAccId)
                .onSuccess { result ->
                    if (result.trades.isNotEmpty()) {
                        val skippedText = if (result.skippedRows > 0) " (${result.skippedRows} skipped)" else ""
                        postMessage("Imported ${result.trades.size} trades$skippedText")
                        onImported()
                    } else {
                        postMessage(result.errors.firstOrNull() ?: "No valid trades parsed.")
                    }
                }
                .onFailure { err ->
                    postMessage(friendlyFirestoreMessage(err, "CSV import failed."))
                }
        }
    }

    fun exportCsvToUri(uri: Uri) {
        val tradesToExport = uiState.value.allAccountTrades
        viewModelScope.launch {
            repository.exportCsvToUri(uri, tradesToExport)
                .onSuccess { count -> postMessage("Exported $count trades to CSV") }
                .onFailure { postMessage("Export failed: ${it.localizedMessage}") }
        }
    }

    // Full JSON Backup & Restore (Google Drive / Files via SAF)
    fun exportJsonBackupToUri(uri: Uri) {
        val state = uiState.value
        viewModelScope.launch {
            repository.exportJsonBackupToUri(
                uri = uri,
                accounts = state.accounts,
                playbooks = state.playbooks,
                trades = state.allTradesUnfiltered,
                journals = state.allJournals
            ).onSuccess { summary ->
                postMessage(
                    "Backup saved (${summary.tradesCount} trades, ${summary.playbooksCount} playbooks, ${summary.journalsCount} reviews)"
                )
            }.onFailure { err ->
                postMessage("Backup failed: ${err.localizedMessage ?: "Unable to write file"}")
            }
        }
    }

    fun importJsonBackupFromUri(uri: Uri) {
        viewModelScope.launch {
            repository.importJsonBackupFromUri(uri)
                .onSuccess { summary ->
                    postMessage(
                        "Restored ${summary.tradesCount} trades, ${summary.playbooksCount} playbooks, and ${summary.journalsCount} journals"
                    )
                }
                .onFailure { err ->
                    postMessage(friendlyFirestoreMessage(err, err.localizedMessage ?: "Could not restore backup file."))
                }
        }
    }

    // Opt-in Demo Dataset
    fun loadDemoDataset() {
        val targetAccId = uiState.value.defaultWriteAccountId
        viewModelScope.launch {
            repository.loadSampleDataset(targetAccId)
                .onSuccess {
                    filterState.update {
                        it.copy(
                            calendarMonth = YearMonthKey.current(),
                            selectedDateIso = DateUtils.todayIso()
                        )
                    }
                    postMessage("Loaded sample multi-asset journal (tagged as Demo)")
                }
                .onFailure { err ->
                    postMessage(friendlyFirestoreMessage(err, "Could not load demo history."))
                }
        }
    }

    fun clearDemoDataset() {
        viewModelScope.launch {
            repository.clearSampleDataset()
                .onSuccess { postMessage("Removed all demo sample records") }
                .onFailure { err -> postMessage(friendlyFirestoreMessage(err, "Could not remove demo records.")) }
        }
    }

    class Factory(
        private val repository: TradingRepository,
        private val currentUserId: String,
        private val userDisplayName: String?,
        private val userEmail: String?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EdgeLogViewModel(
                repository = repository,
                currentUserId = currentUserId,
                userDisplayName = userDisplayName,
                userEmail = userEmail
            ) as T
        }
    }
}
