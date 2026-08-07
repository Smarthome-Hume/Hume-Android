package com.smarthome.hume.ui.home

import com.smarthome.hume.core.model.HomeEntity
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.util.Locale
import kotlin.math.abs

/*
 * Entity reading helpers shared by every card in ui.home.
 *
 * The old auto-detecting "SolarEnergyCard" lived here and guessed its sensors
 * from device_class keywords. The SwiftUI app never guesses: it reads the fixed
 * IDs in HumeConfig, so the home screen now uses SolarChartCard and BatteryCard
 * and that card is gone.
 */

internal fun HomeEntity.attrString(key: String): String? =
    (attributes[key] as? JsonPrimitive)?.contentOrNull

internal fun HomeEntity.deviceClass(): String? = attrString("device_class")

internal fun HomeEntity.unit(): String = attrString("unit_of_measurement").orEmpty()

/** friendly_name, falling back to the entity ID like the SwiftUI accessor does. */
internal fun HomeEntity.friendly(): String = attrString("friendly_name") ?: id

/** Value plus unit, one decimal below 100 and none above (HumeTheme formatting). */
internal fun HomeEntity.formatted(): String {
    val value = numericState ?: return state
    val text = if (abs(value) >= 100) String.format(Locale.US, "%.0f", value)
    else String.format(Locale.US, "%.1f", value)
    val suffix = unit()
    return if (suffix.isBlank()) text else "$text $suffix"
}
