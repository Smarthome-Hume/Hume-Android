package com.smarthome.hume.feature.energy

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.composed
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.smarthome.hume.core.ui.components.M3EMotion

/**
 * Rise entrance giong demo (.rise): opacity 0->1, translateY 22px->0,
 * .7s emphasized decelerate, chay 1 lan duy nhat cho moi key.
 * [played] duoc nho o EnergyScreen de doi subtab khong replay.
 */
fun Modifier.riseOnce(
    key: String,
    delayMs: Int,
    played: MutableSet<String>,
): Modifier = composed {
    var shown by remember(key) { mutableStateOf(played.contains(key)) }
    LaunchedEffect(key) {
        if (!played.contains(key)) {
            shown = true
            played.add(key)
        }
    }
    val alpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(700, delayMillis = delayMs, easing = M3EMotion.emphasized),
        label = "riseAlpha",
    )
    val dy by animateFloatAsState(
        targetValue = if (shown) 0f else 22f,
        animationSpec = tween(700, delayMillis = delayMs, easing = M3EMotion.emphasized),
        label = "riseDy",
    )
    this
        .alpha(alpha)
        .offset(y = dy.dp)
}

/**
 * Fade/slide ngan khi doi subtab — demo: .etab{transition:opacity .18s,transform .18s},
 * .pre{opacity:0;translateY(10px)}.
 */
fun Modifier.paneFade(visible: Boolean): Modifier = composed {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(180),
        label = "paneAlpha",
    )
    val dy by animateFloatAsState(
        targetValue = if (visible) 0f else 10f,
        animationSpec = tween(180),
        label = "paneDy",
    )
    this
        .alpha(alpha)
        .offset(y = dy.dp)
}

/**
 * Entrance stagger cho hang (doi che do Cong suat/Nang luong: fade/slide + stagger 45ms;
 * legend donut: tu phai sang, delay 250/400ms) — demo .dvrow.pre / .dli.
 */
fun Modifier.staggerEnter(
    index: Int,
    visible: Boolean,
    baseDelayMs: Int = 0,
    staggerMs: Int = 45,
    fromX: Float = 0f,
    fromY: Float = 10f,
): Modifier = composed {
    val d = if (visible) baseDelayMs + index * staggerMs else 0
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400, delayMillis = d, easing = M3EMotion.emphasized),
        label = "stagAlpha",
    )
    val ox by animateFloatAsState(
        targetValue = if (visible) 0f else fromX,
        animationSpec = tween(450, delayMillis = d, easing = M3EMotion.emphasized),
        label = "stagX",
    )
    val oy by animateFloatAsState(
        targetValue = if (visible) 0f else fromY,
        animationSpec = tween(450, delayMillis = d, easing = M3EMotion.emphasized),
        label = "stagY",
    )
    this
        .alpha(alpha)
        .offset(x = ox.dp, y = oy.dp)
}
