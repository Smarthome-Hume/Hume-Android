package com.smarthome.hume

import androidx.compose.ui.graphics.Color

/**
 * Tach ARGB Int thanh [Color] an toan — ham chung duy nhat trong module
 * :app cho MainActivity + M3ERootScreen (audit C-02).
 *
 * (2026-10-01, fix crash khi ap dung mau tuy chinh: dung cach tach ARGB
 * an toan thay vi Color(Long.toULong()) de tranh van de bit pattern.)
 * Tra ve null neu tach that bai (giu nguyen hanh vi cu o ca 2 call site).
 */
internal fun colorFromArgb(argb: Int): Color? = runCatching {
    Color(
        red = ((argb shr 16) and 0xFF) / 255f,
        green = ((argb shr 8) and 0xFF) / 255f,
        blue = (argb and 0xFF) / 255f,
        alpha = ((argb shr 24) and 0xFF) / 255f,
    )
}.getOrNull()
