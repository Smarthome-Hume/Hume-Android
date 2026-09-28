package com.smarthome.hume.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bathtub
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.Desk
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalLaundryService
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.SoupKitchen
import androidx.compose.material.icons.outlined.Stairs
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Icon he thong M3E: Material Symbols Rounded outlined-only tuyet doi.
 * Trang thai bat/chon the hien bang pill/container + chu dam, khong doi filled.
 */
object M3EIcons {
    val Light = Icons.Outlined.Lightbulb
    val Solar = Icons.Outlined.WbSunny
    val Battery = Icons.Outlined.BatteryChargingFull
    val Power = Icons.Outlined.Bolt
    val Plug = Icons.Outlined.Power
    val Home = Icons.Outlined.Home
    val Climate = Icons.Outlined.AcUnit

    fun room(key: String): ImageVector = when (key) {
        "bed" -> Icons.Outlined.Bed
        "child" -> Icons.Outlined.ChildCare
        "sparkles" -> Icons.Outlined.AutoAwesome
        "sofa" -> Icons.Outlined.Weekend
        "bath" -> Icons.Outlined.Bathtub
        "kitchen" -> Icons.Outlined.SoupKitchen
        "washer" -> Icons.Outlined.LocalLaundryService
        "hallway" -> Icons.Outlined.Stairs
        else -> Icons.Outlined.Home
    }

    fun device(key: String): ImageVector = when (key) {
        "sun" -> Solar
        "plug" -> Plug
        "house" -> Home
        "desk" -> Icons.Outlined.Desk
        "door" -> Icons.Outlined.MeetingRoom
        "snowflake" -> Climate
        "fire" -> Icons.Outlined.Whatshot
        "bulb", "lightbulb" -> Light
        "switch" -> Power
        else -> Power
    }
}
