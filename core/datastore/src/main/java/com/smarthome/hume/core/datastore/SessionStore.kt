package com.smarthome.hume.core.datastore

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.smarthome.hume.core.model.AuthSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.humeDataStore by preferencesDataStore("hume_settings")

/**
 * Phien dang nhap HA: URL (DataStore plaintext) + token (EncryptedSharedPreferences).
 *
 * DUNG CHUNG file/key voi SettingsStore cu (app module):
 * "hume_settings"/ha_url, "hume_secrets"/ha_token — de UI cu va moi doc
 * cung mot nguon, khong lech du lieu trong luc migrate.
 */
class SessionStore(private val context: Context) {

    private object Keys {
        val HaUrl = stringPreferencesKey("ha_url")
        val HaTokenLegacy = stringPreferencesKey("ha_token")
    }

    companion object {
        const val DEFAULT_URL = "http://192.168.102.22:8123"
    }

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
        Log.w("SessionStore", "EncryptedSharedPreferences unavailable, falling back", e)
        context.getSharedPreferences("hume_secrets_fallback", Context.MODE_PRIVATE)
    }

    private fun readToken(): String {
        val prefs = encryptedPrefs()
        prefs.getString("ha_token", null)?.let { return it }
        // Migration 1 lan: token cu dang plaintext trong DataStore.
        val legacy = runBlocking(Dispatchers.IO) {
            context.humeDataStore.data.map { it[Keys.HaTokenLegacy] ?: "" }.first()
        }.trim()
        if (legacy.isNotBlank()) {
            prefs.edit().putString("ha_token", legacy).apply()
            runBlocking(Dispatchers.IO) {
                context.humeDataStore.edit { it.remove(Keys.HaTokenLegacy) }
            }
            Log.i("SessionStore", "Migrated HA token from plaintext DataStore to encrypted storage")
        }
        return legacy
    }

    private val tokenFlow = MutableStateFlow(readToken())

    val session: Flow<AuthSession> =
        context.humeDataStore.data.map { it[Keys.HaUrl] ?: DEFAULT_URL }
            .combine(tokenFlow) { url, token -> AuthSession(url, token) }

    suspend fun save(url: String, token: String) {
        val cleanUrl = url.trim().trimEnd('/')
        val cleanToken = token.trim()
        context.humeDataStore.edit { prefs -> prefs[Keys.HaUrl] = cleanUrl }
        encryptedPrefs().edit().putString("ha_token", cleanToken).apply()
        tokenFlow.value = cleanToken
    }

    suspend fun clear() {
        encryptedPrefs().edit().remove("ha_token").apply()
        tokenFlow.value = ""
    }

    /** Doc lai tu disk (dung sau khi noi khac ghi de). */
    fun refresh() {
        tokenFlow.value = readToken()
    }
}
