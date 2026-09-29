package com.smarthome.hume.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
 * - Cot 3 mau: hom nay = primary dac, tren TB = tertiary, duoi TB = surfaceContainerHigh + vien
 * - Duong TB dut net + nhan "TB x.x"
 * - Legend: Hom nay / Tren TB / Duoi TB
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
    if (vals.isEmpty()) return
    val maxValue = (vals.maxOrNull() ?: 0f).coerceAtLeast(0.01f)
    val avg = vals.average().toFloat()
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp),
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
        // Nhan "TB x.x" o dau TRAI duong TB (tranh de len cot cao ben phai)
        Box(
            Modifier
                .align(Alignment.TopStart)
                .offset(y = (avgY - 22.dp).coerceAtLeast(0.dp))
                .padding(start = 4.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(cs.surfaceContainerHighest.copy(alpha = 0.85f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        ) {
            Text(
                "TB ${"%.1f".format(avg)}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = cs.onSurfaceVariant,
                maxLines = 1,
            )
        }
        vals.forEachIndexed { i, v ->
            val today = i == vals.lastIndex
            val top = y(v)
            val h = (140.dp - top).coerceAtLeast(4.dp)
            val above = v >= avg
            // today: primary dac; tren TB: primaryContainer (khac biet ro voi today);
            // duoi TB: surfaceContainerHigh + vien. (tertiary trung primary o 1 so theme)
            val barColor = when {
                today -> cs.primary
                above -> cs.primaryContainer
                else -> cs.surfaceContainerHigh
            }
            Box(
                modifier = Modifier
                    .offset(x = x(i) - bw / 2, y = top)
                    .width(bw)
                    .height(h)
                    .clip(RoundedCornerShape(50))
                    .background(barColor)
                    .then(
                        if (!today && !above)
                            Modifier.border(1.5.dp, cs.outlineVariant, RoundedCornerShape(50))
                        else Modifier
                    )
                    .pointerInput(i) {
                        detectTapGestures(onTap = {
                            selected = if (selected == i) null else i
                        })
                    },
            )
            if (selected == i) {
                Box(
                    modifier = Modifier
                        .offset(x = x(i) - 60.dp, y = (top - 40.dp).coerceAtLeast(0.dp))
                        .width(120.dp)
                        .heightIn(min = 24.dp)
                        .shadow(6.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(cs.surfaceContainerHigh)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${labels.getOrElse(i) { "" }}: ${"%.1f".format(v)} kWh",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
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
    // Legend
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LegendItem(color = cs.primary, label = "Hôm nay")
        LegendItem(color = cs.primaryContainer, label = "Trên TB")
        LegendItem(color = cs.surfaceContainerHigh, label = "Dưới TB", border = true)
    }
}

@Composable
private fun LegendItem(color: androidx.compose.ui.graphics.Color, label: String, border: Boolean = false) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(10.dp)
                .height(10.dp)
                .clip(CircleShape)
                .background(color)
                .then(if (border) Modifier.border(1.dp, cs.outlineVariant, CircleShape) else Modifier),
        )
        Text(
            label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = cs.onSurfaceVariant,
            modifier = Modifier.padding(start = 5.dp),
        )
    }
}
