package com.smarthome.hume.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.smarthome.hume.core.model.ClimateUi
import com.smarthome.hume.core.model.DeviceUi
import com.smarthome.hume.core.model.RoomUi
import com.smarthome.hume.core.ui.components.M3ECard
import com.smarthome.hume.core.ui.components.M3EConnectedButtonGroup
import com.smarthome.hume.core.ui.components.M3EIcons
import com.smarthome.hume.core.ui.components.M3ESwitch

/** Luoi 2 cot the phong compact. Phong co den bat thi fill primaryContainer. */
@Composable
fun RoomGrid(
    rooms: List<RoomUi>,
    onRoom: (RoomUi) -> Unit,
    onToggleLight: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Phòng", style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 4.dp))
        rooms.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { room ->
                    RoomCard(
                        room = room,
                        onOpen = { onRoom(room) },
                        onToggleLight = { room.lightEntityId?.let(onToggleLight) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RoomCard(
    room: RoomUi,
    onOpen: () -> Unit,
    onToggleLight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    M3ECard(
        onClick = onOpen,
        containerColor = if (room.lightOn) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHighest,
        contentPadding = 16.dp,
        modifier = modifier,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                M3EIcons.room(room.iconKey), null,
                tint = if (room.lightOn) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(room.name, style = MaterialTheme.typography.titleSmall,
                    color = if (room.lightOn) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface)
                val sub = buildList {
                    room.tempC?.let { add("%.0f°".format(it)) }
                    add("${room.devicesOn}/${room.deviceCount} bật")
                }.joinToString(" · ")
                Text(sub, style = MaterialTheme.typography.bodySmall,
                    color = if (room.lightOn) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // Nut den: bat thi fill dac primary
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (room.lightOn) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                    .clickable(onClick = onToggleLight),
            ) {
                Icon(
                    M3EIcons.Light, null,
                    tint = if (room.lightOn) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

/** Sheet chi tiet phong: nhiet do/do am, dieu hoa, danh sach thiet bi. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomSheet(
    room: RoomUi,
    onDismiss: () -> Unit,
    onToggle: (String) -> Unit,
    onClimateTemp: (String, Double) -> Unit,
    onHvacMode: (String, String) -> Unit,
    onToggleClimate: (String) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 44.dp, topEnd = 44.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        LazyColumn(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(M3EIcons.room(room.iconKey), null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(room.name, style = MaterialTheme.typography.headlineSmall)
                }
            }
            // 2 the mini nhiet do / do am
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    EnvTile(
                        icon = Icons.Outlined.Thermostat, label = "Nhiệt độ",
                        value = room.tempC?.let { "%.1f°".format(it) } ?: "—",
                        container = MaterialTheme.colorScheme.primaryContainer,
                        onContainer = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                    EnvTile(
                        icon = Icons.Outlined.WaterDrop, label = "Độ ẩm",
                        value = room.humidityPct?.let { "%.0f%%".format(it) } ?: "—",
                        container = MaterialTheme.colorScheme.tertiaryContainer,
                        onContainer = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            // Dieu hoa
            room.climate?.let { c ->
                item { ClimateCard(c, onClimateTemp, onHvacMode, onToggleClimate) }
            }
            // Thiet bi
            if (room.devices.isNotEmpty()) {
                item {
                    Text("Thiết bị", style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp))
                }
                items(room.devices, key = { it.entityId }) { d ->
                    DeviceRow(d, onToggle = { onToggle(d.entityId) })
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun EnvTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    container: androidx.compose.ui.graphics.Color,
    onContainer: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    M3ECard(contentPadding = 16.dp, shape = RoundedCornerShape(30.dp), modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(container),
            ) {
                Icon(icon, null, tint = onContainer, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(value, style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium)
                Text(label, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private val hvacLabels = mapOf(
    "cool" to "Làm lạnh",
    "heat" to "Sưởi",
    "auto" to "Tự động",
    "fan_only" to "Quạt",
    "dry" to "Hút ẩm",
)

/** The dieu hoa — dung demo rev12 (.ac-top): stepper + nut nguon, khong title. */
@Composable
private fun ClimateCard(
    c: ClimateUi,
    onTemp: (String, Double) -> Unit,
    onMode: (String, String) -> Unit,
    onToggle: (String) -> Unit,
) {
    val target = c.targetTemp ?: 26.0
    M3ECard(
        shape = RoundedCornerShape(24.dp),
        contentPadding = 16.dp,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (c.isOn) 1f else 0.55f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Stepper: - | nhiet do + "Muc tieu" | +
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepperButton(Icons.Outlined.Remove) {
                    onTemp(c.entityId, (target - 1).coerceIn(16.0, 31.0))
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    Text("%.0f°".format(target),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold)
                    Text("Mục tiêu", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StepperButton(Icons.Outlined.Add) {
                    onTemp(c.entityId, (target + 1).coerceIn(16.0, 31.0))
                }
            }
            // Nut nguon tron 54dp primaryContainer
            val cs = MaterialTheme.colorScheme
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(if (c.isOn) cs.primaryContainer else cs.surfaceContainerHigh)
                    .clickable { onToggle(c.entityId) },
            ) {
                Icon(
                    Icons.Outlined.PowerSettingsNew, null,
                    tint = if (c.isOn) cs.onPrimaryContainer else cs.onSurfaceVariant,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
    val modes = c.modes.filter { it in hvacLabels }.takeIf { it.isNotEmpty() }
        ?: listOf("cool", "heat", "auto", "fan_only")
    val selectedMode = c.hvacMode.takeIf { it in modes } ?: modes.first()
    M3ECard(contentPadding = 12.dp, modifier = Modifier.fillMaxWidth()) {
        M3EConnectedButtonGroup(
            options = modes,
            selected = selectedMode,
            onSelect = { onMode(c.entityId, it) },
            label = { hvacLabels[it] ?: it },
        )
    }
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp))
    }
}

/** 1 hang thiet bi — dung demo rev12 (.dev): ten + trang thai + switch. */
@Composable
fun DeviceRow(
    d: DeviceUi,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    M3ECard(
        contentPadding = 16.dp,
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (d.isOn) cs.primaryContainer
                        else cs.surfaceContainerHigh,
                    ),
            ) {
                Icon(
                    M3EIcons.device(d.iconKey), null,
                    tint = if (d.isOn) cs.onPrimaryContainer
                    else cs.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(d.label, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold)
                Text(
                    if (d.isOn) "Đang bật" else "Đang tắt",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            }
            M3ESwitch(checked = d.isOn, onCheckedChange = { onToggle() })
        }
    }
}
