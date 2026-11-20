package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.domain.DateUtils
import com.example.ui.screens.AccountsAndToolsSheet
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.CalendarJournalScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PlaybooksScreen
import com.example.ui.screens.TradeDetailScreen
import com.example.ui.screens.TradeEditorScreen
import com.example.ui.screens.TradesListScreen
import com.example.ui.state.EdgeLogViewModel

object EdgeLogRoutes {
    const val ROUTE_DASHBOARD = "dashboard"
    const val ROUTE_CALENDAR = "calendar_journal"
    const val ROUTE_TRADES = "trades_list"
    const val ROUTE_ANALYTICS = "analytics"
    const val ROUTE_PLAYBOOKS = "playbooks"
    const val ROUTE_TRADE_DETAIL = "trade_detail/{tradeId}"
    const val ROUTE_TRADE_EDITOR = "trade_editor?tradeId={tradeId}&dateIso={dateIso}"

    fun tradeDetail(tradeId: String) = "trade_detail/$tradeId"
    fun tradeEditor(tradeId: String = "", dateIso: String = "") =
        "trade_editor?tradeId=$tradeId&dateIso=$dateIso"
}

private data class TopLevelDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val topLevelDestinations = listOf(
    TopLevelDestination(
        route = EdgeLogRoutes.ROUTE_DASHBOARD,
        labelRes = R.string.nav_dashboard,
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard
    ),
    TopLevelDestination(
        route = EdgeLogRoutes.ROUTE_CALENDAR,
        labelRes = R.string.nav_calendar,
        selectedIcon = Icons.Filled.CalendarMonth,
        unselectedIcon = Icons.Outlined.CalendarMonth
    ),
    TopLevelDestination(
        route = EdgeLogRoutes.ROUTE_TRADES,
        labelRes = R.string.nav_trades,
        selectedIcon = Icons.Filled.ReceiptLong,
        unselectedIcon = Icons.Outlined.ReceiptLong
    ),
    TopLevelDestination(
        route = EdgeLogRoutes.ROUTE_ANALYTICS,
        labelRes = R.string.nav_analytics,
        selectedIcon = Icons.Filled.Analytics,
        unselectedIcon = Icons.Outlined.Analytics
    ),
    TopLevelDestination(
        route = EdgeLogRoutes.ROUTE_PLAYBOOKS,
        labelRes = R.string.nav_playbooks,
        selectedIcon = Icons.Filled.MenuBook,
        unselectedIcon = Icons.Outlined.MenuBook
    )
)

@Composable
fun EdgeLogApp(
    viewModel: EdgeLogViewModel,
    onSignOut: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    var showAccountsSheet by remember { mutableStateOf(false) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissSnackbar()
        }
    }

    // Zero-permission SAF CSV Import & Export launchers
    val csvOpenLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importCsvFromUri(uri)
    }

    val csvCreateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) viewModel.exportCsvToUri(uri)
    }

    // Zero-permission SAF Full-Journal JSON Backup & Restore launchers (supports Google Drive directly!)
    val jsonBackupCreateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) viewModel.exportJsonBackupToUri(uri)
    }

    val jsonBackupOpenLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importJsonBackupFromUri(uri)
    }

    val isTopLevel = topLevelDestinations.any { it.route == currentRoute }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 680.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (isTopLevel && !useNavRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        topLevelDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(dest.labelRes),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag("nav_${dest.route}")
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (isTopLevel && !useNavRail && currentRoute != EdgeLogRoutes.ROUTE_PLAYBOOKS) {
                    FloatingActionButton(
                        onClick = { navController.navigate(EdgeLogRoutes.tradeEditor()) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("log_trade_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.cd_log_trade_fab)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isTopLevel && useNavRail) {
                    NavigationRail(
                        modifier = Modifier
                            .fillMaxHeight()
                            .statusBarsPadding()
                            .navigationBarsPadding(),
                        containerColor = MaterialTheme.colorScheme.surface,
                        header = {
                            FloatingActionButton(
                                onClick = { navController.navigate(EdgeLogRoutes.tradeEditor()) },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.testTag("rail_log_trade_fab")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = stringResource(R.string.cd_log_trade_fab)
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                    ) {
                        topLevelDestinations.forEach { dest ->
                            val selected = currentRoute == dest.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes), maxLines = 1) },
                                modifier = Modifier.testTag("rail_${dest.route}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    NavHost(
                        navController = navController,
                        startDestination = EdgeLogRoutes.ROUTE_DASHBOARD
                    ) {
                        composable(EdgeLogRoutes.ROUTE_DASHBOARD) {
                            DashboardScreen(
                                uiState = uiState,
                                onSelectDateRange = viewModel::setDateRange,
                                onOpenAccountsAndTools = { showAccountsSheet = true },
                                onLogTrade = { navController.navigate(EdgeLogRoutes.tradeEditor()) },
                                onImportCsv = {
                                    csvOpenLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*"))
                                },
                                onLoadDemo = viewModel::loadDemoDataset,
                                onRetrySync = viewModel::retryCloudSync,
                                onOpenTradeDetail = { tradeId ->
                                    navController.navigate(EdgeLogRoutes.tradeDetail(tradeId))
                                },
                                onViewAllTrades = { navigateTopLevel(EdgeLogRoutes.ROUTE_TRADES) }
                            )
                        }

                        composable(EdgeLogRoutes.ROUTE_CALENDAR) {
                            CalendarJournalScreen(
                                uiState = uiState,
                                onPrevMonth = viewModel::prevCalendarMonth,
                                onNextMonth = viewModel::nextCalendarMonth,
                                onSelectDate = viewModel::selectCalendarDate,
                                onSaveJournal = viewModel::saveDailyJournal,
                                onOpenTradeDetail = { tradeId ->
                                    navController.navigate(EdgeLogRoutes.tradeDetail(tradeId))
                                },
                                onLogTradeForDate = { dateIso ->
                                    navController.navigate(EdgeLogRoutes.tradeEditor(dateIso = dateIso))
                                }
                            )
                        }

                        composable(EdgeLogRoutes.ROUTE_TRADES) {
                            TradesListScreen(
                                uiState = uiState,
                                onSearchChange = viewModel::setSearchQuery,
                                onSelectOutcome = viewModel::setOutcomeFilter,
                                onSelectSide = viewModel::setSideFilter,
                                onSelectAssetClass = viewModel::setAssetClassFilter,
                                onSelectSort = viewModel::setSortOption,
                                onClearFilters = viewModel::clearTradeFilters,
                                onOpenTradeDetail = { tradeId ->
                                    navController.navigate(EdgeLogRoutes.tradeDetail(tradeId))
                                },
                                onLogTrade = { navController.navigate(EdgeLogRoutes.tradeEditor()) }
                            )
                        }

                        composable(EdgeLogRoutes.ROUTE_ANALYTICS) {
                            AnalyticsScreen(
                                uiState = uiState,
                                onSelectDateRange = viewModel::setDateRange
                            )
                        }

                        composable(EdgeLogRoutes.ROUTE_PLAYBOOKS) {
                            PlaybooksScreen(
                                uiState = uiState,
                                onSavePlaybook = viewModel::savePlaybook,
                                onDeletePlaybook = viewModel::deletePlaybook
                            )
                        }

                        composable(
                            route = EdgeLogRoutes.ROUTE_TRADE_DETAIL,
                            arguments = listOf(navArgument("tradeId") { type = NavType.StringType })
                        ) { entry ->
                            val tradeId = entry.arguments?.getString("tradeId").orEmpty()
                            val trade = uiState.allAccountTrades.firstOrNull { it.id == tradeId }
                            val playbook = trade?.playbookId?.let { pid ->
                                uiState.playbooks.firstOrNull { it.id == pid }
                            }
                            val accName = trade?.let { t ->
                                uiState.accounts.firstOrNull { it.id == t.accountId }?.name
                            } ?: uiState.activeAccountLabel

                            TradeDetailScreen(
                                trade = trade,
                                playbook = playbook,
                                accountName = accName,
                                onBack = { navController.popBackStack() },
                                onEdit = {
                                    if (trade != null) {
                                        navController.navigate(EdgeLogRoutes.tradeEditor(tradeId = trade.id))
                                    }
                                },
                                onDelete = {
                                    viewModel.deleteTrade(tradeId) {
                                        navController.popBackStack()
                                    }
                                },
                                onAttachChartUri = { uri ->
                                    viewModel.attachChartToExistingTrade(tradeId, uri)
                                },
                                onRemoveChart = { viewModel.removeChartFromTrade(tradeId) }
                            )
                        }

                        composable(
                            route = EdgeLogRoutes.ROUTE_TRADE_EDITOR,
                            arguments = listOf(
                                navArgument("tradeId") {
                                    type = NavType.StringType
                                    defaultValue = ""
                                },
                                navArgument("dateIso") {
                                    type = NavType.StringType
                                    defaultValue = ""
                                }
                            )
                        ) { entry ->
                            val tradeId = entry.arguments?.getString("tradeId").orEmpty()
                            val dateIso = entry.arguments?.getString("dateIso").orEmpty()
                            val existing = if (tradeId.isNotBlank()) {
                                uiState.allAccountTrades.firstOrNull { it.id == tradeId }
                            } else null

                            TradeEditorScreen(
                                existingTrade = existing,
                                initialDateIso = dateIso.ifBlank { null },
                                accounts = uiState.accounts,
                                defaultAccountId = uiState.defaultWriteAccountId,
                                playbooks = uiState.playbooks,
                                recentSymbols = uiState.recentSymbols,
                                onCopyChartImage = viewModel::copyAndReturnChartImage,
                                onSave = { savedRecord ->
                                    viewModel.saveTrade(savedRecord) {
                                        navController.popBackStack()
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAccountsSheet) {
        AccountsAndToolsSheet(
            uiState = uiState,
            onDismiss = { showAccountsSheet = false },
            onSelectAccount = viewModel::selectAccount,
            onSaveAccount = viewModel::saveAccount,
            onDeleteAccount = viewModel::deleteAccount,
            onSelectTheme = viewModel::setThemeMode,
            onBackupJsonToDrive = {
                jsonBackupCreateLauncher.launch("edgelog_backup_${DateUtils.todayIso()}.json")
            },
            onRestoreJsonFromDrive = {
                jsonBackupOpenLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            },
            onPickCsvFile = {
                csvOpenLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*"))
            },
            onPasteCsvImport = { raw -> viewModel.importCsvFromText(raw) },
            onExportCsvFile = {
                csvCreateLauncher.launch("edgelog_trades_${DateUtils.todayIso()}.csv")
            },
            onLoadDemoData = viewModel::loadDemoDataset,
            onClearDemoData = viewModel::clearDemoDataset,
            onSignOut = onSignOut
        )
    }
}
