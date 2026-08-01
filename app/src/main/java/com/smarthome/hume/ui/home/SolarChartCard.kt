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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.ui.theme.HumeColors

/** One bar of the weekly solar chart. */
data class DayValue(val label: String, val value: Double, val isToday: Boolean = false)

/** Weekly production card: salmon bars with a dark trend line on top. */
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
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = HumeColors.TextPrimary)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(totalText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = HumeColors.TextPrimary)
                    Spacer(Modifier.padding(horizontal = 2.dp))
                    Text(unitText, fontSize = 11.sp, color = HumeColors.TextSecondary)
                }
            }
            Spacer(Modifier.height(14.dp))
            if (days.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Text(
                        emptyHint ?: "Ch\u01b0a c\u00f3 d\u1eef li\u1ec7u",
                        style = MaterialTheme.typography.bodySmall,
                        color = HumeColors.TextSecondary,
                    )
                }
            } else {
                WeeklyBars(days)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth()) {
                    days.forEach { day ->
                        Text(
                            day.label,
                            fontSize = 10.sp,
                            color = HumeColors.TextSecondary,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyBars(days: List<DayValue>) {
    val bar = HumeColors.SalmonSoft
    val barToday = HumeColors.Orange
    val line = Color(0xFF5B5350)
    Canvas(Modifier.fillMaxWidth().height(120.dp)) {
        val count = days.size
        if (count == 0) return@Canvas
        val slot = size.width / count
        val barWidth = slot * 0.62f
        val max = days.maxOf { it.value }.takeIf { it > 0.0 } ?: 1.0
        val top = size.height * 0.22f
        val points = ArrayList<Offset>(count)

        days.forEachIndexed { index, day ->
            val ratio = (day.value / max).coerceIn(0.12, 1.0).toFloat()
            val barHeight = (size.height - top) * ratio
            val left = index * slot + (slot - barWidth) / 2f
            val barTop = size.height - barHeight
            drawRoundRect(
                color = if (day.isToday) barToday else bar,
                topLeft = Offset(left, barTop),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2.6f, barWidth / 2.6f),
            )
            points += Offset(left + barWidth / 2f, barTop - top * 0.30f)
        }

        val path = Path()
        points.forEachIndexed { index, point ->
            if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        drawPath(path, line, style = Stroke(width = 2.5f))
        points.forEach { point ->
            drawCircle(Color.White, radius = 6f, center = point)
            drawCircle(line, radius = 6f, center = point, style = Stroke(width = 2.5f))
        }
    }
}
