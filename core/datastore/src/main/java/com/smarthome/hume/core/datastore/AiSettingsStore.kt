package com.smarthome.hume.core.datastore

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Nha cung cap AI cho goi y phan tich nha.
 * OpenAI / Google (Gemini) / Custom dung dinh dang OpenAI-compatible
 * (POST {baseUrl}/chat/completions, Bearer auth).
 * Anthropic dung API native (POST {baseUrl}/messages, x-api-key).
 */
enum class AiProvider(val label: String) {
    OpenAI("OpenAI"),
    Anthropic("Anthropic"),
    Google("Google (Gemini)"),
    DeepSeek("DeepSeek"),
    Custom("Tùy chỉnh (OpenAI-compatible)"),
}

/** Cau hinh AI — phan khong nhay cam luu DataStore, API key luu kho ma hoa. */
data class AiSettings(
    val enabled: Boolean = false,
    val provider: AiProvider = AiProvider.OpenAI,
    /** Chi dung cho Custom; cac provider khac co URL mac dinh. */
    val customBaseUrl: String = "",
    val model: String = "",
    val hasApiKey: Boolean = false,
) {
    /** Base URL thuc te tuy theo provider. */
    val baseUrl: String get() = when (provider) {
        AiProvider.OpenAI -> "https://api.openai.com/v1"
        AiProvider.Anthropic -> "https://api.anthropic.com/v1"
        AiProvider.Google -> "https://generativelanguage.googleapis.com/v1beta/openai"
        AiProvider.DeepSeek -> "https://api.deepseek.com/v1"
        AiProvider.Custom -> customBaseUrl.trim().trimEnd('/')
    }

    /** Model mac dinh neu user chua nhap. */
    val effectiveModel: String get() {
        val trimmed = model.trim()
        if (trimmed.isNotBlank()) return trimmed
        return when (provider) {
            AiProvider.OpenAI -> "gpt-4o-mini"
            AiProvider.Anthropic -> "claude-3-5-haiku-latest"
            AiProvider.Google -> "gemini-2.0-flash"
            AiProvider.DeepSeek -> "deepseek-chat"
            AiProvider.Custom -> ""
        }
    }

    /** Du thong tin de goi API? */
    val isConfigured: Boolean get() =
        enabled && hasApiKey && baseUrl.isNotBlank() && effectiveModel.isNotBlank()
}

/**
 * Luu cau hinh AI: provider/baseUrl/model trong DataStore (hume_settings),
 * API key trong EncryptedSharedPreferences (Android Keystore, AES256-GCM) —
 * cung pattern voi HA token trong SettingsStore. Khong bao gio log key.
 */
class AiSettingsStore(private val context: Context) {

    private object Keys {
        val Enabled = stringPreferencesKey("ai_enabled")
        val Provider = stringPreferencesKey("ai_provider")
        val CustomBaseUrl = stringPreferencesKey("ai_custom_base_url")
        val Model = stringPreferencesKey("ai_model")
    }

    private object SecretKeys {
        const val ApiKey = "ai_api_key"
    }

    private val encryptedPrefsLazy: SharedPreferences by lazy { createEncryptedPrefs() }

    private fun encryptedPrefs(): SharedPreferences = encryptedPrefsLazy

    @Suppress("DEPRECATION")
    private fun createEncryptedPrefs(): SharedPreferences = try {
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
        Log.w("AiSettingsStore", "EncryptedSharedPreferences unavailable, falling back", e)
        context.getSharedPreferences("hume_secrets_fallback", Context.MODE_PRIVATE)
    }

    /**
     * Co API key hay khong (khong doc gia tri ra ngoai).
     * Nap 1 lan tren IO, cap nhat khi save/clear.
     */
    private val _hasApiKey = kotlinx.coroutines.flow.MutableStateFlow<Boolean?>(null)
    val hasApiKey: Flow<Boolean> = _hasApiKey
        .filterNotNull()
        .onStart {
            if (_hasApiKey.value == null) {
                val has = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    encryptedPrefs().contains(SecretKeys.ApiKey)
                }
                _hasApiKey.value = has
            }
        }

    val settings: Flow<AiSettings> =
        context.humeDataStore.data.map { prefs ->
            val provider = runCatching { AiProvider.valueOf(prefs[Keys.Provider] ?: "") }
                .getOrDefault(AiProvider.OpenAI)
            AiSettings(
                enabled = prefs[Keys.Enabled] == "1",
                provider = provider,
                customBaseUrl = prefs[Keys.CustomBaseUrl] ?: "",
                model = prefs[Keys.Model] ?: "",
                hasApiKey = _hasApiKey.value == true,
            )
        }.combine(hasApiKey) { s, hasKey -> s.copy(hasApiKey = hasKey) }

    /** Doc API key (chi dung trong AI call, khong log, khong luu bien lau). */
    fun readApiKey(): String =
        encryptedPrefs().getString(SecretKeys.ApiKey, "").orEmpty()

    suspend fun save(
        enabled: Boolean,
        provider: AiProvider,
        customBaseUrl: String,
        model: String,
        apiKey: String,
    ) {
        context.humeDataStore.edit { prefs ->
            prefs[Keys.Enabled] = if (enabled) "1" else "0"
            prefs[Keys.Provider] = provider.name
            prefs[Keys.CustomBaseUrl] = customBaseUrl.trim()
            prefs[Keys.Model] = model.trim()
        }
        val key = apiKey.trim()
        if (key.isNotEmpty()) {
            encryptedPrefs().edit().putString(SecretKeys.ApiKey, key).apply()
            _hasApiKey.value = true
        }
    }

    /** Xoa API key (gi lai cau hinh khac). */
    fun clearApiKey() {
        encryptedPrefs().edit().remove(SecretKeys.ApiKey).apply()
        _hasApiKey.value = false
    }
}
