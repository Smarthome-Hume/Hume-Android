package com.smarthome.hume.ui.home

import com.smarthome.hume.core.model.HomeEntity

/**
 * Best-effort detection of the energy related sensors in a Home Assistant
 * instance. Everything here is heuristic: the user can override later.
 */
internal object EnergyDetect {

    private fun HomeEntity.idText(): String = entityId.lowercase()

    private fun candidates(entities: Map<String, HomeEntity>, deviceClass: String): List<HomeEntity> =
        entities.values.filter { it.entityId.startsWith("sensor.") && it.deviceClass() == deviceClass }

    /** Instantaneous PV production in W. Negative values are ignored, they are grid export. */
    fun solarPower(entities: Map<String, HomeEntity>): HomeEntity? =
        candidates(entities, "power").firstOrNull { e ->
            val id = e.idText()
            (id.contains("solar") || id.contains("pv_power") || id.contains("pv1") ||
                id.contains("inverter") || id.contains("mat_troi")) && !id.contains("grid")
        }

    /** Daily produced energy in kWh. */
    fun dailyEnergy(entities: Map<String, HomeEntity>): HomeEntity? {
        val energy = candidates(entities, "energy")
        return energy.firstOrNull { e ->
            val id = e.idText()
            (id.contains("today") || id.contains("daily") || id.contains("hom_nay")) &&
                (id.contains("solar") || id.contains("pv") || id.contains("production") || id.contains("yield"))
        } ?: energy.firstOrNull { e ->
            val id = e.idText()
            id.contains("today") || id.contains("daily") || id.contains("hom_nay")
        } ?: energy.firstOrNull()
    }

    fun gridPower(entities: Map<String, HomeEntity>): HomeEntity? =
        candidates(entities, "power").firstOrNull { it.idText().contains("grid") }

    fun loadPower(entities: Map<String, HomeEntity>): HomeEntity? =
        candidates(entities, "power").firstOrNull { e ->
            val id = e.idText()
            id.contains("load") || id.contains("house") || id.contains("consumption") || id.contains("tieu_thu")
        }

    /** Storage battery state of charge in percent. */
    fun battery(entities: Map<String, HomeEntity>): HomeEntity? =
        candidates(entities, "battery").firstOrNull { e ->
            val id = e.idText()
            id.contains("powerwall") || id.contains("battery_soc") || id.contains("pin") ||
                id.contains("storage") || id.contains("ess")
        } ?: candidates(entities, "battery").maxByOrNull { it.numericState ?: -1.0 }

    /** Remaining runtime, if the inverter publishes one. */
    fun batteryRuntime(entities: Map<String, HomeEntity>): HomeEntity? =
        entities.values.firstOrNull { e ->
            val id = e.idText()
            e.entityId.startsWith("sensor.") &&
                (id.contains("runtime") || id.contains("time_remaining") || id.contains("time_to_full") ||
                    id.contains("backup_time"))
        }

    /** True when the storage battery is charging rather than discharging. */
    fun charging(entities: Map<String, HomeEntity>): Boolean {
        val flag = entities.values.firstOrNull {
            it.entityId.startsWith("binary_sensor.") && it.idText().contains("charging")
        }
        if (flag != null) return flag.isOn
        val batteryPower = candidates(entities, "power").firstOrNull { e ->
            val id = e.idText()
            id.contains("battery") || id.contains("powerwall")
        }
        val value = batteryPower?.numericState ?: return false
        return value > 0
    }

    /** Extra tiles shown next to the room cards. */
    fun highlightSensors(entities: Map<String, HomeEntity>, limit: Int = 4): List<HomeEntity> =
        entities.values
            .filter { it.entityId.startsWith("sensor.") && it.deviceClass() == "power" && (it.numericState ?: 0.0) > 0 }
            .sortedByDescending { it.numericState ?: 0.0 }
            .take(limit)
}
