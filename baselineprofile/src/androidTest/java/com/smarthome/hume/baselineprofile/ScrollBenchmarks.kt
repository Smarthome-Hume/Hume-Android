package com.smarthome.hume.baselineprofile

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import org.junit.Assume
import org.junit.Rule
import org.junit.Test

/**
 * Do frame time (frameDurationCpuMs, frameOverrunMs, jank) khi cuon o
 * 3 man nang nhat: Nha, Nang luong, the mo rong trong phong.
 *
 * Dieu kien chay: S26 Ultra da DANG NHAP san (neu dang o LoginScreen thi
 * test tu skip qua Assume, khong do so lieu ao). Phai sinh Baseline
 * Profile truoc (./gradlew :app:generateBenchmarkBaselineProfile) vi
 * CompilationMode.Partial(BaselineProfileMode.Require) se fail to neu
 * chua co profile — do la chu y, khong phai loi do.
 */
class ScrollBenchmarks {

    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun homeScroll() = rule.measureRepeated(
        packageName = APP_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            Assume.assumeTrue("Can dang nhap san tren may do", isLoggedIn())
        },
    ) {
        swipeUp(4)
        swipeDown(2)
    }

    @Test
    fun energyScroll() = rule.measureRepeated(
        packageName = APP_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            Assume.assumeTrue("Can dang nhap san tren may do", isLoggedIn())
            Assume.assumeTrue("Khong mo duoc tab Nang luong", tapTab(TAB_ENERGY))
        },
    ) {
        swipeUp(4)
        swipeDown(2)
    }

    @Test
    fun expandedCardScroll() = rule.measureRepeated(
        packageName = APP_PACKAGE,
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.Partial(BaselineProfileMode.Require),
        startupMode = StartupMode.WARM,
        iterations = 5,
        setupBlock = {
            pressHome()
            startActivityAndWait()
            Assume.assumeTrue("Can dang nhap san tren may do", isLoggedIn())
            Assume.assumeTrue(
                "Khong mo duoc RoomSheet (khong thay phong mac dinh)",
                openFirstRoomSheet(),
            )
            tapFirstDeviceRow()
        },
    ) {
        swipeUp(3)
        swipeDown(2)
    }
}
