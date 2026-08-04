package com.smarthome.hume.ui.home

/*
 * Kich ban da bi loai bo theo yeu cau: khong con khoi "Kich ban" tren trang Nha
 * va khong con man hinh kich ban (ScenesSheet / SceneEditorSheet /
 * SceneScheduleSheet khong con duoc mo tu bat ky dau).
 *
 * File nay chi giu lai nhan trang thai bao dong dung chung, de cac sheet khac
 * van bien dich duoc.
 */
internal fun alarmoLabel(state: String): String = when (state) {
    "armed_away" -> "B\u00e1o \u0111\u1ed9ng: Ra ngo\u00e0i"
    "armed_home" -> "B\u00e1o \u0111\u1ed9ng: \u1ede nh\u00e0"
    "armed_night" -> "B\u00e1o \u0111\u1ed9ng: Ban \u0111\u00eam"
    "armed_custom_bypass" -> "B\u00e1o \u0111\u1ed9ng: T\u00f9y ch\u1ec9nh"
    else -> "B\u00e1o \u0111\u1ed9ng"
}
