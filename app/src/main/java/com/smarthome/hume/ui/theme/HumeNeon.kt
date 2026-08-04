package com.smarthome.hume.ui.theme

import android.graphics.BlurMaskFilter
import android.os.Build
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
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
 * BA DIEU KIEN BAT BUOC
 *
 * 1. ANH SANG CHI O NGOAI VIEN. Den neon hat RA NGOAI, tinh tu vien, va nhat
 *    dan khi ra xa. Ben trong vien khong duoc co bat ky anh sang hay nhip nhap
 *    nhay nao. Vi vay sau khi ve blur, PHAI cat bo toan bo phan nam trong hinh
 *    bang clipPath(ClipOp.Difference). Neu chi de nen phu len phan trong thi
 *    khi nen ban trong suot (vi du chip nen 12%) anh sang se lot qua va nhin
 *    thay nhap nhay ben trong - day dung la loi da mac phai.
 *
 * 2. TOA MUOT. Khong xep nhieu vong stroke dong tam (se hien ro van tung lop).
 *    Phai blur that:
 *      - khoi bo goc -> BlurMaskFilter, 3 lop blur theo dung 3 lop box-shadow
 *      - khoi tron   -> Brush.radialGradient nhieu chang, chuyen mau lien tuc
 *
 * 3. CUNG MOT NHIP. Moi cho phat sang dung CHUNG rememberNeonBeat, pha tinh tu
 *    dong ho khung hinh nen dong pha tuyet doi.
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
 *   0 0 8px 30% | 0 0 16px 15% | 0 0 30px 8%
 */
private val GlowLayers = listOf(
    0.42f to 1.00f,
    0.75f to 0.50f,
    1.00f to 0.26f,
)

/**
 * Den neon hat ra NGOAI vien mot khoi bo goc: blur that, sang nhat sat vien roi
 * nhat dan ra ngoai. Phan ben trong vien duoc cat bo hoan toan.
 *
 * Dat TRUOC .clip()/.background().
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
    // Hinh than the: moi thu nam trong day se bi cat bo.
    val bodyPath = Path().apply {
        addRoundRect(
            RoundRect(
                rect = Rect(0f, 0f, size.width, size.height),
                cornerRadius = CornerRadius(radiusPx, radiusPx),
            )
        )
    }
    clipPath(path = bodyPath, clipOp = ClipOp.Difference) {
        drawIntoCanvas { canvas ->
            val paint = Paint().apply { style = PaintingStyle.Fill }
            val frameworkPaint = paint.asFrameworkPaint()
            frameworkPaint.isAntiAlias = true
            GlowLayers.forEach { (blurRatio, alphaRatio) ->
                val blurPx = spreadPx * blurRatio
                if (blurPx <= 0.5f) return@forEach
                frameworkPaint.color =
                    color.copy(alpha = maxAlpha * alphaRatio * intensity).toArgb()
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
}

/**
 * Den neon hat ra NGOAI vien mot khoi tron: radial gradient nen chuyen mau lien
 * tuc, khong bao gio co van. Phan ben trong duong tron bi cat bo hoan toan.
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
    if (outerRadius <= 0f || baseRadius <= 0f) return@drawBehind
    val center = Offset(size.width / 2f, size.height / 2f)
    val edge = (baseRadius / outerRadius).coerceIn(0f, 0.95f)
    val a = maxAlpha * intensity
    val bodyPath = Path().apply {
        addOval(
            Rect(
                center.x - baseRadius,
                center.y - baseRadius,
                center.x + baseRadius,
                center.y + baseRadius,
            )
        )
    }
    clipPath(path = bodyPath, clipOp = ClipOp.Difference) {
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
                center = center,
                radius = outerRadius,
            ),
            radius = outerRadius,
            center = center,
        )
    }
}
