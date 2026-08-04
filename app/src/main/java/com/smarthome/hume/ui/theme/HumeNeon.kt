package com.smarthome.hume.ui.theme

import android.graphics.BlurMaskFilter
import android.os.Build
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos

/*
 * HIEU UNG NEON - port theo bundle ban HTML cocopi-home.
 *
 *   @keyframes neonPulse {
 *     0%,100% { box-shadow: 0 0 8px  #f9784c4d, 0 0 16px #f9784c26, 0 0 30px #f9784c14 }
 *      20%    { box-shadow: 0 0 14px #f9784c99, 0 0 28px #f9784c4d, 0 0 50px #f9784c26 }
 *   }
 *
 * HAI DIEU KIEN BAT BUOC
 *
 * 1. TOA MUOT. `box-shadow: 0 0 Npx` la vet sang BLUR toa deu ra ngoai tu vien.
 *    KHONG duoc mo phong bang cach xep nhieu vong stroke dong tam - lam vay se
 *    hien ro van tung lop. Phai blur that:
 *      - khoi bo goc  -> BlurMaskFilter tren native canvas (dung 3 lop blur
 *        8/16/30dp giong dung ba lop box-shadow cua CSS)
 *      - khoi tron    -> Brush.radialGradient nhieu color stop, chuyen mau lien
 *        tuc nen khong bao gio co van
 *
 * 2. CUNG MOT NHIP. Moi cho phat sang trong app dung CHUNG rememberNeonBeat:
 *    pha duoc tinh tu dong ho khung hinh (withInfiniteAnimationFrameMillis) nen
 *    tat ca thanh phan sang/tat DONG PHA tuyet doi, khong lech nhau du duoc
 *    tao ra o cac thoi diem khac nhau.
 */

/** Chu ky nhip neon dung chung cho toan bo app. */
const val NEON_BEAT_PERIOD_MS = 1600

/**
 * Nhip neon dung chung: 0 -> 1 -> 0 theo duong cosin nen len xuong muot,
 * dong pha o moi noi vi pha tinh tu dong ho khung hinh.
 */
@Composable
fun rememberNeonBeat(periodMillis: Int = NEON_BEAT_PERIOD_MS): Float {
    val timeMs by produceState(0L) {
        while (true) {
            withInfiniteAnimationFrameMillis { value = it }
        }
    }
    val phase = (timeMs % periodMillis) / periodMillis.toFloat()
    return (0.5f - 0.5f * cos(phase * 2f * Math.PI.toFloat())).coerceIn(0f, 1f)
}

/** Mau cham bao trang thai mo trong ban HTML: #ff5252. */
val NeonDotRed = Color(0xFFFF5252)

/** Mau quang sang cam cua neonPulse trong ban HTML: #f9784c. */
val NeonGlowOrange = Color(0xFFF9784C)

/*
 * Ba lop blur cua box-shadow trong CSS, ti le theo `spread`:
 *   0 0 8px  30%  |  0 0 16px 15%  |  0 0 30px 8%
 */
private val GlowLayers = listOf(
    0.42f to 1.00f,
    0.75f to 0.50f,
    1.00f to 0.26f,
)

/**
 * Vet sang neon quanh khoi bo goc: blur that, sang nhat sat vien roi nhat dan
 * ra ngoai mot cach lien tuc.
 *
 * Dat TRUOC .clip()/.background() de vet sang nam ngoai than the.
 */
fun Modifier.neonGlow(
    color: Color,
    cornerRadius: Dp,
    spread: Dp = 18.dp,
    intensity: Float = 1f,
    maxAlpha: Float = 0.55f,
): Modifier = this.drawBehind {
    if (intensity <= 0.01f) return@drawBehind
    val radiusPx = cornerRadius.toPx()
    val spreadPx = spread.toPx()
    drawIntoCanvas { canvas ->
        val paint = Paint().apply { style = PaintingStyle.Fill }
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.isAntiAlias = true
        GlowLayers.forEach { (blurRatio, alphaRatio) ->
            val blurPx = spreadPx * blurRatio
            if (blurPx <= 0.5f) return@forEach
            frameworkPaint.color = color.copy(alpha = maxAlpha * alphaRatio * intensity).toArgb()
            frameworkPaint.maskFilter =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
                } else {
                    null
                }
            canvas.nativeCanvas.drawRoundRect(
                0f,
                0f,
                size.width,
                size.height,
                radiusPx,
                radiusPx,
                frameworkPaint,
            )
        }
        frameworkPaint.maskFilter = null
    }
}

/**
 * Vet sang neon quanh khoi tron: dung radial gradient nhieu chang nen mau
 * chuyen lien tuc, tuyet doi khong co van.
 */
fun Modifier.neonGlowCircle(
    color: Color,
    spread: Dp = 14.dp,
    intensity: Float = 1f,
    maxAlpha: Float = 0.6f,
): Modifier = this.drawBehind {
    if (intensity <= 0.01f) return@drawBehind
    val baseRadius = size.minDimension / 2f
    val spreadPx = spread.toPx()
    val outerRadius = baseRadius + spreadPx
    if (outerRadius <= 0f) return@drawBehind
    val edge = (baseRadius / outerRadius).coerceIn(0f, 0.95f)
    val a = maxAlpha * intensity
    // Sang deu ben trong den sat vien, sau do tat dan muot ra ngoai.
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to color.copy(alpha = a),
                edge to color.copy(alpha = a),
                edge + (1f - edge) * 0.25f to color.copy(alpha = a * 0.55f),
                edge + (1f - edge) * 0.5f to color.copy(alpha = a * 0.26f),
                edge + (1f - edge) * 0.75f to color.copy(alpha = a * 0.09f),
                1f to Color.Transparent,
            ),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = outerRadius,
        ),
        radius = outerRadius,
        center = Offset(size.width / 2f, size.height / 2f),
    )
}
