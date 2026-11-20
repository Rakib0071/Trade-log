package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.PlaybookRecord
import com.example.data.model.TradeOutcome
import com.example.data.model.TradeRecord
import com.example.data.model.TradeSide
import com.example.domain.DateUtils
import com.example.domain.FormatUtils
import com.example.ui.components.PnlStatusPill
import com.example.ui.components.QuantCard
import com.example.ui.theme.EdgeLogThemeDefaults
import com.example.ui.theme.MonoTextStyles
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TradeDetailScreen(
    trade: TradeRecord?,
    playbook: PlaybookRecord?,
    accountName: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAttachChartUri: (Uri) -> Unit,
    onRemoveChart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = EdgeLogThemeDefaults.colors
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Zero-permission Android Photo Picker per Play Policy compliance
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onAttachChartUri(uri)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = trade?.let { "${it.symbol} · ${it.sideEnum.label}" } ?: "Trade Details",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    if (trade != null) {
                        IconButton(onClick = onEdit, modifier = Modifier.testTag("detail_edit_button")) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit))
                        }
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.testTag("detail_delete_button")
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = ext.loss
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        if (trade == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Trade record not found", style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }

        val pnlColor = when (trade.outcome) {
            TradeOutcome.WIN -> ext.profit
            TradeOutcome.LOSS -> ext.loss
            TradeOutcome.BREAKEVEN -> ext.warning
            TradeOutcome.OPEN -> ext.accentIndigo
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val maxContentWidth = if (maxWidth >= 720.dp) 820.dp else 640.dp

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hero Result Card
                item(key = "detail_hero") {
                    QuantCard(modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = "$accountName · ${trade.assetClassEnum.label}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = if (trade.isClosed) {
                                        FormatUtils.formatCurrency(trade.netPnl, signed = true)
                                    } else "OPEN POSITION",
                                    style = MonoTextStyles.HeroMetric,
                                    color = pnlColor
                                )
                            }
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                PnlStatusPill(
                                    text = trade.sideEnum.label.uppercase(),
                                    isPositive = trade.sideEnum == TradeSide.LONG
                                )
                                trade.rMultiple?.let { r ->
                                    PnlStatusPill(
                                        text = FormatUtils.formatRMultiple(r),
                                        isPositive = r >= 0.0
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = ext.cardBorder)
                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DetailMetricMini("Gross P&L", FormatUtils.formatCurrency(trade.grossPnl, signed = true))
                            DetailMetricMini("Fees", FormatUtils.formatCurrency(trade.fees))
                            DetailMetricMini("ROI", FormatUtils.formatPercent(trade.returnPct, signed = true))
                            DetailMetricMini(
                                "Hold Time",
                                if (trade.isClosed) FormatUtils.formatDurationMinutes(trade.holdingDurationMinutes) else "Live"
                            )
                        }
                    }
                }

                // Execution & Risk Parameters Card
                item(key = "detail_execution") {
                    QuantCard(modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth)) {
                        Text("Execution & Risk Metrics", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))

                        DetailKeyValueRow("Entry Price", FormatUtils.formatPrice(trade.entryPrice))
                        DetailKeyValueRow(
                            "Exit Price",
                            trade.exitPrice?.let { FormatUtils.formatPrice(it) } ?: "Open"
                        )
                        DetailKeyValueRow(
                            "Position Size",
                            "${FormatUtils.formatQuantity(trade.quantity)} (×${FormatUtils.formatQuantity(trade.contractMultiplier)})"
                        )
                        DetailKeyValueRow("Notional Value", FormatUtils.formatCurrency(trade.entryNotional))
                        DetailKeyValueRow(
                            "Stop Loss / Initial Risk",
                            if (trade.stopLossPrice != null) {
                                "${FormatUtils.formatPrice(trade.stopLossPrice)} (${FormatUtils.formatCurrency(trade.initialRiskDollars ?: 0.0)})"
                            } else "Not set"
                        )
                        DetailKeyValueRow(
                            "Take Profit Target",
                            trade.takeProfitPrice?.let { FormatUtils.formatPrice(it) } ?: "Not set"
                        )
                        DetailKeyValueRow("Entry Time", DateUtils.formatDateTimeShort(trade.entryTimeEpochMs))
                        DetailKeyValueRow(
                            "Exit Time",
                            trade.exitTimeEpochMs?.let { DateUtils.formatDateTimeShort(it) } ?: "Active"
                        )
                    }
                }

                // Chart Screenshot Card (Zero-permission Photo Picker)
                item(key = "detail_chart") {
                    QuantCard(modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Chart Markup", style = MaterialTheme.typography.titleMedium)
                            if (trade.chartImagePath != null) {
                                Row {
                                    TextButton(
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        }
                                    ) {
                                        Text(stringResource(R.string.action_replace_chart))
                                    }
                                    TextButton(onClick = onRemoveChart) {
                                        Text(stringResource(R.string.action_remove_chart), color = ext.loss)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        val chartFile = trade.chartImagePath?.let { File(it) }
                        if (chartFile != null && chartFile.exists()) {
                            AsyncImage(
                                model = chartFile,
                                contentDescription = stringResource(R.string.cd_trade_chart),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        } else {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("detail_attach_chart_button")
                            ) {
                                Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.action_attach_chart))
                            }
                        }
                    }
                }

                // Playbook Compliance & Psychology Card
                item(key = "detail_psychology") {
                    QuantCard(modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth)) {
                        Text("Playbook & Psychology Review", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Execution Rating", style = MaterialTheme.typography.labelSmall, color = ext.textMuted)
                                Spacer(Modifier.height(4.dp))
                                Row {
                                    for (star in 1..5) {
                                        Icon(
                                            imageVector = if (star <= trade.rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                            contentDescription = null,
                                            tint = if (star <= trade.rating) ext.warning else ext.textMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Pre-Trade Mindset", style = MaterialTheme.typography.labelSmall, color = ext.textMuted)
                                Spacer(Modifier.height(4.dp))
                                Surface(
                                    color = ext.accentIndigoContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = trade.emotionEnum.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ext.accentIndigo,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        if (playbook != null) {
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = ext.cardBorder)
                            Spacer(Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Playbook: ${playbook.name}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                PnlStatusPill(
                                    text = "${trade.playbookAdherencePct}% Rules",
                                    isPositive = trade.playbookAdherencePct >= 75
                                )
                            }
                            if (playbook.rules.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                val checkedSet = trade.checkedRules.toSet()
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    playbook.rules.forEach { rule ->
                                        val checked = checkedSet.isEmpty() && trade.playbookAdherencePct == 100 ||
                                            checkedSet.contains(rule)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (checked) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                                contentDescription = null,
                                                tint = if (checked) ext.profit else ext.textMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = rule,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (trade.setupTags.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text("Setup Tags", style = MaterialTheme.typography.labelSmall, color = ext.textMuted)
                            Spacer(Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                trade.setupTags.forEach { tag ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = tag,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (trade.mistakeTags.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text("Mistakes Logged", style = MaterialTheme.typography.labelSmall, color = ext.warning)
                            Spacer(Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                trade.mistakeTags.forEach { tag ->
                                    Surface(
                                        color = ext.warningContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = tag,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ext.warning,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Journal Notes Card
                item(key = "detail_notes") {
                    QuantCard(modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth)) {
                        Text("Trade Notes & Post-Mortem", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = trade.notes.ifBlank { "No notes written yet. Tap Edit to record your thesis and lessons learned." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (trade.notes.isBlank()) ext.textMuted else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                item(key = "detail_bottom") {
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Trade?") },
            text = { Text("This will permanently remove this execution and recalculate your performance metrics.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) {
                    Text(stringResource(R.string.action_delete), color = ext.loss)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun DetailMetricMini(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = EdgeLogThemeDefaults.colors.textMuted)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MonoTextStyles.DataRow, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun DetailKeyValueRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MonoTextStyles.DataRow, color = MaterialTheme.colorScheme.onSurface)
    }
}
