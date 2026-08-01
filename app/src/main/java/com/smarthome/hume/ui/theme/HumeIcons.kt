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
import androidx.compose.material.icons.rounded.ElectricalServices
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocalLaundryService
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SoupKitchen
import androidx.compose.material.icons.rounded.Stairs
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material.icons.rounded.Weekend
import androidx.compose.ui.graphics.vector.ImageVector
import com.smarthome.hume.core.model.HumeTab

/** SF Symbol -> Material icon mapping, mirroring IconMapper.swift. */
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
    val Power = Icons.Rounded.Bolt
    val Plug = Icons.Rounded.ElectricalServices
    val House = Icons.Rounded.Home

    fun room(key: String): ImageVector = when (key) {
        "bed" -> Icons.Rounded.Bed
        "child" -> Icons.Rounded.ChildCare
        "sparkles" -> Icons.Rounded.AutoAwesome
        "sofa" -> Icons.Rounded.Weekend
        "bath" -> Icons.Rounded.Bathtub
        "kitchen" -> Icons.Rounded.Kitchen
        "washer" -> Icons.Rounded.LocalLaundryService
        "hallway" -> Icons.Rounded.Stairs
        else -> Icons.Rounded.Home
    }

    /** Icon keys used by the SwiftUI sensor/device cards. */
    fun sensor(key: String): ImageVector = when (key) {
        "sun" -> Solar
        "battery-full", "battery-charging" -> Battery
        "plug" -> Plug
        "house" -> House
        "desk" -> Desk
        "door" -> DoorClosed
        "snowflake" -> Climate
        "fire" -> Icons.Rounded.LocalFireDepartment
        "cooking" -> Icons.Rounded.SoupKitchen
        "dishwasher", "washer", "dryer" -> Icons.Rounded.LocalLaundryService
        else -> Power
    }

    /**
     * Icon for an entity id, used by the energy power bars where SwiftUI passes
     * battery-charging / sun / plug / house per bar.
     */
    fun forEntity(entityId: String): ImageVector {
        val id = entityId.lowercase()
        return when {
            id.contains("battery") -> Battery
            id.contains("pv") || id.contains("solar") -> Solar
            id.contains("aptomat") || id.contains("grid") -> Plug
            id.contains("nha") || id.contains("home") -> House
            else -> Power
        }
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
