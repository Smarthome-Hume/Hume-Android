package com.smarthome.hume.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Chu chay marquee khi tran khung.
 *
 * Khong hieu ung mo mep: chu bi cat thang o bien khung (don gian,
 * sach, khong "tho" nhu ban fade DstIn).
 *
 * Nhip: nghi 1.5s o dau (hien tron tu ky tu dau) -> cuon het
 * (toc do ti le do tran) -> nghi 1.2s -> cuon ve mem -> lap lai.
 */
@Composable
fun MarqueeText(
    text: String,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    color: Color,
    modifier: Modifier = Modifier,
    /**
     * Khoang cach tu mep trai khung cuon den vi tri text luc nghi.
     * Dung de mo rong viewport den sat vien icon tron.
     */
    startPadding: Dp = 0.dp,
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(text) {
        delay(1500)
        while (true) {
            val max = scrollState.maxValue
            if (max > 0) {
                scrollState.animateScrollTo(
                    max,
                    animationSpec = tween(
                        durationMillis = (max * 15).coerceAtLeast(900),
                        easing = LinearEasing,
                    ),
                )
                delay(1200)
                scrollState.animateScrollTo(
                    0,
                    animationSpec = tween(durationMillis = 800, easing = LinearEasing),
                )
                delay(1500)
            } else {
                if (scrollState.value != 0) scrollState.scrollTo(0)
                delay(2000)
            }
        }
    }
    Box(
        modifier = modifier.horizontalScroll(scrollState, enabled = false),
    ) {
        Text(
            text,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = color,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(start = startPadding),
        )
    }
}
