package com.smarthome.hume.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Tuy chon giao dien M3E: seed mau + che do toi (luu trong hume_settings). */
data class ThemeSettings(
    /** Ten M3ESeed (Cam, Green, Blue, Violet, Red, Pink, Teal, Amber). */
    val seedName: String = "Cam",
    val darkMode: Boolean? = null, // null = theo he thong
)

class ThemeStore(private val context: Context) {

    private object Keys {
        val Seed = stringPreferencesKey("theme_seed")
        val DarkMode = booleanPreferencesKey("theme_dark_mode")
    }

    val settings: Flow<ThemeSettings> =
        context.humeDataStore.data.map { prefs ->
            ThemeSettings(
                seedName = prefs[Keys.Seed] ?: "Cam",
                darkMode = if (prefs.contains(Keys.DarkMode)) prefs[Keys.DarkMode] else null,
            )
        }

    suspend fun setSeed(seedName: String) {
        context.humeDataStore.edit { it[Keys.Seed] = seedName }
    }

    suspend fun setDarkMode(dark: Boolean?) {
        context.humeDataStore.edit {
            if (dark == null) it.remove(Keys.DarkMode) else it[Keys.DarkMode] = dark
        }
    }
}
