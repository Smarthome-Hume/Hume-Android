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
import com.smarthome.hume.core.ui.theme.M3ETokens
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
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
    // (2026-11-18) Khoi phuc: khe giua cac dong generic 4->8dp (iOS 8pt).
    Column(verticalArrangement = Arrangement.spacedBy(M3ETokens.RoomSheet.GenericGap)) {
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
//  - number: hen gio DP 8 (phut, 0–1440), nuoc mem DP 106 (3–15)
//  - localtuya device_id: eb37b93fb51d417056uicd
//  - chuong trinh: CHI 3 ma da biet duoc gui qua localtuya.set_dp DP 6
//    (Intensive/Auto do truc tiep, Eco suy tu ten demo REA — can 1 lan
//    chon tren than may de xac nhan dut diem). Hang ngay/Nhanh/Rua dem
//    hien mo, bam chi thu lai, KHONG gui gi.
//  - nut Dung: INERT nhu iOS (khong co DP chac chan ket thuc chu trinh).
// ══════════════════════════════════════════════════════════════════

internal data class DishwasherProgram(
    val display: String,
    val icon: ImageVector,
    val machineValue: String?,
    val durationMin: Int?,
)

internal object DishwasherPrograms {
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

    /** Toan bo 12 entity the rua bat doc (iOS DishwasherLiveManager.cardEntities)
     * — HomeViewModel dung de nhan biet lenh rua bat va refetch sau lenh (N-09). */
    val cardEntityIds: Set<String> = setOf(
        statusEntity, stageEntity, remainingEntity, tempEntity, programEntity,
        powerSwitch, startSwitch, steamSwitch, stackSwitch, antibacSwitch,
        timerNumber, softNumber,
    )

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
    var softSlider by remember { mutableFloatStateOf(softLevel.toFloat().coerceIn(3f, 15f)) }
    var draggingSoft by remember { mutableStateOf(false) }
    LaunchedEffect(softLevel) {
        if (!draggingSoft) softSlider = softLevel.toFloat().coerceIn(3f, 15f)
    }

    // (2026-11-18) Khoi phuc: khe doc tong the rua bat 8->14dp (iOS 14pt).
    Column(verticalArrangement = Arrangement.spacedBy(M3ETokens.RoomSheet.DishwasherGap)) {
        // Hero: trai = nhan + gio lon, phai = nhiet do + nhan.
        Row(verticalAlignment = Alignment.Top) {
            Column(verticalArrangement = Arrangement.spacedBy(M3ETokens.RoomSheet.DishwasherHeroGap)) {
                Text(
                    "Thời gian chu trình",
                    // (Viec 5) lineHeight tu nhien thay vi M3 16sp
                    // (iOS bodySmall 12pt tu nhien ~14,6): cot trai hero
                    // 82dp -> 80,6dp (van ngan hon iOS 86,1dp vi
                    // displayLarge theme 64sp < tu nhien iOS 69,5).
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = M3ETokens.Text.BodySmall.fontSize,
                        lineHeight = M3ETokens.Text.BodySmall.lineHeight,
                    ),
                    color = cs.onSurfaceVariant,
                )
                Text(
                    heroTime,
                    // (18/11 token) LH tu dat 52sp -> 69,5sp (DisplayHero):
                    // LH = 57 x 1,219; hero cao them 17,5dp (xem bao cao vong token).
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = M3ETokens.Text.DisplayHero.fontSize,
                        lineHeight = M3ETokens.Text.DisplayHero.lineHeight,
                    ),
                    color = cs.onSurface,
                )
            }
            Spacer(Modifier.weight(1f))
            if (temp != null) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(M3ETokens.RoomSheet.DishwasherHeroGap),
                ) {
                    Text(
                        "$temp°C",
                        // (Viec 5) lineHeight tu nhien thay vi M3 28sp
                        // (iOS titleLarge 22pt tu nhien ~26,8): chi dong
                        // nhat chu, khong doi chieu cao hero (cot trai ap dao).
                        style = MaterialTheme.typography.titleLarge.copy(
                            lineHeight = M3ETokens.naturalLineHeight(22.sp),
                        ),
                        color = cs.onSurface,
                    )
                    Text(
                        "Nhiệt độ nước",
                        // (Viec 5) lineHeight tu nhien thay vi M3 16sp
                        // (iOS bodySmall 12pt tu nhien ~14,6): cot phai
                        // 46dp -> 43,4dp nhu iOS.
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = M3ETokens.Text.BodySmall.fontSize,
                            lineHeight = M3ETokens.Text.BodySmall.lineHeight,
                        ),
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
            // (Viec 5) lineHeight tu nhien 19,5 thay vi M3 24sp (-4,5dp):
            // iOS titleMedium 16pt tu nhien.
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = M3ETokens.Text.SectionTitle.fontSize,
                lineHeight = M3ETokens.Text.SectionTitle.lineHeight,
            ),
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
        // (2026-11-18) Khoi phuc: khe khoi chuong trinh 6->10dp (iOS 10pt).
        Column(verticalArrangement = Arrangement.spacedBy(M3ETokens.RoomSheet.DishwasherProgramGap)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { programOpen = !programOpen },
            ) {
                Text(
                    "CHƯƠNG TRÌNH",
                    // (Viec 5) lineHeight tu nhien 13,4 thay vi M3 16sp:
                    // iOS labelSmall 11pt tu nhien.
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = M3ETokens.Text.Caption.fontSize,
                        lineHeight = M3ETokens.Text.Caption.lineHeight,
                    ),
                    letterSpacing = 0.5.sp,
                    color = cs.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(cs.primaryContainer)
                        // (2026-11-18) Khoi phuc: padding doc chip chuong trinh 3->4dp (iOS 4dp -> chip 25,1dp).
                        .padding(horizontal = M3ETokens.Spacing.RowGap, vertical = M3ETokens.RoomSheet.DishwasherProgramChipPaddingV),
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
                        // (Viec 5) lineHeight tu nhien 17,1 thay vi M3 20sp:
                        // iOS labelLarge 14pt tu nhien; chip 28dp -> 25,1dp,
                        // hang tieu de chuong trinh 28dp -> 25,1dp (-2,9dp).
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = M3ETokens.Text.ButtonText.fontSize,
                            lineHeight = M3ETokens.Text.ButtonText.lineHeight,
                        ),
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onPrimaryContainer,
                    )
                }
                Spacer(Modifier.size(M3ETokens.Spacing.Sm))
                // iOS RoomExpandedCards.swift:200 chevron
                // .font(.system(size: 12)) — Android cu: 14dp.
                Icon(
                    Icons.Outlined.KeyboardArrowDown, null,
                    tint = cs.onSurfaceVariant,
                    modifier = Modifier
                        .size(12.dp)
                        .graphicsLayer { rotationZ = chevronRot },
                )
            }
            AnimatedVisibility(
                visible = programOpen,
                enter = expandVertically(tween(450, easing = M3EMotion.spring)) +
                    fadeIn(tween(450, easing = M3EMotion.spring)),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(M3ETokens.Spacing.Md),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    p.all.forEach { prog ->
                        val isCurrent = prog.machineValue == programRaw
                        val known = prog.machineValue != null
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            // (2026-11-18) Khoi phuc: khe icon-nhan chuong trinh 2->4dp (iOS 4pt).
                            verticalArrangement = Arrangement.spacedBy(M3ETokens.RoomSheet.DishwasherProgramItemGap),
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
                                    // (2026-11-18) Khoi phuc: vong tron chuong trinh 44->56dp, icon 20->26dp (iOS 56/26pt).
                                    .size(M3ETokens.RoomSheet.DishwasherProgramCircle)
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
                                    modifier = Modifier.size(M3ETokens.RoomSheet.DishwasherProgramIcon),
                                )
                            }
                            Text(
                                prog.display,
                                // (Viec 5) lineHeight tu nhien 13,4 thay vi M3
                                // 16sp (-2,6dp): iOS labelSmall 11pt tu nhien;
                                // item chuong trinh 76dp -> 73,4dp nhu iOS.
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = M3ETokens.Text.Caption.fontSize,
                                    lineHeight = M3ETokens.Text.Caption.lineHeight,
                                ),
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
        Row(horizontalArrangement = Arrangement.spacedBy(M3ETokens.Spacing.Md)) {
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

        // NUOC MEM: nhan + nut -/+ 56dp + slider 3–15, gui khi tha tay
        // qua number.set_value DP 106, hien thi "x/15".
        // (2026-11-18) Khoi phuc: khe khoi nuoc mem 6->10dp (iOS 10pt).
        Column(verticalArrangement = Arrangement.spacedBy(M3ETokens.RoomSheet.DishwasherSectionGap)) {
            Text(
                "NƯỚC MỀM",
                // (Viec 5) lineHeight tu nhien 13,4 thay vi M3 16sp (-2,6dp):
                // iOS labelSmall 11pt tu nhien.
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = M3ETokens.Text.Caption.fontSize,
                    lineHeight = M3ETokens.Text.Caption.lineHeight,
                ),
                letterSpacing = 0.5.sp,
                color = cs.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                DishwasherStepperButton(
                    icon = Icons.Outlined.Remove,
                    enabled = softLevel > 3,
                    onClick = {
                        val v = maxOf(3, softLevel - 1)
                        actions.onSetNumber(p.softNumber, v.toDouble())
                        softSlider = v.toFloat()
                    },
                )
                Spacer(Modifier.size(M3ETokens.Spacing.Md))
                Slider(
                    value = softSlider,
                    onValueChange = {
                        softSlider = it
                        draggingSoft = true
                    },
                    valueRange = 3f..15f,
                    steps = 11,
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
                Spacer(Modifier.size(M3ETokens.Spacing.Md))
                DishwasherStepperButton(
                    icon = Icons.Outlined.Add,
                    enabled = softLevel < 15,
                    onClick = {
                        val v = minOf(15, softLevel + 1)
                        actions.onSetNumber(p.softNumber, v.toDouble())
                        softSlider = v.toFloat()
                    },
                )
                Spacer(Modifier.size(M3ETokens.Spacing.Md))
                Row {
                    Text(
                        "$softLevel",
                        // (Viec 5) lineHeight tu nhien thay vi M3 44sp
                        // (iOS displaySmall 36pt tu nhien ~43,9): chi dong
                        // nhat chu, khong doi chieu cao hang (stepper 56dp ap dao).
                        style = MaterialTheme.typography.displaySmall.copy(
                            lineHeight = M3ETokens.naturalLineHeight(36.sp),
                        ),
                        color = cs.onSurface,
                        modifier = Modifier.alignByBaseline(),
                    )
                    Text(
                        "/15",
                        // (Viec 5) lineHeight tu nhien thay vi M3 20sp
                        // (iOS bodyMedium 14pt tu nhien ~17,1): nhu tren.
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = M3ETokens.Text.Body.fontSize,
                            lineHeight = M3ETokens.Text.Body.lineHeight,
                        ),
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = cs.surfaceContainerHigh)

        // HEN GIO BAT DAU: chi nut -/+ buoc 1 gio, 0–1440 phut (DP 8).
        // (2026-11-18) Khoi phuc: khe khoi hen gio 6->10dp (iOS 10pt).
        Column(verticalArrangement = Arrangement.spacedBy(M3ETokens.RoomSheet.DishwasherSectionGap)) {
            Text(
                "HẸN GIỜ BẮT ĐẦU",
                // (Viec 5) lineHeight tu nhien 13,4 thay vi M3 16sp (-2,6dp):
                // iOS labelSmall 11pt tu nhien.
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = M3ETokens.Text.Caption.fontSize,
                    lineHeight = M3ETokens.Text.Caption.lineHeight,
                ),
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
                    // (Viec 5) lineHeight tu nhien thay vi M3 44sp
                    // (iOS displaySmall 36pt tu nhien ~43,9): chi dong
                    // nhat chu, khong doi chieu cao hang (stepper 56dp ap dao).
                    style = MaterialTheme.typography.displaySmall.copy(
                        lineHeight = M3ETokens.naturalLineHeight(36.sp),
                    ),
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
            horizontalArrangement = Arrangement.spacedBy(M3ETokens.Spacing.Md),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    // (2026-11-18) Khoi phuc: nut nguon rua bat 44->56dp (iOS 56pt; icon 24 giu).
                    .size(M3ETokens.RoomSheet.DishwasherPower)
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
                    modifier = Modifier.size(M3ETokens.Icon.Standard),
                )
            }
            // Dung: CHUA noi HA — khong co DP chac chan ket thuc chu trinh.
            // Nut hien dung vi tri/kich thuoc REA (60dp, vuong do khi dang
            // chay) de cho test chot DP roi noi 1 dong, KHONG bam duoc.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    // (2026-11-18) Khoi phuc: nut Dung 44->60dp (iOS 60pt; icon 24 giu).
                    .size(M3ETokens.RoomSheet.DishwasherStop)
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
                    modifier = Modifier.size(M3ETokens.Icon.Standard),
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(cs.primary)
                    .clickable { actions.onToggle(p.startSwitch) }
                    // (2026-11-18) Khoi phuc: padding doc nut Bat dau 10->16dp (iOS 16dp -> nut 49,1dp).
                    .padding(vertical = M3ETokens.RoomSheet.DishwasherStartPaddingV),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(M3ETokens.Spacing.Sm),
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
                        // (Viec 5) lineHeight tu nhien thay vi M3 20sp
                        // (iOS labelLarge 14pt tu nhien ~17,1): nut
                        // 52dp -> 49,1dp; hang nut van cao 60dp (nut Dung
                        // ap dao) nen khong doi chieu cao hang.
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = M3ETokens.Text.ButtonText.fontSize,
                            lineHeight = M3ETokens.Text.ButtonText.lineHeight,
                        ),
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
    // (2026-11-18) Khoi phuc: khe doan rua 6->10dp (iOS 10pt).
    Column(verticalArrangement = Arrangement.spacedBy(M3ETokens.RoomSheet.DishwasherSegmentGap)) {
        // Thanh manh: nen + phan da chay (scale theo progress).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(M3ETokens.Spacing.Xs)
                .clip(CircleShape)
                .background(cs.surfaceContainerHigh),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(M3ETokens.Spacing.Xs)
                    .clip(CircleShape)
                    .background(cs.primary),
            )
        }
        Row {
            segments.forEachIndexed { i, (label, icon) ->
                val done = i <= current && current >= 0
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    // (2026-11-18) Khoi phuc: khe icon-nhan doan 4->6dp (iOS 6pt; node 20->24dp, icon 11 giu).
                    verticalArrangement = Arrangement.spacedBy(M3ETokens.RoomSheet.DishwasherSegmentLabelGap),
                    modifier = Modifier.weight(1f),
                ) {
                    // Moi buoc 1 icon; trang thai bang mau nen cham:
                    // da qua/cham toi = primary, chua toi = surface.
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(M3ETokens.RoomSheet.DishwasherSegmentNode)
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
                        // (Viec 5) lineHeight tu nhien 13,4 thay vi M3 16sp
                        // (-2,6dp): iOS labelSmall 11pt tu nhien.
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = M3ETokens.Text.Caption.fontSize,
                            lineHeight = M3ETokens.Text.Caption.lineHeight,
                        ),
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
            .padding(horizontal = M3ETokens.Spacing.Md, vertical = M3ETokens.RoomSheet.DishwasherMetaPillPaddingV),
    ) {
        Icon(icon, null, tint = cs.primary, modifier = Modifier.size(12.dp))
        Text(
            value,
            // (Viec 5) lineHeight tu nhien 17,1 thay vi M3 20sp (-2,9dp):
            // iOS labelLarge 14pt tu nhien; pill 36dp -> 33,1dp nhu iOS.
            // (2026-11-18) Khoi phuc: padding doc pill 5->8dp (pill 27,1->33,1dp, token).
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = M3ETokens.Text.ButtonText.fontSize,
                lineHeight = M3ETokens.Text.ButtonText.lineHeight,
            ),
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
            // (2026-11-18) Giam 6dp: o tang cuong 76->70dp (vong trong 40dp/icon 18dp giu; ca 2 trang thai dong/mo deu -6dp).
            .height(M3ETokens.RoomSheet.DishwasherBoost)
            .clip(RoundedCornerShape(M3ETokens.Corner.Card))
            .background(if (isOn) cs.primaryContainer else cs.surfaceContainerHigh)
            .alpha(if (known) 1f else 0.5f)
            .clickable { onToggle(entity) },
    ) {
        // Vong tron trang trong o nhu HTML.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                // (2026-11-18) Khoi phuc: vong tron trong 32->40dp, icon 16->18dp (iOS 40/18pt).
                .size(M3ETokens.RoomSheet.DishwasherBoostCircle)
                .clip(CircleShape)
                .background(Color.White),
        ) {
            Icon(
                icon, label,
                tint = if (isOn) cs.primary else cs.onSurfaceVariant,
                modifier = Modifier.size(M3ETokens.RoomSheet.DishwasherBoostIcon),
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
            // (2026-11-18) Khoi phuc: nut stepper rua bat 44->56dp, icon 18->20dp (iOS 56/20pt).
            .size(M3ETokens.RoomSheet.DishwasherStepper)
            .clip(CircleShape)
            .background(cs.surfaceContainerHigh)
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled) { onClick() },
    ) {
        Icon(icon, null, tint = cs.onSurface, modifier = Modifier.size(M3ETokens.RoomSheet.DishwasherStepperIcon))
    }
}

@Composable
fun ExpandedInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            // (2026-11-17) lineHeight tu nhien 17,1 thay vi M3 20 (-2,9dp/dong).
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = M3ETokens.Text.Body.lineHeight),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = M3ETokens.Text.Body.lineHeight),
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

    // (2026-11-18) GIU khe doc tong the den 8dp, LECH CO CHU Y so voi iOS 14pt:
    // Slider M3 min 48dp (iOS ~31pt) + khoi mau 2 hang da cong +47dp; khe 14
    // se day den len 199,7dp (+53 so voi iOS).
    // Giam 6dp vong 18/11: khoi mau 68->62dp (cham 32->28, hue 28->26);
    // tong khong hieu ung 124->118dp, co hieu ung 187,7->181,7dp.
    Column(verticalArrangement = Arrangement.spacedBy(M3ETokens.RoomSheet.LightGap)) {
        // Do sang.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.WbSunny, null,
                tint = cs.primary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(M3ETokens.Spacing.Md))
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
            Spacer(Modifier.size(M3ETokens.Spacing.Md))
            Text(
                "${brightness.toInt()}%",
                // (Viec 5) lineHeight tu nhien thay vi M3 20sp
                // (iOS labelLarge 14pt tu nhien ~17,1): chi dong nhat
                // chu, khong doi chieu cao hang (Slider M3 48dp ap dao —
                // xem bao cao: iOS Slider ~31pt, phan thua 17dp nay can
                // quyet dinh rieng truoc khi cat).
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = M3ETokens.Text.ButtonText.fontSize,
                    lineHeight = M3ETokens.Text.ButtonText.lineHeight,
                ),
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurface,
                textAlign = TextAlign.End,
                modifier = Modifier.width(44.dp),
            )
        }
        // Mau sac — hue slider (track gradient) + mau hien tai.
        // (2026-11-18) Khoi phuc: khe khoi mau sac 5->8dp (iOS 8pt; khoi mau 32dp / hue 28dp giu).
        Column(verticalArrangement = Arrangement.spacedBy(M3ETokens.RoomSheet.LightInnerGap)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Màu sắc",
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = M3ETokens.Text.Body.lineHeight),
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface,
                )
                Spacer(Modifier.weight(1f))
                val rgb = currentRgb()
                Box(
                    modifier = Modifier
                        // (2026-11-18) Giam 6dp: cham mau 32->28dp (token; khoi mau 68->62dp).
                        .size(M3ETokens.RoomSheet.LightColorDot)
                        .clip(CircleShape)
                        .background(Color(rgb[0], rgb[1], rgb[2]))
                        .border(1.dp, cs.outlineVariant, CircleShape),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // (2026-11-18) Giam 6dp: thanh hue 28->26dp (token; khoi mau 68->62dp).
                    .height(M3ETokens.RoomSheet.LightHueHeight)
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
            // (2026-11-18) Khoi phuc: khe khoi hieu ung 5->8dp (iOS 8pt).
            Column(verticalArrangement = Arrangement.spacedBy(M3ETokens.RoomSheet.LightInnerGap)) {
                Text(
                    "Hiệu ứng",
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = M3ETokens.Text.Body.lineHeight),
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(M3ETokens.Spacing.Sm)) {
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
                                // (2026-11-18) Khoi phuc: padding doc chip hieu ung 5->8dp (chip 24,6->30,6dp, iOS 30,6pt).
                                .padding(horizontal = M3ETokens.Spacing.Md, vertical = M3ETokens.RoomSheet.LightEffectChipPaddingV),
                        ) {
                            Text(
                                effectDisplay(eff),
                                // (2026-11-17) lineHeight tu nhien 14,6 thay vi M3 16 (-1,4dp).
                                style = MaterialTheme.typography.labelMedium.copy(lineHeight = M3ETokens.Text.ButtonTextSmall.lineHeight),
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
