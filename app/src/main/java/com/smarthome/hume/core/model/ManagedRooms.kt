package com.smarthome.hume.core.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Model phong/thiet bi duoc quan ly dong (them/sua/xoa/keo-tha tu UI Quan ly phong).
 * Port tu iOS RoomStore (UserDefaults) sang DataStore Preferences (JSON string).
 *
 * Khac voi model cu [DeviceConfig]/[RoomBubbleConfig] (hardcode, dung cho Home UI),
 * model nay co `id` dang UUID de CRUD on dinh, va convert qua lai khi render.
 */

/** Thiet bi trong phong. `type`: "toggle" | "fan" | "climate" | "cover" | "lock". */
@Serializable
data class ManagedDevice(
    /** UUID string. */
    val id: String,
    val name: String,
    val entityId: String,
    val type: String,
    val icon: String,
    val sub: String = "",
    val powerEntity: String? = null,
) {
    /** Convert sang model cu dung cho Home UI hien tai. */
    fun toDeviceConfig(): DeviceConfig = DeviceConfig(
        type = type,
        entity = entityId,
        label = name,
        sub = sub,
        icon = icon,
        powerEntity = powerEntity,
    )

    companion object {
        /** Tao thiet bi moi voi UUID ngau nhien. */
        fun create(
            name: String,
            entityId: String,
            type: String,
            icon: String,
            sub: String = "",
            powerEntity: String? = null,
        ): ManagedDevice = ManagedDevice(
            id = UUID.randomUUID().toString(),
            name = name,
            entityId = entityId,
            type = type,
            icon = icon,
            sub = sub,
            powerEntity = powerEntity,
        )
    }
}

/** Phong duoc quan ly dong. */
@Serializable
data class ManagedRoom(
    /** UUID string. */
    val id: String,
    val name: String,
    val icon: String,
    val tempEntity: String = "",
    val humidityEntity: String = "",
    val devices: List<ManagedDevice> = emptyList(),
    val sortOrder: Int = 0,
) {
    /** Convert sang model cu dung cho Home UI hien tai. */
    fun toRoomBubbleConfig(): RoomBubbleConfig = RoomBubbleConfig(
        key = name,
        label = name,
        icon = icon,
        tempEntity = tempEntity.ifBlank { null },
        humidityEntity = humidityEntity.ifBlank { null },
        devices = devices.map { it.toDeviceConfig() },
    )

    companion object {
        /** Tao phong moi voi UUID ngau nhien. */
        fun create(
            name: String,
            icon: String,
            tempEntity: String = "",
            humidityEntity: String = "",
            devices: List<ManagedDevice> = emptyList(),
            sortOrder: Int = 0,
        ): ManagedRoom = ManagedRoom(
            id = UUID.randomUUID().toString(),
            name = name,
            icon = icon,
            tempEntity = tempEntity,
            humidityEntity = humidityEntity,
            devices = devices,
            sortOrder = sortOrder,
        )
    }
}

/**
 * Phong mac dinh: convert tu [RoomBubbleConfig.all] (hardcode) sang [ManagedRoom].
 * Gan UUID moi cho phong + thiet bi, `sortOrder` theo index trong danh sach goc.
 * Dung de seed DataStore lan dau (giong iOS: `rooms = RoomBubbleConfig.all`).
 */
fun defaultRooms(): List<ManagedRoom> =
    RoomBubbleConfig.all.mapIndexed { index, legacy ->
        ManagedRoom(
            id = UUID.randomUUID().toString(),
            name = legacy.label,
            icon = legacy.icon,
            tempEntity = legacy.tempEntity.orEmpty(),
            humidityEntity = legacy.humidityEntity.orEmpty(),
            devices = legacy.devices.map { device ->
                ManagedDevice(
                    id = UUID.randomUUID().toString(),
                    name = device.label,
                    entityId = device.entity,
                    type = device.type,
                    icon = device.icon,
                    sub = device.sub,
                    powerEntity = device.powerEntity,
                )
            },
            sortOrder = index,
        )
    }
