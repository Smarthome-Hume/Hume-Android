package com.smarthome.hume.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bathtub
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Desk
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalLaundryService
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SoupKitchen
import androidx.compose.material.icons.outlined.Stairs
import androidx.compose.material.icons.outlined.Thermometer
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.ui.graphics.vector.ImageVector
import com.smarthome.hume.core.model.HumeTab

/*
 * Icon he thong: Material Icons OUTLINED (net manh, mem mai) — thay Phosphor tu ve.
 * Quyet dinh 2026-09-28 (user chon outlined manh): Material Design icon net manh;
 * tren Compose dung material-icons-extended (ImageVector).
 *
 * Navbar: tat ca outlined; tab active nhan manh bang pill + label SemiBold.
 */
object HumeIcons {
    val Light = Icons.Outlined.Lightbulb
    val Temperature = Icons.Outlined.Thermometer
    val Humidity = Icons.Outlined.WaterDrop
    val Climate = Icons.Outlined.AcUnit
    val Door = Icons.Outlined.MeetingRoom
    val DoorClosed = Icons.Outlined.MeetingRoom
    val Alarm = Icons.Outlined.Shield
    val AlarmOk = Icons.Outlined.VerifiedUser
    val Bell = Icons.Outlined.Notifications
    val Night = Icons.Outlined.DarkMode
    val Solar = Icons.Outlined.WbSunny
    val Battery = Icons.Outlined.BatteryChargingFull
    val BatteryFull = Icons.Outlined.BatteryFull
    val Desk = Icons.Outlined.Desk
    val Sunrise = Icons.Outlined.WbTwilight
    val Leaving = Icons.Outlined.Logout
    val Coming = Icons.Outlined.Home
    val Power = Icons.Outlined.Bolt
    val Plug = Icons.Outlined.Power
    val House = Icons.Outlined.Home

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

    /** Icon keys used by the sensor/device cards. */
    fun sensor(key: String): ImageVector = when (key) {
        "sun" -> Solar
        "battery-charging" -> Battery
        "battery-full" -> BatteryFull
        "plug" -> Plug
        "house" -> House
        "desk" -> Desk
        "door" -> DoorClosed
        "snowflake" -> Climate
        "fire" -> Icons.Outlined.Whatshot
        "cooking" -> Icons.Outlined.SoupKitchen
        "dishwasher", "washer", "dryer" -> Icons.Outlined.LocalLaundryService
        "bulb", "lightbulb" -> Light
        else -> Power
    }

    /**
     * Icon for an entity id, used by the energy power bars.
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

    /** Navbar: outlined manh — tab active phan biet bang pill + label dam. */
    fun tab(tab: HumeTab): ImageVector = when (tab) {
        HumeTab.Home -> Icons.Outlined.Home
        HumeTab.Energy -> Icons.Outlined.Bolt
        HumeTab.Security -> Icons.Outlined.Shield
        HumeTab.Profile -> Icons.Outlined.Person
    }

    /** Icon for a scene, guessed from its name. */
    fun scene(label: String): ImageVector {
        val text = label.lowercase()
        return when {
            text.contains("sáng") || text.contains("morning") || text.contains("wake") -> Sunrise
            text.contains("ngủ") || text.contains("night") || text.contains("sleep") -> Night
            text.contains("ra khỏi") || text.contains("away") || text.contains("leave") -> Leaving
            text.contains("về nhà") || text.contains("home") || text.contains("arrive") -> Coming
            else -> Icons.Outlined.AutoAwesome
        }
    }
}
