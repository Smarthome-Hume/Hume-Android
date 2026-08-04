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
import androidx.compose.ui.graphics.Color

/*
 * HIEU UNG NEON - port DUNG theo bundle ban HTML cocopi-home.
 * Trich tu assets/index-CG3412vx.css + index-BZ2dR2AJ.js:
 *
 *   @keyframes neon-blink { 0%,100% { opacity:1 } 50% { opacity:.3 } }
 *     -> cham do 8px o goc icon the phong khi cua/cua so dang mo:
 *        background #ff5252,
 *        box-shadow 0 0 6px rgba(255,82,82,.8), 0 0 12px .4, 0 0 20px .2,
 *        animation neon-blink 1s ease-in-out infinite.
 *        CHI cham nhay - KHONG bao gio nhay ca the phong.
 *
 *   @keyframes neonPulse {
 *     0%,100% { box-shadow: 0 0 8px  #f9784c4d, 0 0 16px #f9784c26, 0 0 30px #f9784c14 }
 *      20%    { box-shadow: 0 0 14px #f9784c99, 0 0 28px #f9784c4d, 0 0 50px #f9784c26 }
 *   }
 *     -> DINH quang sang o 20% chu ky roi diu dan ve cuoi (khong phai sine).
 *     -> 6s: chip dang bat  (nut chuong khi co thong bao, vong icon an ninh
 *        khac disarmed, chip so bong den > 0).
 *     -> 60s: lop gradient rat mo tren the dang bat (cham gan nhu tinh).
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
 * neonPulse: cuong do quang sang 0 -> 1 (o 20% chu ky) -> 0.
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
