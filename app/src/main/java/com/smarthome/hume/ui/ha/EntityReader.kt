package com.smarthome.hume.ui.ha

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.HomeEntity
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonPrimitive

/**
 * EntityReader in Core/EntityObservation.swift.
 *
 * On iOS a card subscribes to ha.entityUpdated filtered down to its own entity
 * id, so a change somewhere else in the house never redraws it. The Compose
 * equivalent is a flow of that one entity: the map plus distinctUntilChanged
 * means the caller only recomposes when its own entity really changed, instead
 * of every time any of the ~1590 entities moves.
 *
 * Read the value inside the smallest composable that needs it. Passing the
 * whole entity map down a screen defeats the purpose.
 *
 * ```
 * val light = rememberEntity(ha, room.lightEntity)
 * ```
 */
@Composable
fun rememberEntity(ha: HomeAssistantRepository, entityId: String): HomeEntity? {
    val initial = remember(ha, entityId) { ha.entities.value[entityId] }
    val flow = remember(ha, entityId) {
        ha.entities.map { it[entityId] }.distinctUntilChanged()
    }
    return flow.collectAsStateWithLifecycle(initialValue = initial).value
}

/** State string of one entity, or null while it is unknown. */
@Composable
fun rememberEntityState(ha: HomeAssistantRepository, entityId: String): String? =
    rememberEntity(ha, entityId)?.state

/** ha.isOn(entityId) in HomeAssistantManager.swift. */
@Composable
fun rememberEntityOn(ha: HomeAssistantRepository, entityId: String): Boolean =
    rememberEntity(ha, entityId)?.isOn == true

/** Numeric reading of one sensor, or null when the state is not a number. */
@Composable
fun rememberEntityValue(ha: HomeAssistantRepository, entityId: String): Double? =
    rememberEntity(ha, entityId)?.numericState

/** Plain text of one attribute, e.g. the climate target temperature. */
fun HomeEntity.attrText(key: String): String? {
    val raw = attributes[key] ?: return null
    return (raw as? JsonPrimitive)?.content ?: raw.toString()
}

/** One attribute of one entity, e.g. the climate target temperature. */
@Composable
fun rememberEntityAttribute(
    ha: HomeAssistantRepository,
    entityId: String,
    key: String,
): String? = rememberEntity(ha, entityId)?.attrText(key)
