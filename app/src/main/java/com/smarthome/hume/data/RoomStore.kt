package com.smarthome.hume.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.smarthome.hume.core.datastore.humeDataStore
import com.smarthome.hume.core.model.ManagedDevice
import com.smarthome.hume.core.model.ManagedRoom
import com.smarthome.hume.core.model.defaultRooms
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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

    suspend fun saveRooms(rooms: List<ManagedRoom>) {
        context.humeDataStore.edit { prefs ->
            prefs[Keys.ManagedRooms] = encode(rooms)
        }
    }

    /**
     * Doc danh sach phong. Neu DataStore chua co gi thi tra ve [defaultRooms]
     * va luu ngay (seed lan dau, giong iOS).
     */
    suspend fun loadRooms(): List<ManagedRoom> {
        val existing = roomsFlow.first()
        if (existing.isNotEmpty()) return existing.sortedBy { it.sortOrder }
        val defaults = defaultRooms()
        saveRooms(defaults)
        return defaults
    }

    // MARK: - CRUD phong

    /** Them phong moi; `sortOrder` tu gan = max hien tai + 1. */
    suspend fun addRoom(room: ManagedRoom) {
        val rooms = roomsFlow.first().toMutableList()
        val nextOrder = (rooms.maxOfOrNull { it.sortOrder } ?: -1) + 1
        rooms.add(room.copy(sortOrder = nextOrder))
        saveRooms(rooms)
    }

    suspend fun updateRoom(room: ManagedRoom) {
        val rooms = roomsFlow.first().toMutableList()
        val index = rooms.indexOfFirst { it.id == room.id }
        if (index >= 0) {
            rooms[index] = room
            saveRooms(rooms)
        }
    }

    suspend fun deleteRoom(roomId: String) {
        val rooms = roomsFlow.first().filterNot { it.id == roomId }
        saveRooms(rooms)
    }

    /** Di chuyen phong tu vi tri [from] sang [to]; danh lai sortOrder 0..n-1. */
    suspend fun moveRoom(from: Int, to: Int) {
        val rooms = roomsFlow.first().sortedBy { it.sortOrder }.toMutableList()
        if (from !in rooms.indices || to !in rooms.indices || from == to) return
        val item = rooms.removeAt(from)
        rooms.add(to, item)
        saveRooms(rooms.mapIndexed { index, room -> room.copy(sortOrder = index) })
    }

    // MARK: - CRUD thiet bi trong phong

    suspend fun addDevice(roomId: String, device: ManagedDevice) {
        val rooms = roomsFlow.first().toMutableList()
        val index = rooms.indexOfFirst { it.id == roomId }
        if (index >= 0) {
            val room = rooms[index]
            rooms[index] = room.copy(devices = room.devices + device)
            saveRooms(rooms)
        }
    }

    suspend fun updateDevice(roomId: String, device: ManagedDevice) {
        val rooms = roomsFlow.first().toMutableList()
        val roomIndex = rooms.indexOfFirst { it.id == roomId }
        if (roomIndex < 0) return
        val devices = rooms[roomIndex].devices.toMutableList()
        val deviceIndex = devices.indexOfFirst { it.id == device.id }
        if (deviceIndex >= 0) {
            devices[deviceIndex] = device
            rooms[roomIndex] = rooms[roomIndex].copy(devices = devices)
            saveRooms(rooms)
        }
    }

    suspend fun deleteDevice(roomId: String, deviceId: String) {
        val rooms = roomsFlow.first().toMutableList()
        val index = rooms.indexOfFirst { it.id == roomId }
        if (index >= 0) {
            val room = rooms[index]
            rooms[index] = room.copy(devices = room.devices.filterNot { it.id == deviceId })
            saveRooms(rooms)
        }
    }

    /** Reset ve danh sach mac dinh (convert tu RoomBubbleConfig.all). */
    suspend fun resetToDefaults() {
        saveRooms(defaultRooms())
    }
}
