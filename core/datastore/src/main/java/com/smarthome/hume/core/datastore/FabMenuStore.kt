package com.smarthome.hume.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.fabMenuDataStore by preferencesDataStore(name = "fab_menu")

/** Cau hinh cho 1 chuc nang trong FAB menu: entity + trang thai muc tieu. */
data class FabActionConfig(
    val entityId: String = "",
    val targetState: String = "on",
)

/** 4 chuc nang cua FAB menu. */
enum class FabFunction(val key: String, val label: String) {
    Lights("lights", "Bật đèn"),
    Ac26("ac26", "Điều hoà 26°"),
    ArmAway("arm_away", "Bật an ninh"),
    Eco("eco", "Tiết kiệm điện"),
}

class FabMenuStore(
    private val context: Context,
) {
    private fun entityKey(f: FabFunction) = stringPreferencesKey("fab_${f.key}_entity")
    private fun stateKey(f: FabFunction) = stringPreferencesKey("fab_${f.key}_state")

    fun configFlow(f: FabFunction): Flow<FabActionConfig> =
        context.fabMenuDataStore.data.map { prefs ->
            FabActionConfig(
                entityId = prefs[entityKey(f)] ?: "",
                targetState = prefs[stateKey(f)] ?: "on",
            )
        }

    suspend fun saveConfig(f: FabFunction, config: FabActionConfig) {
        context.fabMenuDataStore.edit { prefs ->
            prefs[entityKey(f)] = config.entityId
            prefs[stateKey(f)] = config.targetState
        }
    }
}
