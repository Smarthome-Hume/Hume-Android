package com.smarthome.hume.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.scene.LocalScene
import com.smarthome.hume.core.scene.LocalSceneStore
import com.smarthome.hume.ui.scenes.ScenesSheet
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.glassSurface

// Port of the scene cluster in HomeView.swift + Views/Home/4_Scenes/SceneCardView.swift.
// Scenes come from LocalSceneStore, never from scene.* entities: exactly one can be
// active, tapping an active card turns it back off, and the alarm state is mirrored
// in both directions. Tapping the header opens ScenesView.

private val SceneRadius = 25.dp

@Composable
fun SceneGridSection(
    ha: HomeAssistantRepository,
    alarmState: String?,
    onOpenScenes: () -> Unit = {},
) {
    val context = LocalContext.current
    val store = remember { LocalSceneStore.get(context) }
    val scenes by store.scenes.collectAsStateWithLifecycle()
    var showScenes by remember { mutableStateOf(false) }

    // Alarm changed outside the app -> reflect it locally, but skip the echo of
    // a scene we just activated ourselves.
    LaunchedEffect(alarmState) {
        val state = alarmState
        if (!state.isNullOrEmpty() && !store.recentlyActivatedLocally) {
            store.syncFromAlarmState(state, ha)
        }
    }

    if (showScenes) {
        ScenesSheet(ha = ha, onDismiss = { showScenes = false })
    }

    val visible = store.pinnedVisible(scenes).take(4)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable {
                showScenes = true
                onOpenScenes()
            },
        ) {
            Text(
                "K\u1ecbch b\u1ea3n",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = HumeColors.TextPrimary,
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = HumeColors.TextSecondary,
                modifier = Modifier.size(15.dp),
            )
        }
        visible.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { scene ->
                    Box(Modifier.weight(1f)) {
                        SceneCard(scene) { store.activate(scene.id, ha) }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SceneCard(scene: LocalScene, onTap: () -> Unit) {
    val accent = sceneColor(scene.colorHex)
    val isOn = scene.isActive
    Column(
        Modifier
            .fillMaxWidth()
            .glassSurface(radius = SceneRadius, elevation = if (isOn) 10.dp else 4.dp)
            .background(
                if (isOn) accent.copy(alpha = 0.10f) else Color.Transparent,
                RoundedCornerShape(SceneRadius),
            )
            .border(
                1.dp,
                if (isOn) accent.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(SceneRadius),
            )
            .clickable { onTap() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(
                    if (isOn) accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f),
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                sceneIcon(scene.icon),
                contentDescription = null,
                tint = if (isOn) accent else HumeColors.TextPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            scene.name,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = HumeColors.TextPrimary,
            maxLines = 2,
        )
        val alarmo = scene.alarmoState
        if (alarmo != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = if (isOn) accent else HumeColors.TextSecondary,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    alarmoLabel(alarmo),
                    fontSize = 11.sp,
                    color = if (isOn) accent else HumeColors.TextSecondary,
                    maxLines = 1,
                )
            }
        } else if (scene.actions.isNotEmpty()) {
            Text(
                scene.actions.size.toString() + " h\u00e0nh \u0111\u1ed9ng",
                fontSize = 12.sp,
                color = HumeColors.TextSecondary,
                maxLines = 1,
            )
        }
    }
}

internal fun alarmoLabel(state: String): String = when (state) {
    "armed_away" -> "B\u00e1o \u0111\u1ed9ng: Ra ngo\u00e0i"
    "armed_home" -> "B\u00e1o \u0111\u1ed9ng: \u1ede nh\u00e0"
    "armed_night" -> "B\u00e1o \u0111\u1ed9ng: Ban \u0111\u00eam"
    "armed_custom_bypass" -> "B\u00e1o \u0111\u1ed9ng: T\u00f9y ch\u1ec9nh"
    else -> "B\u00e1o \u0111\u1ed9ng"
}

private fun sceneIcon(key: String): ImageVector = when (key) {
    "sunrise" -> Icons.Rounded.WbTwilight
    "moon" -> Icons.Rounded.Bedtime
    "walk" -> Icons.Rounded.DirectionsWalk
    "house" -> Icons.Rounded.Home
    else -> Icons.Rounded.PlayArrow
}

private fun sceneColor(hex: String): Color {
    val cleaned = hex.removePrefix("#")
    val value = cleaned.toLongOrNull(16) ?: return HumeColors.Orange
    return if (cleaned.length <= 6) Color(value or 0xFF000000L) else Color(value)
}
