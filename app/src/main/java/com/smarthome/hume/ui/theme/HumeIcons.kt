package com.smarthome.hume.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bathtub
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Desk
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.material.icons.filled.Stairs
import androidx.compose.material.icons.filled.Thermometer
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.Bolt as BoltOutlined
import androidx.compose.material.icons.outlined.Home as HomeOutlined
import androidx.compose.material.icons.outlined.Person as PersonOutlined
import androidx.compose.material.icons.outlined.Shield as ShieldOutlined
import androidx.compose.ui.graphics.vector.ImageVector
import com.smarthome.hume.core.model.HumeTab

/*
 * Icon he thong: Material Icons (filled/outlined) — thay the Phosphor tu ve.
 * Quyet dinh 2026-09-28: Material Design dung icon phang Material Symbols;
 * tren Compose dung material-icons-extended (ImageVector), khong doi API.
 *
 * Navbar: tab DANG CHON dung ban filled, tab con lai dung outlined
 * - dung cach One UI / iOS 26 phan biet tab active.
 */
object HumeIcons {
    val Light = Icons.Filled.Lightbulb
    val Temperature = Icons.Filled.Thermometer
    val Humidity = Icons.Filled.WaterDrop
    val Climate = Icons.Filled.AcUnit
    val Door = Icons.Filled.MeetingRoom
    val DoorClosed = Icons.Filled.MeetingRoom
    val Alarm = Icons.Filled.Shield
    val AlarmOk = Icons.Filled.VerifiedUser
    val Bell = Icons.Filled.Notifications
    val Night = Icons.Filled.DarkMode
    val Solar = Icons.Filled.WbSunny
    val Battery = Icons.Filled.BatteryChargingFull
    val BatteryFull = Icons.Filled.BatteryFull
    val Desk = Icons.Filled.Desk
    val Sunrise = Icons.Filled.WbTwilight
    val Leaving = Icons.Filled.Logout
    val Coming = Icons.Filled.Home
    val Power = Icons.Filled.Bolt
    val Plug = Icons.Filled.Power
    val House = Icons.Filled.Home

    fun room(key: String): ImageVector = when (key) {
        "bed" -> Icons.Filled.Bed
        "child" -> Icons.Filled.ChildCare
        "sparkles" -> Icons.Filled.AutoAwesome
        "sofa" -> Icons.Filled.Weekend
        "bath" -> Icons.Filled.Bathtub
        "kitchen" -> Icons.Filled.SoupKitchen
        "washer" -> Icons.Filled.LocalLaundryService
        "hallway" -> Icons.Filled.Stairs
        else -> Icons.Filled.Home
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
        "fire" -> Icons.Filled.Whatshot
        "cooking" -> Icons.Filled.SoupKitchen
        "dishwasher", "washer", "dryer" -> Icons.Filled.LocalLaundryService
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

    /** Tab DANG CHON: icon filled. */
    fun tab(tab: HumeTab): ImageVector = when (tab) {
        HumeTab.Home -> Icons.Filled.Home
        HumeTab.Energy -> Icons.Filled.Bolt
        HumeTab.Security -> Icons.Filled.Shield
        HumeTab.Profile -> Icons.Filled.Person
    }

    /** Tab khong chon: outlined. */
    fun tabOutline(tab: HumeTab): ImageVector = when (tab) {
        HumeTab.Home -> HomeOutlined
        HumeTab.Energy -> BoltOutlined
        HumeTab.Security -> ShieldOutlined
        HumeTab.Profile -> PersonOutlined
    }

    fun tab(tab: HumeTab, selected: Boolean): ImageVector =
        if (selected) tab(tab) else tabOutline(tab)

    /** Icon for a scene, guessed from its name. */
    fun scene(label: String): ImageVector {
        val text = label.lowercase()
        return when {
            text.contains("sáng") || text.contains("morning") || text.contains("wake") -> Sunrise
            text.contains("ngủ") || text.contains("night") || text.contains("sleep") -> Night
            text.contains("ra khỏi") || text.contains("away") || text.contains("leave") -> Leaving
            text.contains("về nhà") || text.contains("home") || text.contains("arrive") -> Coming
            else -> Icons.Filled.AutoAwesome
        }
    }
}
