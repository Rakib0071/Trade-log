package com.example.data.sample

import com.example.data.model.AssetClass
import com.example.data.model.DailyJournalRecord
import com.example.data.model.EmotionTag
import com.example.data.model.JournalMood
import com.example.data.model.MarketRegime
import com.example.data.model.PlaybookRecord
import com.example.data.model.TradeRecord
import com.example.data.model.TradeSide
import com.example.data.model.TradeStatus
import com.example.domain.DateUtils
import java.util.Calendar

/**
 * Generates an optional, clearly tagged demo trading history ONLY when explicitly requested by the user.
 */
object SampleDatasetGenerator {

    fun buildPlaybooks(): List<PlaybookRecord> = listOf(
        PlaybookRecord(
            name = "Opening Range Breakout",
            description = "Momentum continuation above/below the first 15-minute range on high relative volume.",
            marketCondition = "Trending",
            rules = listOf(
                "RVOL > 1.5x at open",
                "Clean 15m range structure",
                "Entry on retest of ORB level",
                "Hard stop below range midpoint"
            ),
            targetRiskReward = 2.5,
            colorHex = 0xFF00D084L,
            demoRecord = true
        ),
        PlaybookRecord(
            name = "VWAP Trend Pullback",
            description = "Institutional trend continuation entry after a controlled pullback into intraday VWAP.",
            marketCondition = "Trending",
            rules = listOf(
                "Higher-timeframe bias aligned",
                "Price holds VWAP on declining pullback volume",
                "Higher low confirmed on 5m",
                "No major catalyst within 30m"
            ),
            targetRiskReward = 2.2,
            colorHex = 0xFF6366F1L,
            demoRecord = true
        ),
        PlaybookRecord(
            name = "Liquidity Sweep Reversal",
            description = "Fade failed breakout above prior day high/low after aggressive stop-run absorption.",
            marketCondition = "Range-Bound",
            rules = listOf(
                "Sweep of PDH or PDL",
                "Immediate displacement wick back inside range",
                "Structure shift on 5m",
                "Fixed max risk 1R"
            ),
            targetRiskReward = 3.0,
            colorHex = 0xFF38BDF8L,
            demoRecord = true
        )
    )

    fun buildTradesAndJournals(
        accountId: String,
        orbId: String,
        vwapId: String,
        sweepId: String,
        nowMs: Long = System.currentTimeMillis()
    ): Pair<List<TradeRecord>, List<DailyJournalRecord>> {
        val tradingDays = mutableListOf<Calendar>()
        val cursor = Calendar.getInstance().apply { timeInMillis = nowMs }
        while (tradingDays.size < 18) {
            val dow = cursor.get(Calendar.DAY_OF_WEEK)
            if (dow != Calendar.SATURDAY && dow != Calendar.SUNDAY) {
                tradingDays.add(cursor.clone() as Calendar)
            }
            cursor.add(Calendar.DAY_OF_YEAR, -1)
        }
        tradingDays.reverse() // oldest -> newest

        fun dayTime(dayIdx: Int, hour: Int, minute: Int): Long {
            val c = tradingDays[dayIdx.coerceIn(0, tradingDays.lastIndex)].clone() as Calendar
            c.set(Calendar.HOUR_OF_DAY, hour)
            c.set(Calendar.MINUTE, minute)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            return c.timeInMillis
        }

        val trades = listOf(
            TradeRecord(
                accountId = accountId, symbol = "NVDA", assetClass = AssetClass.STOCK.name,
                side = TradeSide.LONG.name, status = TradeStatus.CLOSED.name,
                entryPrice = 134.20, exitPrice = 138.90, quantity = 100.0, contractMultiplier = 1.0,
                fees = 3.60, stopLossPrice = 132.40, takeProfitPrice = 139.00,
                entryTimeEpochMs = dayTime(0, 9, 46), exitTimeEpochMs = dayTime(0, 11, 18),
                playbookId = orbId, playbookAdherencePct = 100,
                checkedRules = listOf(
                    "RVOL > 1.5x at open",
                    "Clean 15m range structure",
                    "Entry on retest of ORB level",
                    "Hard stop below range midpoint"
                ),
                setupTags = listOf("A+ Setup", "Opening Range"),
                emotionBefore = EmotionTag.CALM.name, rating = 5,
                notes = "Textbook 15m ORB retest with semiconductor sector leadership. Scaled out into $139 resistance.",
                demoRecord = true
            ),
            TradeRecord(
                accountId = accountId, symbol = "ES", assetClass = AssetClass.FUTURES.name,
                side = TradeSide.SHORT.name, status = TradeStatus.CLOSED.name,
                entryPrice = 5942.0, exitPrice = 5931.5, quantity = 1.0, contractMultiplier = 50.0,
                fees = 4.60, stopLossPrice = 5948.0, takeProfitPrice = 5928.0,
                entryTimeEpochMs = dayTime(1, 10, 5), exitTimeEpochMs = dayTime(1, 11, 20),
                playbookId = sweepId, playbookAdherencePct = 100,
                setupTags = listOf("Liquidity Sweep"),
                emotionBefore = EmotionTag.DISCIPLINED.name, rating = 5,
                notes = "Swept prior day high by 3 handles and immediately snapped back inside range.",
                demoRecord = true
            ),
            TradeRecord(
                accountId = accountId, symbol = "TSLA", assetClass = AssetClass.STOCK.name,
                side = TradeSide.LONG.name, status = TradeStatus.CLOSED.name,
                entryPrice = 324.50, exitPrice = 319.80, quantity = 80.0, contractMultiplier = 1.0,
                fees = 3.20, stopLossPrice = 320.00, takeProfitPrice = 334.00,
                entryTimeEpochMs = dayTime(2, 12, 35), exitTimeEpochMs = dayTime(2, 13, 50),
                playbookId = orbId, playbookAdherencePct = 50,
                mistakeTags = listOf("FOMO", "Chased Entry"),
                setupTags = listOf("Breakout"),
                emotionBefore = EmotionTag.FOMO.name, rating = 2,
                notes = "Chased an extended midday candle during lunch chop instead of waiting for pullback.",
                demoRecord = true
            ),
            TradeRecord(
                accountId = accountId, symbol = "META", assetClass = AssetClass.STOCK.name,
                side = TradeSide.LONG.name, status = TradeStatus.CLOSED.name,
                entryPrice = 582.00, exitPrice = 591.40, quantity = 50.0, contractMultiplier = 1.0,
                fees = 3.10, stopLossPrice = 578.00, takeProfitPrice = 592.00,
                entryTimeEpochMs = dayTime(3, 10, 42), exitTimeEpochMs = dayTime(3, 13, 15),
                playbookId = vwapId, playbookAdherencePct = 100,
                setupTags = listOf("VWAP Reclaim", "A+ Setup"),
                emotionBefore = EmotionTag.CONFIDENT.name, rating = 5,
                notes = "Clean higher low at VWAP; held calmly through minor mid-session consolidation.",
                demoRecord = true
            ),
            TradeRecord(
                accountId = accountId, symbol = "SPY", assetClass = AssetClass.OPTIONS.name,
                side = TradeSide.LONG.name, status = TradeStatus.CLOSED.name,
                entryPrice = 2.45, exitPrice = 3.28, quantity = 5.0, contractMultiplier = 100.0,
                fees = 6.50, stopLossPrice = 1.95, takeProfitPrice = 3.45,
                entryTimeEpochMs = dayTime(4, 9, 52), exitTimeEpochMs = dayTime(4, 10, 40),
                playbookId = orbId, playbookAdherencePct = 75,
                setupTags = listOf("Opening Range"),
                emotionBefore = EmotionTag.CALM.name, rating = 4,
                notes = "0DTE calls off opening drive; took profit just before key level.",
                demoRecord = true
            ),
            TradeRecord(
                accountId = accountId, symbol = "AMD", assetClass = AssetClass.STOCK.name,
                side = TradeSide.SHORT.name, status = TradeStatus.CLOSED.name,
                entryPrice = 151.60, exitPrice = 153.40, quantity = 150.0, contractMultiplier = 1.0,
                fees = 3.80, stopLossPrice = 153.20, takeProfitPrice = 147.50,
                entryTimeEpochMs = dayTime(5, 11, 10), exitTimeEpochMs = dayTime(5, 12, 5),
                playbookId = vwapId, playbookAdherencePct = 75,
                mistakeTags = listOf("Early Exit"),
                emotionBefore = EmotionTag.ANXIOUS.name, rating = 3,
                notes = "Valid setup stopped out on sector bounce.",
                demoRecord = true
            ),
            TradeRecord(
                accountId = accountId, symbol = "AMD", assetClass = AssetClass.STOCK.name,
                side = TradeSide.SHORT.name, status = TradeStatus.CLOSED.name,
                entryPrice = 153.10, exitPrice = 155.30, quantity = 150.0, contractMultiplier = 1.0,
                fees = 3.80, stopLossPrice = 155.00, takeProfitPrice = 149.00,
                entryTimeEpochMs = dayTime(5, 12, 22), exitTimeEpochMs = dayTime(5, 13, 18),
                playbookId = null, playbookAdherencePct = 25,
                mistakeTags = listOf("Revenge Trade", "Overtrading"),
                emotionBefore = EmotionTag.FRUSTRATED.name, rating = 1,
                notes = "Re-entered immediately out of frustration after first loss. Violated daily rule.",
                demoRecord = true
            ),
            TradeRecord(
                accountId = accountId, symbol = "NQ", assetClass = AssetClass.FUTURES.name,
                side = TradeSide.LONG.name, status = TradeStatus.CLOSED.name,
                entryPrice = 20810.0, exitPrice = 20852.0, quantity = 1.0, contractMultiplier = 20.0,
                fees = 4.80, stopLossPrice = 20788.0, takeProfitPrice = 20855.0,
                entryTimeEpochMs = dayTime(6, 9, 48), exitTimeEpochMs = dayTime(6, 10, 55),
                playbookId = orbId, playbookAdherencePct = 100,
                setupTags = listOf("A+ Setup", "Opening Range"),
                emotionBefore = EmotionTag.DISCIPLINED.name, rating = 5,
                notes = "Reset mindset after yesterday. Waited strictly for 15m ORB retest and let winner run.",
                demoRecord = true
            ),
            TradeRecord(
                accountId = accountId, symbol = "BTCUSD", assetClass = AssetClass.CRYPTO.name,
                side = TradeSide.LONG.name, status = TradeStatus.CLOSED.name,
                entryPrice = 91400.0, exitPrice = 93150.0, quantity = 0.25, contractMultiplier = 1.0,
                fees = 14.50, stopLossPrice = 90650.0, takeProfitPrice = 93500.0,
                entryTimeEpochMs = dayTime(7, 14, 15), exitTimeEpochMs = dayTime(7, 17, 40),
                playbookId = vwapId, playbookAdherencePct = 100,
                setupTags = listOf("Trend Pullback"),
                emotionBefore = EmotionTag.CALM.name, rating = 5,
                notes = "Strong power-hour spot flow pushing BTC through intraday compression.",
                demoRecord = true
            ),
            TradeRecord(
                accountId = accountId, symbol = "AAPL", assetClass = AssetClass.STOCK.name,
                side = TradeSide.LONG.name, status = TradeStatus.CLOSED.name,
                entryPrice = 228.40, exitPrice = 231.10, quantity = 120.0, contractMultiplier = 1.0,
                fees = 3.00, stopLossPrice = 227.10, takeProfitPrice = 231.50,
                entryTimeEpochMs = dayTime(8, 10, 15), exitTimeEpochMs = dayTime(8, 12, 30),
                playbookId = vwapId, playbookAdherencePct = 100,
                setupTags = listOf("VWAP Reclaim"),
                emotionBefore = EmotionTag.CALM.name, rating = 4,
                notes = "Steady grind higher above rising VWAP.",
                demoRecord = true
            ),
            TradeRecord(
                accountId = accountId, symbol = "NVDA", assetClass = AssetClass.STOCK.name,
                side = TradeSide.SHORT.name, status = TradeStatus.CLOSED.name,
                entryPrice = 141.10, exitPrice = 142.85, quantity = 100.0, contractMultiplier = 1.0,
                fees = 3.20, stopLossPrice = 142.80, takeProfitPrice = 137.50,
                entryTimeEpochMs = dayTime(9, 13, 10), exitTimeEpochMs = dayTime(9, 14, 25),
                playbookId = sweepId, playbookAdherencePct = 75,
                mistakeTags = listOf("Moved Stop"),
                emotionBefore = EmotionTag.ANXIOUS.name, rating = 2,
                notes = "Shorted into an uptrend day; respected stop slightly late.",
                demoRecord = true
            ),
            TradeRecord(
                accountId = accountId, symbol = "ES", assetClass = AssetClass.FUTURES.name,
                side = TradeSide.LONG.name, status = TradeStatus.CLOSED.name,
                entryPrice = 5955.0, exitPrice = 5968.5, quantity = 1.0, contractMultiplier = 50.0,
                fees = 4.60, stopLossPrice = 5948.5, takeProfitPrice = 5970.0,
                entryTimeEpochMs = dayTime(10, 9, 50), exitTimeEpochMs = dayTime(10, 11, 10),
                playbookId = orbId, playbookAdherencePct = 100,
                setupTags = listOf("Opening Range", "A+ Setup"),
                emotionBefore = EmotionTag.CONFIDENT.name, rating = 5,
                notes = "Clean breadth thrust right off opening balance.",
                demoRecord = true
            ),
            TradeRecord(
                accountId = accountId, symbol = "SOLUSD", assetClass = AssetClass.CRYPTO.name,
                side = TradeSide.LONG.name, status = TradeStatus.CLOSED.name,
                entryPrice = 218.50, exitPrice = 229.00, quantity = 35.0, contractMultiplier = 1.0,
                fees = 9.20, stopLossPrice = 213.80, takeProfitPrice = 230.00,
                entryTimeEpochMs = dayTime(11, 14, 25), exitTimeEpochMs = dayTime(11, 16, 40),
                playbookId = vwapId, playbookAdherencePct = 100,
                setupTags = listOf("Trend Pullback"),
                emotionBefore = EmotionTag.DISCIPLINED.name, rating = 5,
                notes = "Held cleanly above 4H breakout retest.",
                demoRecord = true
            ),
            TradeRecord(
                accountId = accountId, symbol = "QQQ", assetClass = AssetClass.OPTIONS.name,
                side = TradeSide.LONG.name, status = TradeStatus.CLOSED.name,
                entryPrice = 3.10, exitPrice = 2.48, quantity = 4.0, contractMultiplier = 100.0,
                fees = 5.40, stopLossPrice = 2.50, takeProfitPrice = 4.30,
                entryTimeEpochMs = dayTime(12, 12, 45), exitTimeEpochMs = dayTime(12, 13, 30),
                playbookId = orbId, playbookAdherencePct = 50,
                mistakeTags = listOf("FOMO"),
                emotionBefore = EmotionTag.FOMO.name, rating = 2,
                notes = "Lunch-hour fake breakout; theta decay hurt quickly.",
                demoRecord = true
            ),
            TradeRecord(
                accountId = accountId, symbol = "TSLA", assetClass = AssetClass.STOCK.name,
                side = TradeSide.SHORT.name, status = TradeStatus.CLOSED.name,
                entryPrice = 336.40, exitPrice = 328.90, quantity = 75.0, contractMultiplier = 1.0,
                fees = 3.40, stopLossPrice = 339.80, takeProfitPrice = 328.00,
                entryTimeEpochMs = dayTime(13, 10, 12), exitTimeEpochMs = dayTime(13, 11, 45),
                playbookId = sweepId, playbookAdherencePct = 100,
                setupTags = listOf("Liquidity Sweep", "A+ Setup"),
                emotionBefore = EmotionTag.CALM.name, rating = 5,
                notes = "Failed breakout above $338 weekly level with massive rejection wick.",
                demoRecord = true
            ),
            TradeRecord(
                accountId = accountId, symbol = "MSFT", assetClass = AssetClass.STOCK.name,
                side = TradeSide.LONG.name, status = TradeStatus.CLOSED.name,
                entryPrice = 421.80, exitPrice = 426.90, quantity = 80.0, contractMultiplier = 1.0,
                fees = 3.10, stopLossPrice = 419.50, takeProfitPrice = 427.50,
                entryTimeEpochMs = dayTime(14, 10, 50), exitTimeEpochMs = dayTime(14, 14, 5),
                playbookId = vwapId, playbookAdherencePct = 100,
                setupTags = listOf("VWAP Reclaim"),
                emotionBefore = EmotionTag.DISCIPLINED.name, rating = 4,
                notes = "Patient VWAP bounce; trailed stop under 15m higher lows.",
                demoRecord = true
            ),
            TradeRecord(
                accountId = accountId, symbol = "NQ", assetClass = AssetClass.FUTURES.name,
                side = TradeSide.LONG.name, status = TradeStatus.CLOSED.name,
                entryPrice = 20940.0, exitPrice = 20916.0, quantity = 1.0, contractMultiplier = 20.0,
                fees = 4.80, stopLossPrice = 20918.0, takeProfitPrice = 20990.0,
                entryTimeEpochMs = dayTime(15, 14, 55), exitTimeEpochMs = dayTime(15, 15, 22),
                playbookId = orbId, playbookAdherencePct = 50,
                mistakeTags = listOf("Oversized", "Chased Entry"),
                emotionBefore = EmotionTag.TIRED.name, rating = 2,
                notes = "Late day whip-saw; should have stopped trading after morning session.",
                demoRecord = true
            ),
            TradeRecord(
                accountId = accountId, symbol = "NVDA", assetClass = AssetClass.STOCK.name,
                side = TradeSide.LONG.name, status = TradeStatus.CLOSED.name,
                entryPrice = 143.10, exitPrice = 148.60, quantity = 120.0, contractMultiplier = 1.0,
                fees = 3.60, stopLossPrice = 140.90, takeProfitPrice = 149.00,
                entryTimeEpochMs = dayTime(16, 9, 44), exitTimeEpochMs = dayTime(16, 11, 30),
                playbookId = orbId, playbookAdherencePct = 100,
                setupTags = listOf("A+ Setup", "Gap & Go"),
                emotionBefore = EmotionTag.CONFIDENT.name, rating = 5,
                notes = "Post-earnings follow-through with huge institutional bid on the open.",
                demoRecord = true
            ),
            TradeRecord(
                accountId = accountId, symbol = "EURUSD", assetClass = AssetClass.FOREX.name,
                side = TradeSide.SHORT.name, status = TradeStatus.CLOSED.name,
                entryPrice = 1.06420, exitPrice = 1.06080, quantity = 10.0, contractMultiplier = 10_000.0,
                fees = 7.00, stopLossPrice = 1.06580, takeProfitPrice = 1.06050,
                entryTimeEpochMs = dayTime(16, 8, 25), exitTimeEpochMs = dayTime(16, 10, 15),
                playbookId = sweepId, playbookAdherencePct = 100,
                setupTags = listOf("Liquidity Sweep"),
                emotionBefore = EmotionTag.CALM.name, rating = 5,
                notes = "London-NY overlap sweep of Asian high; smooth 2R move into NY open.",
                demoRecord = true
            ),
            TradeRecord(
                accountId = accountId, symbol = "ES", assetClass = AssetClass.FUTURES.name,
                side = TradeSide.LONG.name, status = TradeStatus.CLOSED.name,
                entryPrice = 5980.0, exitPrice = 5993.0, quantity = 1.0, contractMultiplier = 50.0,
                fees = 4.60, stopLossPrice = 5974.0, takeProfitPrice = 5994.0,
                entryTimeEpochMs = dayTime(17, 9, 48), exitTimeEpochMs = dayTime(17, 10, 52),
                playbookId = orbId, playbookAdherencePct = 100,
                setupTags = listOf("Opening Range", "A+ Setup"),
                emotionBefore = EmotionTag.DISCIPLINED.name, rating = 5,
                notes = "Executed plan with zero hesitation. Locked in green week.",
                demoRecord = true
            ),
            TradeRecord(
                accountId = accountId, symbol = "AAPL", assetClass = AssetClass.STOCK.name,
                side = TradeSide.LONG.name, status = TradeStatus.OPEN.name,
                entryPrice = 232.60, exitPrice = null, quantity = 100.0, contractMultiplier = 1.0,
                fees = 1.50, stopLossPrice = 229.80, takeProfitPrice = 240.00,
                entryTimeEpochMs = dayTime(17, 14, 20), exitTimeEpochMs = null,
                playbookId = vwapId, playbookAdherencePct = 100,
                setupTags = listOf("Trend Pullback"),
                emotionBefore = EmotionTag.CALM.name, rating = 4,
                notes = "Active multi-day swing position with hard stop below daily structure.",
                demoRecord = true
            )
        )

        val journals = listOf(
            DailyJournalRecord(
                accountId = accountId,
                dateIso = DateUtils.epochToIsoDate(dayTime(17, 9, 0)),
                preMarketPlan = "Focus on ES opening range above 5975 and AAPL daily continuation. No lunch trades.",
                postMarketReview = "Clean execution on ES ORB (+2.1R). Stopped trading midday once target hit.",
                lessonsLearned = "Morning window (9:30-11:00) continues to generate >80% of edge.",
                mood = JournalMood.DISCIPLINED.name,
                marketRegime = MarketRegime.TRENDING_UP.name,
                followedPlan = true,
                demoRecord = true
            ),
            DailyJournalRecord(
                accountId = accountId,
                dateIso = DateUtils.epochToIsoDate(dayTime(16, 9, 0)),
                preMarketPlan = "Watch NVDA gap & go and EURUSD London high sweep.",
                postMarketReview = "Strongest session of the month. Both setups hit targets cleanly.",
                lessonsLearned = "Pre-defining stop and target before entry eliminates mid-trade anxiety.",
                mood = JournalMood.FOCUSED.name,
                marketRegime = MarketRegime.TRENDING_UP.name,
                followedPlan = true,
                demoRecord = true
            ),
            DailyJournalRecord(
                accountId = accountId,
                dateIso = DateUtils.epochToIsoDate(dayTime(15, 9, 0)),
                preMarketPlan = "Stay selective ahead of late-week positioning.",
                postMarketReview = "Overstayed into late afternoon and chased NQ when tired.",
                lessonsLearned = "Hard cutoff at 14:30 if no A+ setup has triggered.",
                mood = JournalMood.STRESSED.name,
                marketRegime = MarketRegime.HIGH_VOL.name,
                followedPlan = false,
                demoRecord = true
            ),
            DailyJournalRecord(
                accountId = accountId,
                dateIso = DateUtils.epochToIsoDate(dayTime(5, 9, 0)),
                preMarketPlan = "Watch AMD weakness below VWAP.",
                postMarketReview = "First trade was fine; second was a pure revenge re-entry.",
                lessonsLearned = "Walk away for 20 minutes mandatory after any full -1R stop out.",
                mood = JournalMood.TILTED.name,
                marketRegime = MarketRegime.RANGE_BOUND.name,
                followedPlan = false,
                demoRecord = true
            )
        )

        return trades to journals
    }
}
