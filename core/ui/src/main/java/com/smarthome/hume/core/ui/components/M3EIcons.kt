package com.smarthome.hume.core.ui.components

/**
 * Icon glyph dung chung — Material Symbols Rounded, da pin wght=200 / FILL=0
 * (net manh kieu Gemini, giong he t .ms trong demo HTML).
 * Moi entry la mot glyph PUA trong font res/font/material_symbols_rounded.ttf,
 * dung voi [MsIcon]. KHONG dung Icons.Outlined truc tiep nua
 * (material-icons-extended ve net day hon, khac style HTML).
 */
object M3EIcons {
    val Light = Ms.lightbulb
    val Solar = Ms.wb_sunny
    val Battery = Ms.battery_charging_full
    val Power = Ms.bolt
    val Plug = Ms.power
    val Home = Ms.home
    val Climate = Ms.ac_unit
    val Check = Ms.check
    val ChevronDown = Ms.expand_more
    val BatteryFull = Ms.battery_full
    val SolarPower = Ms.solar_power
    val ElectricMeter = Ms.electric_meter
    val Door = Ms.door_front
    val Motion = Ms.sensors
    val Presence = Ms.person_search
    val Smoke = Ms.smoke_free
    val Leak = Ms.water_drop
    val Videocam = Ms.videocam
    val Rec = Ms.fiber_manual_record
    val Mic = Ms.mic
    val PhotoCamera = Ms.photo_camera
    val Fullscreen = Ms.fullscreen
    val PlayCircle = Ms.play_circle
    val Lock = Ms.lock
    val Shield = Ms.shield
    val Close = Ms.close
    val FlightTakeoff = Ms.flight_takeoff
    val Bedtime = Ms.bedtime
    val PowerSettingsNew = Ms.power_settings_new
    val AutoAwesome = Ms.auto_awesome
    val Add = Ms.add
    val Search = Ms.search
    val Bell = Ms.notifications
    val Person = Ms.person

    fun room(key: String): String = when (key) {
        "bed" -> Ms.bed
        "child" -> Ms.child_care
        "sparkles" -> Ms.auto_awesome
        "sofa" -> Ms.weekend
        "bath" -> Ms.bathtub
        "kitchen" -> Ms.soup_kitchen
        "washer" -> Ms.local_laundry_service
        "hallway" -> Ms.stairs
        else -> Ms.home
    }

    fun device(key: String): String = when (key) {
        "sun" -> Solar
        "plug" -> Plug
        "house" -> Home
        "desk" -> Ms.desk
        "door" -> Ms.meeting_room
        "snowflake" -> Climate
        "fire" -> Ms.whatshot
        "bulb", "lightbulb" -> Light
        "switch" -> Power
        else -> Power
    }
}
