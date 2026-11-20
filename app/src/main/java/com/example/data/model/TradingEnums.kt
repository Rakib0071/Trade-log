package com.example.data.model

enum class AssetClass(val label: String, val shortLabel: String, val defaultMultiplier: Double) {
    STOCK("Stocks", "STK", 1.0),
    OPTIONS("Options", "OPT", 100.0),
    FUTURES("Futures", "FUT", 50.0),
    FOREX("Forex", "FX", 10_000.0),
    CRYPTO("Crypto", "CRY", 1.0);

    companion object {
        fun fromRaw(raw: String): AssetClass =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) || it.label.equals(raw.trim(), ignoreCase = true) }
                ?: STOCK
    }
}

enum class TradeSide(val label: String) {
    LONG("Long"),
    SHORT("Short");

    companion object {
        fun fromRaw(raw: String): TradeSide {
            val cleaned = raw.trim().uppercase()
            return if (cleaned == "SHORT" || cleaned == "SELL" || cleaned == "S") SHORT else LONG
        }
    }
}

enum class TradeStatus(val label: String) {
    CLOSED("Closed"),
    OPEN("Open");

    companion object {
        fun fromRaw(raw: String): TradeStatus =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: CLOSED
    }
}

enum class TradeOutcome(val label: String) {
    WIN("Win"),
    LOSS("Loss"),
    BREAKEVEN("Breakeven"),
    OPEN("Open")
}

enum class EmotionTag(val label: String) {
    CALM("Calm"),
    CONFIDENT("Confident"),
    DISCIPLINED("Disciplined"),
    ANXIOUS("Anxious"),
    FOMO("FOMO"),
    FRUSTRATED("Frustrated"),
    TIRED("Tired");

    companion object {
        fun fromRaw(raw: String): EmotionTag =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) || it.label.equals(raw.trim(), ignoreCase = true) }
                ?: CALM
    }
}

enum class JournalMood(val label: String) {
    DISCIPLINED("Disciplined"),
    FOCUSED("Focused"),
    NEUTRAL("Neutral"),
    STRESSED("Stressed"),
    TILTED("Tilted");

    companion object {
        fun fromRaw(raw: String): JournalMood =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: FOCUSED
    }
}

enum class MarketRegime(val label: String) {
    TRENDING_UP("Trend Up"),
    TRENDING_DOWN("Trend Down"),
    RANGE_BOUND("Range / Chop"),
    HIGH_VOL("High Volatility");

    companion object {
        fun fromRaw(raw: String): MarketRegime =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: TRENDING_UP
    }
}

enum class DateRangeFilter(val label: String, val days: Int?) {
    D7("7D", 7),
    D30("30D", 30),
    D90("90D", 90),
    YTD("YTD", null),
    ALL("All", null)
}

enum class TradeSortOption(val label: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    HIGHEST_PNL("Best P&L"),
    LOWEST_PNL("Worst P&L"),
    BEST_R("Best R")
}

object StandardMistakeTags {
    val all = listOf(
        "FOMO",
        "Early Exit",
        "Revenge Trade",
        "Oversized",
        "Moved Stop",
        "Chased Entry",
        "No Plan",
        "Overtrading",
        "Hesitated"
    )
}

object StandardSetupTags {
    val all = listOf(
        "A+ Setup",
        "Gap & Go",
        "VWAP Reclaim",
        "Opening Range",
        "Trend Pullback",
        "Liquidity Sweep",
        "Breakout",
        "Mean Reversion"
    )
}
