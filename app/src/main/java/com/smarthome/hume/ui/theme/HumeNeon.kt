package com.smarthome.hume.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.pow

/*
 * HIEU UNG NEON - port theo bundle ban HTML cocopi-home.
 *
 *   @keyframes neon-blink { 0%,100% { opacity:1 } 50% { opacity:.3 } }
 *   @keyframes neonPulse {
 *     0%,100% { box-shadow: 0 0 8px  #f9784c4d, 0 0 16px #f9784c26, 0 0 30px #f9784c14 }
 *      20%    { box-shadow: 0 0 14px #f9784c99, 0 0 28px #f9784c4d, 0 0 50px #f9784c26 }
 *   }
 *
 * QUAN TRONG: `box-shadow: 0 0 Npx mau` cua CSS la VET SANG toa deu ra ngoai
 * tu VIEN va nhat dan - KHONG phai bong do. Vi vay tuyet doi khong dung
 * Modifier.shadow (bong do lech + toi mau). Thay vao do ve nhieu vong vien
 * dong tam noi ra ngoai voi alpha giam dan (neonGlow / neonGlowCircle) - dung
 * ba lop 8/16/30px nhu ban HTML.
 */

/** neon-blink: opacity 1 -> .3 -> 1, mac dinh 1s. */
@Composable
fun rememberNeonBlink(periodMillis: Int = 1000): Float {
    val transition = rememberInfiniteTransition(label = "neonBlink")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMillis / 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "neonBlinkAlpha",
    )
    return alpha
}

/**
 * neonPulse: cuong do quang sang 0 -> 1 (dinh o 20% chu ky) -> 0.
 * Bam dung keyframes CSS: len nhanh, xuong cham, khong doi chieu.
 */
@Composable
fun rememberNeonPulse(periodMillis: Int = 6000): Float {
    val transition = rememberInfiniteTransition(label = "neonPulse")
    val level by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = periodMillis
                0f at 0 using FastOutSlowInEasing
                1f at (periodMillis * 20 / 100) using FastOutSlowInEasing
                0f at periodMillis
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "neonPulseLevel",
    )
    return level
}

/** Mau cham bao trang thai mo trong ban HTML: #ff5252. */
val NeonDotRed = Color(0xFFFF5252)

/** Mau quang sang cam cua neonPulse trong ban HTML: #f9784c. */
val NeonGlowOrange = Color(0xFFF9784C)

private const val GlowRings = 16

/**
 * Vet sang neon quanh mot khoi bo goc: bat dau NGAY TAI VIEN roi lan ra ngoai
 * `spread` va nhat dan (alpha giam theo luy thua ~2.2 giong duoi box-shadow).
 *
 * Dat TRUOC .clip()/.background() de vet sang nam ngoai than the.
 *
 * @param color mau vet sang
 * @param cornerRadius bo goc cua than the
 * @param spread do lan toa toi da tinh tu vien
 * @param intensity 0..1, thuong lay tu rememberNeonPulse
 * @param maxAlpha do dam ngay sat vien khi intensity = 1
 */
fun Modifier.neonGlow(
    color: Color,
    cornerRadius: Dp,
    spread: Dp = 18.dp,
    intensity: Float = 1f,
    maxAlpha: Float = 0.55f,
): Modifier = this.drawBehind {
    if (intensity <= 0.01f) return@drawBehind
    val spreadPx = spread.toPx()
    val radiusPx = cornerRadius.toPx()
    val ringWidth = spreadPx / GlowRings
    // Ve tu vong NGOAI CUNG vao trong de vong sat vien nam tren cung.
    for (ring in GlowRings downTo 1) {
        val far = ring / GlowRings.toFloat()
        val grow = spreadPx * far
        val alpha = (1f - far).pow(2.2f) * maxAlpha * intensity
        if (alpha <= 0.002f) continue
        drawRoundRect(
            color = color.copy(alpha = alpha),
            topLeft = Offset(-grow, -grow),
            size = Size(size.width + grow * 2, size.height + grow * 2),
            cornerRadius = CornerRadius(radiusPx + grow),
            style = Stroke(width = ringWidth * 1.8f),
        )
    }
}

/** Nhu neonGlow nhung cho khoi tron (cham bao, vong icon, nut chuong). */
fun Modifier.neonGlowCircle(
    color: Color,
    spread: Dp = 14.dp,
    intensity: Float = 1f,
    maxAlpha: Float = 0.6f,
): Modifier = this.drawBehind {
    if (intensity <= 0.01f) return@drawBehind
    val spreadPx = spread.toPx()
    val baseRadius = size.minDimension / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    val ringWidth = spreadPx / GlowRings
    for (ring in GlowRings downTo 1) {
        val far = ring / GlowRings.toFloat()
        val alpha = (1f - far).pow(2.2f) * maxAlpha * intensity
        if (alpha <= 0.002f) continue
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = baseRadius + spreadPx * far,
            center = center,
            style = Stroke(width = ringWidth * 1.8f),
        )
    }
}
