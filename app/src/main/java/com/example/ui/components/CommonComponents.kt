package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DateRangeFilter
import com.example.data.model.TradeOutcome
import com.example.data.model.TradeRecord
import com.example.data.model.TradeSide
import com.example.domain.DateUtils
import com.example.domain.FormatUtils
import com.example.ui.theme.EdgeLogThemeDefaults
import com.example.ui.theme.MonoTextStyles
import kotlin.math.abs

@Composable
fun QuantCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val ext = EdgeLogThemeDefaults.colors
    val cardModifier = if (onClick != null) {
        modifier.clip(MaterialTheme.shapes.medium).clickable(onClick = onClick)
    } else {
        modifier
    }
    Card(
        modifier = cardModifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, ext.cardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun DateRangeFilterStrip(
    selected: DateRangeFilter,
    onSelect: (DateRangeFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DateRangeFilter.entries.forEach { range ->
            val isSelected = range == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(range) },
                label = {
                    Text(
                        text = range.label,
                        style = MonoTextStyles.MicroBadge
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.testTag("date_range_${range.name.lowercase()}")
            )
        }
    }
}

@Composable
fun PnlStatusPill(
    text: String,
    isPositive: Boolean?,
    modifier: Modifier = Modifier
) {
    val ext = EdgeLogThemeDefaults.colors
    val bg = when (isPositive) {
        true -> ext.profitContainer
        false -> ext.lossContainer
        null -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val fg = when (isPositive) {
        true -> ext.profit
        false -> ext.loss
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            style = MonoTextStyles.MicroBadge,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            maxLines = 1
        )
    }
}

@Composable
fun TradeListItemCard(
    trade: TradeRecord,
    playbookName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = EdgeLogThemeDefaults.colors
    val pnlColor = when (trade.outcome) {
        TradeOutcome.WIN -> ext.profit
        TradeOutcome.LOSS -> ext.loss
        TradeOutcome.BREAKEVEN -> ext.warning
        TradeOutcome.OPEN -> ext.accentIndigo
    }
    val sideBg = if (trade.sideEnum == TradeSide.LONG) ext.profitContainer else ext.lossContainer
    val sideFg = if (trade.sideEnum == TradeSide.LONG) ext.profit else ext.loss

    QuantCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("trade_card_${trade.id}"),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Left: Symbol + Side + Asset + Timestamp
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(sideBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (trade.sideEnum == TradeSide.LONG) {
                            Icons.AutoMirrored.Filled.TrendingUp
                        } else {
                            Icons.AutoMirrored.Filled.TrendingDown
                        },
                        contentDescription = trade.sideEnum.label,
                        tint = sideFg,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = trade.symbol,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            color = sideBg,
                            shape = RoundedCornerShape(5.dp)
                        ) {
                            Text(
                                text = trade.sideEnum.label.uppercase(),
                                style = MonoTextStyles.CalendarCell,
                                color = sideFg,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(5.dp)
                        ) {
                            Text(
                                text = trade.assetClassEnum.shortLabel,
                                style = MonoTextStyles.CalendarCell,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                        if (trade.demoRecord) {
                            Surface(
                                color = ext.accentIndigoContainer,
                                shape = RoundedCornerShape(5.dp)
                            ) {
                                Text(
                                    text = "DEMO",
                                    style = MonoTextStyles.CalendarCell,
                                    color = ext.accentIndigo,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = buildString {
                            append(DateUtils.formatDateTimeShort(trade.entryTimeEpochMs))
                            append(" · Qty ${FormatUtils.formatQuantity(trade.quantity)}")
                            if (trade.isClosed) {
                                append(" · ${FormatUtils.formatDurationMinutes(trade.holdingDurationMinutes)}")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right: Net P&L + R-Multiple
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (trade.isClosed) {
                        FormatUtils.formatCurrency(trade.netPnl, signed = true)
                    } else {
                        "OPEN"
                    },
                    style = MonoTextStyles.CardMetric,
                    color = pnlColor
                )
                Spacer(Modifier.height(3.dp))
                val rMult = trade.rMultiple
                if (rMult != null) {
                    Text(
                        text = FormatUtils.formatRMultiple(rMult),
                        style = MonoTextStyles.MicroBadge,
                        color = if (rMult >= 0) ext.profit else ext.loss
                    )
                } else if (trade.isClosed) {
                    Text(
                        text = FormatUtils.formatPercent(trade.returnPct, signed = true),
                        style = MonoTextStyles.MicroBadge,
                        color = pnlColor
                    )
                } else {
                    Text(
                        text = "@ ${FormatUtils.formatPrice(trade.entryPrice)}",
                        style = MonoTextStyles.MicroBadge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Price execution & Playbook / Mistakes footer row
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (trade.exitPrice != null) {
                    "${FormatUtils.formatPrice(trade.entryPrice)} → ${FormatUtils.formatPrice(trade.exitPrice)}"
                } else {
                    "Entry ${FormatUtils.formatPrice(trade.entryPrice)}"
                },
                style = MonoTextStyles.MicroBadge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (playbookName != null) {
                    Surface(
                        color = ext.accentIndigoContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = playbookName,
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.accentIndigo,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                val firstMistake = trade.mistakeTags.firstOrNull()
                if (firstMistake != null) {
                    Surface(
                        color = ext.warningContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (trade.mistakeTags.size > 1) "$firstMistake +${trade.mistakeTags.size - 1}" else firstMistake,
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.warning,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = ext.warning,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = "${trade.rating}",
                        style = MonoTextStyles.MicroBadge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun HorizontalMetricBarRow(
    label: String,
    secondaryText: String,
    value: Double,
    maxAbsValue: Double,
    winRatePct: Double?,
    modifier: Modifier = Modifier
) {
    val ext = EdgeLogThemeDefaults.colors
    val isPos = value >= 0.0
    val barColor = if (isPos) ext.profit else ext.loss
    val fraction = (abs(value) / maxAbsValue.coerceAtLeast(1.0)).toFloat().coerceIn(0.04f, 1f)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = secondaryText,
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.textMuted
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (winRatePct != null) {
                    Text(
                        text = FormatUtils.formatPercent(winRatePct, decimals = 0),
                        style = MonoTextStyles.MicroBadge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = FormatUtils.formatCurrency(value, signed = true, compact = true),
                    style = MonoTextStyles.DataRow,
                    color = barColor
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
fun EmptyJournalHeroCard(
    onLogTrade: () -> Unit,
    onImportCsv: () -> Unit,
    onLoadDemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = EdgeLogThemeDefaults.colors
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, ext.cardBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_journal_hero),
                    contentDescription = stringResource(R.string.cd_hero_banner),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.88f)
                                )
                            )
                        )
                )
                Surface(
                    color = ext.profitContainer.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(14.dp)
                ) {
                    Text(
                        text = "PERFORMANCE TERMINAL",
                        style = MonoTextStyles.CalendarCell,
                        color = ext.profit,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                Text(
                    text = stringResource(R.string.empty_dashboard_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.empty_dashboard_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onLogTrade,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("empty_log_trade_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.action_log_trade))
                    }
                    OutlinedButton(
                        onClick = onImportCsv,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("empty_import_csv_button")
                    ) {
                        Icon(Icons.Outlined.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.action_import_csv))
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onLoadDemo,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .testTag("empty_load_demo_button")
                ) {
                    Icon(Icons.Outlined.AutoGraph, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.action_load_demo))
                }
            }
        }
    }
}
