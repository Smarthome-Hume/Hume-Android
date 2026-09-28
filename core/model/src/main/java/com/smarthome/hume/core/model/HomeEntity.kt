package com.smarthome.hume.core.model

/**
 * Domain model trung tam — UI khong bao gio thay entity_id tho cua HA.
 * Theo docs/dashboard-architecture.md: Data / Binding / UI tach 3 lop.
 */

sealed interface HomeEntity {
    val entityId: String
    val areaId: String?
    val friendlyName: String

    data class Light(
        override val entityId: String,
        override val areaId: String?,
        override val friendlyName: String,
        val isOn: Boolean,
        val brightness: Int?, // 0-255
        val colorTemp: Int?,
    ) : HomeEntity

    data class Climate(
        override val entityId: String,
        override val areaId: String?,
        override val friendlyName: String,
        val hvacMode: String, // off, cool, heat, auto, dry, fan_only
        val currentTemp: Double?,
        val targetTemp: Double?,
    ) : HomeEntity

    data class Sensor(
        override val entityId: String,
        override val areaId: String?,
        override val friendlyName: String,
        val deviceClass: String?, // temperature, humidity, pm25, power, energy, battery...
        val state: String,
        val unit: String?,
        val value: Double?,
    ) : HomeEntity

    data class BinarySensor(
        override val entityId: String,
        override val areaId: String?,
        override val friendlyName: String,
        val deviceClass: String?, // door, window, motion, occupancy...
        val isOn: Boolean,
        val lastChanged: Long?,
    ) : HomeEntity

    data class Switch(
        override val entityId: String,
        override val areaId: String?,
        override val friendlyName: String,
        val isOn: Boolean,
    ) : HomeEntity

    data class Cover(
        override val entityId: String,
        override val areaId: String?,
        override val friendlyName: String,
        val position: Int?, // 0-100
        val isOpen: Boolean,
    ) : HomeEntity

    data class Lock(
        override val entityId: String,
        override val areaId: String?,
        override val friendlyName: String,
        val isLocked: Boolean,
    ) : HomeEntity

    data class Camera(
        override val entityId: String,
        override val areaId: String?,
        override val friendlyName: String,
        val snapshotUrl: String?,
        val streamUrl: String?,
    ) : HomeEntity

    data class Fan(
        override val entityId: String,
        override val areaId: String?,
        override val friendlyName: String,
        val isOn: Boolean,
        val speed: Int?,
    ) : HomeEntity
}

/** Phong — gom entity theo area. */
data class HomeArea(
    val areaId: String,
    val name: String,
    val iconName: String, // ten Material Symbol (outlined-only)
    val temperature: Double?,
    val humidity: Double?,
    val entities: List<HomeEntity> = emptyList(),
) {
    val lightsOn: Int get() = entities.filterIsInstance<HomeEntity.Light>().count { it.isOn }
    val devicesOn: Int get() = entities.count {
        when (it) {
            is HomeEntity.Light -> it.isOn
            is HomeEntity.Switch -> it.isOn
            is HomeEntity.Fan -> it.isOn
            else -> false
        }
    }
    /** Card fill theo den bat (r.light) — nhu demo rev12. */
    val lightOn: Boolean get() = entities.filterIsInstance<HomeEntity.Light>().any { it.isOn }
}

/** Che do an ninh — nhu demo: O nha / Vang nha / Ban dem / Tat. */
enum class SecurityMode { Home, Away, Night, Off }

/** Trang thai nang luong cho flow card + battery bar. */
data class EnergyState(
    val solarKw: Double,
    val gridKw: Double,   // + = lay tu luoi, - = ban ra luoi
    val homeKw: Double,
    val batteryKw: Double, // + = dang xa, - = dang sac
    val batteryCharging: Boolean,
    val soc: Int,          // 0-100
    val pvStrings: List<Double> = emptyList(),  // PV1, PV2...
    val breakers: List<Double> = emptyList(),   // CB1, CB2, CB3...
) {
    /** Nguyen tac da chot: xa -> Su dung can truoc; sac -> Du tru day 20% truoc. */
    val reservePct: Int get() = minOf(soc, 20)
    val usagePct: Int get() = maxOf(0, soc - 20)
}
