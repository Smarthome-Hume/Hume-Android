package com.smarthome.hume.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bathtub
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.FiberManualRecord
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.Desk
import androidx.compose.material.icons.outlined.ElectricMeter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocalLaundryService
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.SoupKitchen
import androidx.compose.material.icons.outlined.SolarPower
import androidx.compose.material.icons.outlined.Stairs
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Shield
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
    val Check = Icons.Outlined.Check
    val ChevronDown = Icons.Outlined.ExpandMore
    val BatteryFull = Icons.Outlined.BatteryFull
    val SolarPower = Icons.Outlined.SolarPower
    val ElectricMeter = Icons.Outlined.ElectricMeter
    val Door = Icons.Outlined.MeetingRoom
    val Motion = Icons.Outlined.DirectionsWalk
    val Presence = Icons.Outlined.PersonSearch
    val Smoke = Icons.Outlined.Whatshot
    val Leak = Icons.Outlined.WaterDrop
    val Videocam = Icons.Outlined.Videocam
    val Rec = Icons.Outlined.FiberManualRecord
    val Mic = Icons.Outlined.Mic
    val PhotoCamera = Icons.Outlined.PhotoCamera
    val Fullscreen = Icons.Outlined.Fullscreen
    val PlayCircle = Icons.Outlined.PlayCircle
    val Lock = Icons.Outlined.Lock
    val Shield = Icons.Outlined.Shield
    val Close = Icons.Outlined.Close
    val FlightTakeoff = Icons.Outlined.FlightTakeoff
    val Bedtime = Icons.Outlined.Bedtime
    val PowerSettingsNew = Icons.Outlined.PowerSettingsNew
    val AutoAwesome = Icons.Outlined.AutoAwesome
    val Add = Icons.Outlined.Add
    val Search = Icons.Outlined.Search
    val Bell = Icons.Outlined.Notifications
    val Person = Icons.Outlined.Person

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
