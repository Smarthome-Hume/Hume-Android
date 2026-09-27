package com.smarthome.hume.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class HAEntity(
    @SerialName("entity_id") val entityId: String,
    val state: String,
    val attributes: Map<String, JsonElement> = emptyMap(),
    @SerialName("last_changed") val lastChanged: String? = null,
    @SerialName("last_updated") val lastUpdated: String? = null,
)

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
}

fun HAEntity.toHomeEntity() = HomeEntity(entityId, state, attributes, lastChanged, lastUpdated)

enum class HumeTab(val label: String) { Home("Nhà"), Energy("Năng lượng"), Security("An ninh"), Profile("Hồ sơ"), AI("AI") }

data class RoomConfig(val name: String, val rawKey: String, val lightEntity: String, val tempEntity: String, val humidityEntity: String, val contactEntity: String?, val icon: String, val hasClimate: Boolean, val climateEntity: String? = null)

object DefaultRooms {
    val climateRooms = listOf(
        RoomConfig("Phòng Ngủ", "Phòng<br>Ngủ chính", "light.bedroom", "sensor.cam_bien_moi_truong_phong_ngu_lon_temperature", "sensor.cam_bien_moi_truong_phong_ngu_lon_humidity", "binary_sensor.cam_bien_cua_phong_ngu_chinh_contact", "bed", true, "climate.air_condition"),
        RoomConfig("Phòng Trẻ Em", "Phòng<br>Trẻ em", "light.bedroom_spare", "sensor.cam_bien_moi_truong_phong_ngu_be_temperature", "sensor.cam_bien_moi_truong_phong_ngu_be_humidity", "binary_sensor.cam_bien_cua_phong_ngu_be_contact", "child", true, "climate.dieu_hoa_2"),
        RoomConfig("Phòng Thờ", "Phòng<br>Thờ", "light.worship_room", "sensor.cam_bien_moi_truong_t3_temperature", "sensor.cam_bien_moi_truong_t3_humidity", "binary_sensor.cam_bien_pir_phong_tho_occupancy", "sparkles", true, "climate.dieu_hoa"),
    )
    val basicRooms = listOf(
        RoomConfig("Phòng Khách", "Phòng<br>Khách", "light.living_room", "sensor.cam_bien_moi_truong_t1_temperature", "sensor.cam_bien_moi_truong_t1_humidity", "binary_sensor.cam_bien_cua_kinh_contact", "sofa", false),
        RoomConfig("Phòng Tắm", "Phòng<br>Tắm", "light.cong_tac_nha_ve_sinh_t2_l1", "sensor.cam_bien_moi_truong_nha_tam_t2_temperature", "sensor.cam_bien_moi_truong_nha_tam_t2_humidity", "binary_sensor.cam_bien_cua_phong_tam_contact", "bath", false),
        RoomConfig("Phòng Bếp", "Phòng<br>Bếp", "light.kitchen_room", "sensor.cam_bien_moi_truong_t1_temperature", "sensor.cam_bien_moi_truong_t1_humidity", null, "kitchen", false),
    )
}
