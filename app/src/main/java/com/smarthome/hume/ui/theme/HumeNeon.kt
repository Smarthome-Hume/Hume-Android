package com.smarthome.hume.ui.theme

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

/*
 * HIEU UNG NEON - port DUNG theo ban HTML cocopi-home, khong tu nghi thêm.
 *
 * Trong bundle chi co dung hai keyframes va chung chi duoc gan cho may cho:
 *
 *   @keyframes neon-blink { 0%,100% { opacity: 1 } 50% { opacity: .3 } }
 *     -> CHAM trang thai nho: cham do bao cua/cua so dang mo (1s),
 *        cham xanh/do bao ket noi (1.5s).
 *        KHONG ap dung cho ca the phong.
 *
 *   @keyframes neonPulse { ... box-shadow toa dan roi diu lai ... }
 *     -> quang sang quanh CHIP dang bat: chip thong bao khi co thong bao,
 *        chip an ninh khi khac disarmed, chip so bong den khi > 0 (6s);
 *        va mot lop gradient rat mo tren the dang bat (60s - cham den muc
 *        gan nhu tinh, nen ban Android khong lam lop nay).
 */

/** neon-blink: opacity 1 -> .3 -> 1. */
@Composable
fun rememberNeonBlink(periodMillis: Int = 1000): Float {
    val transition = rememberInfiniteTransition(label = "neonBlink")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMillis / 2),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "neonBlinkAlpha",
    )
    return alpha
}

/** neonPulse: cuong do quang sang 0 -> 1 -> 0, chu ky 6s cho chip. */
@Composable
fun rememberNeonPulse(periodMillis: Int = 6000): Float {
    val transition = rememberInfiniteTransition(label = "neonPulse")
    val level by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMillis / 2),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "neonPulseLevel",
    )
    return level
}
