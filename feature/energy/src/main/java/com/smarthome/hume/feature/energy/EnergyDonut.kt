package com.smarthome.hume.feature.energy

import androidx.compose.animation.core.Animatable
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
import kotlin.math.sin

/**
 * Donut: track surfaceHigh + slice (primary/tertiary/info),
 * stroke 16dp butt cap (de khe 3dp sach, khong bi round cap lan vao).
 * Khe giua cac slice: DO DAI CUNG CO DINH 3dp, ve truc tiep bang cach tru
 * gap khoi sweep moi slice (khong dung overlay) — port tu iOS d06e533.
 * Giua donut: mac dinh hien TONG kWh; khi user an vao slice nao thi hien
 * gia tri + ten cua slice do (an lai de ve tong).
 * Entrance / data update: ve tung slice stagger (draw 1.46s emphasized,
 * delay 180ms/slice) kem wobble (vot qua roi nay ve) — port tu iOS c5b64c6.
 */
@Composable
fun EnergyDonut(
    total: Double,
    slices: List<EnergyDonutSlice>,
    sliceColors: List<Color>,
    modifier: Modifier = Modifier,
) {
    val track = LocalHumeExtraColors.current.surfaceHigh
    // Khi data doi: ve lai tu dau kem wobble thay vi nhay cung —
    // port tu iOS c5b64c6. Animatable de snapTo(0) roi replay.
    val tAnim = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        tAnim.snapTo(0f)
        tAnim.animateTo(1f, animationSpec = tween(1460, easing = M3EMotion.emphasized))
    }
    val t = tAnim.value
    // Slice dang duoc chon (null = hien tong)
    var selected by remember { mutableStateOf<Int?>(null) }
    // Chuan hoa fractions de tong = 1 (tranh gap thua do lam tron) — nhu iOS;
    // dung chung cho ve + tap de khop nhau.
    val normFractions = remember(slices) {
        val sum = slices.sumOf { it.fraction }
        if (sum > 0) slices.map { it.fraction / sum } else slices.map { it.fraction }
    }
    // Goc bat dau (do) cua tung slice, tinh tu -90 (12h) theo chieu kim dong ho
    val sliceStarts = remember(normFractions) {
        val starts = mutableListOf<Float>()
        var acc = -90f
        normFractions.forEach { f ->
            starts.add(acc)
            acc += (f * 360).toFloat()
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
                    val sweep = (normFractions[i] * 360).toFloat()
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
            val r = minOf(size.width, size.height) / 2f - 8.dp.toPx()
            drawArc(
                color = track, startAngle = 0f, sweepAngle = 360f,
                useCenter = false, style = Stroke(stroke, cap = StrokeCap.Round),
            )
            // Khe co DO DAI CUNG CO DINH 3dp, khong phu thuoc so segment:
            // gapDeg = arcLength / r -> doi ra do. Tru deu 2 dau moi slice,
            // ve TRUC TIEP (khong dung overlay de len) — port tu iOS d06e533.
            val gapDeg = if (r > 0f) (3.dp.toPx() / r) * (180f / Math.PI.toFloat()) else 0f
            var start = -90f
            normFractions.forEachIndexed { i, f ->
                val fullSweep = (f * 360).toFloat()
                // Stagger + wobble (port tu iOS c5b64c6): slice vot qua roi
                // nay ve thay vi nhay cung khi data update.
                val rawLocal = ((t * 1460f - i * 180f) / 1100f).coerceIn(0f, 1f)
                val wobble = if (rawLocal < 1f) {
                    (sin(rawLocal * 12f + i) * 0.15 * (1f - rawLocal)).toFloat()
                } else 0f
                val local = (rawLocal + wobble).coerceIn(0f, 1.15f)
                val drawStart = start + gapDeg / 2f
                val drawSweep = maxOf(0f, fullSweep - gapDeg)
                val animSweep = drawSweep * local
                if (animSweep > 0.5f) {
                    drawArc(
                        color = sliceColors[i % sliceColors.size],
                        startAngle = drawStart, sweepAngle = animSweep,
                        // Butt cap: khe 3dp sach, khong bi round cap lan vao — nhu iOS
                        useCenter = false, style = Stroke(stroke, cap = StrokeCap.Butt),
                    )
                }
                start += fullSweep
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
