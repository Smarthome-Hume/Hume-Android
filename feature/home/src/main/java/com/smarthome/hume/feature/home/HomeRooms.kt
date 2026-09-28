package com.smarthome.hume.feature.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.model.ClimateUi
import com.smarthome.hume.core.model.DeviceUi
import com.smarthome.hume.core.model.HomeNotification
import com.smarthome.hume.core.model.RoomUi
import com.smarthome.hume.core.ui.theme.LocalHumeExtraColors
import com.smarthome.hume.core.ui.components.M3EIcons
import com.smarthome.hume.core.ui.components.MsIcon
import com.smarthome.hume.core.ui.components.M3EMotion
import com.smarthome.hume.core.ui.components.M3ESwitch
import com.smarthome.hume.core.ui.components.pressMorph
import com.smarthome.hume.core.ui.components.rememberHaptic
import com.smarthome.hume.core.ui.components.rememberNeighborPress

/**
 * Luoi the phong theo demo rev12 (.roomc): layout DOC — nut den 48px tren,
 * ten 13.5px/700, sub 11.5px/500; padding 16px 14px; cham bao cua mo .rdot2;
 * :active{scale(.95); radius 20px} spring; rise stagger .36s + i*.03s.
 * (Tieu de "Phong" do HomeScreen ve — khong ve trung o day.)
 */
@Composable
fun RoomGrid(
    rooms: List<RoomUi>,
    notifications: List<HomeNotification>,
    onRoom: (RoomUi) -> Unit,
    onToggleLight: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rooms.chunked(2).forEachIndexed { ri, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEachIndexed { ci, room ->
                    RoomCard(
                        room = room,
                        doorOpen = doorOpenForRoom(room, notifications),
                        onOpen = { onRoom(room) },
                        onToggleLight = { room.lightEntityId?.let(onToggleLight) },
                        riseDelayMs = 360 + (ri * 2 + ci) * 30,
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
    doorOpen: Boolean,
    onOpen: () -> Unit,
    onToggleLight: () -> Unit,
    riseDelayMs: Int,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val haptic = rememberHaptic()
    Box(
        modifier = modifier
            .riseIn(riseDelayMs)
            .pressMorphCard(
                pressedScale = 0.95f,
                corner = 28.dp,
                pressedCorner = 20.dp,
                onClick = { haptic(); onOpen() },
            )
            .background(
                if (room.lightOn) cs.primaryContainer
                else cs.surfaceContainerHighest,
            )
            .padding(horizontal = 14.dp, vertical = 16.dp),
    ) {
            Column {
                // Nut den 48px (icon phong): tat = surfaceContainer, bat = primary
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (room.lightOn) cs.primary else cs.surfaceContainer,
                        )
                        .pressMorph(pressedScale = 0.85f) {
                            haptic()
                            onToggleLight()
                        },
                ) {
                    MsIcon(
                        M3EIcons.room(room.iconKey), null,
                        tint = if (room.lightOn) cs.onPrimary
                        else cs.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    room.name,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (room.lightOn) cs.onPrimaryContainer
                    else cs.onSurface,
                    maxLines = 1,
                    softWrap = false,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                val sub = buildList {
                    room.tempC?.let { add("%.1f°".format(it)) }
                    add("${room.deviceCount} thiết bị")
                    add("${room.devicesOn} bật")
                }.joinToString(" · ")
                Text(
                    sub,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (room.lightOn) cs.onPrimaryContainer
                    else cs.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            // Cham bao cua mo .rdot2 9px
            if (doorOpen) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .shadow(6.dp, CircleShape, ambientColor = cs.error, spotColor = cs.error)
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(cs.error),
                )
            }
        }
    }

/** Tay cam keo sheet (.grab 44x5). Dung chung cho RoomSheet + cac sheet trong Overlays. */
@Composable
internal fun GrabHandle() {
    Box(
        modifier = Modifier
            .padding(top = 4.dp, bottom = 16.dp)
            .size(44.dp, 5.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.outline),
    )
}

/**
 * Sheet chi tiet phong theo demo rev12 (.sheet): nen surfaceContainer,
 * grab, tieu de 24px/500 + dong sub "N thiet bi · M dang bat" 12px/500.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomSheet(
    room: RoomUi,
    notifications: List<HomeNotification>,
    onDismiss: () -> Unit,
    onToggle: (String) -> Unit,
    onClimateTemp: (String, Double) -> Unit,
    onHvacMode: (String, String) -> Unit,
    onToggleClimate: (String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 44.dp, topEnd = 44.dp),
        containerColor = cs.surfaceContainer,
        dragHandle = { GrabHandle() },
    ) {
        LazyColumn(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MsIcon(
                            M3EIcons.room(room.iconKey), null,
                            tint = cs.primary,
                            modifier = Modifier.size(30.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            room.name,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            color = cs.onSurface,
                        )
                    }
                    Text(
                        "${room.deviceCount} thiết bị · ${room.devicesOn} đang bật",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                    )
                }
            }
            // 2 tile nhiet do / do am (.shenv .tile: khong press morph)
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    EnvTile(
                        icon = Icons.Outlined.Thermostat, label = "Nhiệt độ",
                        value = room.tempC?.let { "%.1f°".format(it) } ?: "—",
                        container = cs.primaryContainer,
                        onContainer = cs.onPrimaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                    EnvTile(
                        icon = Icons.Outlined.WaterDrop, label = "Độ ẩm",
                        value = room.humidityPct?.let { "%.0f%%".format(it) } ?: "—",
                        container = LocalHumeExtraColors.current.infoContainer,
                        onContainer = LocalHumeExtraColors.current.onInfoContainer,
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
                    Text(
                        "Thiết bị",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                items(room.devices, key = { it.entityId }) { d ->
                    DeviceRow(
                        d,
                        contactOpen = contactOpenForDevice(d.iconKey, d.label, notifications),
                        onToggle = { onToggle(d.entityId) },
                    )
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun EnvTile(
    icon: ImageVector,
    label: String,
    value: String,
    container: Color,
    onContainer: Color,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(30.dp))
            .background(cs.surfaceContainerHighest)
            .padding(14.dp),
    ) {
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
                Text(
                    value,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = cs.onSurface,
                )
                Text(
                    label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
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

/**
 * The dieu hoa theo demo rev12: .ac-top (stepper + nut nguon, khong title)
 * + .ac-modes chua cum .rmm. Tat dieu hoa -> stepper + modes mo di (opacity .35).
 */
@Composable
private fun ClimateCard(
    c: ClimateUi,
    onTemp: (String, Double) -> Unit,
    onMode: (String, String) -> Unit,
    onToggle: (String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val target = c.targetTemp ?: 26.0
    val dim = if (c.isOn) 1f else 0.35f
    val haptic = rememberHaptic()
    // .ac-top: surfaceHighest, bo 24px, padding 14px 16px
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(dim)
            .clip(RoundedCornerShape(24.dp))
            .background(cs.surfaceContainerHighest)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Stepper: - | nhiet do muc tieu | +
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepperButton(Icons.Outlined.Remove, enabled = c.isOn) {
                    haptic()
                    onTemp(c.entityId, (target - 1).coerceIn(16.0, 31.0))
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    Text(
                        "%.0f°".format(target),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = cs.onSurface,
                        maxLines = 1,
                        softWrap = false,
                    )
                    Text(
                        "Mục tiêu",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
                StepperButton(Icons.Outlined.Add, enabled = c.isOn) {
                    haptic()
                    onTemp(c.entityId, (target + 1).coerceIn(16.0, 31.0))
                }
            }
            // Nut nguon tron 54px: bat = primaryContainer, tat = surfaceContainer
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(if (c.isOn) cs.primaryContainer else cs.surfaceContainer)
                    .pressMorph(pressedScale = 0.88f) {
                        haptic()
                        onToggle(c.entityId)
                    },
            ) {
                Icon(
                    Icons.Outlined.PowerSettingsNew, null,
                    tint = if (c.isOn) cs.onPrimaryContainer else cs.onSurfaceVariant,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    // .ac-modes: surfaceHighest, bo 24px, padding 14px
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(dim)
            .clip(RoundedCornerShape(24.dp))
            .background(cs.surfaceContainerHighest)
            .padding(14.dp),
    ) {
        ClimateModeGroup(c = c, enabled = c.isOn, onMode = onMode)
    }
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(cs.surfaceContainerHighest)
            .pressMorph(pressedScale = 0.85f, onClick = if (enabled) onClick else null),
    ) {
        Icon(
            icon, null,
            tint = cs.onSurface,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * Cum che do dieu hoa (.ac-modes .rmm): khong vien, chu 12.5px/700,
 * padding 14px 6px, bo 18px; chon = primaryContainer, KHONG check;
 * neighbor-press (nhan: 1.45, ke ben: 0.82), :active scale(.9) spring.
 */
@Composable
private fun ClimateModeGroup(
    c: ClimateUi,
    enabled: Boolean,
    onMode: (String, String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val haptic = rememberHaptic()
    val modes = c.modes.filter { it in hvacLabels }.takeIf { it.isNotEmpty() }
        ?: listOf("cool", "heat", "auto", "fan_only")
    val selectedMode = c.hvacMode.takeIf { it in modes } ?: modes.first()
    val np = rememberNeighborPress(modes.size)
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        modes.forEachIndexed { i, m ->
            val sel = m == selectedMode
            var pressed by remember { mutableStateOf(false) }
            val w by animateFloatAsState(
                targetValue = np.weightFor(i),
                animationSpec = tween(350, easing = M3EMotion.spring),
                label = "rmmW",
            )
            val scale by animateFloatAsState(
                targetValue = if (pressed) 0.9f else 1f,
                animationSpec = tween(300, easing = M3EMotion.spring),
                label = "rmmScale",
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(w)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (sel) cs.primaryContainer else cs.surfaceContainer)
                    .pointerInput(i, enabled) {
                        if (!enabled) return@pointerInput
                        detectTapGestures(
                            onPress = {
                                pressed = true
                                np.press(i)
                                tryAwaitRelease()
                                pressed = false
                                np.release()
                            },
                            onTap = {
                                haptic()
                                onMode(c.entityId, m)
                            },
                        )
                    }
                    .padding(vertical = 14.dp, horizontal = 6.dp),
            ) {
                Text(
                    hvacLabels[m] ?: m,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (sel) cs.onPrimaryContainer else cs.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}

/**
 * 1 hang thiet bi theo demo rev12 (.dev): gap 13px, surfaceHighest,
 * bo 28px -> 18px khi nhan, padding 14px 16px;
 * icon 48px: tat = surfaceHigh, bat = primaryContainer;
 * ten 14px/700, trang thai 12px/500;
 * cam bien cua: chip .cchip ("DONG" surfaceContainer / "MO" errorContainer,
 * 11px/800) thay switch, sub "Dang dong"/"Dang mo".
 */
@Composable
fun DeviceRow(
    d: DeviceUi,
    onToggle: () -> Unit,
    contactOpen: Boolean? = null,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val extra = LocalHumeExtraColors.current
    val haptic = rememberHaptic()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .pressMorphCard(
                pressedScale = 1f,
                corner = 28.dp,
                pressedCorner = 18.dp,
                onClick = null,
            )
            .background(cs.surfaceContainerHighest)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (d.isOn) cs.primaryContainer
                    else extra.surfaceHigh,
                ),
        ) {
            MsIcon(
                M3EIcons.device(d.iconKey), null,
                tint = if (d.isOn) cs.onPrimaryContainer
                else cs.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                d.label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = cs.onSurface,
            )
            val powerW = d.powerW
            Text(
                when {
                    contactOpen == true -> "Đang mở"
                    contactOpen == false -> "Đang đóng"
                    d.isOn -> "Đang bật"
                    else -> "Đang tắt"
                } + (if (d.isOn && powerW != null && powerW > 0)
                    " · ${if (powerW >= 1000) "%.1f kW".format(powerW / 1000) else "%.0f W".format(powerW)}"
                else ""),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = cs.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (contactOpen != null) {
            // Chip contact .cchip
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (contactOpen) cs.errorContainer
                        else cs.surfaceContainer,
                    )
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    if (contactOpen) "MỞ" else "ĐÓNG",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                    color = if (contactOpen) cs.onErrorContainer
                    else cs.onSurfaceVariant,
                )
            }
        } else {
            M3ESwitch(
                checked = d.isOn,
                onCheckedChange = {
                    haptic()
                    onToggle()
                },
            )
        }
    }
}
