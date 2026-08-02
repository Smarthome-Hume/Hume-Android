package com.smarthome.hume.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Palette sampled from the SwiftUI build. The iOS app ships a single fixed
 * palette (.preferredColorScheme(.light)), so every card colour here is a
 * constant and never flips with the system theme.
 */
object HumeColors {
    val Orange = Color(0xFFF9784C)
    val OrangeDeep = Color(0xFFE8542A)
    val OrangeSoft = Color(0xFFFCE3D9)
    val OrangeSofter = Color(0xFFFDF0EA)
    val Salmon = Color(0xFFF5A18C)
    val SalmonSoft = Color(0xFFF8BCAB)
    val RoomOnStart = Color(0xFFFBDCCD)
    val RoomOnEnd = Color(0xFFF7C9B4)
    val ChipPink = Color(0xFFFDE9E2)
    val ChipYellow = Color(0xFFFFF4DA)
    val ChipYellowIcon = Color(0xFFF2B33D)
    val SceneGreenBg = Color(0xFFE9F7EF)
    val SceneGreen = Color(0xFF3BA776)
    val Green = Color(0xFF3ED598)
    val Blue = Color(0xFF73B9F2)
    val Amber = Color(0xFFFFC46B)
    val AmberBar = Color(0xFFFFE08A)
    val BarGrey = Color(0xFFD9D5D2)
    val Purple = Color(0xFFAD99E6)
    val Red = Color(0xFFE5484D)
    val Background = Color(0xFFF7F4F2)
    val Card = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFF211D1B)
    val TextSecondary = Color(0xFF938A85)
    val Divider = Color(0xFFEDE7E3)
    val Ink = Color(0xFF16130F)
}

/**
 * Every Material role is mapped onto the Hume palette, so a composable that
 * reads MaterialTheme.colorScheme lands on the same colours as one that reads
 * HumeColors directly. Mixing the two was what produced dark surfaces behind
 * light cards when the phone was in dark mode.
 */
private val HumeScheme = lightColorScheme(
    primary = HumeColors.Orange,
    onPrimary = Color.White,
    primaryContainer = HumeColors.OrangeSoft,
    onPrimaryContainer = HumeColors.TextPrimary,
    inversePrimary = HumeColors.OrangeDeep,
    secondary = HumeColors.Salmon,
    onSecondary = Color.White,
    secondaryContainer = HumeColors.ChipPink,
    onSecondaryContainer = HumeColors.TextPrimary,
    tertiary = HumeColors.Amber,
    onTertiary = HumeColors.TextPrimary,
    tertiaryContainer = HumeColors.ChipYellow,
    onTertiaryContainer = HumeColors.TextPrimary,
    background = HumeColors.Background,
    onBackground = HumeColors.TextPrimary,
    surface = HumeColors.Background,
    onSurface = HumeColors.TextPrimary,
    surfaceVariant = Color(0xFFF1EBE7),
    onSurfaceVariant = HumeColors.TextSecondary,
    surfaceTint = HumeColors.Orange,
    inverseSurface = HumeColors.Ink,
    inverseOnSurface = Color(0xFFF7F4F2),
    surfaceContainerLowest = HumeColors.Card,
    surfaceContainerLow = HumeColors.Card,
    surfaceContainer = HumeColors.Card,
    surfaceContainerHigh = Color(0xFFFBF8F6),
    surfaceContainerHighest = Color(0xFFF3EDEA),
    outline = Color(0xFFDCD5D0),
    outlineVariant = HumeColors.Divider,
    scrim = Color(0x66000000),
    error = HumeColors.Red,
    onError = Color.White,
    errorContainer = Color(0xFFFBE0E0),
    onErrorContainer = Color(0xFF7A1216),
)

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
 * The app has one look. `darkTheme` is kept only so existing call sites keep
 * compiling; it is deliberately ignored, exactly like the SwiftUI original,
 * which pins .light for every screen.
 */
@Composable
fun HumeTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HumeScheme,
        shapes = HumeMaterialShapes,
        content = content,
    )
}
