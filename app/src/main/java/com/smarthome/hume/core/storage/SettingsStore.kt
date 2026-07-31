package com.smarthome.hume.core.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.humeDataStore by preferencesDataStore("hume_settings")

data class HumeSettings(val haUrl: String = "http://192.168.102.22:8123", val haToken: String = "") {
    val hasToken: Boolean get() = haToken.isNotBlank() && haToken != "ĐIỀN_TOKEN_VÀO_ĐÂY"
}

class SettingsStore(private val context: Context) {
    private object Keys {
        val HaUrl = stringPreferencesKey("ha_url")
        val HaToken = stringPreferencesKey("ha_token")
    }
    val settings: Flow<HumeSettings> = context.humeDataStore.data.map { prefs ->
        HumeSettings(prefs[Keys.HaUrl] ?: "http://192.168.102.22:8123", prefs[Keys.HaToken] ?: "")
    }
    suspend fun saveHomeAssistant(url: String, token: String) {
        context.humeDataStore.edit { prefs ->
            prefs[Keys.HaUrl] = url.trim().trimEnd('/')
            prefs[Keys.HaToken] = token.trim()
        }
    }
    suspend fun logout() { context.humeDataStore.edit { it[Keys.HaToken] = "" } }
}
