package com.smarthome.hume.ui.profile

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Power
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.scene.ManagedKind
import com.smarthome.hume.core.storage.HumeSettings
import com.smarthome.hume.ui.manage.ManageListSheet
import com.smarthome.hume.ui.sensors.SensorsSheet
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeShapes
import com.smarthome.hume.ui.theme.glassSurface

/**
 * Port of Views/Profile/DeviceManagerView.swift.
 *
 * Kept groups, in the original order: the Home Assistant connection card with
 * the masked token and the live status pill, the sensor card that counts every
 * entity per domain and opens the sensor manager, the managed lists that feed
 * the Home header, and the info card. The AI group is gone because the AI tab
 * was removed, and the wallpaper group is iOS-only (PhotosPicker + AVFoundation).
 *
 * Read-only value boxes, icon wells and domain chips use tertiarySystemFill
 * like the SwiftUI original, so they do not stay white in dark mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceManagerSheet(
    ha: HomeAssistantRepository,
    settings: HumeSettings,
    onDismiss: () -> Unit,
) {
    val entities by ha.entities.collectAsStateWithLifecycle()
    val connected by ha.connected.collectAsStateWithLifecycle()

    var showToken by remember { mutableStateOf(false) }
    var openSensors by remember { mutableStateOf(false) }
    var manage by remember { mutableStateOf<ManagedKind?>(null) }

    // buildDomainCounts(): every entity grouped by domain, biggest first.
    val domainCounts = entities.keys
        .groupingBy { it.substringBefore('.') }
        .eachCount()
        .toList()
        .sortedByDescending { it.second }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = HumeColors.Background,
        dragHandle = null,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Qu\u1ea3n l\u00fd thi\u1ebft b\u1ecb",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = HumeColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = null, tint = HumeColors.TextSecondary)
                }
            }

            // Group 1 — connection
            Box(Modifier.fillMaxWidth().glassSurface(radius = HumeShapes.Panel).padding(10.dp)) {
                Column(
                    Modifier.fillMaxWidth().glassSurface(radius = HumeShapes.Tile).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    CardTitle(Icons.Rounded.Power, "K\u1ebft n\u1ed1i Home Assistant")

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "URL n\u1ed9i b\u1ed9 / Domain",
                            fontSize = 12.sp,
                            color = HumeColors.TextSecondary,
                        )
                        Text(
                            settings.haUrl.ifBlank { "\u2014" },
                            fontSize = 14.sp,
                            color = HumeColors.TextPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(HumeColors.FillTertiary)
                                .padding(12.dp),
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Long-Lived Access Token", fontSize = 12.sp, color = HumeColors.TextSecondary)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(HumeColors.FillTertiary)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                maskToken(settings.haToken, showToken),
                                fontSize = 13.sp,
                                color = HumeColors.TextPrimary,
                                maxLines = 1,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                if (showToken) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = null,
                                tint = HumeColors.TextSecondary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { showToken = !showToken },
                            )
                        }
                    }

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(HumeColors.Orange.copy(alpha = 0.06f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (connected) Color(0xFF22C55E) else Color(0xFFEF4444)),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (connected) "\u0110\u00e3 k\u1ebft n\u1ed1i" else "M\u1ea5t k\u1ebft n\u1ed1i",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (connected) Color(0xFF22C55E) else Color(0xFFEF4444),
                        )
                    }
                }
            }

            // Group 2 — sensors
            Box(Modifier.fillMaxWidth().glassSurface(radius = HumeShapes.Panel).padding(10.dp)) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .glassSurface(radius = HumeShapes.Tile)
                        .clickable { openSensors = true }
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(42.dp).clip(CircleShape).background(HumeColors.FillTertiary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Sensors,
                                contentDescription = null,
                                tint = HumeColors.Orange,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "C\u1ea3m bi\u1ebfn",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = HumeColors.TextPrimary,
                            )
                            Text(
                                "" + entities.size + " th\u1ef1c th\u1ec3 \u00b7 " + domainCounts.size + " ph\u00e2n lo\u1ea1i",
                                fontSize = 12.sp,
                                color = HumeColors.TextSecondary,
                            )
                        }
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = HumeColors.TextSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    // Top 6 domains as capsules, same as the iOS LazyVGrid.
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        domainCounts.take(6).chunked(3).forEach { line ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                line.forEach { pair ->
                                    Row(
                                        Modifier
                                            .clip(RoundedCornerShape(HumeShapes.Pill))
                                            .background(HumeColors.FillTertiary)
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            pair.first,
                                            fontSize = 11.sp,
                                            color = HumeColors.TextPrimary,
                                            maxLines = 1,
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            pair.second.toString(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = HumeColors.Orange,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Group 3 — managed lists that feed the Home header
            Column(
                Modifier.fillMaxWidth().glassSurface(radius = HumeShapes.Panel).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ManagerRow(
                    icon = Icons.Rounded.Lightbulb,
                    title = "Qu\u1ea3n l\u00fd \u0111\u00e8n",
                    subtitle = "Danh s\u00e1ch \u0111\u00e8n \u0111\u1ebfm tr\u00ean m\u00e0n h\u00ecnh Nh\u00e0",
                ) { manage = ManagedKind.LIGHTS }
                ManagerRow(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "Qu\u1ea3n l\u00fd th\u00f4ng b\u00e1o",
                    subtitle = "C\u1ea3m bi\u1ebfn hi\u1ec7n trong chu\u00f4ng th\u00f4ng b\u00e1o",
                ) { manage = ManagedKind.NOTIF }
            }

            // Group 4 — info
            Column(
                Modifier.fillMaxWidth().glassSurface(radius = HumeShapes.Tile).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CardTitle(Icons.Rounded.Power, "Th\u00f4ng tin")
                InfoRow("Phi\u00ean b\u1ea3n", "2.0.0")
                InfoRow("Server", settings.haUrl.ifBlank { "http://192.168.102.22:8123" })
                InfoRow("Th\u1ef1c th\u1ec3", entities.size.toString())
            }
        }
    }

    if (openSensors) {
        SensorsSheet(ha = ha, onDismiss = { openSensors = false })
    }
    manage?.let { kind ->
        ManageListSheet(kind = kind, ha = ha, onDismiss = { manage = null })
    }
}

@Composable
private fun CardTitle(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = HumeColors.TextPrimary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = HumeColors.TextPrimary)
    }
}

@Composable
private fun ManagerRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .glassSurface(radius = HumeShapes.Tile)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(HumeColors.FillTertiary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = HumeColors.Orange, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = HumeColors.TextPrimary)
            Text(subtitle, fontSize = 12.sp, color = HumeColors.TextSecondary, maxLines = 1)
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = HumeColors.TextSecondary,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().height(24.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, color = HumeColors.TextSecondary, modifier = Modifier.weight(1f))
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = HumeColors.TextPrimary,
            maxLines = 1,
        )
    }
}

/** ha.haToken.prefix(12) + "*****" in DeviceManagerView.swift */
private fun maskToken(token: String, reveal: Boolean): String = when {
    token.isEmpty() -> "\u2014"
    reveal -> token
    else -> token.take(12) + "*****"
}
