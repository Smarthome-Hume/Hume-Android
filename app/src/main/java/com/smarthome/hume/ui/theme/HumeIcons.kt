package com.smarthome.hume.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bathtub
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bed
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.Desk
import androidx.compose.material.icons.rounded.DoorFront
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material.icons.rounded.Weekend
import androidx.compose.ui.graphics.vector.ImageVector
import com.smarthome.hume.core.model.HumeTab

object HumeIcons {
    val Light = Icons.Rounded.Lightbulb
    val Temperature = Icons.Rounded.Thermostat
    val Humidity = Icons.Rounded.WaterDrop
    val Climate = Icons.Rounded.AcUnit
    val Door = Icons.Rounded.MeetingRoom
    val DoorClosed = Icons.Rounded.DoorFront
    val Alarm = Icons.Rounded.Shield
    val Bell = Icons.Rounded.Notifications
    val Night = Icons.Rounded.NightsStay
    val Solar = Icons.Rounded.WbSunny
    val Battery = Icons.Rounded.BatteryChargingFull
    val Desk = Icons.Rounded.Desk
    val Sunrise = Icons.Rounded.WbTwilight
    val Leaving = Icons.Rounded.Logout
    val Coming = Icons.Rounded.Home

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

    /** Icon for a scene, guessed from its name. */
    fun scene(label: String): ImageVector {
        val text = label.lowercase()
        return when {
            text.contains("s\u00e1ng") || text.contains("morning") || text.contains("wake") -> Sunrise
            text.contains("ng\u1ee7") || text.contains("night") || text.contains("sleep") -> Night
            text.contains("ra kh\u1ecfi") || text.contains("away") || text.contains("leave") -> Leaving
            text.contains("v\u1ec1 nh\u00e0") || text.contains("home") || text.contains("arrive") -> Coming
            else -> Icons.Rounded.AutoAwesome
        }
    }
}
