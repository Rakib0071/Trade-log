package com.example.data.model

import androidx.annotation.Keep
import com.example.domain.DateUtils
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlin.math.abs

@Keep
@IgnoreExtraProperties
data class TradingAccountRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val broker: String = "Manual",
    val startingBalance: Double = 25_000.0,
    val currency: String = "USD",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PlaybookRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val marketCondition: String = "Any",
    val rules: List<String> = emptyList(),
    val targetRiskReward: Double = 2.0,
    val colorHex: Long = 0xFF00D084L,
    val demoRecord: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TradeRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val accountId: String = "",
    val symbol: String = "",
    val assetClass: String = AssetClass.STOCK.name,
    val side: String = TradeSide.LONG.name,
    val status: String = TradeStatus.CLOSED.name,
    val entryPrice: Double = 0.0,
    val exitPrice: Double? = null,
    val quantity: Double = 0.0,
    val contractMultiplier: Double = 1.0,
    val fees: Double = 0.0,
    val stopLossPrice: Double? = null,
    val takeProfitPrice: Double? = null,
    val entryTimeEpochMs: Long = 0L,
    val exitTimeEpochMs: Long? = null,
    val playbookId: String? = null,
    val playbookAdherencePct: Int = 100,
    val checkedRules: List<String> = emptyList(),
    val mistakeTags: List<String> = emptyList(),
    val setupTags: List<String> = emptyList(),
    val emotionBefore: String = EmotionTag.CALM.name,
    val rating: Int = 3,
    val notes: String = "",
    val chartImagePath: String? = null,
    val demoRecord: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val assetClassEnum: AssetClass get() = AssetClass.fromRaw(assetClass)

    @get:Exclude
    val sideEnum: TradeSide get() = TradeSide.fromRaw(side)

    @get:Exclude
    val statusEnum: TradeStatus get() = TradeStatus.fromRaw(status)

    @get:Exclude
    val emotionEnum: EmotionTag get() = EmotionTag.fromRaw(emotionBefore)

    @get:Exclude
    val isClosed: Boolean
        get() = statusEnum == TradeStatus.CLOSED && exitPrice != null

    @get:Exclude
    val entryNotional: Double
        get() = abs(entryPrice * quantity * contractMultiplier)

    @get:Exclude
    val grossPnl: Double
        get() {
            val exit = exitPrice ?: return 0.0
            if (statusEnum == TradeStatus.OPEN) return 0.0
            val direction = if (sideEnum == TradeSide.LONG) 1.0 else -1.0
            return (exit - entryPrice) * quantity * contractMultiplier * direction
        }

    @get:Exclude
    val netPnl: Double
        get() = if (isClosed) grossPnl - fees else 0.0

    @get:Exclude
    val returnPct: Double
        get() = if (isClosed && entryNotional > 0.0) (netPnl / entryNotional) * 100.0 else 0.0

    @get:Exclude
    val initialRiskDollars: Double?
        get() {
            val sl = stopLossPrice ?: return null
            val riskPerUnit = abs(entryPrice - sl)
            if (riskPerUnit <= 0.0) return null
            return riskPerUnit * quantity * contractMultiplier
        }

    @get:Exclude
    val plannedRewardDollars: Double?
        get() {
            val tp = takeProfitPrice ?: return null
            val rewardPerUnit = abs(tp - entryPrice)
            if (rewardPerUnit <= 0.0) return null
            return rewardPerUnit * quantity * contractMultiplier
        }

    @get:Exclude
    val rMultiple: Double?
        get() {
            if (!isClosed) return null
            val risk = initialRiskDollars ?: return null
            if (risk <= 0.0) return null
            return netPnl / risk
        }

    @get:Exclude
    val holdingDurationMinutes: Long
        get() {
            val exitMs = exitTimeEpochMs ?: return 0L
            return ((exitMs - entryTimeEpochMs) / 60_000L).coerceAtLeast(1L)
        }

    @get:Exclude
    val outcome: TradeOutcome
        get() = when {
            !isClosed -> TradeOutcome.OPEN
            netPnl > 0.01 -> TradeOutcome.WIN
            netPnl < -0.01 -> TradeOutcome.LOSS
            else -> TradeOutcome.BREAKEVEN
        }

    @get:Exclude
    val effectiveDateEpochMs: Long
        get() = exitTimeEpochMs ?: entryTimeEpochMs

    @get:Exclude
    val effectiveDateIso: String
        get() = DateUtils.epochToIsoDate(effectiveDateEpochMs)
}

@Keep
@IgnoreExtraProperties
data class DailyJournalRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val accountId: String = "",
    val dateIso: String = "",
    val preMarketPlan: String = "",
    val postMarketReview: String = "",
    val lessonsLearned: String = "",
    val mood: String = JournalMood.FOCUSED.name,
    val marketRegime: String = MarketRegime.TRENDING_UP.name,
    val followedPlan: Boolean = true,
    val demoRecord: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val moodEnum: JournalMood get() = JournalMood.fromRaw(mood)

    @get:Exclude
    val marketRegimeEnum: MarketRegime get() = MarketRegime.fromRaw(marketRegime)
}
