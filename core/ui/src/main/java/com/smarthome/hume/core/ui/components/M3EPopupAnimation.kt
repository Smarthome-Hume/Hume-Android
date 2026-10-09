package com.smarthome.hume.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.TwoWayConverter
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.pow

/**
 * Port tu iOS: InsightPopup.swift (chi phan animation).
 *
 * Primitives tai dung cho popup overlay:
 * - Enter (iOS 58a3542): popup PHONG RA TU VI TRI NUT BAM (frame animation),
 *   khong dung scaleEffect. Neu khong co sourceRect, phong tu diem giua.
 * - Exit: fade + thu nho ve (giu don gian).
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
 *     sourceRectDp = buttonBoundsInRoot, // Rect (dp) cua nut bam, null = giua
 * ) {
 *     InsightCard(...)
 * }
 * ```
 */

/** Converter Rect <-> AnimationVector4D cho Animatable. */
private val RectConverter = TwoWayConverter<Rect, AnimationVector4D>(
    convertToVector = { AnimationVector4D(it.left, it.top, it.right, it.bottom) },
    convertFromVector = { Rect(it.v1, it.v2, it.v3, it.v4) },
)

/**
 * Specs animation popup — tai dung neu muon tu ghep AnimatedVisibility.
 * SwiftUI `spring(response:)` → Compose `spring(stiffness = (2π/response)²)`.
 */
object M3EPopupAnimation {
    /** Enter cu: scale spring 0.9 → 1 + fade (giu de tuong thich). */
    fun popupEnter(initialScale: Float = 0.9f): EnterTransition =
        fadeIn(tween(300)) + scaleIn(
            initialScale = initialScale,
            animationSpec = springFromResponse(responseSec = 0.35),
        )

    /** Exit: fade + scale nho (giu don gian nhu cu). */
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
 * - Content: phong ra tu [sourceRectDp] (vi tri nut bam) — port iOS 58a3542,
 *   khong dung scaleEffect. Null → phong tu diem giua man hinh.
 *
 * @param visible hien/ẩn popup.
 * @param onDismissRequest null → scrim khong bat tap.
 * @param scrimAlpha do mo cua scrim (0..1), mac dinh 0.45 nhu iOS.
 * @param sourceRectDp Rect (dp, toa do root) cua nut bam de popup phong ra tu do.
 * @param contentAlignment can chinh content, mac dinh giua.
 */
@Composable
fun M3EPopupOverlay(
    visible: Boolean,
    onDismissRequest: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    scrimAlpha: Float = 0.45f,
    sourceRectDp: Rect? = null,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    // Scrim fade rieng — nhu iOS (bgOpacity).
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
            if (visible) {
                PopupRectContent(
                    sourceRectDp = sourceRectDp,
                    contentAlignment = contentAlignment,
                    content = content,
                )
            }
        }
    }
}

/**
 * Content popup voi frame animation tu sourceRect → targetRect
 * (port iOS 58a3542: khong dung scaleEffect).
 */
@Composable
private fun PopupRectContent(
    sourceRectDp: Rect?,
    contentAlignment: Alignment,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val maxW = maxWidth
        val maxH = maxHeight
        // Target: content nam giua (se do kich thuoc thuc te sau)
        var contentSizeDp by remember { mutableStateOf<Pair<Dp, Dp>?>(null) }

        val targetRect = remember(contentSizeDp, maxW, maxH) {
            val (w, h) = contentSizeDp ?: (maxW * 0.8f to maxH * 0.5f)
            // Can giua theo contentAlignment (mac dinh Center)
            val left = when (contentAlignment) {
                Alignment.CenterStart, Alignment.TopStart, Alignment.BottomStart ->
                    0.dp
                Alignment.CenterEnd, Alignment.TopEnd, Alignment.BottomEnd ->
                    maxW - w
                else -> (maxW - w) / 2
            }
            val top = when (contentAlignment) {
                Alignment.TopStart, Alignment.TopCenter, Alignment.TopEnd ->
                    0.dp
                Alignment.BottomStart, Alignment.BottomCenter, Alignment.BottomEnd ->
                    maxH - h
                else -> (maxH - h) / 2
            }
            with(density) {
                Rect(
                    left.toPx(), top.toPx(),
                    (left + w).toPx(), (top + h).toPx(),
                )
            }
        }

        // Source: rect nut bam, hoac 40dp tai giua (fallback nhu iOS)
        val startRect = remember(sourceRectDp, maxW, maxH) {
            sourceRectDp?.let {
                with(density) {
                    Rect(
                        it.left.toPx(), it.top.toPx(),
                        it.right.toPx(), it.bottom.toPx(),
                    )
                }
            } ?: with(density) {
                val cx = maxW.toPx() / 2
                val cy = maxH.toPx() / 2
                val half = 20.dp.toPx()
                Rect(cx - half, cy - half, cx + half, cy + half)
            }
        }

        val animRect = remember { Animatable(startRect, RectConverter) }
        // Reset ve source moi lan hien
        LaunchedEffect(Unit) {
            animRect.snapTo(startRect)
            animRect.animateTo(
                targetRect,
                animationSpec = spring(
                    dampingRatio = 0.8f,
                    stiffness = (2 * PI / 0.35).pow(2).toFloat(),
                ),
            )
        }
        // Khi do duoc kich thuoc thuc, cap nhat target
        LaunchedEffect(targetRect) {
            animRect.animateTo(
                targetRect,
                animationSpec = spring(
                    dampingRatio = 0.8f,
                    stiffness = (2 * PI / 0.35).pow(2).toFloat(),
                ),
            )
        }

        val current = animRect.value
        val offsetXDp = with(density) { current.left.toDp() }
        val offsetYDp = with(density) { current.top.toDp() }
        val widthDp = with(density) { (current.right - current.left).toDp() }
        val heightDp = with(density) { (current.bottom - current.top).toDp() }

        Box(
            modifier = Modifier
                .offset(x = offsetXDp, y = offsetYDp)
                .size(widthDp, heightDp)
                .onGloballyPositioned { coords ->
                    val size = coords.size
                    val w = with(density) { size.width.toDp() }
                    val h = with(density) { size.height.toDp() }
                    if (contentSizeDp?.first != w || contentSizeDp?.second != h) {
                        contentSizeDp = w to h
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}
