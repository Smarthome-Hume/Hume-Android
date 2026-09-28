package com.smarthome.hume.core.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.ui.R

/**
 * Hume M3 Expressive theme — port tu demo HTML (hume-m3e-v4-dashboard.html).
 *
 * Quy tac da chot voi user:
 * - Shape expressive: card 28-44dp (KHONG dung 12dp baseline M3 thuong).
 * - Icon: Material Symbols Rounded outlined-only tuyet doi.
 * - Motion: vao = emphasized decelerate, ra = accelerate, tuong tac = spring overshoot.
 * - Type: Montserrat; display 57/700, headline 24/500.
 */

// ---- Extra colors khong co trong M3 ColorScheme (tu demo) ----
@Immutable
data class HumeExtraColors(
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val violet: Color,
    val violetContainer: Color,
    val onVioletContainer: Color,
    val pink: Color,
    val pinkContainer: Color,
    val onPinkContainer: Color,
    /** Nen trang / surface goc cua demo (dung cho tonal layering). */
    val surfaceLow: Color,
    val surfaceHigh: Color,
    /** --surfaceHighest cua demo: the (card) — #FFFFFF light / #35302A dark. */
    val surfaceHighest: Color,
    /** --surfaceLowest cua demo: navbar — #F4F2EF light / #28211B dark. */
    val surfaceLowest: Color,
)

private fun extraColors(seed: M3ESeed, dark: Boolean): HumeExtraColors {
    val s = seed.name.lowercase()
    fun t(name: String): Color {
        val map = if (dark) M3E_DARK_TOKENS else M3E_LIGHT_TOKENS
        val v = map[s]?.get(name) ?: map["cam"]!!.getValue(name)
        return Color(0xFF000000 or v.toLong(16))
    }
    return HumeExtraColors(
        success = t("success"), successContainer = t("successContainer"),
        onSuccessContainer = t("onSuccessContainer"),
        info = t("info"), infoContainer = t("infoContainer"),
        onInfoContainer = t("onInfoContainer"),
        violet = t("violet"), violetContainer = t("violetContainer"),
        onVioletContainer = t("onVioletContainer"),
        pink = t("pink"), pinkContainer = t("pinkContainer"),
        onPinkContainer = t("onPinkContainer"),
        surfaceLow = t("surfaceLow"), surfaceHigh = t("surfaceHigh"),
        surfaceHighest = t("surfaceHighest"),
        surfaceLowest = t("surfaceLowest"),
    )
}

val LocalHumeExtraColors = staticCompositionLocalOf {
    HumeExtraColors(
        success = Color.Unspecified, successContainer = Color.Unspecified,
        onSuccessContainer = Color.Unspecified, info = Color.Unspecified,
        infoContainer = Color.Unspecified, onInfoContainer = Color.Unspecified,
        violet = Color.Unspecified, violetContainer = Color.Unspecified,
        onVioletContainer = Color.Unspecified, pink = Color.Unspecified,
        pinkContainer = Color.Unspecified, onPinkContainer = Color.Unspecified,
        surfaceLow = Color.Unspecified, surfaceHigh = Color.Unspecified,
        surfaceHighest = Color.Unspecified,
        surfaceLowest = Color.Unspecified,
    )
}

// ---- Shape expressive (thang M3E mo rong, khong phai baseline) ----
val HumeShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(28.dp),   // card chuan
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(44.dp), // sheet
)

// ---- Motion tokens (tu demo: --ease, --spring) ----
object HumeMotion {
    /** vao: emphasized decelerate cubic-bezier(.05,.7,.1,1) */
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** tuong tac/morph: spring overshoot cubic-bezier(.34,1.45,.5,1) */
    fun <T> expressiveSpring() = spring<T>(
        dampingRatio = 0.7f,
        stiffness = Spring.StiffnessMedium,
    )

    /** press morph: giam 1 nac bo (28 -> 16/18dp), CAM nhay pill -> chu nhat */
    const val PressMorphMs = 180
}

// ---- Typography: Montserrat ----
private val MontserratProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)
private val Montserrat = GoogleFont("Montserrat")

/** Montserrat full weights — gan cho TOAN BO text styles, khong de style nao roi ve font he thong. */
private val montserratFamily = androidx.compose.ui.text.font.FontFamily(
    Font(googleFont = Montserrat, fontProvider = MontserratProvider, weight = FontWeight.Light),
    Font(googleFont = Montserrat, fontProvider = MontserratProvider, weight = FontWeight.Normal),
    Font(googleFont = Montserrat, fontProvider = MontserratProvider, weight = FontWeight.Medium),
    Font(googleFont = Montserrat, fontProvider = MontserratProvider, weight = FontWeight.SemiBold),
    Font(googleFont = Montserrat, fontProvider = MontserratProvider, weight = FontWeight.Bold),
    Font(googleFont = Montserrat, fontProvider = MontserratProvider, weight = FontWeight.ExtraBold),
)

private fun montserrat(weight: FontWeight) = androidx.compose.ui.text.font.FontFamily(
    Font(googleFont = Montserrat, fontProvider = MontserratProvider, weight = weight)
)

val HumeTypography = with(Typography()) {
    copy(
        displayLarge = displayLarge.copy(
            fontFamily = montserratFamily, fontWeight = FontWeight.Bold,
            fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
        displayMedium = displayMedium.copy(fontFamily = montserratFamily),
        displaySmall = displaySmall.copy(fontFamily = montserratFamily),
        headlineLarge = headlineLarge.copy(fontFamily = montserratFamily),
        headlineMedium = headlineMedium.copy(
            fontFamily = montserratFamily, fontWeight = FontWeight.Medium,
            fontSize = 24.sp, lineHeight = 32.sp),
        headlineSmall = headlineSmall.copy(fontFamily = montserratFamily),
        titleLarge = titleLarge.copy(fontFamily = montserratFamily),
        titleMedium = titleMedium.copy(fontFamily = montserratFamily),
        titleSmall = titleSmall.copy(fontFamily = montserratFamily),
        bodyLarge = bodyLarge.copy(fontFamily = montserratFamily),
        bodyMedium = bodyMedium.copy(fontFamily = montserratFamily),
        bodySmall = bodySmall.copy(fontFamily = montserratFamily),
        labelLarge = labelLarge.copy(fontFamily = montserratFamily),
        labelMedium = labelMedium.copy(fontFamily = montserratFamily),
        labelSmall = labelSmall.copy(fontFamily = montserratFamily),
    )
}

// ---- Theme entry ----
@Composable
fun HumeM3ETheme(
    seed: M3ESeed = M3ESeed.Cam,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = colorSchemeFor(seed, darkTheme)
    CompositionLocalProvider(LocalHumeExtraColors provides extraColors(seed, darkTheme)) {
        MaterialTheme(
            colorScheme = scheme,
            shapes = HumeShapes,
            typography = HumeTypography,
            content = content,
        )
    }
}
