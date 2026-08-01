package com.smarthome.hume.ui.energy

import com.smarthome.hume.core.ha.HomeAssistantRepository

/**
 * NumberRowView in EnergyView.swift writes inverter settings through
 * number.set_value. The repository has no dedicated helper, so this is it.
 */
fun HomeAssistantRepository.setNumberValue(entityId: String, value: Double) {
    val rounded = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
    callService(
        domain = "number",
        service = "set_value",
        dataJson = "{\"entity_id\":\"" + entityId + "\",\"value\":" + rounded + "}",
    )
}
