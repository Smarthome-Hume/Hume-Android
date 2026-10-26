package com.smarthome.hume.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smarthome.hume.core.model.DeviceUi
import com.smarthome.hume.core.model.HomeEntity
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
    // D1: chi generic. D2 chen nhanh rua bat (gate DishwasherIds),
    // D3 chen nhanh den (light.smartlight / light.table_led) vao day.
    @Suppress("UNUSED_PARAMETER")
    val unusedActions = actions
    GenericExpandedView(device, entityStates)
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
