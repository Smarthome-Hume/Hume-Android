package com.smarthome.hume.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bathtub
import androidx.compose.material.icons.rounded.Bed
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Weekend
import androidx.compose.ui.graphics.vector.ImageVector
import com.smarthome.hume.core.model.HumeTab

/**
 * SF Symbols -> Material Icons mapping, as planned in the porting notes.
 * RoomConfig.icon carries the old SF Symbol style keys.
 */
object HumeIcons {
    val Light: ImageVector = Icons.Rounded.Lightbulb
    val Temperature: ImageVector = Icons.Rounded.Thermostat
    val Humidity: ImageVector = Icons.Rounded.WaterDrop
    val Climate: ImageVector = Icons.Rounded.AcUnit
    val Door: ImageVector = Icons.Rounded.MeetingRoom
    val Alarm: ImageVector = Icons.Rounded.Shield

    fun room(key: String): ImageVector = when (key) {
        "bed" -> Icons.Rounded.Bed
        "child" -> Icons.Rounded.ChildCare
        "sparkles" -> Icons.Rounded.AutoAwesome
        "sofa" -> Icons.Rounded.Weekend
        "bath" -> Icons.Rounded.Bathtub
        "kitchen" -> Icons.Rounded.Kitchen
        else -> Icons.Rounded.Home
    }

    fun tab(tab: HumeTab): ImageVector = when (tab) {
        HumeTab.Home -> Icons.Rounded.Home
        HumeTab.Energy -> Icons.Rounded.Bolt
        HumeTab.Security -> Icons.Rounded.Shield
        HumeTab.Profile -> Icons.Rounded.Person
        HumeTab.AI -> Icons.Rounded.AutoAwesome
    }
}
