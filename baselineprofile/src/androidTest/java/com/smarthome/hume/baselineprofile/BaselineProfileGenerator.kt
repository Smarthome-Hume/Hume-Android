package com.smarthome.hume.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Sinh Baseline Profile cho app (Batch 1 — do hieu nang).
 *
 * Chay bang: ./gradlew :app:generateBenchmarkBaselineProfile
 * (tren S26 Ultra da dang nhap; xem baselineprofile/README.md).
 * Ket qua duoc plugin copy ve app/src/benchmark/generated/baselineProfiles/
 * roi dong goi vao APK release (assets/dexopt/baseline.prof).
 *
 * Cac journey chi chay khi da dang nhap (isLoggedIn): neu may do dang o
 * man hinh Login thi generator chi thu duoc startup profile, khong fail.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        // 1. Startup profile: khoi dong lanh + cuon trang Nha.
        rule.collect(packageName = APP_PACKAGE, includeInStartupProfile = true) {
            pressHome()
            startActivityAndWait()
            if (isLoggedIn()) {
                swipeUp(3)
            }
        }
        // 2. Runtime profile: tab Nang luong + cuon (khong dua vao startup profile).
        rule.collect(packageName = APP_PACKAGE, includeInStartupProfile = false) {
            pressHome()
            startActivityAndWait()
            if (isLoggedIn() && tapTab(TAB_ENERGY)) {
                swipeUp(3)
                swipeDown(2)
            }
        }
        // 3. Runtime profile: mo RoomSheet + the mo rong dau tien + cuon sheet.
        rule.collect(packageName = APP_PACKAGE, includeInStartupProfile = false) {
            pressHome()
            startActivityAndWait()
            if (isLoggedIn() && openFirstRoomSheet()) {
                tapFirstDeviceRow()
                swipeUp(3)
            }
        }
    }
}
