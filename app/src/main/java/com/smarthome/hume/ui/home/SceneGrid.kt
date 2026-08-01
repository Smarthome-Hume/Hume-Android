package com.smarthome.hume.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeIcons

/** "K\u1ecbch b\u1ea3n" grid. The scene matching the current alarm mode is highlighted green. */
@Composable
fun SceneGridSection(
    scenes: List<SceneItem>,
    alarmState: String?,
    onRun: (SceneItem) -> Unit,
) {
    if (scenes.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("K\u1ecbch b\u1ea3n", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = HumeColors.TextPrimary)
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = HumeColors.TextSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
        scenes.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { scene ->
                    Box(Modifier.weight(1f)) {
                        SceneCard(scene, active = sceneAlarmMode(scene.label) == alarmState, onRun = onRun)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SceneCard(scene: SceneItem, active: Boolean, onRun: (SceneItem) -> Unit) {
    val accent = if (active) HumeColors.SceneGreen else HumeColors.TextPrimary
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (active) HumeColors.SceneGreenBg else Color.White)
            .clickable { onRun(scene) }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            HumeIcons.scene(scene.label),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(20.dp),
        )
        Text(scene.label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = accent, maxLines = 1)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(5.dp).clip(CircleShape)
                    .background(if (active) HumeColors.SceneGreen else HumeColors.TextSecondary)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                sceneDescription(scene.label),
                fontSize = 10.sp,
                color = if (active) HumeColors.SceneGreen else HumeColors.TextSecondary,
                maxLines = 1,
            )
        }
    }
}

/** Alarm mode a scene is expected to leave the house in. */
internal fun sceneAlarmMode(label: String): String? {
    val text = label.lowercase()
    return when {
        text.contains("ng\u1ee7") || text.contains("night") || text.contains("sleep") -> "armed_night"
        text.contains("ra kh\u1ecfi") || text.contains("away") || text.contains("leave") -> "armed_away"
        text.contains("v\u1ec1 nh\u00e0") || text.contains("arrive") -> "armed_home"
        else -> null
    }
}

internal fun sceneDescription(label: String): String = when (sceneAlarmMode(label)) {
    "armed_night" -> "B\u00e1o \u0111\u1ed9ng: Ban \u0111\u00eam"
    "armed_away" -> "B\u00e1o \u0111\u1ed9ng: Ra ngo\u00e0i"
    "armed_home" -> "B\u00e1o \u0111\u1ed9ng: \u1ede nh\u00e0"
    else -> "B\u00e1o \u0111\u1ed9ng: T\u00f9y ch\u1ec9nh"
}
