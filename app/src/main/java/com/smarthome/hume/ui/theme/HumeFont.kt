package com.smarthome.hume.ui.theme

import android.content.Context
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.smarthome.hume.R

/**
 * Swift dung Montserrat (Fonts/Montserrat-*.ttf) qua .appFont().
 *
 * TRUOC DAY font duoc yeu cau bang GoogleFont API ngay trong Compose. Cach do
 * chi tai font o thoi diem ve chu, va khi provider tra ve cham hoac loi thi
 * Compose IM LANG lui ve Roboto - dung hien tuong "mat Montserrat".
 *
 * NAY font khai bao bang font resource XML (res/font/montserrat_*.xml). Android
 * coi do la font cua ung dung: he thong tu tai qua Play Services, co cache,
 * va res/values/preloaded_fonts.xml + meta-data trong manifest bao Play
 * Services tai san ngay luc cai app. Neu may that su khong co Play Services
 * thi moi lui ve SansSerif.
 */
@Suppress("UNUSED_PARAMETER")
internal fun humeFontFamily(context: Context): FontFamily = runCatching {
    FontFamily(
        Font(R.font.montserrat_light, FontWeight.Light),
        Font(R.font.montserrat_regular, FontWeight.Normal),
        Font(R.font.montserrat_medium, FontWeight.Medium),
        Font(R.font.montserrat_semibold, FontWeight.SemiBold),
        Font(R.font.montserrat_bold, FontWeight.Bold),
    )
}.getOrElse { FontFamily.SansSerif }
