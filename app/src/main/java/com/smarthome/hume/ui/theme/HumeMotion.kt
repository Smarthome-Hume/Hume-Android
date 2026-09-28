package com.smarthome.hume.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/*
 * Motion theo huong Material 3 Expressive: vat ly lo xo thay cho easing cung.
 *
 * Cach dung:
 *   val on by animateFloatAsState(if (on) 1f else 0f, animationSpec = HumeMotion.bouncy())
 *   AnimatedVisibility(visible, enter = fadeIn(HumeMotion.snappy()) + scaleIn(HumeMotion.bouncy()), ...)
 */
object HumeMotion {
    /** Lo xo nay, co overshoot — toggle, nut bam, card press, FAB menu. */
    fun <T> bouncy(): AnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow,
    )

    /** Lo xo nhanh, khong nay — doi trang thai nho, mau sac, alpha. */
    fun <T> snappy(): AnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium,
    )

    /** Lo xo mem — bottom sheet, dialog, so lieu "tho". */
    fun <T> gentle(): AnimationSpec<T> = spring(
        dampingRatio = 0.85f,
        stiffness = Spring.StiffnessLow,
    )
}
