package com.example

import com.example.data.model.AssetClass
import com.example.data.model.EmotionTag
import com.example.data.model.PlaybookRecord
import com.example.data.model.TradeRecord
import com.example.data.model.TradeSide
import com.example.data.model.TradeStatus
import com.example.domain.DateUtils
import com.example.domain.TradeAnalyticsEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TradeAnalyticsEngineTest {

    @Test
    fun `analyze computes profit factor, expectancy, drawdown, R-multiples, and mistakes accurately`() {
        val pb = PlaybookRecord(id = "pb_orb", name = "ORB", rules = listOf("Rule 1", "Rule 2"))
        val day1 = DateUtils.isoDateToEpoch("2026-11-10", 9, 45)
        val day2 = DateUtils.isoDateToEpoch("2026-11-11", 10, 15)
        val day3 = DateUtils.isoDateToEpoch("2026-11-12", 11, 0)

        val trades = listOf(
            // Win: (110 - 100) * 100 - 10 = +990, Risk = (100 - 95) * 100 = 500 -> +1.98R
            TradeRecord(
                id = "t1", accountId = "acc_1", symbol = "NVDA", assetClass = AssetClass.STOCK.name,
                side = TradeSide.LONG.name, status = TradeStatus.CLOSED.name,
                entryPrice = 100.0, exitPrice = 110.0, quantity = 100.0, contractMultiplier = 1.0,
                fees = 10.0, stopLossPrice = 95.0,
                entryTimeEpochMs = day1, exitTimeEpochMs = day1 + 60 * 60_000L,
                playbookId = "pb_orb", playbookAdherencePct = 100, emotionBefore = EmotionTag.CALM.name, rating = 5
            ),
            // Loss (Short): (50 - 54) * 100 - 5 = -405, Risk = (54 - 50) * 100 = 400 -> -1.0125R
            TradeRecord(
                id = "t2", accountId = "acc_1", symbol = "AMD", assetClass = AssetClass.STOCK.name,
                side = TradeSide.SHORT.name, status = TradeStatus.CLOSED.name,
                entryPrice = 50.0, exitPrice = 54.0, quantity = 100.0, contractMultiplier = 1.0,
                fees = 5.0, stopLossPrice = 54.0,
                entryTimeEpochMs = day2, exitTimeEpochMs = day2 + 30 * 60_000L,
                playbookId = "pb_orb", playbookAdherencePct = 50, mistakeTags = listOf("FOMO"),
                emotionBefore = EmotionTag.FOMO.name, rating = 2
            ),
            // Win (Futures): (5020 - 5010) * 1 * 50 - 5 = +495, Risk = (5010 - 5005) * 50 = 250 -> +1.98R
            TradeRecord(
                id = "t3", accountId = "acc_1", symbol = "ES", assetClass = AssetClass.FUTURES.name,
                side = TradeSide.LONG.name, status = TradeStatus.CLOSED.name,
                entryPrice = 5010.0, exitPrice = 5020.0, quantity = 1.0, contractMultiplier = 50.0,
                fees = 5.0, stopLossPrice = 5005.0,
                entryTimeEpochMs = day3, exitTimeEpochMs = day3 + 45 * 60_000L,
                playbookId = "pb_orb", playbookAdherencePct = 100, emotionBefore = EmotionTag.DISCIPLINED.name, rating = 5
            ),
            // Open trade (excluded from closed metrics)
            TradeRecord(
                id = "t4", accountId = "acc_1", symbol = "AAPL", assetClass = AssetClass.STOCK.name,
                side = TradeSide.LONG.name, status = TradeStatus.OPEN.name,
                entryPrice = 225.0, exitPrice = null, quantity = 50.0, contractMultiplier = 1.0,
                entryTimeEpochMs = day3 + 7_200_000L
            )
        )

        val summary = TradeAnalyticsEngine.analyze(trades, startingEquity = 10_000.0, playbooks = listOf(pb))

        assertEquals(4, summary.totalTrades)
        assertEquals(3, summary.closedTrades)
        assertEquals(1, summary.openTrades)
        assertEquals(2, summary.wins)
        assertEquals(1, summary.losses)
        assertEquals(1080.0, summary.netPnl, 0.01)
        assertEquals(11_080.0, summary.currentEquity, 0.01)
        assertEquals(66.67, summary.tradeWinRatePct, 0.05)
        assertEquals(3.67, summary.profitFactor, 0.02)
        assertEquals(360.0, summary.expectancy, 0.01)
        assertEquals(405.0, summary.maxDrawdownDollars, 0.01)
        assertEquals(3, summary.rTrackedCount)
        assertNotNull(summary.avgRMultiple)
        assertEquals(1, summary.mistakeBreakdown.size)
        assertEquals("FOMO", summary.mistakeBreakdown.first().tag)
        assertEquals(-405.0, summary.mistakeBreakdown.first().totalPnl, 0.01)
        assertTrue(summary.edgeScore.overall in 50..100)
        assertEquals(3, summary.playbookMetrics["pb_orb"]?.tradeCount)
    }

    @Test
    fun `analyze handles empty trade list safely`() {
        val summary = TradeAnalyticsEngine.analyze(emptyList(), startingEquity = 25_000.0)
        assertEquals(0, summary.totalTrades)
        assertEquals(25_000.0, summary.currentEquity, 0.01)
        assertEquals(0.0, summary.profitFactor, 0.001)
        assertTrue(summary.equityCurve.isEmpty())
    }
}
