package com.smarthome.hume.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Thanh pin M3E — ban CHOT (demo battery-bar):
 * 1 thanh duy nhat chia 2 segment: Du tru = flat (trai) + Su dung = wavy (phai).
 *
 * Giai phau: [flat 10px][khe 4px][wavy 14px][khe 4px][track 10px][stop dot 4px]
 * - flat: width (100%-8px)*pr, primary
 * - wavy: left (100%-8px)*pr+4px, width (100%-8px)*pu; song: buoc 15px,
 *   bien 4, stroke 6; bo 2 dau bang container pill (clip)
 * - track: left (100%-8px)*(pr+pu)+8px, secondaryContainer
 * - pr = min(soc,20)/100, pu = max(0,soc-20)/100
 */
@Composable
fun WavyBatteryBar(
    soc: Int,
    modifier: Modifier = Modifier,
    reserveLimit: Int = 20,
) {
    val socC = soc.coerceIn(0, 100)
    val pr = minOf(socC, reserveLimit) / 100f
    val pu = maxOf(0, socC - reserveLimit) / 100f
    val primary = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.secondaryContainer
    val pill = RoundedCornerShape(50)

    BoxWithConstraints(
        modifier = modifier
            .height(14.dp)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(socC.toFloat(), 0f..100f) },
    ) {
        val w = maxWidth
        val flatW = (w - 8.dp) * pr
        val wavyL = (w - 8.dp) * pr + 4.dp
        val wavyW = (w - 8.dp) * pu
        val trackL = (w - 8.dp) * (pr + pu) + 8.dp
        val trackW = w - 4.dp - trackL

        // Segment flat: Du tru
        if (flatW > 0.dp) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .width(flatW)
                    .height(10.dp)
                    .clip(pill)
                    .background(primary),
            )
        }
        // Segment wavy: Su dung
        if (wavyW > 0.dp) {
            Canvas(
                Modifier
                    .offset(x = wavyL)
                    .align(Alignment.CenterStart)
                    .width(wavyW)
                    .height(14.dp)
                    .clip(pill),
            ) {
                val wavelength = 15.dp.toPx()
                val amplitude = 4.dp.toPx()
                val cy = size.height / 2f
                val path = Path().apply {
                    moveTo(0f, cy)
                    var x = 0f
                    var up = true
                    while (x < size.width) {
                        val cx = x + wavelength / 4f
                        val ex = x + wavelength / 2f
                        // control lech 2A -> dinh song dat A
                        quadraticBezierTo(
                            cx,
                            if (up) cy - 2f * amplitude else cy + 2f * amplitude,
                            ex,
                            cy,
                        )
                        x = ex
                        up = !up
                    }
                    lineTo(size.width, cy)
                }
                drawPath(
                    path = path,
                    color = primary,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
        // Track
        if (trackW > 0.dp) {
            Box(
                Modifier
                    .offset(x = trackL)
                    .align(Alignment.CenterStart)
                    .width(trackW)
                    .height(10.dp)
                    .clip(pill)
                    .background(trackColor),
            )
        }
        // Stop dot
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .size(4.dp)
                .background(primary, CircleShape),
        )
    }
}

/** Legend cham tron dac / icon song mini cho the pin. */
@Composable
fun BatteryLegend(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
            androidx.compose.material3.Text(
                text = "Dự trữ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(22.dp, 10.dp)) {
                val cy = size.height / 2f
                val path = Path().apply {
                    moveTo(0f, cy)
                    quadraticBezierTo(size.width * 0.25f, 0f, size.width * 0.5f, cy)
                    quadraticBezierTo(size.width * 0.75f, size.height, size.width, cy)
                }
                drawPath(path, MaterialTheme.colorScheme.primary, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            }
            androidx.compose.material3.Text(
                text = "Sử dụng",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}
