package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DateRangeFilter
import com.example.domain.EdgeScoreBreakdown
import com.example.domain.FormatUtils
import com.example.domain.PerformanceSummary
import com.example.ui.components.CumulativeEquityCurveChart
import com.example.ui.components.DailyPnlBarChart
import com.example.ui.components.DateRangeFilterStrip
import com.example.ui.components.EdgeScoreRadarChart
import com.example.ui.components.EmptyJournalHeroCard
import com.example.ui.components.PnlStatusPill
import com.example.ui.components.QuantCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.TradeListItemCard
import com.example.ui.components.WinRateDonutGauge
import com.example.ui.state.EdgeLogUiState
import com.example.ui.theme.EdgeLogThemeDefaults
import com.example.ui.theme.MonoTextStyles
import kotlin.math.abs

@Composable
fun DashboardScreen(
    uiState: EdgeLogUiState,
    onSelectDateRange: (DateRangeFilter) -> Unit,
    onOpenAccountsAndTools: () -> Unit,
    onLogTrade: () -> Unit,
    onImportCsv: () -> Unit,
    onLoadDemo: () -> Unit,
    onRetrySync: () -> Unit,
    onOpenTradeDetail: (String) -> Unit,
    onViewAllTrades: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summary = uiState.summary
    val playbookMap = remember(uiState.playbooks) { uiState.playbooks.associate { it.id to it.name } }
    var chartTabIndex by remember { mutableIntStateOf(0) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 720.dp
        val maxContentWidth = if (isWide) 960.dp else 640.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("dashboard_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Header
            item(key = "header") {
                DashboardHeaderBar(
                    accountLabel = uiState.activeAccountLabel,
                    userEmail = uiState.userEmail,
                    hasDemoData = uiState.hasSampleData,
                    hasSyncError = uiState.syncErrorMessage != null,
                    onOpenAccountsAndTools = onOpenAccountsAndTools,
                    modifier = Modifier.widthIn(max = maxContentWidth)
                )
            }

            if (uiState.syncErrorMessage != null) {
                item(key = "sync_error") {
                    val ext = EdgeLogThemeDefaults.colors
                    QuantCard(modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CloudOff,
                                contentDescription = null,
                                tint = ext.loss
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Cloud Sync Issue", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = uiState.syncErrorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(onClick = onRetrySync) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }

            // Date Range Filter Strip
            item(key = "date_filters") {
                DateRangeFilterStrip(
                    selected = uiState.filters.dateRange,
                    onSelect = onSelectDateRange,
                    modifier = Modifier.widthIn(max = maxContentWidth)
                )
            }

            if (uiState.allAccountTrades.isEmpty()) {
                item(key = "empty_hero") {
                    EmptyJournalHeroCard(
                        onLogTrade = onLogTrade,
                        onImportCsv = onImportCsv,
                        onLoadDemo = onLoadDemo,
                        modifier = Modifier.widthIn(max = maxContentWidth)
                    )
                }
            } else {
                // Hero Net P&L & Equity Card
                item(key = "hero_pnl") {
                    HeroNetPnlCard(
                        summary = summary,
                        dateRangeLabel = uiState.filters.dateRange.label,
                        modifier = Modifier.widthIn(max = maxContentWidth)
                    )
                }

                // 2x2 KPI Bento Grid
                item(key = "kpi_grid") {
                    DashboardKpiGrid(
                        summary = summary,
                        isWide = isWide,
                        modifier = Modifier.widthIn(max = maxContentWidth)
                    )
                }

                // Equity Curve / Daily P&L Chart Card
                item(key = "equity_chart") {
                    QuantCard(modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (chartTabIndex == 0) {
                                        stringResource(R.string.metric_equity_curve)
                                    } else {
                                        stringResource(R.string.metric_daily_pnl)
                                    },
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${summary.tradingDays} active trading days",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            SingleChoiceSegmentedButtonRow {
                                SegmentedButton(
                                    selected = chartTabIndex == 0,
                                    onClick = { chartTabIndex = 0 },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                    modifier = Modifier.testTag("chart_tab_equity")
                                ) {
                                    Text("Equity", style = MaterialTheme.typography.labelSmall)
                                }
                                SegmentedButton(
                                    selected = chartTabIndex == 1,
                                    onClick = { chartTabIndex = 1 },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                    modifier = Modifier.testTag("chart_tab_daily")
                                ) {
                                    Text("Daily", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        if (chartTabIndex == 0) {
                            CumulativeEquityCurveChart(points = summary.equityCurve)
                        } else {
                            DailyPnlBarChart(points = summary.equityCurve)
                        }
                    }
                }

                // Edge Score Radar Card
                item(key = "edge_score") {
                    EdgeScoreCard(
                        score = summary.edgeScore,
                        summary = summary,
                        modifier = Modifier.widthIn(max = maxContentWidth)
                    )
                }

                // Recent Trades Section
                item(key = "recent_header") {
                    SectionHeader(
                        title = stringResource(R.string.metric_recent_trades),
                        subtitle = "Showing latest executions in range",
                        trailing = {
                            TextButton(
                                onClick = onViewAllTrades,
                                modifier = Modifier.testTag("dashboard_view_all_trades")
                            ) {
                                Text("View All")
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    )
                }

                val recent = uiState.scopedTrades.take(5)
                if (recent.isEmpty()) {
                    item(key = "no_recent_in_range") {
                        QuantCard(modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth)) {
                            Text(
                                text = "No trades logged inside the ${uiState.filters.dateRange.label} window.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(recent, key = { it.id }) { trade ->
                        TradeListItemCard(
                            trade = trade,
                            playbookName = trade.playbookId?.let { playbookMap[it] },
                            onClick = { onOpenTradeDetail(trade.id) },
                            modifier = Modifier.widthIn(max = maxContentWidth)
                        )
                    }
                }
            }

            item(key = "bottom_spacer") {
                Spacer(Modifier.height(72.dp))
            }
        }
    }
}

@Composable
private fun DashboardHeaderBar(
    accountLabel: String,
    userEmail: String?,
    hasDemoData: Boolean,
    hasSyncError: Boolean,
    onOpenAccountsAndTools: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = EdgeLogThemeDefaults.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = if (hasSyncError) ext.lossContainer else ext.profitContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = if (hasSyncError) Icons.Outlined.CloudOff else Icons.Outlined.CloudDone,
                            contentDescription = if (hasSyncError) "Sync paused" else "Cloud synced",
                            tint = if (hasSyncError) ext.loss else ext.profit,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (hasSyncError) "OFFLINE" else "SYNCED",
                            style = MonoTextStyles.CalendarCell,
                            color = if (hasSyncError) ext.loss else ext.profit
                        )
                    }
                }
                if (hasDemoData) {
                    Surface(
                        color = ext.accentIndigoContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "DEMO",
                            style = MonoTextStyles.CalendarCell,
                            color = ext.accentIndigo,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenAccountsAndTools)
                    .padding(vertical = 2.dp)
                    .testTag("account_selector_pill")
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = if (!userEmail.isNullOrBlank()) "$accountLabel · $userEmail" else accountLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        IconButton(
            onClick = onOpenAccountsAndTools,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .testTag("open_tools_button")
        ) {
            Icon(
                imageVector = Icons.Outlined.Tune,
                contentDescription = stringResource(R.string.cd_accounts_settings),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun HeroNetPnlCard(
    summary: PerformanceSummary,
    dateRangeLabel: String,
    modifier: Modifier = Modifier
) {
    val ext = EdgeLogThemeDefaults.colors
    val isPositive = summary.netPnl >= 0.0
    val accent = if (isPositive) ext.profit else ext.loss

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            accent.copy(alpha = if (ext.isDark) 0.14f else 0.08f),
                            MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${stringResource(R.string.metric_net_pnl)} ($dateRangeLabel)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PnlStatusPill(
                        text = FormatUtils.formatPercent(summary.returnOnEquityPct, signed = true),
                        isPositive = isPositive
                    )
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = FormatUtils.formatCurrency(summary.netPnl, signed = true),
                    style = MonoTextStyles.HeroMetric,
                    color = accent
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.metric_account_equity),
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.textMuted
                        )
                        Text(
                            text = FormatUtils.formatCurrency(summary.currentEquity),
                            style = MonoTextStyles.CardMetric,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PnlStatusPill(
                            text = "${summary.wins}W · ${summary.losses}L",
                            isPositive = null
                        )
                        if (summary.currentStreak != 0) {
                            val winStreak = summary.currentStreak > 0
                            PnlStatusPill(
                                text = if (winStreak) "${summary.currentStreak}W Strk" else "${abs(summary.currentStreak)}L Strk",
                                isPositive = winStreak
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardKpiGrid(
    summary: PerformanceSummary,
    isWide: Boolean,
    modifier: Modifier = Modifier
) {
    val ext = EdgeLogThemeDefaults.colors

    @Composable
    fun WinRateCard(cardMod: Modifier) {
        QuantCard(modifier = cardMod) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.metric_win_rate),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = FormatUtils.formatPercent(summary.tradeWinRatePct),
                        style = MonoTextStyles.KpiValue,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Day Win ${FormatUtils.formatPercent(summary.dayWinRatePct, decimals = 0)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.textMuted
                    )
                }
                WinRateDonutGauge(
                    winRatePct = summary.tradeWinRatePct,
                    wins = summary.wins,
                    losses = summary.losses,
                    gaugeSize = 54.dp
                )
            }
        }
    }

    @Composable
    fun ProfitFactorCard(cardMod: Modifier) {
        QuantCard(modifier = cardMod) {
            Text(
                text = stringResource(R.string.metric_profit_factor),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = FormatUtils.formatRatio(summary.profitFactor),
                style = MonoTextStyles.KpiValue,
                color = if (summary.profitFactor >= 1.5 || summary.profitFactor.isInfinite()) {
                    ext.profit
                } else if (summary.profitFactor < 1.0) {
                    ext.loss
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Spacer(Modifier.height(8.dp))
            val totalGross = (summary.grossProfit + abs(summary.grossLoss)).coerceAtLeast(1.0)
            val winFraction = (summary.grossProfit / totalGross).toFloat().coerceIn(0f, 1f)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(ext.loss.copy(alpha = 0.75f))
            ) {
                if (winFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(winFraction)
                            .height(6.dp)
                            .background(ext.profit)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "PF > 1.50 = strong edge",
                style = MaterialTheme.typography.labelSmall,
                color = ext.textMuted,
                maxLines = 1
            )
        }
    }

    @Composable
    fun AvgWinLossCard(cardMod: Modifier) {
        QuantCard(modifier = cardMod) {
            Text(
                text = stringResource(R.string.metric_avg_win_loss),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${FormatUtils.formatRatio(summary.avgWinLossRatio)}x",
                style = MonoTextStyles.KpiValue,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = FormatUtils.formatCurrency(summary.avgWin, signed = true, compact = true),
                    style = MonoTextStyles.MicroBadge,
                    color = ext.profit
                )
                Text(
                    text = FormatUtils.formatCurrency(-summary.avgLoss, compact = true),
                    style = MonoTextStyles.MicroBadge,
                    color = ext.loss
                )
            }
        }
    }

    @Composable
    fun ExpectancyCard(cardMod: Modifier) {
        QuantCard(modifier = cardMod) {
            Text(
                text = stringResource(R.string.metric_expectancy),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = FormatUtils.formatCurrency(summary.expectancy, signed = true, compact = true),
                style = MonoTextStyles.KpiValue,
                color = if (summary.expectancy >= 0) ext.profit else ext.loss
            )
            Spacer(Modifier.height(6.dp))
            val rText = if (summary.rTrackedCount > 0) {
                "Net ${FormatUtils.formatRMultiple(summary.totalNetR)}"
            } else {
                "Max DD ${FormatUtils.formatPercent(summary.maxDrawdownPct)}"
            }
            Text(
                text = rText,
                style = MonoTextStyles.MicroBadge,
                color = ext.accentIndigo
            )
        }
    }

    if (isWide) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WinRateCard(Modifier.weight(1f))
            ProfitFactorCard(Modifier.weight(1f))
            AvgWinLossCard(Modifier.weight(1f))
            ExpectancyCard(Modifier.weight(1f))
        }
    } else {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WinRateCard(Modifier.weight(1f))
                ProfitFactorCard(Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AvgWinLossCard(Modifier.weight(1f))
                ExpectancyCard(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EdgeScoreCard(
    score: EdgeScoreBreakdown,
    summary: PerformanceSummary,
    modifier: Modifier = Modifier
) {
    val ext = EdgeLogThemeDefaults.colors
    QuantCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.metric_edge_score),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "5-pillar composite consistency rating",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                color = if (score.overall >= 70) ext.profitContainer else ext.accentIndigoContainer,
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${score.overall}",
                        style = MonoTextStyles.CardMetric,
                        color = if (score.overall >= 70) ext.profit else ext.accentIndigo
                    )
                    Text(
                        text = score.grade,
                        style = MonoTextStyles.MicroBadge,
                        fontWeight = FontWeight.Bold,
                        color = if (score.overall >= 70) ext.onProfitContainer else ext.accentIndigo
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            EdgeScoreRadarChart(score = score, sizeDp = 148.dp)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScorePillarBar("Win Rate", score.winRateScore)
                ScorePillarBar("Profit Factor", score.profitFactorScore)
                ScorePillarBar("Payoff Ratio", score.payoffRatioScore)
                ScorePillarBar("Consistency", score.consistencyScore)
                ScorePillarBar("Discipline", score.disciplineScore)
            }
        }

        if (summary.maxDrawdownDollars > 0.0 || summary.totalFees > 0.0) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Max DD: -${FormatUtils.formatCurrency(summary.maxDrawdownDollars, compact = true)} (${FormatUtils.formatPercent(summary.maxDrawdownPct)})",
                    style = MonoTextStyles.MicroBadge,
                    color = ext.loss
                )
                Text(
                    text = "Fees: ${FormatUtils.formatCurrency(summary.totalFees)}",
                    style = MonoTextStyles.MicroBadge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ScorePillarBar(label: String, value: Float) {
    val ext = EdgeLogThemeDefaults.colors
    val color = when {
        value >= 75f -> ext.profit
        value >= 50f -> ext.accentIndigo
        else -> ext.warning
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${value.toInt()}", style = MonoTextStyles.CalendarCell, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((value / 100f).coerceIn(0.05f, 1f))
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
    }
}
