package com.smarthome.hume.ui.theme

import android.content.Context
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font as GoogleFontStyle
import com.smarthome.hume.R

/**
 * Swift dung Montserrat (Fonts/Montserrat-*.ttf) qua .appFont().
 *
 * Tren Android font duoc tai tu Google Fonts provider cua Play Services
 * (Downloadable Fonts) nen khong can chep file .ttf nao vao project.
 * Neu thiet bi khong co Play Services thi tu dong lui ve SansSerif.
 *
 * Chung chi provider nam o res/values/font_certs.xml.
 */
private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val montserrat = GoogleFont("Montserrat")

private val humeWeights = listOf(
    FontWeight.Light,
    FontWeight.Normal,
    FontWeight.Medium,
    FontWeight.SemiBold,
    FontWeight.Bold,
)

@Suppress("UNUSED_PARAMETER")
internal fun humeFontFamily(context: Context): FontFamily = runCatching {
    FontFamily(
        humeWeights.map { weight ->
            GoogleFontStyle(
                googleFont = montserrat,
                fontProvider = googleFontProvider,
                weight = weight,
            )
        }
    )
}.getOrElse { FontFamily.SansSerif }
