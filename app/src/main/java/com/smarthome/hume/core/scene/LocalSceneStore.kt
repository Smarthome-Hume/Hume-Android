package com.smarthome.hume.core.scene

import android.content.Context
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.HumeConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

// Port of Core/LocalSceneStore.swift.
// Scenes are NOT Home Assistant scene.* entities: they are stored locally,
// only one can be active at a time, and each one drives a list of service
// calls plus an optional Alarmo state.

@Serializable
data class LocalSceneAction(
    val id: String = UUID.randomUUID().toString(),
    val entityId: String,
    val entityName: String = "",
    val service: String,
    val params: Map<String, String> = emptyMap(),
)

@Serializable
data class LocalScene(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val icon: String,
    val colorHex: String = "#f9784c",
    val isActive: Boolean = false,
    val isHidden: Boolean = false,
    val isPinned: Boolean = true,
    val actions: List<LocalSceneAction> = emptyList(),
    val sortIndex: Int = 0,
    val alarmoState: String? = null,
)

class LocalSceneStore private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("hume_local_scenes", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _scenes = MutableStateFlow(load())
    val scenes: StateFlow<List<LocalScene>> = _scenes.asStateFlow()

    /** Scenes shown on the Home dashboard, in sort order. */
    fun pinnedVisible(list: List<LocalScene> = _scenes.value): List<LocalScene> =
        list.filter { it.isPinned && !it.isHidden }.sortedBy { it.sortIndex }

    fun activeScene(): LocalScene? = _scenes.value.firstOrNull { it.isActive }

    /** Guards against the WebSocket echo re-triggering a scene we just ran. */
    private var lastLocalActivationAt: Long = 0L
    val recentlyActivatedLocally: Boolean
        get() = System.currentTimeMillis() - lastLocalActivationAt < 6_000L

    // ---- persistence -------------------------------------------------

    private fun load(): List<LocalScene> {
        val raw = prefs.getString(KEY, null) ?: return defaultScenes.also { persist(it) }
        return runCatching { json.decodeFromString<List<LocalScene>>(raw) }
            .getOrElse { defaultScenes }
    }

    private fun persist(list: List<LocalScene>) {
        prefs.edit().putString(KEY, json.encodeToString(list)).apply()
    }

    private fun commit(list: List<LocalScene>) {
        _scenes.value = list
        persist(list)
    }

    // ---- CRUD --------------------------------------------------------

    fun add(scene: LocalScene) =
        commit(_scenes.value + scene.copy(sortIndex = _scenes.value.size))

    fun update(scene: LocalScene) =
        commit(_scenes.value.map { if (it.id == scene.id) scene else it })

    fun delete(id: String) = commit(_scenes.value.filterNot { it.id == id })

    fun setPinned(id: String, pinned: Boolean) =
        commit(_scenes.value.map { if (it.id == id) it.copy(isPinned = pinned) else it })

    fun setHidden(id: String, hidden: Boolean) =
        commit(_scenes.value.map { if (it.id == id) it.copy(isHidden = hidden) else it })

    // ---- activation --------------------------------------------------

    /** Toggle a scene. Turning one on turns every other active scene off first. */
    fun activate(id: String, ha: HomeAssistantRepository) {
        val list = _scenes.value.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index < 0) return

        if (list[index].isActive) {
            list[index].actions.forEach { callOff(it, ha) }
            list[index] = list[index].copy(isActive = false)
        } else {
            for (other in list.indices) {
                if (other != index && list[other].isActive) {
                    list[other].actions.forEach { callOff(it, ha) }
                    list[other] = list[other].copy(isActive = false)
                }
            }
            list[index].actions.forEach { callOn(it, ha) }
            list[index] = list[index].copy(isActive = true)
            list[index].alarmoState?.let { callAlarmo(it, ha) }
            lastLocalActivationAt = System.currentTimeMillis()
        }
        commit(list)
    }

    /** Alarm disarmed on Home Assistant -> nothing is active any more. */
    fun deactivateAll(ha: HomeAssistantRepository) {
        if (_scenes.value.none { it.isActive }) return
        val list = _scenes.value.map { scene ->
            if (!scene.isActive) scene else {
                scene.actions.forEach { callOff(it, ha) }
                scene.copy(isActive = false)
            }
        }
        commit(list)
    }

    /**
     * Alarm state changed on Home Assistant (dashboard, Alarmo, another app)
     * -> mirror it locally. Never calls the alarm service again, that would loop.
     */
    fun syncFromAlarmState(alarmState: String, ha: HomeAssistantRepository) {
        if (alarmState == "disarmed") {
            deactivateAll(ha)
            return
        }
        val list = _scenes.value.toMutableList()
        val index = list.indexOfFirst { it.alarmoState == alarmState }
        if (index < 0) return
        if (list[index].isActive) return
        for (other in list.indices) {
            if (other != index && list[other].isActive) {
                list[other].actions.forEach { callOff(it, ha) }
                list[other] = list[other].copy(isActive = false)
            }
        }
        list[index].actions.forEach { callOn(it, ha) }
        list[index] = list[index].copy(isActive = true)
        commit(list)
    }

    // ---- service calls -----------------------------------------------

    private fun callOn(action: LocalSceneAction, ha: HomeAssistantRepository) {
        val domain = action.entityId.substringBefore('.', "homeassistant")
        val data = buildString {
            append("{\"entity_id\":\"").append(action.entityId).append("\"")
            action.params.forEach { (key, value) ->
                append(",\"").append(key).append("\":\"").append(value).append("\"")
            }
            append("}")
        }
        ha.callService(domain, action.service, data, action.entityId)
    }

    private fun callOff(action: LocalSceneAction, ha: HomeAssistantRepository) {
        val domain = action.entityId.substringBefore('.', "homeassistant")
        if (domain == "alarm_control_panel") return
        ha.callService(
            domain,
            "turn_off",
            "{\"entity_id\":\"" + action.entityId + "\"}",
            action.entityId,
        )
    }

    private fun callAlarmo(state: String, ha: HomeAssistantRepository) {
        val service = "alarm_" + state.replace("armed_", "arm_")
        val entity = HumeConfig.ALARM_PRIMARY
        ha.callService(
            "alarm_control_panel",
            service,
            "{\"entity_id\":\"" + entity + "\",\"code\":\"" + HumeConfig.ALARM_CODE + "\"}",
            entity,
        )
    }

    companion object {
        private const val KEY = "scenes_v1"

        @Volatile
        private var instance: LocalSceneStore? = null

        fun get(context: Context): LocalSceneStore =
            instance ?: synchronized(this) {
                instance ?: LocalSceneStore(context.applicationContext).also { instance = it }
            }

        /** Same four seeded scenes as the iOS build, same entities, same alarm states. */
        val defaultScenes: List<LocalScene> = listOf(
            LocalScene(
                name = "Ch\u00e0o bu\u1ed5i s\u00e1ng",
                icon = "sunrise",
                colorHex = "#f2d26f",
                actions = listOf(
                    LocalSceneAction(entityId = "light.cong_tac_4_nut_l1", entityName = "\u0110\u00e8n ph\u00f2ng kh\u00e1ch", service = "turn_on"),
                    LocalSceneAction(entityId = "light.cong_tac_4_nut_l3", entityName = "\u0110\u00e8n ph\u00f2ng kh\u00e1ch 2", service = "turn_on"),
                    LocalSceneAction(entityId = "climate.air_condition", entityName = "\u0110i\u1ec1u h\u00f2a ph\u00f2ng ng\u1ee7", service = "turn_off"),
                ),
                sortIndex = 0,
                alarmoState = "armed_custom_bypass",
            ),
            LocalScene(
                name = "Ch\u00fac ng\u1ee7 ngon",
                icon = "moon",
                colorHex = "#ad99e6",
                actions = listOf(
                    LocalSceneAction(entityId = "light.cong_tac_4_nut_l1", entityName = "\u0110\u00e8n ph\u00f2ng kh\u00e1ch", service = "turn_off"),
                    LocalSceneAction(entityId = "light.cong_tac_4_nut_l3", entityName = "\u0110\u00e8n ph\u00f2ng kh\u00e1ch 2", service = "turn_off"),
                    LocalSceneAction(entityId = "light.bedroom", entityName = "\u0110\u00e8n ph\u00f2ng ng\u1ee7", service = "turn_off"),
                ),
                sortIndex = 1,
                alarmoState = "armed_night",
            ),
            LocalScene(
                name = "Ra kh\u1ecfi nh\u00e0",
                icon = "walk",
                colorHex = "#73b9f2",
                actions = listOf(
                    LocalSceneAction(entityId = "light.living_room", entityName = "\u0110\u00e8n ph\u00f2ng kh\u00e1ch", service = "turn_off"),
                    LocalSceneAction(entityId = "light.bedroom", entityName = "\u0110\u00e8n ph\u00f2ng ng\u1ee7", service = "turn_off"),
                ),
                sortIndex = 2,
                alarmoState = "armed_away",
            ),
            LocalScene(
                name = "V\u1ec1 nh\u00e0",
                icon = "house",
                colorHex = "#66d19e",
                actions = listOf(
                    LocalSceneAction(entityId = "light.cong_tac_4_nut_l1", entityName = "\u0110\u00e8n ph\u00f2ng kh\u00e1ch", service = "turn_on"),
                ),
                sortIndex = 3,
                alarmoState = "armed_home",
            ),
        )
    }
}
