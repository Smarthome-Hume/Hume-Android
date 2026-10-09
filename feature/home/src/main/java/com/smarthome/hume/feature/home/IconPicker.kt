package com.smarthome.hume.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.smarthome.hume.core.ui.components.Ms
import com.smarthome.hume.core.ui.components.MsIcon

/**
 * Chon icon 2 che do (port iOS IconPickerGrid, RoomManagementView.swift — user 2026-10-09):
 * - Mode 1: go ten Material Symbol bat ky + preview truc tiep.
 * - Mode 2: bam chon nhanh tu luoi phan nhom.
 *
 * [selection] la TEN icon (vd "lightbulb"), khong phai glyph — TextField va luoi
 * dung chung mot nguon su that; glyph giai quyet qua [msGlyphByName].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconPicker(
    selection: String,
    onSelectionChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val trimmed = selection.trim()
    val previewGlyph = msGlyphByName[trimmed]

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Mode 1: o go ten + preview truc tiep
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = selection,
                onValueChange = onSelectionChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Tên icon (vd: lightbulb)") },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false,
                ),
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(cs.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                MsIcon(
                    glyph = previewGlyph ?: Ms.error,
                    contentDescription = selection,
                    modifier = Modifier.size(24.dp),
                    tint = if (previewGlyph != null) cs.primary else cs.onSurfaceVariant,
                )
            }
        }

        // Mode 2: luoi phan nhom
        iconSections.forEach { (title, names) ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = cs.onSurfaceVariant,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    names.forEach { name ->
                        // Ten khong co trong Ms thi bo qua (khong hardcode).
                        val glyph = msGlyphByName[name] ?: return@forEach
                        val isSelected = trimmed == name
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) cs.primaryContainer
                                    else cs.surfaceContainerHighest,
                                )
                                .clickable { onSelectionChange(name) },
                            contentAlignment = Alignment.Center,
                        ) {
                            MsIcon(
                                glyph = glyph,
                                contentDescription = name,
                                modifier = Modifier.size(22.dp),
                                tint = if (isSelected) cs.onPrimaryContainer
                                else cs.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Cac nhom icon dich tu iOS sang Material Symbols.
 * CHI gom ten TON TAI trong [Ms] — ten khong co da bi loai khi dich.
 */
private val iconSections: List<Pair<String, List<String>>> = listOf(
    "Đèn" to listOf("lightbulb", "wb_sunny", "light_mode"),
    "Công tắc & Ổ cắm" to listOf("power", "power_settings_new", "bolt"),
    "Điều hoà & Không khí" to listOf("ac_unit", "snowflake", "thermostat", "device_thermostat", "water_drop"),
    "Nhà bếp" to listOf("cooking", "dishwasher", "soup_kitchen"),
    "An ninh" to listOf("videocam", "shield", "lock", "notifications", "key"),
    "Giải trí" to listOf("play_circle", "mic"),
    "Cảm biến & Mạng" to listOf("sensors", "wifi", "battery_full", "person"),
    "Rèm & Cửa" to listOf("door_front", "door_open"),
    "Năng lượng" to listOf("solar_power", "bolt", "battery_charging_full"),
    "Nội thất" to listOf("bed", "weekend", "bathtub", "home", "desk"),
)

/**
 * Map ten icon -> glyph, lay tu [Ms] (MsIcon.kt).
 * Dung de preview ten go tay va render luoi; ten khong co trong map
 * nghia la icon do khong ton tai — hien placeholder thay vi crash.
 */
private val msGlyphByName: Map<String, String> = mapOf(
    "ac_unit" to Ms.ac_unit,
    "add" to Ms.add,
    "arrow_back" to Ms.arrow_back,
    "auto_awesome" to Ms.auto_awesome,
    "bathtub" to Ms.bathtub,
    "battery_0_bar" to Ms.battery_0_bar,
    "battery_1_bar" to Ms.battery_1_bar,
    "battery_2_bar" to Ms.battery_2_bar,
    "battery_3_bar" to Ms.battery_3_bar,
    "battery_4_bar" to Ms.battery_4_bar,
    "battery_5_bar" to Ms.battery_5_bar,
    "battery_6_bar" to Ms.battery_6_bar,
    "battery_alert" to Ms.battery_alert,
    "battery_charging_full" to Ms.battery_charging_full,
    "battery_full" to Ms.battery_full,
    "bed" to Ms.bed,
    "bedtime" to Ms.bedtime,
    "bolt" to Ms.bolt,
    "check" to Ms.check,
    "chevron_right" to Ms.chevron_right,
    "child_care" to Ms.child_care,
    "close" to Ms.close,
    "cooking" to Ms.cooking,
    "dark_mode" to Ms.dark_mode,
    "desk" to Ms.desk,
    "device_thermostat" to Ms.device_thermostat,
    "dishwasher" to Ms.dishwasher,
    "donut_large" to Ms.donut_large,
    "door_front" to Ms.door_front,
    "electric_meter" to Ms.electric_meter,
    "error" to Ms.error,
    "expand_more" to Ms.expand_more,
    "fiber_manual_record" to Ms.fiber_manual_record,
    "flight_takeoff" to Ms.flight_takeoff,
    "fullscreen" to Ms.fullscreen,
    "home" to Ms.home,
    "info" to Ms.info,
    "key" to Ms.key,
    "language" to Ms.language,
    "light_mode" to Ms.light_mode,
    "lightbulb" to Ms.lightbulb,
    "link" to Ms.link,
    "local_laundry_service" to Ms.local_laundry_service,
    "lock" to Ms.lock,
    "logout" to Ms.logout,
    "meeting_room" to Ms.meeting_room,
    "mic" to Ms.mic,
    "notifications" to Ms.notifications,
    "notifications_off" to Ms.notifications_off,
    "palette" to Ms.palette,
    "person" to Ms.person,
    "person_search" to Ms.person_search,
    "photo_camera" to Ms.photo_camera,
    "play_circle" to Ms.play_circle,
    "power" to Ms.power,
    "power_settings_new" to Ms.power_settings_new,
    "remove" to Ms.remove,
    "search" to Ms.search,
    "sensors" to Ms.sensors,
    "settings_remote" to Ms.settings_remote,
    "shield" to Ms.shield,
    "signal_cellular_alt" to Ms.signal_cellular_alt,
    "smoke_free" to Ms.smoke_free,
    "solar_power" to Ms.solar_power,
    "soup_kitchen" to Ms.soup_kitchen,
    "stairs" to Ms.stairs,
    "thermostat" to Ms.thermostat,
    "videocam" to Ms.videocam,
    "visibility" to Ms.visibility,
    "visibility_off" to Ms.visibility_off,
    "water_drop" to Ms.water_drop,
    "wb_sunny" to Ms.wb_sunny,
    "weekend" to Ms.weekend,
    "whatshot" to Ms.whatshot,
    "wifi" to Ms.wifi,
    "remote_gen" to Ms.remote_gen,
    "snowflake" to Ms.snowflake,
    "lock_open" to Ms.lock_open,
    "door_open" to Ms.door_open,
)
