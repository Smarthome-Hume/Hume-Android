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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.layout
import kotlinx.coroutines.delay

/**
 * Forward baseline tu Text con ra ngoai de Row co the dung alignByBaseline().
 * (Box mac dinh khong forward baseline cua con.)
 */
private fun Modifier.forwardBaseline() = this.then(
    Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val first = placeable[FirstBaseline]
        val last = placeable[LastBaseline]
        val lines = mutableMapOf<AlignmentLine, Int>()
        if (first != AlignmentLine.Unspecified) lines[FirstBaseline] = first
        if (last != AlignmentLine.Unspecified) lines[LastBaseline] = last
        layout(placeable.width, placeable.height, alignmentLines = lines) {
            placeable.placeRelative(0, 0)
        }
    }
)

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
    fontFamily: FontFamily? = null,
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
                        // Hoan tac: giu 15ms/px nhu cu (muc 3 cua user la toc do
                        // cuon trang + mo the phong, khong phai marquee).
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
        modifier = modifier
            .horizontalScroll(scrollState, enabled = false)
            .forwardBaseline(),
    ) {
        Text(
            text,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            color = color,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(start = startPadding),
        )
    }
}

/**
 * Overload nhan TextStyle (cho code da migrate sang MaterialTheme.typography).
 * fontWeight neu truyen se ghi de len style.fontWeight.
 */
@Composable
fun MarqueeText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    startPadding: Dp = 0.dp,
) {
    MarqueeText(
        text = text,
        fontSize = style.fontSize,
        fontWeight = fontWeight ?: style.fontWeight ?: FontWeight.Normal,
        color = color,
        modifier = modifier,
        fontFamily = style.fontFamily,
        startPadding = startPadding,
    )
}
