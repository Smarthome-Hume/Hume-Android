package com.smarthome.hume.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.storage.HumeSettings
import com.smarthome.hume.core.storage.SettingsStore
import com.smarthome.hume.ui.theme.GlassCard
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeShapes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Profile tab, modelled on ProfileView.swift + DeviceManagerView.swift:
 * connection state, entity statistics per domain, and the logout action.
 */
@Composable
fun ProfileScreen(settingsStore: SettingsStore, settings: HumeSettings, ha: HomeAssistantRepository) {
    val entities by ha.entities.collectAsState()
    val connected by ha.connected.collectAsState()
    val error by ha.lastError.collectAsState()
    val registry by ha.registry.collectAsState()
    val areas by ha.areas.collectAsState()

    val byDomain = entities.keys
        .groupingBy { it.substringBefore('.') }
        .eachCount()
        .entries
        .sortedByDescending { it.value }

    Column(
        Modifier
            .fillMaxSize()
            .background(HumeColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text("H\u1ed3 s\u01a1", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = HumeColors.TextPrimary)
        Spacer(Modifier.height(14.dp))

        Panel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (connected) HumeColors.Green else HumeColors.Red),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    if (connected) "\u0110\u00e3 k\u1ebft n\u1ed1i realtime" else "M\u1ea5t k\u1ebft n\u1ed1i WebSocket",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HumeColors.TextPrimary,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(settings.haUrl, fontSize = 13.sp, color = HumeColors.TextSecondary)
            error?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, fontSize = 12.sp, color = HumeColors.Red)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { ha.refresh() }) { Text("L\u00e0m m\u1edbi") }
                OutlinedButton(onClick = { ha.connect() }) { Text("K\u1ebft n\u1ed1i l\u1ea1i") }
            }
        }

        Spacer(Modifier.height(12.dp))
        Panel {
            Text("C\u1ea3m bi\u1ebfn", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = HumeColors.TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(
                entities.size.toString() + " th\u1ef1c th\u1ec3 \u00b7 " + byDomain.size + " ph\u00e2n lo\u1ea1i \u00b7 " +
                    areas.size + " khu v\u1ef1c \u00b7 " + registry.size + " b\u1ea3n ghi registry",
                fontSize = 12.sp,
                color = HumeColors.TextSecondary,
            )
            Spacer(Modifier.height(10.dp))
            byDomain.take(14).forEach { (domain, count) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(domain, fontSize = 13.sp, color = HumeColors.TextPrimary, modifier = Modifier.weight(1f))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(HumeColors.Orange.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                    ) {
                        Text(count.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HumeColors.Orange)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Panel {
            Text("T\u00e0i kho\u1ea3n", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = HumeColors.TextPrimary)
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = {
                ha.disconnect()
                CoroutineScope(Dispatchers.IO).launch { settingsStore.logout() }
            }) { Text("\u0110\u0103ng xu\u1ea5t") }
        }
        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        radius = HumeShapes.Panel,
        padding = PaddingValues(16.dp),
        content = content,
    )
}
