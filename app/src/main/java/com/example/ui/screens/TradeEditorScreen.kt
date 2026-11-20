package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.AssetClass
import com.example.data.model.EmotionTag
import com.example.data.model.PlaybookRecord
import com.example.data.model.StandardMistakeTags
import com.example.data.model.StandardSetupTags
import com.example.data.model.TradeRecord
import com.example.data.model.TradeSide
import com.example.data.model.TradeStatus
import com.example.data.model.TradingAccountRecord
import com.example.domain.DateUtils
import com.example.domain.FormatUtils
import com.example.ui.components.QuantCard
import com.example.ui.theme.EdgeLogThemeDefaults
import com.example.ui.theme.MonoTextStyles
import java.io.File
import kotlin.math.abs

private fun popularSymbolsForAssetClass(assetClass: AssetClass): List<String> = when (assetClass) {
    AssetClass.STOCK -> listOf("NVDA", "AAPL", "TSLA", "SPY", "QQQ", "AMD", "MSFT", "META")
    AssetClass.OPTIONS -> listOf("SPY", "QQQ", "NVDA", "TSLA", "AAPL", "IWM")
    AssetClass.FUTURES -> listOf("ES", "NQ", "YM", "CL", "GC", "MNQ")
    AssetClass.FOREX -> listOf("EURUSD", "GBPUSD", "USDJPY", "XAUUSD", "USDCAD")
    AssetClass.CRYPTO -> listOf("BTCUSD", "ETHUSD", "SOLUSD", "XRPUSD")
}

private val quickTradeReasonTemplates = listOf(
    "Breakout above resistance",
    "Pullback to VWAP",
    "Support bounce",
    "Liquidity sweep reversal",
    "Opening range continuation",
    "Earnings / news momentum"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TradeEditorScreen(
    existingTrade: TradeRecord?,
    initialDateIso: String?,
    accounts: List<TradingAccountRecord>,
    defaultAccountId: String,
    playbooks: List<PlaybookRecord>,
    recentSymbols: List<String>,
    onCopyChartImage: (Uri, (String) -> Unit) -> Unit,
    onSave: (TradeRecord) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = EdgeLogThemeDefaults.colors
    val isEditing = existingTrade != null

    val defaultEntryMs = remember(existingTrade, initialDateIso) {
        when {
            existingTrade != null -> existingTrade.entryTimeEpochMs
            !initialDateIso.isNullOrBlank() -> DateUtils.isoDateToEpoch(initialDateIso, 9, 45)
            else -> System.currentTimeMillis() - 45 * 60_000L
        }
    }

    var accountId by remember { mutableStateOf(existingTrade?.accountId?.ifBlank { defaultAccountId } ?: defaultAccountId) }
    var symbol by remember { mutableStateOf(existingTrade?.symbol.orEmpty()) }
    var assetClass by remember { mutableStateOf(existingTrade?.assetClassEnum ?: AssetClass.STOCK) }
    var side by remember { mutableStateOf(existingTrade?.sideEnum ?: TradeSide.LONG) }
    var status by remember { mutableStateOf(existingTrade?.statusEnum ?: TradeStatus.CLOSED) }

    var entryPriceText by remember { mutableStateOf(existingTrade?.entryPrice?.toString().orEmpty()) }
    var exitPriceText by remember { mutableStateOf(existingTrade?.exitPrice?.toString().orEmpty()) }
    var quantityText by remember { mutableStateOf(existingTrade?.quantity?.let { FormatUtils.formatQuantity(it) }.orEmpty()) }
    var multiplierText by remember { mutableStateOf(existingTrade?.contractMultiplier?.let { FormatUtils.formatQuantity(it) } ?: "1") }
    var feesText by remember { mutableStateOf(existingTrade?.fees?.takeIf { it > 0 }?.toString().orEmpty()) }
    var stopLossText by remember { mutableStateOf(existingTrade?.stopLossPrice?.toString().orEmpty()) }
    var takeProfitText by remember { mutableStateOf(existingTrade?.takeProfitPrice?.toString().orEmpty()) }

    var entryDateText by remember { mutableStateOf(DateUtils.epochToIsoDate(defaultEntryMs)) }
    var entryTimeText by remember { mutableStateOf(DateUtils.formatTimeOnly(defaultEntryMs)) }
    var holdMinutes by remember {
        mutableStateOf(
            if (existingTrade != null && existingTrade.isClosed) {
                existingTrade.holdingDurationMinutes.toString()
            } else "45"
        )
    }

    var selectedPlaybookId by remember { mutableStateOf(existingTrade?.playbookId) }
    var checkedRules by remember { mutableStateOf(existingTrade?.checkedRules?.toSet().orEmpty()) }
    var selectedEmotion by remember { mutableStateOf(existingTrade?.emotionEnum ?: EmotionTag.CALM) }
    var selectedMistakes by remember { mutableStateOf(existingTrade?.mistakeTags?.toSet().orEmpty()) }
    var selectedSetupTags by remember { mutableStateOf(existingTrade?.setupTags?.toSet().orEmpty()) }
    var rating by remember { mutableIntStateOf(existingTrade?.rating ?: 4) }
    var notes by remember { mutableStateOf(existingTrade?.notes.orEmpty()) }
    var chartPath by remember { mutableStateOf(existingTrade?.chartImagePath) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onCopyChartImage(uri) { savedPath -> chartPath = savedPath }
        }
    }

    val quickSymbols = remember(recentSymbols, assetClass) {
        (recentSymbols + popularSymbolsForAssetClass(assetClass)).distinct().take(10)
    }

    // Live P&L + Risk + R-Multiple Calculation
    val parsedEntry = entryPriceText.toDoubleOrNull()
    val parsedExit = exitPriceText.toDoubleOrNull()
    val parsedQty = quantityText.toDoubleOrNull()
    val parsedMult = (multiplierText.toDoubleOrNull() ?: 1.0).coerceAtLeast(0.0001)
    val parsedFees = (feesText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
    val parsedSl = stopLossText.toDoubleOrNull()

    val previewNetPnl = remember(parsedEntry, parsedExit, parsedQty, parsedMult, parsedFees, side, status) {
        if (status == TradeStatus.CLOSED && parsedEntry != null && parsedExit != null && parsedQty != null) {
            val dir = if (side == TradeSide.LONG) 1.0 else -1.0
            (parsedExit - parsedEntry) * parsedQty * parsedMult * dir - parsedFees
        } else null
    }
    val previewRisk = remember(parsedEntry, parsedSl, parsedQty, parsedMult) {
        if (parsedEntry != null && parsedSl != null && parsedQty != null) {
            abs(parsedEntry - parsedSl) * parsedQty * parsedMult
        } else null
    }
    val previewR = remember(previewNetPnl, previewRisk) {
        if (previewNetPnl != null && previewRisk != null && previewRisk > 0.0) {
            previewNetPnl / previewRisk
        } else null
    }

    val tradeTakenHeadline = remember(symbol, side, assetClass, parsedQty, parsedEntry, parsedExit, status) {
        val cleanSym = symbol.trim().uppercase()
        if (cleanSym.isEmpty()) {
            "Select or type the trade you took below"
        } else {
            val action = if (side == TradeSide.LONG) "LONG" else "SHORT"
            val qtyStr = parsedQty?.let { "${FormatUtils.formatQuantity(it)} " }.orEmpty()
            val priceStr = when {
                parsedEntry != null && status == TradeStatus.CLOSED && parsedExit != null ->
                    " · ${FormatUtils.formatPrice(parsedEntry)} → ${FormatUtils.formatPrice(parsedExit)}"
                parsedEntry != null -> " · @ ${FormatUtils.formatPrice(parsedEntry)}"
                else -> ""
            }
            "$action $qtyStr$cleanSym (${assetClass.shortLabel})$priceStr"
        }
    }

    val selectedPlaybook = remember(playbooks, selectedPlaybookId) {
        playbooks.firstOrNull { it.id == selectedPlaybookId }
    }

    fun submitTrade() {
        val cleanSymbol = symbol.trim().uppercase().take(24)
        if (cleanSymbol.isEmpty()) {
            validationError = "Please enter the ticker/symbol of the trade you took (e.g., NVDA, ES, BTCUSD)."
            return
        }
        val entry = entryPriceText.toDoubleOrNull()
        if (entry == null || entry <= 0.0) {
            validationError = "Enter a valid positive entry price."
            return
        }
        val qty = quantityText.toDoubleOrNull()
        if (qty == null || qty <= 0.0) {
            validationError = "Enter a valid positive quantity."
            return
        }
        val exit = if (status == TradeStatus.CLOSED) exitPriceText.toDoubleOrNull() else null
        if (status == TradeStatus.CLOSED && (exit == null || exit <= 0.0)) {
            validationError = "Closed trades require a valid exit price (or switch status to Open)."
            return
        }

        val timeParts = entryTimeText.trim().split(":")
        val hour = timeParts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
        val minute = timeParts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 30
        val entryMs = DateUtils.isoDateToEpoch(entryDateText, hour, minute)
        val durationMin = (holdMinutes.toLongOrNull() ?: 45L).coerceAtLeast(1L)
        val exitMs = if (status == TradeStatus.CLOSED) entryMs + durationMin * 60_000L else null

        val adherencePct = if (selectedPlaybook != null && selectedPlaybook.rules.isNotEmpty()) {
            val matched = selectedPlaybook.rules.count { checkedRules.contains(it) }
            ((matched.toDouble() / selectedPlaybook.rules.size) * 100.0).toInt()
        } else 100

        validationError = null
        val record = TradeRecord(
            id = existingTrade?.id.orEmpty(),
            accountId = accountId.ifBlank { defaultAccountId },
            symbol = cleanSymbol,
            assetClass = assetClass.name,
            side = side.name,
            status = status.name,
            entryPrice = entry,
            exitPrice = exit,
            quantity = qty,
            contractMultiplier = parsedMult,
            fees = parsedFees,
            stopLossPrice = stopLossText.toDoubleOrNull()?.takeIf { it > 0.0 },
            takeProfitPrice = takeProfitText.toDoubleOrNull()?.takeIf { it > 0.0 },
            entryTimeEpochMs = entryMs,
            exitTimeEpochMs = exitMs,
            playbookId = selectedPlaybookId,
            playbookAdherencePct = adherencePct,
            checkedRules = checkedRules.toList().take(20),
            mistakeTags = selectedMistakes.toList().take(15),
            setupTags = selectedSetupTags.toList().take(15),
            emotionBefore = selectedEmotion.name,
            rating = rating,
            notes = notes.trim().take(3000),
            chartImagePath = chartPath,
            demoRecord = existingTrade?.demoRecord ?: false
        )
        onSave(record)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Execution" else "Log New Trade") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("editor_back_button")) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = { submitTrade() },
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("editor_save_top_button")
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.action_save))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
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
                // Live Calculated Preview Bar + Plain-English "Trade Taken" Summary
                item(key = "live_preview") {
                    Surface(
                        modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(
                            1.dp,
                            when {
                                previewNetPnl == null -> ext.cardBorder
                                previewNetPnl >= 0 -> ext.profit.copy(alpha = 0.5f)
                                else -> ext.loss.copy(alpha = 0.5f)
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "TRADE TAKEN",
                                    style = MonoTextStyles.CalendarCell,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = tradeTakenHeadline,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            HorizontalDivider(color = ext.cardBorder)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Est. Net P&L", style = MaterialTheme.typography.labelSmall, color = ext.textMuted)
                                    Text(
                                        text = previewNetPnl?.let { FormatUtils.formatCurrency(it, signed = true) } ?: "—",
                                        style = MonoTextStyles.KpiValue,
                                        color = when {
                                            previewNetPnl == null -> MaterialTheme.colorScheme.onSurfaceVariant
                                            previewNetPnl >= 0 -> ext.profit
                                            else -> ext.loss
                                        }
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Initial Risk", style = MaterialTheme.typography.labelSmall, color = ext.textMuted)
                                    Text(
                                        text = previewRisk?.let { FormatUtils.formatCurrency(it) } ?: "—",
                                        style = MonoTextStyles.CardMetric,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Realized R", style = MaterialTheme.typography.labelSmall, color = ext.textMuted)
                                    Text(
                                        text = previewR?.let { FormatUtils.formatRMultiple(it) } ?: "—",
                                        style = MonoTextStyles.CardMetric,
                                        color = when {
                                            previewR == null -> MaterialTheme.colorScheme.onSurfaceVariant
                                            previewR >= 0 -> ext.profit
                                            else -> ext.loss
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (validationError != null) {
                    item(key = "val_error") {
                        Surface(
                            modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth),
                            color = ext.lossContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = validationError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = ext.onLossContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                // Core "What Trade Did You Take?" Card
                item(key = "instrument_card") {
                    QuantCard(modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth)) {
                        Text("What Trade Did You Take?", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Pick the market, ticker, direction, and setup you traded",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))

                        if (accounts.size > 1) {
                            Text("Account", style = MaterialTheme.typography.labelSmall, color = ext.textMuted)
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                accounts.forEach { acc ->
                                    FilterChip(
                                        selected = accountId == acc.id,
                                        onClick = { accountId = acc.id },
                                        label = { Text(acc.name, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        }

                        Text("Market / Asset Class", style = MaterialTheme.typography.labelSmall, color = ext.textMuted)
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AssetClass.entries.forEach { cls ->
                                FilterChip(
                                    selected = assetClass == cls,
                                    onClick = {
                                        assetClass = cls
                                        if (!isEditing) {
                                            multiplierText = FormatUtils.formatQuantity(cls.defaultMultiplier)
                                        }
                                    },
                                    label = { Text(cls.label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = symbol,
                            onValueChange = { symbol = it.uppercase().take(24) },
                            label = { Text("Ticker / Instrument I Took (e.g., NVDA, ES, EURUSD)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            modifier = Modifier.fillMaxWidth().testTag("editor_symbol_input")
                        )

                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (recentSymbols.isNotEmpty()) "Quick Pick (Recent & Popular)" else "Quick Pick Popular Tickers",
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.textMuted
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            quickSymbols.forEach { ticker ->
                                FilterChip(
                                    selected = symbol.equals(ticker, ignoreCase = true),
                                    onClick = { symbol = ticker },
                                    label = { Text(ticker, style = MonoTextStyles.MicroBadge) },
                                    modifier = Modifier.testTag("quick_symbol_$ticker")
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Text("Direction", style = MaterialTheme.typography.labelSmall, color = ext.textMuted)
                        Spacer(Modifier.height(6.dp))
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = side == TradeSide.LONG,
                                onClick = { side = TradeSide.LONG },
                                shape = SegmentedButtonDefaults.itemShape(0, 2),
                                modifier = Modifier.testTag("editor_side_long")
                            ) { Text("LONG / BUY", style = MonoTextStyles.MicroBadge) }
                            SegmentedButton(
                                selected = side == TradeSide.SHORT,
                                onClick = { side = TradeSide.SHORT },
                                shape = SegmentedButtonDefaults.itemShape(1, 2),
                                modifier = Modifier.testTag("editor_side_short")
                            ) { Text("SHORT / SELL", style = MonoTextStyles.MicroBadge) }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text("Position Status", style = MaterialTheme.typography.labelSmall, color = ext.textMuted)
                        Spacer(Modifier.height(6.dp))
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = status == TradeStatus.CLOSED,
                                onClick = { status = TradeStatus.CLOSED },
                                shape = SegmentedButtonDefaults.itemShape(0, 2),
                                modifier = Modifier.testTag("editor_status_closed")
                            ) { Text("Closed Trade", style = MaterialTheme.typography.labelMedium) }
                            SegmentedButton(
                                selected = status == TradeStatus.OPEN,
                                onClick = { status = TradeStatus.OPEN },
                                shape = SegmentedButtonDefaults.itemShape(1, 2),
                                modifier = Modifier.testTag("editor_status_open")
                            ) { Text("Still Open", style = MaterialTheme.typography.labelMedium) }
                        }
                    }
                }

                // Prices, Size & Risk Card
                item(key = "pricing_card") {
                    QuantCard(modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth)) {
                        Text("Execution Prices & Risk", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = entryPriceText,
                                onValueChange = { entryPriceText = it },
                                label = { Text("Entry Price") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f).testTag("editor_entry_price_input")
                            )
                            OutlinedTextField(
                                value = exitPriceText,
                                onValueChange = { exitPriceText = it },
                                label = { Text("Exit Price") },
                                enabled = status == TradeStatus.CLOSED,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f).testTag("editor_exit_price_input")
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = quantityText,
                                onValueChange = { quantityText = it },
                                label = { Text("Quantity / Size") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f).testTag("editor_quantity_input")
                            )
                            OutlinedTextField(
                                value = multiplierText,
                                onValueChange = { multiplierText = it },
                                label = { Text("Multiplier") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(0.75f)
                            )
                            OutlinedTextField(
                                value = feesText,
                                onValueChange = { feesText = it },
                                label = { Text("Fees ($)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(0.75f)
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = stopLossText,
                                onValueChange = { stopLossText = it },
                                label = { Text("Stop Loss (for R)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f).testTag("editor_sl_input")
                            )
                            OutlinedTextField(
                                value = takeProfitText,
                                onValueChange = { takeProfitText = it },
                                label = { Text("Take Profit Target") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = entryDateText,
                                onValueChange = { entryDateText = it },
                                label = { Text("Date (YYYY-MM-DD)") },
                                singleLine = true,
                                modifier = Modifier.weight(1.2f)
                            )
                            OutlinedTextField(
                                value = entryTimeText,
                                onValueChange = { entryTimeText = it },
                                label = { Text("Time (HH:mm)") },
                                singleLine = true,
                                modifier = Modifier.weight(0.8f)
                            )
                            OutlinedTextField(
                                value = holdMinutes,
                                onValueChange = { holdMinutes = it },
                                label = { Text("Hold (min)") },
                                enabled = status == TradeStatus.CLOSED,
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(0.8f)
                            )
                        }
                    }
                }

                // Dedicated "Why I Took This Trade & Setup Notes" Card right after pricing
                item(key = "trade_rationale_card") {
                    QuantCard(modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth)) {
                        Text("Why I Took This Trade & Chart", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Describe your entry reason, trigger, and exit review",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it.take(3000) },
                            label = { Text("What trade I took, why I entered, & review") },
                            placeholder = { Text("e.g., Bought NVDA breakout above 15m ORB high on strong volume...") },
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier.fillMaxWidth().testTag("editor_notes_input")
                        )

                        Spacer(Modifier.height(8.dp))
                        Text("Quick Entry Reason Prompts", style = MaterialTheme.typography.labelSmall, color = ext.textMuted)
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            quickTradeReasonTemplates.forEach { reason ->
                                SuggestionChip(
                                    onClick = {
                                        val updated = if (notes.isBlank()) reason else "$notes\n• $reason"
                                        notes = updated.take(3000)
                                    },
                                    label = { Text(reason, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        val file = chartPath?.let { File(it) }
                        if (file != null && file.exists()) {
                            AsyncImage(
                                model = file,
                                contentDescription = stringResource(R.string.cd_trade_chart),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (chartPath != null) "Replace Chart Screenshot" else "Attach Chart Screenshot")
                        }
                    }
                }

                // Playbook & Rule Adherence Card
                item(key = "playbook_card") {
                    QuantCard(modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth)) {
                        Text("Playbook & Strategy Compliance", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))

                        if (playbooks.isEmpty()) {
                            Text(
                                text = "Create strategy Playbooks in the Playbooks tab to track rule adherence per setup.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedPlaybookId == null,
                                    onClick = {
                                        selectedPlaybookId = null
                                        checkedRules = emptySet()
                                    },
                                    label = { Text("None", style = MaterialTheme.typography.labelSmall) }
                                )
                                playbooks.forEach { pb ->
                                    FilterChip(
                                        selected = selectedPlaybookId == pb.id,
                                        onClick = {
                                            selectedPlaybookId = pb.id
                                            checkedRules = pb.rules.toSet()
                                        },
                                        label = { Text(pb.name, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }

                            if (selectedPlaybook != null && selectedPlaybook.rules.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Check rules followed:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ext.textMuted
                                )
                                selectedPlaybook.rules.forEach { rule ->
                                    val isChecked = checkedRules.contains(rule)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                checkedRules = if (isChecked) checkedRules - rule else checkedRules + rule
                                            }
                                            .padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = {
                                                checkedRules = if (it) checkedRules + rule else checkedRules - rule
                                            }
                                        )
                                        Text(rule, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text("Setup Tags", style = MaterialTheme.typography.labelSmall, color = ext.textMuted)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            StandardSetupTags.all.forEach { tag ->
                                val picked = selectedSetupTags.contains(tag)
                                FilterChip(
                                    selected = picked,
                                    onClick = {
                                        selectedSetupTags = if (picked) selectedSetupTags - tag else selectedSetupTags + tag
                                    },
                                    label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }

                // Psychology, Mistakes & Rating Card + Bottom Save CTA
                item(key = "psychology_card") {
                    QuantCard(modifier = Modifier.fillMaxWidth().widthIn(max = maxContentWidth)) {
                        Text("Psychology & Mistakes", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))

                        Text("Mindset Before Entry", style = MaterialTheme.typography.labelSmall, color = ext.textMuted)
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EmotionTag.entries.forEach { emo ->
                                FilterChip(
                                    selected = selectedEmotion == emo,
                                    onClick = { selectedEmotion = emo },
                                    label = { Text(emo.label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text("Mistakes (Quantified in Analytics)", style = MaterialTheme.typography.labelSmall, color = ext.warning)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            StandardMistakeTags.all.forEach { tag ->
                                val picked = selectedMistakes.contains(tag)
                                FilterChip(
                                    selected = picked,
                                    onClick = {
                                        selectedMistakes = if (picked) selectedMistakes - tag else selectedMistakes + tag
                                    },
                                    label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Execution Quality Rating", style = MaterialTheme.typography.titleSmall)
                            Row {
                                for (star in 1..5) {
                                    IconButton(
                                        onClick = { rating = star },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                            contentDescription = "Rate $star stars",
                                            tint = if (star <= rating) ext.warning else ext.textMuted
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { submitTrade() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("editor_save_bottom_button")
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (isEditing) "Update Trade Record" else "Save Trade to Cloud Journal")
                        }
                    }
                }

                item(key = "editor_bottom_spacer") {
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
