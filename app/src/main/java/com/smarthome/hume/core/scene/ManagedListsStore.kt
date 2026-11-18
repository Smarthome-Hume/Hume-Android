package com.smarthome.hume.core.scene

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Port of Core/ManagedLists.swift.
// The header light pill counts entities from THIS list, not from the room list.

@Serializable
data class ManagedItem(
    val id: String,
    val name: String = "",
    val icon: String = "lightbulb",
    val hidden: Boolean = false,
)

@Serializable
internal data class ManagedListsSnapshot(
    val lights: List<ManagedItem> = emptyList(),
)

class ManagedListsStore private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("hume_managed_lists", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val snapshotSerializer = ManagedListsSnapshot.serializer()

    private val _lights = MutableStateFlow<List<ManagedItem>>(emptyList())
    val lights: StateFlow<List<ManagedItem>> = _lights.asStateFlow()

    init {
        val raw = prefs.getString(KEY, null)
        val snapshot = raw?.let { text ->
            runCatching { json.decodeFromString(snapshotSerializer, text) }.getOrNull()
        }
        if (snapshot == null) {
            _lights.value = defaultLights.map { ManagedItem(id = it) }
            persist()
        } else {
            _lights.value = snapshot.lights
        }
    }

    private fun persist() {
        val text = json.encodeToString(
            snapshotSerializer,
            ManagedListsSnapshot(_lights.value),
        )
        prefs.edit().putString(KEY, text).apply()
    }

    fun items(): List<ManagedItem> = _lights.value

    fun add(entityId: String, icon: String) {
        if (_lights.value.any { it.id == entityId }) return
        _lights.value = _lights.value + ManagedItem(id = entityId, icon = icon)
        persist()
    }

    fun remove(entityId: String) {
        _lights.value = _lights.value.filterNot { it.id == entityId }
        persist()
    }

    fun update(item: ManagedItem) {
        _lights.value = _lights.value.map { if (it.id == item.id) item else it }
        persist()
    }

    /** Every entity id the list needs realtime updates for. */
    fun watchedIds(): Set<String> = _lights.value.map { it.id }.toSet()

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
