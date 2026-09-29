package com.smarthome.hume.core.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import kotlinx.coroutines.delay

/**
 * Text chay marquee khi tran (khong dung basicMarquee mac dinh vi bi loi chong chu).
 * Dung scroll state + animation tu che: doi 1.5s -> cuon het -> nghi 1s -> quay ve.
 */
@Composable
fun MarqueeText(
    text: String,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(text) {
        delay(1500)
        while (true) {
            val max = scrollState.maxValue
            if (max > 0) {
                scrollState.animateScrollTo(
                    max,
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = (max * 15).coerceAtLeast(1000),
                        easing = androidx.compose.animation.core.LinearEasing,
                    ),
                )
                delay(1000)
                scrollState.animateScrollTo(
                    0,
                    animationSpec = androidx.compose.animation.core.tween(
                        durationMillis = 800,
                        easing = androidx.compose.animation.core.LinearEasing,
                    ),
                )
                delay(1500)
            } else {
                delay(2000)
            }
        }
    }
    Box(
        modifier = modifier
            .horizontalScroll(scrollState, enabled = false),
    ) {
        Text(
            text,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = color,
            maxLines = 1,
            softWrap = false,
        )
    }
}
