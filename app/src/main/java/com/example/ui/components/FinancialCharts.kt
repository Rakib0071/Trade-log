package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.EdgeScoreBreakdown
import com.example.domain.EquityPoint
import com.example.domain.FormatUtils
import com.example.domain.RDistributionBucket
import com.example.ui.theme.EdgeLogThemeDefaults
import com.example.ui.theme.MonoTextStyles
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Interactive Cumulative Net P&L Equity Curve with touch/drag crosshair inspection.
 */
@Composable
fun CumulativeEquityCurveChart(
    points: List<EquityPoint>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 190.dp
) {
    val ext = EdgeLogThemeDefaults.colors
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVar = MaterialTheme.colorScheme.onSurfaceVariant

    var selectedIdx by remember(points) { mutableStateOf<Int?>(null) }

    if (points.isEmpty()) {
        ChartEmptyPlaceholder("Log closed trades to plot your cumulative P&L curve", chartHeight)
        return
    }

    val activePoint = selectedIdx?.let { points.getOrNull(it) } ?: points.last()
    val isPositiveOverall = points.last().cumulativePnl >= 0.0
    val lineColor = if (isPositiveOverall) ext.profit else ext.loss

    Column(modifier = modifier.fillMaxWidth()) {
        // Live crosshair readout strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (activePoint.cumulativePnl >= 0) ext.profit else ext.loss)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (selectedIdx != null) activePoint.label else "Latest (${activePoint.label})",
                    style = MaterialTheme.typography.labelMedium,
                    color = onSurfaceVar
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${activePoint.tradeCount} trd",
                    style = MonoTextStyles.MicroBadge,
                    color = ext.textMuted
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Day ${FormatUtils.formatCurrency(activePoint.dailyPnl, signed = true, compact = true)}",
                    style = MonoTextStyles.MicroBadge,
                    color = if (activePoint.dailyPnl >= 0) ext.profit else ext.loss
                )
                Text(
                    text = FormatUtils.formatCurrency(activePoint.cumulativePnl, signed = true),
                    style = MonoTextStyles.CardMetric,
                    color = if (activePoint.cumulativePnl >= 0) ext.profit else ext.loss
                )
            }
        }

        val minVal = minOf(0.0, points.minOf { it.cumulativePnl })
        val maxVal = maxOf(0.0, points.maxOf { it.cumulativePnl })
        val range = (maxVal - minVal).takeIf { it > 0.01 } ?: 100.0
        val paddedMin = minVal - range * 0.08
        val paddedMax = maxVal + range * 0.08
        val totalSpan = (paddedMax - paddedMin).coerceAtLeast(1.0)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(points) {
                        detectTapGestures(
                            onTap = { offset ->
                                val idx = ((offset.x / size.width.coerceAtLeast(1)) * (points.size - 1))
                                    .roundToInt()
                                    .coerceIn(0, points.lastIndex)
                                selectedIdx = if (selectedIdx == idx) null else idx
                            }
                        )
                    }
                    .pointerInput(points) {
                        detectHorizontalDragGestures(
                            onDragEnd = { selectedIdx = null },
                            onDragCancel = { selectedIdx = null },
                            onHorizontalDrag = { change, _ ->
                                val idx = ((change.position.x / size.width.coerceAtLeast(1)) * (points.size - 1))
                                    .roundToInt()
                                    .coerceIn(0, points.lastIndex)
                                selectedIdx = idx
                            }
                        )
                    }
            ) {
                val w = size.width
                val h = size.height
                val padTop = 10f
                val padBottom = 10f
                val drawH = (h - padTop - padBottom).coerceAtLeast(1f)

                fun yFor(v: Double): Float {
                    val norm = ((v - paddedMin) / totalSpan).toFloat().coerceIn(0f, 1f)
                    return padTop + drawH * (1f - norm)
                }

                // Subtle horizontal grid lines
                val gridLines = 4
                for (i in 0..gridLines) {
                    val y = padTop + drawH * (i.toFloat() / gridLines)
                    drawLine(
                        color = ext.chartGrid,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f
                    )
                }

                // Dashed zero baseline
                val zeroY = yFor(0.0)
                drawLine(
                    color = onSurfaceVar.copy(alpha = 0.45f),
                    start = Offset(0f, zeroY),
                    end = Offset(w, zeroY),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                )

                val coords = points.mapIndexed { idx, pt ->
                    val x = if (points.size == 1) w * 0.5f else (idx.toFloat() / (points.size - 1)) * w
                    Offset(x, yFor(pt.cumulativePnl))
                }

                if (coords.size == 1) {
                    val c = coords.first()
                    drawCircle(color = lineColor.copy(alpha = 0.22f), radius = 16f, center = c)
                    drawCircle(color = lineColor, radius = 6f, center = c)
                } else {
                    val linePath = Path().apply {
                        moveTo(coords.first().x, coords.first().y)
                        for (i in 1 until coords.size) {
                            val prev = coords[i - 1]
                            val curr = coords[i]
                            val midX = (prev.x + curr.x) / 2f
                            cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                        }
                    }

                    val fillPath = Path().apply {
                        addPath(linePath)
                        lineTo(coords.last().x, zeroY)
                        lineTo(coords.first().x, zeroY)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                lineColor.copy(alpha = 0.30f),
                                lineColor.copy(alpha = 0.02f)
                            ),
                            startY = padTop,
                            endY = h
                        )
                    )

                    drawPath(
                        path = linePath,
                        color = lineColor,
                        style = Stroke(
                            width = 3.2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // Active / selected crosshair marker
                val focusIdx = selectedIdx ?: coords.lastIndex
                val focusOffset = coords[focusIdx]
                if (selectedIdx != null) {
                    drawLine(
                        color = onSurface.copy(alpha = 0.35f),
                        start = Offset(focusOffset.x, padTop),
                        end = Offset(focusOffset.x, h - padBottom),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )
                }
                val dotColor = if (points[focusIdx].cumulativePnl >= 0) ext.profit else ext.loss
                drawCircle(color = dotColor.copy(alpha = 0.25f), radius = 11.dp.toPx(), center = focusOffset)
                drawCircle(color = dotColor, radius = 4.5.dp.toPx(), center = focusOffset)
            }
        }

        // X-axis first / middle / last date ticks
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(points.first().label, style = MonoTextStyles.MicroBadge, color = ext.textMuted)
            if (points.size > 2) {
                Text(points[points.size / 2].label, style = MonoTextStyles.MicroBadge, color = ext.textMuted)
            }
            Text(points.last().label, style = MonoTextStyles.MicroBadge, color = ext.textMuted)
        }
    }
}

/**
 * Interactive Daily Net P&L diverging bar chart.
 */
@Composable
fun DailyPnlBarChart(
    points: List<EquityPoint>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 190.dp
) {
    val ext = EdgeLogThemeDefaults.colors
    val onSurfaceVar = MaterialTheme.colorScheme.onSurfaceVariant
    var selectedIdx by remember(points) { mutableStateOf<Int?>(null) }

    if (points.isEmpty()) {
        ChartEmptyPlaceholder("No daily P&L bars yet", chartHeight)
        return
    }

    val recentPoints = points.takeLast(20)
    val active = selectedIdx?.let { recentPoints.getOrNull(it) } ?: recentPoints.last()
    val maxAbs = recentPoints.maxOf { abs(it.dailyPnl) }.coerceAtLeast(1.0)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${active.label} · ${active.tradeCount} trade${if (active.tradeCount == 1) "" else "s"}",
                style = MaterialTheme.typography.labelMedium,
                color = onSurfaceVar
            )
            Text(
                text = FormatUtils.formatCurrency(active.dailyPnl, signed = true),
                style = MonoTextStyles.CardMetric,
                color = if (active.dailyPnl >= 0) ext.profit else ext.loss
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .pointerInput(recentPoints) {
                    detectTapGestures { offset ->
                        val slotW = size.width.toFloat() / recentPoints.size.coerceAtLeast(1)
                        val idx = (offset.x / slotW).toInt().coerceIn(0, recentPoints.lastIndex)
                        selectedIdx = if (selectedIdx == idx) null else idx
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val midY = h / 2f
            val halfH = (h / 2f) - 10f

            drawLine(
                color = ext.chartGrid,
                start = Offset(0f, 10f),
                end = Offset(w, 10f),
                strokeWidth = 1f
            )
            drawLine(
                color = ext.chartGrid,
                start = Offset(0f, h - 10f),
                end = Offset(w, h - 10f),
                strokeWidth = 1f
            )
            drawLine(
                color = onSurfaceVar.copy(alpha = 0.5f),
                start = Offset(0f, midY),
                end = Offset(w, midY),
                strokeWidth = 1.5f
            )

            val slotW = w / recentPoints.size.coerceAtLeast(1)
            val barW = (slotW * 0.62f).coerceIn(4f, 36f)

            recentPoints.forEachIndexed { idx, pt ->
                val ratio = (abs(pt.dailyPnl) / maxAbs).toFloat().coerceIn(0.05f, 1f)
                val barH = ratio * halfH
                val x = idx * slotW + (slotW - barW) / 2f
                val isPos = pt.dailyPnl >= 0.0
                val isSelected = selectedIdx == null || selectedIdx == idx
                val color = (if (isPos) ext.profit else ext.loss).copy(alpha = if (isSelected) 0.95f else 0.35f)
                val topY = if (isPos) midY - barH else midY
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, topY),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(recentPoints.first().label, style = MonoTextStyles.MicroBadge, color = ext.textMuted)
            Text("Tap any bar to inspect", style = MaterialTheme.typography.labelSmall, color = ext.textMuted)
            Text(recentPoints.last().label, style = MonoTextStyles.MicroBadge, color = ext.textMuted)
        }
    }
}

/**
 * Underwater Drawdown Chart showing peak-to-trough percentage drop over time.
 */
@Composable
fun UnderwaterDrawdownChart(
    points: List<EquityPoint>,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 140.dp
) {
    val ext = EdgeLogThemeDefaults.colors
    if (points.isEmpty()) {
        ChartEmptyPlaceholder("No drawdown history recorded yet", chartHeight)
        return
    }
    val maxDd = points.maxOf { it.drawdownPct }.coerceAtLeast(1.0)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight)
    ) {
        val w = size.width
        val h = size.height
        val padTop = 8f
        val padBottom = 8f
        val drawH = (h - padTop - padBottom).coerceAtLeast(1f)

        // Zero peak ceiling
        drawLine(
            color = ext.chartGrid,
            start = Offset(0f, padTop),
            end = Offset(w, padTop),
            strokeWidth = 1.5f
        )
        drawLine(
            color = ext.chartGrid,
            start = Offset(0f, padTop + drawH * 0.5f),
            end = Offset(w, padTop + drawH * 0.5f),
            strokeWidth = 1f
        )

        val coords = points.mapIndexed { i, pt ->
            val x = if (points.size == 1) w * 0.5f else (i.toFloat() / (points.size - 1)) * w
            val y = padTop + ((pt.drawdownPct / maxDd).toFloat().coerceIn(0f, 1f)) * drawH
            Offset(x, y)
        }

        if (coords.size >= 2) {
            val line = Path().apply {
                moveTo(coords.first().x, coords.first().y)
                for (i in 1 until coords.size) {
                    lineTo(coords[i].x, coords[i].y)
                }
            }
            val fill = Path().apply {
                moveTo(coords.first().x, padTop)
                coords.forEach { lineTo(it.x, it.y) }
                lineTo(coords.last().x, padTop)
                close()
            }
            drawPath(
                path = fill,
                brush = Brush.verticalGradient(
                    colors = listOf(ext.loss.copy(alpha = 0.08f), ext.loss.copy(alpha = 0.38f)),
                    startY = padTop,
                    endY = h
                )
            )
            drawPath(
                path = line,
                color = ext.loss,
                style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        } else {
            drawCircle(color = ext.loss, radius = 5.dp.toPx(), center = coords.first())
        }
    }
}

/**
 * 5-Axis Edge Score Radar Chart (TradeZella Zella Score style).
 */
@Composable
fun EdgeScoreRadarChart(
    score: EdgeScoreBreakdown,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 176.dp
) {
    val ext = EdgeLogThemeDefaults.colors
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(score) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(550, easing = FastOutSlowInEasing))
    }

    val axes = listOf(
        "Win %" to score.winRateScore,
        "Profit F." to score.profitFactorScore,
        "Payoff" to score.payoffRatioScore,
        "Consist." to score.consistencyScore,
        "Discipl." to score.disciplineScore
    )

    Box(
        modifier = modifier.size(sizeDp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = (minOf(size.width, size.height) / 2f) * 0.78f
            val rings = 4
            val count = axes.size

            fun vertex(index: Int, r: Float): Offset {
                val angle = -PI / 2.0 + (2.0 * PI * index / count)
                return Offset(
                    cx + (r * cos(angle)).toFloat(),
                    cy + (r * sin(angle)).toFloat()
                )
            }

            // Concentric pentagon grid rings
            for (ring in 1..rings) {
                val r = radius * (ring.toFloat() / rings)
                val ringPath = Path().apply {
                    for (i in 0 until count) {
                        val pt = vertex(i, r)
                        if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                    }
                    close()
                }
                drawPath(
                    path = ringPath,
                    color = ext.chartGrid,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Spokes
            for (i in 0 until count) {
                drawLine(
                    color = ext.chartGrid,
                    start = Offset(cx, cy),
                    end = vertex(i, radius),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Data polygon
            val progress = animProgress.value
            val polyPts = axes.mapIndexed { idx, (_, rawScore) ->
                val norm = ((rawScore / 100f).coerceIn(0.08f, 1f)) * progress
                vertex(idx, radius * norm)
            }

            val polyPath = Path().apply {
                polyPts.forEachIndexed { idx, pt ->
                    if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                }
                close()
            }

            drawPath(
                path = polyPath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        ext.profit.copy(alpha = 0.42f),
                        ext.accentIndigo.copy(alpha = 0.26f)
                    ),
                    center = Offset(cx, cy),
                    radius = radius
                )
            )
            drawPath(
                path = polyPath,
                color = ext.profit,
                style = Stroke(width = 2.2.dp.toPx(), join = StrokeJoin.Round)
            )

            polyPts.forEach { pt ->
                drawCircle(color = ext.profit, radius = 3.5.dp.toPx(), center = pt)
            }
        }
    }
}

/**
 * Circular Semi-Donut Win Rate Gauge.
 */
@Composable
fun WinRateDonutGauge(
    winRatePct: Double,
    wins: Int,
    losses: Int,
    modifier: Modifier = Modifier,
    gaugeSize: Dp = 64.dp
) {
    val ext = EdgeLogThemeDefaults.colors
    val hasData = (wins + losses) > 0
    val sweep = ((winRatePct / 100.0).coerceIn(0.0, 1.0) * 360.0).toFloat()

    Box(
        modifier = modifier.size(gaugeSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 6.dp.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val topLeft = Offset(inset, inset)

            // Background loss/empty ring
            drawArc(
                color = if (hasData) ext.loss.copy(alpha = 0.35f) else ext.chartGrid,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            if (hasData && sweep > 0f) {
                drawArc(
                    color = ext.profit,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
        Text(
            text = if (hasData) "${winRatePct.roundToInt()}%" else "—",
            style = MonoTextStyles.MicroBadge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * R-Multiple Distribution Histogram Bar Chart.
 */
@Composable
fun RMultipleHistogramChart(
    buckets: List<RDistributionBucket>,
    modifier: Modifier = Modifier
) {
    val ext = EdgeLogThemeDefaults.colors
    val maxCount = max(1, buckets.maxOfOrNull { it.count } ?: 1)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(128.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            buckets.forEach { bucket ->
                val heightFraction = (bucket.count.toFloat() / maxCount).coerceIn(0f, 1f)
                val barColor = if (bucket.isPositive) ext.profit else ext.loss
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    if (bucket.count > 0) {
                        Text(
                            text = bucket.count.toString(),
                            style = MonoTextStyles.MicroBadge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(3.dp))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.74f)
                            .height(((heightFraction * 88f).coerceAtLeast(if (bucket.count > 0) 8f else 3f)).dp)
                            .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                            .background(if (bucket.count > 0) barColor else ext.chartGrid)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = bucket.label,
                        style = MonoTextStyles.CalendarCell,
                        color = ext.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartEmptyPlaceholder(message: String, height: Dp) {
    val ext = EdgeLogThemeDefaults.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = ext.textMuted,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}
