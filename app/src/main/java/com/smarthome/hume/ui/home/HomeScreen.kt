package com.smarthome.hume.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
    val status by ha.status.collectAsState()
    val rooms = DefaultRooms.climateRooms + DefaultRooms.basicRooms
    val hasEntities = entities.isNotEmpty()

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFF8F8FA), Color(0xFFEFF3F8))))
            .padding(16.dp)
    ) {
        Text("Xin chào", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = when {
                hasEntities && connected -> "Home Assistant đã kết nối realtime"
                hasEntities -> "Home Assistant đã tải entities"
                else -> status
            },
            color = when {
                hasEntities -> HumeColors.Green
                status.contains("lỗi", ignoreCase = true) || status.contains("HTTP", ignoreCase = true) || status.contains("Token", ignoreCase = true) -> HumeColors.Orange
                else -> HumeColors.Orange
            }
        )
        Spacer(Modifier.height(16.dp))
        ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("Tổng quan", style = MaterialTheme.typography.titleLarge)
                Text("${entities.size} entities đã tải")
                Text("Realtime: ${if (connected) "đã kết nối" else "chưa kết nối"}")
                Text("Trạng thái: $status")
                Text("Alarm: ${entities["alarm_control_panel.alarm_security"]?.state ?: "unknown"}")
            }
        }
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(rooms) { RoomCard(it, ha) }
        }
    }
}

@Composable
private fun RoomCard(room: RoomConfig, ha: HomeAssistantRepository) {
    val entities by ha.entities.collectAsState()
    val light = entities[room.lightEntity]
    ElevatedCard(
        shape = RoundedCornerShape(22.dp),
        onClick = {
            ha.callService(
                room.lightEntity.substringBefore('.'),
                if (light?.isOn == true) "turn_off" else "turn_on",
                "{\"entity_id\":\"${room.lightEntity}\"}"
            )
        }
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(room.name, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("Đèn: ${light?.state ?: "unknown"}")
            Text("${entities[room.tempEntity]?.state ?: "--"}°C · ${entities[room.humidityEntity]?.state ?: "--"}%")
        }
    }
}
