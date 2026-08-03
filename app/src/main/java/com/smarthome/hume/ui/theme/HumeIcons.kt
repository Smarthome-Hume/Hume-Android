package com.smarthome.hume.ui.theme

import androidx.compose.ui.graphics.vector.ImageVector
import com.smarthome.hume.core.model.HumeTab

/*
 * TOAN BO icon lay tu ban HTML (Phosphor), khong con dung Material Icons.
 * Xem HumePhosphorIcons.kt: khung 256, ve bang net.
 *
 * Navbar: tab DANG CHON dung ban TO DAC (ph-*-fill), tab con lai giu net mong
 * - dung cach One UI / iOS 26 phan biet tab active.
 */
object HumeIcons {
    val Light = Ph.Lightbulb
    val Temperature = Ph.Thermometer
    val Humidity = Ph.Drop
    val Climate = Ph.Snowflake
    val Door = Ph.DoorOpen
    val DoorClosed = Ph.Door
    val Alarm = Ph.Shield
    val AlarmOk = Ph.ShieldCheck
    val Bell = Ph.Bell
    val Night = Ph.Moon
    val Solar = Ph.Sun
    val Battery = Ph.Battery
    val Desk = Ph.Desk
    val Sunrise = Ph.SunHorizon
    val Leaving = Ph.SignOut
    val Coming = Ph.House
    val Power = Ph.Lightning
    val Plug = Ph.Plug
    val House = Ph.House

    fun room(key: String): ImageVector = when (key) {
        "bed" -> Ph.Bed
        "child" -> Ph.Baby
        "sparkles" -> Ph.Sparkle
        "sofa" -> Ph.Couch
        "bath" -> Ph.Bathtub
        "kitchen" -> Ph.CookingPot
        "washer" -> Ph.Washer
        "hallway" -> Ph.Stairs
        else -> Ph.House
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
        "fire" -> Ph.Fire
        "cooking" -> Ph.CookingPot
        "dishwasher", "washer", "dryer" -> Ph.Washer
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

    /** Tab DANG CHON: icon to dac. */
    fun tab(tab: HumeTab): ImageVector = when (tab) {
        HumeTab.Home -> Ph.HouseFill
        HumeTab.Energy -> Ph.LightningFill
        HumeTab.Security -> Ph.ShieldFill
        HumeTab.Profile -> Ph.UserFill
        HumeTab.AI -> Ph.SparkleFill
    }

    /** Tab khong chon: net mong. */
    fun tabOutline(tab: HumeTab): ImageVector = when (tab) {
        HumeTab.Home -> Ph.House
        HumeTab.Energy -> Ph.Lightning
        HumeTab.Security -> Ph.Shield
        HumeTab.Profile -> Ph.User
        HumeTab.AI -> Ph.Sparkle
    }

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
            else -> Ph.Sparkle
        }
    }
}
