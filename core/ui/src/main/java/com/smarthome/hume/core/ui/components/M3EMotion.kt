package com.smarthome.hume.core.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Motion tokens port tu demo HTML rev12.
 *
 * - emphasized:      cubic-bezier(.05,.7,.1,1)  — entrance (rise, sheet, dialog)
 * - emphasizedAcc:   cubic-bezier(.3,0,.8,.15)  — exit
 * - spring:          cubic-bezier(.34,1.45,.5,1) — press/morph tuong tac
 */
object M3EMotion {
    val emphasized: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val emphasizedAcc: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val spring: Easing = CubicBezierEasing(0.34f, 1.45f, 0.5f, 1f)
}

/**
 * Press morph theo demo: khi nhan, scale xuong [pressedScale] voi spring.
 * Dung thay cho clickable mac dinh o moi the co press effect.
 *
 * Doc gia tri animation trong graphicsLayer lambda -> chi invalidate draw,
 * khong recompose moi frame (fix 2026-09-30).
 */
fun Modifier.pressMorph(
    pressedScale: Float = 0.93f,
    onClick: (() -> Unit)? = null,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale = animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = tween(300, easing = M3EMotion.spring),
        label = "pressMorph",
    )
    this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            onClick = { onClick?.invoke() },
        )
}

/**
 * Blink vo han cho LIVE dot / neon — demo: blink 1.2–1.6s infinite.
 *
 * Doc alpha trong graphicsLayer lambda -> infinite transition chi
 * invalidate draw, khong recompose 60fps vinh vien (fix 2026-09-30).
 */
fun Modifier.blink(periodMs: Int = 1400): Modifier = composed {
    val t = rememberInfiniteTransition(label = "blink")
    val alpha = t.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMs / 2),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blinkAlpha",
    )
    this.graphicsLayer {
        this.alpha = alpha.value
    }
}

/**
 * State dieu khien neighbor-press cho mot nhom nut (demo: .press-main/.press-nei).
 *
 * Cach dung:
 * ```
 * val np = rememberNeighborPress(count = 4)
 * Row {
 *     items.forEachIndexed { i, _ ->
 *         Box(Modifier.weight(np.weightFor(i)).neighborPressable(np, i) { ... })
 *     }
 * }
 * ```
 * Nhan giu item i: item i weight 1.45, 2 item ke weight 0.82 (giong demo .rmm).
 */
@Composable
fun rememberNeighborPress(
    count: Int,
    pressedWeight: Float = 1.45f,
    neighborWeight: Float = 0.82f,
): NeighborPressState {
    return remember(count, pressedWeight, neighborWeight) {
        NeighborPressState(count, pressedWeight, neighborWeight)
    }
}

class NeighborPressState(
    val count: Int,
    val pressedWeight: Float = 1.45f,
    val neighborWeight: Float = 0.82f,
) {
    var pressedIndex: Int by mutableStateOf(-1)
        private set

    fun press(i: Int) { pressedIndex = i }
    fun release() { pressedIndex = -1 }

    /** Weight cho item i: pressedWeight neu dang nhan, neighborWeight neu ke ben, 1.0 con lai. */
    fun weightFor(i: Int): Float = when {
        pressedIndex < 0 -> 1f
        i == pressedIndex -> pressedWeight
        kotlin.math.abs(i - pressedIndex) == 1 -> neighborWeight
        else -> 1f
    }

    /** True neu item i la nut ke ben nut dang nhan (de scaleX nhu .nbtn.press-nei). */
    fun isNeighbor(i: Int): Boolean =
        pressedIndex >= 0 && kotlin.math.abs(i - pressedIndex) == 1
}

/**
 * Rung nhe khi tuong tac — demo: navigator.vibrate(6..10).
 * Dung: val haptic = rememberHaptic(); haptic()
 */
@Composable
fun rememberHaptic(): () -> Unit {
    val haptic = LocalHapticFeedback.current
    return { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
}

/**
 * Quan sat trang thai pressed tu mot MutableInteractionSource dung chung.
 */
@Composable
fun MutableInteractionSource.collectPressedAsState(): State<Boolean> =
    collectIsPressedAsState()
