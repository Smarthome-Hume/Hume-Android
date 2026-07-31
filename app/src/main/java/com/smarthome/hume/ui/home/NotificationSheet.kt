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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.ui.theme.HumeColors

internal data class HomeAlert(
    val entityId: String,
    val title: String,
    val detail: String,
    val icon: ImageVector,
    val tint: Color,
)

/**
 * Alerts shown by NotificationBottomSheet: open doors/windows, motion, leaks,
 * smoke and low batteries. Derived from device_class so no ID list is needed.
 */
internal fun homeAlerts(entities: Map<String, HomeEntity>): List<HomeAlert> {
    val alerts = mutableListOf<HomeAlert>()
    entities.values.forEach { entity ->
        if (entity.id.startsWith("binary_sensor.") && entity.state == "on") {
            when (entity.deviceClass()) {
                "door", "window", "garage_door", "opening" -> alerts += HomeAlert(
                    entity.id, entity.friendly(), "Đang mở", Icons.Rounded.MeetingRoom, HumeColors.Amber,
                )
                "motion", "occupancy", "presence" -> alerts += HomeAlert(
                    entity.id, entity.friendly(), "Có chuyển động", Icons.Rounded.DirectionsRun, HumeColors.Blue,
                )
                "moisture" -> alerts += HomeAlert(
                    entity.id, entity.friendly(), "Phát hiện rò nước", Icons.Rounded.WaterDrop, HumeColors.Red,
                )
                "smoke", "gas", "carbon_monoxide" -> alerts += HomeAlert(
                    entity.id, entity.friendly(), "Cảnh báo khói / gas", Icons.Rounded.LocalFireDepartment, HumeColors.Red,
                )
            }
        }
        val battery = entity.numericState
        if (entity.deviceClass() == "battery" && battery != null && battery <= 20.0 &&
            entity.id.startsWith("sensor.")
        ) {
            alerts += HomeAlert(
                entity.id, entity.friendly(), "Pin yếu " + entity.formatted(), Icons.Rounded.BatteryAlert, HumeColors.Red,
            )
        }
    }
    return alerts.sortedBy { it.title.lowercase() }.take(40)
}

@Composable
fun NotificationBottomSheet(entities: Map<String, HomeEntity>, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val alerts = homeAlerts(entities)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 32.dp)) {
            Text("Thông báo", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(
                if (alerts.isEmpty()) "Mọi thứ đều ổn" else alerts.size.toString() + " mục cần chú ý",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            if (alerts.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = HumeColors.Green)
                    Spacer(Modifier.width(10.dp))
                    Text("Không có cửa mở, rò nước hay pin yếu.", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                alerts.forEachIndexed { index, alert ->
                    if (index > 0) HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(alert.icon, contentDescription = null, tint = alert.tint, modifier = Modifier.size(22.dp))
                        Column(Modifier.weight(1f)) {
                            Text(alert.title, style = MaterialTheme.typography.bodyLarge, maxLines = 2)
                            Text(
                                alert.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
