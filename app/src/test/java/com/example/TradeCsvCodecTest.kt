package com.example

import com.example.data.csv.TradeCsvCodec
import com.example.data.model.AssetClass
import com.example.data.model.TradeSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TradeCsvCodecTest {

    @Test
    fun `parseCsv and exportToCsv round-trip trades accurately including quoted commas`() {
        val parsed = TradeCsvCodec.parseCsv(TradeCsvCodec.SAMPLE_CSV_SNIPPET, accountId = "acc_7")
        assertEquals(2, parsed.trades.size)
        assertEquals(0, parsed.skippedRows)

        val nvda = parsed.trades[0]
        assertEquals("NVDA", nvda.symbol)
        assertEquals(TradeSide.LONG, nvda.sideEnum)
        assertEquals(AssetClass.STOCK, nvda.assetClassEnum)
        assertEquals(138.40, nvda.entryPrice, 0.001)
        assertEquals(143.90, nvda.exitPrice!!, 0.001)
        assertEquals("acc_7", nvda.accountId)

        val es = parsed.trades[1]
        assertEquals("ES", es.symbol)
        assertEquals(TradeSide.SHORT, es.sideEnum)
        assertEquals(50.0, es.contractMultiplier, 0.001)

        val exported = TradeCsvCodec.exportToCsv(parsed.trades)
        val reparsed = TradeCsvCodec.parseCsv(exported, accountId = "acc_7")
        assertEquals(2, reparsed.trades.size)
        assertEquals(nvda.netPnl, reparsed.trades[0].netPnl, 0.01)
        assertEquals(es.netPnl, reparsed.trades[1].netPnl, 0.01)
    }

    @Test
    fun `parseCsv rejects missing required columns gracefully`() {
        val badCsv = "Ticker,Foo\nNVDA,Bar"
        val result = TradeCsvCodec.parseCsv(badCsv, accountId = "acc_1")
        assertTrue(result.trades.isEmpty())
        assertTrue(result.errors.isNotEmpty())
    }
}
