package com.smarthome.hume.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.PI
import kotlin.math.pow
import kotlin.random.Random

/**
 * Port tu iOS: ChaoticLoading.swift.
 *
 * Modifier cho view bieu do: scale/xoay ngau nhien voi bien do giam dan
 * roi on dinh ve 0 — hieu ung "dang tai du lieu".
 * Doc gia tri animation trong graphicsLayer lambda → chi invalidate draw,
 * khong recompose moi frame (theo fix 2026-09-30 trong M3EMotion.kt).
 *
 * Vi du:
 * ```
 * WeekChartD(
 *     modifier = Modifier.chaoticLoading(isLoading = isLoading),
 * )
 * ```
 *
 * @param isLoading true → chay hieu ung; false → snap ve trang thai on dinh.
 * @param durationMs tong thoi gian 12 steps loan (mac dinh 1200ms nhu iOS).
 */
fun Modifier.chaoticLoading(
    isLoading: Boolean = true,
    durationMs: Long = 1200,
): Modifier = composed {
    val phase = remember { Animatable(0f) }

    // LaunchedEffect tu cancel khi isLoading doi → tuong duong iOS task?.cancel().
    LaunchedEffect(isLoading, durationMs) {
        if (!isLoading) {
            phase.snapTo(0f)
            return@LaunchedEffect
        }
        val stepMs = (durationMs / ChaosSteps).toInt()
        repeat(ChaosSteps) { i ->
            // Giam dan bien do: bat dau loan manh, cuoi on dinh
            val dampen = 1f - i.toFloat() / ChaosSteps
            // Ngau nhien -1...1
            val random = Random.nextFloat() * 2f - 1f
            phase.animateTo(
                targetValue = random * dampen,
                animationSpec = tween(stepMs, easing = ChaosEaseOut),
            )
        }
        // Ve 0 (on dinh) — iOS: .spring(response: 0.4, dampingFraction: 0.7)
        // stiffness = (2π/response)²
        phase.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = 0.7f,
                stiffness = (2 * PI / 0.4).pow(2).toFloat(),
            ),
        )
    }

    this.graphicsLayer {
        // Scale "loan": 1 ± 0.08
        scaleX = 1f + phase.value * ChaosScaleAmp
        scaleY = 1f + phase.value * ChaosScaleAmp
        // Xoay nhe ngau nhien: ±3°
        rotationZ = phase.value * ChaosRotationAmp
    }
}

private const val ChaosSteps = 12
private const val ChaosScaleAmp = 0.08f
private const val ChaosRotationAmp = 3f

/** Tuong duong SwiftUI `.easeOut` = cubic-bezier(0, 0, 0.58, 1). */
private val ChaosEaseOut: Easing = CubicBezierEasing(0f, 0f, 0.58f, 1f)
