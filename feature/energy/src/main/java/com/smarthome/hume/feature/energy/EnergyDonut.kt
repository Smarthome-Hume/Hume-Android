package com.smarthome.hume.feature.energy

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.model.EnergyDonutSlice
import com.smarthome.hume.core.ui.components.M3EMotion
import com.smarthome.hume.core.ui.theme.LocalHumeExtraColors
import java.util.Locale
import kotlin.math.atan2

/**
 * Donut: track surfaceHigh + slice (primary/tertiary/info),
 * stroke 16dp round cap.
 * Giua donut: mac dinh hien TONG kWh; khi user an vao slice nao thi hien
 * gia tri + ten cua slice do (an lai de ve tong).
 * Entrance: ve tung slice stagger (draw 1.1s emphasized, delay 0/.18/.36).
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
    // Slice dang duoc chon (null = hien tong)
    var selected by remember { mutableStateOf<Int?>(null) }
    // Goc bat dau (do) cua tung slice, tinh tu -90 (12h) theo chieu kim dong ho
    val sliceStarts = remember(slices) {
        val starts = mutableListOf<Float>()
        var acc = -90f
        slices.forEach { s ->
            starts.add(acc)
            acc += (s.fraction * 360).toFloat()
        }
        starts
    }
    Box(
        modifier = modifier.pointerInput(slices) {
            detectTapGestures { offset ->
                val w = size.width.toFloat()
                val h = size.height.toFloat()
                if (w <= 0 || h <= 0) return@detectTapGestures
                val dx = offset.x - w / 2f
                val dy = offset.y - h / 2f
                // Goc tap theo he drawArc: 0 = 3h, duong = cung chieu kim dong ho
                var ang = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                // Chuyen ve he bat dau tu -90
                var rel = ang - (-90f)
                while (rel < 0) rel += 360f
                while (rel >= 360) rel -= 360f
                // Tim slice chua goc nay
                val idx = slices.indexOfFirst { s ->
                    val i = slices.indexOf(s)
                    val start = sliceStarts[i] - (-90f)
                    val sweep = (s.fraction * 360).toFloat()
                    val sNorm = if (start < 0) start + 360f else start
                    rel >= sNorm && rel < sNorm + sweep
                }
                selected = if (idx == selected) null else idx.takeIf { it >= 0 }
            }
        },
        contentAlignment = Alignment.Center,
    ) {
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
            // Giua donut: slice duoc chon (gia tri + ten), mac dinh la TONG
            val selSlice = selected?.let { slices.getOrNull(it) }
            Text(
                if (selSlice != null)
                    String.format(Locale.US, "%.1f", total * selSlice.fraction)
                else
                    String.format(Locale.US, "%.1f", total),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum",
                ),
            )
            Text(
                selSlice?.name ?: "kWh",
                style = MaterialTheme.typography.labelSmall.copy( fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}
