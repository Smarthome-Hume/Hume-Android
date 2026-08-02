package com.smarthome.hume.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bathtub
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.Desk
import androidx.compose.material.icons.outlined.DoorFront
import androidx.compose.material.icons.outlined.ElectricalServices
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LocalLaundryService
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SoupKitchen
import androidx.compose.material.icons.outlined.Stairs
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.ui.graphics.vector.ImageVector
import com.smarthome.hume.core.model.HumeTab

/*
 * Bo icon THONG NHAT theo Material Design kieu OUTLINE - dung net mong giong
 * bo icon (lucide) cua ban HTML cocopi da gui, thay cho bo Rounded dac truoc day.
 * Chi rieng tab dang chon tren navbar moi dung ban DAC (Rounded) de tao tuong
 * phan .thin -> .semibold nhu iOS.
 */
object HumeIcons {
    val Light = Icons.Outlined.Lightbulb
    val Temperature = Icons.Outlined.Thermostat
    val Humidity = Icons.Outlined.WaterDrop
    val Climate = Icons.Outlined.AcUnit
    val Door = Icons.Outlined.MeetingRoom
    val DoorClosed = Icons.Outlined.DoorFront
    val Alarm = Icons.Outlined.Shield
    val Bell = Icons.Outlined.Notifications
    val Night = Icons.Outlined.NightsStay
    val Solar = Icons.Outlined.WbSunny
    val Battery = Icons.Outlined.BatteryChargingFull
    val Desk = Icons.Outlined.Desk
    val Sunrise = Icons.Outlined.WbTwilight
    val Leaving = Icons.Outlined.Logout
    val Coming = Icons.Outlined.Home
    val Power = Icons.Outlined.Bolt
    val Plug = Icons.Outlined.ElectricalServices
    val House = Icons.Outlined.Home

    fun room(key: String): ImageVector = when (key) {
        "bed" -> Icons.Outlined.Bed
        "child" -> Icons.Outlined.ChildCare
        "sparkles" -> Icons.Outlined.AutoAwesome
        "sofa" -> Icons.Outlined.Weekend
        "bath" -> Icons.Outlined.Bathtub
        "kitchen" -> Icons.Outlined.Kitchen
        "washer" -> Icons.Outlined.LocalLaundryService
        "hallway" -> Icons.Outlined.Stairs
        else -> Icons.Outlined.Home
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
        "fire" -> Icons.Outlined.LocalFireDepartment
        "cooking" -> Icons.Outlined.SoupKitchen
        "dishwasher", "washer", "dryer" -> Icons.Outlined.LocalLaundryService
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

    /** Bo icon DAC - dung cho tab dang chon (tuong duong SF Symbol .fill / .semibold). */
    fun tab(tab: HumeTab): ImageVector = when (tab) {
        HumeTab.Home -> Icons.Rounded.Home
        HumeTab.Energy -> Icons.Rounded.Bolt
        HumeTab.Security -> Icons.Rounded.Shield
        HumeTab.Profile -> Icons.Rounded.Person
        HumeTab.AI -> Icons.Rounded.AutoAwesome
    }

    /** Bo icon VIEN MANH - dung cho tab khong chon (tuong duong SF Symbol .thin). */
    fun tabOutline(tab: HumeTab): ImageVector = when (tab) {
        HumeTab.Home -> Icons.Outlined.Home
        HumeTab.Energy -> Icons.Outlined.Bolt
        HumeTab.Security -> Icons.Outlined.Shield
        HumeTab.Profile -> Icons.Outlined.Person
        HumeTab.AI -> Icons.Outlined.AutoAwesome
    }

    /** Chon bo icon theo trang thai tab: chon -> dac, khong chon -> vien manh. */
    fun tab(tab: HumeTab, selected: Boolean): ImageVector =
        if (selected) tab(tab) else tabOutline(tab)

    /** Icon for a scene, guessed from its name. */
    fun scene(label: String): ImageVector {
        val text = label.lowercase()
        return when {
            text.contains("s\u00e1ng") || text.contains("morning") || text.contains("wake") -> Sunrise
            text.contains("ng\u1ee7") || text.contains("night") || text.contains("sleep") -> Night
            text.contains("ra kh\u1ecfi") || text.contains("away") || text.contains("leave") -> Leaving
            text.contains("v\u1ec1 nh\u00e0") || text.contains("home") || text.contains("arrive") -> Coming
            else -> Icons.Outlined.AutoAwesome
        }
    }
}
