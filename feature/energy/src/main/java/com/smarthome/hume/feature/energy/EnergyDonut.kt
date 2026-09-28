package com.smarthome.hume.feature.energy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smarthome.hume.core.model.EnergyDonutSlice

/**
 * Donut: track surfaceHigh + slice (primary/tertiary/secondary),
 * stroke 16dp round cap, text o giua: tong kWh + "kWh hôm nay".
 */
@Composable
fun EnergyDonut(
    total: Double,
    slices: List<EnergyDonutSlice>,
    modifier: Modifier = Modifier,
) {
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
    )
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 16.dp.toPx()
            var start = -90f
            drawArc(
                color = track, startAngle = 0f, sweepAngle = 360f,
                useCenter = false, style = Stroke(stroke, cap = StrokeCap.Round),
            )
            slices.forEachIndexed { i, s ->
                val sweep = (s.fraction * 360).toFloat()
                if (sweep > 1f) {
                    drawArc(
                        color = colors[i % colors.size],
                        startAngle = start, sweepAngle = sweep - 3f,
                        useCenter = false, style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                String.format("%.1f", total),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                "kWh hôm nay",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
