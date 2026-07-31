package com.smarthome.hume.ui.security

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smarthome.hume.core.ha.HomeAssistantRepository

@Composable
fun SecurityScreen(ha: HomeAssistantRepository) {
    val entities by ha.entities.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("An ninh", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("Alarm", style = MaterialTheme.typography.titleLarge); Text(entities["alarm_control_panel.alarm_security"]?.state ?: "unknown") } }
        Spacer(Modifier.height(12.dp))
        Text("Frigate camera sẽ dùng Coil cho snapshot và Media3/ExoPlayer cho video clip.")
        Text("HomeKit doorbell trên iOS sẽ cần thay bằng Home Assistant/Frigate vì Android không có HomeKit native API.")
    }
}
