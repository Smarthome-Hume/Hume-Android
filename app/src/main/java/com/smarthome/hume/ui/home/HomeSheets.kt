@file:OptIn(ExperimentalMaterial3Api::class)

package com.smarthome.hume.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Desk
import androidx.compose.material.icons.rounded.ElectricalServices
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocalLaundryService
import androidx.compose.material.icons.rounded.SoupKitchen
import androidx.compose.material.icons.rounded.Stairs
import androidx.compose.material.icons.rounded.ToggleOn
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.DeviceConfig
import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.core.model.RoomBubbleConfig
import com.smarthome.hume.core.model.RoomConfig
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeIcons
import java.util.Locale

/**
 * Room detail sheet ported from BubbleRoomView in HomeView.swift:
 * two large sensor cards, then a two column grid of 200dp device cards.
 * The device list is hardcoded per room exactly like RoomBubbleConfig.
 */
@Composable
fun RoomBottomSheet(
    room: RoomConfig,
    ha: HomeAssistantRepository,
    entities: Map<String, HomeEntity>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val config = RoomBubbleConfig.find(room.rawKey)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, bottom = 32.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(HumeColors.Background),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(HumeIcons.room(room.icon), contentDescription = null, tint = HumeColors.TextPrimary)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    config?.label ?: room.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HumeColors.TextPrimary,
                )
            }
            Spacer(Modifier.height(14.dp))

            SensorBigCard(
                name = "Nhi\u1ec7t \u0111\u1ed9",
                entity = entities[config?.tempEntity ?: room.tempEntity],
                icon = HumeIcons.Temperature,
                kind = SensorKind.Temperature,
            )
            Spacer(Modifier.height(8.dp))
            SensorBigCard(
                name = "\u0110\u1ed9 \u1ea9m",
                entity = entities[config?.humidityEntity ?: room.humidityEntity],
                icon = HumeIcons.Humidity,
                kind = SensorKind.Humidity,
            )

            Spacer(Modifier.height(16.dp))
            Text("Thi\u1ebft b\u1ecb", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = HumeColors.TextPrimary)
            Spacer(Modifier.height(10.dp))

            if (config == null) {
                // Fallback for rooms without a hardcoded device list.
                DeviceGrid(
                    devices = listOf(
                        DeviceConfig.toggle(room.lightEntity, "\u0110\u00e8n", room.lightEntity, "bulb"),
                    ),
                    entities = entities,
                    ha = ha,
                )
            } else {
                // Climate cards first, exactly like orderedDevices in SwiftUI.
                val ordered = config.devices.filter { it.type == "climate" } +
                    config.devices.filter { it.type != "climate" }
                DeviceGrid(devices = ordered, entities = entities, ha = ha)
            }
        }
    }
}

@Composable
private fun DeviceGrid(
    devices: List<DeviceConfig>,
    entities: Map<String, HomeEntity>,
    ha: HomeAssistantRepository,
) {
    devices.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { device ->
                Box(Modifier.weight(1f)) {
                    if (device.type == "climate") {
                        ClimateSquareCard(device, entities, ha)
                    } else {
                        ToggleSquareCard(device, entities, ha)
                    }
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun ToggleSquareCard(
    device: DeviceConfig,
    entities: Map<String, HomeEntity>,
    ha: HomeAssistantRepository,
) {
    val isOn = entities[device.entity]?.isOn == true
    val watts = device.powerEntity?.let { entities[it]?.numericState?.toInt() }
    SquareCard(
        icon = deviceIcon(device.icon),
        label = device.label,
        sub = device.sub,
        isOn = isOn,
        bottom = when {
            !isOn -> "T\u1eaeT"
            watts != null -> watts.toString() + "W"
            else -> "B\u1eacT"
        },
        onToggle = { setLight(ha, device.entity, !isOn) },
    )
}

@Composable
private fun ClimateSquareCard(
    device: DeviceConfig,
    entities: Map<String, HomeEntity>,
    ha: HomeAssistantRepository,
) {
    val mode = entities[device.entity]?.state ?: "off"
    val isOn = mode !in setOf("off", "unavailable", "unknown")
    val target = entities.attr(device.entity, "temperature")?.toDoubleOrNull() ?: 26.0
    val modeText = when (mode) {
        "cool" -> "M\u00e1t"
        "heat" -> "\u1ea4m"
        "dry" -> "Kh\u00f4"
        "fan_only" -> "Qu\u1ea1t"
        "auto", "heat_cool" -> "T\u1ef1 \u0111\u1ed9ng"
        else -> "T\u1eaeT"
    }
    SquareCard(
        icon = Icons.Rounded.AcUnit,
        label = device.label,
        sub = device.sub,
        isOn = isOn,
        bottom = if (isOn) modeText + " - " + target.toInt() + "\u00b0" else "T\u1eaeT",
        onToggle = { ha.setHvacMode(device.entity, if (isOn) "off" else "cool") },
    )
}

@Composable
private fun SquareCard(
    icon: ImageVector,
    label: String,
    sub: String,
    isOn: Boolean,
    bottom: String,
    onToggle: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(if (isOn) HumeColors.Orange.copy(alpha = 0.10f) else Color.White)
            .border(
                1.dp,
                if (isOn) HumeColors.Orange.copy(alpha = 0.40f) else HumeColors.Divider,
                RoundedCornerShape(30.dp),
            )
            .padding(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(HumeColors.Background),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isOn) HumeColors.Orange else HumeColors.TextPrimary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Switch(checked = isOn, onCheckedChange = { onToggle() })
        }
        Spacer(Modifier.height(10.dp))
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HumeColors.TextPrimary, maxLines = 1)
        Text(sub, fontSize = 11.sp, color = HumeColors.TextSecondary, maxLines = 2)
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(HumeColors.Background),
            contentAlignment = Alignment.Center,
        ) {
            Text(bottom, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = HumeColors.TextPrimary)
        }
    }
}

private enum class SensorKind { Temperature, Humidity }

@Composable
private fun SensorBigCard(name: String, entity: HomeEntity?, icon: ImageVector, kind: SensorKind) {
    val value = entity?.numericState
    val unit = entity?.attrString("unit_of_measurement").orEmpty()
    val color = when {
        value == null -> HumeColors.TextSecondary
        kind == SensorKind.Temperature -> when {
            value < 20 -> Color(0xFF4FC3F7)
            value <= 25 -> Color(0xFF66BB6A)
            value <= 30 -> Color(0xFFFFA726)
            else -> Color(0xFFEF5350)
        }
        else -> when {
            value < 40 -> Color(0xFFFFCA28)
            value <= 70 -> Color(0xFF66BB6A)
            else -> Color(0xFF42A5F5)
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(34.dp))
            .background(Color.White)
            .border(1.dp, HumeColors.Divider, RoundedCornerShape(34.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    if (value != null) String.format(Locale.US, "%.1f", value) else "--",
                    fontSize = 28.sp,
                    color = HumeColors.TextPrimary,
                )
                if (unit.isNotBlank()) {
                    Spacer(Modifier.width(4.dp))
                    Text(unit, fontSize = 13.sp, color = HumeColors.TextSecondary)
                }
            }
            Text(name, fontSize = 13.sp, color = HumeColors.TextSecondary)
        }
    }
}

/** IconMapper.swift equivalents for the device list. */
private fun deviceIcon(key: String): ImageVector = when (key) {
    "bulb" -> Icons.Rounded.Lightbulb
    "sun" -> Icons.Rounded.WbSunny
    "desk" -> Icons.Rounded.Desk
    "switch" -> Icons.Rounded.ToggleOn
    "fan" -> Icons.Rounded.Air
    "stairs" -> Icons.Rounded.Stairs
    "plug" -> Icons.Rounded.ElectricalServices
    "fire" -> Icons.Rounded.LocalFireDepartment
    "cooking" -> Icons.Rounded.SoupKitchen
    "snowflake" -> Icons.Rounded.AcUnit
    "washer", "dryer", "dishwasher" -> Icons.Rounded.LocalLaundryService
    else -> Icons.Rounded.ToggleOn
}

/** Lights popup from HomeView.swift. */
@Composable
fun LightsBottomSheet(
    rooms: List<RoomConfig>,
    ha: HomeAssistantRepository,
    entities: Map<String, HomeEntity>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("T\u1ea5t c\u1ea3 \u0111\u00e8n", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { setAllLights(ha, rooms, true) }) { Text("B\u1eadt h\u1ebft") }
                    OutlinedButton(onClick = { setAllLights(ha, rooms, false) }) { Text("T\u1eaft h\u1ebft") }
                }
            }
            Spacer(Modifier.height(12.dp))
            rooms.forEach { room ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(HumeIcons.room(room.icon), contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(room.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (entities[room.lightEntity]?.isOn == true) "B\u1eadt" else "T\u1eaft",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = entities[room.lightEntity]?.isOn == true,
                        onCheckedChange = { setLight(ha, room.lightEntity, it) },
                    )
                }
                HorizontalDivider()
            }
        }
    }
}
