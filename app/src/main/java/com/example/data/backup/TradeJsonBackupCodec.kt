package com.example.data.backup

import com.example.data.model.AssetClass
import com.example.data.model.DailyJournalRecord
import com.example.data.model.EmotionTag
import com.example.data.model.JournalMood
import com.example.data.model.MarketRegime
import com.example.data.model.PlaybookRecord
import com.example.data.model.TradeRecord
import com.example.data.model.TradeSide
import com.example.data.model.TradeStatus
import com.example.data.model.TradingAccountRecord
import org.json.JSONArray
import org.json.JSONObject

data class JournalBackupSnapshot(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAtEpochMs: Long = System.currentTimeMillis(),
    val accounts: List<TradingAccountRecord> = emptyList(),
    val playbooks: List<PlaybookRecord> = emptyList(),
    val trades: List<TradeRecord> = emptyList(),
    val journals: List<DailyJournalRecord> = emptyList()
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

/**
 * Portable full-journal JSON backup & restore codec for saving/loading complete snapshots
 * directly to Google Drive or device files via Android's zero-permission Storage Access Framework.
 */
object TradeJsonBackupCodec {

    fun encode(snapshot: JournalBackupSnapshot): String {
        val root = JSONObject()
        root.put("app", "EdgeLog")
        root.put("schemaVersion", snapshot.schemaVersion)
        root.put("exportedAtEpochMs", snapshot.exportedAtEpochMs)

        val accountsArr = JSONArray()
        snapshot.accounts.forEach { acc ->
            accountsArr.put(JSONObject().apply {
                put("id", acc.id)
                put("name", acc.name)
                put("broker", acc.broker)
                put("startingBalance", acc.startingBalance)
                put("currency", acc.currency)
            })
        }
        root.put("accounts", accountsArr)

        val playbooksArr = JSONArray()
        snapshot.playbooks.forEach { pb ->
            playbooksArr.put(JSONObject().apply {
                put("id", pb.id)
                put("name", pb.name)
                put("description", pb.description)
                put("marketCondition", pb.marketCondition)
                put("rules", JSONArray(pb.rules))
                put("targetRiskReward", pb.targetRiskReward)
                put("colorHex", pb.colorHex)
                put("demoRecord", pb.demoRecord)
            })
        }
        root.put("playbooks", playbooksArr)

        val tradesArr = JSONArray()
        snapshot.trades.forEach { t ->
            tradesArr.put(JSONObject().apply {
                put("id", t.id)
                put("accountId", t.accountId)
                put("symbol", t.symbol)
                put("assetClass", t.assetClass)
                put("side", t.side)
                put("status", t.status)
                put("entryPrice", t.entryPrice)
                if (t.exitPrice != null) put("exitPrice", t.exitPrice)
                put("quantity", t.quantity)
                put("contractMultiplier", t.contractMultiplier)
                put("fees", t.fees)
                if (t.stopLossPrice != null) put("stopLossPrice", t.stopLossPrice)
                if (t.takeProfitPrice != null) put("takeProfitPrice", t.takeProfitPrice)
                put("entryTimeEpochMs", t.entryTimeEpochMs)
                if (t.exitTimeEpochMs != null) put("exitTimeEpochMs", t.exitTimeEpochMs)
                if (t.playbookId != null) put("playbookId", t.playbookId)
                put("playbookAdherencePct", t.playbookAdherencePct)
                put("checkedRules", JSONArray(t.checkedRules))
                put("mistakeTags", JSONArray(t.mistakeTags))
                put("setupTags", JSONArray(t.setupTags))
                put("emotionBefore", t.emotionBefore)
                put("rating", t.rating)
                put("notes", t.notes)
                put("demoRecord", t.demoRecord)
            })
        }
        root.put("trades", tradesArr)

        val journalsArr = JSONArray()
        snapshot.journals.forEach { j ->
            journalsArr.put(JSONObject().apply {
                put("id", j.id)
                put("accountId", j.accountId)
                put("dateIso", j.dateIso)
                put("preMarketPlan", j.preMarketPlan)
                put("postMarketReview", j.postMarketReview)
                put("lessonsLearned", j.lessonsLearned)
                put("mood", j.mood)
                put("marketRegime", j.marketRegime)
                put("followedPlan", j.followedPlan)
                put("demoRecord", j.demoRecord)
            })
        }
        root.put("journals", journalsArr)

        return root.toString(2)
    }

    fun decode(rawJson: String): Result<JournalBackupSnapshot> = runCatching {
        val root = JSONObject(rawJson)
        val version = root.optInt("schemaVersion", 1)
        val exportedAt = root.optLong("exportedAtEpochMs", System.currentTimeMillis())

        val accounts = mutableListOf<TradingAccountRecord>()
        val accountsArr = root.optJSONArray("accounts") ?: JSONArray()
        for (i in 0 until accountsArr.length()) {
            val obj = accountsArr.optJSONObject(i) ?: continue
            val name = obj.optString("name", "").trim().take(60)
            if (name.isEmpty()) continue
            accounts.add(
                TradingAccountRecord(
                    id = obj.optString("id", "").trim(),
                    name = name,
                    broker = obj.optString("broker", "Manual").trim().take(60),
                    startingBalance = obj.optDouble("startingBalance", 25_000.0)
                        .takeIf { it.isFinite() && it >= 0.0 }?.coerceAtMost(1_000_000_000.0) ?: 25_000.0,
                    currency = obj.optString("currency", "USD").trim().ifEmpty { "USD" }.take(8)
                )
            )
        }

        val playbooks = mutableListOf<PlaybookRecord>()
        val playbooksArr = root.optJSONArray("playbooks") ?: JSONArray()
        for (i in 0 until playbooksArr.length()) {
            val obj = playbooksArr.optJSONObject(i) ?: continue
            val name = obj.optString("name", "").trim().take(80)
            if (name.isEmpty()) continue
            playbooks.add(
                PlaybookRecord(
                    id = obj.optString("id", "").trim(),
                    name = name,
                    description = obj.optString("description", "").trim().take(500),
                    marketCondition = obj.optString("marketCondition", "Any").trim().take(60),
                    rules = readStringList(obj.optJSONArray("rules"), 20),
                    targetRiskReward = obj.optDouble("targetRiskReward", 2.0)
                        .takeIf { it.isFinite() }?.coerceIn(0.1, 100.0) ?: 2.0,
                    colorHex = obj.optLong("colorHex", 0xFF00D084L),
                    demoRecord = obj.optBoolean("demoRecord", false)
                )
            )
        }

        val trades = mutableListOf<TradeRecord>()
        val tradesArr = root.optJSONArray("trades") ?: JSONArray()
        for (i in 0 until tradesArr.length()) {
            val obj = tradesArr.optJSONObject(i) ?: continue
            val symbol = obj.optString("symbol", "").trim().uppercase().take(24)
            val entryPrice = obj.optDouble("entryPrice", -1.0)
            val quantity = obj.optDouble("quantity", -1.0)
            if (symbol.isEmpty() || !entryPrice.isFinite() || entryPrice <= 0.0 || !quantity.isFinite() || quantity <= 0.0) {
                continue
            }
            val assetClass = AssetClass.fromRaw(obj.optString("assetClass", AssetClass.STOCK.name))
            val side = TradeSide.fromRaw(obj.optString("side", TradeSide.LONG.name))
            val exitPrice = obj.optNullableDouble("exitPrice")?.takeIf { it > 0.0 }
            val rawStatus = TradeStatus.fromRaw(obj.optString("status", TradeStatus.CLOSED.name))
            val status = if (rawStatus == TradeStatus.CLOSED && exitPrice != null) TradeStatus.CLOSED else TradeStatus.OPEN
            val entryTime = obj.optLong("entryTimeEpochMs", System.currentTimeMillis()).coerceAtLeast(1L)
            val exitTime = if (status == TradeStatus.CLOSED) {
                obj.optNullableLong("exitTimeEpochMs")?.takeIf { it > 0L } ?: (entryTime + 60_000L)
            } else null

            trades.add(
                TradeRecord(
                    id = obj.optString("id", "").trim(),
                    accountId = obj.optString("accountId", "").trim(),
                    symbol = symbol,
                    assetClass = assetClass.name,
                    side = side.name,
                    status = status.name,
                    entryPrice = entryPrice,
                    exitPrice = if (status == TradeStatus.CLOSED) exitPrice else null,
                    quantity = quantity,
                    contractMultiplier = obj.optDouble("contractMultiplier", assetClass.defaultMultiplier)
                        .takeIf { it.isFinite() && it > 0.0 } ?: assetClass.defaultMultiplier,
                    fees = obj.optDouble("fees", 0.0).takeIf { it.isFinite() && it >= 0.0 } ?: 0.0,
                    stopLossPrice = obj.optNullableDouble("stopLossPrice")?.takeIf { it > 0.0 },
                    takeProfitPrice = obj.optNullableDouble("takeProfitPrice")?.takeIf { it > 0.0 },
                    entryTimeEpochMs = entryTime,
                    exitTimeEpochMs = exitTime,
                    playbookId = obj.optNullableString("playbookId")?.takeIf { it.isNotBlank() },
                    playbookAdherencePct = obj.optInt("playbookAdherencePct", 100).coerceIn(0, 100),
                    checkedRules = readStringList(obj.optJSONArray("checkedRules"), 20),
                    mistakeTags = readStringList(obj.optJSONArray("mistakeTags"), 15),
                    setupTags = readStringList(obj.optJSONArray("setupTags"), 15),
                    emotionBefore = EmotionTag.fromRaw(obj.optString("emotionBefore", EmotionTag.CALM.name)).name,
                    rating = obj.optInt("rating", 3).coerceIn(1, 5),
                    notes = obj.optString("notes", "").take(3000),
                    chartImagePath = null, // Local file paths are device-specific
                    demoRecord = obj.optBoolean("demoRecord", false)
                )
            )
        }

        val journals = mutableListOf<DailyJournalRecord>()
        val journalsArr = root.optJSONArray("journals") ?: JSONArray()
        val isoRegex = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")
        for (i in 0 until journalsArr.length()) {
            val obj = journalsArr.optJSONObject(i) ?: continue
            val dateIso = obj.optString("dateIso", "").trim()
            if (!isoRegex.matches(dateIso)) continue
            journals.add(
                DailyJournalRecord(
                    id = obj.optString("id", "").trim(),
                    accountId = obj.optString("accountId", "").trim(),
                    dateIso = dateIso,
                    preMarketPlan = obj.optString("preMarketPlan", "").take(2000),
                    postMarketReview = obj.optString("postMarketReview", "").take(2000),
                    lessonsLearned = obj.optString("lessonsLearned", "").take(2000),
                    mood = JournalMood.fromRaw(obj.optString("mood", JournalMood.FOCUSED.name)).name,
                    marketRegime = MarketRegime.fromRaw(obj.optString("marketRegime", MarketRegime.TRENDING_UP.name)).name,
                    followedPlan = obj.optBoolean("followedPlan", true),
                    demoRecord = obj.optBoolean("demoRecord", false)
                )
            )
        }

        if (accounts.isEmpty() && playbooks.isEmpty() && trades.isEmpty() && journals.isEmpty()) {
            error("Backup file contains no valid EdgeLog accounts, playbooks, trades, or journals.")
        }

        JournalBackupSnapshot(
            schemaVersion = version,
            exportedAtEpochMs = exportedAt,
            accounts = accounts,
            playbooks = playbooks,
            trades = trades,
            journals = journals
        )
    }

    private fun readStringList(arr: JSONArray?, maxLen: Int): List<String> {
        if (arr == null) return emptyList()
        val out = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val s = arr.optString(i, "").trim()
            if (s.isNotEmpty()) out.add(s)
            if (out.size >= maxLen) break
        }
        return out
    }

    private fun JSONObject.optNullableDouble(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        val v = optDouble(key, Double.NaN)
        return if (v.isFinite()) v else null
    }

    private fun JSONObject.optNullableLong(key: String): Long? {
        if (!has(key) || isNull(key)) return null
        val v = optLong(key, Long.MIN_VALUE)
        return if (v != Long.MIN_VALUE) v else null
    }

    private fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key, "").trim().takeIf { it.isNotEmpty() }
    }
}
