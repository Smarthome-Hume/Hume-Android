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

    /** HomeEntity.minutesAgo() in Models.swift. */
    fun minutesAgo(): Int? {
        val changed = lastChanged ?: return null
        val millis = runCatching { java.time.Instant.parse(changed).toEpochMilli() }
            .recoverCatching { java.time.OffsetDateTime.parse(changed).toInstant().toEpochMilli() }
            .getOrNull() ?: return null
        return ((System.currentTimeMillis() - millis) / 60_000L).toInt()
    }
}

fun HAEntity.toHomeEntity() = HomeEntity(entityId, state, attributes, lastChanged, lastUpdated)

enum class HumeTab(val label: String) { Home("Nh\u00e0"), Energy("N\u0103ng l\u01b0\u1ee3ng"), Security("An ninh"), Profile("H\u1ed3 s\u01a1"), AI("AI") }

data class RoomConfig(
    val name: String,
    val rawKey: String,
    val lightEntity: String,
    val tempEntity: String,
    val humidityEntity: String,
    val contactEntity: String?,
    val icon: String,
    val hasClimate: Boolean,
    val climateEntity: String? = null,
)

/** RoomConfig.climateRooms / basicRooms, ported 1:1 from Models.swift. */
object DefaultRooms {
    val climateRooms = listOf(
        RoomConfig("Ph\u00f2ng Ng\u1ee7", "Ph\u00f2ng<br>Ng\u1ee7 ch\u00ednh", "light.bedroom", "sensor.cam_bien_moi_truong_phong_ngu_lon_temperature", "sensor.cam_bien_moi_truong_phong_ngu_lon_humidity", "binary_sensor.cam_bien_cua_phong_ngu_chinh_contact", "bed", true, "climate.air_condition"),
        RoomConfig("Ph\u00f2ng Tr\u1ebb Em", "Ph\u00f2ng<br>Tr\u1ebb em", "light.bedroom_spare", "sensor.cam_bien_moi_truong_phong_ngu_be_temperature", "sensor.cam_bien_moi_truong_phong_ngu_be_humidity", "binary_sensor.cam_bien_cua_phong_ngu_be_contact", "child", true, "climate.dieu_hoa_2"),
        RoomConfig("Ph\u00f2ng Th\u1edd", "Ph\u00f2ng<br>Th\u1edd", "light.worship_room", "sensor.cam_bien_moi_truong_t3_temperature", "sensor.cam_bien_moi_truong_t3_humidity", "binary_sensor.cam_bien_pir_phong_tho_occupancy", "sparkles", true, "climate.dieu_hoa"),
    )
    val basicRooms = listOf(
        RoomConfig("Ph\u00f2ng Kh\u00e1ch", "Ph\u00f2ng<br>Kh\u00e1ch", "light.living_room", "sensor.cam_bien_moi_truong_t1_temperature", "sensor.cam_bien_moi_truong_t1_humidity", "binary_sensor.cam_bien_cua_kinh_contact", "sofa", false),
        RoomConfig("Ph\u00f2ng T\u1eafm", "Ph\u00f2ng<br>T\u1eafm", "light.cong_tac_nha_ve_sinh_t2_l1", "sensor.cam_bien_moi_truong_nha_tam_t2_temperature", "sensor.cam_bien_moi_truong_nha_tam_t2_humidity", "binary_sensor.cam_bien_cua_phong_tam_contact", "bath", false),
        RoomConfig("Ph\u00f2ng B\u1ebfp", "Ph\u00f2ng<br>B\u1ebfp", "light.kitchen_room", "sensor.cam_bien_moi_truong_t1_temperature", "sensor.cam_bien_moi_truong_t1_humidity", null, "kitchen", false),
        RoomConfig("Ph\u00f2ng Gi\u1eb7t", "Ph\u00f2ng<br>Gi\u1eb7t", "light.washing", "sensor.cam_bien_moi_truong_phong_giat_temperature", "sensor.cam_bien_moi_truong_phong_giat_humidity", null, "washer", false),
        RoomConfig("H\u00e0nh Lang", "H\u00e0nh<br>Lang", "light.hall", "sensor.cam_bien_moi_truong_hanh_lang_t2_temperature", "sensor.cam_bien_moi_truong_hanh_lang_t2_humidity", null, "hallway", false),
    )
    val all = climateRooms + basicRooms
}

/** DeviceConfig in Models.swift: one row inside the room detail sheet. */
data class DeviceConfig(
    val type: String,
    val entity: String,
    val label: String,
    val sub: String,
    val icon: String,
    val powerEntity: String? = null,
) {
    companion object {
        fun toggle(entity: String, label: String, sub: String, icon: String, powerEntity: String? = null) =
            DeviceConfig("toggle", entity, label, sub, icon, powerEntity)

        fun climate(
            entity: String,
            label: String = "\u0110i\u1ec1u h\u00f2a",
            sub: String = "\u0110i\u1ec1u h\u00f2a nhi\u1ec1u ch\u1ebf \u0111\u1ed9",
        ) = DeviceConfig("climate", entity, label, sub, "snowflake", null)
    }
}

/** RoomBubbleConfig in Models.swift: hardcoded device list per room. */
data class RoomBubbleConfig(
    val key: String,
    val label: String,
    val icon: String,
    val tempEntity: String?,
    val humidityEntity: String?,
    val devices: List<DeviceConfig>,
) {
    companion object {
        val all: List<RoomBubbleConfig> = listOf(
            RoomBubbleConfig(
                key = "Ph\u00f2ng<br>Ng\u1ee7 ch\u00ednh",
                label = "Ph\u00f2ng ng\u1ee7 ch\u00ednh",
                icon = "bed",
                tempEntity = "sensor.cam_bien_moi_truong_phong_ngu_lon_temperature",
                humidityEntity = "sensor.cam_bien_moi_truong_phong_ngu_lon_humidity",
                devices = listOf(
                    DeviceConfig.climate("climate.air_condition"),
                    DeviceConfig.toggle("light.cong_tac_phong_ngu_l2", "Ban c\u00f4ng", "\u0110\u00e8n tr\u1eafng ban c\u00f4ng t\u1ea7ng 2", "sun"),
                    DeviceConfig.toggle("light.cong_tac_phong_ngu_l1", "\u0110\u00e8n tr\u1ea7n", "\u0110\u00e8n tr\u1eafng tr\u00ean tr\u1ea7n", "bulb"),
                    DeviceConfig.toggle("light.smartlight", "\u0110\u00e8n th\u00f4ng minh", "\u0110\u00e8n \u1ed1p tr\u1ea7n th\u00f4ng minh \u0111\u1ed5i m\u00e0u", "bulb"),
                    DeviceConfig.toggle("light.table_led", "\u0110\u00e8n b\u00e0n h\u1ecdc", "\u0110\u00e8n b\u00e0n th\u00f4ng minh \u0111\u1ed5i m\u00e0u", "desk"),
                    DeviceConfig.toggle("switch.cong_tac_phong_ngu_l3", "Smartlight", "C\u00f4ng t\u1eafc smartlight ph\u00f2ng ng\u1ee7", "switch"),
                ),
            ),
            RoomBubbleConfig(
                key = "Ph\u00f2ng<br>Tr\u1ebb em",
                label = "Ph\u00f2ng tr\u1ebb em",
                icon = "child",
                tempEntity = "sensor.cam_bien_moi_truong_phong_ngu_be_temperature",
                humidityEntity = "sensor.cam_bien_moi_truong_phong_ngu_be_humidity",
                devices = listOf(
                    DeviceConfig.climate("climate.dieu_hoa_2"),
                    DeviceConfig.toggle("light.cong_tac_phong_ngu_nho_left", "D\u1ea3i \u0111\u00e8n", "D\u1ea3i \u0111\u00e8n ph\u00f2ng tr\u1ebb em", "bulb"),
                    DeviceConfig.toggle("light.cong_tac_phong_ngu_nho_right", "\u0110\u00e8n tr\u1ea7n", "\u0110\u00e8n tr\u1ea7n ph\u00f2ng tr\u1ebb em", "bulb"),
                ),
            ),
            RoomBubbleConfig(
                key = "Ph\u00f2ng<br>Th\u1edd",
                label = "Ph\u00f2ng th\u1edd",
                icon = "sparkles",
                tempEntity = "sensor.cam_bien_moi_truong_t3_temperature",
                humidityEntity = "sensor.cam_bien_moi_truong_t3_humidity",
                devices = listOf(
                    DeviceConfig.climate("climate.dieu_hoa"),
                    DeviceConfig.toggle("light.den_phong_tho_l1", "Ph\u00f2ng th\u1edd", "\u0110\u00e8n ph\u00f2ng th\u1edd", "bulb"),
                    DeviceConfig.toggle("light.cong_tac_phong_du_tru", "D\u1ef1 tr\u1eef", "C\u00f4ng t\u1eafc d\u1ef1 tr\u1eef", "switch"),
                    DeviceConfig.toggle("light.den_phong_tho_l2", "Ban c\u00f4ng", "\u0110\u00e8n ban c\u00f4ng t\u1ea7ng 3", "sun"),
                ),
            ),
            RoomBubbleConfig(
                key = "Ph\u00f2ng<br>Kh\u00e1ch",
                label = "Ph\u00f2ng kh\u00e1ch",
                icon = "sofa",
                tempEntity = "sensor.cam_bien_moi_truong_t1_temperature",
                humidityEntity = "sensor.cam_bien_moi_truong_t1_humidity",
                devices = listOf(
                    DeviceConfig.toggle("light.cong_tac_4_nut_l1", "\u0110\u00e8n 1", "\u0110\u00e8n tr\u1ea7n ph\u00f2ng kh\u00e1ch", "bulb"),
                    DeviceConfig.toggle("light.cong_tac_4_nut_l3", "\u0110\u00e8n 2", "\u0110\u00e8n tr\u1ea7n ph\u00f2ng kh\u00e1ch", "bulb"),
                    DeviceConfig.toggle("light.cong_tac_4_nut_l2", "D\u1ea3i \u0111\u00e8n", "D\u1ea3i \u0111\u00e8n ph\u00f2ng kh\u00e1ch", "bulb"),
                    DeviceConfig.toggle("switch.quat_phong_khach", "Qu\u1ea1t", "Qu\u1ea1t tr\u1ea7n ph\u00f2ng kh\u00e1ch", "fan"),
                    DeviceConfig.toggle("light.cong_tac_4_nut_l4", "\u0110\u00e8n s\u00e2n", "\u0110\u00e8n s\u00e2n tr\u01b0\u1edbc", "sun"),
                    DeviceConfig.toggle("switch.o_cam_ngoai_vi", "Xe \u0111i\u1ec7n", "\u1ed4 c\u1eafm s\u1ea1c xe \u0111i\u1ec7n", "plug", "sensor.o_cam_ngoai_vi_power"),
                ),
            ),
            RoomBubbleConfig(
                key = "Ph\u00f2ng<br>T\u1eafm",
                label = "Ph\u00f2ng t\u1eafm",
                icon = "bath",
                tempEntity = "sensor.cam_bien_moi_truong_nha_tam_t2_temperature",
                humidityEntity = "sensor.cam_bien_moi_truong_nha_tam_t2_humidity",
                devices = listOf(
                    DeviceConfig.toggle("light.cong_tac_nha_ve_sinh_t2_l1", "\u0110\u00e8n", "\u0110\u00e8n nh\u00e0 v\u1ec7 sinh t\u1ea7ng 2", "bulb"),
                    DeviceConfig.toggle("switch.cong_tac_nong_lanh", "N\u00f3ng l\u1ea1nh", "B\u00ecnh n\u00f3ng l\u1ea1nh", "fire", "sensor.cong_tac_nong_lanh_power"),
                ),
            ),
            RoomBubbleConfig(
                key = "Ph\u00f2ng<br>B\u1ebfp",
                label = "Ph\u00f2ng b\u1ebfp",
                icon = "kitchen",
                tempEntity = "sensor.cam_bien_moi_truong_t1_temperature",
                humidityEntity = "sensor.cam_bien_moi_truong_t1_humidity",
                devices = listOf(
                    DeviceConfig.toggle("light.cong_tac_phong_an_l2", "\u0110\u00e8n tr\u1ea7n", "\u0110\u00e8n tr\u1ea7n ph\u00f2ng \u0103n", "bulb"),
                    DeviceConfig.toggle("light.cong_tac_phong_an_l3", "D\u1ea3i \u0111\u00e8n", "D\u1ea3i \u0111\u00e8n ph\u00f2ng \u0103n", "bulb"),
                    DeviceConfig.toggle("light.cong_tac_wc_t1", "\u0110\u00e8n WC", "\u0110\u00e8n nh\u00e0 v\u1ec7 sinh t\u1ea7ng 1", "bulb"),
                    DeviceConfig.toggle("switch.o_cam_bep_tu", "B\u1ebfp t\u1eeb", "\u1ed4 c\u1eafm b\u1ebfp t\u1eeb", "cooking", "sensor.o_cam_bep_tu_power"),
                    DeviceConfig.toggle("switch.o_cam_tu_lanh", "T\u1ee7 l\u1ea1nh", "\u1ed4 c\u1eafm t\u1ee7 l\u1ea1nh", "snowflake", "sensor.o_cam_tu_lanh_power"),
                    DeviceConfig.toggle("switch.o_cam_noi_chien", "N\u1ed3i chi\u00ean", "\u1ed4 c\u1eafm n\u1ed3i chi\u00ean", "cooking", "sensor.o_cam_noi_chien_power"),
                    DeviceConfig.toggle("switch.o_cam_may_rua_bat", "M\u00e1y r\u1eeda b\u00e1t", "\u1ed4 c\u1eafm m\u00e1y r\u1eeda b\u00e1t", "dishwasher", "sensor.o_cam_may_rua_bat_power"),
                ),
            ),
            RoomBubbleConfig(
                key = "Ph\u00f2ng<br>Gi\u1eb7t",
                label = "Ph\u00f2ng gi\u1eb7t",
                icon = "washer",
                tempEntity = "sensor.cam_bien_moi_truong_phong_giat_temperature",
                humidityEntity = "sensor.cam_bien_moi_truong_phong_giat_humidity",
                devices = listOf(
                    DeviceConfig.toggle("light.cong_tac_hanh_lang_t3_l2", "\u0110\u00e8n tr\u1ea7n", "\u0110\u00e8n tr\u1ea7n ph\u00f2ng gi\u1eb7t", "bulb"),
                    DeviceConfig.toggle("light.cong_tac_wc_t3_left", "\u0110\u00e8n WC", "\u0110\u00e8n nh\u00e0 v\u1ec7 sinh t\u1ea7ng 3", "bulb"),
                    DeviceConfig.toggle("switch.o_cam_phong_giat", "\u1ed4 c\u1eafm m\u00e1y gi\u1eb7t", "\u1ed4 c\u1eafm m\u00e1y gi\u1eb7t", "washer", "sensor.o_cam_may_giat_power"),
                    DeviceConfig.toggle("switch.o_cam_may_say", "\u1ed4 c\u1eafm m\u00e1y s\u1ea5y", "\u1ed4 c\u1eafm m\u00e1y s\u1ea5y", "dryer", "sensor.o_cam_may_say_power"),
                ),
            ),
            RoomBubbleConfig(
                key = "H\u00e0nh<br>Lang",
                label = "H\u00e0nh lang",
                icon = "hallway",
                tempEntity = "sensor.cam_bien_moi_truong_hanh_lang_t2_temperature",
                humidityEntity = "sensor.cam_bien_moi_truong_hanh_lang_t2_humidity",
                devices = listOf(
                    DeviceConfig.toggle("light.cong_tac_hanh_lang_t1_l2", "\u0110\u00e8n HL T1", "\u0110\u00e8n h\u00e0nh lang t\u1ea7ng 1", "bulb"),
                    DeviceConfig.toggle("light.cong_tac_hanh_lang_t2_l2", "\u0110\u00e8n HL T2", "\u0110\u00e8n h\u00e0nh lang t\u1ea7ng 2", "bulb"),
                    DeviceConfig.toggle("light.cong_tac_hanh_lang_t3_l1", "\u0110\u00e8n HL T3", "\u0110\u00e8n h\u00e0nh lang t\u1ea7ng 3", "bulb"),
                    DeviceConfig.toggle("light.cong_tac_hanh_lang_t1_l1", "\u0110\u00e8n c\u1ea7u thang T1", "\u0110\u00e8n c\u1ea7u thang t\u1ea7ng 1", "stairs"),
                    DeviceConfig.toggle("light.cong_tac_hanh_lang_t3_l3", "\u0110\u00e8n c\u1ea7u thang T2", "\u0110\u00e8n c\u1ea7u thang t\u1ea7ng 2", "stairs"),
                ),
            ),
        )

        fun find(key: String): RoomBubbleConfig? = all.firstOrNull { it.key == key }
    }
}
