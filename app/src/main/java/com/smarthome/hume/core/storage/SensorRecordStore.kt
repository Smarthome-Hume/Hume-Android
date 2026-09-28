package com.smarthome.hume.core.storage

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "HumeHA"
private const val PREFS = "hume_sensor_records"
private const val KEY = "sensor_overrides_v1"

/**
 * Local, per-entity overrides for the sensor manager.
 *
 * The SwiftUI app keeps a SensorRecord row per entity (name from the registry
 * plus localName / isHidden / isFrozen, and deletion from the app database).
 * On Android the live data already comes from HomeAssistantRepository, so only
 * the user-owned bits are persisted here and merged on top at read time.
 */
@Serializable
data class SensorOverride(
    val id: String,
    val localName: String? = null,
    val hidden: Boolean = false,
    val frozen: Boolean = false,
    val deleted: Boolean = false,
) {
    val isDefault: Boolean
        get() = localName == null && !hidden && !frozen && !deleted
}

class SensorRecordStore private constructor(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _overrides = MutableStateFlow(load())
    val overrides: StateFlow<Map<String, SensorOverride>> = _overrides.asStateFlow()

    private fun load(): Map<String, SensorOverride> {
        val raw = prefs.getString(KEY, null) ?: return emptyMap()
        return try {
            json.decodeFromString<List<SensorOverride>>(raw).associateBy { it.id }
        } catch (t: Throwable) {
            Log.e(TAG, "SensorRecordStore load failed", t)
            emptyMap()
        }
    }

    private fun persist(next: Map<String, SensorOverride>) {
        _overrides.value = next
        try {
            val payload = json.encodeToString(next.values.filterNot { it.isDefault })
            prefs.edit().putString(KEY, payload).apply()
        } catch (t: Throwable) {
            Log.e(TAG, "SensorRecordStore persist failed", t)
        }
    }

    private fun mutate(id: String, block: (SensorOverride) -> SensorOverride) {
        val current = _overrides.value[id] ?: SensorOverride(id)
        val updated = block(current)
        val next = _overrides.value.toMutableMap()
        if (updated.isDefault) next.remove(id) else next[id] = updated
        persist(next)
    }

    fun get(id: String): SensorOverride = _overrides.value[id] ?: SensorOverride(id)

    /** Empty or blank clears the local name and falls back to the HA name. */
    fun setLocalName(id: String, name: String?) {
        val cleaned = name?.trim()?.takeIf { it.isNotEmpty() }
        mutate(id) { it.copy(localName = cleaned) }
    }

    fun setHidden(id: String, hidden: Boolean) = mutate(id) { it.copy(hidden = hidden) }

    fun setFrozen(id: String, frozen: Boolean) = mutate(id) { it.copy(frozen = frozen) }

    /** "Xo\u00e1 kh\u1ecfi app" in SensorsView: keeps the entity in HA, hides it from this app. */
    fun delete(id: String) = mutate(id) { it.copy(deleted = true) }

    fun deleteAll(ids: Collection<String>) {
        val next = _overrides.value.toMutableMap()
        ids.forEach { id ->
            val current = next[id] ?: SensorOverride(id)
            next[id] = current.copy(deleted = true)
        }
        persist(next)
    }

    /** "Kh\u00f4i ph\u1ee5c to\u00e0n b\u1ed9 t\u1eeb HA": drop every local override. */
    fun restoreAll() = persist(emptyMap())

    companion object {
        @Volatile
        private var instance: SensorRecordStore? = null

        fun get(context: Context): SensorRecordStore =
            instance ?: synchronized(this) {
                instance ?: SensorRecordStore(context).also { instance = it }
            }
    }
}
