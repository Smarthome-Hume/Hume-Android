package com.smarthome.hume.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.glassSurface
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** One bar of the weekly chart (SolarDayData in SolarEnergyCardView.swift). */
data class DayValue(val label: String, val value: Double, val isToday: Boolean = false)

/** WeekBarLineChart geometry, verbatim from the original. */
private val BarSpacing = 4.dp
private val PlotHeight = 165.dp // chartH 220 * 0.75
private val BarColor = Color(0xFFF9784C)

/**
 * "\u0110i\u1ec7n m\u1eb7t tr\u1eddi" card: header with today's live total, then the week chart.
 *
 * Bieu do TRO LAI DUNG BAN CU: chi mot lop duy nhat cho moi ngay - cot bo goc
 * 8dp (hom nay dam, ngay cu nhat hon) cong duong xu huong Catmull-Rom va cham
 * rong tren moi dinh. Khong con lop nen chu nhat / gach cheo nao phia sau.
 */
@Composable
fun SolarChartCard(
    title: String,
    totalText: String,
    unitText: String,
    days: List<DayValue>,
    emptyHint: String? = null,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .glassSurface(radius = 32.dp)
            .padding(start = 12.dp, end = 8.dp, top = 16.dp, bottom = 16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = HumeColors.TextPrimary)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(totalText, fontSize = 22.sp, color = HumeColors.TextPrimary)
                Spacer(Modifier.width(2.dp))
                Text(unitText, fontSize = 14.sp, color = HumeColors.TextSecondary)
            }
        }
        if (days.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().height(PlotHeight),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    emptyHint ?: "\u0110ang t\u1ea3i...",
                    fontSize = 13.sp,
                    color = HumeColors.TextSecondary,
                )
            }
        } else {
            WeekBarLineChart(
                days = days,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun WeekBarLineChart(days: List<DayValue>, modifier: Modifier = Modifier) {
    var selected by remember(days.size) { mutableStateOf<Int?>(null) }
    val density = LocalDensity.current

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val count = days.size
        val totalWidth = maxWidth
        // barW = (w - spacing * (n - 1)) / n * 0.95, then the row is centred.
        val barWidth: Dp = (totalWidth - BarSpacing * (count - 1)) / count * 0.95f
        val rowWidth = barWidth * count + BarSpacing * (count - 1)
        val rowOffset = max(0f, (totalWidth - rowWidth).value / 2f).dp
        val maxValue = max(days.maxOf { it.value }, 5.0)

        /** Bar height in dp, using the original 1.15 head room. */
        fun barHeight(value: Double): Dp =
            if (value > 0.0) max(4f, (value / (maxValue * 1.15) * PlotHeight.value).toFloat()).dp else 2.dp

        /** Index under a horizontal touch position, in pixels. */
        fun indexAt(x: Float): Int {
            val step = with(density) { (barWidth + BarSpacing).toPx() }
            val start = with(density) { rowOffset.toPx() }
            val raw = ((x - start) / step).roundToInt()
            return min(max(raw, 0), count - 1)
        }

        Column {
            Box(Modifier.fillMaxWidth().height(PlotHeight)) {
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(PlotHeight)
                        .pointerInput(days) {
                            detectTapGestures(onPress = { offset -> selected = indexAt(offset.x) })
                        }
                        .pointerInput(days) {
                            detectDragGestures(
                                onDragStart = { offset -> selected = indexAt(offset.x) },
                                onDragEnd = { selected = null },
                                onDragCancel = { selected = null },
                            ) { change, _ -> selected = indexAt(change.position.x) }
                        }
                ) {
                    val barW = barWidth.toPx()
                    val spacing = BarSpacing.toPx()
                    val start = rowOffset.toPx()
                    val plot = size.height
                    val corner = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    val points = ArrayList<Offset>(count)

                    days.forEachIndexed { index, day ->
                        val h = barHeight(day.value).toPx()
                        val left = start + index * (barW + spacing)
                        val top = plot - h
                        // MOT LOP DUY NHAT: cot gia tri bo goc, khong co nen phia sau.
                        drawRoundRect(
                            color = if (day.isToday) BarColor else BarColor.copy(alpha = 0.38f),
                            topLeft = Offset(left, top),
                            size = Size(barW, h),
                            cornerRadius = corner,
                        )
                        // The line rides the value, not the clamped bar height.
                        val lineH = if (day.value > 0.0) {
                            (day.value / (maxValue * 1.15) * plot).toFloat()
                        } else 0f
                        points += Offset(left + barW / 2f, plot - lineH)
                    }

                    if (points.size >= 2) {
                        drawPath(
                            smoothPath(points),
                            HumeColors.TextPrimary.copy(alpha = 0.45f),
                            style = Stroke(width = 1.5.dp.toPx()),
                        )
                    }
                    points.forEachIndexed { index, point ->
                        val radius = if (selected == index) 3.5.dp.toPx() * 1.6f else 3.5.dp.toPx()
                        drawCircle(Color.White.copy(alpha = 0.25f), radius = radius, center = point)
                        drawCircle(
                            HumeColors.TextPrimary,
                            radius = radius,
                            center = point,
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    }
                }

                selected?.let { index ->
                    val day = days[index]
                    val centre = rowOffset + barWidth * index + BarSpacing * index + barWidth / 2f
                    val lineH = if (day.value > 0.0) {
                        (day.value / (maxValue * 1.15) * PlotHeight.value).toFloat().dp
                    } else 0.dp
                    val tipX = min(max(centre.value, 60f), max(totalWidth.value - 60f, 60f)).dp - 50.dp
                    val tipY = max((PlotHeight - lineH).value - 30f, 0f).dp
                    Box(
                        Modifier
                            .offset(x = tipX, y = tipY)
                            .glassSurface(radius = 12.dp)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            day.label + ": " + String.format(Locale.US, "%.1f", day.value) + " kWh",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HumeColors.TextPrimary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth().padding(start = rowOffset)) {
                days.forEachIndexed { index, day ->
                    if (index > 0) Spacer(Modifier.width(BarSpacing))
                    Text(
                        day.label,
                        fontSize = 11.sp,
                        color = HumeColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(barWidth),
                    )
                }
            }
        }
    }
}

/** SmoothLine in SolarEnergyCardView.swift: Catmull-Rom with tension 0.5. */
private fun smoothPath(points: List<Offset>, tension: Float = 0.5f): Path {
    val path = Path()
    if (points.size < 2) return path
    path.moveTo(points[0].x, points[0].y)
    for (i in 0 until points.size - 1) {
        val p0 = points[max(0, i - 1)]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = points[min(points.size - 1, i + 2)]
        val c1 = Offset(p1.x + (p2.x - p0.x) * tension / 3f, p1.y + (p2.y - p0.y) * tension / 3f)
        val c2 = Offset(p2.x - (p3.x - p1.x) * tension / 3f, p2.y - (p3.y - p1.y) * tension / 3f)
        path.cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
    }
    return path
}
