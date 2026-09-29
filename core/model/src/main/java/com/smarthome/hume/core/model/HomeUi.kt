package com.smarthome.hume.core.model

/** San luong dien mat troi 1 ngay (cho bieu do pill 7 ngay). */
data class SolarDay(
    val label: String, // "T2".."CN"
    val kwh: Float,
)

/** Trang thai pin (Solis) — cong thuc giong Hume goc (EnergyDetect): */
data class BatteryUi(
    val soc: Int = 0,
    val powerKw: Double = 0.0,
    val powerW: Double = 0.0, // cong suat W de xac dinh 3 trang thai
    val backupSoc: Int = 20, // number.solis_s6_eh1p_backup_soc_2, mac dinh 20
    val timeText: String? = null, // friendly_time cua sensor runtime
    val endTime: String? = null, // "14:30" - gio ket thuc sac/xa
) {
    // Hume goc: resting = power 0..5W, discharging = power < 0, charging = power > 5W
    val isResting: Boolean get() = powerW in 0.0..5.0
    val isDischarging: Boolean get() = powerW < 0.0
    val isCharging: Boolean get() = !isResting && !isDischarging
    val statusText: String
        get() = when {
            isResting -> "NGHỈ"
            isDischarging -> "ĐANG XẢ"
            else -> "ĐANG SẠC"
        }
    val reservePct: Int get() = minOf(soc, backupSoc)
    val usagePct: Int get() = maxOf(0, soc - backupSoc)
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
    /** So phut tu luc sensor trigger (de gom goi y trung: giu cai gan nhat). */
    val minutesAgo: Int? = null,
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

/** Trang thai ket noi HA cho den neon avatar. */
enum class ConnectionState { Connected, Connecting, Disconnected }

/** Toan bo state trang Nha. */
data class HomeUiState(
    val userName: String = "",
    val avatarUrl: String? = null,
    /** Key luu avatar theo user (user_id HA, fallback ten). */
    val userKey: String = "",
    val connected: Boolean = false,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val solarWeek: List<SolarDay> = emptyList(),
    val solarTodayKwh: Double? = null,
    val solarNowKw: Double = 0.0,
    val battery: BatteryUi = BatteryUi(),
    val alarm: AlarmUi? = null,
    val lightsOn: List<DeviceUi> = emptyList(),
    val rooms: List<RoomUi> = emptyList(),
    /** Danh sach tim kiem day du: gom ca climate + entity ngoai config tinh,
     * da phan loai dung (dieu hoa / den / o cam / ...). */
    val searchDevices: List<DeviceUi> = emptyList(),
    val notifications: List<HomeNotification> = emptyList(),
)
