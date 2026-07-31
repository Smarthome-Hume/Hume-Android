package com.smarthome.hume.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.ui.theme.HumeColors

/** One bar of the weekly solar chart. */
data class DayValue(val label: String, val value: Double, val isToday: Boolean = false)

/**
 * Weekly solar production card from the prototype: rounded grey bars with the
 * current day highlighted, plus a thin line and dots across the bar tops.
 */
@Composable
fun SolarChartCard(
    title: String,
    totalText: String,
    unitText: String,
    days: List<DayValue>,
    emptyHint: String? = null,
) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(22.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HumeColors.TextPrimary,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(totalText, fontSize = 26.sp, fontWeight = FontWeight.Normal, color = HumeColors.TextPrimary)
                    Spacer(Modifier.padding(horizontal = 2.dp))
                    Text(unitText, fontSize = 14.sp, color = HumeColors.TextSecondary)
                }
            }
            Spacer(Modifier.height(18.dp))
            if (days.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                    Text(
                        emptyHint ?: "Ch\u01b0a c\u00f3 d\u1eef li\u1ec7u",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HumeColors.TextSecondary,
                    )
                }
            } else {
                WeeklyBars(days)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    days.forEach { day ->
                        Text(
                            day.label,
                            fontSize = 13.sp,
                            color = HumeColors.TextSecondary,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyBars(days: List<DayValue>) {
    val barColor = HumeColors.BarGrey
    val todayColor = HumeColors.AmberBar
    val lineColor = Color(0xFF9A928C)
    Canvas(Modifier.fillMaxWidth().height(150.dp)) {
        val count = days.size
        if (count == 0) return@Canvas
        val gap = size.width * 0.02f
        val barWidth = (size.width - gap * (count - 1)) / count
        val max = days.maxOf { it.value }.takeIf { it > 0.0 } ?: 1.0
        val topPadding = size.height * 0.18f
        val centers = ArrayList<Offset>(count)

        days.forEachIndexed { index, day ->
            val ratio = (day.value / max).coerceIn(0.25, 1.0).toFloat()
            val barHeight = (size.height - topPadding) * ratio
            val left = index * (barWidth + gap)
            val top = size.height - barHeight
            drawRoundRect(
                color = if (day.isToday) todayColor else barColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2.4f, barWidth / 2.4f),
            )
            centers += Offset(left + barWidth / 2f, top - topPadding * 0.35f)
        }

        val path = Path()
        centers.forEachIndexed { index, point ->
            if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        drawPath(path, lineColor, style = Stroke(width = 3f))
        centers.forEach { point ->
            drawCircle(Color.White, radius = 8f, center = point)
            drawCircle(lineColor, radius = 8f, center = point, style = Stroke(width = 3f))
        }
    }
}
