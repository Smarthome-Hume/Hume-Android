package com.smarthome.hume.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.ui.components.M3EIcons
import com.smarthome.hume.core.ui.components.M3ETextField
import com.smarthome.hume.core.ui.components.Ms
import com.smarthome.hume.core.ui.components.MsIcon
import com.smarthome.hume.core.ui.components.pressMorph

/**
 * Chon icon 2 che do (port iOS IconPickerGrid, RoomManagementView.swift — user 2026-10-09):
 * - Mode 1: go ten Material Symbol bat ky + preview truc tiep.
 * - Mode 2: bam chon nhanh tu luoi phan nhom (12 nhom nhu iOS, GAP-11).
 *
 * [selection] la TEN icon (vd "lightbulb"), khong phai glyph — M3ETextField va luoi
 * dung chung mot nguon su that; glyph giai quyet qua [msGlyphByName],
 * vector ve tay qua [vectorByName] (hien chi "fan" -> FanFa).
 * Ten go tay khong giai quyet duoc hien "?" (khong ro), khong hien icon loi.
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
    val previewVector = vectorByName[trimmed]

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Mode 1: o go ten (M3ETextField) + preview 48dp bo 16dp primaryContainer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            M3ETextField(
                value = selection,
                onValueChange = onSelectionChange,
                label = "Tên icon",
                placeholder = "vd: lightbulb",
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false,
                ),
            )
            Column {
                // Can preview ngang voi o nhap (bo qua phan nhan phia tren).
                Spacer(Modifier.height(27.dp))
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(cs.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    // Ten la (khong co glyph/vector) -> giu trang thai "khong ro"
                    // ("?") thay vi hien icon loi Ms.error gay hieu lam (GAP-11).
                    when {
                        previewGlyph != null -> MsIcon(
                            glyph = previewGlyph,
                            contentDescription = selection,
                            modifier = Modifier.size(24.dp),
                            tint = cs.onPrimaryContainer,
                        )
                        previewVector != null -> Icon(
                            imageVector = previewVector,
                            contentDescription = selection,
                            modifier = Modifier.size(24.dp),
                            tint = cs.onPrimaryContainer,
                        )
                        else -> Text(
                            text = "?",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = cs.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Mode 2: luoi phan nhom — o 52dp bo 16dp, gap 12dp;
        // dang chon = primaryContainer, chua chon = surfaceContainerHighest.
        iconSections.forEach { (title, names) ->
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurfaceVariant,
                    letterSpacing = 0.4.sp,
                    modifier = Modifier.padding(start = 4.dp),
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    names.forEach { name ->
                        // Ten khong co glyph lan vector thi bo qua (khong hardcode).
                        // Moi ten trong iconSections deu phai giai quyet duoc 1 trong
                        // 2 map — neu luoi thieu o thi la loi dich, khong phai
                        // thieu glyph am tham.
                        val glyph = msGlyphByName[name]
                        val vector = vectorByName[name]
                        if (glyph == null && vector == null) return@forEach
                        val isSelected = trimmed == name
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) cs.primaryContainer
                                    else cs.surfaceContainerHighest,
                                )
                                .pressMorph(pressedScale = 0.9f) { onSelectionChange(name) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (glyph != null) {
                                MsIcon(
                                    glyph = glyph,
                                    contentDescription = name,
                                    modifier = Modifier.size(22.dp),
                                    tint = if (isSelected) cs.onPrimaryContainer
                                    else cs.onSurfaceVariant,
                                )
                            } else {
                                Icon(
                                    imageVector = vector!!,
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
}

/**
 * Cac nhom icon dich tu iOS sang Material Symbols (GAP-11, 12 nhom dung
 * thu tu/ten nhom cua iOS IconPickerGrid, RoomManagementView.swift).
 * Moi ten phai giai quyet duoc qua [msGlyphByName] hoac [vectorByName]:
 * - "fan" khong co glyph Material Symbols nen map sang vector [M3EIcons.FanFa].
 * - Ten SF Symbol khong co tuong duong MS trong font goc v2.978 thi BO
 *   QUA va ghi ro o cuoi file (khong them hang Ms de khoi fallback chu Han).
 */
private val iconSections: List<Pair<String, List<String>>> = listOf(
    "Đèn" to listOf(
        "lightbulb", "lightbulb_2", "lightbulb_circle", "floor_lamp", "table_lamp",
        "wb_sunny", "wb_twilight", "flashlight_on", "flashlight_off",
        "light_mode", "light_group",
    ),
    "Công tắc & Ổ cắm" to listOf(
        "power", "power_settings_new", "bolt",
        "switch_left", "switch_right", "toggle_on", "toggle_off",
        "outlet", "electrical_services",
    ),
    "Điều hoà & Không khí" to listOf(
        "ac_unit", "snowflake", "fan", "thermometer", "thermostat", "device_thermostat",
        "whatshot", "water_drop",
        "humidity_percentage", "humidity_mid", "humidity_high", "humidity_low",
        "air_purifier_gen", "heat_pump", "mode_heat",
    ),
    "Nhà bếp" to listOf(
        "cooking", "dishwasher", "soup_kitchen",
        "oven_gen", "microwave_gen", "stockpot", "flatware", "restaurant",
        "coffee", "local_cafe",
    ),
    "Thiết bị gia dụng" to listOf("local_laundry_service", "print", "scanner", "vacuum"),
    "An ninh" to listOf(
        "videocam", "shield", "lock", "lock_open", "notifications", "notifications_off",
        "key", "visibility", "visibility_off", "smoke_free",
    ),
    "Giải trí" to listOf(
        "play_circle", "mic", "tv", "speaker", "speaker_group", "music_note",
        "sports_esports", "monitor", "desktop_windows",
    ),
    "Cảm biến & Mạng" to listOf(
        "sensors", "wifi", "battery_full", "person", "person_search",
        "cell_tower", "directions_walk", "graphic_eq", "speed", "airwave",
    ),
    "Rèm & Cửa" to listOf(
        "door_front", "door_open", "door_back", "door_sliding",
        "blinds", "blinds_closed", "curtains", "curtains_closed",
        "garage", "window",
    ),
    "Nước" to listOf(
        "sprinkler", "shower", "faucet", "water_drop", "waves", "pool", "water_heater",
    ),
    "Năng lượng" to listOf(
        "solar_power", "bolt", "battery_charging_full",
        "eco", "energy", "local_gas_station", "ev_station",
    ),
    "Nội thất & Khác" to listOf(
        "bed", "weekend", "bathtub", "home", "desk", "stairs",
        "chair", "table_restaurant",
    ),
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
    // (2026-10-26, Batch E GAP-11) 64 ten moi — moi ten da kiem tra
    // codepoint co trong font subset moi (150 glyph); xem MsIcon.kt.
    "air_purifier_gen" to Ms.air_purifier_gen,
    "airwave" to Ms.airwave,
    "blinds" to Ms.blinds,
    "blinds_closed" to Ms.blinds_closed,
    "cell_tower" to Ms.cell_tower,
    "chair" to Ms.chair,
    "coffee" to Ms.coffee,
    "curtains" to Ms.curtains,
    "curtains_closed" to Ms.curtains_closed,
    "desktop_windows" to Ms.desktop_windows,
    "directions_walk" to Ms.directions_walk,
    "door_back" to Ms.door_back,
    "door_sliding" to Ms.door_sliding,
    "eco" to Ms.eco,
    "electrical_services" to Ms.electrical_services,
    "energy" to Ms.energy,
    "ev_station" to Ms.ev_station,
    "faucet" to Ms.faucet,
    "flashlight_off" to Ms.flashlight_off,
    "flashlight_on" to Ms.flashlight_on,
    "flatware" to Ms.flatware,
    "floor_lamp" to Ms.floor_lamp,
    "garage" to Ms.garage,
    "graphic_eq" to Ms.graphic_eq,
    "heat_pump" to Ms.heat_pump,
    "humidity_high" to Ms.humidity_high,
    "humidity_low" to Ms.humidity_low,
    "humidity_mid" to Ms.humidity_mid,
    "humidity_percentage" to Ms.humidity_percentage,
    "light_group" to Ms.light_group,
    "lightbulb_2" to Ms.lightbulb_2,
    "lightbulb_circle" to Ms.lightbulb_circle,
    "local_cafe" to Ms.local_cafe,
    "local_gas_station" to Ms.local_gas_station,
    "microwave_gen" to Ms.microwave_gen,
    "mode_heat" to Ms.mode_heat,
    "monitor" to Ms.monitor,
    "music_note" to Ms.music_note,
    "outlet" to Ms.outlet,
    "oven_gen" to Ms.oven_gen,
    "pool" to Ms.pool,
    "print" to Ms.print,
    "restaurant" to Ms.restaurant,
    "scanner" to Ms.scanner,
    "shower" to Ms.shower,
    "speaker" to Ms.speaker,
    "speaker_group" to Ms.speaker_group,
    "speed" to Ms.speed,
    "sports_esports" to Ms.sports_esports,
    "sprinkler" to Ms.sprinkler,
    "stockpot" to Ms.stockpot,
    "switch_left" to Ms.switch_left,
    "switch_right" to Ms.switch_right,
    "table_lamp" to Ms.table_lamp,
    "table_restaurant" to Ms.table_restaurant,
    "thermometer" to Ms.thermometer,
    "toggle_off" to Ms.toggle_off,
    "toggle_on" to Ms.toggle_on,
    "tv" to Ms.tv,
    "vacuum" to Ms.vacuum,
    "water_heater" to Ms.water_heater,
    "waves" to Ms.waves,
    "wb_twilight" to Ms.wb_twilight,
    "window" to Ms.window,
)

/**
 * Map ten icon -> vector ve tay cho nhung icon khong co glyph Material
 * Symbols trong font goc (hien chi co "fan" -> [M3EIcons.FanFa]; Ms khong
 * co glyph quat nen KHONG them hang Ms.fan — them se fallback chu Han).
 * Luoi va preview uu tien glyph truoc, roi moi toi map nay.
 */
private val vectorByName: Map<String, ImageVector> = mapOf(
    "fan" to M3EIcons.FanFa,
)

/**
 * Ten SF Symbol cua iOS KHONG co tuong duong trong font goc v2.978 nen
 * co tinh bo qua khoi luoi (da kiem tra cmap font goc, khong phai doan):
 * - Den: lamp.desk (khong co desk_lamp/lamp_desk), sunrise/sunset,
 *   light.beacon (khong co beacon), lightbulb.led (chi co lightbulb_2/circle).
 * - Cong tac: powerplug/powercord/poweroutlet (chi co outlet tuong duong).
 * - Dieu hoa: fan.ceiling/desk (chi co 1 vector FanFa chung), humidifier.
 * - Bep: refrigerator (khong co tuong duong MS).
 * - Gia dung: washer rieng/dryer (chi co local_laundry_service chung),
 *   air.conditioner rieng (da co ac_unit o nhom Dieu hoa).
 * - An ninh: shield.checkerboard, bell.badge, smoke (chi co smoke_free).
 * - Giai tri: projector, display rieng (da co monitor/desktop_windows).
 * - Cam bien: aqi, antenna rieng (da co cell_tower), person.viewfinder
 *   (da co person_search), gauge rieng (da co speed).
 * - Cua: door.garage.open/closed rieng (da co garage chung),
 *   door.left.hand (da co door_front/door_open/door_back).
 * - Nuoc: toilet, sprinkler.and.droplets (da co sprinkler + waves).
 * - Nang luong: solarpanel rieng (da co solar_power), leaf rieng (da co eco).
 * - Noi that: sofa rieng (da co weekend), bed.double rieng (da co bed).
 * O go ten van nhan bat ky ten nao; ten khong giai quyet duoc hien "?".
 */
