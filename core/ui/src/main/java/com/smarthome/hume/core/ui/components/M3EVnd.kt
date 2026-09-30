package com.smarthome.hume.core.ui.components

import java.text.NumberFormat
import java.util.Locale

private val vnFormat: NumberFormat by lazy {
    NumberFormat.getInstance(Locale("vi", "VN"))
}

/**
 * Format tien VND dung chung: 1234567 -> "1.234.567đ".
 * Thay the 2 ban private: fmtVnd (BriefScreen.kt) va vnd (EnergyConsTab.kt).
 */
fun Long.toVnd(): String = vnFormat.format(this) + "đ"
