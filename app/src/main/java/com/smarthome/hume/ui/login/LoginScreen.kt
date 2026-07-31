package com.smarthome.hume.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.smarthome.hume.core.storage.SettingsStore
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(settingsStore: SettingsStore) {
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("http://192.168.102.22:8123") }
    var token by remember { mutableStateOf("") }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Hume", style = MaterialTheme.typography.displayMedium)
            Text("Kết nối Home Assistant", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(28.dp))
            OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("Home Assistant URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = token, onValueChange = { token = it }, label = { Text("Long-lived token") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(20.dp))
            Button(onClick = { scope.launch { settingsStore.saveHomeAssistant(url, token) } }, modifier = Modifier.fillMaxWidth()) { Text("Đăng nhập") }
            Spacer(Modifier.height(8.dp))
            Text("QR scanner sẽ port ở phase tiếp theo bằng ML Kit.", style = MaterialTheme.typography.bodySmall)
        }
    }
}
