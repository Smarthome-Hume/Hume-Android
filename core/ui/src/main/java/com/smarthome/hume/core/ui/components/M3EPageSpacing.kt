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
 * Padding day chuan cho moi trang: the cuoi cach navbar DUNG M3ECardGap
 * (14dp), tren moi may. navigationBarsPadding loai tru bien dong cua
 * navigation bar he thong.
 *
 * (2026-10-01, user: giu khoang cach the cuoi - navbar): navbar duc
 * 100%, khong gap 20dp duoi → padding = chieu cao navbar (86dp) +
 * M3ECardGap (14dp).
 */
fun Modifier.m3ePageBottomPadding(): Modifier =
    navigationBarsPadding()
        .padding(bottom = 86.dp + M3ECardGap)

/**
 * Padding day cho trang full-screen KHONG co navbar (vd. Brief mo dang
 * overlay phu ca navbar): the cuoi cach day man hinh dung M3ECardGap.
 */
fun Modifier.m3eScreenBottomPadding(): Modifier =
    navigationBarsPadding()
        .padding(bottom = M3ECardGap)
