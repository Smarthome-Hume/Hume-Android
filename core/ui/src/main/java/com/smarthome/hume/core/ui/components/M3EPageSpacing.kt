package com.smarthome.hume.core.ui.components

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import com.smarthome.hume.core.ui.theme.M3ETokens

/**
 * Khoang cach chuan giua cac the (user 2026-10-01): dung cho tab -> the,
 * the -> the, the -> navbar — mot gia tri duy nhat cho ca he thong.
 * Cắt dưới iOS theo lệnh user 22:03: 14 -> 10dp (khe giữa các thẻ ngắn hơn chuẩn iOS).
 *
 * B-09: gia tri chuan CHI nam o [M3ETokens.Spacing.CardGap] (10dp).
 * [M3ECardGap] chi con la alias de tuong thich — 2 file feature
 * (DeviceEdit.kt, RoomManagement.kt) van import no truc tiep, nam NGOAI
 * pham vi worker B (chi sua core/ui). Worker A migrate 2 file do sang
 * [M3ETokens.Spacing.CardGap] roi moi xoa alias nay.
 */
@Deprecated(
    "Dung M3ETokens.Spacing.CardGap thay the",
    ReplaceWith("M3ETokens.Spacing.CardGap", "com.smarthome.hume.core.ui.theme.M3ETokens"),
)
val M3ECardGap = M3ETokens.Spacing.CardGap

/**
 * Scroll-end clearance for the four tab pages that sit beneath the floating navbar.
 * The page content keeps a consistent 120dp bottom clearance; navbar and system
 * insets remain handled independently by the root layout.
 */
fun Modifier.m3ePageBottomPadding(): Modifier =
    padding(bottom = M3ETokens.Spacing.PageBottom)

/**
 * Padding day cho trang full-screen KHONG co navbar (vd. Brief mo dang
 * overlay phu ca navbar): the cuoi cach day man hinh dung CardGap.
 */
fun Modifier.m3eScreenBottomPadding(): Modifier =
    navigationBarsPadding()
        .padding(bottom = M3ETokens.Spacing.CardGap)
