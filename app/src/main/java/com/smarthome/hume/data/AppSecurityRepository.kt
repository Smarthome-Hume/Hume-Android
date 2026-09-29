package com.smarthome.hume.data

import android.content.Context
import com.smarthome.hume.core.data.SecurityRepository
import com.smarthome.hume.core.frigate.FrigateStore
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.HomeEntity as LegacyEntity
import com.smarthome.hume.core.model.RecordingUi
import com.smarthome.hume.core.model.SecurityCamera
import com.smarthome.hume.core.model.SecurityUiState
import com.smarthome.hume.core.model.SensorKind
import com.smarthome.hume.core.model.SensorUi
import com.smarthome.hume.core.storage.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter: map HomeAssistantRepository + FrigateStore sang SecurityUiState (demo v4, tab An ninh).
 * Port dinh nghia camera/sensor tu SecurityView.swift (ui/security cu).
 */
class AppSecurityRepository(
    private val context: Context,
    private val ha: HomeAssistantRepository,
    private val settingsStore: SettingsStore,
    private val scope: CoroutineScope,
) : SecurityRepository {

    private val _state = MutableStateFlow(SecurityUiState(cameras = CAMERAS))
    override val securityState: StateFlow<SecurityUiState> = _state.asStateFlow()

    private val frigate = FrigateStore.get(context)
    private val refreshed = mutableSetOf<String>()

    init {
        scope.launch {
            ha.entities.collect { entities -> rebuild(entities) }
        }
        scope.launch {
            frigate.byCamera.collect { byCamera ->
                val recs = byCamera.mapValues { (_, list) ->
                    list.take(10).map { r ->
                        val d = Date((r.startTime * 1000).toLong())
                        RecordingUi(
                            id = r.id,
                            timeLabel = SimpleDateFormat("HH:mm", Locale.getDefault()).format(d),
                            dateLabel = SimpleDateFormat("dd/MM", Locale.getDefault()).format(d),
                            // Giai quyet duong dan file that tu FrigateStore ngay khi build UI:
                            // feature module khong thay duoc FrigateStore (nam o :app).
                            clipPath = frigate.clipFile(r).absolutePath,
                        )
                    }
                }
                _state.value = _state.value.copy(recordings = recs)
            }
        }
    }

    override fun refreshRecordings(cameraKey: String) {
        scope.launch {
            val settings = settingsStore.settings.first()
            runCatching { frigate.refresh(cameraKey, settings.haUrl, settings.haToken) }
            refreshed += cameraKey
        }
    }

    override fun snapshotUrl(cameraKey: String): String =
        "http://192.168.102.64:5000/api/$cameraKey/latest.jpg"

    private fun rebuild(entities: Map<String, LegacyEntity>) {
        _state.value = _state.value.copy(
            doorSensors = DOOR_SENSORS.map { it.toUi(entities) },
            motionSensors = MOTION_SENSORS.map { it.toUi(entities) },
            envSensors = ENV_SENSORS.map { it.toUi(entities) },
        )
        // Tu dong tai clip cho camera dau tien khi co ket noi.
        if (refreshed.isEmpty() && ha.connected.value) {
            refreshRecordings(CAMERAS.first().key)
        }
    }

    // ---------- dinh nghia (port tu SecurityView.swift) ----------

    private data class SensorDef(
        val id: String,
        val name: String,
        val iconKey: String,
        val kind: SensorKind,
        val warn: Boolean = false,
    ) {
        fun toUi(entities: Map<String, LegacyEntity>): SensorUi {
            val e = entities[id]
            val isOn = e?.isOn == true
            val ago = e?.let { agoText(it) } ?: ""
            return SensorUi(
                entityId = id,
                name = name,
                iconKey = iconKey,
                kind = kind,
                isOn = isOn,
                lastChange = ago,
                warn = warn,
            )
        }

        private fun agoText(e: LegacyEntity): String {
            val m = e.minutesAgo() ?: return "Giám sát"
            return when {
                m < 1 -> "Vừa xong"
                m < 60 -> "$m phút trước"
                m < 60 * 24 -> "${m / 60} giờ trước"
                else -> "${m / (60 * 24)} ngày trước"
            }
        }
    }

    companion object {
        val CAMERAS = listOf(
            SecurityCamera("living", "Phòng khách"),
            SecurityCamera("kitchen", "Phòng ăn"),
            SecurityCamera("outdoor", "Ngoài trời"),
            SecurityCamera("server", "Phòng thờ"),
            SecurityCamera("bedroom", "Phòng ngủ"),
        )

        private val DOOR_SENSORS = listOf(
            SensorDef("binary_sensor.cam_bien_cua_kinh_contact", "Cửa kính", "door", SensorKind.Door),
            SensorDef("binary_sensor.cam_bien_cua_phong_ngu_chinh_contact", "Phòng ngủ chính", "door", SensorKind.Door),
            SensorDef("binary_sensor.cam_bien_cua_phong_ngu_be_contact", "Phòng ngủ bé", "door", SensorKind.Door),
            SensorDef("binary_sensor.cam_bien_cua_phong_tam_contact", "Phòng tắm", "door", SensorKind.Door),
            SensorDef("binary_sensor.cam_bien_cua_ban_cong_tt2_contact", "Ban công T2", "door", SensorKind.Door),
            SensorDef("binary_sensor.cam_bien_ban_cong_t3_contact", "Ban công T3", "door", SensorKind.Door),
        )
        private val MOTION_SENSORS = listOf(
            SensorDef("binary_sensor.cam_bien_pir_t1_occupancy", "Tầng 1", "motion", SensorKind.Motion),
            SensorDef("binary_sensor.cam_bien_pir_t2_occupancy", "Tầng 2", "motion", SensorKind.Motion),
            SensorDef("binary_sensor.cam_bien_pir_t3_occupancy", "Tầng 3", "motion", SensorKind.Motion),
            SensorDef("binary_sensor.cam_bien_hien_dien_presence", "Hiện diện", "presence", SensorKind.Motion),
            SensorDef("binary_sensor.cam_bien_pir_phong_tho_occupancy", "Phòng thờ", "motion", SensorKind.Motion),
            SensorDef("binary_sensor.cam_bien_tuong_t2_occupancy", "Tường T2", "motion", SensorKind.Motion),
        )
        private val ENV_SENSORS = listOf(
            SensorDef("binary_sensor.cam_bien_khoi_smoke", "Khói", "smoke", SensorKind.Smoke, warn = true),
            SensorDef("binary_sensor.cam_bien_nuoc_water_leak", "Rò rỉ nước", "leak", SensorKind.Leak, warn = true),
        )
    }
}
