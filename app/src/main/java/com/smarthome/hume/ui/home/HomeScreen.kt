package com.smarthome.hume.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.DefaultRooms
import com.smarthome.hume.core.model.RoomConfig
import com.smarthome.hume.ui.theme.HumeColors

@Composable
fun HomeScreen(ha: HomeAssistantRepository) {
    val entities by ha.entities.collectAsState()
    val connected by ha.connected.collectAsState()
    val lastError by ha.lastError.collectAsState()
    val rooms = DefaultRooms.climateRooms + DefaultRooms.basicRooms
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFF8F8FA), Color(0xFFEFF3F8)))).padding(16.dp)) {
        Text("Xin chào", style = MaterialTheme.typography.headlineMedium)
        Text(
            if (connected) "Realtime: đã kết nối" else "Realtime: chưa kết nối (đang dùng REST)",
            color = if (connected) HumeColors.Green else HumeColors.Orange,
        )
        Spacer(Modifier.height(16.dp))
        ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tổng quan", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { ha.refresh() }) { Text("Làm mới") }
                }
                Text("${entities.size} entities đã tải")
                Text("Alarm: ${entities["alarm_control_panel.alarm_security"]?.state ?: "unknown"}")
                lastError?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, color = HumeColors.Orange, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(2), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
            items(rooms) { RoomCard(it, ha) }
        }
    }
}

@Composable
private fun RoomCard(room: RoomConfig, ha: HomeAssistantRepository) {
    val entities by ha.entities.collectAsState()
    val light = entities[room.lightEntity]
    val temp = entities[room.tempEntity]?.state ?: "--"
    val hum = entities[room.humidityEntity]?.state ?: "--"
    ElevatedCard(shape = RoundedCornerShape(22.dp), onClick = {
        val domain = room.lightEntity.substringBefore('.')
        val service = if (light?.isOn == true) "turn_off" else "turn_on"
        ha.callService(domain, service, "{\"entity_id\":\"${room.lightEntity}\"}", room.lightEntity)
    }) {
        Column(Modifier.padding(16.dp)) {
            Text(room.name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Đèn: ${light?.state ?: "unknown"}",
                color = if (light?.isOn == true) HumeColors.Green else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("$temp°C · $hum%")
        }
    }
}
