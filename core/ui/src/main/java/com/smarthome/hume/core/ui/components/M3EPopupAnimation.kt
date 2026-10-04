package com.smarthome.hume.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlin.math.PI
import kotlin.math.pow

/**
 * Port tu iOS: InsightPopup.swift (chi phan animation).
 *
 * Primitives tai dung cho popup overlay:
 * - Enter: scale spring 0.9 → 1 + fade (iOS: spring response 0.35, damping 0.8).
 * - Exit: scale spring ve nho + fade (iOS: spring response 0.3, damping 0.8).
 * - Nen: scrim dim (tuong duong iOS ultraThinMaterial + dim 0.45),
 *   tap de dong, khong ripple.
 *
 * Luu y: Android khong co backdrop-blur re nhu iOS (can API 31+ RenderEffect
 * va snapshot nen), nen chi dung scrim dim — muon blur that thi caller tu
 * blur content ben duoi (vd: Haze hoac graphicsLayer tren content).
 *
 * Khong lam noi dung popup — nhom khac lam.
 *
 * Vi du:
 * ```
 * M3EPopupOverlay(
 *     visible = showPopup,
 *     onDismissRequest = { showPopup = false },
 * ) {
 *     // noi dung card popup o day
 *     InsightCard(...)
 * }
 * ```
 */

/**
 * Specs animation popup — tai dung neu muon tu ghep AnimatedVisibility.
 * SwiftUI `spring(response:)` → Compose `spring(stiffness = (2π/response)²)`.
 */
object M3EPopupAnimation {
    /** Enter: scale spring 0.9 → 1 + fade (iOS response 0.35s, damping 0.8). */
    fun popupEnter(initialScale: Float = 0.9f): EnterTransition =
        fadeIn(tween(300)) + scaleIn(
            initialScale = initialScale,
            animationSpec = springFromResponse(responseSec = 0.35),
        )

    /** Exit: scale spring ve nho + fade (iOS response 0.3s, damping 0.8). */
    fun popupExit(targetScale: Float = 0.95f): ExitTransition =
        fadeOut(tween(300)) + scaleOut(
            targetScale = targetScale,
            animationSpec = springFromResponse(responseSec = 0.3),
        )

    /** Scrim chi fade (khong scale). */
    val scrimEnter: EnterTransition = fadeIn(tween(300))

    /** Scrim chi fade (khong scale). */
    val scrimExit: ExitTransition = fadeOut(tween(300))

    /**
     * Chuyen SwiftUI `spring(response:, dampingFraction:)` sang Compose spring.
     * stiffness = (2π / response)², dampingRatio = dampingFraction.
     */
    fun springFromResponse(
        responseSec: Double,
        dampingRatio: Float = 0.8f,
    ): FiniteAnimationSpec<Float> = spring(
        dampingRatio = dampingRatio,
        stiffness = (2 * PI / responseSec).pow(2).toFloat(),
    )
}

/**
 * Overlay popup fullscreen tai dung.
 *
 * - Scrim: [MaterialTheme.colorScheme.scrim] voi [scrimAlpha] (mac dinh 0.45
 *   nhu iOS), tap de dong (khong ripple) khi co [onDismissRequest].
 * - Content: nam giua, enter = scale spring [enterInitialScale] → 1 + fade.
 *
 * @param visible hien/ẩn popup (AnimatedVisibility lo animate).
 * @param onDismissRequest null → scrim khong bat tap.
 * @param scrimAlpha do mo cua scrim (0..1), mac dinh 0.45 nhu iOS.
 * @param enterInitialScale scale ban dau khi enter, mac dinh 0.9.
 */
@Composable
fun M3EPopupOverlay(
    visible: Boolean,
    onDismissRequest: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    scrimAlpha: Float = 0.45f,
    enterInitialScale: Float = 0.9f,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    // Scrim fade rieng, content scale+fade rieng — nhu iOS (bgOpacity vs animRect).
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = M3EPopupAnimation.scrimEnter,
        exit = M3EPopupAnimation.scrimExit,
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.scrim.copy(alpha = scrimAlpha),
                    )
                    .then(
                        if (onDismissRequest != null) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onDismissRequest,
                            )
                        } else {
                            Modifier
                        },
                    ),
            )
            AnimatedVisibility(
                visible = visible,
                enter = M3EPopupAnimation.popupEnter(initialScale = enterInitialScale),
                exit = M3EPopupAnimation.popupExit(),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = contentAlignment,
                ) {
                    content()
                }
            }
        }
    }
}
