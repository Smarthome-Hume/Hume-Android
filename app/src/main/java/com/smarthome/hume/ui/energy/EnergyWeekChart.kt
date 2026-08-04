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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.smarthome.hume.core.ha.HistoryFetcher
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.storage.DailySnapshotStore
import com.smarthome.hume.ui.theme.HumeColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.Locale
import kotlin.math.max
import kotlin.math.round

/** One column of the week chart, same shape as SolarDayData in SwiftUI. */
data class WeekDay(val name: String, val value: Double, val isToday: Boolean)

/*
 * Soc cheo cua cot ngay cu — cung mot cong thuc voi the dien mat troi
 * (SolarChartCard) de ba bieu do nhin giong nhau: net 1dp, buoc 10dp.
 */
private val HatchStroke = 1.dp
private val HatchStep = 10.dp

/**
 * Port of EnergyWeekChart + WeekBarLineChart (Views/Energy/EnergyView.swift and
 * Views/Home/2_Energy/SolarEnergyCardView.swift).
 *
 * Sau ngay qua khu lay tu cache snapshot, cot thu bay la hom nay va chay truc
 * tiep theo websocket.
 *
 * TAI SAO TRUOC DAY THIEU DATA: ca tuan duoc hoi bang MOT request 168 gio tren
 * client dung chung voi kenh realtime (read timeout 20s). Home Assistant tra
 * cham hon the la request bi huy -> khong ngay nao co so. Nay moi ngay la mot
 * request rieng qua HistoryFetcher (timeout 60s, chay song song).
 *
 * TAT CA loi mang deu bi nuot tai cho: mot IOException nem ra tu async se lan
 * len LaunchedEffect va lam VANG APP, nen moi request duoc boc runCatching.
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
        runCatching {
            val now = System.currentTimeMillis()
            val dayMs = 24L * 60L * 60L * 1000L
            val missing = (1..6).map { DailySnapshotStore.startOfDay(now, -it) }
                .filter { store.get(entityId, it) == null }

            if (missing.isNotEmpty() && HistoryFetcher.isConfigured) {
                // Buoc 1: moi ngay mot request rieng, chay song song.
                coroutineScope {
                    missing.map { dayStart ->
                        async(Dispatchers.IO) {
                            val value = runCatching {
                                HistoryFetcher.fetchRange(entityId, dayStart, dayStart + dayMs)
                                    .maxOfOrNull { it.value } ?: 0.0
                            }.getOrDefault(0.0)
                            dayStart to value
                        }
                    }.awaitAll()
                }.forEach { (dayStart, value) ->
                    if (value > 0.0) store.set(entityId, dayStart, value)
                }
            }

            // Buoc 2: ngay nao van trong thi thu lai bang duong cu (mot lan 7 ngay).
            val stillMissing = missing.filter { store.get(entityId, it) == null }
            if (stillMissing.isNotEmpty()) {
                val points = runCatching { ha.fetchHistory(entityId, hours = 24 * 7) }
                    .getOrDefault(emptyList())
                stillMissing.forEach { dayStart ->
                    val dayPoints = points.filter { it.timeMs in dayStart until (dayStart + dayMs) }
                    if (dayPoints.isNotEmpty()) store.set(entityId, dayStart, dayPoints.maxOf { it.value })
                }
            }
            runCatching { store.prune() }

            past = (6 downTo 1).map { offset ->
                val dayStart = DailySnapshotStore.startOfDay(now, -offset)
                val value = store.get(entityId, dayStart) ?: 0.0
                WeekDay(DailySnapshotStore.dayLabel(dayStart), round(value * 10) / 10, false)
            }
        }
    }

    val data = past + WeekDay(DailySnapshotStore.dayLabel(System.currentTimeMillis()), todayLive, true)

    Column(modifier.fillMaxWidth()) {
        // HTML: header dung alignItems 'baseline' — so va don vi cung chan chu.
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            Text(
                title,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
                color = HumeColors.TextPrimary,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.weight(1f).alignByBaseline(),
            )
            Text(
                String.format(Locale.US, "%.1f", todayLive),
                fontSize = 22.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.SemiBold,
                color = HumeColors.TextPrimary,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.alignByBaseline(),
            )
            Spacer(Modifier.padding(horizontal = 1.dp))
            Text(
                unit,
                fontSize = 13.sp,
                lineHeight = 26.sp,
                color = HumeColors.TextSecondary,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.alignByBaseline(),
            )
        }
        Spacer(Modifier.height(14.dp))
        WeekBars(data)
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            data.forEach { day ->
                Text(
                    day.name,
                    fontSize = 11.sp,
                    color = if (day.isToday) HumeColors.Orange else HumeColors.TextSecondary,
                    fontWeight = if (day.isToday) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Bars + smoothed line + dots, the WeekBarLineChart drawing on iOS. */
@Composable
private fun WeekBars(data: List<WeekDay>) {
    // Never let the tallest bar touch the ceiling: the iOS chart keeps ~20%
    // headroom above the peak so the marker dot stays inside the plot area.
    val peak = data.maxOfOrNull { it.value } ?: 0.0
    val scale = max(peak * 1.2, 1.0)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(176.dp)
            .padding(horizontal = 4.dp),
    ) {
        if (data.isEmpty()) return@Canvas
        val spacing = 6.dp.toPx()
        val count = data.size
        val barW = (size.width - spacing * (count - 1)) / count
        val baseline = size.height - 2.dp.toPx()
        val plotH = baseline - 8.dp.toPx()
        val radius = 8.dp.toPx()
        val hatchStroke = HatchStroke.toPx()
        val hatchStep = HatchStep.toPx()

        val tops = data.mapIndexed { index, day ->
            val ratio = (day.value / scale).toFloat().coerceIn(0f, 1f)
            val barH = if (day.value > 0) max(6.dp.toPx(), ratio * plotH) else 0f
            val left = index * (barW + spacing)
            val top = baseline - barH
            if (barH > 0f) {
                drawRoundRect(
                    color = if (day.isToday) HumeColors.Orange else HumeColors.Orange.copy(alpha = 0.32f),
                    topLeft = Offset(left, top),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(radius, radius),
                )
                // Cot ngay cu: soc cheo canvas giong the dien mat troi.
                if (!day.isToday) {
                    val shape = Path().apply {
                        addRoundRect(
                            RoundRect(
                                left = left,
                                top = top,
                                right = left + barW,
                                bottom = baseline,
                                cornerRadius = CornerRadius(radius, radius),
                            ),
                        )
                    }
                    clipPath(shape) {
                        var x = left - barH
                        while (x < left + barW + barH) {
                            drawLine(
                                color = HumeColors.Orange.copy(alpha = 0.25f),
                                start = Offset(x, baseline),
                                end = Offset(x + barH, top),
                                strokeWidth = hatchStroke,
                            )
                            x += hatchStep
                        }
                    }
                }
            }
            Offset(left + barW / 2f, top)
        }

        // The trend line only makes sense once at least two days carry a value.
        val filled = data.count { it.value > 0 }
        if (filled >= 2) {
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
            drawPath(path, color = HumeColors.Orange.copy(alpha = 0.55f), style = Stroke(width = 2.dp.toPx()))
        }
        // A dot on a zero day would sit on the axis and look like a data point.
        data.forEachIndexed { index, day ->
            if (day.value <= 0) return@forEachIndexed
            val point = tops[index]
            drawCircle(Color.White, radius = 4.dp.toPx(), center = point)
            drawCircle(HumeColors.Orange, radius = 4.dp.toPx(), center = point, style = Stroke(width = 2.dp.toPx()))
        }
    }
}
