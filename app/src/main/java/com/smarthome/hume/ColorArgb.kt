package com.smarthome.hume

import androidx.compose.ui.graphics.Color

/**
 * Tach ARGB Long thanh [Color] an toan — ham chung duy nhat trong module
 * :app cho MainActivity + M3ERootScreen (audit C-02).
 *
 * Nhan [Long] vi [com.smarthome.hume.core.datastore.ThemeSettings.customColor]
 * luu trong datastore dang ARGB Long (ThemeStore.kt). `.toInt()` giu nguyen
 * 32 bit thap — chinh la pattern ARGB.
 *
 * (2026-10-01, fix crash khi ap dung mau tuy chinh: dung cach tach ARGB
 * an toan thay vi Color(Long.toULong()) de tranh van de bit pattern.)
 * Tra ve null neu tach that bai (giu nguyen hanh vi cu o ca 2 call site).
 */
internal fun colorFromArgb(argb: Long): Color? = runCatching {
    val argbInt = argb.toInt()
    Color(
        red = ((argbInt shr 16) and 0xFF) / 255f,
        green = ((argbInt shr 8) and 0xFF) / 255f,
        blue = (argbInt and 0xFF) / 255f,
        alpha = ((argbInt shr 24) and 0xFF) / 255f,
    )
}.getOrNull()
