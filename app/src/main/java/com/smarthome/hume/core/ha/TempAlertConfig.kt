package com.smarthome.hume.core.ha

import android.content.Context
import android.content.SharedPreferences

/**
 * Port 1:1 iOS TempAlertConfig.swift (v2.7) — chon phong + nguong
 * nong/lanh cho canh bao nhiet do.
 *
 * iOS doc truc tiep UserDefaults de ca app (UI) lan trigger handler
 * (HomeAssistantManager) dung chung mot nguon cau hinh; Android dung
 * SharedPreferences file [PREFS_NAME] voi dung ten key nhu iOS
 * ("temp_alert_*"), de mot ngay UI cai dat doc chung khong lech key.
 *
 * LUU Y (doc code iOS, khong phai doan): TempAlert tren iOS KHONG tao
 * automation HA nao — no chi (1) dua sensor vao watched set
 * (HomeAssistantManager.swift:555-556), (2) dua sensor vao bucket
 * .trigger (dong 233) de nhan event realtime, (3) UI hien Live
 * Activity bieu do khi vuot nguong. Vi vay file nay + TempAlertWatcher
 * cung khong tao/xoa automation: khong co ten automation TempAlert nao
 * tren iOS de port, khong duoc bia ra.
 */
class TempAlertConfig(private val prefs: SharedPreferences) {

    /** Snapshot cau hinh hien tai (port TempAlertConfig.current). */
    data class Current(
        val enabled: Boolean,
        val sensor: String,
        val humiditySensor: String,
        val hot: Double,
        val coldEnabled: Boolean,
        val cold: Double,
    )

    /** Cam bien nhiet do dang theo doi (mac dinh: phong ngu lon). */
    val sensor: String
        get() {
            val s = prefs.getString(KEY_SENSOR, DEFAULT_SENSOR) ?: DEFAULT_SENSOR
            return s.ifEmpty { DEFAULT_SENSOR }
        }

    /** Cam bien do am tuong ung, suy tu cam bien nhiet do. */
    val humiditySensor: String get() = humiditySensorFor(sensor)

    val enabled: Boolean
        get() = if (!prefs.contains(KEY_ENABLED)) true
        else prefs.getBoolean(KEY_ENABLED, true)

    /** Nguong nong mac dinh 37 do (iOS). */
    val hot: Double
        get() = if (!prefs.contains(KEY_HOT)) 37.0
        else prefs.getFloat(KEY_HOT, 37f).toDouble()

    val coldEnabled: Boolean get() = prefs.getBoolean(KEY_COLD_ENABLED, false)

    /** Nguong lanh mac dinh 16 do (iOS). */
    val cold: Double
        get() = if (!prefs.contains(KEY_COLD)) 16.0
        else prefs.getFloat(KEY_COLD, 16f).toDouble()

    /** Cau hinh hien tai (port TempAlertConfig.current). */
    fun current(): Current = Current(
        enabled = enabled,
        sensor = sensor,
        humiditySensor = humiditySensor,
        hot = hot,
        coldEnabled = coldEnabled,
        cold = cold,
    )

    companion object {
        /** File prefs rieng cho cai dat automation (temp alert + zone arrival). */
        const val PREFS_NAME = "hume_automation"

        /** Port TempAlertConfig.defaultSensor. */
        const val DEFAULT_SENSOR = "sensor.cam_bien_moi_truong_phong_ngu_lon_temperature"

        const val KEY_ENABLED = "temp_alert_enabled"
        const val KEY_SENSOR = "temp_alert_sensor"
        const val KEY_HOT = "temp_alert_hot"
        const val KEY_COLD_ENABLED = "temp_cold_alert_enabled"
        const val KEY_COLD = "temp_alert_cold"

        fun load(context: Context): TempAlertConfig =
            TempAlertConfig(
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
            )

        /** Port TempAlertConfig.humiditySensor(for:): "_temperature" -> "_humidity". */
        fun humiditySensorFor(tempSensor: String): String =
            tempSensor.replace("_temperature", "_humidity")

        /**
         * Danh sach phong chon cam bien — copy nguyen
         * AutomationSettingsView.swift:31-39 (ten hien thi + entity_id).
         * UI cai dat sau nay dung truc tiep, khong tu dat lai ten/sensor.
         */
        val TEMP_ROOMS: List<Pair<String, String>> = listOf(
            "Phòng ngủ lớn" to "sensor.cam_bien_moi_truong_phong_ngu_lon_temperature",
            "Phòng ngủ bé" to "sensor.cam_bien_moi_truong_phong_ngu_be_temperature",
            "Phòng giặt" to "sensor.cam_bien_moi_truong_phong_giat_temperature",
            "Nhà tắm T2" to "sensor.cam_bien_moi_truong_nha_tam_t2_temperature",
            "Hành lang T2" to "sensor.cam_bien_moi_truong_hanh_lang_t2_temperature",
            "Tầng 1" to "sensor.cam_bien_moi_truong_t1_temperature",
            "Tầng 3" to "sensor.cam_bien_moi_truong_t3_temperature",
        )
    }
}
