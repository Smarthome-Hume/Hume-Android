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
import com.smarthome.hume.core.model.FrigateRemoteConfig
import com.smarthome.hume.core.model.isLocalHaUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

val Context.humeDataStore by preferencesDataStore("hume_settings")

/**
 * Phien dang nhap HA: URL (DataStore plaintext) + token (EncryptedSharedPreferences).
 *
 * DUNG CHUNG file/key voi SettingsStore cu (app module):
 * "hume_settings"/ha_url, "hume_secrets"/ha_token — de UI cu va moi doc
 * cung mot nguon, khong lech du lieu trong luc migrate.
 */
class SessionStore(private val context: Context) {

    private object Keys {
        val HaUrl = stringPreferencesKey("ha_url") // legacy: 1 URL duy nhat (truoc dual-path)
        val HaLocalUrl = stringPreferencesKey("ha_local_url")
        val HaRemoteUrl = stringPreferencesKey("ha_remote_url")
        val FrigateRemoteUrl = stringPreferencesKey("frigate_remote_url")
        val HaTokenLegacy = stringPreferencesKey("ha_token")
        val HaAvatarUrl = stringPreferencesKey("ha_avatar_url")
    }

    companion object {
        const val DEFAULT_URL = "http://192.168.102.22:8123"
        const val DEFAULT_LOCAL_URL = "http://192.168.102.22:8123"
        const val DEFAULT_REMOTE_URL = "https://haiha93.xyz"
        const val DEFAULT_FRIGATE_REMOTE_URL = "https://frigate.haiha93.xyz"
    }

    @Suppress("DEPRECATION")
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

    private fun readCfAccess(): Pair<String, String> {
        val prefs = encryptedPrefs()
        return (prefs.getString("cf_access_id", null).orEmpty() to
            prefs.getString("cf_access_secret", null).orEmpty())
    }

    val session: Flow<AuthSession> =
        context.humeDataStore.data.map { prefs ->
            // Migrate 1 lan: ha_url cu (1 URL duy nhat) -> local hoac remote tuy dia chi.
            val legacy = prefs[Keys.HaUrl]?.trim().orEmpty()
            val migratedLocal = prefs[Keys.HaLocalUrl] ?: legacy.takeIf { it.isNotBlank() && isLocalHaUrl(it) }
            val migratedRemote = prefs[Keys.HaRemoteUrl] ?: legacy.takeIf { it.isNotBlank() && !isLocalHaUrl(it) }
            AuthSession(
                localUrl = migratedLocal ?: DEFAULT_LOCAL_URL,
                remoteUrl = migratedRemote ?: DEFAULT_REMOTE_URL,
                avatarUrl = prefs[Keys.HaAvatarUrl] ?: "",
            )
        }.combine(tokenFlow) { s, token -> s.copy(token = token) }

    suspend fun save(localUrl: String, remoteUrl: String, token: String, avatarUrl: String = "") {
        val cleanLocal = localUrl.trim().trimEnd('/')
        val cleanRemote = remoteUrl.trim().trimEnd('/')
        val cleanToken = token.trim()
        context.humeDataStore.edit { prefs ->
            if (cleanLocal.isNotBlank()) prefs[Keys.HaLocalUrl] = cleanLocal
            else prefs.remove(Keys.HaLocalUrl)
            if (cleanRemote.isNotBlank()) prefs[Keys.HaRemoteUrl] = cleanRemote
            else prefs.remove(Keys.HaRemoteUrl)
            prefs.remove(Keys.HaUrl) // legacy da migrate sang 2 key moi
            // Luon ghi/xoa avatar key de khong giu avatar cu cua user truoc
            if (avatarUrl.isNotBlank()) prefs[Keys.HaAvatarUrl] = avatarUrl
            else prefs.remove(Keys.HaAvatarUrl)
        }
        encryptedPrefs().edit().putString("ha_token", cleanToken).apply()
        tokenFlow.value = cleanToken
    }

    /** Tuong thich nguoc: luu 1 URL duy nhat nhu ban cu (mac dinh coi la local). */
    suspend fun save(url: String, token: String, avatarUrl: String = "") {
        save(localUrl = url, remoteUrl = "", token = token, avatarUrl = avatarUrl)
    }

    suspend fun clear() {
        encryptedPrefs().edit().remove("ha_token").apply()
        context.humeDataStore.edit { prefs ->
            prefs.remove(Keys.HaUrl)
            prefs.remove(Keys.HaLocalUrl)
            prefs.remove(Keys.HaRemoteUrl)
            prefs.remove(Keys.HaAvatarUrl)
        }
        tokenFlow.value = ""
    }

    /** Doc lai tu disk (dung sau khi noi khac ghi de). */
    fun refresh() {
        tokenFlow.value = readToken()
    }

    /**
     * Kich hoat phat lai [frigateRemote] sau moi lan luu. Can thiet vi
     * cfClientId/Secret nam trong encrypted prefs (ngoai DataStore): neu user
     * chi doi secret ma URL giu nguyen, DataStore co the khong emit.
     */
    private val frigateRefresh = MutableStateFlow(0)

    /** Cau hinh xem Frigate tu xa (doi theo realtime khi user sua o tab Toi). */
    val frigateRemote: Flow<FrigateRemoteConfig> =
        combine(context.humeDataStore.data, frigateRefresh) { prefs, _ ->
            val (id, secret) = readCfAccess()
            FrigateRemoteConfig(
                remoteUrl = prefs[Keys.FrigateRemoteUrl] ?: DEFAULT_FRIGATE_REMOTE_URL,
                cfClientId = id,
                cfClientSecret = secret,
            )
        }.distinctUntilChanged()

    suspend fun saveFrigateRemote(url: String, cfClientId: String, cfClientSecret: String) {
        context.humeDataStore.edit { prefs ->
            val clean = url.trim().trimEnd('/')
            if (clean.isNotBlank()) prefs[Keys.FrigateRemoteUrl] = clean
            else prefs.remove(Keys.FrigateRemoteUrl)
        }
        // O nhap trong = giu gia tri cu (dung nhu hint tren UI), tranh vo tinh
        // xoa secret khi user quay lai card va bam Luu ma khong nhap lai secret.
        val (oldId, oldSecret) = readCfAccess()
        encryptedPrefs().edit()
            .putString("cf_access_id", cfClientId.trim().ifBlank { oldId })
            .putString("cf_access_secret", cfClientSecret.trim().ifBlank { oldSecret })
            .apply()
        frigateRefresh.value++
    }
}
