package com.smarthome.hume.ui.energy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.storage.DailySnapshotStore
import com.smarthome.hume.ui.theme.HumeColors
import java.util.Locale
import kotlin.math.max
import kotlin.math.round

/** One column of the week chart, same shape as SolarDayData in SwiftUI. */
data class WeekDay(val name: String, val value: Double, val isToday: Boolean)

/**
 * Port of EnergyWeekChart + WeekBarLineChart (Views/Energy/EnergyView.swift and
 * Views/Home/2_Energy/SolarEnergyCardView.swift).
 *
 * Six past days come from the snapshot cache (filled once from history), the
 * seventh column is today and stays live off the websocket state.
 */
@Composable
fun EnergyWeekChart(
    ha: HomeAssistantRepository,
    title: String,
    entityId: String,
    unit: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val store = remember { DailySnapshotStore.get(context) }
    val entities by ha.entities.collectAsState()
    var past by remember(entityId) { mutableStateOf<List<WeekDay>>(emptyList()) }

    val todayLive = round((entities[entityId]?.numericState ?: 0.0) * 10) / 10

    LaunchedEffect(entityId) {
        val now = System.currentTimeMillis()
        val todayStart = DailySnapshotStore.startOfDay(now)
        val missing = (1..6).map { DailySnapshotStore.startOfDay(now, -it) }
            .filter { store.get(entityId, it) == null }
        if (missing.isNotEmpty()) {
            // One history call covers the whole week; daily counters are read as the
            // day maximum, the same value the iOS task group collects per day.
            val points = runCatching { ha.fetchHistory(entityId, hours = 24 * 7) }.getOrDefault(emptyList())
            missing.forEach { dayStart ->
                val dayEnd = dayStart + 24L * 60L * 60L * 1000L
                val dayPoints = points.filter { it.timeMs in dayStart until dayEnd }
                if (dayPoints.isNotEmpty()) {
                    store.set(entityId, dayStart, dayPoints.maxOf { it.value })
                }
            }
            store.prune()
        }
        past = (6 downTo 1).map { offset ->
            val dayStart = DailySnapshotStore.startOfDay(now, -offset)
            val value = store.get(entityId, dayStart) ?: 0.0
            WeekDay(DailySnapshotStore.dayLabel(dayStart), round(value * 10) / 10, false)
        }
    }

    val data = past + WeekDay(DailySnapshotStore.dayLabel(System.currentTimeMillis()), todayLive, true)

    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = HumeColors.TextPrimary, modifier = Modifier.weight(1f))
            Text(
                String.format(Locale.US, "%.1f", todayLive),
                fontSize = 22.sp,
                color = HumeColors.TextPrimary,
            )
            Spacer(Modifier.padding(horizontal = 1.dp))
            Text(unit, fontSize = 14.sp, color = HumeColors.TextSecondary)
        }
        Spacer(Modifier.height(16.dp))
        WeekBars(data)
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            data.forEach { day ->
                Text(
                    day.name,
                    fontSize = 11.sp,
                    color = HumeColors.TextSecondary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Bars + smoothed line + dots, the WeekBarLineChart drawing on iOS. */
@Composable
private fun WeekBars(data: List<WeekDay>) {
    val maxVal = max(data.maxOfOrNull { it.value } ?: 5.0, 5.0)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(184.dp)
            .padding(horizontal = 4.dp),
    ) {
        if (data.isEmpty()) return@Canvas
        val spacing = 4.dp.toPx()
        val count = data.size
        val barW = (size.width - spacing * (count - 1)) / count
        val plotH = size.height * 0.92f
        val radius = 8.dp.toPx()

        val tops = data.mapIndexed { index, day ->
            val ratio = (day.value / (maxVal * 1.15)).toFloat().coerceIn(0f, 1f)
            val barH = if (day.value > 0) max(4.dp.toPx(), ratio * plotH) else 2.dp.toPx()
            val left = index * (barW + spacing)
            val top = plotH - barH
            drawRoundRect(
                color = if (day.isToday) HumeColors.Orange else HumeColors.Orange.copy(alpha = 0.38f),
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(barW, barH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
            )
            Offset(left + barW / 2f, top)
        }

        if (tops.size >= 2) {
            val path = Path()
            path.moveTo(tops[0].x, tops[0].y)
            for (i in 0 until tops.size - 1) {
                val p0 = tops[max(i - 1, 0)]
                val p1 = tops[i]
                val p2 = tops[i + 1]
                val p3 = tops[minOf(i + 2, tops.size - 1)]
                val c1 = Offset(p1.x + (p2.x - p0.x) / 6f, p1.y + (p2.y - p0.y) / 6f)
                val c2 = Offset(p2.x - (p3.x - p1.x) / 6f, p2.y - (p3.y - p1.y) / 6f)
                path.cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
            }
            drawPath(path, color = HumeColors.Ink.copy(alpha = 0.45f), style = Stroke(width = 1.5.dp.toPx()))
        }
        tops.forEach { point ->
            drawCircle(Color.White, radius = 3.5.dp.toPx(), center = point)
            drawCircle(HumeColors.Ink, radius = 3.5.dp.toPx(), center = point, style = Stroke(width = 2.dp.toPx()))
        }
    }
}
