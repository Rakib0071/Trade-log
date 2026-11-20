package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DailyJournalRecord
import com.example.data.model.JournalMood
import com.example.data.model.MarketRegime
import com.example.domain.DailySummary
import com.example.domain.DateUtils
import com.example.domain.FormatUtils
import com.example.domain.YearMonthKey
import com.example.ui.components.PnlStatusPill
import com.example.ui.components.QuantCard
import com.example.ui.components.TradeListItemCard
import com.example.ui.state.EdgeLogUiState
import com.example.ui.theme.EdgeLogThemeDefaults
import com.example.ui.theme.MonoTextStyles
import kotlin.math.abs

@Composable
fun CalendarJournalScreen(
    uiState: EdgeLogUiState,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (String) -> Unit,
    onSaveJournal: (
        dateIso: String,
        preMarket: String,
        postMarket: String,
        lessons: String,
        mood: JournalMood,
        regime: MarketRegime,
        followedPlan: Boolean
    ) -> Unit,
    onOpenTradeDetail: (String) -> Unit,
    onLogTradeForDate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val month = uiState.filters.calendarMonth
    val selectedDate = uiState.filters.selectedDateIso
    val dailyMap = uiState.calendarDailyMap
    val playbookMap = remember(uiState.playbooks) { uiState.playbooks.associate { it.id to it.name } }

    // Month summary metrics
    val monthPrefix = String.format(java.util.Locale.US, "%04d-%02d-", month.year, month.month)
    val monthDays = remember(dailyMap, monthPrefix) {
        dailyMap.entries.filter { it.key.startsWith(monthPrefix) }.map { it.value }
    }
    val monthNetPnl = monthDays.sumOf { it.netPnl }
    val monthWinDays = monthDays.count { it.netPnl > 0.01 }
    val monthLossDays = monthDays.count { it.netPnl < -0.01 }
    val monthDayWinPct = if (monthWinDays + monthLossDays > 0) {
        (monthWinDays.toDouble() / (monthWinDays + monthLossDays)) * 100.0
    } else 0.0

    val selectedDayTrades = remember(uiState.allAccountTrades, selectedDate) {
        uiState.tradesForDate(selectedDate)
    }
    val selectedDaySummary = dailyMap[selectedDate]
    val existingJournal = uiState.journals[selectedDate]

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxContentWidth = if (maxWidth >= 720.dp) 960.dp else 640.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("calendar_journal_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item(key = "cal_header") {
                Column(modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth)) {
                    Text(
                        text = "P&L Calendar & Daily Journal",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap any trading day to inspect executions & write your session review",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Month Navigation & Heatmap Card
            item(key = "calendar_card") {
                MonthlyPnlCalendarCard(
                    month = month,
                    selectedDateIso = selectedDate,
                    dailyMap = dailyMap,
                    journalsMap = uiState.journals,
                    monthNetPnl = monthNetPnl,
                    monthTradingDays = monthDays.size,
                    monthDayWinPct = monthDayWinPct,
                    onPrevMonth = onPrevMonth,
                    onNextMonth = onNextMonth,
                    onSelectDate = onSelectDate,
                    modifier = Modifier.widthIn(max = maxContentWidth)
                )
            }

            // Selected Day Summary + Executions
            item(key = "selected_day_trades") {
                QuantCard(modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = DateUtils.formatIsoDisplay(selectedDate),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = if (selectedDaySummary != null) {
                                    "${selectedDaySummary.tradeCount} trade${if (selectedDaySummary.tradeCount == 1) "" else "s"} · ${selectedDaySummary.wins}W ${selectedDaySummary.losses}L"
                                } else {
                                    "No closed executions logged"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (selectedDaySummary != null) {
                            PnlStatusPill(
                                text = FormatUtils.formatCurrency(selectedDaySummary.netPnl, signed = true),
                                isPositive = if (abs(selectedDaySummary.netPnl) < 0.01) null else selectedDaySummary.netPnl > 0
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    if (selectedDayTrades.isEmpty()) {
                        OutlinedButton(
                            onClick = { onLogTradeForDate(selectedDate) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("calendar_log_trade_for_day")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Log Trade for ${DateUtils.formatIsoDisplay(selectedDate)}")
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            selectedDayTrades.forEach { trade ->
                                TradeListItemCard(
                                    trade = trade,
                                    playbookName = trade.playbookId?.let { playbookMap[it] },
                                    onClick = { onOpenTradeDetail(trade.id) }
                                )
                            }
                            OutlinedButton(
                                onClick = { onLogTradeForDate(selectedDate) },
                                modifier = Modifier.fillMaxWidth().height(44.dp)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Add Another Execution")
                            }
                        }
                    }
                }
            }

            // Daily Session Review Editor Card
            item(key = "daily_journal_editor") {
                DailyJournalEditorCard(
                    dateIso = selectedDate,
                    existingJournal = existingJournal,
                    onSave = onSaveJournal,
                    modifier = Modifier.widthIn(max = maxContentWidth)
                )
            }

            item(key = "cal_bottom_spacer") {
                Spacer(Modifier.height(72.dp))
            }
        }
    }
}

@Composable
private fun MonthlyPnlCalendarCard(
    month: YearMonthKey,
    selectedDateIso: String,
    dailyMap: Map<String, DailySummary>,
    journalsMap: Map<String, DailyJournalRecord>,
    monthNetPnl: Double,
    monthTradingDays: Int,
    monthDayWinPct: Double,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = EdgeLogThemeDefaults.colors
    val daysInMonth = month.daysInMonth()
    val leadingBlanks = month.firstDayOfWeekMondayBased()
    val totalCells = leadingBlanks + daysInMonth
    val weeksCount = (totalCells + 6) / 7
    val todayIso = remember { DateUtils.todayIso() }

    val maxAbsMonthDayPnl = remember(month, dailyMap) {
        (1..daysInMonth).mapNotNull { d -> dailyMap[month.isoDateForDay(d)]?.netPnl }
            .maxOfOrNull { abs(it) }
            ?.coerceAtLeast(100.0) ?: 100.0
    }

    QuantCard(modifier = modifier.fillMaxWidth()) {
        // Month switcher + Monthly Net P&L
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onPrevMonth,
                    modifier = Modifier.size(48.dp).testTag("calendar_prev_month")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.cd_prev_month)
                    )
                }
                Text(
                    text = month.displayTitle(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier.size(48.dp).testTag("calendar_next_month")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.cd_next_month)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = FormatUtils.formatCurrency(monthNetPnl, signed = true),
                    style = MonoTextStyles.CardMetric,
                    color = when {
                        monthNetPnl > 0.01 -> ext.profit
                        monthNetPnl < -0.01 -> ext.loss
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = "$monthTradingDays days · ${FormatUtils.formatPercent(monthDayWinPct, decimals = 0)} win",
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.textMuted
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Day-of-week header
        val dowLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            dowLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.textMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Calendar grid rows + collect weekly totals
        val weeklyTotals = mutableListOf<Double>()
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (week in 0 until weeksCount) {
                var weekSum = 0.0
                var weekHasTrades = false
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (col in 0 until 7) {
                        val cellIndex = week * 7 + col
                        val dayNum = cellIndex - leadingBlanks + 1
                        if (dayNum in 1..daysInMonth) {
                            val dateIso = month.isoDateForDay(dayNum)
                            val daySummary = dailyMap[dateIso]
                            if (daySummary != null) {
                                weekSum += daySummary.netPnl
                                weekHasTrades = true
                            }
                            val hasJournal = journalsMap.containsKey(dateIso)
                            val isSelected = dateIso == selectedDateIso
                            val isToday = dateIso == todayIso

                            CalendarDayCell(
                                dayNum = dayNum,
                                summary = daySummary,
                                maxAbsMonthPnl = maxAbsMonthDayPnl,
                                hasJournal = hasJournal,
                                isSelected = isSelected,
                                isToday = isToday,
                                onClick = { onSelectDate(dateIso) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(0.92f))
                        }
                    }
                }
                weeklyTotals.add(if (weekHasTrades) weekSum else Double.NaN)
            }
        }

        // Weekly Totals Strip
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            weeklyTotals.forEachIndexed { idx, total ->
                val hasData = !total.isNaN()
                val pillBg = when {
                    !hasData -> MaterialTheme.colorScheme.surfaceContainer
                    total > 0.01 -> ext.profitContainer
                    total < -0.01 -> ext.lossContainer
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                }
                val pillFg = when {
                    !hasData -> ext.textMuted
                    total > 0.01 -> ext.profit
                    total < -0.01 -> ext.loss
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Surface(
                    color = pillBg,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ext.cardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("W${idx + 1}", style = MaterialTheme.typography.labelSmall, color = ext.textMuted)
                        Text(
                            text = if (hasData) FormatUtils.formatCurrency(total, signed = true, compact = true) else "—",
                            style = MonoTextStyles.MicroBadge,
                            color = pillFg
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    dayNum: Int,
    summary: DailySummary?,
    maxAbsMonthPnl: Double,
    hasJournal: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = EdgeLogThemeDefaults.colors
    val intensity = if (summary != null) {
        (0.20f + 0.55f * (abs(summary.netPnl) / maxAbsMonthPnl).toFloat().coerceIn(0f, 1f))
    } else 0f

    val bgColor = when {
        summary == null -> MaterialTheme.colorScheme.surfaceContainer
        summary.netPnl > 0.01 -> ext.profit.copy(alpha = if (ext.isDark) intensity * 0.42f else intensity * 0.28f)
        summary.netPnl < -0.01 -> ext.loss.copy(alpha = if (ext.isDark) intensity * 0.45f else intensity * 0.28f)
        else -> ext.warning.copy(alpha = 0.20f)
    }

    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> ext.accentIndigo
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .aspectRatio(0.92f)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 2.dp else if (isToday) 1.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 3.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dayNum.toString(),
                    style = MonoTextStyles.CalendarCell,
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (hasJournal) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(ext.accentIndigo)
                    )
                }
            }

            if (summary != null) {
                Text(
                    text = FormatUtils.formatCurrency(summary.netPnl, signed = true, compact = true),
                    style = MonoTextStyles.CalendarCell,
                    color = when {
                        summary.netPnl > 0.01 -> ext.profit
                        summary.netPnl < -0.01 -> ext.loss
                        else -> ext.warning
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
                Text(
                    text = "${summary.tradeCount}t",
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.textMuted,
                    maxLines = 1
                )
            } else {
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun DailyJournalEditorCard(
    dateIso: String,
    existingJournal: DailyJournalRecord?,
    onSave: (
        dateIso: String,
        preMarket: String,
        postMarket: String,
        lessons: String,
        mood: JournalMood,
        regime: MarketRegime,
        followedPlan: Boolean
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var preMarket by remember(dateIso, existingJournal) { mutableStateOf(existingJournal?.preMarketPlan.orEmpty()) }
    var postMarket by remember(dateIso, existingJournal) { mutableStateOf(existingJournal?.postMarketReview.orEmpty()) }
    var lessons by remember(dateIso, existingJournal) { mutableStateOf(existingJournal?.lessonsLearned.orEmpty()) }
    var mood by remember(dateIso, existingJournal) { mutableStateOf(existingJournal?.moodEnum ?: JournalMood.FOCUSED) }
    var regime by remember(dateIso, existingJournal) { mutableStateOf(existingJournal?.marketRegimeEnum ?: MarketRegime.TRENDING_UP) }
    var followedPlan by remember(dateIso, existingJournal) { mutableStateOf(existingJournal?.followedPlan ?: true) }

    LaunchedEffect(dateIso, existingJournal) {
        preMarket = existingJournal?.preMarketPlan.orEmpty()
        postMarket = existingJournal?.postMarketReview.orEmpty()
        lessons = existingJournal?.lessonsLearned.orEmpty()
        mood = existingJournal?.moodEnum ?: JournalMood.FOCUSED
        regime = existingJournal?.marketRegimeEnum ?: MarketRegime.TRENDING_UP
        followedPlan = existingJournal?.followedPlan ?: true
    }

    QuantCard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                imageVector = Icons.Outlined.EditNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text("Daily Session Review", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = DateUtils.formatIsoDisplay(dateIso),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text("Session Mood", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            JournalMood.entries.forEach { item ->
                FilterChip(
                    selected = mood == item,
                    onClick = { mood = item },
                    label = { Text(item.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Text("Market Regime", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MarketRegime.entries.forEach { item ->
                FilterChip(
                    selected = regime == item,
                    onClick = { regime = item },
                    label = { Text(item.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Followed Trading Plan", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Respected risk limits and entry criteria",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = followedPlan,
                onCheckedChange = { followedPlan = it },
                modifier = Modifier.testTag("journal_followed_plan_switch")
            )
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = preMarket,
            onValueChange = { preMarket = it },
            label = { Text("Pre-Market Plan & Key Levels") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth().testTag("journal_premarket_input")
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = postMarket,
            onValueChange = { postMarket = it },
            label = { Text("Post-Market Execution Review") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth().testTag("journal_postmarket_input")
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = lessons,
            onValueChange = { lessons = it },
            label = { Text("Key Lesson / Adjustment for Tomorrow") },
            minLines = 2,
            maxLines = 3,
            modifier = Modifier.fillMaxWidth().testTag("journal_lessons_input")
        )

        Spacer(Modifier.height(14.dp))
        Button(
            onClick = {
                onSave(dateIso, preMarket, postMarket, lessons, mood, regime, followedPlan)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_daily_journal_button")
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Save Session Review")
        }
    }
}
