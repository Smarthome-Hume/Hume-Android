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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.smarthome.hume.core.scene.ManagedKind
import com.smarthome.hume.core.scene.ManagedListsStore
import com.smarthome.hume.ui.manage.ManageListSheet
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeIcons
import com.smarthome.hume.ui.theme.Ph
import com.smarthome.hume.ui.theme.glassSurface
import java.util.Locale

/*
 * SO DO CHOT CUA POPUP CHI TIET PHONG (yeu cau moi):
 *   - KHONG con lop nen kinh bao ngoai nhom the nho: cac the nam truc tiep
 *     tren mat sheet, chi cach nhau bang khoang trong.
 *   - The dieu khien (nut an) : 200 -> 150dp, pill trang thai 48 -> 34dp.
 *   - The nhiet do / do am    : cao co dinh 72dp (truoc ~92), sparkline 50 -> 30.
 *   - Sheet chua status bar bang statusBarsPadding() nen khong de len thanh
 *     thong bao cua may nua.
 *   - Icon: toan bo dung Phosphor net mong (Ph.*), khong con Material dac.
 */
private val DeviceCardHeight = 150.dp
private val DeviceCardPill = 34.dp
private val SensorCardHeight = 72.dp

/**
 * Room detail sheet ported from BubbleRoomView in HomeView.swift.
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        // GAP voi dinh may: sheet khong bao gio trum len thanh thong bao.
        modifier = Modifier.statusBarsPadding(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 40.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(HumeColors.Background),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        HumeIcons.room(room.icon),
                        contentDescription = null,
                        tint = HumeColors.TextPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    config?.label ?: room.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = HumeColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(HumeColors.Background)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Ph.X,
                        contentDescription = "\u0110\u00f3ng",
                        tint = HumeColors.TextPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // KHONG con GroupGlassContainer bao ngoai: hai the nam truc tiep tren sheet.
            Column(
                Modifier.fillMaxWidth(),
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

            Spacer(Modifier.height(14.dp))
            Text("Thi\u1ebft b\u1ecb", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = HumeColors.TextPrimary)
            Spacer(Modifier.height(8.dp))

            val devices = if (config == null) {
                listOf(DeviceConfig.toggle(room.lightEntity, "\u0110\u00e8n", room.lightEntity, "bulb"))
            } else {
                // orderedDevices in SwiftUI: the air conditioner comes first.
                config.devices.filter { it.type == "climate" } +
                    config.devices.filter { it.type != "climate" }
            }

            // Luoi thiet bi cung KHONG con lop nen bao ngoai.
            DeviceGrid(devices, entities, ha) { popup = it }
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
        if (index > 0) Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        icon = Ph.Snowflake,
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

/**
 * The dieu khien: cao 150dp (truoc 200), radius 35, pill trang thai 34dp
 * (truoc 48). Icon circle 40 (icon 20), ten 14sp, dong phu 11sp mot dong.
 */
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
            .height(DeviceCardHeight)
            .glassSurface(radius = 35.dp)
            .border(
                1.dp,
                if (isOn) accent.copy(alpha = 0.40f) else HumeColors.Divider,
                RoundedCornerShape(35.dp),
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Box {
                Box(
                    Modifier
                        .size(40.dp)
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
                        modifier = Modifier.size(20.dp),
                    )
                }
                // The little hand badge tells the user this icon opens a popup.
                if (onIconDoubleTap != null) {
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .size(15.dp)
                            .clip(CircleShape)
                            .background(HumeColors.Card),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Ph.HandTap,
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
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            fontSize = 14.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Bold,
            color = HumeColors.TextPrimary,
            maxLines = 1,
        )
        Text(
            sub,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            color = HumeColors.TextPrimary.copy(alpha = 0.6f),
            maxLines = 1,
        )
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .fillMaxWidth()
                .height(DeviceCardPill)
                .clip(RoundedCornerShape(17.dp))
                .background(HumeColors.Background),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                bottom,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = HumeColors.TextPrimary,
                maxLines = 1,
            )
        }
    }
}

private enum class SensorKind { Temperature, Humidity }

/**
 * The nhiet do / do am: cao co dinh 72dp (truoc ~92), radius 40, vong icon 42
 * (icon 20), so do 24sp, ten 11sp, sparkline 24h cao 30dp o day the.
 *
 * SUA: hang noi dung (vong icon + so do + ten) truoc day nam o TREN CUNG cua
 * Box (mac dinh TopStart) nen vong icon bi lech len, khong dong tam voi duong
 * bo cua the. Nay hang duoc canh GIUA theo chieu doc bang .align(Center) nen
 * tam vong icon trung dung tam the.
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

    Box(Modifier.fillMaxWidth().height(SensorCardHeight).glassSurface(radius = 40.dp)) {
        if (entityId != null) {
            MiniSparkline(
                entityId = entityId,
                color = color,
                ha = ha,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(30.dp)
                    .padding(horizontal = 4.dp),
            )
        }
        Row(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(HumeColors.Background),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Row {
                    Text(
                        if (value != null) String.format(Locale.US, "%.1f", value) else "--",
                        fontSize = 24.sp,
                        lineHeight = 27.sp,
                        color = HumeColors.TextPrimary,
                        modifier = Modifier.alignByBaseline(),
                    )
                    if (unit.isNotBlank()) {
                        Spacer(Modifier.width(3.dp))
                        Text(
                            unit,
                            fontSize = 12.sp,
                            lineHeight = 27.sp,
                            color = HumeColors.TextPrimary.copy(alpha = 0.5f),
                            modifier = Modifier.alignByBaseline(),
                        )
                    }
                }
                Text(name, fontSize = 11.sp, lineHeight = 13.sp, color = HumeColors.TextSecondary)
            }
            Icon(
                Ph.ArrowUpRight,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp),
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

/** IconMapper.swift equivalents \u2014 toan bo dung Phosphor net mong. */
private fun deviceIcon(key: String): ImageVector = when (key) {
    "bulb", "lightbulb" -> Ph.Lightbulb
    "sun" -> Ph.Sun
    "desk" -> Ph.Desk
    "switch" -> Ph.ToggleRight
    "fan" -> Ph.Fan
    "stairs" -> Ph.Stairs
    "plug" -> Ph.Plug
    "fire" -> Ph.Fire
    "cooking" -> Ph.CookingPot
    "snowflake" -> Ph.Snowflake
    "washer", "dryer", "dishwasher" -> Ph.Washer
    else -> Ph.ToggleRight
}

private val LightsPopupYellow = Color(0xFFFFC107)

/**
 * LightsPopupView from HomeView.swift. The source of truth is the managed light
 * list (24 seeded entities), not the eight room cards, and only the ones that
 * are currently on are listed.
 */
@Composable
fun LightsBottomSheet(
    rooms: List<RoomConfig>,
    ha: HomeAssistantRepository,
    entities: Map<String, HomeEntity>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val store = remember(context) { ManagedListsStore.get(context) }
    val lights by store.lights.collectAsStateWithLifecycle()
    val active = lights.filter { !it.hidden && entities[it.id]?.isOn == true }
    var manage by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.statusBarsPadding(),
    ) {
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
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { manage = true }) {
                    Icon(
                        Ph.Gear,
                        contentDescription = "Qu\u1ea3n l\u00fd \u0111\u00e8n",
                        tint = HumeColors.Orange,
                        modifier = Modifier.size(18.dp),
                    )
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
                                    Ph.PowerButton,
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

    if (manage) {
        ManageListSheet(kind = ManagedKind.LIGHTS, ha = ha, onDismiss = { manage = false })
    }
}
