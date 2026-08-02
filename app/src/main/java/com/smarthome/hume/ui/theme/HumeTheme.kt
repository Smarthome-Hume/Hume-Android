package com.smarthome.hume.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/*
 * Colour system ported 1:1 from the SwiftUI project.
 *
 * Core/AppTheme.swift builds every neutral with
 *     Color.dynamic(light:dark:) -> UIColor { userInterfaceStyle == .dark ? dark : light }
 * so the iOS app follows the system setting and has no in-app switch. The
 * thirteen grey steps below are copied verbatim from that file, and the accent
 * colours are copied from the same file plus Theme/HumeTheme.swift. Accents are
 * plain constants in Swift, so they must NOT change between modes here either.
 *
 * The colours Swift takes from UIKit (systemBackground, label, secondaryLabel,
 * tertiaryLabel, tertiarySystemFill, secondarySystemFill) are reproduced with
 * Apple's published values for both appearances.
 */
object HumeColors {

    /**
     * Current appearance. Set once per composition by [HumeTheme] from
     * isSystemInDarkTheme(). It is snapshot state, so every colour read below
     * recomposes (and redraws inside Canvas) when the system theme flips.
     */
    var isDark by mutableStateOf(false)
        internal set

    private fun dyn(light: Color, dark: Color): Color = if (isDark) dark else light

    // ── AppTheme.swift: Color.dynamic grey ramp ──────────────────────────
    val Gray000: Color get() = dyn(Color(0xFFFFFFFF), Color(0xFF161616))
    val Gray00: Color get() = dyn(Color(0xFFF6F6F6), Color(0xFF313131))
    val Gray01: Color get() = dyn(Color(0xFFF6F6F6), Color(0xFF1C1C1C))
    val Gray100: Color get() = dyn(Color(0xFFCECECE), Color(0xFF2F2F2F))
    val Gray200: Color get() = dyn(Color(0xFFB5B5B5), Color(0xFF3A3A3A))
    val Gray300: Color get() = dyn(Color(0xFF9A9A9A), Color(0xFF545454))
    val Gray400: Color get() = dyn(Color(0xFF828282), Color(0xFF696969))
    val Gray500: Color get() = dyn(Color(0xFF6D6D6D), Color(0xFF7F7F7F))
    val Gray600: Color get() = dyn(Color(0xFF595959), Color(0xFF979797))
    val Gray700: Color get() = dyn(Color(0xFF484848), Color(0xFFAFAFAF))
    val Gray800: Color get() = dyn(Color(0xFF373737), Color(0xFFC7C7C7))
    val Gray900: Color get() = dyn(Color(0xFF262626), Color(0xFFEDEDED))
    val Gray1000: Color get() = dyn(Color(0xFF101010), Color(0xFFFAFAFA))

    // ── UIKit semantic colours used by HumeTheme.swift ───────────────────
    /** Color(.systemBackground) */
    val PageBG: Color get() = dyn(Color(0xFFFFFFFF), Color(0xFF000000))
    /** Color(.label) */
    val Label: Color get() = dyn(Color(0xFF000000), Color(0xFFFFFFFF))
    /** Color(.secondaryLabel) */
    val LabelSecondary: Color get() = dyn(Color(0x993C3C43), Color(0x99EBEBF5))
    /** Color(.tertiaryLabel) */
    val LabelTertiary: Color get() = dyn(Color(0x4D3C3C43), Color(0x4DEBEBF5))
    /** Color(.tertiarySystemFill) - the fallback background of humeElement */
    val FillTertiary: Color get() = dyn(Color(0x1F767680), Color(0x3D767680))
    /** Color(.secondarySystemFill) */
    val FillSecondary: Color get() = dyn(Color(0x29787880), Color(0x52787880))
    /** Separator, used for hairlines */
    val Separator: Color get() = dyn(Color(0x4A3C3C43), Color(0xA6545458))

    // ── Fixed accents (AppTheme.swift + Theme/HumeTheme.swift) ───────────
    /** AppTheme.activeOrange */
    val Orange = Color(0xFFF9784C)
    /** ProfileView header gradient midpoint */
    val OrangeDeep = Color(0xFFE8653A)
    /** AppTheme.activePink */
    val Salmon = Color(0xFFFAC0B6)
    /** HumeTheme.orange */
    val OrangePure = Color(0xFFFF7700)
    /** HumeTheme.green */
    val Green = Color(0xFF4AC84F)
    val Purple = Color(0xFFAD99E6)
    val Yellow = Color(0xFFF2D26F)
    val Red = Color(0xFFF28073)
    val Blue = Color(0xFF73B9F2)
    val Pink = Color(0xFFF285C9)
    val Lime = Color(0xFFB8E674)
    val AlarmGreen = Color(0xFF4CAF50)
    val AlarmOrange = Color(0xFFFF9800)
    val AlarmBlue = Color(0xFF5C6BC0)
    val AlarmRed = Color(0xFFF28073)
    val NotifRed = Color(0xFFFF5252)
    val SolarYellow = Color(0xFFF2D26F)
    val BattGreen = Color(0xFF3EFD51)
    val BattOrange = Color(0xFFF9784C)

    // ── Names the Android screens already use ────────────────────────────
    // Kept so no call site has to change; each one now points at the Swift
    // token it actually corresponds to.

    /** Page background: Color(.systemBackground). */
    val Background: Color get() = PageBG
    /** Card surface: AppTheme.gray000, the base of every glass card. */
    val Card: Color get() = Gray000
    /** Recessed surface behind cards: AppTheme.gray01. */
    val CardSunken: Color get() = Gray01
    val TextPrimary: Color get() = Label
    val TextSecondary: Color get() = LabelSecondary
    val Divider: Color get() = dyn(Color(0xFFE6E6E6), Color(0xFF2F2F2F))
    /** AppTheme.gray1000, the strongest ink colour. */
    val Ink: Color get() = Gray1000
    /** Inactive bar/track: AppTheme.gray100. */
    val BarGrey: Color get() = Gray100

    // Tinted fills. Swift paints these as accent-with-opacity so they work on
    // either background; keeping them translucent does the same here.
    val OrangeSoft: Color get() = Orange.copy(alpha = if (isDark) 0.22f else 0.14f)
    val OrangeSofter: Color get() = Orange.copy(alpha = if (isDark) 0.14f else 0.08f)
    val ChipPink: Color get() = Orange.copy(alpha = if (isDark) 0.24f else 0.12f)
    val ChipYellow: Color get() = Yellow.copy(alpha = if (isDark) 0.24f else 0.18f)
    val ChipYellowIcon: Color get() = Yellow
    val SceneGreenBg: Color get() = AlarmGreen.copy(alpha = if (isDark) 0.22f else 0.14f)
    val SceneGreen: Color get() = AlarmGreen
    val SalmonSoft: Color get() = Salmon.copy(alpha = if (isDark) 0.28f else 0.55f)
    val Amber: Color get() = AlarmOrange
    val AmberBar: Color get() = Color(0xFFFFB74D)

    // Room card "light is on" gradient = AppTheme.activeGradient.
    val RoomOnStart: Color get() = Orange.copy(alpha = if (isDark) 0.34f else 0.22f)
    val RoomOnEnd: Color get() = Salmon.copy(alpha = if (isDark) 0.26f else 0.55f)
}

/** Frosted-glass constants matching HumeCardModifier / HumeElementModifier. */
object HumeGlass {
    /** .regularMaterial over the page background. */
    val card: Color get() = if (HumeColors.isDark) Color(0xB3161616) else Color(0xD9FFFFFF)
    /** Specular edge: .white.opacity(0.14) in Swift, dimmed for light mode. */
    val edge: Color get() = if (HumeColors.isDark) Color(0x24FFFFFF) else Color(0x14000000)
    /** humeElement fallback background: Color(.tertiarySystemFill). */
    val element: Color get() = HumeColors.FillTertiary
}

private fun schemeFor(dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = Color(0xFFF9784C),
        onPrimary = Color(0xFF3A1206),
        primaryContainer = Color(0xFF7A2E14),
        onPrimaryContainer = Color(0xFFFFDBCF),
        secondary = Color(0xFFFAC0B6),
        onSecondary = Color(0xFF3A1206),
        tertiary = Color(0xFFF2D26F),
        background = Color(0xFF000000),
        onBackground = Color(0xFFFFFFFF),
        surface = Color(0xFF000000),
        onSurface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFF1C1C1C),
        onSurfaceVariant = Color(0xFFAFAFAF),
        surfaceContainerLowest = Color(0xFF101010),
        surfaceContainerLow = Color(0xFF161616),
        surfaceContainer = Color(0xFF161616),
        surfaceContainerHigh = Color(0xFF1C1C1C),
        surfaceContainerHighest = Color(0xFF313131),
        outline = Color(0xFF545454),
        outlineVariant = Color(0xFF2F2F2F),
        error = Color(0xFFF28073),
    )
} else {
    lightColorScheme(
        primary = Color(0xFFF9784C),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFAC0B6),
        onPrimaryContainer = Color(0xFF101010),
        secondary = Color(0xFFFAC0B6),
        onSecondary = Color(0xFF101010),
        tertiary = Color(0xFFF2D26F),
        background = Color(0xFFFFFFFF),
        onBackground = Color(0xFF000000),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF000000),
        surfaceVariant = Color(0xFFF6F6F6),
        onSurfaceVariant = Color(0xFF6D6D6D),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFFFFFFF),
        surfaceContainer = Color(0xFFF6F6F6),
        surfaceContainerHigh = Color(0xFFF6F6F6),
        surfaceContainerHighest = Color(0xFFCECECE),
        outline = Color(0xFF9A9A9A),
        outlineVariant = Color(0xFFCECECE),
        error = Color(0xFFF28073),
    )
}

/**
 * Material shape scale for the theme. The app's own corner radii live in the
 * HumeShapes object in HumeSurfaces.kt, so this one keeps a different name.
 */
private val HumeMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

/**
 * Follows the system appearance, exactly like the SwiftUI app, which never
 * calls preferredColorScheme and lets Color.dynamic resolve per trait.
 */
@Composable
fun HumeTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    SideEffect { HumeColors.isDark = darkTheme }
    MaterialTheme(
        colorScheme = schemeFor(darkTheme),
        shapes = HumeMaterialShapes,
        content = content,
    )
}
