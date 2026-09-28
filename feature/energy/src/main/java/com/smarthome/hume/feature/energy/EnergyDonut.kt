package com.smarthome.hume.feature.energy

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.model.EnergyDonutSlice
import com.smarthome.hume.core.ui.components.M3EMotion
import com.smarthome.hume.core.ui.theme.LocalHumeExtraColors
import java.util.Locale

/**
 * Donut: track surfaceHigh + slice (primary/tertiary/info),
 * stroke 16dp round cap, text o giua: tong kWh + "kWh hôm nay".
 * Entrance: ve tung slice stagger (demo: draw 1.1s emphasized, delay 0/.18/.36).
 */
@Composable
fun EnergyDonut(
    total: Double,
    slices: List<EnergyDonutSlice>,
    sliceColors: List<Color>,
    modifier: Modifier = Modifier,
) {
    val track = LocalHumeExtraColors.current.surfaceHigh
    var play by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { play = true }
    val t by animateFloatAsState(
        targetValue = if (play) 1f else 0f,
        animationSpec = tween(1460, easing = M3EMotion.emphasized),
        label = "donutDraw",
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 16.dp.toPx()
            drawArc(
                color = track, startAngle = 0f, sweepAngle = 360f,
                useCenter = false, style = Stroke(stroke, cap = StrokeCap.Round),
            )
            var start = -90f
            slices.forEachIndexed { i, s ->
                val local = ((t * 1460f - i * 180f) / 1100f).coerceIn(0f, 1f)
                val sweep = (s.fraction * 360 * local).toFloat()
                if (sweep > 1f) {
                    drawArc(
                        color = sliceColors[i % sliceColors.size],
                        startAngle = start, sweepAngle = sweep,
                        useCenter = false, style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
                start += (s.fraction * 360).toFloat()
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                String.format(Locale.US, "%.1f", total),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFeatureSettings = "tnum",
                ),
            )
            Text(
                "kWh hôm nay",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
