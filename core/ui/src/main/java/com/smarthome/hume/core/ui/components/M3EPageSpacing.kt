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
// - Chieu cao navbar: 86dp
// - Margin day navbar: 20dp
// (2026-10-02, user): navbar dac 0.98, thu tu nen -> the -> navbar,
// the cuoi cach navbar 20dp.

/**
 * Padding day chuan cho trang co navbar (user 2026-10-02):
 * the cuoi cach navbar dung 20dp. Navbar dac 0.98 nen content
 * khong chui dang sau — padding = navbar (86dp) + margin day (20dp)
 * + gap (20dp), cong navigationBarsPadding cho system.
 */
fun Modifier.m3ePageBottomPadding(): Modifier =
    navigationBarsPadding()
        .padding(bottom = 86.dp + 20.dp + 20.dp)

/**
 * Padding day cho trang full-screen KHONG co navbar (vd. Brief mo dang
 * overlay phu ca navbar): the cuoi cach day man hinh dung M3ECardGap.
 */
fun Modifier.m3eScreenBottomPadding(): Modifier =
    navigationBarsPadding()
        .padding(bottom = M3ECardGap)
