package com.example

import com.example.data.backup.JournalBackupSnapshot
import com.example.data.backup.TradeJsonBackupCodec
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TradeJsonBackupCodecTest {

    @Test
    fun `encode and decode round-trips complete multi-entity journal snapshot`() {
        val original = JournalBackupSnapshot(
            accounts = listOf(
                TradingAccountRecord(
                    id = "acc_main",
                    name = "Main Margin",
                    broker = "IBKR",
                    startingBalance = 50_000.0,
                    currency = "USD"
                )
            ),
            playbooks = listOf(
                PlaybookRecord(
                    id = "pb_orb",
                    name = "15m ORB",
                    description = "Breakout retest",
                    marketCondition = "Trending",
                    rules = listOf("High RVOL", "Clean range"),
                    targetRiskReward = 2.5,
                    colorHex = 0xFF00D084L,
                    demoRecord = false
                )
            ),
            trades = listOf(
                TradeRecord(
                    id = "tr_1",
                    accountId = "acc_main",
                    symbol = "NVDA",
                    assetClass = AssetClass.STOCK.name,
                    side = TradeSide.LONG.name,
                    status = TradeStatus.CLOSED.name,
                    entryPrice = 140.0,
                    exitPrice = 146.5,
                    quantity = 100.0,
                    contractMultiplier = 1.0,
                    fees = 4.0,
                    stopLossPrice = 138.0,
                    takeProfitPrice = 148.0,
                    entryTimeEpochMs = 1_760_000_000_000L,
                    exitTimeEpochMs = 1_760_003_600_000L,
                    playbookId = "pb_orb",
                    playbookAdherencePct = 100,
                    checkedRules = listOf("High RVOL", "Clean range"),
                    mistakeTags = emptyList(),
                    setupTags = listOf("A+ Setup"),
                    emotionBefore = EmotionTag.DISCIPLINED.name,
                    rating = 5,
                    notes = "Bought ORB breakout with semiconductor strength",
                    demoRecord = false
                )
            ),
            journals = listOf(
                DailyJournalRecord(
                    id = "acc_main_2026-11-20",
                    accountId = "acc_main",
                    dateIso = "2026-11-20",
                    preMarketPlan = "Watch NVDA above 140",
                    postMarketReview = "Executed cleanly",
                    lessonsLearned = "Patience pays",
                    mood = JournalMood.DISCIPLINED.name,
                    marketRegime = MarketRegime.TRENDING_UP.name,
                    followedPlan = true,
                    demoRecord = false
                )
            )
        )

        val encoded = TradeJsonBackupCodec.encode(original)
        val decoded = TradeJsonBackupCodec.decode(encoded).getOrThrow()

        assertEquals(1, decoded.accounts.size)
        assertEquals("Main Margin", decoded.accounts.first().name)
        assertEquals(1, decoded.playbooks.size)
        assertEquals(listOf("High RVOL", "Clean range"), decoded.playbooks.first().rules)
        assertEquals(1, decoded.trades.size)
        assertEquals("NVDA", decoded.trades.first().symbol)
        assertEquals(646.0, decoded.trades.first().netPnl, 0.01)
        assertEquals("pb_orb", decoded.trades.first().playbookId)
        assertEquals(1, decoded.journals.size)
        assertEquals("2026-11-20", decoded.journals.first().dateIso)
        assertTrue(decoded.journals.first().followedPlan)
    }

    @Test
    fun `decode rejects empty or malformed json gracefully`() {
        val result = TradeJsonBackupCodec.decode("{}")
        assertTrue(result.isFailure)
    }
}
