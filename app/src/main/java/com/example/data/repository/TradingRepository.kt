package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.R
import com.example.data.backup.JournalBackupSnapshot
import com.example.data.backup.TradeJsonBackupCodec
import com.example.data.csv.CsvImportResult
import com.example.data.csv.TradeCsvCodec
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.DailyJournalRecord
import com.example.data.model.PlaybookRecord
import com.example.data.model.TradeRecord
import com.example.data.model.TradingAccountRecord
import com.example.data.preferences.AppPreferences
import com.example.data.sample.SampleDatasetGenerator
import com.example.ui.theme.AppThemeMode
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.WriteBatch
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class JsonRestoreSummary(
    val accountsCount: Int,
    val playbooksCount: Int,
    val tradesCount: Int,
    val journalsCount: Int
)

class TradingRepository(
    private val context: Context,
    private val db: FirebaseFirestore,
    private val preferences: AppPreferences,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context, preferences: AppPreferences) : this(
        context = context.applicationContext,
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        preferences = preferences
    )

    val themeMode: Flow<AppThemeMode> = preferences.themeMode
    val selectedAccountId: Flow<String> = preferences.selectedAccountId

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun accountsCollection(uid: String) =
        db.collection("users").document(uid).collection("accounts")

    private fun playbooksCollection(uid: String) =
        db.collection("users").document(uid).collection("playbooks")

    private fun tradesCollection(uid: String) =
        db.collection("users").document(uid).collection("trades")

    private fun journalsCollection(uid: String) =
        db.collection("users").document(uid).collection("journals")

    fun observeAccounts(userId: String): Flow<List<TradingAccountRecord>> = flow {
        val path = "users/$userId/accounts"
        emitAll(
            accountsCollection(userId)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        TradingAccountRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    logStreamErrorIfActive(userId, path, error)
                    throw error
                }
        )
    }

    fun observePlaybooks(userId: String): Flow<List<PlaybookRecord>> = flow {
        val path = "users/$userId/playbooks"
        emitAll(
            playbooksCollection(userId)
                .orderBy("name", Query.Direction.ASCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        PlaybookRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    logStreamErrorIfActive(userId, path, error)
                    throw error
                }
        )
    }

    fun observeTrades(userId: String): Flow<List<TradeRecord>> = flow {
        val path = "users/$userId/trades"
        emitAll(
            tradesCollection(userId)
                .orderBy("entryTimeEpochMs", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        TradeRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    logStreamErrorIfActive(userId, path, error)
                    throw error
                }
        )
    }

    fun observeJournals(userId: String): Flow<List<DailyJournalRecord>> = flow {
        val path = "users/$userId/journals"
        emitAll(
            journalsCollection(userId)
                .orderBy("dateIso", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        DailyJournalRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    logStreamErrorIfActive(userId, path, error)
                    throw error
                }
        )
    }

    private fun logStreamErrorIfActive(userId: String, path: String, error: Throwable) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
        }
    }

    suspend fun ensureDefaultAccount(): Result<String> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/accounts"
        return try {
            val uid = requireUserId()
            val existing = accountsCollection(uid).limit(1).get().await()
            if (!existing.isEmpty) {
                return Result.success(existing.documents.first().id)
            }
            val defaultRef = accountsCollection(uid).document(DEFAULT_ACCOUNT_ID)
            path = defaultRef.path
            val payload = accountCreatePayload(
                uid = uid,
                account = TradingAccountRecord(
                    name = "Primary Account",
                    broker = "Manual",
                    startingBalance = 25_000.0,
                    currency = "USD"
                )
            )
            defaultRef.set(payload).await()
            Result.success(defaultRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun setThemeMode(mode: AppThemeMode) = preferences.setThemeMode(mode)

    suspend fun setSelectedAccountId(accountId: String) = preferences.setSelectedAccountId(accountId)

    suspend fun saveAccount(account: TradingAccountRecord): Result<String> {
        val isCreate = account.id.isBlank()
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/accounts/${account.id}"
        return try {
            val uid = requireUserId()
            val col = accountsCollection(uid)
            val docRef = if (isCreate) col.document() else col.document(account.id)
            path = docRef.path
            if (isCreate) {
                docRef.set(accountCreatePayload(uid, account)).await()
            } else {
                docRef.update(accountUpdatePayload(account)).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(accountId: String): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/accounts/$accountId"
        return try {
            val uid = requireUserId()
            require(accountId.isNotBlank()) { "Account ID cannot be blank" }
            val accRef = accountsCollection(uid).document(accountId)
            path = accRef.path

            val linkedTrades = tradesCollection(uid).whereEqualTo("accountId", accountId).get().await()
            val linkedJournals = journalsCollection(uid).whereEqualTo("accountId", accountId).get().await()

            val ops = mutableListOf<(WriteBatch) -> Unit>()
            linkedTrades.documents.forEach { doc -> ops.add { it.delete(doc.reference) } }
            linkedJournals.documents.forEach { doc -> ops.add { it.delete(doc.reference) } }
            ops.add { it.delete(accRef) }
            commitInBatches(ops)

            preferences.setSelectedAccountId(AppPreferences.ALL_ACCOUNTS_ID)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun savePlaybook(playbook: PlaybookRecord): Result<String> {
        val isCreate = playbook.id.isBlank()
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/playbooks/${playbook.id}"
        return try {
            val uid = requireUserId()
            val col = playbooksCollection(uid)
            val docRef = if (isCreate) col.document() else col.document(playbook.id)
            path = docRef.path
            if (isCreate) {
                docRef.set(playbookCreatePayload(uid, playbook)).await()
            } else {
                docRef.update(playbookUpdatePayload(playbook)).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePlaybook(playbookId: String): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/playbooks/$playbookId"
        return try {
            val uid = requireUserId()
            require(playbookId.isNotBlank()) { "Playbook ID cannot be blank" }
            val pbRef = playbooksCollection(uid).document(playbookId)
            path = pbRef.path

            val linkedTrades = tradesCollection(uid).whereEqualTo("playbookId", playbookId).get().await()
            val ops = mutableListOf<(WriteBatch) -> Unit>()
            linkedTrades.documents.forEach { doc ->
                ops.add { batch ->
                    batch.update(
                        doc.reference,
                        mapOf(
                            "playbookId" to FieldValue.delete(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            ops.add { it.delete(pbRef) }
            commitInBatches(ops)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveTrade(trade: TradeRecord): Result<String> {
        val isCreate = trade.id.isBlank()
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/trades/${trade.id}"
        return try {
            val uid = requireUserId()
            val targetAccountId = trade.accountId.ifBlank { ensureDefaultAccount().getOrThrow() }
            val normalizedTrade = trade.copy(accountId = targetAccountId)
            val col = tradesCollection(uid)
            val docRef = if (isCreate) col.document() else col.document(normalizedTrade.id)
            path = docRef.path
            if (isCreate) {
                docRef.set(tradeCreatePayload(uid, normalizedTrade)).await()
            } else {
                docRef.update(tradeUpdatePayload(normalizedTrade)).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isCreate) OperationType.CREATE else OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateTradeChartPath(tradeId: String, chartImagePath: String?): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/trades/$tradeId"
        return try {
            val uid = requireUserId()
            require(tradeId.isNotBlank()) { "Trade ID cannot be blank" }
            val docRef = tradesCollection(uid).document(tradeId)
            path = docRef.path
            val payload: Map<String, Any> = mapOf(
                "chartImagePath" to (chartImagePath?.take(500) ?: FieldValue.delete()),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteTrade(tradeId: String): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/trades/$tradeId"
        return try {
            val uid = requireUserId()
            require(tradeId.isNotBlank()) { "Trade ID cannot be blank" }
            val docRef = tradesCollection(uid).document(tradeId)
            path = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun saveDailyJournal(journal: DailyJournalRecord): Result<String> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/journals"
        return try {
            val uid = requireUserId()
            val targetAccountId = journal.accountId.ifBlank { ensureDefaultAccount().getOrThrow() }
            val normalized = journal.copy(accountId = targetAccountId)
            val docId = "${normalized.accountId}_${normalized.dateIso}"
            val docRef = journalsCollection(uid).document(docId)
            path = docRef.path

            val exists = docRef.get().await().exists()
            if (exists) {
                docRef.update(journalUpdatePayload(normalized)).await()
            } else {
                docRef.set(journalCreatePayload(uid, normalized)).await()
            }
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun importCsvText(rawCsv: String, accountId: String): Result<CsvImportResult> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/trades"
        return try {
            val uid = requireUserId()
            val targetAccountId = accountId.ifBlank { ensureDefaultAccount().getOrThrow() }
            val result = TradeCsvCodec.parseCsv(rawCsv, targetAccountId)
            if (result.trades.isNotEmpty()) {
                val col = tradesCollection(uid)
                path = col.path
                val ops = result.trades.map { trade ->
                    val docRef = col.document()
                    val payload = tradeCreatePayload(uid, trade)
                    val op: (WriteBatch) -> Unit = { batch -> batch.set(docRef, payload) }
                    op
                }
                commitInBatches(ops)
            }
            Result.success(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun importCsvFromUri(uri: Uri, accountId: String): Result<CsvImportResult> =
        withContext(Dispatchers.IO) {
            val raw = try {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: return@withContext Result.success(
                        CsvImportResult(emptyList(), 0, listOf("Unable to open selected CSV file."))
                    )
            } catch (e: Exception) {
                return@withContext Result.failure(e)
            }
            importCsvText(raw, accountId)
        }

    suspend fun exportCsvToUri(uri: Uri, trades: List<TradeRecord>): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                val csv = TradeCsvCodec.exportToCsv(trades)
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                    writer.write(csv)
                } ?: error("Unable to open destination file for CSV export.")
                trades.size
            }
        }

    suspend fun exportJsonBackupToUri(
        uri: Uri,
        accounts: List<TradingAccountRecord>,
        playbooks: List<PlaybookRecord>,
        trades: List<TradeRecord>,
        journals: List<DailyJournalRecord>
    ): Result<JsonRestoreSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val snapshot = JournalBackupSnapshot(
                accounts = accounts,
                playbooks = playbooks,
                trades = trades,
                journals = journals
            )
            val json = TradeJsonBackupCodec.encode(snapshot)
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                writer.write(json)
            } ?: error("Unable to open destination file for JSON backup.")
            JsonRestoreSummary(
                accountsCount = accounts.size,
                playbooksCount = playbooks.size,
                tradesCount = trades.size,
                journalsCount = journals.size
            )
        }
    }

    suspend fun importJsonBackupFromUri(uri: Uri): Result<JsonRestoreSummary> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val rawJson = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("Unable to open selected backup file.")
            }
            val snapshot = TradeJsonBackupCodec.decode(rawJson).getOrThrow()

            val accCol = accountsCollection(uid)
            val pbCol = playbooksCollection(uid)
            val trCol = tradesCollection(uid)
            val jnCol = journalsCollection(uid)

            val existingAccIds = accCol.get().await().documents.map { it.id }.toSet()
            val existingPbIds = pbCol.get().await().documents.map { it.id }.toSet()
            val existingTrIds = trCol.get().await().documents.map { it.id }.toSet()
            val existingJnIds = jnCol.get().await().documents.map { it.id }.toSet()

            val knownAccIds = existingAccIds.toMutableSet()
            val knownPbIds = existingPbIds.toMutableSet()
            val knownTrIds = existingTrIds.toMutableSet()
            val knownJnIds = existingJnIds.toMutableSet()

            // 1. Accounts first so existsAfter rules always pass for trades & journals
            val accountIdMap = mutableMapOf<String, String>()
            val accountOps = mutableListOf<(WriteBatch) -> Unit>()
            val seenAccTargetIds = mutableSetOf<String>()

            snapshot.accounts.forEach { acc ->
                val targetId = if (isValidFirestoreId(acc.id)) acc.id else accCol.document().id
                if (acc.id.isNotBlank()) accountIdMap[acc.id] = targetId
                if (!seenAccTargetIds.add(targetId)) return@forEach
                val docRef = accCol.document(targetId)
                if (knownAccIds.contains(targetId)) {
                    val payload = accountUpdatePayload(acc)
                    accountOps.add { it.update(docRef, payload) }
                } else {
                    val payload = accountCreatePayload(uid, acc)
                    accountOps.add { it.set(docRef, payload) }
                    knownAccIds.add(targetId)
                }
            }
            path = accCol.path
            commitInBatches(accountOps)

            val defaultRestoredAccId = accountIdMap.values.firstOrNull()
                ?: knownAccIds.firstOrNull()
                ?: ensureDefaultAccount().getOrThrow().also { knownAccIds.add(it) }

            // 2. Playbooks
            val playbookIdMap = mutableMapOf<String, String>()
            val playbookOps = mutableListOf<(WriteBatch) -> Unit>()
            val seenPbTargetIds = mutableSetOf<String>()
            snapshot.playbooks.forEach { pb ->
                val targetId = if (isValidFirestoreId(pb.id)) pb.id else pbCol.document().id
                if (pb.id.isNotBlank()) playbookIdMap[pb.id] = targetId
                if (!seenPbTargetIds.add(targetId)) return@forEach
                val docRef = pbCol.document(targetId)
                if (knownPbIds.contains(targetId)) {
                    val payload = playbookUpdatePayload(pb)
                    playbookOps.add { it.update(docRef, payload) }
                } else {
                    val payload = playbookCreatePayload(uid, pb)
                    playbookOps.add { it.set(docRef, payload) }
                    knownPbIds.add(targetId)
                }
            }
            path = pbCol.path
            commitInBatches(playbookOps)

            // 3. Trades & Daily Journals
            val tradeAndJournalOps = mutableListOf<(WriteBatch) -> Unit>()
            val seenTrTargetIds = mutableSetOf<String>()
            snapshot.trades.forEach { trade ->
                val mappedAccId = accountIdMap[trade.accountId]
                    ?: trade.accountId.takeIf { knownAccIds.contains(it) }
                    ?: defaultRestoredAccId
                val mappedPbId = trade.playbookId?.let { pid ->
                    playbookIdMap[pid] ?: pid.takeIf { knownPbIds.contains(it) }
                }
                val normalized = trade.copy(accountId = mappedAccId, playbookId = mappedPbId)
                val targetId = if (isValidFirestoreId(trade.id)) trade.id else trCol.document().id
                if (!seenTrTargetIds.add(targetId)) return@forEach
                val docRef = trCol.document(targetId)
                if (knownTrIds.contains(targetId)) {
                    val payload = tradeUpdatePayload(normalized)
                    tradeAndJournalOps.add { it.update(docRef, payload) }
                } else {
                    val payload = tradeCreatePayload(uid, normalized)
                    tradeAndJournalOps.add { it.set(docRef, payload) }
                    knownTrIds.add(targetId)
                }
            }

            val seenJnDocIds = mutableSetOf<String>()
            snapshot.journals.forEach { journal ->
                val mappedAccId = accountIdMap[journal.accountId]
                    ?: journal.accountId.takeIf { knownAccIds.contains(it) }
                    ?: defaultRestoredAccId
                val normalized = journal.copy(accountId = mappedAccId)
                val docId = "${mappedAccId}_${normalized.dateIso}"
                if (!seenJnDocIds.add(docId)) return@forEach
                val docRef = jnCol.document(docId)
                if (knownJnIds.contains(docId)) {
                    val payload = journalUpdatePayload(normalized)
                    tradeAndJournalOps.add { it.update(docRef, payload) }
                } else {
                    val payload = journalCreatePayload(uid, normalized)
                    tradeAndJournalOps.add { it.set(docRef, payload) }
                    knownJnIds.add(docId)
                }
            }

            path = trCol.path
            commitInBatches(tradeAndJournalOps)

            Result.success(
                JsonRestoreSummary(
                    accountsCount = snapshot.accounts.size,
                    playbooksCount = snapshot.playbooks.size,
                    tradesCount = snapshot.trades.size,
                    journalsCount = snapshot.journals.size
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun copyChartImageToPrivateStorage(sourceUri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.filesDir, "trade_charts").apply { mkdirs() }
                val destFile = File(dir, "chart_${UUID.randomUUID()}.jpg")
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: error("Could not read selected image.")
                destFile.absolutePath
            }
        }

    suspend fun loadSampleDataset(targetAccountId: String): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/trades"
        return try {
            val uid = requireUserId()
            // Clear any prior demo records first so loading demo data stays idempotent
            clearSampleDataset().getOrThrow()

            val accId = targetAccountId.ifBlank { ensureDefaultAccount().getOrThrow() }
            val pbCol = playbooksCollection(uid)
            val trCol = tradesCollection(uid)
            val jnCol = journalsCollection(uid)

            val samplePlaybooks = SampleDatasetGenerator.buildPlaybooks()
            val pbRefs = samplePlaybooks.map { pbCol.document() }
            val orbId = pbRefs[0].id
            val vwapId = pbRefs[1].id
            val sweepId = pbRefs[2].id

            val (sampleTrades, sampleJournals) = SampleDatasetGenerator.buildTradesAndJournals(
                accountId = accId,
                orbId = orbId,
                vwapId = vwapId,
                sweepId = sweepId
            )

            val existingJournalIds = jnCol.get().await().documents.map { it.id }.toSet()

            val ops = mutableListOf<(WriteBatch) -> Unit>()
            samplePlaybooks.forEachIndexed { idx, pb ->
                val payload = playbookCreatePayload(uid, pb)
                val ref = pbRefs[idx]
                ops.add { it.set(ref, payload) }
            }
            sampleTrades.forEach { trade ->
                val ref = trCol.document()
                val payload = tradeCreatePayload(uid, trade)
                ops.add { it.set(ref, payload) }
            }
            sampleJournals.forEach { journal ->
                val docId = "${accId}_${journal.dateIso}"
                val ref = jnCol.document(docId)
                if (existingJournalIds.contains(docId)) {
                    val payload = journalUpdatePayload(journal)
                    ops.add { it.update(ref, payload) }
                } else {
                    val payload = journalCreatePayload(uid, journal)
                    ops.add { it.set(ref, payload) }
                }
            }

            path = trCol.path
            commitInBatches(ops)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun clearSampleDataset(): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/trades"
        return try {
            val uid = requireUserId()
            val demoTrades = tradesCollection(uid).whereEqualTo("demoRecord", true).get().await()
            val demoPlaybooks = playbooksCollection(uid).whereEqualTo("demoRecord", true).get().await()
            val demoJournals = journalsCollection(uid).whereEqualTo("demoRecord", true).get().await()

            val ops = mutableListOf<(WriteBatch) -> Unit>()
            demoTrades.documents.forEach { doc -> ops.add { it.delete(doc.reference) } }
            demoPlaybooks.documents.forEach { doc -> ops.add { it.delete(doc.reference) } }
            demoJournals.documents.forEach { doc -> ops.add { it.delete(doc.reference) } }
            commitInBatches(ops)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private suspend fun commitInBatches(ops: List<(WriteBatch) -> Unit>) {
        if (ops.isEmpty()) return
        ops.chunked(MAX_BATCH_OPS).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { op -> op(batch) }
            batch.commit().await()
        }
    }

    private fun accountCreatePayload(uid: String, account: TradingAccountRecord): Map<String, Any> = mapOf(
        "userId" to uid,
        "name" to account.name.trim().ifEmpty { "Primary Account" }.take(60),
        "broker" to account.broker.trim().take(60),
        "startingBalance" to account.startingBalance.coerceIn(0.0, 1_000_000_000.0),
        "currency" to account.currency.trim().ifEmpty { "USD" }.take(8),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun accountUpdatePayload(account: TradingAccountRecord): Map<String, Any> = mapOf(
        "name" to account.name.trim().ifEmpty { "Primary Account" }.take(60),
        "broker" to account.broker.trim().take(60),
        "startingBalance" to account.startingBalance.coerceIn(0.0, 1_000_000_000.0),
        "currency" to account.currency.trim().ifEmpty { "USD" }.take(8),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun playbookCreatePayload(uid: String, playbook: PlaybookRecord): Map<String, Any> = mapOf(
        "userId" to uid,
        "name" to playbook.name.trim().ifEmpty { "Untitled Playbook" }.take(80),
        "description" to playbook.description.trim().take(500),
        "marketCondition" to playbook.marketCondition.trim().take(60),
        "rules" to playbook.rules.map { it.trim() }.filter { it.isNotEmpty() }.take(20),
        "targetRiskReward" to playbook.targetRiskReward.coerceIn(0.1, 100.0),
        "colorHex" to playbook.colorHex,
        "demoRecord" to playbook.demoRecord,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun playbookUpdatePayload(playbook: PlaybookRecord): Map<String, Any> = mapOf(
        "name" to playbook.name.trim().ifEmpty { "Untitled Playbook" }.take(80),
        "description" to playbook.description.trim().take(500),
        "marketCondition" to playbook.marketCondition.trim().take(60),
        "rules" to playbook.rules.map { it.trim() }.filter { it.isNotEmpty() }.take(20),
        "targetRiskReward" to playbook.targetRiskReward.coerceIn(0.1, 100.0),
        "colorHex" to playbook.colorHex,
        "demoRecord" to playbook.demoRecord,
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun tradeCreatePayload(uid: String, trade: TradeRecord): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to uid,
            "accountId" to trade.accountId,
            "symbol" to trade.symbol.trim().uppercase().ifEmpty { "TICKER" }.take(24),
            "assetClass" to trade.assetClassEnum.name,
            "side" to trade.sideEnum.name,
            "status" to trade.statusEnum.name,
            "entryPrice" to trade.entryPrice.coerceAtLeast(0.000001),
            "exitPrice" to trade.exitPrice?.takeIf { it > 0.0 },
            "quantity" to trade.quantity.coerceAtLeast(0.000001),
            "contractMultiplier" to trade.contractMultiplier.coerceAtLeast(0.000001),
            "fees" to trade.fees.coerceAtLeast(0.0),
            "stopLossPrice" to trade.stopLossPrice?.takeIf { it > 0.0 },
            "takeProfitPrice" to trade.takeProfitPrice?.takeIf { it > 0.0 },
            "entryTimeEpochMs" to trade.entryTimeEpochMs.coerceAtLeast(1L),
            "exitTimeEpochMs" to trade.exitTimeEpochMs?.takeIf { it > 0L },
            "playbookId" to trade.playbookId?.takeIf { isValidFirestoreId(it) },
            "playbookAdherencePct" to trade.playbookAdherencePct.coerceIn(0, 100),
            "checkedRules" to trade.checkedRules.map { it.trim() }.filter { it.isNotEmpty() }.take(20),
            "mistakeTags" to trade.mistakeTags.map { it.trim() }.filter { it.isNotEmpty() }.take(15),
            "setupTags" to trade.setupTags.map { it.trim() }.filter { it.isNotEmpty() }.take(15),
            "emotionBefore" to trade.emotionEnum.name,
            "rating" to trade.rating.coerceIn(1, 5),
            "notes" to trade.notes.trim().take(3000),
            "chartImagePath" to trade.chartImagePath?.trim()?.takeIf { it.isNotEmpty() }?.take(500),
            "demoRecord" to trade.demoRecord,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return raw.filterValues { it != null }.mapValues { it.value!! }
    }

    private fun tradeUpdatePayload(trade: TradeRecord): Map<String, Any> = mapOf(
        "accountId" to trade.accountId,
        "symbol" to trade.symbol.trim().uppercase().ifEmpty { "TICKER" }.take(24),
        "assetClass" to trade.assetClassEnum.name,
        "side" to trade.sideEnum.name,
        "status" to trade.statusEnum.name,
        "entryPrice" to trade.entryPrice.coerceAtLeast(0.000001),
        "exitPrice" to (trade.exitPrice?.takeIf { it > 0.0 } ?: FieldValue.delete()),
        "quantity" to trade.quantity.coerceAtLeast(0.000001),
        "contractMultiplier" to trade.contractMultiplier.coerceAtLeast(0.000001),
        "fees" to trade.fees.coerceAtLeast(0.0),
        "stopLossPrice" to (trade.stopLossPrice?.takeIf { it > 0.0 } ?: FieldValue.delete()),
        "takeProfitPrice" to (trade.takeProfitPrice?.takeIf { it > 0.0 } ?: FieldValue.delete()),
        "entryTimeEpochMs" to trade.entryTimeEpochMs.coerceAtLeast(1L),
        "exitTimeEpochMs" to (trade.exitTimeEpochMs?.takeIf { it > 0L } ?: FieldValue.delete()),
        "playbookId" to (trade.playbookId?.takeIf { isValidFirestoreId(it) } ?: FieldValue.delete()),
        "playbookAdherencePct" to trade.playbookAdherencePct.coerceIn(0, 100),
        "checkedRules" to trade.checkedRules.map { it.trim() }.filter { it.isNotEmpty() }.take(20),
        "mistakeTags" to trade.mistakeTags.map { it.trim() }.filter { it.isNotEmpty() }.take(15),
        "setupTags" to trade.setupTags.map { it.trim() }.filter { it.isNotEmpty() }.take(15),
        "emotionBefore" to trade.emotionEnum.name,
        "rating" to trade.rating.coerceIn(1, 5),
        "notes" to trade.notes.trim().take(3000),
        "chartImagePath" to (trade.chartImagePath?.trim()?.takeIf { it.isNotEmpty() }?.take(500) ?: FieldValue.delete()),
        "demoRecord" to trade.demoRecord,
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun journalCreatePayload(uid: String, journal: DailyJournalRecord): Map<String, Any> = mapOf(
        "userId" to uid,
        "accountId" to journal.accountId,
        "dateIso" to journal.dateIso,
        "preMarketPlan" to journal.preMarketPlan.trim().take(2000),
        "postMarketReview" to journal.postMarketReview.trim().take(2000),
        "lessonsLearned" to journal.lessonsLearned.trim().take(2000),
        "mood" to journal.moodEnum.name,
        "marketRegime" to journal.marketRegimeEnum.name,
        "followedPlan" to journal.followedPlan,
        "demoRecord" to journal.demoRecord,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun journalUpdatePayload(journal: DailyJournalRecord): Map<String, Any> = mapOf(
        "preMarketPlan" to journal.preMarketPlan.trim().take(2000),
        "postMarketReview" to journal.postMarketReview.trim().take(2000),
        "lessonsLearned" to journal.lessonsLearned.trim().take(2000),
        "mood" to journal.moodEnum.name,
        "marketRegime" to journal.marketRegimeEnum.name,
        "followedPlan" to journal.followedPlan,
        "demoRecord" to journal.demoRecord,
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun isValidFirestoreId(id: String): Boolean =
        id.length in 1..128 && VALID_ID_REGEX.matches(id)

    companion object {
        private const val DEFAULT_ACCOUNT_ID = "primary_default"
        private const val MAX_BATCH_OPS = 400
        private val VALID_ID_REGEX = Regex("^[a-zA-Z0-9_\\-]+$")
    }
}
