package com.smarthome.hume.core.model

import kotlinx.serialization.json.JsonElement

/**
 * Entity HA dang nhe — state tho + attributes (port iOS HomeEntity).
 *
 * LICH SU DI CHUYEN (2026-11-17, parent fix truoc khi push port đợt 2):
 * class nay truoc day nam trong `app/.../core/model/Models.kt` (nguon cua
 * module :app) trong khi `:core:data`, `:feature:home`, `:feature:energy`
 * deu import no (HomeRepository, RoomExpandedCards, PowerwallLiveData,
 * PowerwallViewModel) — cac module do KHONG depend :app nen build se gay
 * "unresolved reference: HomeEntity" o buoc bien dich module. Doi sang
 * :core:model (module nao cung depend) thi moi import cu (cung FQN
 * `com.smarthome.hume.core.model.HomeEntity`) giu nguyen, khong doi
 * hanh vi. `HAEntity` + `toHomeEntity()` van o Models.kt cua :app vi chi
 * :app dung (HomeAssistantRepository).
 */
data class HomeEntity(
    val id: String,
    val state: String,
    val attributes: Map<String, JsonElement> = emptyMap(),
    val lastChanged: String? = null,
    val lastUpdated: String? = null,
) {
    val entityId: String get() = id
    val isOn: Boolean get() = state == "on"
    val numericState: Double? get() = state.toDoubleOrNull()

    /** HomeEntity.minutesAgo() in Models.swift. */
    fun minutesAgo(): Int? {
        val changed = lastChanged ?: return null
        val millis = runCatching { java.time.Instant.parse(changed).toEpochMilli() }
            .recoverCatching { java.time.OffsetDateTime.parse(changed).toInstant().toEpochMilli() }
            .getOrNull() ?: return null
        return ((System.currentTimeMillis() - millis) / 60_000L).toInt()
    }
}
