package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AssetClass
import com.example.data.model.TradeOutcome
import com.example.data.model.TradeSide
import com.example.data.model.TradeSortOption
import com.example.domain.FormatUtils
import com.example.ui.components.PnlStatusPill
import com.example.ui.components.QuantCard
import com.example.ui.components.TradeListItemCard
import com.example.ui.state.EdgeLogUiState
import com.example.ui.theme.EdgeLogThemeDefaults
import com.example.ui.theme.MonoTextStyles

@Composable
fun TradesListScreen(
    uiState: EdgeLogUiState,
    onSearchChange: (String) -> Unit,
    onSelectOutcome: (TradeOutcome?) -> Unit,
    onSelectSide: (TradeSide?) -> Unit,
    onSelectAssetClass: (AssetClass?) -> Unit,
    onSelectSort: (TradeSortOption) -> Unit,
    onClearFilters: () -> Unit,
    onOpenTradeDetail: (String) -> Unit,
    onLogTrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = EdgeLogThemeDefaults.colors
    val filters = uiState.filters
    val trades = uiState.filteredTradesList
    val playbookMap = remember(uiState.playbooks) { uiState.playbooks.associate { it.id to it.name } }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val filteredNetPnl = remember(trades) { trades.filter { it.isClosed }.sumOf { it.netPnl } }
    val hasActiveFilter = filters.outcome != null ||
        filters.side != null ||
        filters.assetClass != null ||
        filters.searchQuery.isNotBlank()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxContentWidth = if (maxWidth >= 720.dp) 960.dp else 640.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("trades_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header + Sort Menu
            item(key = "trades_header") {
                Row(
                    modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Trade Execution Log",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "${trades.size} trade${if (trades.size == 1) "" else "s"} matching view",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box {
                        OutlinedButton(
                            onClick = { sortMenuExpanded = true },
                            modifier = Modifier.height(44.dp).testTag("trades_sort_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(filters.sortOption.label, style = MaterialTheme.typography.labelMedium)
                        }
                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            TradeSortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        onSelectSort(option)
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Search Field
            item(key = "search_bar") {
                OutlinedTextField(
                    value = filters.searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("Search symbol, playbook, mistake, or note...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (filters.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.cd_clear_search)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = maxContentWidth)
                        .testTag("trades_search_input")
                )
            }

            // Outcome & Side Filter Chips
            item(key = "filter_chips") {
                Column(
                    modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = filters.outcome == null && filters.side == null,
                            onClick = {
                                onSelectOutcome(null)
                                onSelectSide(null)
                            },
                            label = { Text("All", style = MaterialTheme.typography.labelSmall) }
                        )
                        FilterChip(
                            selected = filters.outcome == TradeOutcome.WIN,
                            onClick = {
                                onSelectOutcome(if (filters.outcome == TradeOutcome.WIN) null else TradeOutcome.WIN)
                            },
                            label = { Text("Wins", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("filter_outcome_win")
                        )
                        FilterChip(
                            selected = filters.outcome == TradeOutcome.LOSS,
                            onClick = {
                                onSelectOutcome(if (filters.outcome == TradeOutcome.LOSS) null else TradeOutcome.LOSS)
                            },
                            label = { Text("Losses", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("filter_outcome_loss")
                        )
                        FilterChip(
                            selected = filters.outcome == TradeOutcome.OPEN,
                            onClick = {
                                onSelectOutcome(if (filters.outcome == TradeOutcome.OPEN) null else TradeOutcome.OPEN)
                            },
                            label = { Text("Open", style = MaterialTheme.typography.labelSmall) }
                        )
                        FilterChip(
                            selected = filters.side == TradeSide.LONG,
                            onClick = {
                                onSelectSide(if (filters.side == TradeSide.LONG) null else TradeSide.LONG)
                            },
                            label = { Text("Long", style = MaterialTheme.typography.labelSmall) }
                        )
                        FilterChip(
                            selected = filters.side == TradeSide.SHORT,
                            onClick = {
                                onSelectSide(if (filters.side == TradeSide.SHORT) null else TradeSide.SHORT)
                            },
                            label = { Text("Short", style = MaterialTheme.typography.labelSmall) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssetClass.entries.forEach { asset ->
                            FilterChip(
                                selected = filters.assetClass == asset,
                                onClick = {
                                    onSelectAssetClass(if (filters.assetClass == asset) null else asset)
                                },
                                label = { Text(asset.label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }

            // Summary strip for current filtered subset
            if (trades.isNotEmpty()) {
                item(key = "filtered_summary") {
                    Row(
                        modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Subset Net P&L:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            PnlStatusPill(
                                text = FormatUtils.formatCurrency(filteredNetPnl, signed = true),
                                isPositive = if (filteredNetPnl == 0.0) null else filteredNetPnl > 0.0
                            )
                        }
                        if (hasActiveFilter) {
                            TextButton(onClick = onClearFilters) {
                                Text("Reset Filters")
                            }
                        }
                    }
                }
            }

            if (trades.isEmpty()) {
                item(key = "empty_trades") {
                    QuantCard(modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ReceiptLong,
                                contentDescription = null,
                                tint = ext.textMuted,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.empty_trades_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.empty_trades_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (hasActiveFilter) {
                                    OutlinedButton(onClick = onClearFilters) {
                                        Text("Clear Filters")
                                    }
                                }
                                Button(onClick = onLogTrade) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.action_log_trade))
                                }
                            }
                        }
                    }
                }
            } else {
                items(trades, key = { it.id }) { trade ->
                    TradeListItemCard(
                        trade = trade,
                        playbookName = trade.playbookId?.let { playbookMap[it] },
                        onClick = { onOpenTradeDetail(trade.id) },
                        modifier = Modifier.widthIn(max = maxContentWidth)
                    )
                }
            }

            item(key = "trades_bottom_spacer") {
                Spacer(Modifier.height(72.dp))
            }
        }
    }
}
