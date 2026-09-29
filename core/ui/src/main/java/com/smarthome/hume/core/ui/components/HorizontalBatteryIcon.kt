package com.smarthome.hume.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Icon pin nam ngang ve bang Canvas — hien dung muc % (min hon icon font chi co vai muc).
 * Kieu M3E: vien bo tron, phan sac day mau dac.
 */
@Composable
fun HorizontalBatteryIcon(
    soc: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val frac = (soc.coerceIn(0, 100)) / 100f
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val nubW = w * 0.08f
        val bodyW = w - nubW - w * 0.04f
        val strokeW = (h * 0.09f).coerceAtLeast(2.dp.toPx())
        val radius = h * 0.28f

        // Vien than pin
        drawRoundRect(
            color = color,
            topLeft = Offset(strokeW / 2, strokeW / 2),
            size = Size(bodyW - strokeW, h - strokeW),
            cornerRadius = CornerRadius(radius, radius),
            style = Stroke(width = strokeW),
        )
        // Num pin (cuc +)
        val nubH = h * 0.38f
        drawRoundRect(
            color = color,
            topLeft = Offset(bodyW + w * 0.02f, (h - nubH) / 2),
            size = Size(nubW, nubH),
            cornerRadius = CornerRadius(nubH / 2, nubH / 2),
        )
        // Phan tram pin (fill dac)
        if (frac > 0f) {
            val pad = strokeW * 1.6f
            val fillW = (bodyW - pad * 2) * frac
            if (fillW > 1f) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(pad, pad),
                    size = Size(fillW, h - pad * 2),
                    cornerRadius = CornerRadius(radius * 0.7f, radius * 0.7f),
                )
            }
        }
    }
}
