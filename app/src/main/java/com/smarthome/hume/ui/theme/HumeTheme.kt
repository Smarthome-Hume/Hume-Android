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

/** Brand accents carried over from the SwiftUI app. */
object HumeColors {
    val Orange = Color(0xFFF9784C)
    val Green = Color(0xFF66D19E)
    val Blue = Color(0xFF73B9F2)
    val Purple = Color(0xFFAD99E6)
    val Amber = Color(0xFFFFC46B)
    val Red = Color(0xFFE5484D)
}

private val LightScheme = lightColorScheme(
    primary = Color(0xFFB3300B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBD1),
    onPrimaryContainer = Color(0xFF3D0700),
    secondary = Color(0xFF00629E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1E4FF),
    onSecondaryContainer = Color(0xFF001D34),
    tertiary = Color(0xFF2A6A4B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFAFF2C9),
    onTertiaryContainer = Color(0xFF00210F),
    background = Color(0xFFF6F3F1),
    onBackground = Color(0xFF201A18),
    surface = Color(0xFFF6F3F1),
    onSurface = Color(0xFF201A18),
    surfaceVariant = Color(0xFFF5DED8),
    onSurfaceVariant = Color(0xFF53433F),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFFBF8F6),
    surfaceContainerHighest = Color(0xFFF2ECE9),
    outline = Color(0xFF85736E),
    outlineVariant = Color(0xFFD8C2BC),
    error = Color(0xFFBA1A1A),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFFFB5A0),
    onPrimary = Color(0xFF5F1500),
    primaryContainer = Color(0xFF872000),
    onPrimaryContainer = Color(0xFFFFDBD1),
    secondary = Color(0xFF9BCBFF),
    onSecondary = Color(0xFF003355),
    secondaryContainer = Color(0xFF004A78),
    onSecondaryContainer = Color(0xFFD1E4FF),
    tertiary = Color(0xFF94D5AE),
    onTertiary = Color(0xFF00391F),
    tertiaryContainer = Color(0xFF0B5135),
    onTertiaryContainer = Color(0xFFAFF2C9),
    background = Color(0xFF12100F),
    onBackground = Color(0xFFEDE0DC),
    surface = Color(0xFF12100F),
    onSurface = Color(0xFFEDE0DC),
    surfaceVariant = Color(0xFF53433F),
    onSurfaceVariant = Color(0xFFD8C2BC),
    surfaceContainerLowest = Color(0xFF0D0B0A),
    surfaceContainerLow = Color(0xFF1B1917),
    surfaceContainer = Color(0xFF1F1D1B),
    surfaceContainerHigh = Color(0xFF2A2725),
    surfaceContainerHighest = Color(0xFF353230),
    outline = Color(0xFFA08C87),
    outlineVariant = Color(0xFF53433F),
    error = Color(0xFFFFB4AB),
)

/** One UI leans on very round corners, so the whole shape scale is bumped up. */
private val HumeShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun HumeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        shapes = HumeShapes,
        content = content,
    )
}
