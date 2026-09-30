package com.smarthome.hume.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Hieu ung "rise" dung chung: mo dan + truot len 22dp trong 700ms
 * voi M3EMotion.emphasized. Thay the 2 ban riseIn rieng o
 * feature/home (HomeMotion.kt) va feature/security (SecurityScreen.kt).
 */
fun Modifier.riseIn(delayMs: Int = 0): Modifier = composed {
    val density = LocalDensity.current
    val progress = remember { Animatable(0f) }
    LaunchedEffect(delayMs) {
        if (delayMs > 0) delay(delayMs.toLong())
        progress.animateTo(1f, animationSpec = tween(700, easing = M3EMotion.emphasized))
    }
    this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * with(density) { 22.dp.toPx() }
    }
}
