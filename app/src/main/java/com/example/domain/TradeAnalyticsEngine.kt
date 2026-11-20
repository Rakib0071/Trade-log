package com.example.domain

import com.example.data.model.PlaybookRecord
import com.example.data.model.TradeRecord
import com.example.data.model.AssetClass
import com.example.data.model.EmotionTag
import com.example.data.model.TradeOutcome
import com.example.data.model.TradeSide
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class EdgeScoreBreakdown(
    val overall: Int = 0,
    val grade: String = "—",
    val winRateScore: Float = 0f,       // 0..100
    val profitFactorScore: Float = 0f,  // 0..100
    val payoffRatioScore: Float = 0f,   // 0..100
    val consistencyScore: Float = 0f,   // 0..100
    val disciplineScore: Float = 0f     // 0..100
)

data class EquityPoint(
    val dateIso: String,
    val label: String,
    val dailyPnl: Double,
    val cumulativePnl: Double,
    val equity: Double,
    val drawdownDollars: Double,
    val drawdownPct: Double,
    val tradeCount: Int
)

data class DailySummary(
    val dateIso: String,
    val netPnl: Double,
    val tradeCount: Int,
    val wins: Int,
    val losses: Int,
    val totalR: Double,
    val rCount: Int
)

data class BucketMetric(
    val key: String,
    val label: String,
    val tradeCount: Int,
    val wins: Int,
    val losses: Int,
    val netPnl: Double,
    val avgPnl: Double,
    val winRatePct: Double,
    val profitFactor: Double
)

data class SideSummary(
    val side: TradeSide,
    val tradeCount: Int,
    val wins: Int,
    val losses: Int,
    val netPnl: Double,
    val winRatePct: Double,
    val profitFactor: Double,
    val avgPnl: Double
)

data class MistakeMetric(
    val tag: String,
    val tradeCount: Int,
    val totalPnl: Double,
    val avgPnl: Double,
    val winRatePct: Double
)

data class RDistributionBucket(
    val label: String,
    val count: Int,
    val isPositive: Boolean
)

data class PlaybookStats(
    val playbookId: String,
    val tradeCount: Int,
    val wins: Int,
    val losses: Int,
    val netPnl: Double,
    val winRatePct: Double,
    val profitFactor: Double,
    val expectancy: Double,
    val avgR: Double?,
    val avgAdherencePct: Double
)

data class PerformanceSummary(
    val totalTrades: Int = 0,
    val closedTrades: Int = 0,
    val openTrades: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val breakevens: Int = 0,
    val netPnl: Double = 0.0,
    val grossProfit: Double = 0.0,
    val grossLoss: Double = 0.0,
    val totalFees: Double = 0.0,
    val startingEquity: Double = 0.0,
    val currentEquity: Double = 0.0,
    val returnOnEquityPct: Double = 0.0,
    val tradeWinRatePct: Double = 0.0,
    val profitFactor: Double = 0.0,
    val avgWin: Double = 0.0,
    val avgLoss: Double = 0.0,
    val avgWinLossRatio: Double = 0.0,
    val expectancy: Double = 0.0,
    val avgRMultiple: Double? = null,
    val totalNetR: Double = 0.0,
    val rTrackedCount: Int = 0,
    val tradingDays: Int = 0,
    val winningDays: Int = 0,
    val losingDays: Int = 0,
    val breakevenDays: Int = 0,
    val dayWinRatePct: Double = 0.0,
    val bestDayPnl: Double = 0.0,
    val worstDayPnl: Double = 0.0,
    val largestWin: Double = 0.0,
    val largestLoss: Double = 0.0,
    val maxDrawdownDollars: Double = 0.0,
    val maxDrawdownPct: Double = 0.0,
    val currentStreak: Int = 0,
    val bestWinStreak: Int = 0,
    val worstLossStreak: Int = 0,
    val avgWinHoldMinutes: Long = 0L,
    val avgLossHoldMinutes: Long = 0L,
    val edgeScore: EdgeScoreBreakdown = EdgeScoreBreakdown(),
    val equityCurve: List<EquityPoint> = emptyList(),
    val dailyPnlMap: Map<String, DailySummary> = emptyMap(),
    val dayOfWeekBreakdown: List<BucketMetric> = emptyList(),
    val sessionBreakdown: List<BucketMetric> = emptyList(),
    val symbolBreakdown: List<BucketMetric> = emptyList(),
    val assetClassBreakdown: List<BucketMetric> = emptyList(),
    val longStats: SideSummary = SideSummary(TradeSide.LONG, 0, 0, 0, 0.0, 0.0, 0.0, 0.0),
    val shortStats: SideSummary = SideSummary(TradeSide.SHORT, 0, 0, 0, 0.0, 0.0, 0.0, 0.0),
    val mistakeBreakdown: List<MistakeMetric> = emptyList(),
    val emotionBreakdown: List<BucketMetric> = emptyList(),
    val rDistribution: List<RDistributionBucket> = emptyList(),
    val playbookMetrics: Map<String, PlaybookStats> = emptyMap()
)

object TradeAnalyticsEngine {

    fun analyze(
        trades: List<TradeRecord>,
        startingEquity: Double,
        playbooks: List<PlaybookRecord> = emptyList()
    ): PerformanceSummary {
        val totalTrades = trades.size
        val closed = trades.filter { it.isClosed }.sortedBy { it.effectiveDateEpochMs }
        val openCount = totalTrades - closed.size

        if (closed.isEmpty()) {
            return PerformanceSummary(
                totalTrades = totalTrades,
                closedTrades = 0,
                openTrades = openCount,
                startingEquity = startingEquity,
                currentEquity = startingEquity,
                playbookMetrics = playbooks.associate { pb ->
                    pb.id to PlaybookStats(pb.id, 0, 0, 0, 0.0, 0.0, 0.0, 0.0, null, 0.0)
                }
            )
        }

        val winTrades = closed.filter { it.outcome == TradeOutcome.WIN }
        val lossTrades = closed.filter { it.outcome == TradeOutcome.LOSS }
        val beTrades = closed.filter { it.outcome == TradeOutcome.BREAKEVEN }

        val wins = winTrades.size
        val losses = lossTrades.size
        val breakevens = beTrades.size

        val netPnl = closed.sumOf { it.netPnl }
        val grossProfit = winTrades.sumOf { it.netPnl }
        val grossLossAbs = abs(lossTrades.sumOf { it.netPnl })
        val totalFees = closed.sumOf { it.fees }

        val currentEquity = startingEquity + netPnl
        val roePct = if (startingEquity > 0.0) (netPnl / startingEquity) * 100.0 else 0.0

        val decisiveCount = wins + losses
        val tradeWinRatePct = if (decisiveCount > 0) (wins.toDouble() / decisiveCount) * 100.0 else 0.0
        val profitFactor = computeProfitFactor(grossProfit, grossLossAbs)

        val avgWin = if (wins > 0) grossProfit / wins else 0.0
        val avgLoss = if (losses > 0) grossLossAbs / losses else 0.0
        val avgWinLossRatio = when {
            avgLoss > 0.0 -> avgWin / avgLoss
            avgWin > 0.0 -> Double.POSITIVE_INFINITY
            else -> 0.0
        }

        val winProb = if (decisiveCount > 0) wins.toDouble() / decisiveCount else 0.0
        val lossProb = if (decisiveCount > 0) losses.toDouble() / decisiveCount else 0.0
        val expectancy = (winProb * avgWin) - (lossProb * avgLoss)

        val rValues = closed.mapNotNull { it.rMultiple }
        val totalNetR = rValues.sum()
        val avgRMultiple = if (rValues.isNotEmpty()) totalNetR / rValues.size else null

        val largestWin = winTrades.maxOfOrNull { it.netPnl } ?: 0.0
        val largestLoss = lossTrades.minOfOrNull { it.netPnl } ?: 0.0

        val avgWinHold = if (winTrades.isNotEmpty()) {
            winTrades.map { it.holdingDurationMinutes }.average().toLong()
        } else 0L
        val avgLossHold = if (lossTrades.isNotEmpty()) {
            lossTrades.map { it.holdingDurationMinutes }.average().toLong()
        } else 0L

        // Streaks
        var currentStreak = 0
        var bestWinStreak = 0
        var worstLossStreak = 0
        var runningWin = 0
        var runningLoss = 0
        for (t in closed) {
            when (t.outcome) {
                TradeOutcome.WIN -> {
                    runningWin++
                    runningLoss = 0
                    if (runningWin > bestWinStreak) bestWinStreak = runningWin
                    currentStreak = runningWin
                }
                TradeOutcome.LOSS -> {
                    runningLoss++
                    runningWin = 0
                    if (runningLoss > worstLossStreak) worstLossStreak = runningLoss
                    currentStreak = -runningLoss
                }
                else -> Unit
            }
        }

        // Daily aggregation & equity curve + drawdown
        val groupedByDay = closed.groupBy { it.effectiveDateIso }.toSortedMap()
        val dailyMap = LinkedHashMap<String, DailySummary>()
        val equityCurve = mutableListOf<EquityPoint>()

        var cumulative = 0.0
        var peakEquity = startingEquity
        var maxDrawdownDollars = 0.0
        var maxDrawdownPct = 0.0
        var winningDays = 0
        var losingDays = 0
        var breakevenDays = 0
        var bestDayPnl = Double.NEGATIVE_INFINITY
        var worstDayPnl = Double.POSITIVE_INFINITY

        for ((dateIso, dayTrades) in groupedByDay) {
            val dayPnl = dayTrades.sumOf { it.netPnl }
            val dayWins = dayTrades.count { it.outcome == TradeOutcome.WIN }
            val dayLosses = dayTrades.count { it.outcome == TradeOutcome.LOSS }
            val dayRs = dayTrades.mapNotNull { it.rMultiple }

            dailyMap[dateIso] = DailySummary(
                dateIso = dateIso,
                netPnl = dayPnl,
                tradeCount = dayTrades.size,
                wins = dayWins,
                losses = dayLosses,
                totalR = dayRs.sum(),
                rCount = dayRs.size
            )

            when {
                dayPnl > 0.01 -> winningDays++
                dayPnl < -0.01 -> losingDays++
                else -> breakevenDays++
            }
            if (dayPnl > bestDayPnl) bestDayPnl = dayPnl
            if (dayPnl < worstDayPnl) worstDayPnl = dayPnl

            cumulative += dayPnl
            val equity = startingEquity + cumulative
            if (equity > peakEquity) peakEquity = equity
            val ddDollars = (peakEquity - equity).coerceAtLeast(0.0)
            val ddPct = if (peakEquity > 0.0) (ddDollars / peakEquity) * 100.0 else 0.0
            if (ddDollars > maxDrawdownDollars) maxDrawdownDollars = ddDollars
            if (ddPct > maxDrawdownPct) maxDrawdownPct = ddPct

            val epochForLabel = dayTrades.last().effectiveDateEpochMs
            equityCurve.add(
                EquityPoint(
                    dateIso = dateIso,
                    label = DateUtils.formatDateMonthDay(epochForLabel),
                    dailyPnl = dayPnl,
                    cumulativePnl = cumulative,
                    equity = equity,
                    drawdownDollars = ddDollars,
                    drawdownPct = ddPct,
                    tradeCount = dayTrades.size
                )
            )
        }

        val tradingDays = dailyMap.size
        val decisiveDays = winningDays + losingDays
        val dayWinRatePct = if (decisiveDays > 0) (winningDays.toDouble() / decisiveDays) * 100.0 else 0.0
        if (bestDayPnl == Double.NEGATIVE_INFINITY) bestDayPnl = 0.0
        if (worstDayPnl == Double.POSITIVE_INFINITY) worstDayPnl = 0.0

        // Edge Score (5-axis composite)
        val edgeScore = computeEdgeScore(
            closed = closed,
            winRatePct = tradeWinRatePct,
            profitFactor = profitFactor,
            payoffRatio = avgWinLossRatio,
            dailyPnls = dailyMap.values.map { it.netPnl },
            maxDrawdownPct = maxDrawdownPct
        )

        // Day of week breakdown (Mon..Fri, plus weekend if crypto/forex traded)
        val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val byDow = closed.groupBy { DateUtils.dayOfWeekMondayBased(it.entryTimeEpochMs) }
        val dayOfWeekBreakdown = (0..6).mapNotNull { dow ->
            val list = byDow[dow].orEmpty()
            if (dow >= 5 && list.isEmpty()) null else buildBucket(dow.toString(), dayLabels[dow], list)
        }

        // Session / Time of day breakdown
        val sessionBuckets = listOf(
            "PRE" to "Pre-Mkt",
            "OPEN" to "Open 9:30",
            "MID_AM" to "Mid-AM",
            "LUNCH" to "Lunch",
            "POWER" to "Power Hr",
            "AFTER" to "After-Hrs"
        )
        val bySession = closed.groupBy { classifySession(it.entryTimeEpochMs) }
        val sessionBreakdown = sessionBuckets.map { (key, label) ->
            buildBucket(key, label, bySession[key].orEmpty())
        }

        // Symbol breakdown (sorted by net P&L descending)
        val symbolBreakdown = closed
            .groupBy { it.symbol.trim().uppercase() }
            .map { (sym, list) -> buildBucket(sym, sym, list) }
            .sortedByDescending { it.netPnl }

        // Asset class breakdown
        val assetClassBreakdown = AssetClass.entries
            .mapNotNull { asset ->
                val list = closed.filter { it.assetClassEnum == asset }
                if (list.isEmpty()) null else buildBucket(asset.name, asset.label, list)
            }
            .sortedByDescending { it.netPnl }

        // Long vs Short
        val longStats = buildSideSummary(TradeSide.LONG, closed.filter { it.sideEnum == TradeSide.LONG })
        val shortStats = buildSideSummary(TradeSide.SHORT, closed.filter { it.sideEnum == TradeSide.SHORT })

        // Mistake cost quantifier
        val mistakeGroups = mutableMapOf<String, MutableList<TradeRecord>>()
        for (t in closed) {
            for (tag in t.mistakeTags) {
                mistakeGroups.getOrPut(tag) { mutableListOf() }.add(t)
            }
        }
        val mistakeBreakdown = mistakeGroups.map { (tag, list) ->
            val pnl = list.sumOf { it.netPnl }
            val w = list.count { it.outcome == TradeOutcome.WIN }
            val l = list.count { it.outcome == TradeOutcome.LOSS }
            val wr = if (w + l > 0) (w.toDouble() / (w + l)) * 100.0 else 0.0
            MistakeMetric(
                tag = tag,
                tradeCount = list.size,
                totalPnl = pnl,
                avgPnl = pnl / list.size,
                winRatePct = wr
            )
        }.sortedBy { it.totalPnl } // Most costly mistakes first

        // Emotion breakdown
        val emotionBreakdown = EmotionTag.entries
            .mapNotNull { emotion ->
                val list = closed.filter { it.emotionEnum == emotion }
                if (list.isEmpty()) null else buildBucket(emotion.name, emotion.label, list)
            }
            .sortedByDescending { it.netPnl }

        // R-Multiple Distribution
        val rDistribution = listOf(
            RDistributionBucket("<-2R", rValues.count { it < -2.0 }, false),
            RDistributionBucket("-2..-1R", rValues.count { it >= -2.0 && it < -1.0 }, false),
            RDistributionBucket("-1..0R", rValues.count { it >= -1.0 && it < 0.0 }, false),
            RDistributionBucket("0..1R", rValues.count { it >= 0.0 && it < 1.0 }, true),
            RDistributionBucket("1..2R", rValues.count { it >= 1.0 && it < 2.0 }, true),
            RDistributionBucket("2..3R", rValues.count { it >= 2.0 && it < 3.0 }, true),
            RDistributionBucket(">3R", rValues.count { it >= 3.0 }, true)
        )

        // Playbook stats
        val playbookMetrics = playbooks.associate { pb ->
            val pbTrades = closed.filter { it.playbookId == pb.id }
            pb.id to buildPlaybookStats(pb.id, pbTrades)
        }

        return PerformanceSummary(
            totalTrades = totalTrades,
            closedTrades = closed.size,
            openTrades = openCount,
            wins = wins,
            losses = losses,
            breakevens = breakevens,
            netPnl = netPnl,
            grossProfit = grossProfit,
            grossLoss = -grossLossAbs,
            totalFees = totalFees,
            startingEquity = startingEquity,
            currentEquity = currentEquity,
            returnOnEquityPct = roePct,
            tradeWinRatePct = tradeWinRatePct,
            profitFactor = profitFactor,
            avgWin = avgWin,
            avgLoss = avgLoss,
            avgWinLossRatio = avgWinLossRatio,
            expectancy = expectancy,
            avgRMultiple = avgRMultiple,
            totalNetR = totalNetR,
            rTrackedCount = rValues.size,
            tradingDays = tradingDays,
            winningDays = winningDays,
            losingDays = losingDays,
            breakevenDays = breakevenDays,
            dayWinRatePct = dayWinRatePct,
            bestDayPnl = bestDayPnl,
            worstDayPnl = worstDayPnl,
            largestWin = largestWin,
            largestLoss = largestLoss,
            maxDrawdownDollars = maxDrawdownDollars,
            maxDrawdownPct = maxDrawdownPct,
            currentStreak = currentStreak,
            bestWinStreak = bestWinStreak,
            worstLossStreak = worstLossStreak,
            avgWinHoldMinutes = avgWinHold,
            avgLossHoldMinutes = avgLossHold,
            edgeScore = edgeScore,
            equityCurve = equityCurve,
            dailyPnlMap = dailyMap,
            dayOfWeekBreakdown = dayOfWeekBreakdown,
            sessionBreakdown = sessionBreakdown,
            symbolBreakdown = symbolBreakdown,
            assetClassBreakdown = assetClassBreakdown,
            longStats = longStats,
            shortStats = shortStats,
            mistakeBreakdown = mistakeBreakdown,
            emotionBreakdown = emotionBreakdown,
            rDistribution = rDistribution,
            playbookMetrics = playbookMetrics
        )
    }

    private fun computeProfitFactor(grossProfit: Double, grossLossAbs: Double): Double {
        return when {
            grossLossAbs > 0.0 -> grossProfit / grossLossAbs
            grossProfit > 0.0 -> Double.POSITIVE_INFINITY
            else -> 0.0
        }
    }

    private fun computeEdgeScore(
        closed: List<TradeRecord>,
        winRatePct: Double,
        profitFactor: Double,
        payoffRatio: Double,
        dailyPnls: List<Double>,
        maxDrawdownPct: Double
    ): EdgeScoreBreakdown {
        // 1. Win Rate score: 30% -> 20, 50% -> 60, 70%+ -> 100
        val winRateScore = ((winRatePct - 20.0) / 50.0 * 100.0).coerceIn(5.0, 100.0).toFloat()

        // 2. Profit Factor score: 0.5 -> 15, 1.5 -> 60, 2.8+ -> 100
        val clampedPf = if (profitFactor.isInfinite()) 3.0 else profitFactor.coerceIn(0.0, 3.0)
        val pfScore = (clampedPf / 2.6 * 100.0).coerceIn(0.0, 100.0).toFloat()

        // 3. Avg Win / Avg Loss Payoff score: 1.0 -> 45, 2.0 -> 85, 2.5+ -> 100
        val clampedPayoff = if (payoffRatio.isInfinite()) 2.6 else payoffRatio.coerceIn(0.0, 2.6)
        val payoffScore = (clampedPayoff / 2.3 * 100.0).coerceIn(0.0, 100.0).toFloat()

        // 4. Consistency score: penalize high max drawdown & extreme daily variance
        val ddPenalty = min(65.0, maxDrawdownPct * 2.5)
        val stdPenalty = if (dailyPnls.size >= 3) {
            val mean = dailyPnls.average()
            val variance = dailyPnls.map { (it - mean) * (it - mean) }.average()
            val std = sqrt(variance)
            val meanAbs = dailyPnls.map { abs(it) }.average().coerceAtLeast(1.0)
            min(30.0, (std / meanAbs) * 14.0)
        } else 15.0
        val positiveDaysBonus = if (dailyPnls.isNotEmpty()) {
            (dailyPnls.count { it > 0.0 }.toDouble() / dailyPnls.size) * 20.0
        } else 0.0
        val consistencyScore = (85.0 - ddPenalty - stdPenalty + positiveDaysBonus).coerceIn(10.0, 100.0).toFloat()

        // 5. Discipline score: Playbook adherence & low mistake rate
        val mistakeRate = closed.count { it.mistakeTags.isNotEmpty() }.toDouble() / max(1, closed.size)
        val linkedToPlaybook = closed.filter { it.playbookId != null }
        val avgAdherence = if (linkedToPlaybook.isNotEmpty()) {
            linkedToPlaybook.map { it.playbookAdherencePct }.average()
        } else {
            75.0
        }
        val avgRatingScore = (closed.map { it.rating }.average() / 5.0) * 100.0
        val disciplineScore = (
            avgAdherence * 0.45 +
                (1.0 - mistakeRate) * 100.0 * 0.35 +
                avgRatingScore * 0.20
            ).coerceIn(10.0, 100.0).toFloat()

        val overall = (
            winRateScore * 0.20f +
                pfScore * 0.25f +
                payoffScore * 0.20f +
                consistencyScore * 0.20f +
                disciplineScore * 0.15f
            ).toInt().coerceIn(0, 100)

        val grade = when {
            overall >= 90 -> "A+"
            overall >= 82 -> "A"
            overall >= 74 -> "B+"
            overall >= 65 -> "B"
            overall >= 55 -> "C+"
            overall >= 45 -> "C"
            else -> "D"
        }

        return EdgeScoreBreakdown(
            overall = overall,
            grade = grade,
            winRateScore = winRateScore,
            profitFactorScore = pfScore,
            payoffRatioScore = payoffScore,
            consistencyScore = consistencyScore,
            disciplineScore = disciplineScore
        )
    }

    private fun buildBucket(key: String, label: String, trades: List<TradeRecord>): BucketMetric {
        val wins = trades.count { it.outcome == TradeOutcome.WIN }
        val losses = trades.count { it.outcome == TradeOutcome.LOSS }
        val netPnl = trades.sumOf { it.netPnl }
        val gp = trades.filter { it.outcome == TradeOutcome.WIN }.sumOf { it.netPnl }
        val gl = abs(trades.filter { it.outcome == TradeOutcome.LOSS }.sumOf { it.netPnl })
        val wr = if (wins + losses > 0) (wins.toDouble() / (wins + losses)) * 100.0 else 0.0
        val avg = if (trades.isNotEmpty()) netPnl / trades.size else 0.0
        return BucketMetric(
            key = key,
            label = label,
            tradeCount = trades.size,
            wins = wins,
            losses = losses,
            netPnl = netPnl,
            avgPnl = avg,
            winRatePct = wr,
            profitFactor = computeProfitFactor(gp, gl)
        )
    }

    private fun buildSideSummary(side: TradeSide, trades: List<TradeRecord>): SideSummary {
        val wins = trades.count { it.outcome == TradeOutcome.WIN }
        val losses = trades.count { it.outcome == TradeOutcome.LOSS }
        val netPnl = trades.sumOf { it.netPnl }
        val gp = trades.filter { it.outcome == TradeOutcome.WIN }.sumOf { it.netPnl }
        val gl = abs(trades.filter { it.outcome == TradeOutcome.LOSS }.sumOf { it.netPnl })
        val wr = if (wins + losses > 0) (wins.toDouble() / (wins + losses)) * 100.0 else 0.0
        val avg = if (trades.isNotEmpty()) netPnl / trades.size else 0.0
        return SideSummary(
            side = side,
            tradeCount = trades.size,
            wins = wins,
            losses = losses,
            netPnl = netPnl,
            winRatePct = wr,
            profitFactor = computeProfitFactor(gp, gl),
            avgPnl = avg
        )
    }

    private fun buildPlaybookStats(playbookId: String, trades: List<TradeRecord>): PlaybookStats {
        if (trades.isEmpty()) {
            return PlaybookStats(playbookId, 0, 0, 0, 0.0, 0.0, 0.0, 0.0, null, 0.0)
        }
        val winList = trades.filter { it.outcome == TradeOutcome.WIN }
        val lossList = trades.filter { it.outcome == TradeOutcome.LOSS }
        val wins = winList.size
        val losses = lossList.size
        val netPnl = trades.sumOf { it.netPnl }
        val gp = winList.sumOf { it.netPnl }
        val gl = abs(lossList.sumOf { it.netPnl })
        val decisive = wins + losses
        val wr = if (decisive > 0) (wins.toDouble() / decisive) * 100.0 else 0.0
        val avgW = if (wins > 0) gp / wins else 0.0
        val avgL = if (losses > 0) gl / losses else 0.0
        val expectancy = if (decisive > 0) {
            (wins.toDouble() / decisive) * avgW - (losses.toDouble() / decisive) * avgL
        } else 0.0
        val rs = trades.mapNotNull { it.rMultiple }
        return PlaybookStats(
            playbookId = playbookId,
            tradeCount = trades.size,
            wins = wins,
            losses = losses,
            netPnl = netPnl,
            winRatePct = wr,
            profitFactor = computeProfitFactor(gp, gl),
            expectancy = expectancy,
            avgR = if (rs.isNotEmpty()) rs.average() else null,
            avgAdherencePct = trades.map { it.playbookAdherencePct }.average()
        )
    }

    private fun classifySession(epochMs: Long): String {
        val (hour, minute) = DateUtils.hourAndMinute(epochMs)
        val mins = hour * 60 + minute
        return when {
            mins < 9 * 60 + 30 -> "PRE"
            mins < 10 * 60 + 30 -> "OPEN"
            mins < 12 * 60 -> "MID_AM"
            mins < 14 * 60 -> "LUNCH"
            mins < 16 * 60 -> "POWER"
            else -> "AFTER"
        }
    }
}
