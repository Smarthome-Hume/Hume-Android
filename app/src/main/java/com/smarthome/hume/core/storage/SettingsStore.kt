package com.smarthome.hume.core.storage

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.humeDataStore by preferencesDataStore("hume_settings")

data class HumeSettings(val haUrl: String = "http://192.168.102.22:8123", val haToken: String = "") {
    val hasToken: Boolean get() = haToken.isNotBlank() && haToken != "ĐIỀN_TOKEN_VÀO_ĐÂY"
}

/**
 * URL luu plaintext trong DataStore (khong nhay cam).
 * HA token luu trong EncryptedSharedPreferences (Android Keystore, AES256-GCM).
 * Migration mot lan: token cu dang plaintext trong DataStore se duoc chuyen
 * sang kho ma hoa roi xoa ban plaintext.
 */
class SettingsStore(private val context: Context) {
    private object Keys {
        val HaUrl = stringPreferencesKey("ha_url")
        /** Chi doc de migration, khong bao gio ghi nua. */
        val HaTokenLegacy = stringPreferencesKey("ha_token")
    }

    private val tokenFlow = MutableStateFlow(migrateTokenIfNeeded())

    private fun encryptedPrefs(): SharedPreferences = try {
        val masterKey = MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "hume_secrets",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (e: Exception) {
        // Keystore khong kha dung (rat hiem): fallback ve SharedPreferences
        // thuong de app van chay duoc, ghi log canh bao.
        Log.w("SettingsStore", "EncryptedSharedPreferences unavailable, falling back", e)
        context.getSharedPreferences("hume_secrets_fallback", Context.MODE_PRIVATE)
    }

    private fun migrateTokenIfNeeded(): String {
        val prefs = encryptedPrefs()
        prefs.getString("ha_token", null)?.let { return it }
        // Token cu dang plaintext trong DataStore -> chuyen sang kho ma hoa.
        val legacy = runBlocking(Dispatchers.IO) {
            context.humeDataStore.data.map { it[Keys.HaTokenLegacy] ?: "" }.first()
        }.trim()
        if (legacy.isNotBlank()) {
            prefs.edit().putString("ha_token", legacy).apply()
            runBlocking(Dispatchers.IO) {
                context.humeDataStore.edit { it.remove(Keys.HaTokenLegacy) }
            }
            Log.i("SettingsStore", "Migrated HA token from plaintext DataStore to encrypted storage")
        }
        return legacy
    }

    val settings: Flow<HumeSettings> =
        context.humeDataStore.data.map { it[Keys.HaUrl] ?: "http://192.168.102.22:8123" }
            .combine(tokenFlow) { url, token -> HumeSettings(url, token) }

    suspend fun saveHomeAssistant(url: String, token: String) {
        val cleanUrl = url.trim().trimEnd('/')
        val cleanToken = token.trim()
        context.humeDataStore.edit { prefs -> prefs[Keys.HaUrl] = cleanUrl }
        encryptedPrefs().edit().putString("ha_token", cleanToken).apply()
        tokenFlow.value = cleanToken
    }

    suspend fun logout() {
        encryptedPrefs().edit().remove("ha_token").apply()
        tokenFlow.value = ""
    }
}
