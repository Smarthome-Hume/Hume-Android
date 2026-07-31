package com.smarthome.hume.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object HumeColors {
    val Orange = Color(0xFFF9784C)
    val Green = Color(0xFF66D19E)
    val Blue = Color(0xFF73B9F2)
    val Purple = Color(0xFFAD99E6)
}

@Composable
fun HumeTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) darkColorScheme(primary = HumeColors.Orange, secondary = HumeColors.Blue) else lightColorScheme(primary = HumeColors.Orange, secondary = HumeColors.Blue, background = Color(0xFFF7F7F9))
    MaterialTheme(colorScheme = scheme, content = content)
}
