package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DateRangeFilter
import com.example.domain.FormatUtils
import com.example.domain.PerformanceSummary
import com.example.domain.SideSummary
import com.example.ui.components.DateRangeFilterStrip
import com.example.ui.components.HorizontalMetricBarRow
import com.example.ui.components.PnlStatusPill
import com.example.ui.components.QuantCard
import com.example.ui.components.RMultipleHistogramChart
import com.example.ui.components.UnderwaterDrawdownChart
import com.example.ui.state.EdgeLogUiState
import com.example.ui.theme.EdgeLogThemeDefaults
import com.example.ui.theme.MonoTextStyles
import kotlin.math.abs

@Composable
fun AnalyticsScreen(
    uiState: EdgeLogUiState,
    onSelectDateRange: (DateRangeFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val summary = uiState.summary
    var reportTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Timing", "Symbols", "Psych")

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxContentWidth = if (maxWidth >= 720.dp) 960.dp else 640.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("analytics_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item(key = "analytics_header") {
                Column(modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth)) {
                    Text("Performance Reports", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = "Deep breakdown of edge, drawdowns, timing, and behavioral leaks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item(key = "analytics_date_filter") {
                DateRangeFilterStrip(
                    selected = uiState.filters.dateRange,
                    onSelect = onSelectDateRange,
                    modifier = Modifier.widthIn(max = maxContentWidth)
                )
            }

            item(key = "analytics_subtabs") {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth)
                ) {
                    tabs.forEachIndexed { idx, label ->
                        SegmentedButton(
                            selected = reportTab == idx,
                            onClick = { reportTab = idx },
                            shape = SegmentedButtonDefaults.itemShape(idx, tabs.size),
                            modifier = Modifier.testTag("analytics_tab_${label.lowercase()}")
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                }
            }

            if (summary.closedTrades == 0) {
                item(key = "analytics_empty") {
                    QuantCard(modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.Analytics,
                                contentDescription = null,
                                tint = EdgeLogThemeDefaults.colors.textMuted,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text("No Closed Trades in Scope", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Log closed trades or widen the date range to unlock quantitative breakdown reports.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                when (reportTab) {
                    0 -> item(key = "tab_overview") {
                        AnalyticsOverviewSection(
                            summary = summary,
                            modifier = Modifier.widthIn(max = maxContentWidth)
                        )
                    }
                    1 -> item(key = "tab_timing") {
                        AnalyticsTimingSection(
                            summary = summary,
                            modifier = Modifier.widthIn(max = maxContentWidth)
                        )
                    }
                    2 -> item(key = "tab_symbols") {
                        AnalyticsSymbolsSection(
                            summary = summary,
                            modifier = Modifier.widthIn(max = maxContentWidth)
                        )
                    }
                    else -> item(key = "tab_psych") {
                        AnalyticsPsychologySection(
                            summary = summary,
                            modifier = Modifier.widthIn(max = maxContentWidth)
                        )
                    }
                }
            }

            item(key = "analytics_bottom") {
                Spacer(Modifier.height(72.dp))
            }
        }
    }
}

@Composable
private fun AnalyticsOverviewSection(
    summary: PerformanceSummary,
    modifier: Modifier = Modifier
) {
    val ext = EdgeLogThemeDefaults.colors
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Underwater Drawdown Card
        QuantCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Equity Drawdown (Underwater)", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Peak-to-trough capital decline over time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                PnlStatusPill(
                    text = "-${FormatUtils.formatPercent(summary.maxDrawdownPct)}",
                    isPositive = false
                )
            }
            Spacer(Modifier.height(12.dp))
            UnderwaterDrawdownChart(points = summary.equityCurve)
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Max DD: -${FormatUtils.formatCurrency(summary.maxDrawdownDollars)}",
                    style = MonoTextStyles.MicroBadge,
                    color = ext.loss
                )
                Text(
                    text = "Best Day: ${FormatUtils.formatCurrency(summary.bestDayPnl, signed = true, compact = true)}",
                    style = MonoTextStyles.MicroBadge,
                    color = ext.profit
                )
            }
        }

        // Long vs Short Head-to-Head
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SideComparisonCard(summary.longStats, Modifier.weight(1f))
            SideComparisonCard(summary.shortStats, Modifier.weight(1f))
        }

        // Hold Time & Streaks Diagnostic Card
        QuantCard(modifier = Modifier.fillMaxWidth()) {
            Text("Execution Behavior & Streaks", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DiagnosticStatCol("Avg Win Hold", FormatUtils.formatDurationMinutes(summary.avgWinHoldMinutes), ext.profit)
                DiagnosticStatCol("Avg Loss Hold", FormatUtils.formatDurationMinutes(summary.avgLossHoldMinutes), ext.loss)
                DiagnosticStatCol("Best Streak", "${summary.bestWinStreak}W", ext.profit)
                DiagnosticStatCol("Worst Streak", "${summary.worstLossStreak}L", ext.loss)
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = ext.cardBorder)
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DiagnosticStatCol("Largest Win", FormatUtils.formatCurrency(summary.largestWin, signed = true, compact = true), ext.profit)
                DiagnosticStatCol("Largest Loss", FormatUtils.formatCurrency(summary.largestLoss, compact = true), ext.loss)
                DiagnosticStatCol("Total Fees", FormatUtils.formatCurrency(summary.totalFees), MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // R-Multiple Distribution Histogram
        QuantCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("R-Multiple Distribution", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (summary.rTrackedCount > 0) {
                            "${summary.rTrackedCount} risk-tracked trades · Avg ${FormatUtils.formatRMultiple(summary.avgRMultiple ?: 0.0)}"
                        } else {
                            "Add Stop Loss prices when logging trades to track R-multiples"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (summary.rTrackedCount > 0) {
                    PnlStatusPill(
                        text = FormatUtils.formatRMultiple(summary.totalNetR),
                        isPositive = summary.totalNetR >= 0.0
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            RMultipleHistogramChart(buckets = summary.rDistribution)
        }
    }
}

@Composable
private fun SideComparisonCard(stats: SideSummary, modifier: Modifier = Modifier) {
    val ext = EdgeLogThemeDefaults.colors
    QuantCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stats.side.label.uppercase(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text("${stats.tradeCount} trd", style = MonoTextStyles.MicroBadge, color = ext.textMuted)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = FormatUtils.formatCurrency(stats.netPnl, signed = true, compact = true),
            style = MonoTextStyles.KpiValue,
            color = if (stats.netPnl >= 0) ext.profit else ext.loss
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Win ${FormatUtils.formatPercent(stats.winRatePct, decimals = 0)} · PF ${FormatUtils.formatRatio(stats.profitFactor)}",
            style = MonoTextStyles.MicroBadge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DiagnosticStatCol(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = EdgeLogThemeDefaults.colors.textMuted)
        Spacer(Modifier.height(3.dp))
        Text(value, style = MonoTextStyles.DataRow, color = valueColor)
    }
}

@Composable
private fun AnalyticsTimingSection(
    summary: PerformanceSummary,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Day of Week Performance
        QuantCard(modifier = Modifier.fillMaxWidth()) {
            Text("Performance by Day of Week", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Identify which weekdays produce consistent edge vs choppy losses",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            val maxAbsDow = summary.dayOfWeekBreakdown.maxOfOrNull { abs(it.netPnl) }?.coerceAtLeast(1.0) ?: 1.0
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                summary.dayOfWeekBreakdown.forEach { bucket ->
                    HorizontalMetricBarRow(
                        label = bucket.label,
                        secondaryText = "${bucket.tradeCount} trades (${bucket.wins}W/${bucket.losses}L)",
                        value = bucket.netPnl,
                        maxAbsValue = maxAbsDow,
                        winRatePct = if (bucket.tradeCount > 0) bucket.winRatePct else null
                    )
                }
            }
        }

        // Intraday Time-of-Day Session Performance
        QuantCard(modifier = Modifier.fillMaxWidth()) {
            Text("Performance by Market Session", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Net P&L and win rate segmented by entry time window",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            val activeSessions = summary.sessionBreakdown.filter { it.tradeCount > 0 }
            val maxAbsSess = activeSessions.maxOfOrNull { abs(it.netPnl) }?.coerceAtLeast(1.0) ?: 1.0
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                summary.sessionBreakdown.forEach { bucket ->
                    HorizontalMetricBarRow(
                        label = bucket.label,
                        secondaryText = "${bucket.tradeCount} trd · Avg ${FormatUtils.formatCurrency(bucket.avgPnl, signed = true, compact = true)}",
                        value = bucket.netPnl,
                        maxAbsValue = maxAbsSess,
                        winRatePct = if (bucket.tradeCount > 0) bucket.winRatePct else null
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsSymbolsSection(
    summary: PerformanceSummary,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Asset Class Breakdown
        QuantCard(modifier = Modifier.fillMaxWidth()) {
            Text("Asset Class Breakdown", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            val maxAbsAsset = summary.assetClassBreakdown.maxOfOrNull { abs(it.netPnl) }?.coerceAtLeast(1.0) ?: 1.0
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                summary.assetClassBreakdown.forEach { bucket ->
                    HorizontalMetricBarRow(
                        label = bucket.label,
                        secondaryText = "${bucket.tradeCount} trd · PF ${FormatUtils.formatRatio(bucket.profitFactor)}",
                        value = bucket.netPnl,
                        maxAbsValue = maxAbsAsset,
                        winRatePct = bucket.winRatePct
                    )
                }
            }
        }

        // Symbol P&L Leaderboard
        QuantCard(modifier = Modifier.fillMaxWidth()) {
            Text("Instrument Leaderboard", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Ranked from highest to lowest cumulative Net P&L",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            val maxAbsSym = summary.symbolBreakdown.maxOfOrNull { abs(it.netPnl) }?.coerceAtLeast(1.0) ?: 1.0
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                summary.symbolBreakdown.forEach { bucket ->
                    HorizontalMetricBarRow(
                        label = bucket.label,
                        secondaryText = "${bucket.tradeCount} trd (${bucket.wins}W/${bucket.losses}L) · PF ${FormatUtils.formatRatio(bucket.profitFactor)}",
                        value = bucket.netPnl,
                        maxAbsValue = maxAbsSym,
                        winRatePct = bucket.winRatePct
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalyticsPsychologySection(
    summary: PerformanceSummary,
    modifier: Modifier = Modifier
) {
    val ext = EdgeLogThemeDefaults.colors
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Mistake Cost Quantifier
        QuantCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = ext.warning)
                Column {
                    Text("Mistake Cost Quantifier", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "Exact dollar impact of tagged behavioral leaks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            if (summary.mistakeBreakdown.isEmpty()) {
                Surface(
                    color = ext.profitContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Zero mistake tags logged in this period. Tag execution errors like FOMO or Early Exit when logging trades to quantify their P&L impact.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ext.onProfitContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                val maxAbsMistake = summary.mistakeBreakdown.maxOfOrNull { abs(it.totalPnl) }?.coerceAtLeast(1.0) ?: 1.0
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    summary.mistakeBreakdown.forEach { m ->
                        HorizontalMetricBarRow(
                            label = m.tag,
                            secondaryText = "${m.tradeCount}x · Avg ${FormatUtils.formatCurrency(m.avgPnl, signed = true, compact = true)}",
                            value = m.totalPnl,
                            maxAbsValue = maxAbsMistake,
                            winRatePct = m.winRatePct
                        )
                    }
                }
            }
        }

        // Pre-Trade Mindset Performance
        QuantCard(modifier = Modifier.fillMaxWidth()) {
            Text("Mindset vs Performance", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Correlating emotional state before entry with realized outcome",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            val maxAbsEmo = summary.emotionBreakdown.maxOfOrNull { abs(it.netPnl) }?.coerceAtLeast(1.0) ?: 1.0
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                summary.emotionBreakdown.forEach { bucket ->
                    HorizontalMetricBarRow(
                        label = bucket.label,
                        secondaryText = "${bucket.tradeCount} trd · Avg ${FormatUtils.formatCurrency(bucket.avgPnl, signed = true, compact = true)}",
                        value = bucket.netPnl,
                        maxAbsValue = maxAbsEmo,
                        winRatePct = bucket.winRatePct
                    )
                }
            }
        }
    }
}
