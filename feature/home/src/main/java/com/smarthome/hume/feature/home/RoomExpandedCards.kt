package com.smarthome.hume.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import com.smarthome.hume.core.model.DeviceUi
import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.core.ui.components.M3EMotion
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

/**
 * Noi dung mo rong khi tap the thiet bi trong phong (port iOS
 * RoomExpandedCards.swift + HomeDeviceRow expanded slot, GAP-07/08/09).
 * Co che mo/thu + animation nam o DeviceRow / RoomSheet; file nay chi
 * chua NOI DUNG mo rong tung loai the, render trong cung the.
 *
 * Gate dispatcher y nhu iOS (khong doan loai tu ten hien thi):
 * - rua bat: entity == switch.o_cam_may_rua_bat hoac
 *   powerEntity == sensor.o_cam_may_rua_bat_power
 * - den day du: chi light.smartlight va light.table_led, den khac -> generic
 * - con lai: generic (trang thai + cong suat + attribute neu co)
 */

/** Cac hanh dong HA ma the mo rong goi — RoomSheet truyen tu HomeScreen. */
data class ExpandedActions(
    val onToggle: (String) -> Unit = {},
    val onSetNumber: (String, Double) -> Unit = { _, _ -> },
    val onSetLocalTuyaDp: (String, Int, String) -> Unit = { _, _, _ -> },
    val onSetBrightness: (String, Int) -> Unit = { _, _ -> },
    val onSetLightRgb: (String, Int, Int, Int) -> Unit = { _, _, _, _ -> },
    val onSetLightEffect: (String, String) -> Unit = { _, _ -> },
)

@Composable
fun RoomExpandedContent(
    device: DeviceUi,
    entityStates: Map<String, HomeEntity>,
    actions: ExpandedActions = ExpandedActions(),
) {
    when {
        // Gate y nhu iOS: entity hoac powerEntity (khong doan tu ten).
        device.entityId == DishwasherIds.MAIN_SWITCH ||
            device.powerEntityId == DishwasherIds.POWER_SENSOR -> {
            DishwasherExpandedView(entityStates, actions)
        }
        // Gate dung 2 entity iOS — den khac roi ve generic.
        device.entityId == "light.smartlight" ||
            device.entityId == "light.table_led" -> {
            LightExpandedView(device.entityId, entityStates, actions)
        }
        else -> GenericExpandedView(device, entityStates)
    }
}

/** Entity/DP may rua bat Texgio — verify tu code iOS (xem DishwasherIds). */
object DishwasherIds {
    const val MAIN_SWITCH = "switch.o_cam_may_rua_bat"
    const val POWER_SENSOR = "sensor.o_cam_may_rua_bat_power"
}

/** Dinh dang cong suat nhu iOS: >= 1000W -> x.x kW, duoi -> W nguyen. */
internal fun formatExpandedW(w: Double): String =
    if (w >= 1000) "%.1f kW".format(w / 1000) else "%.0f W".format(w)

/** Doc attribute so tu HomeEntity (JsonElement) — null neu khong co. */
internal fun HomeEntity.attrDouble(name: String): Double? =
    (attributes[name] as? JsonPrimitive)?.doubleOrNull

internal fun HomeEntity.attrStringList(name: String): List<String> {
    val arr = attributes[name] as? kotlinx.serialization.json.JsonArray ?: return emptyList()
    return arr.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
}

internal fun HomeEntity.attrString(name: String): String? =
    (attributes[name] as? JsonPrimitive)?.contentOrNull

internal fun HomeEntity.attrIntList(name: String): List<Int>? {
    val arr = attributes[name] as? kotlinx.serialization.json.JsonArray ?: return null
    val nums = arr.mapNotNull { (it as? JsonPrimitive)?.doubleOrNull?.toInt() }
    return if (nums.size >= 3) nums.take(3) else null
}

/**
 * Noi dung mo rong chung (iOS GenericExpandedView): chi hien thong tin
 * that, khong tu them nut dieu khien.
 */
@Composable
fun GenericExpandedView(
    device: DeviceUi,
    entityStates: Map<String, HomeEntity>,
) {
    val stateRaw = entityStates[device.entityId]?.state
        ?: if (device.isOn) "on" else "off"
    val entity = entityStates[device.entityId]
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ExpandedInfoRow(
            label = "Trạng thái",
            value = when (stateRaw) {
                "on" -> "Đang bật"
                "off" -> "Đang tắt"
                "open" -> "Đang mở"
                "closed" -> "Đang đóng"
                else -> "Không rõ"
            },
        )
        val w = device.powerW
        if (w != null && w > 0) {
            ExpandedInfoRow(label = "Công suất", value = formatExpandedW(w))
        }
        entity?.attrDouble("percentage")?.let {
            ExpandedInfoRow(label = "Tốc độ", value = "${it.toInt()}%")
        }
        entity?.attrDouble("current_position")?.let {
            ExpandedInfoRow(label = "Độ mở", value = "${it.toInt()}%")
        }
        entity?.attrDouble("battery_level")?.let {
            ExpandedInfoRow(label = "Pin", value = "${it.toInt()}%")
        }
    }
}

// ══════════════════════════════════════════════════════════════════
//  MAY RUA BAT (the REA — 1 the nhu 1 app, GAP-08)
//  Thu tu y nhu iOS DishwasherExpandedView: hero + trang thai + doan rua
//  + 3 pill meta + chuong trinh + 3 o tang cuong + nuoc mem + hen gio
//  + hang nut cuoi. Entity/DP verify tu code iOS (RoomExpandedCards.swift
//  + DishwasherLiveManager.swift), khong doan:
//  - sensor: trang thai DP 7, giai doan DP 10, con lai DP 9 (phut),
//    nhiet do DP 11, chuong trinh DP 6
//  - switch: nguon DP 1, bat dau DP 2 (CHUA TUNG TEST TU HA),
//    hoi nuoc DP 108, chong dia DP 102, khoa tre em DP 101 (o "Say chong
//    khuan" — ten entity goc la khoa tre em, iOS dung lam tang cuong)
//  - number: hen gio DP 8 (phut, 0–1440), nuoc mem DP 106 (1–15)
//  - localtuya device_id: eb37b93fb51d417056uicd
//  - chuong trinh: CHI 3 ma da biet duoc gui qua localtuya.set_dp DP 6
//    (Intensive/Auto do truc tiep, Eco suy tu ten demo REA — can 1 lan
//    chon tren than may de xac nhan dut diem). Hang ngay/Nhanh/Rua dem
//    hien mo, bam chi thu lai, KHONG gui gi.
//  - nut Dung: INERT nhu iOS (khong co DP chac chan ket thuc chu trinh).
// ══════════════════════════════════════════════════════════════════

private data class DishwasherProgram(
    val display: String,
    val icon: ImageVector,
    val machineValue: String?,
    val durationMin: Int?,
)

private object DishwasherPrograms {
    const val deviceId = "eb37b93fb51d417056uicd"

    const val statusEntity = "sensor.texgio_tgfpcm795b_g_trang_thai"
    const val stageEntity = "sensor.texgio_tgfpcm795b_g_giai_doan_rua"
    const val remainingEntity = "sensor.texgio_tgfpcm795b_g_thoi_gian_con_lai"
    const val tempEntity = "sensor.texgio_tgfpcm795b_g_nhiet_do"
    const val programEntity = "sensor.texgio_tgfpcm795b_g_chuong_trinh"

    const val powerSwitch = "switch.texgio_tgfpcm795b_g_may_rua_bat"
    const val startSwitch = "switch.texgio_tgfpcm795b_g_bat_dau"
    const val steamSwitch = "switch.texgio_tgfpcm795b_g_hoi_nuoc"
    const val stackSwitch = "switch.texgio_tgfpcm795b_g_chong_dia"
    const val antibacSwitch = "switch.texgio_tgfpcm795b_g_khoa_tre_em"
    const val timerNumber = "number.texgio_tgfpcm795b_g_hen_gio"
    const val softNumber = "number.texgio_tgfpcm795b_g_nuoc_mem"

    val all: List<DishwasherProgram> = listOf(
        DishwasherProgram("Tiết kiệm", Icons.Outlined.Eco, "Eco", null),
        DishwasherProgram("Chuyên sâu", Icons.Outlined.LocalFireDepartment, "Intensive", 150),
        DishwasherProgram("Hằng ngày", Icons.Outlined.WbSunny, null, null),
        DishwasherProgram("Nhanh", Icons.Outlined.Bolt, null, null),
        DishwasherProgram("Tự động", Icons.Outlined.Autorenew, "Auto", 180),
        DishwasherProgram("Rửa đêm", Icons.Outlined.DarkMode, null, null),
    )

    /** Thu tu chu trinh (iOS segments): icon SF -> Material tuong duong. */
    val segments: List<Pair<String, ImageVector>> = listOf(
        "Rửa sơ" to Icons.Outlined.WaterDrop,
        "Rửa chính" to Icons.Filled.WaterDrop,
        "Tráng" to Icons.Outlined.Autorenew,
        "Sấy" to Icons.Outlined.Air,
    )

    /** Map gia tri may -> doan (iOS stageMap). Gia tri la GIU doan cu. */
    val stageMap: Map<String, Int> = mapOf(
        "Prewash" to 0, "Main_wash" to 1, "Rinsing" to 2,
    )

    fun statusDisplay(raw: String): String = when (raw) {
        "Run" -> "Đang rửa"
        "Pause" -> "Tạm dừng"
        "Idle" -> "Sẵn sàng"
        "Power_off" -> "Đã tắt"
        else -> "Không rõ"
    }

    fun displayProgram(raw: String): String = when (raw) {
        "Intensive" -> "Chuyên sâu"
        "Auto" -> "Tự động"
        "Eco" -> "Tiết kiệm"
        else -> "Không rõ"
    }
}

@Composable
fun DishwasherExpandedView(
    entityStates: Map<String, HomeEntity>,
    actions: ExpandedActions,
) {
    val cs = MaterialTheme.colorScheme
    val p = DishwasherPrograms
    val statusRaw = entityStates[p.statusEntity]?.state ?: "unavailable"
    val stageRaw = entityStates[p.stageEntity]?.state ?: "unavailable"
    val programRaw = entityStates[p.programEntity]?.state ?: "unavailable"
    val remaining = entityStates[p.remainingEntity]?.numericState?.toInt() ?: 0
    val temp = entityStates[p.tempEntity]?.numericState?.toInt()
    val steamOn = entityStates[p.steamSwitch]?.state == "on"
    val stackOn = entityStates[p.stackSwitch]?.state == "on"
    val selectedProgram = p.all.firstOrNull { it.machineValue == programRaw }
    val totalMin: Int = selectedProgram?.durationMin?.let { dur ->
        var t = dur
        if (steamOn) t += 30
        if (stackOn) t += 20
        t
    } ?: remaining
    val progress: Float = if (totalMin > 0) {
        ((totalMin - remaining).toFloat() / totalMin).coerceIn(0f, 1f)
    } else 0f
    val timerMin = entityStates[p.timerNumber]?.numericState?.toInt() ?: 0
    val timerText = when {
        timerMin == 0 -> "Tắt"
        timerMin % 60 == 0 -> "${timerMin / 60} giờ"
        else -> "$timerMin phút"
    }
    val softLevel = entityStates[p.softNumber]?.numericState?.toInt() ?: 0
    val running = statusRaw == "Run" || statusRaw == "Pause"
    val heroTime = "${remaining / 60}:" +
        String.format(java.util.Locale.US, "%02d", remaining % 60)
    val programDisplay = p.displayProgram(programRaw)

    // Doan hien tai: gia tri la giu doan cu (kieu DishwasherLiveManager
    // lastStageIndex; the mo rong iOS goc truyen nil -> -1, nhung task
    // port chot "doan la giu doan cu" nen giu lastStage o day).
    var lastStageIdx by remember { mutableIntStateOf(0) }
    val mappedIdx = p.stageMap[stageRaw]
    LaunchedEffect(mappedIdx) {
        if (mappedIdx != null) lastStageIdx = mappedIdx
    }
    val currentIdx = mappedIdx ?: lastStageIdx

    var programOpen by remember { mutableStateOf(false) }
    val chevronRot by animateFloatAsState(
        targetValue = if (programOpen) 180f else 0f,
        animationSpec = tween(450, easing = M3EMotion.spring),
        label = "dwChevron",
    )
    var softSlider by remember { mutableFloatStateOf(softLevel.toFloat().coerceIn(1f, 15f)) }
    var draggingSoft by remember { mutableStateOf(false) }
    LaunchedEffect(softLevel) {
        if (!draggingSoft) softSlider = softLevel.toFloat().coerceIn(1f, 15f)
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Hero: trai = nhan + gio lon, phai = nhiet do + nhan.
        Row(verticalAlignment = Alignment.Top) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Thời gian chu trình",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
                Text(
                    heroTime,
                    style = MaterialTheme.typography.displayLarge,
                    color = cs.onSurface,
                )
            }
            Spacer(Modifier.weight(1f))
            if (temp != null) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        "$temp°C",
                        style = MaterialTheme.typography.titleLarge,
                        color = cs.onSurface,
                    )
                    Text(
                        "Nhiệt độ nước",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                }
            }
        }
        // Dong trang thai: "San sang • Chuyen sau".
        Text(
            "${
                p.statusDisplay(statusRaw)
            } • $programDisplay",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = cs.onSurface,
        )
        // 4 doan: thanh manh + cham co icon + nhan duoi chan.
        DishwasherSegmentFlow(
            segments = p.segments,
            current = currentIdx,
            progress = progress,
        )
        // Hang meta: 3 pill icon + gia tri, khong chu thich, dan deu.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            DishwasherMetaPill(Icons.Outlined.Schedule, "$totalMin phút")
            DishwasherMetaPill(Icons.Outlined.Timer, timerText)
            DishwasherMetaPill(Icons.Outlined.WaterDrop, "Mức $softLevel")
        }

        HorizontalDivider(thickness = 1.dp, color = cs.surfaceContainerHigh)

        // Hang chuong trinh kieu the an ninh: an tieu de bung 1 hang
        // ngang 6 icon (spring 0.45).
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { programOpen = !programOpen },
            ) {
                Text(
                    "CHƯƠNG TRÌNH",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 0.5.sp,
                    color = cs.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(cs.primaryContainer)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    if (selectedProgram != null) {
                        Icon(
                            selectedProgram.icon, null,
                            tint = cs.onPrimaryContainer,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.size(6.dp))
                    }
                    Text(
                        selectedProgram?.durationMin?.let {
                            "$programDisplay · $it phút"
                        } ?: programDisplay,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.size(8.dp))
                Icon(
                    Icons.Outlined.KeyboardArrowDown, null,
                    tint = cs.onSurfaceVariant,
                    modifier = Modifier
                        .size(14.dp)
                        .graphicsLayer { rotationZ = chevronRot },
                )
            }
            AnimatedVisibility(
                visible = programOpen,
                enter = expandVertically(tween(450, easing = M3EMotion.spring)) +
                    fadeIn(tween(450, easing = M3EMotion.spring)),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    p.all.forEach { prog ->
                        val isCurrent = prog.machineValue == programRaw
                        val known = prog.machineValue != null
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .alpha(if (known) 1f else 0.4f)
                                .clickable {
                                    programOpen = false
                                    // Chi gui ma da biet; ma chua ro thi
                                    // chi thu lai, khong gui gi vao may.
                                    prog.machineValue?.let { mv ->
                                        actions.onSetLocalTuyaDp(p.deviceId, 6, mv)
                                    }
                                },
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isCurrent) cs.primaryContainer
                                        else cs.surfaceContainerHigh,
                                    ),
                            ) {
                                Icon(
                                    prog.icon, null,
                                    tint = if (isCurrent) cs.onPrimaryContainer
                                    else cs.onSurfaceVariant,
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                            Text(
                                prog.display,
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = cs.surfaceContainerHigh)

        // 3 o tang cuong: o vuong 76dp, vong tron trang 40dp + icon 18dp.
        // Thu tu iOS: chong dia (DP 102) — hoi nuoc (DP 108) — say chong
        // khuan (DP 101, entity goc _khoa_tre_em).
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DishwasherBoostButton(
                label = "Rửa tăng cường",
                icon = Icons.Outlined.Layers,
                entity = p.stackSwitch,
                states = entityStates,
                onToggle = actions.onToggle,
            )
            DishwasherBoostButton(
                label = "Sấy tăng cường",
                icon = Icons.Outlined.Cloud,
                entity = p.steamSwitch,
                states = entityStates,
                onToggle = actions.onToggle,
            )
            DishwasherBoostButton(
                label = "Sấy chống khuẩn",
                icon = Icons.Outlined.Shield,
                entity = p.antibacSwitch,
                states = entityStates,
                onToggle = actions.onToggle,
            )
        }

        HorizontalDivider(thickness = 1.dp, color = cs.surfaceContainerHigh)

        // NUOC MEM: nhan + nut -/+ 56dp + slider 1–15, gui khi tha tay
        // qua number.set_value DP 106, hien thi "x/15".
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "NƯỚC MỀM",
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 0.5.sp,
                color = cs.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                DishwasherStepperButton(
                    icon = Icons.Outlined.Remove,
                    enabled = softLevel > 1,
                    onClick = {
                        val v = maxOf(1, softLevel - 1)
                        actions.onSetNumber(p.softNumber, v.toDouble())
                        softSlider = v.toFloat()
                    },
                )
                Spacer(Modifier.size(12.dp))
                Slider(
                    value = softSlider,
                    onValueChange = {
                        softSlider = it
                        draggingSoft = true
                    },
                    valueRange = 1f..15f,
                    steps = 13,
                    onValueChangeFinished = {
                        draggingSoft = false
                        actions.onSetNumber(p.softNumber, softSlider.toInt().toDouble())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = cs.primary,
                        activeTrackColor = cs.primary,
                        inactiveTrackColor = cs.surfaceContainerHigh,
                    ),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.size(12.dp))
                DishwasherStepperButton(
                    icon = Icons.Outlined.Add,
                    enabled = softLevel < 15,
                    onClick = {
                        val v = minOf(15, softLevel + 1)
                        actions.onSetNumber(p.softNumber, v.toDouble())
                        softSlider = v.toFloat()
                    },
                )
                Spacer(Modifier.size(12.dp))
                Row {
                    Text(
                        "$softLevel",
                        style = MaterialTheme.typography.displaySmall,
                        color = cs.onSurface,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Text(
                        "/15",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = cs.surfaceContainerHigh)

        // HEN GIO BAT DAU: chi nut -/+ buoc 1 gio, 0–1440 phut (DP 8).
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "HẸN GIỜ BẮT ĐẦU",
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 0.5.sp,
                color = cs.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                DishwasherStepperButton(
                    icon = Icons.Outlined.Remove,
                    enabled = timerMin > 0,
                    onClick = {
                        actions.onSetNumber(
                            p.timerNumber,
                            maxOf(0, timerMin - 60).toDouble(),
                        )
                    },
                )
                Spacer(Modifier.weight(1f))
                Text(
                    timerText,
                    style = MaterialTheme.typography.displaySmall,
                    color = cs.onSurface,
                )
                Spacer(Modifier.weight(1f))
                DishwasherStepperButton(
                    icon = Icons.Outlined.Add,
                    enabled = timerMin < 1440,
                    onClick = {
                        actions.onSetNumber(
                            p.timerNumber,
                            minOf(1440, timerMin + 60).toDouble(),
                        )
                    },
                )
            }
        }

        HorizontalDivider(thickness = 1.dp, color = cs.surfaceContainerHigh)

        // Hang nut cuoi: nguon (DP 1) + Dung (INERT) + Bat dau/Tam dung
        // (DP 2 — CHUA TUNG TEST TU HA, lan dau bam can de y may chay
        // dung khong).
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (statusRaw != "Power_off") cs.primaryContainer
                        else cs.surfaceContainerHigh,
                    )
                    .clickable { actions.onToggle(p.powerSwitch) },
            ) {
                Icon(
                    Icons.Outlined.PowerSettingsNew, null,
                    tint = if (statusRaw != "Power_off") cs.onPrimaryContainer
                    else cs.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
            // Dung: CHUA noi HA — khong co DP chac chan ket thuc chu trinh.
            // Nut hien dung vi tri/kich thuoc REA (60dp, vuong do khi dang
            // chay) de cho test chot DP roi noi 1 dong, KHONG bam duoc.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(
                        if (running) cs.errorContainer
                        else cs.surfaceContainerHigh,
                    )
                    .alpha(if (running) 1f else 0.5f),
            ) {
                Icon(
                    Icons.Filled.Stop, null,
                    tint = if (running) cs.onErrorContainer
                    else cs.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(cs.primary)
                    .clickable { actions.onToggle(p.startSwitch) }
                    .padding(vertical = 16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        if (statusRaw == "Run") Icons.Outlined.Pause
                        else Icons.Outlined.PlayArrow,
                        null,
                        tint = cs.onPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        if (statusRaw == "Run") "Tạm dừng" else "Bắt đầu rửa",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun DishwasherSegmentFlow(
    segments: List<Pair<String, ImageVector>>,
    current: Int,
    progress: Float,
) {
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Thanh manh: nen + phan da chay (scale theo progress).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(cs.surfaceContainerHigh),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(cs.primary),
            )
        }
        Row {
            segments.forEachIndexed { i, (label, icon) ->
                val done = i <= current && current >= 0
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    // Moi buoc 1 icon; trang thai bang mau nen cham:
                    // da qua/cham toi = primary, chua toi = surface.
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(
                                if (done) cs.primary
                                else cs.surfaceContainerHigh,
                            ),
                    ) {
                        Icon(
                            icon, null,
                            tint = if (done) cs.onPrimary
                            else cs.onSurfaceVariant,
                            modifier = Modifier.size(11.dp),
                        )
                    }
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (i == current) cs.onSurface
                        else cs.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun DishwasherMetaPill(icon: ImageVector, value: String) {
    val cs = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(CircleShape)
            .background(cs.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Icon(icon, null, tint = cs.primary, modifier = Modifier.size(14.dp))
        Text(
            value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = cs.onSurface,
            maxLines = 1,
        )
    }
}

@Composable
private fun RowScope.DishwasherBoostButton(
    label: String,
    icon: ImageVector,
    entity: String,
    states: Map<String, HomeEntity>,
    onToggle: (String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val state = states[entity]?.state ?: "unavailable"
    val isOn = state == "on"
    val known = state == "on" || state == "off"
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(1f)
            .height(76.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(if (isOn) cs.primaryContainer else cs.surfaceContainerHigh)
            .alpha(if (known) 1f else 0.5f)
            .clickable { onToggle(entity) },
    ) {
        // Vong tron trang trong o nhu HTML.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White),
        ) {
            Icon(
                icon, label,
                tint = if (isOn) cs.primary else cs.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun DishwasherStepperButton(
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(cs.surfaceContainerHigh)
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled) { onClick() },
    ) {
        Icon(icon, null, tint = cs.onSurface, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun ExpandedInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ══════════════════════════════════════════════════════════════════
//  DEN (GAP-09) — port iOS LightExpandedView.
//  Gate: CHI light.smartlight va light.table_led (dispatcher). Den khac
//  roi ve generic. Khong co dieu khien color_temp (iOS cung khong co).
//  - Slider sang: gui khi THA TAY qua HomeViewModel.setBrightness (da co,
//    brightness_pct — tuong duong brightness 0–255 cua iOS), chong echo
//    kieu iOS lastSentBrightness (o day theo %: bo qua sync ngoai khi
//    lech <= 1% do lam tron pct <-> 0–255).
//  - Mau sac: Android khong co ColorPicker he thong nhu SwiftUI nen dung
//    thanh truot hue kieu MeScreen (track gradient HSV) — giu nguyen
//    saturation/value cua mau hien tai, gui rgb_color debounce 300ms
//    (HA tu doi sang xy cho smartlight, nhu comment iOS).
//  - Hieu ung: hang chip cuon ngang (LazyRow — den ban ~150 chip),
//    gui MA GOC, nhan Viet hoa qua effectDisplay (port y iOS).
// ══════════════════════════════════════════════════════════════════

/** Ten hieu ung -> tieng Viet (port y iOS effectDisplay). Chi doi NHAN;
 * gia tri gui ve HA van la ma goc. */
private fun effectDisplay(eff: String): String {
    val exact: Map<String, String> = mapOf(
        "blink" to "Nhấp nháy", "breathe" to "Thở", "okay" to "OK",
        "channel_change" to "Đổi kênh",
        "finish_effect" to "Kết thúc hiệu ứng", "stop_effect" to "Dừng hiệu ứng",
        "colorloop" to "Xoay màu", "stop_colorloop" to "Dừng xoay màu",
        "candle" to "Nến", "fire" to "Lửa", "ocean" to "Đại dương",
        "forest" to "Rừng", "sunset" to "Hoàng hôn", "party" to "Tiệc",
        "romantic" to "Lãng mạn", "random" to "Ngẫu nhiên",
        "strobe" to "Nhấp nhanh", "flash" to "Chớp",
        "twinkle" to "Lấp lánh", "sparkle" to "Lấp lánh",
        "wave" to "Sóng", "gradient" to "Chuyển sắc",
        "cycle" to "Xoay vòng", "pulse" to "Nhịp", "flow" to "Chảy",
        "police" to "Cảnh sát",
    )
    exact[eff]?.let { return it }
    val lower = eff.lowercase()
    val families: List<Pair<String, String>> = listOf(
        "rainbow" to "Cầu vồng", "chase" to "Đuổi",
        "fade" to "Mờ dần", "music" to "Nhạc",
        "strobe" to "Nhấp nhanh", "flash" to "Chớp",
        "twinkle" to "Lấp lánh", "sparkle" to "Lấp lánh",
        "wave" to "Sóng", "gradient" to "Chuyển sắc",
        "cycle" to "Xoay vòng", "pulse" to "Nhịp",
        "flow" to "Chảy", "candle" to "Nến", "fire" to "Lửa",
    )
    for ((prefix, vn) in families) {
        if (lower.startsWith(prefix)) {
            val rest = eff.drop(prefix.length).replace("_", " ").trim()
            if (rest.isEmpty()) return vn
            val capitalized = rest.split(" ").joinToString(" ") { word ->
                word.replaceFirstChar { c -> c.uppercase() }
            }
            return "$vn $capitalized"
        }
    }
    return eff.replace("_", " ").split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { c -> c.uppercase() }
    }
}

private fun hsvOf(rgb: List<Int>?): FloatArray {
    val hsv = FloatArray(3)
    if (rgb != null) {
        android.graphics.Color.RGBToHSV(rgb[0], rgb[1], rgb[2], hsv)
    } else {
        hsv[0] = 0f; hsv[1] = 0f; hsv[2] = 1f
    }
    return hsv
}

@Composable
fun LightExpandedView(
    entityId: String,
    entityStates: Map<String, HomeEntity>,
    actions: ExpandedActions,
) {
    val cs = MaterialTheme.colorScheme
    val entity = entityStates[entityId]
    val attrBrightness = entity?.attrDouble("brightness") // 0–255
    val attrPct = attrBrightness?.let { (it / 255.0 * 100.0).toFloat() }
    val attrRgb = entity?.attrIntList("rgb_color")
    val effectList = entity?.attrStringList("effect_list").orEmpty()
    val currentEffect = entity?.attrString("effect")

    // Do sang — gui khi tha tay, chong echo kieu iOS.
    var brightness by remember { mutableFloatStateOf(attrPct ?: 0f) }
    var draggingBrightness by remember { mutableStateOf(false) }
    var lastSentPercent by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(attrPct) {
        if (!draggingBrightness && attrPct != null &&
            (lastSentPercent == null || abs(attrPct - lastSentPercent!!) > 1f)
        ) {
            brightness = attrPct
        }
    }

    // Mau sac — hue slider, debounce 300ms, chong echo kieu iOS lastSentRGB.
    val initialHsv = remember { hsvOf(attrRgb) }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var satVal by remember { mutableStateOf(initialHsv[1] to initialHsv[2]) }
    var lastSentRgb by remember { mutableStateOf<List<Int>?>(null) }
    val scope = rememberCoroutineScope()
    var colorJob by remember { mutableStateOf<Job?>(null) }
    LaunchedEffect(attrRgb) {
        if (attrRgb != null && attrRgb != lastSentRgb) {
            val hsv = hsvOf(attrRgb)
            hue = hsv[0]
            satVal = hsv[1] to hsv[2]
        }
    }
    fun currentRgb(): List<Int> {
        val packed = android.graphics.Color.HSVToColor(
            floatArrayOf(hue, satVal.first, satVal.second),
        )
        return listOf(
            android.graphics.Color.red(packed),
            android.graphics.Color.green(packed),
            android.graphics.Color.blue(packed),
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Do sang.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.WbSunny, null,
                tint = cs.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(12.dp))
            Slider(
                value = brightness,
                onValueChange = {
                    brightness = it
                    draggingBrightness = true
                },
                valueRange = 0f..100f,
                onValueChangeFinished = {
                    draggingBrightness = false
                    lastSentPercent = brightness
                    actions.onSetBrightness(entityId, brightness.toInt())
                },
                colors = SliderDefaults.colors(
                    thumbColor = cs.primary,
                    activeTrackColor = cs.primary,
                    inactiveTrackColor = cs.surfaceContainerHigh,
                ),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.size(12.dp))
            Text(
                "${brightness.toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurface,
                textAlign = TextAlign.End,
                modifier = Modifier.width(44.dp),
            )
        }
        // Mau sac — hue slider (track gradient) + mau hien tai.
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Màu sắc",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface,
                )
                Spacer(Modifier.weight(1f))
                val rgb = currentRgb()
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(rgb[0], rgb[1], rgb[2]))
                        .border(1.dp, cs.outlineVariant, CircleShape),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = List(7) { i ->
                                Color.hsv(i * 60f, 1f, 1f)
                            } + listOf(Color.hsv(0f, 1f, 1f)),
                        ),
                    ),
            ) {
                Slider(
                    value = hue,
                    onValueChange = { newHue ->
                        hue = newHue
                        // Mau dang la trang (sat = 0) thi keo hue phai ra
                        // mau that, khong thi truot khong co tac dung nhin thay.
                        if (satVal.first == 0f) satVal = 1f to satVal.second
                        colorJob?.cancel()
                        colorJob = scope.launch {
                            delay(300)
                            val rgb = currentRgb()
                            lastSentRgb = rgb
                            actions.onSetLightRgb(entityId, rgb[0], rgb[1], rgb[2])
                        }
                    },
                    valueRange = 0f..360f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.Transparent,
                        inactiveTrackColor = Color.Transparent,
                    ),
                    modifier = Modifier.matchParentSize(),
                )
            }
        }
        // Hieu ung — hang chip cuon ngang (den ban ~150 chip -> LazyRow).
        if (effectList.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Hiệu ứng",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(effectList, key = { it }) { eff ->
                        val selected = eff == currentEffect
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (selected) cs.primaryContainer
                                    else cs.surfaceContainerHigh,
                                )
                                .clickable {
                                    actions.onSetLightEffect(entityId, eff)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(
                                effectDisplay(eff),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) cs.onPrimaryContainer
                                else cs.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
