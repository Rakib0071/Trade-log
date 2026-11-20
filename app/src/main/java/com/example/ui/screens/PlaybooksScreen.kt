package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.PlaybookRecord
import com.example.domain.FormatUtils
import com.example.domain.PlaybookStats
import com.example.ui.components.PnlStatusPill
import com.example.ui.components.QuantCard
import com.example.ui.state.EdgeLogUiState
import com.example.ui.theme.EdgeLogThemeDefaults
import com.example.ui.theme.MonoTextStyles
import com.example.ui.theme.PlaybookColorSwatches

@Composable
fun PlaybooksScreen(
    uiState: EdgeLogUiState,
    onSavePlaybook: (PlaybookRecord) -> Unit,
    onDeletePlaybook: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = EdgeLogThemeDefaults.colors
    var editingPlaybook by remember { mutableStateOf<PlaybookRecord?>(null) }
    var showEditorDialog by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxContentWidth = if (maxWidth >= 720.dp) 960.dp else 640.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("playbooks_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item(key = "playbooks_header") {
                Row(
                    modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Strategy Playbooks", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = "Track setup expectancy, win rate, and rule compliance",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            editingPlaybook = null
                            showEditorDialog = true
                        },
                        modifier = Modifier.height(44.dp).testTag("new_playbook_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            if (uiState.playbooks.isEmpty()) {
                item(key = "playbooks_empty") {
                    QuantCard(modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MenuBook,
                                contentDescription = null,
                                tint = ext.textMuted,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.empty_playbooks_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.empty_playbooks_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    editingPlaybook = null
                                    showEditorDialog = true
                                }
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.action_add_playbook))
                            }
                        }
                    }
                }
            } else {
                items(uiState.playbooks, key = { it.id }) { playbook ->
                    val stats = uiState.summary.playbookMetrics[playbook.id]
                    PlaybookCard(
                        playbook = playbook,
                        stats = stats,
                        onEdit = {
                            editingPlaybook = playbook
                            showEditorDialog = true
                        },
                        onDelete = { onDeletePlaybook(playbook.id) },
                        modifier = Modifier.widthIn(max = maxContentWidth)
                    )
                }
            }

            item(key = "playbooks_bottom") {
                Spacer(Modifier.height(72.dp))
            }
        }
    }

    if (showEditorDialog) {
        PlaybookEditorDialog(
            existing = editingPlaybook,
            onDismiss = { showEditorDialog = false },
            onSave = {
                onSavePlaybook(it)
                showEditorDialog = false
            }
        )
    }
}

@Composable
private fun PlaybookCard(
    playbook: PlaybookRecord,
    stats: PlaybookStats?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = EdgeLogThemeDefaults.colors
    val accent = Color(playbook.colorHex)
    var confirmDelete by remember { mutableStateOf(false) }

    QuantCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playbook.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${playbook.marketCondition} · Target ${FormatUtils.formatRatio(playbook.targetRiskReward)}R",
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.textMuted
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (stats != null && stats.tradeCount > 0) {
                    PnlStatusPill(
                        text = FormatUtils.formatCurrency(stats.netPnl, signed = true, compact = true),
                        isPositive = stats.netPnl >= 0.0
                    )
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit playbook", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { confirmDelete = true }, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete playbook",
                        tint = ext.loss,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (playbook.description.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = playbook.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(12.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PlaybookStatCol("Trades", "${stats?.tradeCount ?: 0}")
                PlaybookStatCol("Win %", stats?.takeIf { it.tradeCount > 0 }?.let { FormatUtils.formatPercent(it.winRatePct, decimals = 0) } ?: "—")
                PlaybookStatCol("PF", stats?.takeIf { it.tradeCount > 0 }?.let { FormatUtils.formatRatio(it.profitFactor) } ?: "—")
                PlaybookStatCol(
                    "Expect.",
                    stats?.takeIf { it.tradeCount > 0 }?.let { FormatUtils.formatCurrency(it.expectancy, signed = true, compact = true) } ?: "—"
                )
                PlaybookStatCol(
                    "Rules %",
                    stats?.takeIf { it.tradeCount > 0 }?.let { "${it.avgAdherencePct.toInt()}%" } ?: "—"
                )
            }
        }

        if (playbook.rules.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = ext.cardBorder)
            Spacer(Modifier.height(8.dp))
            Text("Entry Checklist (${playbook.rules.size} rules)", style = MaterialTheme.typography.labelSmall, color = ext.textMuted)
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                playbook.rules.forEach { rule ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(7.dp))
                        Text(rule, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete Playbook?") },
            text = { Text("Linked trades will remain in your journal, but their playbook association will be cleared.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    }
                ) { Text("Delete", color = ext.loss) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PlaybookStatCol(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = EdgeLogThemeDefaults.colors.textMuted)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MonoTextStyles.DataRow, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun PlaybookEditorDialog(
    existing: PlaybookRecord?,
    onDismiss: () -> Unit,
    onSave: (PlaybookRecord) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name.orEmpty()) }
    var description by remember { mutableStateOf(existing?.description.orEmpty()) }
    var condition by remember { mutableStateOf(existing?.marketCondition ?: "Trending") }
    var targetRR by remember { mutableStateOf((existing?.targetRiskReward ?: 2.5).toString()) }
    var rulesMultiline by remember { mutableStateOf(existing?.rules?.joinToString("\n").orEmpty()) }
    var selectedColor by remember { mutableStateOf(existing?.colorHex ?: PlaybookColorSwatches.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Create Playbook" else "Edit Playbook") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Strategy Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("playbook_name_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Edge Hypothesis / Description") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = condition,
                        onValueChange = { condition = it },
                        label = { Text("Market Condition") },
                        singleLine = true,
                        modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                        value = targetRR,
                        onValueChange = { targetRR = it },
                        label = { Text("Target R:R") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(0.8f)
                    )
                }
                OutlinedTextField(
                    value = rulesMultiline,
                    onValueChange = { rulesMultiline = it },
                    label = { Text("Checklist Rules (one per line)") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth().testTag("playbook_rules_input")
                )
                Text("Accent Color", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PlaybookColorSwatches.forEach { hex ->
                        val isSelected = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(
                                    width = if (isSelected) 2.5.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim()
                    if (cleanName.isNotEmpty()) {
                        val ruleList = rulesMultiline
                            .lines()
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .take(20)
                        onSave(
                            PlaybookRecord(
                                id = existing?.id.orEmpty(),
                                name = cleanName.take(80),
                                description = description.trim().take(500),
                                marketCondition = condition.trim().ifEmpty { "Any" }.take(60),
                                rules = ruleList,
                                targetRiskReward = (targetRR.toDoubleOrNull() ?: 2.0).coerceIn(0.1, 100.0),
                                colorHex = selectedColor,
                                demoRecord = false
                            )
                        )
                    }
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("playbook_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
