package com.smarthome.hume.core.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.LocalContentColor
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.isFinite
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.smarthome.hume.core.ui.R
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalTextApi::class)
private val MsFontFamily = FontFamily(
    Font(
        R.font.material_symbols_rounded,
        // Outlined: giong ban cu (pin wght 200, FILL 0, opsz 24)
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("wght", 200f),
            FontVariation.Setting("opsz", 24f),
            FontVariation.Setting("FILL", 0f),
            FontVariation.Setting("GRAD", 0f),
        ),
    ),
)

/** Bien the FILL=1 cho icon active (vd: navbar tab dang chon). */
@OptIn(ExperimentalTextApi::class)
private val MsFilledFontFamily = FontFamily(
    Font(
        R.font.material_symbols_rounded,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("wght", 400f),
            FontVariation.Setting("opsz", 24f),
            FontVariation.Setting("FILL", 1f),
            FontVariation.Setting("GRAD", 0f),
        ),
    ),
)

/**
 * Icon Material Symbols Rounded variable font.
 * Mac dinh outlined (FILL=0, wght=200) + synthetic bold nhu ban HTML;
 * [filled] = true -> FILL=1, wght=400 cho trang thai active.
 *
 * Dung: MsIcon(M3EIcons.Search, null, tint = ..., modifier = Modifier.size(22.dp))
 * Kich thuoc chu = canh nho nhat cua modifier (thuong la Modifier.size);
 * neu modifier khong co kich thuoc co dinh thi mac dinh 24.sp.
 */
@Composable
fun MsIcon(
    glyph: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    filled: Boolean = false,
) {
    // Can quang hoc: dua tam muc in (ink) cua glyph ve chinh giua box.
    // Bao dam icon luon nam giua icon-background du glyph co ve lech
    // (side bearing bat doi xung, synthetic bold, ...).
    var inkOffset by remember(glyph) { mutableStateOf(IntOffset.Zero) }
    BoxWithConstraints(
        modifier = modifier.semantics {
            if (contentDescription != null) this.contentDescription = contentDescription
        },
        contentAlignment = Alignment.Center,
    ) {
        // Kich thuoc chu = canh nho nhat cua constraints KHI ca 2 chieu deu huu han
        // (tuc la modifier co size co dinh nhu Modifier.size(44.dp)).
        // Neu 1 chieu vo han (vd: icon nam trong Column scroll duoc) thi KHONG
        // duoc lay chieu con lai lam co chu — truoc day lay width ~380dp lam
        // fontSize -> icon phong to thanh chu khong lo pha vo layout
        // (2026-09-30, the font chu hien "chevron_right" khong lo).
        val side = minOf(maxWidth, maxHeight)
        val fs = if (maxWidth.isFinite && maxHeight.isFinite) {
            with(LocalDensity.current) { side.toSp() }
        } else {
            24.sp
        }
        BasicText(
            text = glyph,
            modifier = Modifier.offset { inkOffset },
            onTextLayout = { result ->
                val bb = runCatching { result.getBoundingBox(0) }.getOrNull()
                if (bb != null && !bb.isEmpty) {
                    val dx = result.size.width / 2f - (bb.left + bb.right) / 2f
                    val dy = result.size.height / 2f - (bb.top + bb.bottom) / 2f
                    val next = IntOffset(dx.roundToInt(), dy.roundToInt())
                    if (next != inkOffset) inkOffset = next
                }
            },
            style = TextStyle(
                fontFamily = if (filled) MsFilledFontFamily else MsFontFamily,
                fontSize = fs,
                lineHeight = fs,
                // Outlined: net day hon bang synthetic bold (font static wght 200).
                // Filled: glyph dac san nen khong can.
                fontWeight = if (filled) androidx.compose.ui.text.font.FontWeight.Normal
                else androidx.compose.ui.text.font.FontWeight.Bold,
                color = tint,
                textAlign = TextAlign.Center,
                // Them shadow cung mau de gia day net (chi ban outlined)
                shadow = if (filled) null else androidx.compose.ui.graphics.Shadow(
                    color = tint,
                    offset = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                    blurRadius = 0.8f,
                ),
            ),
        )
    }
}

@Composable
fun MsIcon(
    glyph: Any,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    filled: Boolean = false,
) {
    when (glyph) {
        is String -> MsIcon(glyph, contentDescription, modifier, tint, filled)
        is ImageVector -> Icon(
            imageVector = glyph,
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint,
        )
        else -> error("Unsupported icon type: ${glyph::class.qualifiedName}")
    }
}

/**
 * Bang glyph PUA (font material_symbols_rounded.ttf trong res/font).
 * Ten = ten ligature goc cua Material Symbols.
 */
object Ms {
    val ac_unit = "\uEB3B"
    val add = "\uE145"
    val arrow_back = "\uE5C4"
    val auto_awesome = "\uE65F"
    val bathtub = "\uEA41"
    val battery_0_bar = "\uEBDC"
    val battery_1_bar = "\uEBD9"
    val battery_2_bar = "\uEBE0"
    val battery_3_bar = "\uEBDD"
    val battery_4_bar = "\uEBE2"
    val battery_5_bar = "\uEBD4"
    val battery_6_bar = "\uEBD2"
    val battery_alert = "\uE19C"
    val battery_charging_full = "\uE1A3"
    val battery_full = "\uE1A4"
    val bed = "\uEFDF"
    val bedtime = "\uE1F9"
    val bolt = "\uEA0B"
    val check = "\uE5CA"
    val chevron_right = "\uE409"
    val child_care = "\uEB41"
    val close = "\uE14C"
    /** Noi tren bep — dung cho bep tu (them 2026-09-30). */
    val cooking = "\uE2B6"
    val dark_mode = "\uE51C"
    val desk = "\uF8F4"
    val device_thermostat = "\uE1FF"
    /** May rua bat (them 2026-09-30). */
    val dishwasher = "\uE9A0"
    val donut_large = "\uE917"
    val door_front = "\uEFFD"
    val electric_meter = "\uEC1B"
    val error = "\uE000"
    val expand_more = "\uE5CF"
    // (2026-10-22) 4 glyph bo sung vao font subset (regen tu Material
    // Symbols Rounded goc, da doi chieu outline 79 glyph cu khong doi):
    // dung cho cac nut Sua/Xoa/sap xep trong Quan ly phong.
    val edit = "\uE3C9"
    val delete = "\uE872"
    val keyboard_arrow_up = "\uE316"
    val keyboard_arrow_down = "\uE313"
    val fiber_manual_record = "\uE061"
    val flight_takeoff = "\uE905"
    val fullscreen = "\uE5D0"
    val home = "\uE88A"
    val info = "\uE88E"
    val key = "\uE73C"
    val language = "\uE894"
    val light_mode = "\uE518"
    val lightbulb = "\uE0F0"
    val link = "\uE157"
    val local_laundry_service = "\uE54A"
    val lock = "\uE88D"
    val logout = "\uE9BA"
    val meeting_room = "\uEB4F"
    val mic = "\uE029"
    val notifications = "\uE7F4"
    val notifications_off = "\uE7F6"
    val palette = "\uE3B7"
    val person = "\uE7FD"
    val person_search = "\uF106"
    val photo_camera = "\uE3B0"
    val play_circle = "\uE038"
    val power = "\uE63C"
    val power_settings_new = "\uE8AC"
    val remove = "\uE15B"
    val search = "\uE8B6"
    val sensors = "\uE51E"
    val settings_remote = "\uE8C7"
    val shield = "\uE75B"
    val signal_cellular_alt = "\uE202"
    val smoke_free = "\uEB4A"
    val solar_power = "\uEC0F"
    val soup_kitchen = "\uE7D3"
    val stairs = "\uF1A9"
    val thermostat = "\uF076"
    val videocam = "\uE04B"
    val visibility = "\uE417"
    val visibility_off = "\uE8F5"
    val water_drop = "\uE798"
    val wb_sunny = "\uE430"
    val weekend = "\uE16B"
    val whatshot = "\uE80E"
    val wifi = "\uE63E"
    val remote_gen = "\uE83E"
    // snowflake: codepoint official ED5B (ban cu F16F tro nham glyph power).
    val snowflake = "\uED5B"
    val lock_open = "\uE898"
    val door_open = "\uE77C"
    // (2026-10-26, Batch E GAP-11/18) 66 glyph bo sung vao font subset
    // (regen tu Material Symbols Rounded goc v2.978, upem 960 — codepoint
    // lay tu cmap font goc theo ten glyph, khong phai tu file codepoints
    // master; 83 glyph cu da doi chieu 0 diff o ca 2 bien the app dung).
    // Gom 64 glyph cho picker 12 nhom + filter/water_pump cho tab Nuoc.
    val air_purifier_gen = "\uE829"
    val airwave = "\uF154"
    val blinds = "\uE286"
    val blinds_closed = "\uEC1F"
    val cell_tower = "\uEBBA"
    val chair = "\uEFED"
    val coffee = "\uEFEF"
    val curtains = "\uEC1E"
    val curtains_closed = "\uEC1D"
    val desktop_windows = "\uE30C"
    val directions_walk = "\uE536"
    val door_back = "\uEFFC"
    val door_sliding = "\uEFFE"
    val eco = "\uEA35"
    val electrical_services = "\uF102"
    val energy = "\uE9A6"
    val ev_station = "\uE56D"
    val faucet = "\uE278"
    val filter = "\uE3D3"
    val flashlight_off = "\uF00A"
    val flashlight_on = "\uF00B"
    val flatware = "\uF00C"
    val floor_lamp = "\uE21E"
    val garage = "\uF011"
    val graphic_eq = "\uE1B8"
    val heat_pump = "\uEC18"
    val humidity_high = "\uF163"
    val humidity_low = "\uF164"
    val humidity_mid = "\uF165"
    val humidity_percentage = "\uF87E"
    val light_group = "\uE28B"
    val lightbulb_2 = "\uF3E3"
    val lightbulb_circle = "\uEBFE"
    val local_cafe = "\uEB44"
    val local_gas_station = "\uE546"
    val microwave_gen = "\uE847"
    val mode_heat = "\uF16A"
    val monitor = "\uEF5B"
    val music_note = "\uE405"
    val outlet = "\uF1D4"
    val oven_gen = "\uE843"
    val pool = "\uEB48"
    val print = "\uE8AD"
    val restaurant = "\uE56C"
    val scanner = "\uE329"
    val shower = "\uF061"
    val speaker = "\uE32D"
    val speaker_group = "\uE32E"
    val speed = "\uE9E4"
    val sports_esports = "\uEA28"
    val sprinkler = "\uE29A"
    val stockpot = "\uF545"
    val switch_left = "\uF1D1"
    val switch_right = "\uF1D2"
    val table_lamp = "\uE1F2"
    val table_restaurant = "\uEAC6"
    val thermometer = "\uE846"
    val toggle_off = "\uE9F5"
    val toggle_on = "\uE9F6"
    val tv = "\uE63B"
    val vacuum = "\uEFC5"
    val water_heater = "\uE284"
    val water_pump = "\uF5D8"
    val waves = "\uE176"
    val wb_twilight = "\uE1C6"
    val window = "\uF088"
}
