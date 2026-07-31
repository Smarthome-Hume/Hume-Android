@file:OptIn(ExperimentalMaterial3Api::class)

package com.smarthome.hume.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.DefaultRooms
import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.core.model.RoomConfig
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeIcons
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.time.LocalTime
import java.util.Locale

internal const val ALARM_ENTITY = "alarm_control_panel.alarm_security"

/**
 * Home dashboard, ported from Hume/Views/Home/HomeView.swift.
 *
 * GreetingHeader -> AlarmLightsCard -> RoomGrid -> RoomBottomSheet / LightsBottomSheet.
 * SolarEnergyCard, SceneSection and ChartDialog land in the next slice.
 */
@Composable
fun HomeScreen(ha: HomeAssistantRepository) {
    val entities by ha.entities.collectAsState()
    val connected by ha.connected.collectAsState()
    val lastError by ha.lastError.collectAsState()
    val rooms = remember { DefaultRooms.climateRooms + DefaultRooms.basicRooms }

    var roomSheet by remember { mutableStateOf<RoomConfig?>(null) }
    var lightsSheet by remember { mutableStateOf(false) }

    val lightsOn = rooms.count { entities[it.lightEntity]?.isOn == true }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            GreetingHeader(
                connected = connected,
                entityCount = entities.size,
                error = lastError,
                onRefresh = { ha.refresh() },
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            AlarmLightsCard(
                alarmState = entities[ALARM_ENTITY]?.state,
                lightsOn = lightsOn,
                totalLights = rooms.size,
                onOpenLights = { lightsSheet = true },
                onAllOff = { setAllLights(ha, rooms, false) },
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                "Phòng",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp),
            )
        }
        items(rooms, key = { it.name }) { room ->
            RoomCard(
                room = room,
                entities = entities,
                onToggle = { toggleLight(ha, room, entities) },
                onOpen = { roomSheet = room },
            )
        }
    }

    roomSheet?.let { room ->
        RoomBottomSheet(room = room, ha = ha, entities = entities, onDismiss = { roomSheet = null })
    }
    if (lightsSheet) {
        LightsBottomSheet(rooms = rooms, ha = ha, entities = entities, onDismiss = { lightsSheet = false })
    }
}

@Composable
private fun GreetingHeader(
    connected: Boolean,
    entityCount: Int,
    error: String?,
    onRefresh: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(greeting(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    if (connected) "Realtime · $entityCount entities" else "REST · $entityCount entities",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (connected) HumeColors.Green else HumeColors.Amber,
                )
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Làm mới")
            }
        }
        error?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..10 -> "Chào buổi sáng"
    in 11..13 -> "Chào buổi trưa"
    in 14..17 -> "Chào buổi chiều"
    else -> "Chào buổi tối"
}

@Composable
private fun AlarmLightsCard(
    alarmState: String?,
    lightsOn: Int,
    totalLights: Int,
    onOpenLights: () -> Unit,
    onAllOff: () -> Unit,
) {
    val armed = alarmState != null && alarmState != "disarmed"
    ElevatedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                StatusTile(
                    icon = HumeIcons.Alarm,
                    title = "An ninh",
                    value = alarmLabel(alarmState),
                    tint = if (armed) HumeColors.Green else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                StatusTile(
                    icon = HumeIcons.Light,
                    title = "Đèn",
                    value = if (lightsOn == 0) "Tất cả đã tắt" else "$lightsOn/$totalLights đang bật",
                    tint = if (lightsOn > 0) HumeColors.Amber else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = onOpenLights, modifier = Modifier.weight(1f)) { Text("Quản lý đèn") }
                OutlinedButton(onClick = onAllOff, modifier = Modifier.weight(1f), enabled = lightsOn > 0) { Text("Tắt tất cả") }
            }
        }
    }
}

@Composable
private fun StatusTile(
    icon: ImageVector,
    title: String,
    value: String,
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun RoomCard(
    room: RoomConfig,
    entities: Map<String, HomeEntity>,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
) {
    val isOn = entities[room.lightEntity]?.isOn == true
    val container by animateColorAsState(
        targetValue = if (isOn) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "roomCardContainer",
    )
    Card(
        onClick = onOpen,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = container,
            contentColor = if (isOn) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        ),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(HumeIcons.room(room.icon), contentDescription = null, modifier = Modifier.size(20.dp))
                }
                Switch(checked = isOn, onCheckedChange = { onToggle() })
            }
            Spacer(Modifier.height(10.dp))
            Text(room.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (isOn) "Đèn bật" else "Đèn tắt",
                style = MaterialTheme.typography.bodySmall,
                color = if (isOn) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Metric(HumeIcons.Temperature, entities.num(room.tempEntity, 1) + "°")
                Spacer(Modifier.width(10.dp))
                Metric(HumeIcons.Humidity, entities.num(room.humidityEntity, 0) + "%")
            }
        }
    }
}

@Composable
internal fun Metric(icon: ImageVector, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

/* ---------- helpers shared with the bottom sheets ---------- */

internal fun toggleLight(ha: HomeAssistantRepository, room: RoomConfig, entities: Map<String, HomeEntity>) {
    val isOn = entities[room.lightEntity]?.isOn == true
    ha.callService(
        room.lightEntity.substringBefore('.'),
        if (isOn) "turn_off" else "turn_on",
        """{"entity_id":"${room.lightEntity}"}""",
        room.lightEntity,
    )
}

internal fun setLight(ha: HomeAssistantRepository, entityId: String, on: Boolean) {
    ha.callService(
        entityId.substringBefore('.'),
        if (on) "turn_on" else "turn_off",
        """{"entity_id":"$entityId"}""",
        entityId,
    )
}

internal fun setAllLights(ha: HomeAssistantRepository, rooms: List<RoomConfig>, on: Boolean) {
    rooms.forEach { setLight(ha, it.lightEntity, on) }
}

internal fun Map<String, HomeEntity>.num(entityId: String, digits: Int): String {
    val value = this[entityId]?.numericState ?: return "--"
    return String.format(Locale.US, "%." + digits + "f", value)
}

internal fun Map<String, HomeEntity>.attr(entityId: String, key: String): String? =
    (this[entityId]?.attributes?.get(key) as? JsonPrimitive)?.contentOrNull

internal fun alarmLabel(state: String?): String = when (state) {
    "disarmed" -> "Đã tắt"
    "armed_home" -> "Bảo vệ ở nhà"
    "armed_away" -> "Bảo vệ đi vắng"
    "armed_night" -> "Bảo vệ ban đêm"
    "armed_vacation" -> "Bảo vệ nghỉ dài"
    "arming", "pending" -> "Đang kích hoạt"
    "triggered" -> "Đang báo động"
    null -> "không rõ"
    else -> state
}
