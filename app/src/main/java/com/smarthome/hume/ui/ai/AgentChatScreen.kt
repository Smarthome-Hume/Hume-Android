package com.smarthome.hume.ui.ai

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AgentChatScreen() {
    var text by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("AI Butler", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        ElevatedCard(Modifier.weight(1f).fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("AI provider, function calling và voice tiếng Việt sẽ được port từ SwiftUI module AI ở phase sau.") } }
        Spacer(Modifier.height(12.dp))
        Row { OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.weight(1f), placeholder = { Text("Nhắn với Hume...") }); Spacer(Modifier.width(8.dp)); Button(onClick = { text = "" }) { Text("Gửi") } }
    }
}
