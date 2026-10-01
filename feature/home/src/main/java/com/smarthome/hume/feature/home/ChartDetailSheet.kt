package com.smarthome.hume.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.ui.components.HorizontalBatteryIcon
import com.smarthome.hume.core.ui.components.M3EIcons
import com.smarthome.hume.core.ui.components.Ms
import com.smarthome.hume.core.ui.components.MsIcon
import com.smarthome.hume.core.ui.theme.LocalHumeExtraColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/** Loai sheet bieu do chi tiet. */
enum class ChartDetailType { Battery, Solar }

/** Nguon du lieu cua tung bieu do trong sheet lich su. */
enum class ChartHistorySeries { BatterySoc, BatteryPower, SolarPower }

typealias ChartHistoryLoader = suspend (
    series: ChartHistorySeries,
    startMs: Long,
    endMs: Long,
) -> List<Pair<Long, Double>>

/**
 * Sheet bieu do chi tiet khi cham vao the Hieu nang pin / Dien mat troi.
 * - Pin: bieu do vung SOC 24h + bieu do cot cong suat sac/xa
 * - Solar: bieu do vung cong suat 24h
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartDetailSheet(
    type: ChartDetailType,
    onDismiss: () -> Unit,
    loadHistory: ChartHistoryLoader = { _, _, _ -> emptyList() },
) {
    val cs = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 44.dp, topEnd = 44.dp),
        containerColor = cs.surfaceContainer,
        dragHandle = { GrabHandle() },
    ) {
        val maxSheetH = (LocalConfiguration.current.screenHeightDp * 0.85f).dp
        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .heightIn(max = maxSheetH),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (type == ChartDetailType.Battery) {
                        HorizontalBatteryIcon(
                            soc = 100,
                            color = cs.primary,
                            modifier = Modifier.size(width = 34.dp, height = 20.dp),
                        )
                    } else {
                        MsIcon(
                            M3EIcons.SolarPower,
                            null,
                            tint = cs.primary,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            if (type == ChartDetailType.Battery) "Hiệu năng pin" else "Điện mặt trời",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Medium,
                            color = cs.onSurface,
                        )
                        Text(
                            "Lịch sử 24 giờ qua",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant,
                        )
                    }
                }
            }
            if (type == ChartDetailType.Battery) {
                item {
                    BatterySocChart(loadHistory)
                }
                item {
                    BatteryPowerChart(loadHistory)
                }
            } else {
                item {
                    SolarPowerChart(loadHistory)
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun BatterySocChart(loadHistory: ChartHistoryLoader) {
    val cs = MaterialTheme.colorScheme
    var points by remember { mutableStateOf<List<Pair<Long, Double>>?>(null) }
    LaunchedEffect(Unit) {
        val now = System.currentTimeMillis()
        val data = runCatching {
            loadHistory(ChartHistorySeries.BatterySoc, now - 24 * 3600 * 1000L, now)
        }.getOrNull()
        points = data?.takeIf { it.isNotEmpty() }
    }
    ChartCard(title = "Dung lượng pin (SOC)", unit = "%") {
        val p = points
        if (p == null) {
            LoadingChart()
        } else {
            val vals = p.map { it.second.toFloat() }
            val times = p.map { it.first }
            AreaChart(
                vals = vals,
                times = times,
                color = LocalHumeExtraColors.current.success,
                fillAlpha = 0.25f,
            )
        }
    }
}

@Composable
private fun BatteryPowerChart(loadHistory: ChartHistoryLoader) {
    val cs = MaterialTheme.colorScheme
    var points by remember { mutableStateOf<List<Pair<Long, Double>>?>(null) }
    LaunchedEffect(Unit) {
        val now = System.currentTimeMillis()
        val data = runCatching {
            loadHistory(ChartHistorySeries.BatteryPower, now - 24 * 3600 * 1000L, now)
        }.getOrNull()
        // Giam mat do diem de cot khong qua day (lay 48 diem)
        points = data?.takeIf { it.isNotEmpty() }?.let { downsample(it, 48) }
    }
    ChartCard(title = "Công suất sạc / xả", unit = "W") {
        val p = points
        if (p == null) {
            LoadingChart()
        } else {
            val vals = p.map { it.second.toFloat() }
            val times = p.map { it.first }
            PowerBarChart(
                vals = vals,
                times = times,
                posColor = LocalHumeExtraColors.current.success,
                negColor = cs.error,
            )
        }
    }
}

@Composable
private fun SolarPowerChart(loadHistory: ChartHistoryLoader) {
    val cs = MaterialTheme.colorScheme
    var points by remember { mutableStateOf<List<Pair<Long, Double>>?>(null) }
    LaunchedEffect(Unit) {
        val now = System.currentTimeMillis()
        val data = runCatching {
            loadHistory(ChartHistorySeries.SolarPower, now - 24 * 3600 * 1000L, now)
        }.getOrNull()
        points = data?.takeIf { it.isNotEmpty() }?.let { downsample(it, 96) }
    }
    ChartCard(title = "Công suất phát điện", unit = "W") {
        val p = points
        if (p == null) {
            LoadingChart()
        } else {
            val vals = p.map { (it.second / 1000.0).toFloat() } // W -> kW cho de doc
            val times = p.map { it.first }
            AreaChart(
                vals = vals,
                times = times,
                color = Color(0xFFF59E0B),
                fillAlpha = 0.3f,
                unit = "kW",
            )
        }
    }
}

private fun downsample(points: List<Pair<Long, Double>>, max: Int): List<Pair<Long, Double>> {
    if (points.size <= max) return points
    val step = points.size.toFloat() / max
    return List(max) { i -> points[(i * step).toInt().coerceIn(0, points.size - 1)] }
}

@Composable
private fun ChartCard(
    title: String,
    unit: String,
    content: @Composable () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(cs.surfaceContainerHighest)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurface,
            )
            Text(
                unit,
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun LoadingChart() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            strokeWidth = 3.dp,
        )
    }
}

/**
 * Bieu do vung (area): duong cong + fill gradient.
 * Dung cho SOC pin, cong suat solar.
 */
@Composable
private fun AreaChart(
    vals: List<Float>,
    times: List<Long>,
    color: Color,
    fillAlpha: Float = 0.25f,
    unit: String = "",
) {
    val cs = MaterialTheme.colorScheme
    if (vals.isEmpty()) return
    val maxV = (vals.maxOrNull() ?: 0f).coerceAtLeast(0.01f)
    val minV = vals.minOrNull() ?: 0f
    val range = (maxV - minV).coerceAtLeast(0.01f)
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val padL = 8.dp.toPx()
                val padR = 8.dp.toPx()
                val padT = 12.dp.toPx()
                val padB = 8.dp.toPx()
                val cw = w - padL - padR
                val ch = h - padT - padB
                fun x(i: Int): Float = padL + cw * i / (vals.size - 1).coerceAtLeast(1)
                fun y(v: Float): Float = padT + ch * (1f - (v - minV) / range)

                // Fill area (duong cong muot bang cubic bezier)
                val fillPath = Path().apply {
                    moveTo(x(0), y(vals[0]))
                    for (i in 1 until vals.size) {
                        val midX = (x(i - 1) + x(i)) / 2f
                        cubicTo(midX, y(vals[i - 1]), midX, y(vals[i]), x(i), y(vals[i]))
                    }
                    lineTo(x(vals.size - 1), padT + ch)
                    lineTo(x(0), padT + ch)
                    close()
                }
                drawPath(
                    fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            color.copy(alpha = fillAlpha),
                            color.copy(alpha = 0.02f),
                        ),
                    ),
                )
                // Line (duong cong muot)
                val linePath = Path().apply {
                    moveTo(x(0), y(vals[0]))
                    for (i in 1 until vals.size) {
                        val midX = (x(i - 1) + x(i)) / 2f
                        cubicTo(midX, y(vals[i - 1]), midX, y(vals[i]), x(i), y(vals[i]))
                    }
                }
                drawPath(
                    linePath,
                    color = color,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
                )
                // Max point dot
                val maxIdx = vals.indexOf(maxV)
                if (maxIdx >= 0) {
                    drawCircle(
                        color = color,
                        radius = 4.dp.toPx(),
                        center = Offset(x(maxIdx), y(vals[maxIdx])),
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = Offset(x(maxIdx), y(vals[maxIdx])),
                    )
                }
            }
            // Max value label
            Text(
                "${"%.1f".format(maxV)}$unit",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurface,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(8.dp))
                    .background(cs.surfaceContainerHigh)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        // X labels: 4 moc thoi gian
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val fmt = remember { SimpleDateFormat("HH:mm", Locale.US) }
            listOf(0, vals.size / 3, vals.size * 2 / 3, vals.size - 1).distinct().forEach { i ->
                val idx = i.coerceIn(0, times.size - 1)
                Text(
                    fmt.format(Date(times[idx])),
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Bieu do cot cong suat: cot duong (sac) / cot am (xa), duong zero o giua.
 */
@Composable
private fun PowerBarChart(
    vals: List<Float>,
    times: List<Long>,
    posColor: Color,
    negColor: Color,
) {
    val cs = MaterialTheme.colorScheme
    if (vals.isEmpty()) return
    val maxAbs = vals.maxOf { abs(it) }.coerceAtLeast(0.01f)
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val padT = 8.dp.toPx()
                val padB = 8.dp.toPx()
                val ch = h - padT - padB
                val zeroY = padT + ch / 2f
                val bw = w / vals.size
                // Zero line
                drawLine(
                    color = cs.outlineVariant,
                    start = Offset(0f, zeroY),
                    end = Offset(w, zeroY),
                    strokeWidth = 1.dp.toPx(),
                )
                vals.forEachIndexed { i, v ->
                    val bh = (abs(v) / maxAbs) * (ch / 2f) * 0.92f
                    val top = if (v >= 0) zeroY - bh else zeroY
                    drawRoundRect(
                        color = if (v >= 0) posColor else negColor,
                        topLeft = Offset(i * bw + bw * 0.2f, top),
                        size = androidx.compose.ui.geometry.Size(bw * 0.6f, bh.coerceAtLeast(2.dp.toPx())),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                    )
                }
            }
        }
        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            LegendDot(color = posColor, label = "Sạc")
            LegendDot(color = negColor, label = "Xả")
        }
        // X labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val fmt = remember { SimpleDateFormat("HH:mm", Locale.US) }
            listOf(0, vals.size / 2, vals.size - 1).distinct().forEach { i ->
                val idx = i.coerceIn(0, times.size - 1)
                Text(
                    fmt.format(Date(times[idx])),
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = cs.onSurfaceVariant,
            modifier = Modifier.padding(start = 5.dp),
        )
    }
}
