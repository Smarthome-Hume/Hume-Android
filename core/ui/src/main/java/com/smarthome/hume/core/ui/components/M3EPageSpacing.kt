package com.smarthome.hume.core.ui.components

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Khoang cach chuan giua cac the (user 2026-10-01): dung cho tab -> the,
 * the -> the, the -> navbar — mot gia tri duy nhat cho ca he thong.
 */
val M3ECardGap = 14.dp

// Navbar floating (M3ERootScreen.M3ENavBar), do tu code:
// (2026-10-01) NavbarHeight/NavbarBottomMargin khong con dung sau khi
// bo 120dp khoi m3ePageBottomPadding (user: navbar trong suot, content
// cuon tu do duoi navbar).

/**
 * (2026-10-01, user: xoa sach padding day content): chi giu
 * navigationBarsPadding(). Khong con 120dp/100dp.
 */
fun Modifier.m3ePageBottomPadding(): Modifier =
    navigationBarsPadding()

/**
 * Padding day cho trang full-screen KHONG co navbar (vd. Brief mo dang
 * overlay phu ca navbar): the cuoi cach day man hinh dung M3ECardGap.
 */
fun Modifier.m3eScreenBottomPadding(): Modifier =
    navigationBarsPadding()
        .padding(bottom = M3ECardGap)
