package com.smarthome.hume.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Icon registry dung chung. Phan lon entry la glyph PUA trong font
 * Material Symbols Rounded da pin; Download va Share dung Material vector
 * vi hai glyph nay khong co trong font subset hien tai.
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
    val ChevronRight = Ms.chevron_right
    val Fab = Ms.power_settings_new
    val BatteryFull = Ms.battery_full
    /** Icon pin theo muc % — dung thay cho Battery/BatteryFull tinh. */
    fun batteryLevel(soc: Int): String = when {
        soc <= 10 -> Ms.battery_alert
        soc <= 20 -> Ms.battery_1_bar
        soc <= 35 -> Ms.battery_2_bar
        soc <= 50 -> Ms.battery_3_bar
        soc <= 65 -> Ms.battery_4_bar
        soc <= 80 -> Ms.battery_5_bar
        soc <= 95 -> Ms.battery_6_bar
        else -> Ms.battery_full
    }
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
    val Download = Icons.Outlined.FileDownload
    val Share = Icons.Outlined.Share
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
        "snowflake" -> Ms.snowflake
        "fire" -> Ms.whatshot
        "bulb", "lightbulb", "light" -> Light
        "switch" -> Power
        // Thiet bi theo ten (2026-09-30): map key cau hinh sang glyph Ms co san.
        "washer", "dryer" -> Ms.local_laundry_service
        "cooking", "dishwasher" -> Ms.soup_kitchen
        "stairs" -> Ms.stairs
        else -> Power
    }

    /**
     * Icon vector tu ve (phong cach Material Symbols outlined) cho cac thiet bi
     * khong co glyph trong font subset: tu lanh, quat, tivi.
     * Tra null neu key da duoc [device] xu ly bang font.
     */
    fun deviceVector(key: String): ImageVector? = when (key) {
        "fridge" -> Fridge
        "fan" -> Fan
        "tv" -> Tv
        else -> null
    }

    /** Tu lanh 2 canh — stroke 2dp, dau tron, viewport 24. */
    val Fridge: ImageVector by lazy {
        ImageVector.Builder("fridge", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Than tu
                moveTo(8f, 2f); horizontalLineTo(16f)
                arcTo(2f, 2f, 0f, false, true, 18f, 4f)
                verticalLineTo(20f)
                arcTo(2f, 2f, 0f, false, true, 16f, 22f)
                horizontalLineTo(8f)
                arcTo(2f, 2f, 0f, false, true, 6f, 20f)
                verticalLineTo(4f)
                arcTo(2f, 2f, 0f, false, true, 8f, 2f)
                close()
                // Ngon ngang giua 2 canh
                moveTo(6f, 10f); horizontalLineTo(18f)
                // Tay nam canh tren + canh duoi
                moveTo(15f, 4.8f); verticalLineTo(7.2f)
                moveTo(15f, 12.8f); verticalLineTo(16f)
            }
        }.build()
    }

    /** Quat 3 canh — vong ngoai + canh cong + truc giua. */
    val Fan: ImageVector by lazy {
        ImageVector.Builder("fan", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Vong ngoai
                moveTo(12f, 3.5f)
                arcTo(8.5f, 8.5f, 0f, true, true, 11.9f, 3.5f)
                close()
                // 3 canh cong tu truc
                moveTo(12f, 12f); curveTo(14f, 9.5f, 16f, 7.5f, 18.2f, 6.8f)
                moveTo(12f, 12f); curveTo(9.2f, 13.2f, 6.5f, 14f, 4.6f, 13f)
                moveTo(12f, 12f); curveTo(12.5f, 14.8f, 12.8f, 17.2f, 12f, 19.5f)
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 10.8f)
                arcTo(1.2f, 1.2f, 0f, true, true, 11.9f, 10.8f)
                close()
            }
        }.build()
    }

    /** Tivi — man hinh bo goc + chan de. */
    val Tv: ImageVector by lazy {
        ImageVector.Builder("tv", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Man hinh
                moveTo(5f, 5f); horizontalLineTo(19f)
                arcTo(2f, 2f, 0f, false, true, 21f, 7f)
                verticalLineTo(13f)
                arcTo(2f, 2f, 0f, false, true, 19f, 15f)
                horizontalLineTo(5f)
                arcTo(2f, 2f, 0f, false, true, 3f, 13f)
                verticalLineTo(7f)
                arcTo(2f, 2f, 0f, false, true, 5f, 5f)
                close()
                // Co + de chan
                moveTo(12f, 15f); verticalLineTo(18.5f)
                moveTo(8.5f, 20.5f); horizontalLineTo(15.5f)
            }
        }.build()
    }
}

/**
 * Icon thiet bi theo [iconKey]: uu tien vector tu ve (tu lanh/quat/tivi),
 * fallback sang glyph font Material Symbols. Dung thay cho
 * `MsIcon(M3EIcons.device(key), ...)` de icon khop ten thiet bi.
 */
@Composable
fun DeviceIcon(
    iconKey: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = androidx.compose.material3.LocalContentColor.current,
) {
    val vector = M3EIcons.deviceVector(iconKey)
    if (vector != null) {
        androidx.compose.material3.Icon(
            imageVector = vector,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint,
        )
    } else {
        MsIcon(
            M3EIcons.device(iconKey), contentDescription,
            tint = tint,
            modifier = modifier,
        )
    }
}
