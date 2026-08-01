package com.smarthome.hume.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Palette sampled from the prototype recording. */
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

private val LightScheme = lightColorScheme(
    primary = HumeColors.Orange,
    onPrimary = Color.White,
    primaryContainer = HumeColors.OrangeSoft,
    onPrimaryContainer = HumeColors.TextPrimary,
    secondary = HumeColors.Salmon,
    onSecondary = Color.White,
    secondaryContainer = HumeColors.ChipPink,
    onSecondaryContainer = HumeColors.TextPrimary,
    tertiary = HumeColors.Amber,
    background = HumeColors.Background,
    onBackground = HumeColors.TextPrimary,
    surface = HumeColors.Background,
    onSurface = HumeColors.TextPrimary,
    surfaceVariant = Color(0xFFF1EBE7),
    onSurfaceVariant = HumeColors.TextSecondary,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFFBF8F6),
    surfaceContainerHighest = Color(0xFFF3EDEA),
    outline = Color(0xFFDCD5D0),
    outlineVariant = HumeColors.Divider,
    error = HumeColors.Red,
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFFF9E7D),
    onPrimary = Color(0xFF3A1206),
    primaryContainer = Color(0xFF7A2E14),
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = Color(0xFF9FB4D6),
    secondaryContainer = Color(0xFF2C3647),
    onSecondaryContainer = Color(0xFFE6EDF9),
    tertiary = HumeColors.Amber,
    background = Color(0xFF12100F),
    onBackground = Color(0xFFF2EDEA),
    surface = Color(0xFF12100F),
    onSurface = Color(0xFFF2EDEA),
    surfaceVariant = Color(0xFF2A2725),
    onSurfaceVariant = Color(0xFFB6ADA8),
    surfaceContainerLowest = Color(0xFF171514),
    surfaceContainerLow = Color(0xFF1B1918),
    surfaceContainer = Color(0xFF1F1D1B),
    surfaceContainerHigh = Color(0xFF2A2725),
    surfaceContainerHighest = Color(0xFF353230),
    outline = Color(0xFF6B625D),
    outlineVariant = Color(0xFF3A3634),
    error = Color(0xFFFF6B6F),
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

@Composable
fun HumeTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        shapes = HumeMaterialShapes,
        content = content,
    )
}
