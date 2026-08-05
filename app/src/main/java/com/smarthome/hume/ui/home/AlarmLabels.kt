package com.smarthome.hume.ui.home

/**
 * Nhan trang thai bao dong dung chung cho cac sheet trong tab Nha.
 *
 * Truoc day ham nay nam trong SceneGrid.kt - phan con lai cua khoi "Kich ban"
 * da bi go bo. File kich ban da duoc xoa han trong Dot 3 nen ham chuyen sang
 * day, van cung package nen moi noi goi cu khong phai sua.
 */
internal fun alarmoLabel(state: String): String = when (state) {
    "armed_away" -> "B\u00e1o \u0111\u1ed9ng: Ra ngo\u00e0i"
    "armed_home" -> "B\u00e1o \u0111\u1ed9ng: \u1ede nh\u00e0"
    "armed_night" -> "B\u00e1o \u0111\u1ed9ng: Ban \u0111\u00eam"
    "armed_custom_bypass" -> "B\u00e1o \u0111\u1ed9ng: T\u00f9y ch\u1ec9nh"
    else -> "B\u00e1o \u0111\u1ed9ng"
}
