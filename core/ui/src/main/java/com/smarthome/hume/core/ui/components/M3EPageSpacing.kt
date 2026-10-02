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

/**
 * Scroll-end clearance for the four tab pages that sit beneath the floating navbar.
 * The page content keeps a consistent 120dp bottom clearance; navbar and system
 * insets remain handled independently by the root layout.
 */
fun Modifier.m3ePageBottomPadding(): Modifier =
    padding(bottom = 120.dp)

/**
 * Padding day cho trang full-screen KHONG co navbar (vd. Brief mo dang
 * overlay phu ca navbar): the cuoi cach day man hinh dung M3ECardGap.
 */
fun Modifier.m3eScreenBottomPadding(): Modifier =
    navigationBarsPadding()
        .padding(bottom = M3ECardGap)
