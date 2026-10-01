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
// - item: icon 27dp + spacer 4dp + label 13dp + padding trong 14dp + padding ngoai 8dp = 66dp
// - row: 66dp + padding 20dp = 86dp
// - cach day man hinh 20dp
// => dinh navbar cach day man hinh 106dp (chua ke navigation bar he thong).
private val NavbarHeight = 86.dp
private val NavbarBottomMargin = 20.dp

/**
 * Padding day chuan cho moi trang: the cuoi cach navbar DUNG M3ECardGap
 * (14dp), tren moi may. navigationBarsPadding loai tru bien dong cua
 * navigation bar he thong (cu chi / phim bam) nen gap giu nguyen 14dp.
 *
 * Thay the M3EPageBottomSpacing cu (140dp doan mo, sai tren may co
 * navigation bar nho).
 */
fun Modifier.m3ePageBottomPadding(): Modifier =
    navigationBarsPadding()
        .padding(bottom = NavbarHeight + NavbarBottomMargin + M3ECardGap)
