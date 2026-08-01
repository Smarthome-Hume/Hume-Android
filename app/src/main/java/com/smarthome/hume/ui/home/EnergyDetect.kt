package com.smarthome.hume.ui.home

import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.core.model.HumeConfig

/**
 * Energy readings resolved from the exact entity IDs used by the SwiftUI app.
 * Nothing here guesses by device_class any more.
 */
internal object EnergyDetect {

    fun solarPower(entities: Map<String, HomeEntity>): HomeEntity? = entities[HumeConfig.PV_POWER]

    fun dailyEnergy(entities: Map<String, HomeEntity>): HomeEntity? = entities[HumeConfig.PV_TODAY]

    fun battery(entities: Map<String, HomeEntity>): HomeEntity? = entities[HumeConfig.BATTERY_SOC]

    fun batteryPower(entities: Map<String, HomeEntity>): HomeEntity? = entities[HumeConfig.BATTERY_POWER]

    fun soc(entities: Map<String, HomeEntity>): Double = battery(entities)?.numericState ?: 0.0

    fun power(entities: Map<String, HomeEntity>): Double = batteryPower(entities)?.numericState ?: 0.0

    /** number.solis_s6_eh1p_backup_soc_2, defaults to 20% like the iOS card. */
    fun backupSoc(entities: Map<String, HomeEntity>): Double =
        entities[HumeConfig.BACKUP_SOC]?.numericState ?: 20.0

    fun resting(entities: Map<String, HomeEntity>): Boolean {
        val p = power(entities)
        return p in 0.0..5.0
    }

    fun discharging(entities: Map<String, HomeEntity>): Boolean = power(entities) < 0.0

    fun charging(entities: Map<String, HomeEntity>): Boolean =
        !resting(entities) && !discharging(entities)

    /** Time-remaining sensor depends on direction, exactly like PowerwallCardView. */
    fun batteryRuntime(entities: Map<String, HomeEntity>): HomeEntity? {
        val id = if (discharging(entities)) HumeConfig.BATTERY_TIME_LEFT else HumeConfig.BATTERY_TIME_TO_FULL
        return entities[id]
    }

    /** friendly_time attribute first, raw state as fallback. */
    fun runtimeText(entities: Map<String, HomeEntity>): String {
        val entity = batteryRuntime(entities) ?: return "--"
        return entity.attrString("friendly_time") ?: entity.state
    }

    /** Hours remaining, used to compute the finish clock time. */
    fun runtimeHours(entities: Map<String, HomeEntity>): Double? = batteryRuntime(entities)?.numericState

    /** Entity IDs the home screen needs pushed into the realtime bucket. */
    fun watchedIds(): Set<String> = buildSet {
        add(HumeConfig.PV_POWER)
        add(HumeConfig.PV_TODAY)
        add(HumeConfig.BATTERY_SOC)
        add(HumeConfig.BATTERY_POWER)
        add(HumeConfig.BACKUP_SOC)
        add(HumeConfig.BATTERY_TIME_LEFT)
        add(HumeConfig.BATTERY_TIME_TO_FULL)
        add(HumeConfig.GRID_DAILY)
        add(HumeConfig.HOME_DAILY)
        add(HumeConfig.ACTIVE_CARD)
        add(HumeConfig.ACTIVE_CARD_2)
        add(HumeConfig.ALARM_PRIMARY)
        add(HumeConfig.ALARM_FALLBACK)
        HumeConfig.deviceCards.values.forEach { add(it.entityId) }
        HumeConfig.doorCards.values.forEach { add(it.entityId) }
    }
}
