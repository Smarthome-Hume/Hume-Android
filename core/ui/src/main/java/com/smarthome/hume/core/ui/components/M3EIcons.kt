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
    val Light: ImageVector get() = LightCeiling
    val Solar = Ms.wb_sunny
    val Battery = Ms.battery_charging_full
    val Power: ImageVector get() = BoltFa
    val Plug: ImageVector get() = PlugFa
    val Home = Ms.home
    val Climate = Ms.ac_unit
    val Check = Ms.check
    val ChevronDown = Ms.expand_more
    val ChevronRight = Ms.chevron_right
    val Fab = Ms.power_settings_new
    val BatteryFull: ImageVector get() = BatteryFullFa
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
    /** Vector chuong rong de dung truc tiep voi Icon() (2026-09-30). */
    val BellVector: ImageVector get() = BellWide
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
    // ---- Custom icons tu file SVG (Font Awesome fill) ----

    /** Den tran - dung cho tat ca icon den (Font Awesome, fill dac). */
    val LightCeiling: ImageVector by lazy {
        ImageVector.Builder("lightceiling", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(23.782f, 14.932f)
                curveTo(22.131f, 10.206f, 17.439f, 6.743f, 12.5f, 6.522f)
                verticalLineTo(0.5f)
                curveTo(12.5f, 0.224f, 12.276f, 0f, 12f, 0f)
                curveTo(11.724f, 0f, 11.5f, 0.224f, 11.5f, 0.5f)
                verticalLineTo(6.521f)
                curveTo(6.561f, 6.743f, 1.869f, 10.205f, 0.218f, 14.932f)
                curveTo(-0.197f, 16.12f, -0.019f, 17.388f, 0.706f, 18.41f)
                curveTo(1.424f, 19.421f, 2.546f, 20f, 3.786f, 20f)
                horizontalLineTo(8f)
                curveTo(8f, 22.206f, 9.794f, 24f, 12f, 24f)
                curveTo(14.206f, 24f, 16f, 22.206f, 16f, 20f)
                horizontalLineTo(20.214f)
                curveTo(21.454f, 20f, 22.576f, 19.42f, 23.294f, 18.41f)
                curveTo(24.02f, 17.388f, 24.197f, 16.12f, 23.782f, 14.932f)
                moveTo(12f, 23f)
                curveTo(10.346f, 23f, 9f, 21.654f, 9f, 20f)
                horizontalLineTo(15f)
                curveTo(15f, 21.654f, 13.654f, 23f, 12f, 23f)
                moveTo(22.479f, 17.831f)
                curveTo(21.952f, 18.574f, 21.126f, 19f, 20.214f, 19f)
                horizontalLineTo(3.786f)
                curveTo(2.874f, 19f, 2.049f, 18.574f, 1.521f, 17.831f)
                curveTo(0.985f, 17.077f, 0.854f, 16.14f, 1.161f, 15.262f)
                curveTo(2.732f, 10.764f, 7.291f, 7.5f, 12f, 7.5f)
                curveTo(16.709f, 7.5f, 21.268f, 10.764f, 22.839f, 15.262f)
                curveTo(23.146f, 16.14f, 23.015f, 17.076f, 22.479f, 17.831f)
            }
        }.build()
    }

    /** Khien gach - trang thai Tat (Font Awesome, fill dac). */
    val ShieldSlash: ImageVector by lazy {
        ImageVector.Builder("shieldslash", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(16.268f, 20.713f)
                curveTo(16.569f, 21.176f, 16.439f, 21.795f, 15.976f, 22.097f)
                curveTo(14.711f, 22.921f, 13.547f, 23.456f, 12.794f, 23.76f)
                curveTo(12.794f, 23.76f, 12.364f, 24f, 12.002f, 24f)
                curveTo(11.64f, 24f, 11.152f, 23.693f, 11.152f, 23.693f)
                curveTo(9.005f, 22.619f, 1.999f, 18.605f, 1.999f, 12.043f)
                verticalLineTo(8.493f)
                curveTo(1.999f, 7.94f, 2.447f, 7.493f, 2.999f, 7.493f)
                curveTo(3.551f, 7.493f, 3.999f, 7.94f, 3.999f, 8.493f)
                verticalLineTo(12.043f)
                curveTo(3.999f, 17.459f, 10.158f, 20.96f, 12.046f, 21.904f)
                curveTo(12.595f, 21.683f, 13.694f, 21.196f, 14.883f, 20.421f)
                curveTo(15.345f, 20.12f, 15.965f, 20.251f, 16.267f, 20.713f)
                moveTo(23.706f, 23.707f)
                curveTo(23.511f, 23.902f, 23.255f, 24f, 22.999f, 24f)
                curveTo(22.743f, 24f, 22.487f, 23.902f, 22.292f, 23.707f)
                lineTo(0.293f, 1.707f)
                curveTo(-0.098f, 1.316f, -0.098f, 0.684f, 0.293f, 0.293f)
                curveTo(0.684f, -0.098f, 1.316f, -0.098f, 1.707f, 0.293f)
                lineTo(4.167f, 2.753f)
                curveTo(4.555f, 2.487f, 4.98f, 2.275f, 5.426f, 2.126f)
                lineTo(11.685f, 0.051f)
                curveTo(11.89f, -0.017f, 12.11f, -0.017f, 12.315f, 0.051f)
                lineTo(18.574f, 2.126f)
                curveTo(20.623f, 2.806f, 22f, 4.713f, 22f, 6.872f)
                verticalLineTo(12.043f)
                curveTo(22f, 14.392f, 21.29f, 16.552f, 19.889f, 18.475f)
                lineTo(23.707f, 22.293f)
                curveTo(24.098f, 22.684f, 24.098f, 23.316f, 23.707f, 23.707f)
                moveTo(5.622f, 4.207f)
                lineTo(18.454f, 17.039f)
                curveTo(19.481f, 15.528f, 20.001f, 13.852f, 20.001f, 12.043f)
                verticalLineTo(6.872f)
                curveTo(20.001f, 5.577f, 19.175f, 4.433f, 17.945f, 4.024f)
                lineTo(12.001f, 2.053f)
                lineTo(6.057f, 4.024f)
                curveTo(5.906f, 4.074f, 5.761f, 4.135f, 5.622f, 4.207f)
            }
        }.build()
    }

    /** Khien tick - trang thai Bat (Font Awesome, fill dac). */
    val ShieldCheck: ImageVector by lazy {
        ImageVector.Builder("shieldcheck", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(11.948f, 24.009f)
                lineTo(11.594f, 23.852f)
                curveTo(11.2f, 23.679f, 2f, 19.524f, 2f, 12f)
                verticalLineTo(5.476f)
                arcTo(2.983f, 2.983f, 0f, false, true, 4.051f, 2.644f)
                lineTo(12f, 0.009f)
                lineTo(19.949f, 2.644f)
                arcTo(2.983f, 2.983f, 0f, false, true, 22f, 5.476f)
                verticalLineTo(12f)
                curveTo(22f, 20.577f, 12.712f, 23.755f, 12.316f, 23.887f)
                moveTo(12f, 2.106f)
                lineTo(4.684f, 4.532f)
                arcTo(0.992f, 0.992f, 0f, false, false, 4f, 5.476f)
                verticalLineTo(12f)
                curveTo(4f, 17.494f, 10.44f, 21.058f, 12.047f, 21.861f)
                curveTo(13.651f, 21.216f, 20f, 18.263f, 20f, 12f)
                verticalLineTo(5.476f)
                arcTo(0.992f, 0.992f, 0f, false, false, 19.316f, 4.532f)
                moveTo(11.111f, 14.542f)
                horizontalLineTo(11.078f)
                arcTo(1.872f, 1.872f, 0f, false, true, 9.733f, 13.942f)
                lineTo(7.427f, 11.542f)
                lineTo(8.868f, 10.16f)
                lineTo(11.112f, 12.5f)
                lineTo(16.293f, 7.319f)
                lineTo(17.707f, 8.733f)
                lineTo(12.446f, 13.994f)
                arcTo(1.873f, 1.873f, 0f, false, true, 11.111f, 14.542f)
            }
        }.build()
    }

    /** Quat (Font Awesome, fill dac). */
    val FanFa: ImageVector by lazy {
        ImageVector.Builder("fanfa", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(22.941f, 9.755f)
                curveTo(22.203f, 8.932f, 21.187f, 8.445f, 20.083f, 8.386f)
                curveTo(18.159f, 8.282f, 15.942f, 8.914f, 14.405f, 9.469f)
                curveTo(14.219f, 9.292f, 14.012f, 9.141f, 13.791f, 9.009f)
                curveTo(15.035f, 7.706f, 16.56f, 6.108f, 16.654f, 4.364f)
                curveTo(16.714f, 3.26f, 16.34f, 2.198f, 15.601f, 1.375f)
                curveTo(14.863f, 0.552f, 13.848f, 0.066f, 12.744f, 0.006f)
                curveTo(11.632f, -0.055f, 10.578f, 0.32f, 9.755f, 1.058f)
                curveTo(8.933f, 1.797f, 8.446f, 2.811f, 8.387f, 3.915f)
                curveTo(8.281f, 5.839f, 8.914f, 8.057f, 9.47f, 9.594f)
                curveTo(9.293f, 9.78f, 9.142f, 9.987f, 9.009f, 10.208f)
                curveTo(7.706f, 8.964f, 6.107f, 7.439f, 4.364f, 7.345f)
                curveTo(3.252f, 7.284f, 2.198f, 7.659f, 1.375f, 8.397f)
                curveTo(0.553f, 9.137f, 0.066f, 10.152f, 0.007f, 11.256f)
                curveTo(-0.054f, 12.36f, 0.32f, 13.422f, 1.059f, 14.245f)
                curveTo(1.798f, 15.068f, 2.851f, 15.625f, 4.347f, 15.625f)
                curveTo(6.165f, 15.625f, 8.171f, 15.045f, 9.595f, 14.53f)
                curveTo(9.781f, 14.707f, 9.988f, 14.858f, 10.209f, 14.991f)
                curveTo(8.965f, 16.294f, 7.44f, 17.892f, 7.346f, 19.636f)
                curveTo(7.286f, 20.74f, 7.66f, 21.802f, 8.399f, 22.625f)
                curveTo(9.137f, 23.448f, 10.153f, 23.935f, 11.257f, 23.994f)
                curveTo(11.332f, 23.998f, 11.407f, 24f, 11.483f, 24f)
                curveTo(13.664f, 24f, 15.495f, 22.287f, 15.614f, 20.084f)
                curveTo(15.72f, 18.16f, 15.087f, 15.942f, 14.531f, 14.405f)
                curveTo(14.708f, 14.219f, 14.859f, 14.012f, 14.992f, 13.791f)
                curveTo(16.295f, 15.035f, 17.894f, 16.56f, 19.638f, 16.654f)
                curveTo(19.713f, 16.658f, 19.788f, 16.66f, 19.864f, 16.66f)
                curveTo(22.045f, 16.66f, 23.876f, 14.947f, 23.995f, 12.744f)
                curveTo(24.056f, 11.64f, 23.682f, 10.578f, 22.943f, 9.755f)
                moveTo(4.026f, 13.616f)
                curveTo(3.456f, 13.585f, 2.93f, 13.334f, 2.548f, 12.908f)
                curveTo(2.167f, 12.482f, 1.974f, 11.934f, 2.005f, 11.363f)
                curveTo(2.035f, 10.792f, 2.286f, 10.267f, 2.712f, 9.886f)
                curveTo(3.108f, 9.531f, 3.465f, 9.342f, 4.257f, 9.342f)
                curveTo(5.264f, 9.342f, 6.454f, 10.534f, 7.506f, 11.538f)
                curveTo(7.849f, 11.866f, 8.201f, 12.194f, 8.552f, 12.499f)
                curveTo(8.565f, 12.586f, 8.571f, 12.674f, 8.59f, 12.759f)
                curveTo(7.255f, 13.219f, 5.498f, 13.691f, 4.026f, 13.615f)
                moveTo(11.091f, 2.547f)
                curveTo(11.487f, 2.192f, 11.989f, 2f, 12.517f, 2f)
                curveTo(13.214f, 2f, 13.732f, 2.286f, 14.114f, 2.711f)
                curveTo(14.495f, 3.136f, 14.688f, 3.685f, 14.657f, 4.256f)
                curveTo(14.603f, 5.261f, 13.465f, 6.454f, 12.461f, 7.506f)
                curveTo(12.134f, 7.849f, 11.805f, 8.2f, 11.5f, 8.551f)
                curveTo(11.413f, 8.564f, 11.325f, 8.57f, 11.24f, 8.589f)
                curveTo(10.779f, 7.254f, 10.305f, 5.495f, 10.385f, 4.024f)
                curveTo(10.415f, 3.453f, 10.666f, 2.928f, 11.092f, 2.547f)
                moveTo(13.501f, 12f)
                curveTo(13.501f, 12.827f, 12.828f, 13.5f, 12.001f, 13.5f)
                curveTo(11.174f, 13.5f, 10.501f, 12.827f, 10.501f, 12f)
                curveTo(10.501f, 11.173f, 11.174f, 10.5f, 12.001f, 10.5f)
                curveTo(12.828f, 10.5f, 13.501f, 11.173f, 13.501f, 12f)
                moveTo(12.91f, 21.453f)
                curveTo(12.484f, 21.835f, 11.929f, 22.025f, 11.365f, 21.997f)
                curveTo(10.795f, 21.966f, 10.269f, 21.715f, 9.887f, 21.289f)
                curveTo(9.506f, 20.864f, 9.313f, 20.315f, 9.344f, 19.744f)
                curveTo(9.398f, 18.739f, 10.536f, 17.546f, 11.54f, 16.494f)
                curveTo(11.867f, 16.151f, 12.196f, 15.8f, 12.501f, 15.449f)
                curveTo(12.588f, 15.436f, 12.676f, 15.43f, 12.761f, 15.411f)
                curveTo(13.222f, 16.746f, 13.696f, 18.505f, 13.616f, 19.976f)
                curveTo(13.586f, 20.547f, 13.335f, 21.071f, 12.909f, 21.453f)
                moveTo(19.744f, 14.657f)
                curveTo(18.738f, 14.603f, 17.547f, 13.465f, 16.495f, 12.461f)
                curveTo(16.152f, 12.133f, 15.8f, 11.805f, 15.449f, 11.5f)
                curveTo(15.436f, 11.413f, 15.43f, 11.325f, 15.411f, 11.24f)
                curveTo(16.746f, 10.779f, 18.499f, 10.304f, 19.975f, 10.384f)
                curveTo(20.545f, 10.415f, 21.071f, 10.666f, 21.453f, 11.092f)
                curveTo(21.834f, 11.518f, 22.027f, 12.066f, 21.996f, 12.637f)
                curveTo(21.933f, 13.816f, 20.91f, 14.716f, 19.744f, 14.658f)
            }
        }.build()
    }

    /** TV (Font Awesome, fill dac). */
    val TvRetro: ImageVector by lazy {
        ImageVector.Builder("tvretro", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 6f)
                horizontalLineTo(14.135f)
                lineTo(17.768f, 1.64f)
                curveTo(18.122f, 1.216f, 18.064f, 0.585f, 17.64f, 0.232f)
                curveTo(17.216f, -0.121f, 16.586f, -0.064f, 16.232f, 0.36f)
                lineTo(12f, 5.438f)
                lineTo(7.768f, 0.36f)
                curveTo(7.414f, -0.064f, 6.784f, -0.122f, 6.36f, 0.232f)
                curveTo(5.936f, 0.586f, 5.879f, 1.216f, 6.232f, 1.64f)
                lineTo(9.865f, 6f)
                horizontalLineTo(5f)
                curveTo(2.243f, 6f, 0f, 8.243f, 0f, 11f)
                verticalLineTo(19f)
                curveTo(0f, 21.757f, 2.243f, 24f, 5f, 24f)
                horizontalLineTo(19f)
                curveTo(21.757f, 24f, 24f, 21.757f, 24f, 19f)
                verticalLineTo(11f)
                curveTo(24f, 8.243f, 21.757f, 6f, 19f, 6f)
                moveTo(22f, 19f)
                curveTo(22f, 20.654f, 20.654f, 22f, 19f, 22f)
                horizontalLineTo(5f)
                curveTo(3.346f, 22f, 2f, 20.654f, 2f, 19f)
                verticalLineTo(11f)
                curveTo(2f, 9.346f, 3.346f, 8f, 5f, 8f)
                horizontalLineTo(19f)
                curveTo(20.654f, 8f, 22f, 9.346f, 22f, 11f)
                verticalLineTo(19f)
                moveTo(13f, 10f)
                horizontalLineTo(6f)
                curveTo(4.897f, 10f, 4f, 10.897f, 4f, 12f)
                verticalLineTo(18f)
                curveTo(4f, 19.103f, 4.897f, 20f, 6f, 20f)
                horizontalLineTo(13f)
                curveTo(14.103f, 20f, 15f, 19.103f, 15f, 18f)
                verticalLineTo(12f)
                curveTo(15f, 10.897f, 14.103f, 10f, 13f, 10f)
                moveTo(6f, 18f)
                verticalLineTo(12f)
                horizontalLineTo(13f)
                verticalLineTo(18f)
                curveTo(13f, 18f, 6f, 18f, 6f, 18f)
                moveTo(20f, 12.5f)
                curveTo(20f, 13.328f, 19.328f, 14f, 18.5f, 14f)
                curveTo(17.672f, 14f, 17f, 13.328f, 17f, 12.5f)
                curveTo(17f, 11.672f, 17.672f, 11f, 18.5f, 11f)
                curveTo(19.328f, 11f, 20f, 11.672f, 20f, 12.5f)
                moveTo(20f, 17.5f)
                curveTo(20f, 18.328f, 19.328f, 19f, 18.5f, 19f)
                curveTo(17.672f, 19f, 17f, 18.328f, 17f, 17.5f)
                curveTo(17f, 16.672f, 17.672f, 16f, 18.5f, 16f)
                curveTo(19.328f, 16f, 20f, 16.672f, 20f, 17.5f)
            }
        }.build()
    }

    /** O cam (Font Awesome, fill dac). */
    val PlugFa: ImageVector by lazy {
        ImageVector.Builder("plugfa", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(22f, 7f)
                horizontalLineTo(17f)
                verticalLineTo(1f)
                curveTo(17f, 0.448f, 16.553f, 0f, 16f, 0f)
                curveTo(15.447f, 0f, 15f, 0.448f, 15f, 1f)
                verticalLineTo(7f)
                horizontalLineTo(9f)
                verticalLineTo(1f)
                curveTo(9f, 0.448f, 8.553f, 0f, 8f, 0f)
                curveTo(7.447f, 0f, 7f, 0.448f, 7f, 1f)
                verticalLineTo(7f)
                horizontalLineTo(2f)
                curveTo(1.447f, 7f, 1f, 7.448f, 1f, 8f)
                curveTo(1f, 8.552f, 1.447f, 9f, 2f, 9f)
                horizontalLineTo(3f)
                verticalLineTo(12f)
                curveTo(3f, 16.624f, 6.506f, 20.445f, 11f, 20.944f)
                verticalLineTo(23f)
                curveTo(11f, 23.552f, 11.447f, 24f, 12f, 24f)
                curveTo(12.553f, 24f, 13f, 23.552f, 13f, 23f)
                verticalLineTo(20.944f)
                curveTo(17.494f, 20.445f, 21f, 16.624f, 21f, 12f)
                verticalLineTo(9f)
                horizontalLineTo(22f)
                curveTo(22.553f, 9f, 23f, 8.552f, 23f, 8f)
                curveTo(23f, 7.448f, 22.553f, 7f, 22f, 7f)
                moveTo(19f, 12f)
                curveTo(19f, 15.86f, 15.859f, 19f, 12f, 19f)
                curveTo(8.141f, 19f, 5f, 15.86f, 5f, 12f)
                verticalLineTo(9f)
                horizontalLineTo(19f)
                verticalLineTo(12f)
            }
        }.build()
    }

    /** Cong suat (Font Awesome, fill dac). */
    val BoltFa: ImageVector by lazy {
        ImageVector.Builder("boltfa", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(11.24f, 24f)
                arcTo(2.262f, 2.262f, 0f, false, true, 10.292f, 23.788f)
                arcTo(2.18f, 2.18f, 0f, false, true, 9.092f, 21.166f)
                lineTo(10.653f, 16f)
                horizontalLineTo(6.975f)
                arcTo(3f, 3f, 0f, false, true, 4.1f, 12.131f)
                lineTo(7.124f, 2.131f)
                arcTo(2.983f, 2.983f, 0f, false, true, 10f, 0f)
                horizontalLineTo(13.693f)
                arcTo(2.6f, 2.6f, 0f, false, true, 16.126f, 3.511f)
                lineTo(14.443f, 8f)
                horizontalLineTo(17f)
                arcTo(3f, 3f, 0f, false, true, 19.483f, 12.684f)
                lineTo(13.083f, 22.984f)
                arcTo(2.2f, 2.2f, 0f, false, true, 11.24f, 24f)
                moveTo(10f, 2f)
                arcTo(1f, 1f, 0f, false, false, 9.042f, 2.71f)
                lineTo(6.018f, 12.71f)
                arcTo(1f, 1f, 0f, false, false, 6.975f, 14f)
                horizontalLineTo(12f)
                arcTo(1f, 1f, 0f, false, true, 12.957f, 15.29f)
                lineTo(11.01f, 21.732f)
                arcTo(0.183f, 0.183f, 0f, false, false, 11.131f, 21.973f)
                arcTo(0.188f, 0.188f, 0f, false, false, 11.4f, 21.9f)
                lineTo(17.8f, 11.6f)
                arcTo(1f, 1f, 0f, false, false, 17.878f, 10.537f)
                arcTo(0.979f, 0.979f, 0f, false, false, 17f, 10f)
                horizontalLineTo(13f)
                arcTo(1f, 1f, 0f, false, true, 12.063f, 8.649f)
                lineTo(14.253f, 2.809f)
                arcTo(0.6f, 0.6f, 0f, false, false, 13.693f, 2f)
            }
        }.build()
    }

    /** Pin day (Font Awesome, fill dac). */
    val BatteryFullFa: ImageVector by lazy {
        ImageVector.Builder("batteryfullfa", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(23f, 8f)
                horizontalLineTo(21.899f)
                curveTo(21.434f, 5.721f, 19.414f, 4f, 17f, 4f)
                horizontalLineTo(5f)
                curveTo(2.243f, 4f, 0f, 6.243f, 0f, 9f)
                verticalLineTo(15f)
                curveTo(0f, 17.757f, 2.243f, 20f, 5f, 20f)
                horizontalLineTo(17f)
                curveTo(19.414f, 20f, 21.435f, 18.279f, 21.899f, 16f)
                horizontalLineTo(23f)
                curveTo(23.552f, 16f, 24f, 15.552f, 24f, 15f)
                verticalLineTo(9f)
                curveTo(24f, 8.448f, 23.552f, 8f, 23f, 8f)
                moveTo(17f, 18f)
                horizontalLineTo(5f)
                curveTo(3.346f, 18f, 2f, 16.654f, 2f, 15f)
                verticalLineTo(9f)
                curveTo(2f, 7.346f, 3.346f, 6f, 5f, 6f)
                horizontalLineTo(17f)
                curveTo(18.654f, 6f, 20f, 7.346f, 20f, 9f)
                verticalLineTo(15f)
                curveTo(20f, 16.654f, 18.654f, 18f, 17f, 18f)
                moveTo(17f, 8f)
                horizontalLineTo(5f)
                curveTo(4.447f, 8f, 4f, 8.448f, 4f, 9f)
                verticalLineTo(15f)
                curveTo(4f, 15.552f, 4.447f, 16f, 5f, 16f)
                horizontalLineTo(17f)
                curveTo(17.553f, 16f, 18f, 15.552f, 18f, 15f)
                verticalLineTo(9f)
                curveTo(18f, 8.448f, 17.553f, 8f, 17f, 8f)
            }
        }.build()
    }

    /** Cua mo (Font Awesome, fill dac). */
    val DoorOpen: ImageVector by lazy {
        ImageVector.Builder("dooropen", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(23f, 22f)
                horizontalLineTo(20f)
                verticalLineTo(6f)
                curveTo(20f, 3.794f, 18.206f, 2f, 16f, 2f)
                horizontalLineTo(15.465f)
                curveTo(15.227f, 1.589f, 14.915f, 1.218f, 14.536f, 0.908f)
                curveTo(13.606f, 0.146f, 12.396f, -0.157f, 11.216f, 0.079f)
                lineTo(8.019f, 0.718f)
                curveTo(5.69f, 1.184f, 4f, 3.246f, 4f, 5.621f)
                verticalLineTo(22f)
                horizontalLineTo(1f)
                curveTo(0.448f, 22f, 0f, 22.447f, 0f, 23f)
                curveTo(0f, 23.553f, 0.448f, 24f, 1f, 24f)
                horizontalLineTo(23f)
                curveTo(23.552f, 24f, 24f, 23.553f, 24f, 23f)
                curveTo(24f, 22.447f, 23.552f, 22f, 23f, 22f)
                moveTo(16f, 4f)
                curveTo(17.103f, 4f, 18f, 4.897f, 18f, 6f)
                verticalLineTo(22f)
                horizontalLineTo(16f)
                verticalLineTo(4f)
                horizontalLineTo(16f)
                moveTo(6f, 5.621f)
                curveTo(6f, 4.195f, 7.014f, 2.958f, 8.411f, 2.679f)
                lineTo(11.607f, 2.04f)
                curveTo(12.197f, 1.921f, 12.802f, 2.073f, 13.268f, 2.455f)
                curveTo(13.733f, 2.836f, 13.999f, 3.399f, 13.999f, 4.001f)
                verticalLineTo(22f)
                horizontalLineTo(6f)
                verticalLineTo(5.621f)
                moveTo(12f, 12.5f)
                curveTo(12f, 13.328f, 11.328f, 14f, 10.5f, 14f)
                curveTo(9.672f, 14f, 9f, 13.328f, 9f, 12.5f)
                curveTo(9f, 11.672f, 9.672f, 11f, 10.5f, 11f)
                curveTo(11.328f, 11f, 12f, 11.672f, 12f, 12.5f)
            }
        }.build()
    }

    /** Cua dong (Font Awesome, fill dac). */
    val DoorClosed: ImageVector by lazy {
        ImageVector.Builder("doorclosed", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(23f, 22f)
                horizontalLineTo(20f)
                verticalLineTo(5f)
                curveTo(20f, 2.243f, 17.757f, 0f, 15f, 0f)
                horizontalLineTo(9f)
                curveTo(6.243f, 0f, 4f, 2.243f, 4f, 5f)
                verticalLineTo(22f)
                horizontalLineTo(1f)
                curveTo(0.447f, 22f, 0f, 22.448f, 0f, 23f)
                curveTo(0f, 23.552f, 0.447f, 24f, 1f, 24f)
                horizontalLineTo(23f)
                curveTo(23.553f, 24f, 24f, 23.552f, 24f, 23f)
                curveTo(24f, 22.448f, 23.553f, 22f, 23f, 22f)
                moveTo(6f, 5f)
                curveTo(6f, 3.346f, 7.346f, 2f, 9f, 2f)
                horizontalLineTo(15f)
                curveTo(16.654f, 2f, 18f, 3.346f, 18f, 5f)
                verticalLineTo(22f)
                horizontalLineTo(6f)
                verticalLineTo(5f)
                moveTo(16f, 12.5f)
                curveTo(16f, 13.328f, 15.328f, 14f, 14.5f, 14f)
                curveTo(13.672f, 14f, 13f, 13.328f, 13f, 12.5f)
                curveTo(13f, 11.672f, 13.672f, 11f, 14.5f, 11f)
                curveTo(15.328f, 11f, 16f, 11.672f, 16f, 12.5f)
            }
        }.build()
    }

    /** Ban dem (Font Awesome, fill dac). */
    val MoonStars: ImageVector by lazy {
        ImageVector.Builder("moonstars", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(13.274f, 5.865f)
                lineTo(14.105f, 6.168f)
                curveTo(14.433f, 6.288f, 14.691f, 6.541f, 14.812f, 6.861f)
                lineTo(15.119f, 7.681f)
                curveTo(15.265f, 8.072f, 15.638f, 8.331f, 16.055f, 8.331f)
                horizontalLineTo(16.055f)
                curveTo(16.472f, 8.331f, 16.845f, 8.073f, 16.992f, 7.682f)
                lineTo(17.299f, 6.864f)
                curveTo(17.421f, 6.542f, 17.679f, 6.288f, 18.007f, 6.169f)
                lineTo(18.838f, 5.866f)
                curveTo(19.233f, 5.722f, 19.495f, 5.346f, 19.495f, 4.927f)
                curveTo(19.495f, 4.508f, 19.232f, 4.132f, 18.838f, 3.988f)
                lineTo(18.007f, 3.685f)
                curveTo(17.679f, 3.565f, 17.421f, 3.312f, 17.3f, 2.991f)
                lineTo(16.992f, 2.171f)
                curveTo(16.846f, 1.78f, 16.472f, 1.522f, 16.055f, 1.522f)
                horizontalLineTo(16.055f)
                curveTo(15.638f, 1.522f, 15.265f, 1.781f, 15.119f, 2.172f)
                lineTo(14.813f, 2.989f)
                curveTo(14.691f, 3.311f, 14.433f, 3.565f, 14.105f, 3.684f)
                lineTo(13.274f, 3.987f)
                curveTo(12.879f, 4.131f, 12.617f, 4.507f, 12.617f, 4.926f)
                curveTo(12.617f, 5.345f, 12.88f, 5.721f, 13.274f, 5.865f)
                moveTo(22.386f, 12.004f)
                curveTo(21.984f, 11.836f, 21.516f, 11.948f, 21.235f, 12.283f)
                curveTo(20.307f, 13.389f, 18.728f, 13.904f, 16.267f, 13.904f)
                curveTo(12.453f, 13.904f, 10.088f, 12.874f, 10.088f, 7.746f)
                curveTo(10.088f, 5.349f, 10.62f, 3.727f, 11.714f, 2.789f)
                curveTo(12.044f, 2.505f, 12.153f, 2.04f, 11.983f, 1.639f)
                curveTo(11.813f, 1.239f, 11.412f, 0.993f, 10.968f, 1.035f)
                curveTo(5.285f, 1.572f, 1f, 6.277f, 1f, 11.977f)
                curveTo(1f, 18.039f, 5.944f, 22.971f, 12.022f, 22.971f)
                curveTo(17.742f, 22.971f, 22.46f, 18.693f, 22.995f, 13.02f)
                curveTo(23.037f, 12.584f, 22.79f, 12.172f, 22.386f, 12.003f)
            }
        }.build()
    }

    /** Cam bien (Font Awesome, fill dac). */
    val SensorFa: ImageVector by lazy {
        ImageVector.Builder("sensorfa", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(11f, 24f)
                horizontalLineTo(5f)
                curveTo(2.243f, 24f, 0f, 21.757f, 0f, 19f)
                verticalLineTo(13f)
                curveTo(0f, 10.243f, 2.243f, 8f, 5f, 8f)
                horizontalLineTo(11f)
                curveTo(13.757f, 8f, 16f, 10.243f, 16f, 13f)
                verticalLineTo(19f)
                curveTo(16f, 21.757f, 13.757f, 24f, 11f, 24f)
                moveTo(5f, 10f)
                curveTo(3.346f, 10f, 2f, 11.346f, 2f, 13f)
                verticalLineTo(19f)
                curveTo(2f, 20.654f, 3.346f, 22f, 5f, 22f)
                horizontalLineTo(11f)
                curveTo(12.654f, 22f, 14f, 20.654f, 14f, 19f)
                verticalLineTo(13f)
                curveTo(14f, 11.346f, 12.654f, 10f, 11f, 10f)
                horizontalLineTo(5f)
                moveTo(24f, 11f)
                curveTo(24f, 4.935f, 19.065f, 0f, 13f, 0f)
                curveTo(12.447f, 0f, 12f, 0.448f, 12f, 1f)
                curveTo(12f, 1.552f, 12.447f, 2f, 13f, 2f)
                curveTo(17.963f, 2f, 22f, 6.038f, 22f, 11f)
                curveTo(22f, 11.552f, 22.447f, 12f, 23f, 12f)
                curveTo(23.553f, 12f, 24f, 11.552f, 24f, 11f)
                moveTo(20f, 11f)
                curveTo(20f, 7.14f, 16.859f, 4f, 13f, 4f)
                curveTo(12.447f, 4f, 12f, 4.448f, 12f, 5f)
                curveTo(12f, 5.552f, 12.447f, 6f, 13f, 6f)
                curveTo(15.757f, 6f, 18f, 8.243f, 18f, 11f)
                curveTo(18f, 11.552f, 18.447f, 12f, 19f, 12f)
                curveTo(19.553f, 12f, 20f, 11.552f, 20f, 11f)
                moveTo(5f, 12f)
                curveTo(4.448f, 12f, 4f, 12.448f, 4f, 13f)
                curveTo(4f, 13.552f, 4.448f, 14f, 5f, 14f)
                curveTo(5.552f, 14f, 6f, 13.552f, 6f, 13f)
                curveTo(6f, 12.448f, 5.552f, 12f, 5f, 12f)
            }
        }.build()
    }

    /** Nhiet do (Font Awesome, fill dac). */
    val Thermometer: ImageVector by lazy {
        ImageVector.Builder("thermometer", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(13f, 14.184f)
                verticalLineTo(8f)
                arcTo(1f, 1f, 0f, false, false, 11f, 8f)
                verticalLineTo(14.184f)
                arcTo(2.994f, 2.994f, 0f, true, false, 13f, 14.184f)
                moveTo(17f, 12.111f)
                verticalLineTo(5f)
                curveTo(16.789f, -1.609f, 7.209f, -1.6f, 7f, 5f)
                verticalLineTo(12.111f)
                arcTo(7f, 7f, 0f, true, false, 17f, 12.111f)
                moveTo(12f, 22f)
                arcTo(5.018f, 5.018f, 0f, false, true, 8.668f, 13.281f)
                arcTo(1f, 1f, 0f, false, false, 9f, 12.537f)
                verticalLineTo(5f)
                arcTo(3f, 3f, 0f, false, true, 15f, 5f)
                verticalLineTo(12.537f)
                arcTo(1f, 1f, 0f, false, false, 15.332f, 13.281f)
                arcTo(5.018f, 5.018f, 0f, false, true, 12f, 22f)
            }
        }.build()
    }

    /** Do am (Font Awesome, fill dac). */
    val Humidity: ImageVector by lazy {
        ImageVector.Builder("humidity", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(10f, 17f)
                arcTo(1f, 1f, 0f, false, true, 9.169f, 15.445f)
                lineTo(13.169f, 9.445f)
                arcTo(1f, 1f, 0f, false, true, 14.833f, 10.555f)
                lineTo(10.833f, 16.555f)
                arcTo(1f, 1f, 0f, false, true, 10f, 17f)
                moveTo(19.779f, 20.778f)
                curveTo(26.637f, 13.442f, 21.298f, 6.257f, 15.214f, 1.178f)
                horizontalLineTo(15.214f)
                arcTo(4.947f, 4.947f, 0f, false, false, 8.788f, 1.178f)
                curveTo(2.706f, 6.231f, -2.63f, 13.491f, 4.222f, 20.778f)
                arcTo(11f, 11f, 0f, false, false, 19.778f, 20.778f)
                moveTo(13.919f, 2.7f)
                horizontalLineTo(13.919f)
                curveTo(18.7f, 6.777f, 24.43f, 12.966f, 18.364f, 19.364f)
                arcTo(9.043f, 9.043f, 0f, false, true, 5.636f, 19.364f)
                curveTo(-0.435f, 12.964f, 5.311f, 6.764f, 10.081f, 2.702f)
                arcTo(2.958f, 2.958f, 0f, false, true, 13.919f, 2.702f)
                moveTo(8f, 10f)
                arcTo(1f, 1f, 0f, false, false, 10f, 10f)
                arcTo(1f, 1f, 0f, false, false, 8f, 10f)
                moveTo(14f, 16f)
                arcTo(1f, 1f, 0f, false, false, 16f, 16f)
                arcTo(1f, 1f, 0f, false, false, 14f, 16f)
            }
        }.build()
    }

    /** Den ban (Font Awesome, fill dac). */
    val Lamp: ImageVector by lazy {
        ImageVector.Builder("lamp", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(23.955f, 10.212f)
                lineTo(23.177f, 4.117f)
                curveTo(22.876f, 1.77f, 20.865f, 0f, 18.5f, 0f)
                horizontalLineTo(5.489f)
                curveTo(3.123f, 0f, 1.113f, 1.77f, 0.813f, 4.117f)
                lineTo(0.034f, 10.212f)
                curveTo(-0.12f, 11.422f, 0.254f, 12.642f, 1.06f, 13.558f)
                curveTo(1.867f, 14.475f, 3.03f, 15f, 4.25f, 15f)
                horizontalLineTo(10.999f)
                verticalLineTo(16f)
                curveTo(10.999f, 17.827f, 9.463f, 17.993f, 8.999f, 18f)
                curveTo(7.345f, 18f, 5.999f, 19.346f, 5.999f, 21f)
                curveTo(5.999f, 22.654f, 7.345f, 24f, 8.999f, 24f)
                horizontalLineTo(14.999f)
                curveTo(16.653f, 24f, 17.999f, 22.654f, 17.999f, 21f)
                curveTo(17.999f, 19.381f, 16.71f, 18.058f, 15.104f, 18.002f)
                curveTo(14.74f, 17.924f, 12.999f, 17.475f, 12.999f, 16f)
                verticalLineTo(15f)
                horizontalLineTo(19.738f)
                curveTo(20.959f, 15f, 22.121f, 14.475f, 22.927f, 13.559f)
                curveTo(23.734f, 12.642f, 24.108f, 11.423f, 23.953f, 10.212f)
                moveTo(14.998f, 20f)
                curveTo(15.55f, 20f, 15.998f, 20.449f, 15.998f, 21f)
                curveTo(15.998f, 21.551f, 15.55f, 22f, 14.998f, 22f)
                horizontalLineTo(8.998f)
                curveTo(8.446f, 22f, 7.998f, 21.551f, 7.998f, 21f)
                curveTo(7.998f, 20.449f, 8.446f, 20f, 8.998f, 20f)
                curveTo(9.981f, 20f, 11.258f, 19.602f, 12.101f, 18.622f)
                curveTo(12.942f, 19.445f, 14.005f, 19.892f, 14.998f, 20f)
                moveTo(21.426f, 12.237f)
                curveTo(20.999f, 12.722f, 20.384f, 13f, 19.738f, 13f)
                horizontalLineTo(4.251f)
                curveTo(3.605f, 13f, 2.988f, 12.722f, 2.562f, 12.236f)
                curveTo(2.135f, 11.751f, 1.937f, 11.106f, 2.019f, 10.465f)
                lineTo(2.798f, 4.37f)
                curveTo(2.97f, 3.019f, 4.127f, 2f, 5.489f, 2f)
                horizontalLineTo(18.5f)
                curveTo(19.862f, 2f, 21.02f, 3.019f, 21.192f, 4.37f)
                lineTo(21.97f, 10.465f)
                curveTo(22.052f, 11.106f, 21.854f, 11.752f, 21.427f, 12.237f)
            }
        }.build()
    }

    /** May tinh (Font Awesome, fill dac). */
    val Computer: ImageVector by lazy {
        ImageVector.Builder("computer", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(5f, 19f)
                horizontalLineTo(11f)
                verticalLineTo(21f)
                horizontalLineTo(7f)
                arcTo(1f, 1f, 0f, false, false, 7f, 23f)
                horizontalLineTo(17f)
                arcTo(1f, 1f, 0f, false, false, 17f, 21f)
                horizontalLineTo(13f)
                verticalLineTo(19f)
                horizontalLineTo(19f)
                arcTo(5.009f, 5.009f, 0f, false, false, 23.9f, 15f)
                horizontalLineTo(0.1f)
                arcTo(5.009f, 5.009f, 0f, false, false, 5f, 19f)
                moveTo(19f, 1f)
                horizontalLineTo(5f)
                arcTo(5.006f, 5.006f, 0f, false, false, 0f, 6f)
                verticalLineTo(13f)
                horizontalLineTo(24f)
                verticalLineTo(6f)
                arcTo(5.006f, 5.006f, 0f, false, false, 19f, 1f)
            }
        }.build()
    }

    /** Sofa (Font Awesome, fill dac). */
    val Couch: ImageVector by lazy {
        ImageVector.Builder("couch", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(22f, 9.172f)
                verticalLineTo(7f)
                curveTo(22f, 3.691f, 19.309f, 1f, 16f, 1f)
                horizontalLineTo(8f)
                curveTo(4.691f, 1f, 2f, 3.691f, 2f, 7f)
                verticalLineTo(9.172f)
                curveTo(0.836f, 9.585f, 0f, 10.696f, 0f, 12f)
                verticalLineTo(16f)
                curveTo(0f, 17.632f, 0.786f, 19.084f, 2f, 19.997f)
                curveTo(2f, 19.997f, 2f, 19.999f, 2f, 20f)
                verticalLineTo(22f)
                curveTo(2f, 22.552f, 2.448f, 23f, 3f, 23f)
                curveTo(3.552f, 23f, 4f, 22.552f, 4f, 22f)
                verticalLineTo(20.899f)
                curveTo(4.323f, 20.965f, 4.658f, 21f, 5f, 21f)
                horizontalLineTo(19f)
                curveTo(19.342f, 21f, 19.677f, 20.965f, 20f, 20.899f)
                verticalLineTo(22f)
                curveTo(20f, 22.552f, 20.448f, 23f, 21f, 23f)
                curveTo(21.552f, 23f, 22f, 22.552f, 22f, 22f)
                verticalLineTo(20f)
                curveTo(22f, 20f, 22f, 19.998f, 22f, 19.997f)
                curveTo(23.214f, 19.084f, 24f, 17.632f, 24f, 16f)
                verticalLineTo(12f)
                curveTo(24f, 10.696f, 23.164f, 9.585f, 22f, 9.172f)
                moveTo(8f, 3f)
                horizontalLineTo(16f)
                curveTo(18.206f, 3f, 20f, 4.794f, 20f, 7f)
                verticalLineTo(9.172f)
                curveTo(18.836f, 9.585f, 18f, 10.696f, 18f, 12f)
                verticalLineTo(14f)
                horizontalLineTo(6f)
                verticalLineTo(12f)
                curveTo(6f, 10.696f, 5.164f, 9.585f, 4f, 9.172f)
                verticalLineTo(7f)
                curveTo(4f, 4.794f, 5.794f, 3f, 8f, 3f)
                moveTo(22f, 16f)
                curveTo(22f, 17.654f, 20.654f, 19f, 19f, 19f)
                horizontalLineTo(5f)
                curveTo(3.346f, 19f, 2f, 17.654f, 2f, 16f)
                verticalLineTo(12f)
                curveTo(2f, 11.449f, 2.449f, 11f, 3f, 11f)
                curveTo(3.551f, 11f, 4f, 11.449f, 4f, 12f)
                verticalLineTo(15f)
                curveTo(4f, 15.552f, 4.448f, 16f, 5f, 16f)
                horizontalLineTo(19f)
                curveTo(19.552f, 16f, 20f, 15.552f, 20f, 15f)
                verticalLineTo(12f)
                curveTo(20f, 11.449f, 20.449f, 11f, 21f, 11f)
                curveTo(21.551f, 11f, 22f, 11.449f, 22f, 12f)
                verticalLineTo(16f)
            }
        }.build()
    }

    /** Lua (Font Awesome, fill dac). */
    val Flame: ImageVector by lazy {
        ImageVector.Builder("flame", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(354.773f, 61.867f)
                curveTo(337.984f, 47.638f, 320.384f, 32.683f, 302.464f, 16.384f)
                curveTo(288.717f, 3.882f, 270.151f, -1.912f, 251.733f, 0.555f)
                curveTo(233.885f, 2.913f, 218.005f, 13.072f, 208.384f, 28.288f)
                curveTo(182.954f, 70.784f, 164.974f, 117.313f, 155.221f, 165.867f)
                curveTo(151.394f, 160.341f, 147.999f, 154.529f, 145.066f, 148.48f)
                curveTo(134.962f, 127.192f, 109.514f, 118.125f, 88.226f, 128.229f)
                curveTo(83.072f, 130.675f, 78.461f, 134.13f, 74.666f, 138.389f)
                curveTo(38.883f, 174.955f, 19.046f, 224.21f, 19.498f, 275.37f)
                curveTo(18.481f, 382.902f, 90.812f, 477.313f, 194.901f, 504.32f)
                curveTo(214.268f, 509.193f, 234.152f, 511.714f, 254.122f, 511.829f)
                curveTo(254.762f, 511.829f, 261.567f, 511.765f, 264.319f, 511.573f)
                curveTo(391.679f, 507.448f, 492.745f, 402.925f, 492.586f, 275.498f)
                curveTo(492.501f, 178.859f, 428.672f, 124.672f, 354.773f, 61.867f)
                moveTo(253.589f, 469.013f)
                curveTo(237.712f, 467.805f, 222.022f, 461.374f, 210.176f, 451.818f)
                curveTo(191.626f, 438.692f, 179.351f, 419.444f, 176.427f, 397.269f)
                curveTo(172.8f, 362.666f, 194.134f, 317.418f, 237.718f, 266.304f)
                lineTo(237.718f, 266.304f)
                curveTo(242.288f, 260.966f, 248.974f, 257.907f, 256.001f, 257.941f)
                lineTo(256.001f, 257.941f)
                curveTo(262.937f, 257.891f, 269.533f, 260.942f, 273.985f, 266.261f)
                curveTo(313.921f, 313.664f, 335.852f, 357.397f, 335.852f, 389.418f)
                curveTo(335.729f, 431.488f, 302.846f, 464.768f, 260.972f, 468.821f)
                curveTo(259.133f, 468.999f, 256f, 469.269f, 253.589f, 469.013f)
                moveTo(374.955f, 428.437f)
                curveTo(373.696f, 429.418f, 372.31f, 430.208f, 371.03f, 431.146f)
                curveTo(375.952f, 417.768f, 378.487f, 403.63f, 378.518f, 389.375f)
                curveTo(378.518f, 335.466f, 339.371f, 277.695f, 306.561f, 238.719f)
                curveTo(294.008f, 223.852f, 275.544f, 215.268f, 256.086f, 215.252f)
                horizontalLineTo(256f)
                curveTo(236.503f, 215.217f, 217.972f, 223.742f, 205.312f, 238.569f)
                lineTo(205.312f, 238.569f)
                curveTo(153.152f, 299.668f, 129.195f, 354.558f, 134.101f, 401.726f)
                curveTo(135.266f, 412.676f, 138.063f, 423.39f, 142.4f, 433.513f)
                curveTo(91.742f, 396.807f, 61.893f, 337.926f, 62.229f, 275.369f)
                curveTo(61.817f, 234.73f, 77.843f, 195.648f, 106.666f, 166.996f)
                curveTo(111.587f, 177.226f, 117.496f, 186.95f, 124.309f, 196.031f)
                curveTo(133.666f, 208.681f, 149.651f, 214.551f, 164.97f, 210.964f)
                curveTo(180.589f, 207.509f, 192.716f, 195.19f, 195.925f, 179.519f)
                curveTo(204.496f, 134.215f, 220.875f, 90.745f, 244.33f, 51.05f)
                curveTo(247.216f, 46.512f, 251.983f, 43.506f, 257.322f, 42.858f)
                curveTo(263.289f, 42.055f, 269.304f, 43.938f, 273.749f, 47.999f)
                curveTo(292.053f, 64.639f, 310.016f, 79.999f, 327.082f, 94.442f)
                curveTo(398.293f, 154.922f, 449.77f, 198.613f, 449.77f, 275.498f)
                curveTo(449.954f, 335.331f, 422.334f, 391.856f, 375.018f, 428.479f)
                lineTo(374.955f, 428.437f)
            }
        }.build()
    }

    /** Phat (Font Awesome, fill dac). */
    val PlayFa: ImageVector by lazy {
        ImageVector.Builder("playfa", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(20.492f, 7.969f)
                lineTo(10.954f, 0.975f)
                arcTo(5f, 5f, 0f, false, false, 3f, 5.005f)
                verticalLineTo(19f)
                arcTo(4.994f, 4.994f, 0f, false, false, 10.954f, 23.03f)
                lineTo(20.492f, 16.036f)
                arcTo(5f, 5f, 0f, false, false, 20.492f, 7.974f)
            }
        }.build()
    }

    /** Tam dung (Font Awesome, fill dac). */
    val PauseFa: ImageVector by lazy {
        ImageVector.Builder("pausefa", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6.5f, 0f)
                arcTo(3.5f, 3.5f, 0f, false, false, 3f, 3.5f)
                verticalLineTo(20.5f)
                arcTo(3.5f, 3.5f, 0f, false, false, 10f, 20.5f)
                verticalLineTo(3.5f)
                arcTo(3.5f, 3.5f, 0f, false, false, 6.5f, 0f)
                moveTo(17.5f, 0f)
                arcTo(3.5f, 3.5f, 0f, false, false, 14f, 3.5f)
                verticalLineTo(20.5f)
                arcTo(3.5f, 3.5f, 0f, false, false, 21f, 20.5f)
                verticalLineTo(3.5f)
                arcTo(3.5f, 3.5f, 0f, false, false, 17.5f, 0f)
            }
        }.build()
    }

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
        // Bep tu / noi chien / may rua bat: icon rieng, khong dung chung soup_kitchen.
        "washer", "dryer" -> Ms.local_laundry_service
        "cooking" -> Ms.cooking
        "dishwasher" -> Ms.dishwasher
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
        "airfryer" -> AirFryer
        // Dieu hoa: dan lanh treo tuong ve tay (2026-09-30, user yeu cau
        // thiet ke lai — Ms.snowflake dang tro sai codepoint F16F = glyph power).
        "snowflake" -> AirConditioner
        else -> null
    }

    /**
     * Dieu hoa treo tuong — dan lanh + cua gio + gio thoi (2026-09-30).
     * Stroke 2dp, dau tron, viewport 24 (cung style Fridge/Fan/Tv/AirFryer).
     */
    val AirConditioner: ImageVector by lazy {
        ImageVector.Builder("air_conditioner", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Than dan lanh
                moveTo(5f, 4.5f); horizontalLineTo(19f)
                arcTo(2f, 2f, 0f, false, true, 21f, 6.5f)
                verticalLineTo(9.5f)
                arcTo(2f, 2f, 0f, false, true, 19f, 11.5f)
                horizontalLineTo(5f)
                arcTo(2f, 2f, 0f, false, true, 3f, 9.5f)
                verticalLineTo(6.5f)
                arcTo(2f, 2f, 0f, false, true, 5f, 4.5f)
                close()
                // Vach mat truoc + den hien thi
                moveTo(5f, 8f); horizontalLineTo(19f)
                moveTo(16.5f, 6.2f); horizontalLineTo(18.5f)
                // Cua gio
                moveTo(6f, 14.5f); horizontalLineTo(18f)
                // Gio thoi xuong
                moveTo(9.5f, 16.5f); verticalLineTo(19.5f)
                moveTo(14.5f, 16.5f); verticalLineTo(19.5f)
            }
        }.build()
    }

    /**
     * Noi chien khong dau — than hop bo goc + panel dieu khien + tay cam khay (2026-09-30).
     * Stroke 2dp, dau tron, viewport 24 (cung style Fridge/Fan/Tv).
     */
    val AirFryer: ImageVector by lazy {
        ImageVector.Builder("air_fryer", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Than may
                moveTo(7f, 3.5f); horizontalLineTo(17f)
                arcTo(2f, 2f, 0f, false, true, 19f, 5.5f)
                verticalLineTo(18.5f)
                arcTo(2f, 2f, 0f, false, true, 17f, 20.5f)
                horizontalLineTo(7f)
                arcTo(2f, 2f, 0f, false, true, 5f, 18.5f)
                verticalLineTo(5.5f)
                arcTo(2f, 2f, 0f, false, true, 7f, 3.5f)
                close()
                // Vach ngan panel dieu khien
                moveTo(5f, 8.5f); horizontalLineTo(19f)
                // Man hinh nho tren panel
                moveTo(10f, 6f); horizontalLineTo(14f)
                // Tay cam khay chien
                moveTo(10.5f, 12.5f); horizontalLineTo(13.5f)
                arcTo(1f, 1f, 0f, false, true, 14.5f, 13.5f)
                verticalLineTo(14.5f)
                arcTo(1f, 1f, 0f, false, true, 13.5f, 15.5f)
                horizontalLineTo(10.5f)
                arcTo(1f, 1f, 0f, false, true, 9.5f, 14.5f)
                verticalLineTo(13.5f)
                arcTo(1f, 1f, 0f, false, true, 10.5f, 12.5f)
                close()
            }
        }.build()
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
    val Fan: ImageVector get() = FanFa

    /** Tivi — man hinh bo goc + chan de. */
    val Tv: ImageVector get() = TvRetro

    /**
     * Chuong thong bao RONG — ve lai vi glyph Material hep (2026-09-30, user yeu cau).
     * Than chuong trai rong 4..20, stroke 2dp, dau tron, viewport 24.
     */
    val BellWide: ImageVector by lazy {
        ImageVector.Builder("bell_wide", 24.dp, 24.dp, 24f, 24f).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Num tron tren dinh
                moveTo(12f, 2.6f)
                arcTo(1.4f, 1.4f, 0f, true, true, 12f, 5.4f)
                arcTo(1.4f, 1.4f, 0f, true, true, 12f, 2.6f)
                close()
                // Than chuong: vai cong rong tu dinh xuong mieng
                moveTo(12f, 5.4f)
                curveTo(8.5f, 5.4f, 6.8f, 8.2f, 6.2f, 11.5f)
                lineTo(5f, 16.5f)
                // Mieng chuong loe rong
                curveTo(4.7f, 17.8f, 5.6f, 18.8f, 7f, 18.8f)
                horizontalLineTo(17f)
                curveTo(18.4f, 18.8f, 19.3f, 17.8f, 19f, 16.5f)
                lineTo(17.8f, 11.5f)
                curveTo(17.2f, 8.2f, 15.5f, 5.4f, 12f, 5.4f)
                close()
                // Qua lac duoi
                moveTo(12f, 18.8f)
                lineTo(12f, 20.2f)
                moveTo(12f, 21.4f)
                arcTo(1.2f, 1.2f, 0f, true, true, 12f, 23.8f)
                arcTo(1.2f, 1.2f, 0f, true, true, 12f, 21.4f)
                close()
            }
        }.build()
    }
    /** Nha (Font Awesome) (fill dac). */
    val HouseFa: ImageVector by lazy {
        ImageVector.Builder("housefa", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 24f)
                horizontalLineTo(5f)
                curveTo(2.243f, 24f, 0f, 21.757f, 0f, 19f)
                verticalLineTo(9.724f)
                curveTo(0f, 8.059f, 0.824f, 6.509f, 2.204f, 5.579f)
                lineTo(9.203f, 0.855f)
                curveTo(10.902f, -0.291f, 13.098f, -0.291f, 14.797f, 0.855f)
                lineTo(21.797f, 5.579f)
                curveTo(23.176f, 6.509f, 24f, 8.058f, 24f, 9.724f)
                verticalLineTo(19f)
                curveTo(24f, 21.757f, 21.757f, 24f, 19f, 24f)
                moveTo(12f, 1.997f)
                curveTo(11.416f, 1.997f, 10.832f, 2.169f, 10.322f, 2.514f)
                lineTo(3.322f, 7.237f)
                curveTo(2.494f, 7.795f, 2f, 8.724f, 2f, 9.723f)
                verticalLineTo(18.999f)
                curveTo(2f, 20.653f, 3.346f, 21.999f, 5f, 21.999f)
                horizontalLineTo(19f)
                curveTo(20.654f, 21.999f, 22f, 20.653f, 22f, 18.999f)
                verticalLineTo(9.724f)
                curveTo(22f, 8.725f, 21.506f, 7.795f, 20.679f, 7.238f)
                lineTo(13.678f, 2.514f)
                curveTo(13.168f, 2.169f, 12.584f, 1.997f, 12f, 1.997f)
            }
        }.build()
    }

    /** Khien (Font Awesome) (fill dac). */
    val ShieldFa: ImageVector by lazy {
        ImageVector.Builder("shieldfa", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19.949f, 2.644f)
                lineTo(12f, 0.009f)
                lineTo(4.051f, 2.644f)
                arcTo(2.982f, 2.982f, 0f, false, false, 2f, 5.476f)
                verticalLineTo(12f)
                curveTo(2f, 19.524f, 11.2f, 23.679f, 11.594f, 23.852f)
                lineTo(11.948f, 24.009f)
                lineTo(12.316f, 23.887f)
                curveTo(12.711f, 23.755f, 22f, 20.577f, 22f, 12f)
                verticalLineTo(5.476f)
                arcTo(2.983f, 2.983f, 0f, false, false, 19.949f, 2.644f)
                moveTo(20f, 12f)
                curveTo(20f, 18.263f, 13.651f, 21.216f, 12.047f, 21.861f)
                curveTo(10.44f, 21.058f, 4f, 17.494f, 4f, 12f)
                verticalLineTo(5.476f)
                arcTo(0.994f, 0.994f, 0f, false, true, 4.684f, 4.532f)
                lineTo(12f, 2.106f)
                lineTo(19.316f, 4.532f)
                arcTo(0.992f, 0.992f, 0f, false, true, 20f, 5.476f)
            }
        }.build()
    }


    /** Gia dinh (Font Awesome) (fill dac). */
    val FamilyFa: ImageVector by lazy {
        ImageVector.Builder("familyfa", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(16.5f, 2.5f)
                curveTo(16.5f, 1.119f, 17.619f, 0f, 19f, 0f)
                curveTo(20.381f, 0f, 21.5f, 1.119f, 21.5f, 2.5f)
                curveTo(21.5f, 3.881f, 20.381f, 5f, 19f, 5f)
                curveTo(17.619f, 5f, 16.5f, 3.881f, 16.5f, 2.5f)
                moveTo(23.977f, 16.628f)
                lineTo(23.101f, 9.62f)
                curveTo(22.843f, 7.557f, 21.08f, 6f, 19f, 6f)
                curveTo(18.039f, 6f, 17.102f, 6.338f, 16.361f, 6.952f)
                curveTo(15.936f, 7.305f, 15.878f, 7.935f, 16.23f, 8.36f)
                curveTo(16.584f, 8.786f, 17.213f, 8.844f, 17.638f, 8.491f)
                curveTo(18.02f, 8.175f, 18.503f, 8f, 18.999f, 8f)
                curveTo(20.072f, 8f, 20.982f, 8.803f, 21.115f, 9.868f)
                lineTo(21.991f, 16.876f)
                curveTo(22.027f, 17.165f, 21.941f, 17.443f, 21.748f, 17.662f)
                curveTo(21.556f, 17.88f, 21.29f, 18f, 20.998f, 18f)
                horizontalLineTo(17.998f)
                curveTo(17.446f, 18f, 16.998f, 18.447f, 16.998f, 19f)
                curveTo(16.998f, 19.553f, 17.446f, 20f, 17.998f, 20f)
                horizontalLineTo(19.998f)
                verticalLineTo(23f)
                curveTo(19.998f, 23.553f, 20.446f, 24f, 20.998f, 24f)
                curveTo(21.55f, 24f, 21.998f, 23.553f, 21.998f, 23f)
                verticalLineTo(19.811f)
                curveTo(22.472f, 19.642f, 22.907f, 19.37f, 23.247f, 18.985f)
                curveTo(23.816f, 18.339f, 24.081f, 17.481f, 23.975f, 16.628f)
                moveTo(5f, 5f)
                curveTo(6.381f, 5f, 7.5f, 3.881f, 7.5f, 2.5f)
                curveTo(7.5f, 1.119f, 6.381f, 0f, 5f, 0f)
                curveTo(3.619f, 0f, 2.5f, 1.119f, 2.5f, 2.5f)
                curveTo(2.5f, 3.881f, 3.619f, 5f, 5f, 5f)
                moveTo(7.333f, 6.228f)
                curveTo(6.905f, 6.078f, 6.456f, 6f, 6f, 6f)
                horizontalLineTo(4f)
                curveTo(1.794f, 6f, 0f, 7.794f, 0f, 10f)
                verticalLineTo(13f)
                curveTo(0f, 14.474f, 0.81f, 15.75f, 2f, 16.444f)
                verticalLineTo(23f)
                curveTo(2f, 23.553f, 2.448f, 24f, 3f, 24f)
                curveTo(3.552f, 24f, 4f, 23.553f, 4f, 23f)
                verticalLineTo(17f)
                horizontalLineTo(5f)
                curveTo(5.552f, 17f, 6f, 16.553f, 6f, 16f)
                curveTo(6f, 15.447f, 5.552f, 15f, 5f, 15f)
                horizontalLineTo(4f)
                curveTo(2.897f, 15f, 2f, 14.103f, 2f, 13f)
                verticalLineTo(10f)
                curveTo(2f, 8.897f, 2.897f, 8f, 4f, 8f)
                horizontalLineTo(6f)
                curveTo(6.229f, 8f, 6.453f, 8.038f, 6.667f, 8.114f)
                curveTo(7.189f, 8.295f, 7.759f, 8.023f, 7.943f, 7.504f)
                curveTo(8.127f, 6.983f, 7.854f, 6.412f, 7.333f, 6.228f)
                moveTo(12f, 10f)
                curveTo(13.381f, 10f, 14.5f, 8.881f, 14.5f, 7.5f)
                curveTo(14.5f, 6.119f, 13.381f, 5f, 12f, 5f)
                curveTo(10.619f, 5f, 9.5f, 6.119f, 9.5f, 7.5f)
                curveTo(9.5f, 8.881f, 10.619f, 10f, 12f, 10f)
                moveTo(16f, 14f)
                verticalLineTo(17f)
                curveTo(16f, 17.883f, 15.609f, 18.67f, 15f, 19.22f)
                verticalLineTo(23f)
                curveTo(15f, 23.553f, 14.552f, 24f, 14f, 24f)
                curveTo(13.448f, 24f, 13f, 23.553f, 13f, 23f)
                verticalLineTo(20f)
                horizontalLineTo(11f)
                verticalLineTo(23f)
                curveTo(11f, 23.553f, 10.552f, 24f, 10f, 24f)
                curveTo(9.448f, 24f, 9f, 23.553f, 9f, 23f)
                verticalLineTo(19.22f)
                curveTo(8.391f, 18.671f, 8f, 17.883f, 8f, 17f)
                verticalLineTo(14f)
                curveTo(8f, 12.346f, 9.346f, 11f, 11f, 11f)
                horizontalLineTo(13f)
                curveTo(14.654f, 11f, 16f, 12.346f, 16f, 14f)
                moveTo(13f, 18f)
                curveTo(13.551f, 18f, 14f, 17.552f, 14f, 17f)
                verticalLineTo(14f)
                curveTo(14f, 13.448f, 13.551f, 13f, 13f, 13f)
                horizontalLineTo(11f)
                curveTo(10.449f, 13f, 10f, 13.448f, 10f, 14f)
                verticalLineTo(17f)
                curveTo(10f, 17.552f, 10.449f, 18f, 11f, 18f)
                horizontalLineTo(13f)
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
