package com.smarthome.hume.ui.theme

import android.content.Context
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.res.ResourcesCompat
import com.smarthome.hume.R

/**
 * Swift dung Montserrat (Fonts/Montserrat-*.ttf) qua .appFont().
 *
 * Font khai bao bang font resource XML (res/font/montserrat_*.xml) + preloaded
 * fonts, nen he thong tu tai qua Play Services va co cache.
 *
 * TAI SAO TRUOC DAY VANG APP: font resource duoc nap kieu Blocking. Khi provider
 * chua co font (may thieu Play Services, chua tai xong, hoac dang offline) thi
 * Compose NEM LOI NGAY LUC VE CHU - runCatching o day khong the bat vi loi xay
 * ra sau, trong lan ve dau tien. Nay:
 *   1. thu nap thu font regular bang ResourcesCompat truoc, that bai thi dung
 *      luon SansSerif;
 *   2. cac Font deu dat loadingStrategy = OptionalLocal, nghia la neu khong nap
 *      duoc thi Compose lang le dung font he thong thay vi nem loi.
 */
internal fun humeFontFamily(context: Context): FontFamily {
    val available = runCatching {
        ResourcesCompat.getFont(context, R.font.montserrat_regular) != null
    }.getOrDefault(false)
    if (!available) return FontFamily.SansSerif

    return runCatching {
        FontFamily(
            listOf(
                R.font.montserrat_light to FontWeight.Light,
                R.font.montserrat_regular to FontWeight.Normal,
                R.font.montserrat_medium to FontWeight.Medium,
                R.font.montserrat_semibold to FontWeight.SemiBold,
                R.font.montserrat_bold to FontWeight.Bold,
            ).map { (resId, weight) ->
                Font(
                    resId = resId,
                    weight = weight,
                    style = FontStyle.Normal,
                    loadingStrategy = FontLoadingStrategy.OptionalLocal,
                )
            }
        )
    }.getOrElse { FontFamily.SansSerif }
}
