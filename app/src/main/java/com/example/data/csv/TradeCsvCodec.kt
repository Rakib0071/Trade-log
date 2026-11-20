package com.example.data.csv

import com.example.data.model.AssetClass
import com.example.data.model.EmotionTag
import com.example.data.model.TradeRecord
import com.example.data.model.TradeSide
import com.example.data.model.TradeStatus
import com.example.domain.DateUtils

data class CsvImportResult(
    val trades: List<TradeRecord>,
    val skippedRows: Int,
    val errors: List<String>
)

object TradeCsvCodec {

    const val TEMPLATE_HEADER =
        "Symbol,Side,AssetClass,EntryPrice,ExitPrice,Quantity,Multiplier,Fees,StopLoss,TakeProfit,EntryTime,ExitTime,Emotion,Mistakes,Tags,Rating,Notes"

    val SAMPLE_CSV_SNIPPET = """
        $TEMPLATE_HEADER
        NVDA,LONG,STOCK,138.40,143.90,100,1,4.20,136.20,144.00,2026-11-18 09:42,2026-11-18 11:15,CONFIDENT,,A+ Setup,5,"Clean opening range break with strong relative volume"
        ES,SHORT,FUTURES,5962.50,5948.00,2,50,8.40,5971.00,5946.00,2026-11-19 10:10,2026-11-19 11:40,DISCIPLINED,,VWAP Reclaim,4,"Rejected prior day high cleanly"
    """.trimIndent()

    fun exportToCsv(trades: List<TradeRecord>): String {
        val sb = StringBuilder()
        sb.appendLine(TEMPLATE_HEADER)
        for (t in trades.sortedBy { it.entryTimeEpochMs }) {
            val cols = listOf(
                t.symbol,
                t.side,
                t.assetClass,
                t.entryPrice.toString(),
                t.exitPrice?.toString() ?: "",
                t.quantity.toString(),
                t.contractMultiplier.toString(),
                t.fees.toString(),
                t.stopLossPrice?.toString() ?: "",
                t.takeProfitPrice?.toString() ?: "",
                DateUtils.formatIsoDateTime(t.entryTimeEpochMs),
                t.exitTimeEpochMs?.let { DateUtils.formatIsoDateTime(it) } ?: "",
                t.emotionBefore,
                t.mistakeTags.joinToString("; "),
                t.setupTags.joinToString("; "),
                t.rating.toString(),
                t.notes
            )
            sb.appendLine(cols.joinToString(",") { escapeCsvCell(it) })
        }
        return sb.toString()
    }

    fun parseCsv(rawCsv: String, accountId: String): CsvImportResult {
        val lines = rawCsv
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .split("\n")
            .filter { it.isNotBlank() }

        if (lines.size < 2) {
            return CsvImportResult(emptyList(), 0, listOf("CSV must include a header row and at least one trade row."))
        }

        val headers = parseCsvLine(lines.first()).map { normalizeHeader(it) }
        val symbolIdx = findColumn(headers, "symbol", "ticker", "instrument", "asset", "contract")
        val sideIdx = findColumn(headers, "side", "direction", "action", "type")
        val assetIdx = findColumn(headers, "assetclass", "assettype", "category", "market")
        val entryIdx = findColumn(headers, "entryprice", "entry", "openprice", "buyprice", "avgentry")
        val exitIdx = findColumn(headers, "exitprice", "exit", "closeprice", "sellprice", "avgexit")
        val qtyIdx = findColumn(headers, "quantity", "qty", "size", "shares", "contracts", "volume")
        val multIdx = findColumn(headers, "multiplier", "contractmultiplier", "pointvalue")
        val feesIdx = findColumn(headers, "fees", "commission", "commissions", "fee")
        val slIdx = findColumn(headers, "stoploss", "sl", "stop", "initialstop")
        val tpIdx = findColumn(headers, "takeprofit", "tp", "target")
        val entryTimeIdx = findColumn(headers, "entrytime", "opendate", "date", "opentime", "timestamp")
        val exitTimeIdx = findColumn(headers, "exittime", "closedate", "closetime")
        val emotionIdx = findColumn(headers, "emotion", "emotionbefore", "mindset")
        val mistakesIdx = findColumn(headers, "mistakes", "mistaketags", "errors")
        val tagsIdx = findColumn(headers, "tags", "setuptags", "setup", "strategy")
        val ratingIdx = findColumn(headers, "rating", "grade", "score")
        val notesIdx = findColumn(headers, "notes", "comment", "comments", "journal")

        if (symbolIdx < 0 || entryIdx < 0 || qtyIdx < 0) {
            return CsvImportResult(
                emptyList(),
                lines.size - 1,
                listOf("Missing required columns. Include at least Symbol, EntryPrice, and Quantity.")
            )
        }

        val imported = mutableListOf<TradeRecord>()
        val errors = mutableListOf<String>()
        var skipped = 0
        val now = System.currentTimeMillis()

        for (rowIdx in 1 until lines.size) {
            val cols = parseCsvLine(lines[rowIdx])
            val symbol = cols.getOrNull(symbolIdx)?.trim()?.uppercase()?.take(24).orEmpty()
            val entryPrice = parseDoubleClean(cols.getOrNull(entryIdx))
            val qty = parseDoubleClean(cols.getOrNull(qtyIdx))

            if (symbol.isEmpty() || entryPrice == null || entryPrice <= 0.0 || qty == null || qty <= 0.0) {
                skipped++
                errors.add("Row ${rowIdx + 1}: invalid symbol, entry price, or quantity.")
                continue
            }

            val assetClass = assetIdx.takeIf { it >= 0 }
                ?.let { cols.getOrNull(it) }
                ?.takeIf { it.isNotBlank() }
                ?.let { AssetClass.fromRaw(it) }
                ?: AssetClass.STOCK

            val side = sideIdx.takeIf { it >= 0 }
                ?.let { cols.getOrNull(it) }
                ?.takeIf { it.isNotBlank() }
                ?.let { TradeSide.fromRaw(it) }
                ?: TradeSide.LONG

            val exitPrice = exitIdx.takeIf { it >= 0 }?.let { parseDoubleClean(cols.getOrNull(it)) }
            val status = if (exitPrice != null && exitPrice > 0.0) TradeStatus.CLOSED else TradeStatus.OPEN
            val multiplier = multIdx.takeIf { it >= 0 }?.let { parseDoubleClean(cols.getOrNull(it)) }
                ?.takeIf { it > 0.0 }
                ?: assetClass.defaultMultiplier
            val fees = feesIdx.takeIf { it >= 0 }?.let { parseDoubleClean(cols.getOrNull(it)) }?.coerceAtLeast(0.0) ?: 0.0
            val stopLoss = slIdx.takeIf { it >= 0 }?.let { parseDoubleClean(cols.getOrNull(it)) }?.takeIf { it > 0.0 }
            val takeProfit = tpIdx.takeIf { it >= 0 }?.let { parseDoubleClean(cols.getOrNull(it)) }?.takeIf { it > 0.0 }

            val entryTime = entryTimeIdx.takeIf { it >= 0 }
                ?.let { cols.getOrNull(it) }
                ?.let { DateUtils.parseFlexibleDateTime(it) }
                ?: (now - (lines.size - rowIdx) * 3_600_000L)

            val parsedExitTime = exitTimeIdx.takeIf { it >= 0 }
                ?.let { cols.getOrNull(it) }
                ?.let { DateUtils.parseFlexibleDateTime(it) }
            val exitTime = if (status == TradeStatus.CLOSED) {
                (parsedExitTime ?: (entryTime + 45 * 60_000L)).coerceAtLeast(entryTime + 60_000L)
            } else null

            val emotion = emotionIdx.takeIf { it >= 0 }
                ?.let { cols.getOrNull(it) }
                ?.takeIf { it.isNotBlank() }
                ?.let { EmotionTag.fromRaw(it) }
                ?: EmotionTag.CALM

            val mistakes = mistakesIdx.takeIf { it >= 0 }
                ?.let { cols.getOrNull(it) }
                .orEmpty()
                .split(";", "|", ",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .take(15)

            val tags = tagsIdx.takeIf { it >= 0 }
                ?.let { cols.getOrNull(it) }
                .orEmpty()
                .split(";", "|", ",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .take(15)

            val rating = ratingIdx.takeIf { it >= 0 }
                ?.let { cols.getOrNull(it)?.trim()?.toIntOrNull() }
                ?.coerceIn(1, 5)
                ?: 3
            val notes = notesIdx.takeIf { it >= 0 }?.let { cols.getOrNull(it)?.trim()?.take(3000) }.orEmpty()

            imported.add(
                TradeRecord(
                    accountId = accountId,
                    symbol = symbol,
                    assetClass = assetClass.name,
                    side = side.name,
                    status = status.name,
                    entryPrice = entryPrice,
                    exitPrice = if (status == TradeStatus.CLOSED) exitPrice else null,
                    quantity = qty,
                    contractMultiplier = multiplier,
                    fees = fees,
                    stopLossPrice = stopLoss,
                    takeProfitPrice = takeProfit,
                    entryTimeEpochMs = entryTime,
                    exitTimeEpochMs = exitTime,
                    mistakeTags = mistakes,
                    setupTags = tags,
                    emotionBefore = emotion.name,
                    rating = rating,
                    notes = notes,
                    demoRecord = false
                )
            )
        }

        return CsvImportResult(imported, skipped, errors)
    }

    private fun normalizeHeader(raw: String): String =
        raw.trim().lowercase().replace(Regex("[^a-z0-9]"), "")

    private fun findColumn(headers: List<String>, vararg aliases: String): Int {
        for (alias in aliases) {
            val exact = headers.indexOf(alias)
            if (exact >= 0) return exact
        }
        return -1
    }

    private fun parseDoubleClean(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        return raw.trim()
            .replace("$", "")
            .replace(",", "")
            .replace("%", "")
            .toDoubleOrNull()
    }

    private fun escapeCsvCell(value: String): String {
        val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n')
        return if (needsQuotes) {
            "\"${value.replace("\"", "\"\"")}\""
        } else value
    }

    internal fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}
