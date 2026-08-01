package com.smarthome.hume.core.scene

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Port of Core/ManagedLists.swift.
// The header light pill counts entities from THIS list, not from the room list,
// and the bell counts only the sensors the user added to the notification list
// (empty on a fresh install, exactly like iOS).

@Serializable
data class ManagedItem(
    val id: String,
    val name: String = "",
    val icon: String = "lightbulb",
    val hidden: Boolean = false,
)

enum class ManagedKind { LIGHTS, NOTIF }

class ManagedListsStore private constructor(context: Context) {

    @Serializable
    private data class Snapshot(
        val lights: List<ManagedItem> = emptyList(),
        val notif: List<ManagedItem> = emptyList(),
    )

    private val prefs = context.getSharedPreferences("hume_managed_lists", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _lights = MutableStateFlow<List<ManagedItem>>(emptyList())
    private val _notif = MutableStateFlow<List<ManagedItem>>(emptyList())
    val lights: StateFlow<List<ManagedItem>> = _lights.asStateFlow()
    val notif: StateFlow<List<ManagedItem>> = _notif.asStateFlow()

    init {
        val raw = prefs.getString(KEY, null)
        val snapshot = raw?.let { text ->
            runCatching { json.decodeFromString<Snapshot>(text) }.getOrNull()
        }
        if (snapshot == null) {
            _lights.value = defaultLights.map { ManagedItem(id = it) }
            _notif.value = emptyList()
            persist()
        } else {
            _lights.value = snapshot.lights
            _notif.value = snapshot.notif
        }
    }

    private fun persist() {
        val text = json.encodeToString(Snapshot(_lights.value, _notif.value))
        prefs.edit().putString(KEY, text).apply()
    }

    fun items(kind: ManagedKind): List<ManagedItem> =
        if (kind == ManagedKind.LIGHTS) _lights.value else _notif.value

    fun add(entityId: String, kind: ManagedKind, icon: String) {
        val target = if (kind == ManagedKind.LIGHTS) _lights else _notif
        if (target.value.any { it.id == entityId }) return
        target.value = target.value + ManagedItem(id = entityId, icon = icon)
        persist()
    }

    fun remove(entityId: String, kind: ManagedKind) {
        val target = if (kind == ManagedKind.LIGHTS) _lights else _notif
        target.value = target.value.filterNot { it.id == entityId }
        persist()
    }

    fun update(item: ManagedItem, kind: ManagedKind) {
        val target = if (kind == ManagedKind.LIGHTS) _lights else _notif
        target.value = target.value.map { if (it.id == item.id) item else it }
        persist()
    }

    /** Every entity id the two lists need realtime updates for. */
    fun watchedIds(): Set<String> =
        (_lights.value.map { it.id } + _notif.value.map { it.id }).toSet()

    companion object {
        private const val KEY = "managed_lists_v1"

        @Volatile
        private var instance: ManagedListsStore? = null

        fun get(context: Context): ManagedListsStore =
            instance ?: synchronized(this) {
                instance ?: ManagedListsStore(context.applicationContext).also { instance = it }
            }

        /** Seeded light list, identical to ManagedLists.swift. */
        val defaultLights: List<String> = listOf(
            "light.cong_tac_4_nut_l1",
            "light.cong_tac_4_nut_l3",
            "light.cong_tac_4_nut_l4",
            "light.cong_tac_4_nut_l2",
            "light.cong_tac_phong_an_l2",
            "light.cong_tac_phong_an_l3",
            "light.cong_tac_wc_t1",
            "light.cong_tac_phong_ngu_l1",
            "light.cong_tac_phong_ngu_l2",
            "light.smartlight",
            "light.table_led",
            "light.cong_tac_phong_ngu_nho_right",
            "light.cong_tac_phong_ngu_nho_left",
            "light.cong_tac_wc_t3_left",
            "light.cong_tac_hanh_lang_t3_l3",
            "light.cong_tac_phong_du_tru",
            "light.den_phong_tho_l2",
            "light.den_phong_tho_l1",
            "light.cong_tac_hanh_lang_t1_l2",
            "light.cong_tac_hanh_lang_t1_l1",
            "light.cong_tac_hanh_lang_t2_l2",
            "light.cong_tac_hanh_lang_t3_l2",
            "light.cong_tac_hanh_lang_t3_l1",
            "light.cong_tac_nha_ve_sinh_t2_l1",
        )
    }
}
