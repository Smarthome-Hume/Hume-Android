package com.smarthome.hume.core.model

/**
 * Hardcoded Home Assistant entity IDs ported 1:1 from the SwiftUI app
 * (Hume/Core/Models.swift, Views/Home/*). Do not guess these values;
 * they mirror the real installation.
 */
object HumeConfig {

    // ---- Energy (SolarEnergyCardView.swift / PowerwallCardView.swift) ----
    const val PV_POWER = "sensor.solis_s6_eh1p_total_pv_power_2"
    const val PV_TODAY = "sensor.solis_s6_eh1p_pv_today_energy_generation_2"
    const val BATTERY_SOC = "sensor.solis_s6_eh1p_battery_soc_2"
    const val BATTERY_POWER = "sensor.battery_power_flow"
    const val BACKUP_SOC = "number.solis_s6_eh1p_backup_soc_2"
    const val BATTERY_TIME_LEFT = "sensor.thoi_gian_pin_con_lai"
    const val BATTERY_TIME_TO_FULL = "sensor.battery_time_to_full_2"
    const val GRID_DAILY = "sensor.aptomat_tong_daily"
    const val HOME_DAILY = "sensor.energy_home_daily"

    // ---- Alarm (AlarmLights.swift) ----
    const val ALARM_PRIMARY = "alarm_control_panel.alarmo"
    const val ALARM_FALLBACK = "alarm_control_panel.alarm_security"
    const val ALARM_CODE = "210793"

    /** service suffix, label, HA state */
    val alarmModes: List<Triple<String, String, String>> = listOf(
        Triple("disarm", "T\u1eaft", "disarmed"),
        Triple("arm_home", "Nh\u00e0", "armed_home"),
        Triple("arm_away", "V\u1eafng", "armed_away"),
        Triple("arm_night", "\u0110\u00eam", "armed_night"),
        Triple("arm_custom_bypass", "S\u00e1ng", "armed_custom_bypass"),
    )

    fun alarmLabel(state: String?): String = when (state) {
        "armed_home" -> "\u1ede nh\u00e0"
        "armed_away" -> "V\u1eafng nh\u00e0"
        "armed_night" -> "Ban \u0111\u00eam"
        "armed_custom_bypass" -> "Bu\u1ed5i s\u00e1ng"
        "disarmed" -> "\u0110\u00e3 t\u1eaft"
        "arming", "pending" -> "\u0110ang k\u00edch ho\u1ea1t"
        else -> "B\u00e1o \u0111\u1ed9ng"
    }

    // ---- Small sensor pages (HomeView.swift onAppear) ----
    data class SensorTile(val label: String, val entityId: String, val unit: String, val icon: String)

    val sensorTiles: List<SensorTile> = listOf(
        SensorTile("\u0110i\u1ec7n m\u1eb7t tr\u1eddi", PV_POWER, "W", "sun"),
        SensorTile("S\u1ea3n l\u01b0\u1ee3ng", PV_TODAY, "kWh", "sun"),
        SensorTile("Dung l\u01b0\u1ee3ng Pin", BATTERY_SOC, "%", "battery-full"),
        SensorTile("C\u00f4ng su\u1ea5t Pin", BATTERY_POWER, "W", "battery-charging"),
        SensorTile("\u0110i\u1ec7n l\u01b0\u1edbi", GRID_DAILY, "kW", "plug"),
        SensorTile("\u0110i\u1ec7n ti\u00eau th\u1ee5", HOME_DAILY, "kWh", "house"),
    )

    // ---- Device card selector (DeviceCardView.swift) ----
    const val ACTIVE_CARD = "sensor.dashboard_active_card"
    const val ACTIVE_CARD_2 = "sensor.dashboard_active_card_2"

    data class DeviceCard(val entityId: String, val icon: String, val label: String)

    val deviceCards: Map<String, DeviceCard> = mapOf(
        "Table" to DeviceCard("sensor.o_cam_ban_lam_viec_power", "desk", "B\u00e0n h\u1ecdc"),
        "Dishwasher" to DeviceCard("sensor.o_cam_may_rua_bat_power", "dishwasher", "M\u00e1y r\u1eeda b\u00e1t"),
        "A/C Baby" to DeviceCard("sensor.dieu_hoa_spare_room_power", "snowflake", "\u0110i\u1ec1u ho\u00e0 tr\u1ebb"),
        "Boiler" to DeviceCard("sensor.cong_tac_nong_lanh_power", "fire", "N\u00f3ng l\u1ea1nh"),
        "A/C Master" to DeviceCard("sensor.air_condition_current_extrapolated_power", "snowflake", "\u0110i\u1ec1u ho\u00e0 l\u1edbn"),
        "Stove" to DeviceCard("sensor.o_cam_bep_tu_power", "cooking", "B\u1ebfp t\u1eeb"),
        "Bicycle Plug" to DeviceCard("sensor.o_cam_ngoai_vi_power", "plug", "\u1ed4 c\u1eafm ngo\u00e0i"),
        "Dryer" to DeviceCard("sensor.o_cam_may_say_power", "dryer", "M\u00e1y s\u1ea5y"),
        "Fridge" to DeviceCard("sensor.o_cam_tu_lanh_power", "snowflake", "T\u1ee7 l\u1ea1nh"),
        "Washing" to DeviceCard("sensor.o_cam_may_giat_power", "washer", "M\u00e1y gi\u1eb7t"),
    )

    // ---- Door card selector (DoorCardView.swift) ----
    data class DoorCard(val entityId: String, val label: String)

    val doorCards: Map<String, DoorCard> = mapOf(
        "Master" to DoorCard("binary_sensor.cam_bien_cua_phong_ngu_chinh_contact", "C\u1eeda ph\u00f2ng ng\u1ee7 ch\u00ednh"),
        "Baby" to DoorCard("binary_sensor.cam_bien_cua_phong_ngu_be_contact", "C\u1eeda ph\u00f2ng tr\u1ebb em"),
        "Glass" to DoorCard("binary_sensor.cam_bien_cua_kinh_contact", "C\u1eeda k\u00ednh"),
        "Batchroom Door" to DoorCard("binary_sensor.cam_bien_cua_phong_tam_contact", "C\u1eeda ph\u00f2ng t\u1eafm"),
    )

    /** Climate modes that count as "running" (ClimateRoomCardView.swift). */
    val activeClimateModes = setOf("heat_cool", "cool", "heat", "fan_only", "dry")

    /** Weekday labels indexed like Calendar.DAY_OF_WEEK (1 = Sunday). */
    val dayNames = listOf("CN", "T2", "T3", "T4", "T5", "T6", "T7")
}
