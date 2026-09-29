package com.smarthome.hume.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Tuy chon giao dien M3E: seed mau + che do toi (luu trong hume_settings). */
data class ThemeSettings(
    /** Ten M3ESeed (Cam, Green, Blue, Violet, Red, Pink, Teal, Amber). */
    val seedName: String = "Cam",
    val darkMode: Boolean? = null, // null = theo he thong
    /** Mau tuy chinh (ARGB Long) — null = dung seed co san. */
    val customColor: Long? = null,
    /** Ten font Google Fonts (mac dinh Montserrat) — tai qua Play Services. */
    val fontFamily: String = "Montserrat",
)

class ThemeStore(private val context: Context) {

    private object Keys {
        val Seed = stringPreferencesKey("theme_seed")
        val CustomColor = longPreferencesKey("theme_custom_color")
        val DarkMode = booleanPreferencesKey("theme_dark_mode")
        val FontFamily = stringPreferencesKey("theme_font_family")
    }

    val settings: Flow<ThemeSettings> =
        context.humeDataStore.data.map { prefs ->
            ThemeSettings(
                seedName = prefs[Keys.Seed] ?: "Cam",
                darkMode = if (prefs.contains(Keys.DarkMode)) prefs[Keys.DarkMode] else null,
                customColor = prefs[Keys.CustomColor],
                fontFamily = prefs[Keys.FontFamily] ?: "Montserrat",
            )
        }

    suspend fun setSeed(seedName: String) {
        context.humeDataStore.edit { it[Keys.Seed] = seedName }
    }

    suspend fun setCustomColor(argb: Long?) {
        context.humeDataStore.edit {
            if (argb == null) it.remove(Keys.CustomColor) else it[Keys.CustomColor] = argb
        }
    }

    suspend fun setDarkMode(dark: Boolean?) {
        context.humeDataStore.edit {
            if (dark == null) it.remove(Keys.DarkMode) else it[Keys.DarkMode] = dark
        }
    }

    suspend fun setFontFamily(family: String) {
        context.humeDataStore.edit { it[Keys.FontFamily] = family }
    }
}
