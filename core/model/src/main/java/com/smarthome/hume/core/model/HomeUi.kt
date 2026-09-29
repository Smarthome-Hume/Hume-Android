package com.smarthome.hume.core.model

/** San luong dien mat troi 1 ngay (cho bieu do pill 7 ngay). */
data class SolarDay(
    val label: String, // "T2".."CN"
    val kwh: Float,
)

/** Trang thai pin (Solis). */
data class BatteryUi(
    val soc: Int = 0,
    val powerKw: Double = 0.0, // >0 = dang sac, <0 = dang xa (theo sensor battery_power_flow, giong Hume goc)
    val timeText: String? = null, // "Còn 3g12p" / "Sạc đầy sau 1g05p"
    val endTime: String? = null, // "14:30" - gio ket thuc sac/xa
) {
    val isCharging: Boolean get() = powerKw > 0.05
    val reservePct: Int get() = minOf(soc, 20)
    val usagePct: Int get() = maxOf(0, soc - 20)
}

/** Trang thai bao dong. */
data class AlarmUi(
    val entityId: String,
    val state: String, // disarmed / armed_home / armed_away / armed_night / armed_custom_bypass / triggered...
    val label: String, // "Tắt" / "Ở nhà" / ...
    val isArmed: Boolean,
)

/** Thong bao dieu kien (cua moi mo, cam bien moi kich hoat...). */
data class HomeNotification(
    val id: String,
    val title: String,
    val body: String,
    val timeText: String,
)

enum class DeviceKind { Toggle, Climate }

data class DeviceUi(
    val entityId: String,
    val label: String,
    val sub: String,
    val iconKey: String,
    val kind: DeviceKind = DeviceKind.Toggle,
    val isOn: Boolean = false,
    val powerW: Double? = null,
)

data class ClimateUi(
    val entityId: String,
    val currentTemp: Double? = null,
    val targetTemp: Double? = null,
    val hvacMode: String = "off", // off/cool/heat/auto/fan_only/dry
    val isOn: Boolean = false,
    val modes: List<String> = emptyList(), // hvac_modes HA ho tro
)

data class RoomUi(
    val key: String,
    val name: String,
    val iconKey: String,
    val tempC: Double? = null,
    val humidityPct: Double? = null,
    val lightEntityId: String? = null,
    val lightOn: Boolean = false,
    val devicesOn: Int = 0,
    val deviceCount: Int = 0,
    val climate: ClimateUi? = null,
    val devices: List<DeviceUi> = emptyList(),
)

/** Toan bo state trang Nha. */
data class HomeUiState(
    val userName: String = "",
    val connected: Boolean = false,
    val solarWeek: List<SolarDay> = emptyList(),
    val solarTodayKwh: Double? = null,
    val solarNowKw: Double = 0.0,
    val battery: BatteryUi = BatteryUi(),
    val alarm: AlarmUi? = null,
    val lightsOn: List<DeviceUi> = emptyList(),
    val rooms: List<RoomUi> = emptyList(),
    val notifications: List<HomeNotification> = emptyList(),
)
