@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.smarthome.hume.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.ArrowOutward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Desk
import androidx.compose.material.icons.rounded.ElectricalServices
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocalLaundryService
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.SoupKitchen
import androidx.compose.material.icons.rounded.Stairs
import androidx.compose.material.icons.rounded.ToggleOn
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.Canvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.DeviceConfig
import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.core.model.RoomBubbleConfig
import com.smarthome.hume.core.model.RoomConfig
import com.smarthome.hume.core.scene.ManagedListsStore
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeIcons
import com.smarthome.hume.ui.theme.glassSurface
import java.util.Locale

/**
 * Room detail sheet ported from BubbleRoomView in HomeView.swift.
 *
 * Layout of the original, top to bottom: a header with a 48pt icon circle, the
 * room name at 26pt and a 44pt close button, one glass group holding the two
 * sensor cards, the "Thiet bi" title, then a second glass group with a two
 * column grid of 200pt square device cards, climate first. Double tapping the
 * icon of an RGB light or an air conditioner opens a popup on top of the sheet.
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
    var popup by remember { mutableStateOf<RoomPopup?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, dragHandle = null) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 40.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(HumeColors.Background),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        HumeIcons.room(room.icon),
                        contentDescription = null,
                        tint = HumeColors.TextPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    config?.label ?: room.name,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = HumeColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(HumeColors.Background)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "\u0110\u00f3ng",
                        tint = HumeColors.TextPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            // GroupGlassContainer(cornerRadius: 52, innerPadding: 10, spacing: 8)
            Column(
                Modifier.fillMaxWidth().glassSurface(radius = 52.dp).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SensorBigCard(
                    name = "Nhi\u1ec7t \u0111\u1ed9",
                    entityId = config?.tempEntity ?: room.tempEntity,
                    entities = entities,
                    ha = ha,
                    icon = HumeIcons.Temperature,
                    kind = SensorKind.Temperature,
                )
                SensorBigCard(
                    name = "\u0110\u1ed9 \u1ea9m",
                    entityId = config?.humidityEntity ?: room.humidityEntity,
                    entities = entities,
                    ha = ha,
                    icon = HumeIcons.Humidity,
                    kind = SensorKind.Humidity,
                )
            }

            Spacer(Modifier.height(16.dp))
            Text("Thi\u1ebft b\u1ecb", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = HumeColors.TextPrimary)
            Spacer(Modifier.height(10.dp))

            val devices = if (config == null) {
                listOf(DeviceConfig.toggle(room.lightEntity, "\u0110\u00e8n", room.lightEntity, "bulb"))
            } else {
                // orderedDevices in SwiftUI: the air conditioner comes first.
                config.devices.filter { it.type == "climate" } +
                    config.devices.filter { it.type != "climate" }
            }

            // GroupGlassContainer(cornerRadius: 47, innerPadding: 10, spacing: 0)
            Column(Modifier.fillMaxWidth().glassSurface(radius = 47.dp).padding(10.dp)) {
                DeviceGrid(devices, entities, ha) { popup = it }
            }
        }
    }

    when (val open = popup) {
        is RoomPopup.Rgb -> RgbPopup(open.entityId, entities, ha) { popup = null }
        is RoomPopup.Climate -> ClimatePopup(open.entityId, entities, ha) { popup = null }
        null -> Unit
    }
}

@Composable
private fun DeviceGrid(
    devices: List<DeviceConfig>,
    entities: Map<String, HomeEntity>,
    ha: HomeAssistantRepository,
    onOpenPopup: (RoomPopup) -> Unit,
) {
    devices.chunked(2).forEachIndexed { index, row ->
        if (index > 0) Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { device ->
                Box(Modifier.weight(1f)) {
                    if (device.type == "climate") {
                        ClimateSquareCard(device, entities, ha, onOpenPopup)
                    } else {
                        ToggleSquareCard(device, entities, ha, onOpenPopup)
                    }
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

/** BubbleToggleCard: the bottom pill shows the live wattage when a power sensor exists. */
@Composable
private fun ToggleSquareCard(
    device: DeviceConfig,
    entities: Map<String, HomeEntity>,
    ha: HomeAssistantRepository,
    onOpenPopup: (RoomPopup) -> Unit,
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
        onToggle = { ha.toggle(device.entity) },
        onIconDoubleTap = if (isRgbLight(device.entity)) {
            { onOpenPopup(RoomPopup.Rgb(device.entity)) }
        } else null,
    )
}

/** BubbleClimateCard: the toggle switches between off and cool, never turn_on. */
@Composable
private fun ClimateSquareCard(
    device: DeviceConfig,
    entities: Map<String, HomeEntity>,
    ha: HomeAssistantRepository,
    onOpenPopup: (RoomPopup) -> Unit,
) {
    val mode = entities[device.entity]?.state ?: "off"
    val isOn = mode !in setOf("off", "unavailable", "unknown")
    val target = entities[device.entity]?.attrString("temperature")?.toDoubleOrNull() ?: 26.0
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
        accent = climateAccent(mode),
        bottom = if (isOn) modeText + " - " + target.toInt() + "\u00b0" else "T\u1eaeT",
        onToggle = { ha.setHvacMode(device.entity, if (isOn) "off" else "cool") },
        onIconDoubleTap = { onOpenPopup(RoomPopup.Climate(device.entity)) },
    )
}

private fun climateAccent(mode: String): Color = when (mode) {
    "cool", "heat_cool" -> Color(0xFF73B9F2)
    "heat" -> Color(0xFFF9784C)
    "dry" -> Color(0xFFF2D26F)
    "fan_only" -> Color(0xFF66D19E)
    else -> HumeColors.Orange
}

/** squareCard() in HomeView.swift: 200pt tall, radius 35, status pill 48pt tall. */
@Composable
private fun SquareCard(
    icon: ImageVector,
    label: String,
    sub: String,
    isOn: Boolean,
    bottom: String,
    onToggle: () -> Unit,
    accent: Color = HumeColors.Orange,
    onIconDoubleTap: (() -> Unit)? = null,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .glassSurface(radius = 35.dp)
            .border(
                1.dp,
                if (isOn) accent.copy(alpha = 0.40f) else HumeColors.Divider,
                RoundedCornerShape(35.dp),
            )
            .padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Box {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(HumeColors.Background)
                        .combinedClickable(
                            enabled = onIconDoubleTap != null,
                            onClick = {},
                            onDoubleClick = { onIconDoubleTap?.invoke() },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (isOn) accent else HumeColors.TextPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                }
                // The little hand badge tells the user this icon opens a popup.
                if (onIconDoubleTap != null) {
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(HumeColors.Card),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.TouchApp,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(9.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Switch(checked = isOn, onCheckedChange = { onToggle() })
        }
        Spacer(Modifier.height(12.dp))
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HumeColors.TextPrimary, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(
            sub,
            fontSize = 12.sp,
            color = HumeColors.TextPrimary.copy(alpha = 0.6f),
            maxLines = 2,
        )
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(25.dp))
                .background(HumeColors.Background),
            contentAlignment = Alignment.Center,
        ) {
            Text(bottom, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = HumeColors.TextPrimary)
        }
    }
}

private enum class SensorKind { Temperature, Humidity }

/**
 * BubbleSensorCard: radius 40, a 52pt glass icon circle, the reading at 30pt and
 * a faded 24 hour sparkline behind everything.
 */
@Composable
private fun SensorBigCard(
    name: String,
    entityId: String?,
    entities: Map<String, HomeEntity>,
    ha: HomeAssistantRepository,
    icon: ImageVector,
    kind: SensorKind,
) {
    val entity = entityId?.let { entities[it] }
    val value = entity?.numericState
    val unit = entity?.unit().orEmpty()
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

    Box(Modifier.fillMaxWidth().glassSurface(radius = 40.dp)) {
        if (entityId != null) {
            MiniSparkline(
                entityId = entityId,
                color = color,
                ha = ha,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(horizontal = 4.dp),
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(HumeColors.Background),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        if (value != null) String.format(Locale.US, "%.1f", value) else "--",
                        fontSize = 30.sp,
                        color = HumeColors.TextPrimary,
                    )
                    if (unit.isNotBlank()) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            unit,
                            fontSize = 14.sp,
                            color = HumeColors.TextPrimary.copy(alpha = 0.5f),
                        )
                    }
                }
                Text(name, fontSize = 14.sp, color = HumeColors.TextSecondary)
            }
            Icon(
                Icons.Rounded.ArrowOutward,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/**
 * MiniSparkline in HomeView.swift: the last 24 hours downsampled to 40 points,
 * drawn as a Catmull-Rom curve with tension 0.3 and a fade underneath.
 */
@Composable
private fun MiniSparkline(
    entityId: String,
    color: Color,
    ha: HomeAssistantRepository,
    modifier: Modifier = Modifier,
) {
    var points by remember(entityId) { mutableStateOf<List<Double>>(emptyList()) }
    LaunchedEffect(entityId) {
        val raw = ha.fetchHistory(entityId, 24)
        if (raw.size < 2) return@LaunchedEffect
        val step = maxOf(1, raw.size / 40)
        points = raw.filterIndexed { index, _ -> index % step == 0 }
            .map { it.value }
            .takeLast(40)
    }
    if (points.size < 2) {
        Box(modifier)
        return
    }

    val min = points.min()
    val max = points.max()
    val range = if (max - min < 0.001) 1.0 else max - min

    Canvas(modifier) {
        val width = size.width
        val height = size.height
        val stepX = width / (points.size - 1)
        val coords = points.mapIndexed { index, value ->
            Offset(
                x = index * stepX,
                y = (height - ((value - min) / range * (height - 4)).toFloat()).coerceIn(0f, height),
            )
        }
        val line = smoothPath(coords, tension = 0.3f)
        val area = Path().apply {
            addPath(line)
            lineTo(coords.last().x, height)
            lineTo(coords.first().x, height)
            close()
        }
        drawPath(
            area,
            Brush.verticalGradient(
                listOf(color.copy(alpha = 0.28f), Color.Transparent),
                startY = 0f,
                endY = height,
            ),
        )
        drawPath(line, color.copy(alpha = 0.75f), style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round))
    }
}

/** Same Catmull-Rom helper the solar chart uses, with the sparkline tension. */
private fun smoothPath(points: List<Offset>, tension: Float): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points.first().x, points.first().y)
    for (index in 0 until points.size - 1) {
        val p0 = points[maxOf(0, index - 1)]
        val p1 = points[index]
        val p2 = points[index + 1]
        val p3 = points[minOf(points.size - 1, index + 2)]
        val c1 = Offset(p1.x + (p2.x - p0.x) / 6f * tension * 2f, p1.y + (p2.y - p0.y) / 6f * tension * 2f)
        val c2 = Offset(p2.x - (p3.x - p1.x) / 6f * tension * 2f, p2.y - (p3.y - p1.y) / 6f * tension * 2f)
        path.cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
    }
    return path
}

/** IconMapper.swift equivalents for the device list. */
private fun deviceIcon(key: String): ImageVector = when (key) {
    "bulb", "lightbulb" -> Icons.Rounded.Lightbulb
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

private val LightsPopupYellow = Color(0xFFFFC107)

/**
 * LightsPopupView from HomeView.swift. The source of truth is the managed light
 * list (24 seeded entities), not the eight room cards, and only the ones that
 * are currently on are listed. Each row has a power button that turns that
 * single light off, which is what the iOS popup does.
 */
@Composable
fun LightsBottomSheet(
    rooms: List<RoomConfig>,
    ha: HomeAssistantRepository,
    entities: Map<String, HomeEntity>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val store = remember { ManagedListsStore.get(LocalContext.current) }
    val lights by store.lights.collectAsStateWithLifecycle()
    val active = lights.filter { !it.hidden && entities[it.id]?.isOn == true }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "\u0110\u00e8n \u0111ang s\u00e1ng",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HumeColors.TextPrimary,
                )
                if (active.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .background(HumeColors.Orange, RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            active.size.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            if (active.isEmpty()) {
                Text(
                    "Kh\u00f4ng c\u00f3 \u0111\u00e8n n\u00e0o \u0111ang s\u00e1ng",
                    fontSize = 14.sp,
                    color = HumeColors.TextSecondary,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    active.forEach { item ->
                        val entity = entities[item.id]
                        val roomName = rooms.firstOrNull { it.lightEntity == item.id }?.name.orEmpty()
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(HumeColors.Background)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(LightsPopupYellow.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    deviceIcon(item.icon),
                                    contentDescription = null,
                                    tint = LightsPopupYellow,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.name.ifEmpty { entity?.friendly() ?: item.id },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = HumeColors.TextPrimary,
                                    maxLines = 2,
                                )
                                if (roomName.isNotEmpty()) {
                                    Text(roomName, fontSize = 10.sp, color = HumeColors.TextSecondary)
                                }
                            }
                            IconButton(onClick = { ha.turnOff(item.id) }) {
                                Icon(
                                    Icons.Rounded.PowerSettingsNew,
                                    contentDescription = "T\u1eaft",
                                    tint = HumeColors.TextSecondary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
