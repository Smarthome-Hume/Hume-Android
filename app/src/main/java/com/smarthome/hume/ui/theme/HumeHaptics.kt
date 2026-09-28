package com.smarthome.hume.ui.theme

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/*
 * RUNG CUA HE THONG (diem 2).
 *
 * VI SAO TRUOC DAY MAT RUNG:
 *  - CONTEXT_CLICK va KEYBOARD_TAP la hai hang so "yeu" nhat. One UI gan chung
 *    vao muc "Rung khi cham" / rung ban phim; neu muc do rung cham de o 0 hoac
 *    tat thi may im hoan toan.
 *  - performHapticFeedback tra ve false khi View dang tat haptic hoac khi he
 *    thong bo qua hang so do, nhung ban cu khong he kiem tra gia tri tra ve nen
 *    khong co duong lui nao.
 *
 * CACH SUA:
 *  1. Dung hang so dung ngu canh: TOGGLE_ON / TOGGLE_OFF (Android 14+), lui ve
 *     CONFIRM (Android 11+), cuoi cung moi la CONTEXT_CLICK.
 *  2. Gui kem FLAG_IGNORE_VIEW_SETTING de View khong chan.
 *  3. Neu he thong van tu choi (tra ve false) thi tu rung bang Vibrator voi
 *     hieu ung dung san EFFECT_CLICK / EFFECT_TICK.
 *
 * Quy uoc dung trong app:
 *  - toggle(): bat/tat cong tac thiet bi (nhip ro nhat).
 *  - tap():    nhan nut nho, doi che do, tang giam nhiet do.
 *  - longPress(): giu lau de mo man hinh quan ly.
 */
class HumeHaptics(private val view: View) {

    private val vibrator: Vibrator? = run {
        val context = view.context
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
        }
    }

    /** Gat cong tac. Truyen on = false khi dang tat de nhip rung nhe hon. */
    fun toggle(on: Boolean = true) {
        val constant = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                if (on) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> HapticFeedbackConstants.CONFIRM
            else -> HapticFeedbackConstants.CONTEXT_CLICK
        }
        if (!perform(constant)) fallback(VibrationEffect.EFFECT_CLICK, 20L)
    }

    fun tap() {
        if (!perform(HapticFeedbackConstants.CLOCK_TICK)) fallback(VibrationEffect.EFFECT_TICK, 10L)
    }

    fun longPress() {
        if (!perform(HapticFeedbackConstants.LONG_PRESS)) fallback(VibrationEffect.EFFECT_HEAVY_CLICK, 35L)
    }

    private fun perform(constant: Int): Boolean {
        // View bi tat haptic thi moi lenh deu roi vao im lang, bat lai truoc khi goi.
        view.isHapticFeedbackEnabled = true
        return view.performHapticFeedback(constant, HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING)
    }

    private fun fallback(predefined: Int, millis: Long) {
        val motor = vibrator ?: return
        if (!motor.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            motor.vibrate(VibrationEffect.createPredefined(predefined))
        } else {
            motor.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
}

@Composable
fun rememberHumeHaptics(): HumeHaptics {
    val view = LocalView.current
    return remember(view) { HumeHaptics(view) }
}
