package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.csv.TradeCsvCodec
import com.example.data.preferences.AppPreferences
import com.example.domain.FormatUtils
import com.example.ui.state.EdgeLogUiState
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.EdgeLogThemeDefaults
import com.example.ui.theme.MonoTextStyles

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsAndToolsSheet(
    uiState: EdgeLogUiState,
    onDismiss: () -> Unit,
    onSelectAccount: (String) -> Unit,
    onSaveAccount: (String, String, String, Double) -> Unit,
    onDeleteAccount: (String) -> Unit,
    onSelectTheme: (AppThemeMode) -> Unit,
    onBackupJsonToDrive: () -> Unit,
    onRestoreJsonFromDrive: () -> Unit,
    onPickCsvFile: () -> Unit,
    onPasteCsvImport: (String) -> Unit,
    onExportCsvFile: () -> Unit,
    onLoadDemoData: () -> Unit,
    onClearDemoData: () -> Unit,
    onSignOut: () -> Unit
) {
    val ext = EdgeLogThemeDefaults.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showPasteCsvDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Cloud Account, Backups & Tools", style = MaterialTheme.typography.headlineSmall)

            // Connected Google Account & Cloud Sync Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, ext.profit.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CloudDone,
                                contentDescription = null,
                                tint = ext.profit,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = uiState.userDisplayName?.takeIf { it.isNotBlank() } ?: "Google Account Connected",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!uiState.userEmail.isNullOrBlank()) {
                                    Text(
                                        text = uiState.userEmail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        TextButton(
                            onClick = {
                                onDismiss()
                                onSignOut()
                            },
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = null,
                                tint = ext.loss,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.auth_sign_out), color = ext.loss)
                        }
                    }

                    Surface(
                        color = ext.profitContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${stringResource(R.string.cloud_sync_active)} · Clearing app data or reinstalling will automatically restore your journal when you sign back in.",
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.onProfitContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Full Journal Backup & Restore (.json to Google Drive / Files)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Google Drive / File Backup (.json)", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Save a portable full-journal snapshot (accounts, playbooks, trades, and daily reviews) directly to Google Drive or device files, or restore a previous backup.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            onDismiss()
                            onBackupJsonToDrive()
                        },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("sheet_backup_json")
                    ) {
                        Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Backup .JSON")
                    }
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onRestoreJsonFromDrive()
                        },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("sheet_restore_json")
                    ) {
                        Icon(Icons.Outlined.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Restore .JSON")
                    }
                }
            }

            HorizontalDivider(color = ext.cardBorder)

            // Trading Accounts Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Trading Accounts", style = MaterialTheme.typography.titleSmall)
                    TextButton(
                        onClick = { showAddAccountDialog = true },
                        modifier = Modifier.testTag("add_account_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Account")
                    }
                }

                if (uiState.accounts.size > 1) {
                    AccountOptionRow(
                        name = "All Accounts Combined",
                        subtitle = "${uiState.accounts.size} portfolios",
                        balance = uiState.accounts.sumOf { it.startingBalance },
                        selected = uiState.selectedAccountId == AppPreferences.ALL_ACCOUNTS_ID,
                        canDelete = false,
                        onSelect = {
                            onSelectAccount(AppPreferences.ALL_ACCOUNTS_ID)
                            onDismiss()
                        },
                        onDelete = {}
                    )
                }

                uiState.accounts.forEach { acc ->
                    AccountOptionRow(
                        name = acc.name,
                        subtitle = acc.broker,
                        balance = acc.startingBalance,
                        selected = uiState.selectedAccountId == acc.id ||
                            (uiState.accounts.size == 1 && uiState.selectedAccountId == AppPreferences.ALL_ACCOUNTS_ID),
                        canDelete = uiState.accounts.size > 1,
                        onSelect = {
                            onSelectAccount(acc.id)
                            onDismiss()
                        },
                        onDelete = { onDeleteAccount(acc.id) }
                    )
                }
            }

            HorizontalDivider(color = ext.cardBorder)

            // CSV Import / Export Section
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Broker CSV Import & Export", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Import broker trade statements or export trades as a spreadsheet-ready CSV.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            onDismiss()
                            onPickCsvFile()
                        },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("sheet_import_csv_file")
                    ) {
                        Icon(Icons.Outlined.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Import CSV")
                    }
                    OutlinedButton(
                        onClick = { showPasteCsvDialog = true },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("sheet_paste_csv")
                    ) {
                        Icon(Icons.Outlined.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Paste CSV")
                    }
                }
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onExportCsvFile()
                    },
                    enabled = uiState.allAccountTrades.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sheet_export_csv")
                ) {
                    Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Export ${uiState.allAccountTrades.size} Trades to CSV")
                }
            }

            HorizontalDivider(color = ext.cardBorder)

            // Appearance Theme Picker
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Terminal Theme", style = MaterialTheme.typography.titleSmall)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    AppThemeMode.entries.forEachIndexed { idx, mode ->
                        SegmentedButton(
                            selected = uiState.themeMode == mode,
                            onClick = { onSelectTheme(mode) },
                            shape = SegmentedButtonDefaults.itemShape(idx, AppThemeMode.entries.size)
                        ) {
                            Text(mode.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                }
            }

            HorizontalDivider(color = ext.cardBorder)

            // Opt-in Demo Dataset Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Demo Exploration Dataset", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Optionally load tagged multi-asset sample trades to test analytics, or remove demo data cleanly without affecting your real trades.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onLoadDemoData()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).height(46.dp).testTag("sheet_load_demo")
                    ) {
                        Icon(Icons.Outlined.AutoGraph, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Load Demo")
                    }
                    if (uiState.hasSampleData) {
                        OutlinedButton(
                            onClick = {
                                onClearDemoData()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f).height(46.dp).testTag("sheet_clear_demo")
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, tint = ext.loss, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Clear Demo", color = ext.loss)
                        }
                    }
                }
            }
        }
    }

    if (showAddAccountDialog) {
        AddAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onSave = { name, broker, balance ->
                onSaveAccount("", name, broker, balance)
                showAddAccountDialog = false
            }
        )
    }

    if (showPasteCsvDialog) {
        PasteCsvDialog(
            onDismiss = { showPasteCsvDialog = false },
            onImport = { raw ->
                onPasteCsvImport(raw)
                showPasteCsvDialog = false
                onDismiss()
            }
        )
    }
}

@Composable
private fun AccountOptionRow(
    name: String,
    subtitle: String,
    balance: Double,
    selected: Boolean,
    canDelete: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val ext = EdgeLogThemeDefaults.colors
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else ext.cardBorder
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                if (selected) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "Selected account",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                }
                Column {
                    Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "$subtitle · Start ${FormatUtils.formatCurrency(balance, compact = true)}",
                        style = MonoTextStyles.MicroBadge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (canDelete) {
                IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete account",
                        tint = ext.loss,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AddAccountDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var broker by remember { mutableStateOf("") }
    var balanceText by remember { mutableStateOf("25000") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Trading Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    label = { Text("Account Name (e.g., Prop 50K)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = broker,
                    onValueChange = { broker = it.take(60) },
                    label = { Text("Broker / Prop Firm") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text("Starting Balance ($)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bal = balanceText.toDoubleOrNull() ?: 25_000.0
                    onSave(name.trim(), broker.trim(), bal)
                },
                enabled = name.isNotBlank()
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PasteCsvDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var csvText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Paste Historical CSV") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Columns supported: Symbol, Side, AssetClass, EntryPrice, ExitPrice, Quantity, Multiplier, Fees, StopLoss, TakeProfit, EntryTime, ExitTime, Notes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = csvText,
                    onValueChange = { csvText = it },
                    label = { Text("CSV Rows with Header") },
                    minLines = 5,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth().testTag("paste_csv_input")
                )
                TextButton(
                    onClick = { csvText = TradeCsvCodec.SAMPLE_CSV_SNIPPET },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Insert Format Example")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(csvText) },
                enabled = csvText.isNotBlank(),
                modifier = Modifier.testTag("confirm_paste_csv_button")
            ) { Text("Import") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
