package com.smarthome.hume.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.Ph
import com.smarthome.hume.ui.theme.glassPill
import com.smarthome.hume.ui.theme.glassSurface
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The two popups a room bubble can open, ported from RGBPopupView.swift and
 * ClimatePopupView.swift. In the SwiftUI app they are reached by double tapping
 * the icon of a device card, never by the card toggle, and they sit on top of
 * the open room sheet over a dark scrim.
 *
 * Icon: toan bo dung Phosphor net mong (Ph.*), khong con Material dac.
 * Popup cung chua status bar bang statusBarsPadding().
 */

/** BubblePopup in HomeView.swift. */
sealed interface RoomPopup {
    data class Rgb(val entityId: String) : RoomPopup
    data class Climate(val entityId: String) : RoomPopup
}

/** Only these two lights expose colour control in the SwiftUI app. */
fun isRgbLight(entityId: String): Boolean =
    entityId == "light.smartlight" || entityId == "light.table_led"

private fun HomeEntity.attrDouble(key: String): Double? =
    attrString(key)?.toDoubleOrNull()

@Composable
private fun PopupShell(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(onClick = onDismiss)
                .statusBarsPadding(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .padding(horizontal = 20.dp)
                    .widthIn(max = 360.dp)
                    .glassSurface(radius = 35.dp)
                    // Swallow taps so the card itself never closes the popup.
                    .clickable(enabled = false) {},
            ) {
                content()
            }
        }
    }
}

@Composable
private fun CloseButton(onDismiss: () -> Unit) {
    Box(
        Modifier
            .glassPill(16.dp)
            .clickable(onClick = onDismiss)
            .padding(horizontal = 20.dp, vertical = 6.dp),
    ) {
        Text("\u0110\u00d3NG", fontSize = 13.sp, color = HumeColors.TextSecondary)
    }
}

/* ------------------------------ climate ------------------------------ */

private data class ClimateMode(val key: String, val label: String, val icon: ImageVector, val color: Color)

private val climateModes = listOf(
    ClimateMode("off", "T\u1eaft", Ph.PowerButton, Color(0xFF8E8E93)),
    ClimateMode("cool", "M\u00e1t", Ph.Snowflake, Color(0xFF73B9F2)),
    ClimateMode("dry", "Kh\u00f4", Ph.Drop, Color(0xFFF2D26F)),
    ClimateMode("fan_only", "Qu\u1ea1t", Ph.Fan, Color(0xFF66D19E)),
    ClimateMode("heat_cool", "T\u1ef1 \u0111\u1ed9ng", Ph.Thermometer, Color(0xFFF9784C)),
)

@Composable
fun ClimatePopup(
    entityId: String,
    entities: Map<String, HomeEntity>,
    ha: HomeAssistantRepository,
    onDismiss: () -> Unit,
) {
    val entity = entities[entityId]
    val mode = entity?.state ?: "off"
    val isOn = mode !in setOf("off", "unavailable", "unknown")
    val target = entity?.attrDouble("temperature") ?: 26.0
    val current = entity?.attrDouble("current_temperature")

    PopupShell(onDismiss) {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                entity?.friendly() ?: entityId,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = HumeColors.TextPrimary,
            )

            // Target temperature stepper, clamped to 16..31 like the SwiftUI popup.
            Row(
                Modifier.alpha(if (isOn) 1f else 0.4f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                StepButton(Ph.Minus, isOn && target > 16) {
                    ha.setClimateTemperature(entityId, target - 1)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        target.roundToInt().toString() + "\u00b0",
                        fontSize = 48.sp,
                        lineHeight = 54.sp,
                        fontWeight = FontWeight.Light,
                        color = HumeColors.TextPrimary,
                    )
                    if (current != null) {
                        Text(
                            "Hi\u1ec7n t\u1ea1i " + current.roundToInt() + "\u00b0",
                            fontSize = 12.sp,
                            color = HumeColors.TextSecondary,
                        )
                    }
                }
                StepButton(Ph.Plus, isOn && target < 31) {
                    ha.setClimateTemperature(entityId, target + 1)
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                climateModes.forEach { item ->
                    val active = mode == item.key
                    Column(
                        Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (active) item.color else HumeColors.Background)
                            .clickable { ha.setHvacMode(entityId, item.key) },
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = null,
                            tint = if (active) Color.White else HumeColors.TextSecondary,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            item.label,
                            fontSize = 9.sp,
                            lineHeight = 11.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            color = if (active) Color.White else HumeColors.TextSecondary,
                            maxLines = 1,
                        )
                    }
                }
            }

            CloseButton(onDismiss)
        }
    }
}

@Composable
private fun StepButton(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(HumeColors.Background)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = HumeColors.TextPrimary, modifier = Modifier.size(18.dp))
    }
}

/* -------------------------------- rgb -------------------------------- */

private data class ColorPreset(val name: String, val r: Int, val g: Int, val b: Int)

private val colorPresets = listOf(
    ColorPreset("Tr\u1eafng \u1ea5m", 255, 200, 150),
    ColorPreset("Tr\u1eafng l\u1ea1nh", 230, 240, 255),
    ColorPreset("Xanh l\u00e1", 100, 200, 100),
    ColorPreset("Xanh d\u01b0\u01a1ng", 100, 150, 255),
    ColorPreset("V\u00e0ng", 255, 200, 50),
    ColorPreset("H\u1ed3ng", 255, 150, 200),
    ColorPreset("\u0110\u1ecf", 255, 80, 80),
    ColorPreset("T\u00edm", 200, 100, 255),
)

@Composable
fun RgbPopup(
    entityId: String,
    entities: Map<String, HomeEntity>,
    ha: HomeAssistantRepository,
    onDismiss: () -> Unit,
) {
    val entity = entities[entityId]
    val isOn = entity?.isOn == true
    val brightness = entity?.attrDouble("brightness")?.toInt() ?: 255
    val rgb = (entity?.attributes?.get("rgb_color") as? JsonArray)
        ?.mapNotNull { it.jsonPrimitive.doubleOrNull?.toInt() }
    val minMired = entity?.attrDouble("min_mireds") ?: 153.0
    val maxMired = entity?.attrDouble("max_mireds") ?: 500.0
    val mired = entity?.attrDouble("color_temp") ?: minMired

    PopupShell(onDismiss) {
        Column(
            Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                entity?.friendly() ?: entityId,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = HumeColors.TextPrimary,
            )

            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if (isOn) Color.Yellow.copy(alpha = 0.20f) else HumeColors.Background)
                    .border(
                        1.dp,
                        if (isOn) Color.Yellow else HumeColors.TextSecondary,
                        CircleShape,
                    )
                    .clickable { ha.toggle(entityId) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Ph.Lightbulb,
                    contentDescription = null,
                    tint = if (isOn) Color.Yellow else HumeColors.TextSecondary,
                    modifier = Modifier.size(24.dp),
                )
            }

            Column(Modifier.fillMaxWidth()) {
                Text(
                    "\u0110\u1ed9 s\u00e1ng: " + (brightness * 100 / 255) + "%",
                    fontSize = 12.sp,
                    color = HumeColors.TextSecondary,
                )
                Slider(
                    value = brightness / 255f,
                    onValueChange = { ha.setLightBrightness(entityId, (it * 100).roundToInt()) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = HumeColors.Orange,
                        activeTrackColor = HumeColors.Orange,
                    ),
                )
            }

            if (maxMired > minMired) {
                val warmth = ((mired - minMired) / (maxMired - minMired)).toFloat()
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        "Nhi\u1ec7t \u0111\u1ed9 m\u00e0u: " + when {
                            warmth > 0.6f -> "\u1ea4m"
                            warmth < 0.4f -> "L\u1ea1nh"
                            else -> "Trung t\u00ednh"
                        },
                        fontSize = 12.sp,
                        color = HumeColors.TextSecondary,
                    )
                    Slider(
                        value = mired.toFloat(),
                        // Home Assistant takes kelvin now, mireds are the legacy unit.
                        onValueChange = { ha.setLightColorTemp(entityId, (1_000_000f / it).roundToInt()) },
                        valueRange = minMired.toFloat()..maxMired.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFB74D),
                            activeTrackColor = Color(0xFFFFF9C4),
                            inactiveTrackColor = Color(0xFF4FC3F7).copy(alpha = 0.5f),
                        ),
                    )
                }
            }

            colorPresets.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { preset ->
                        val active = rgb != null && rgb.size >= 3 &&
                            abs(rgb[0] - preset.r) < 30 &&
                            abs(rgb[1] - preset.g) < 30 &&
                            abs(rgb[2] - preset.b) < 30
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(preset.r, preset.g, preset.b))
                                    .border(
                                        2.dp,
                                        if (active) Color.White else Color.Transparent,
                                        RoundedCornerShape(12.dp),
                                    )
                                    .clickable {
                                        ha.callService(
                                            "light",
                                            "turn_on",
                                            "{\"entity_id\":\"" + entityId + "\",\"rgb_color\":[" +
                                                preset.r + "," + preset.g + "," + preset.b + "]}",
                                            entityId,
                                        )
                                    },
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                preset.name,
                                fontSize = 9.sp,
                                lineHeight = 11.sp,
                                color = HumeColors.TextPrimary,
                                maxLines = 1,
                            )
                        }
                    }
                    if (row.size < 4) Spacer(Modifier.width(0.dp))
                }
            }

            CloseButton(onDismiss)
        }
    }
}
