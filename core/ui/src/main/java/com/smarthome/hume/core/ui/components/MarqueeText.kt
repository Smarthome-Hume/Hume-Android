package com.smarthome.hume.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Chu chay marquee khi tran khung.
 *
 * Phan tich bug cu ("Ban cong T2" mat chu B): text chi dai hon khung
 * vai px -> marquee tu cuon sang trai; Box cat cung (clip) nen giua
 * animation ky tu dau bi day ra khoi vung nhin thay, trong nhu bi
 * mat chu du icon khong he che.
 *
 * Ban nay giu dang chu chay nhung lam dung:
 * - Fade 2 mep (DstIn) theo vi tri cuon: mep nao dang cat chu thi mo
 *   dan chu thay vi cat cung -> nhin la hieu chu dang cuon.
 * - Mep trai fade neo vao hinh tron icon BG (startPadding): chu chay
 *   den cham vien tron moi mo dan, khong co "buc tuong thang dung"
 *   loi giua khong trung.
 * - Nhip: nghi 1.5s o dau (hien tron tu ky tu dau) -> cuon het
 *   (toc do ti le do tran) -> nghi 1.2s -> cuon ve mem -> lap lai.
 */
@Composable
fun MarqueeText(
    text: String,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    color: Color,
    modifier: Modifier = Modifier,
    fadeWidth: Dp = 14.dp,
    /**
     * Khoang cach tu mep trai khung cuon den vi tri text luc nghi.
     * Dung de mo rong viewport den sat vien icon tron: text chay den
     * cham icon moi bat dau mo (fade neo o mep trai khung).
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
        modifier = modifier
            .horizontalScroll(scrollState, enabled = false)
            .marqueeEdgeFade(scrollState, fadeWidth),
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

/**
 * Mo dan mep trai/phai tuy theo text dang bi cat o mep nao.
 * Do mo ti le voi muc cat: o sat mep (chua cat) thi khong fade,
 * giu ky tu dau/cuoi sac net khi dung yen.
 */
private fun Modifier.marqueeEdgeFade(scrollState: ScrollState, fadeWidth: Dp): Modifier =
    drawWithContent {
        drawContent()
        val max = scrollState.maxValue.toFloat()
        if (max <= 0f) return@drawWithContent
        val fw = fadeWidth.toPx().coerceAtMost(size.width / 2f)
        if (fw <= 0f) return@drawWithContent
        val v = scrollState.value.toFloat()
        val startFade = (v / fw).coerceIn(0f, 1f)
        val endFade = ((max - v) / fw).coerceIn(0f, 1f)
        if (startFade <= 0f && endFade <= 0f) return@drawWithContent
        val w = size.width
        drawRect(
            brush = Brush.horizontalGradient(
                0f to Color.Black.copy(alpha = 1f - startFade),
                (fw / w) to Color.Black,
                ((w - fw) / w) to Color.Black,
                1f to Color.Black.copy(alpha = 1f - endFade),
            ),
            blendMode = BlendMode.DstIn,
        )
    }
