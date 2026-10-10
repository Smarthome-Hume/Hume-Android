package com.smarthome.hume.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.smarthome.hume.core.datastore.humeDataStore
import com.smarthome.hume.core.model.ManagedDevice
import com.smarthome.hume.core.model.ManagedRoom
import com.smarthome.hume.core.model.defaultRooms
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * Data layer cho Quan ly phong — port tu iOS `RoomStore.swift`.
 *
 * Luu danh sach phong dang JSON string trong DataStore Preferences
 * (chung file `hume_settings` voi SettingsStore/SessionStore), key `"managed_rooms"`.
 * Lan dau chua co du lieu thi seed tu [defaultRooms] (convert tu
 * `RoomBubbleConfig.all` hardcode) roi luu lai.
 */
class RoomStore(private val context: Context) {

    private object Keys {
        val ManagedRooms = stringPreferencesKey("managed_rooms")
    }

    private val json = Json { ignoreUnknownKeys = true }

    private fun decode(raw: String?): List<ManagedRoom> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<ManagedRoom>>(raw) }
            .getOrElse { emptyList() }
    }

    private fun encode(rooms: List<ManagedRoom>): String =
        json.encodeToString(rooms)

    /** Flow danh sach phong theo thu tu hien thi. */
    val roomsFlow: Flow<List<ManagedRoom>> =
        context.humeDataStore.data.map { prefs -> decode(prefs[Keys.ManagedRooms]) }

    /** All mutations read the latest value INSIDE edit, including across instances. */
    private suspend fun mutateRooms(transform: (List<ManagedRoom>) -> List<ManagedRoom>) {
        context.humeDataStore.edit { prefs ->
            val raw = prefs[Keys.ManagedRooms]
            val current = if (raw == null) defaultRooms() else decode(raw)
            prefs[Keys.ManagedRooms] = encode(transform(current))
        }
    }

    suspend fun saveRooms(rooms: List<ManagedRoom>) {
        context.humeDataStore.edit { it[Keys.ManagedRooms] = encode(rooms) }
    }

    /** Seed only a missing key. A persisted empty list is a valid configuration. */
    suspend fun loadRooms(): List<ManagedRoom> {
        var loaded: List<ManagedRoom> = emptyList()
        context.humeDataStore.edit { prefs ->
            val raw = prefs[Keys.ManagedRooms]
            loaded = if (raw == null) defaultRooms() else decode(raw)
            if (raw == null) prefs[Keys.ManagedRooms] = encode(loaded)
        }
        return loaded.sortedBy { it.sortOrder }
    }

    suspend fun addRoom(room: ManagedRoom) = mutateRooms { rooms ->
        val nextOrder = (rooms.maxOfOrNull { it.sortOrder } ?: -1) + 1
        rooms + room.copy(sortOrder = nextOrder)
    }

    suspend fun updateRoom(room: ManagedRoom) = mutateRooms { rooms ->
        rooms.map { if (it.id == room.id) room else it }
    }

    suspend fun deleteRoom(roomId: String) = mutateRooms { rooms ->
        rooms.filterNot { it.id == roomId }
    }

    suspend fun moveRoom(from: Int, to: Int) = mutateRooms { current ->
        val rooms = current.sortedBy { it.sortOrder }.toMutableList()
        if (from !in rooms.indices || to !in rooms.indices || from == to) {
            current
        } else {
            val item = rooms.removeAt(from)
            rooms.add(to, item)
            rooms.mapIndexed { index, room -> room.copy(sortOrder = index) }
        }
    }

    suspend fun addDevice(roomId: String, device: ManagedDevice) = mutateRooms { rooms ->
        rooms.map { room ->
            if (room.id == roomId) room.copy(devices = room.devices + device) else room
        }
    }

    suspend fun updateDevice(roomId: String, device: ManagedDevice) = mutateRooms { rooms ->
        rooms.map { room ->
            if (room.id == roomId) room.copy(
                devices = room.devices.map { if (it.id == device.id) device else it },
            ) else room
        }
    }

    suspend fun deleteDevice(roomId: String, deviceId: String) = mutateRooms { rooms ->
        rooms.map { room ->
            if (room.id == roomId) room.copy(
                devices = room.devices.filterNot { it.id == deviceId },
            ) else room
        }
    }

    suspend fun resetToDefaults() = mutateRooms { defaultRooms() }
}
