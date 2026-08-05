package com.smarthome.hume.ui.theme

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/*
 * RUNG CUA HE THONG (diem 2).
 *
 * Dung View.performHapticFeedback cua Android chu khong tu goi Vibrator:
 *  - Khong can quyen VIBRATE.
 *  - Ton trong cai dat cua may: neu nguoi dung tat "Phan hoi xuc giac" trong
 *    Cai dat > Am thanh va rung cua One UI thi app cung khong rung, dung nhu
 *    moi app he thong cua Samsung.
 *  - One UI map cac hang so nay sang dong co rung tuyen tinh cua may nen cam
 *    giac giong cong tac trong Cai dat nhanh.
 *
 * Quy uoc dung trong app:
 *  - toggle(): bat/tat cong tac thiet bi (nhip ro nhat).
 *  - tap():    nhan nut nho, doi che do, tang giam nhiet do.
 *  - longPress(): giu lau de mo man hinh quan ly.
 */
class HumeHaptics(private val view: View) {
    fun toggle() {
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    fun tap() {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun longPress() {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}

@Composable
fun rememberHumeHaptics(): HumeHaptics {
    val view = LocalView.current
    return remember(view) { HumeHaptics(view) }
}
