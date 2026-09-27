package com.smarthome.hume.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.storage.HumeSettings
import com.smarthome.hume.core.storage.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(settingsStore: SettingsStore, settings: HumeSettings, ha: HomeAssistantRepository) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Hồ sơ", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        ElevatedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("Home Assistant", style = MaterialTheme.typography.titleLarge); Text(settings.haUrl); Spacer(Modifier.height(8.dp)); OutlinedButton(onClick = { ha.disconnect(); CoroutineScope(Dispatchers.IO).launch { settingsStore.logout() } }) { Text("Đăng xuất") } } }
        Spacer(Modifier.height(12.dp))
        Text("Quản lý thiết bị, scene, AI settings sẽ port ở các phase tiếp theo.")
    }
}
