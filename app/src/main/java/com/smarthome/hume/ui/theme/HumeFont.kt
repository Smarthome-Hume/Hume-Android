package com.smarthome.hume.ui.theme

import android.content.Context
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font as GoogleFontStyle
import com.smarthome.hume.R

/**
 * Swift dung Montserrat (Fonts/Montserrat-*.ttf) qua .appFont().
 *
 * Tren Android font duoc lay theo thu tu:
 *  1. res/font/montserrat_*.ttf neu co san trong project.
 *  2. Google Fonts downloadable provider (Play Services) - khong can file .ttf nao.
 *  3. SansSerif he thong neu ca hai deu that bai.
 */
private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val montserratGoogleFont = GoogleFont("Montserrat")

private val humeWeights = listOf(
    "montserrat_light" to FontWeight.Light,
    "montserrat_regular" to FontWeight.Normal,
    "montserrat_medium" to FontWeight.Medium,
    "montserrat_semibold" to FontWeight.SemiBold,
    "montserrat_bold" to FontWeight.Bold,
)

internal fun humeFontFamily(context: Context): FontFamily {
    val bundled = humeWeights.mapNotNull { (name, weight) ->
        val id = context.resources.getIdentifier(name, "font", context.packageName)
        if (id != 0) Font(id, weight) else null
    }
    if (bundled.isNotEmpty()) return FontFamily(bundled)

    return runCatching {
        FontFamily(
            humeWeights.map { (_, weight) ->
                GoogleFontStyle(googleFont = montserratGoogleFont, fontProvider = googleFontProvider, weight = weight)
            }
        )
    }.getOrElse { FontFamily.SansSerif }
}
