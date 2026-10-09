package com.smarthome.hume.data

import com.smarthome.hume.core.model.ManagedDevice
import com.smarthome.hume.core.model.ManagedRoom
import com.smarthome.hume.feature.home.DynDevice
import com.smarthome.hume.feature.home.DynRoom
import com.smarthome.hume.feature.home.RoomStore as RoomStoreUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Adapter: real RoomStore (app module) -> UI RoomStore interface (feature/home).
 * Converts ManagedRoom/ManagedDevice <-> DynRoom/DynDevice.
 */
class RoomStoreAdapter(
    private val real: RoomStore,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) : RoomStoreUi {

    override val roomsFlow: StateFlow<List<DynRoom>> =
        real.roomsFlow
            .map { rooms -> rooms.sortedBy { it.sortOrder }.map { it.toDyn() } }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    override suspend fun addRoom(room: DynRoom) {
        real.addRoom(room.toManaged())
    }

    override suspend fun updateRoom(room: DynRoom) {
        real.updateRoom(room.toManaged())
    }

    override suspend fun deleteRoom(roomId: String) {
        real.deleteRoom(roomId)
    }

    override suspend fun moveRoom(from: Int, to: Int) {
        real.moveRoom(from, to)
    }

    override suspend fun resetToDefaults() {
        real.resetToDefaults()
    }

    override suspend fun addDevice(roomId: String, device: DynDevice) {
        real.addDevice(roomId, device.toManaged())
    }

    override suspend fun updateDevice(roomId: String, device: DynDevice) {
        real.updateDevice(roomId, device.toManaged())
    }

    override suspend fun deleteDevice(roomId: String, deviceId: String) {
        real.deleteDevice(roomId, deviceId)
    }

    private fun ManagedRoom.toDyn() = DynRoom(
        id = id,
        name = name,
        icon = icon,
        tempEntity = tempEntity,
        humidityEntity = humidityEntity,
        devices = devices.map { it.toDyn() },
        sortOrder = sortOrder,
    )

    private fun ManagedDevice.toDyn() = DynDevice(
        id = id,
        name = name,
        entityId = entityId,
        type = type,
        icon = icon,
        sub = sub,
        powerEntity = powerEntity,
    )

    private fun DynRoom.toManaged() = ManagedRoom(
        id = id,
        name = name,
        icon = icon,
        tempEntity = tempEntity,
        humidityEntity = humidityEntity,
        devices = devices.map { it.toManaged() },
        sortOrder = sortOrder,
    )

    private fun DynDevice.toManaged() = ManagedDevice(
        id = id,
        name = name,
        entityId = entityId,
        type = type,
        icon = icon,
        sub = sub,
        powerEntity = powerEntity,
    )
}
