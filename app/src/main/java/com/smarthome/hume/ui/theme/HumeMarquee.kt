@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.smarthome.hume.ui.theme

import androidx.compose.foundation.basicMarquee
import androidx.compose.ui.Modifier

/*
 * CHAY CHU KHI TRAN THE - port tu ban HTML cocopi.
 *
 * Ban HTML:
 *   .marquee-wrap { white-space: nowrap; overflow: hidden }
 *   .marquee-wrap > span { padding-right: 50px }
 *   animation: marquee 8s linear infinite   (0% translate(0) -> 100% translate(-100%))
 *   ... va animation chi duoc gan khi chu THUC SU tran, nguoc lai la `none`.
 *
 * Compose: basicMarquee() cung chi chay khi noi dung rong hon khung chua, con
 * vua khung thi dung yen - dung y het ban web. Lap vo han, co nghi giua hai
 * vong giong khoang padding-right 50px cua ban goc.
 *
 * LUU Y: chi co tac dung khi Text dat maxLines = 1 va softWrap = false, va
 * KHONG duoc dung TextOverflow.Ellipsis (dau ... se de len chu dang chay).
 */
fun Modifier.humeMarquee(): Modifier = this.basicMarquee(
    iterations = Int.MAX_VALUE,
    repeatDelayMillis = 1_200,
)
