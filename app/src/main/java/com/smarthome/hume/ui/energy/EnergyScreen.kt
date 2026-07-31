package com.smarthome.hume.ui.energy

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
fun EnergyScreen(ha: HomeAssistantRepository) {
    val entities by ha.entities.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Năng lượng", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("Điện mặt trời", style = MaterialTheme.typography.titleLarge); Text("PV hôm nay: ${entities["sensor.solis_s6_eh1p_pv_today_energy_generation_2"]?.state ?: "--"} kWh"); Text("Công suất nhà: ${entities["sensor.cong_suat_nha"]?.state ?: "--"} W"); Text("Pin: ${entities["sensor.solis_s6_eh1p_battery_soc_2"]?.state ?: "--"}%") } }
        Spacer(Modifier.height(12.dp))
        Text("Charts, flow card và phân tích tuần sẽ port ở phase Energy.")
    }
}
