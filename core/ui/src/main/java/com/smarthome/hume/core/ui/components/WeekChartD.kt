package com.smarthome.hume.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Bieu do cot phuong an D (HTML demo hume-m3e-solar-charts.html):
 * - Mau cot theo dai gradient dam->nhat dua tren gia tri (cao nhat = primary dam nhat)
 * - Duong TB dut net + nhan "TB x.x"
 * - Cham cot hien tooltip gia tri
 */
@Composable
fun WeekChartD(
    vals: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    var selected by remember { mutableStateOf<Int?>(null) }
    // TB an mac dinh — an vao bieu do moi hien
    var showAvg by remember { mutableStateOf(false) }
    if (vals.isEmpty()) return
    val maxValue = (vals.maxOrNull() ?: 0f).coerceAtLeast(0.01f)
    val minValue = vals.minOrNull() ?: 0f
    val range = (maxValue - minValue).coerceAtLeast(0.01f)
    val avg = vals.average().toFloat()
    // Mau kinh mo: surface trong suot 70% — khong vien, khong shadow cung
    val glassBg = cs.surface.copy(alpha = 0.72f)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { showAvg = !showAvg })
            },
    ) {
        val w = maxWidth
        val sx = w / 320.dp
        fun x(i: Int): androidx.compose.ui.unit.Dp = (20f + i * (280f / 6f)).dp * sx
        fun y(v: Float): androidx.compose.ui.unit.Dp = 14.dp + 126.dp * (1f - (v / maxValue).coerceIn(0f, 1f))
        val bw = 30.dp * sx
        val avgY = y(avg)
        // Duong TB dut net (ve sau cot)
        Canvas(Modifier.fillMaxWidth().height(150.dp)) {
            val yPx = avgY.toPx()
            val marginPx = 20.dp.toPx() * sx
            drawLine(
                color = cs.onSurfaceVariant.copy(alpha = 0.7f),
                start = Offset(marginPx, yPx),
                end = Offset(size.width - marginPx, yPx),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
            )
        }
        vals.forEachIndexed { i, v ->
            val today = i == vals.lastIndex
            val top = y(v)
            val h = (140.dp - top).coerceAtLeast(4.dp)
            // Dai mau dam->nhat theo gia tri: cao nhat = primary dam, thap nhat = primaryContainer nhat
            val t = ((v - minValue) / range).coerceIn(0f, 1f)
            val barColor = androidx.compose.ui.graphics.lerp(cs.primaryContainer, cs.primary, t)
            Box(
                modifier = Modifier
                    .offset(x = x(i) - bw / 2, y = top)
                    .width(bw)
                    .height(h)
                    .clip(RoundedCornerShape(50))
                    .background(barColor)
                    .pointerInput(i) {
                        detectTapGestures(onTap = {
                            selected = if (selected == i) null else i
                        })
                    },
            )
            if (selected == i) {
                // Tooltip dang pill M3E: vien thuoc tron hoan toan, nen dac
                // surfaceContainerHigh + bong do (khong dung nen mo glass).
                Box(
                    modifier = Modifier
                        .offset(x = x(i) - 80.dp, y = (top - 44.dp).coerceAtLeast(0.dp))
                        .width(160.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .shadow(8.dp, CircleShape)
                            .clip(CircleShape)
                            .background(cs.surfaceContainerHigh)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${labels.getOrElse(i) { "" }}: ${"%.1f".format(v)} kWh",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = cs.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        // Nhan "TB x.x" — an mac dinh, an vao bieu do moi hien; nen kinh mo khong vien
        if (showAvg) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = (avgY - 22.dp).coerceAtLeast(0.dp))
                    .padding(end = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(glassBg)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    "TB ${"%.1f".format(avg)}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface,
                    maxLines = 1,
                )
            }
        }
    }
    Spacer(Modifier.height(2.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        labels.forEachIndexed { i, l ->
            val today = i == labels.lastIndex
            Text(
                l,
                fontSize = 10.5.sp,
                fontWeight = if (today) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = if (today) cs.primary else cs.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
