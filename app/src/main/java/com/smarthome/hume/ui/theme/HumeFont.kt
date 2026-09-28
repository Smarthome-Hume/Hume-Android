package com.smarthome.hume.ui.theme

import android.content.Context
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.smarthome.hume.R

/**
 * Swift dung Montserrat (Fonts/Montserrat-*.ttf) qua .appFont().
 *
 * Font lay bang GoogleFont API (Downloadable Fonts cua Play Services). Cach nay
 * nap ASYNC: lan ve dau tien co the la font he thong, xong font that se thay
 * vao. Neu provider loi thi Compose lang le dung font he thong - KHONG bao gio
 * lam vang app.
 *
 * DUNG doi sang Font(R.font.montserrat_*) nua: font resource nap kieu Blocking,
 * khi provider chua co font no NEM LOI ngay luc ve chu va app crash. Cac file
 * res/font/montserrat_*.xml + res/values/preloaded_fonts.xml van duoc giu lai,
 * vi chung bao Play Services tai san Montserrat ngay luc cai app, nho vay
 * GoogleFont API o day lay duoc font tu cache gan nhu tuc thi.
 */
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val montserrat = GoogleFont("Montserrat")

@Suppress("UNUSED_PARAMETER")
internal fun humeFontFamily(context: Context): FontFamily = runCatching {
    FontFamily(
        Font(googleFont = montserrat, fontProvider = provider, weight = FontWeight.Light),
        Font(googleFont = montserrat, fontProvider = provider, weight = FontWeight.Normal),
        Font(googleFont = montserrat, fontProvider = provider, weight = FontWeight.Medium),
        Font(googleFont = montserrat, fontProvider = provider, weight = FontWeight.SemiBold),
        Font(googleFont = montserrat, fontProvider = provider, weight = FontWeight.Bold),
    )
}.getOrElse { FontFamily.SansSerif }
