package com.smarthome.hume.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

/** Package cua app duoc do (applicationId trong app/build.gradle.kts). */
internal const val APP_PACKAGE = "com.smarthome.hume"

/** Nhan tab duoi cung theo HumeTab.label (Models.kt:39). */
internal const val TAB_HOME = "Nhà"
internal const val TAB_ENERGY = "Năng lượng"

/**
 * Phong mac dinh (RoomBubbleConfig.all, Models.kt) — thu theo thu tu nay
 * de mo RoomSheet. Neu user da doi ten/xoa het phong mac dinh thi
 * openFirstRoomSheet() tra ve false va benchmark tu skip (xem README).
 */
internal val DEFAULT_ROOM_NAMES = listOf(
    "Phòng khách",
    "Phòng ngủ chính",
    "Phòng bếp",
    "Phòng trẻ em",
    "Phòng thờ",
    "Phòng tắm",
    "Phòng giặt",
    "Hành lang",
)

/**
 * true neu man hinh chinh da hien (da dang nhap). Chua dang nhap thi
 * MainActivity hien LoginScreen, khong co tab "Nhà".
 */
internal fun MacrobenchmarkScope.isLoggedIn(): Boolean =
    device.wait(Until.hasObject(By.text(TAB_HOME)), 3_000)

/** Bam tab duoi cung theo label; tra ve false neu khong tim thay tab. */
internal fun MacrobenchmarkScope.tapTab(label: String): Boolean {
    val tab = device.findObject(By.text(label))
    if (!tab.exists()) return false
    tab.click()
    device.waitForIdle()
    return true
}

/** Cuon len (noi dung chay len) [times] lan, moi lan 20 buoc cho muot. */
internal fun MacrobenchmarkScope.swipeUp(times: Int, steps: Int = 20) {
    val w = device.displayWidth
    val h = device.displayHeight
    repeat(times) {
        device.swipe(w / 2, (h * 0.8).toInt(), w / 2, (h * 0.3).toInt(), steps)
        device.waitForIdle()
    }
}

/** Cuon xuong (noi dung chay xuong) [times] lan. */
internal fun MacrobenchmarkScope.swipeDown(times: Int, steps: Int = 20) {
    val w = device.displayWidth
    val h = device.displayHeight
    repeat(times) {
        device.swipe(w / 2, (h * 0.3).toInt(), w / 2, (h * 0.8).toInt(), steps)
        device.waitForIdle()
    }
}

/**
 * Mo RoomSheet bang cach bam ten phong mac dinh dau tien tim thay.
 * Tra ve false neu khong thay phong nao (chua dang nhap / da doi ten het).
 */
internal fun MacrobenchmarkScope.openFirstRoomSheet(): Boolean {
    for (name in DEFAULT_ROOM_NAMES) {
        val room = device.findObject(By.text(name))
        if (room.exists()) {
            room.click()
            device.waitForIdle()
            return true
        }
    }
    return false
}

/**
 * Bam thu vao hang thiet bi dau tien trong RoomSheet (best-effort):
 * sheet chiem hau het man hinh; diem (50% rong, 40% cao) roi vao vung
 * hang thiet bi dau tien o duoi header phong. Neu bam truot (khong mo
 * the mo rong) thi benchmark van do duoc cuon sheet — xem README de
 * chinh toa do / ten phong cho dung may va du lieu that cua user.
 */
internal fun MacrobenchmarkScope.tapFirstDeviceRow() {
    val w = device.displayWidth
    val h = device.displayHeight
    device.click(w / 2, (h * 0.40).toInt())
    device.waitForIdle()
}
