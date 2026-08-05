@file:OptIn(ExperimentalMaterial3Api::class)

package com.smarthome.hume.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MeetingRoom
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.core.scene.ManagedListsStore
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeIcons

internal data class HomeAlert(
    val entityId: String,
    val title: String,
    val detail: String,
    val icon: ImageVector,
    val tint: Color,
)

/**
 * Device-class derived alerts. Kept for the security screen; the home bell no
 * longer uses it, because NotifPopupView.swift only shows the sensors the user
 * added to the managed notification list.
 */
internal fun homeAlerts(entities: Map<String, HomeEntity>): List<HomeAlert> {
    val alerts = mutableListOf<HomeAlert>()
    entities.values.forEach { entity ->
        if (entity.id.startsWith("binary_sensor.") && entity.state == "on") {
            when (entity.deviceClass()) {
                "door", "window", "garage_door", "opening" -> alerts += HomeAlert(
                    entity.id, entity.friendly(), "\u0110ang m\u1edf", Icons.Rounded.MeetingRoom, HumeColors.Amber,
                )
                "motion", "occupancy", "presence" -> alerts += HomeAlert(
                    entity.id, entity.friendly(), "C\u00f3 chuy\u1ec3n \u0111\u1ed9ng", Icons.Rounded.DirectionsRun, HumeColors.Blue,
                )
                "moisture" -> alerts += HomeAlert(
                    entity.id, entity.friendly(), "Ph\u00e1t hi\u1ec7n r\u00f2 n\u01b0\u1edbc", Icons.Rounded.WaterDrop, HumeColors.Red,
                )
                "smoke", "gas", "carbon_monoxide" -> alerts += HomeAlert(
                    entity.id, entity.friendly(), "C\u1ea3nh b\u00e1o kh\u00f3i / gas", Icons.Rounded.LocalFireDepartment, HumeColors.Red,
                )
            }
        }
        val battery = entity.numericState
        if (entity.deviceClass() == "battery" && battery != null && battery <= 20.0 &&
            entity.id.startsWith("sensor.")
        ) {
            alerts += HomeAlert(
                entity.id, entity.friendly(), "Pin y\u1ebfu " + entity.formatted(), Icons.Rounded.BatteryAlert, HumeColors.Red,
            )
        }
    }
    return alerts.sortedBy { it.title.lowercase() }.take(40)
}

/**
 * NotifPopupView.swift: the managed notification entities that are currently on,
 * with the custom name and icon the user picked, plus how long ago they changed.
 *
 * The list is empty on a fresh install exactly like iOS, so the sheet always
 * shows a visible "Ch\u1ecdn c\u1ea3m bi\u1ebfn" action that opens ManageListSheet
 * (the entity picker). Long pressing the bell still opens the same screen.
 */
@Composable
fun NotificationBottomSheet(
    entities: Map<String, HomeEntity>,
    onManage: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val store = remember { ManagedListsStore.get(context) }
    val notif by store.notif.collectAsStateWithLifecycle()

    val active = notif.filter { !it.hidden && entities[it.id]?.isOn == true }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Th\u00f4ng b\u00e1o",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HumeColors.TextPrimary,
                )
                if (active.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .background(HumeColors.Orange, RoundedCornerShape(50))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            active.size.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier
                        .background(HumeColors.Orange.copy(alpha = 0.12f), RoundedCornerShape(50))
                        .clickable(onClick = onManage)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = HumeColors.Orange,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Ch\u1ecdn c\u1ea3m bi\u1ebfn",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = HumeColors.Orange,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            if (active.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = HumeColors.Green)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (notif.isEmpty())
                            "Ch\u01b0a ch\u1ecdn c\u1ea3m bi\u1ebfn n\u00e0o \u2014 nh\u1ea5n \"Ch\u1ecdn c\u1ea3m bi\u1ebfn\" \u0111\u1ec3 th\u00eam."
                        else
                            "Kh\u00f4ng c\u00f3 thi\u1ebft b\u1ecb n\u00e0o \u0111ang ho\u1ea1t \u0111\u1ed9ng",
                        fontSize = 14.sp,
                        color = HumeColors.TextSecondary,
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    active.forEach { item ->
                        val entity = entities[item.id]
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    Color.White.copy(alpha = 0.08f),
                                    RoundedCornerShape(20.dp),
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                Modifier
                                    .size(32.dp)
                                    .background(Color.White.copy(alpha = 0.08f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    HumeIcons.sensor(item.icon),
                                    contentDescription = null,
                                    tint = HumeColors.TextPrimary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.name.ifEmpty { entity?.friendly() ?: item.id },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = HumeColors.TextPrimary,
                                    maxLines = 2,
                                )
                                val since = sinceText(entity?.lastChanged)
                                if (since.isNotEmpty()) {
                                    Text(since, fontSize = 10.sp, color = HumeColors.TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** NotifPopupView.fmtLastChange */
private fun sinceText(lastChanged: String?): String {
    val millis = parseTimestamp(lastChanged) ?: return ""
    val seconds = (System.currentTimeMillis() - millis) / 1000L
    return when {
        seconds < 60 -> "V\u1eeba xong"
        seconds < 3600 -> (seconds / 60).toString() + " ph\u00fat tr\u01b0\u1edbc"
        seconds < 86400 -> (seconds / 3600).toString() + " gi\u1edd tr\u01b0\u1edbc"
        else -> (seconds / 86400).toString() + " ng\u00e0y tr\u01b0\u1edbc"
    }
}
