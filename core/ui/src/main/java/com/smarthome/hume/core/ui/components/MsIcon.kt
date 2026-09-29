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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.isFinite
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.smarthome.hume.core.ui.R

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
        val side = minOf(maxWidth, maxHeight)
        val fs = if (side.isFinite) {
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
    val dark_mode = "\uE51C"
    val desk = "\uF8F4"
    val device_thermostat = "\uE1FF"
    val donut_large = "\uE917"
    val door_front = "\uEFFD"
    val electric_meter = "\uEC1B"
    val error = "\uE000"
    val expand_more = "\uE5CF"
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
    val snowflake = "\uF16F"
    val lock_open = "\uE898"
    val door_open = "\uE77C"
}
