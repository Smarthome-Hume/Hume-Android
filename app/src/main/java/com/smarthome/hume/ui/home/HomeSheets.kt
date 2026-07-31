@file:OptIn(ExperimentalMaterial3Api::class)

package com.smarthome.hume.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.core.model.RoomConfig
import com.smarthome.hume.ui.theme.HumeIcons

/** Room popup from HomeView.swift. */
@Composable
fun RoomBottomSheet(
    room: RoomConfig,
    ha: HomeAssistantRepository,
    entities: Map<String, HomeEntity>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(HumeIcons.room(room.icon), contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text(room.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(16.dp))

            SheetRow(
                label = "Đèn",
                value = if (entities[room.lightEntity]?.isOn == true) "Bật" else "Tắt",
                trailing = {
                    Switch(
                        checked = entities[room.lightEntity]?.isOn == true,
                        onCheckedChange = { setLight(ha, room.lightEntity, it) },
                    )
                },
            )
            HorizontalDivider()
            SheetRow(label = "Nhiệt độ", value = entities.num(room.tempEntity, 1) + "°C")
            HorizontalDivider()
            SheetRow(label = "Độ ẩm", value = entities.num(room.humidityEntity, 0) + "%")

            if (room.hasClimate && room.climateEntity != null) {
                HorizontalDivider()
                val climate = entities[room.climateEntity]
                val target = entities.attr(room.climateEntity, "temperature")
                SheetRow(
                    label = "Điều hòa",
                    value = buildString {
                        append(climate?.state ?: "không rõ")
                        if (target != null) append(" · đặt $target°C")
                    },
                )
            }

            room.contactEntity?.let { contact ->
                HorizontalDivider()
                val state = entities[contact]?.state
                SheetRow(
                    label = if (contact.contains("occupancy")) "Cảm biến chuyển động" else "Cảm biến cửa",
                    value = when (state) {
                        "on" -> if (contact.contains("occupancy")) "Có người" else "Đang mở"
                        "off" -> if (contact.contains("occupancy")) "Không có ai" else "Đã đóng"
                        else -> "không rõ"
                    },
                )
            }

            Spacer(Modifier.height(18.dp))
            Text("Entity ID", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            listOfNotNull(room.lightEntity, room.tempEntity, room.humidityEntity, room.climateEntity, room.contactEntity).forEach { id ->
                Text(
                    id + "  →  " + (entities[id]?.state ?: "KHÔNG TỒN TẠI"),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (entities[id] == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Lights popup from HomeView.swift. */
@Composable
fun LightsBottomSheet(
    rooms: List<RoomConfig>,
    ha: HomeAssistantRepository,
    entities: Map<String, HomeEntity>,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Tất cả đèn", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { setAllLights(ha, rooms, true) }) { Text("Bật hết") }
                    OutlinedButton(onClick = { setAllLights(ha, rooms, false) }) { Text("Tắt hết") }
                }
            }
            Spacer(Modifier.height(12.dp))
            rooms.forEach { room ->
                SheetRow(
                    label = room.name,
                    value = if (entities[room.lightEntity]?.isOn == true) "Bật" else "Tắt",
                    leading = {
                        Icon(HumeIcons.room(room.icon), contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                    },
                    trailing = {
                        Switch(
                            checked = entities[room.lightEntity]?.isOn == true,
                            onCheckedChange = { setLight(ha, room.lightEntity, it) },
                        )
                    },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun SheetRow(
    label: String,
    value: String,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        trailing?.invoke()
    }
}
