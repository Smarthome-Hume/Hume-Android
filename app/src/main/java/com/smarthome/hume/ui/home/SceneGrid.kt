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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeIcons

/** "K\u1ecbch b\u1ea3n" grid: two columns of descriptive scene cards. */
@Composable
fun SceneGridSection(scenes: List<SceneItem>, onRun: (SceneItem) -> Unit) {
    if (scenes.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            "K\u1ecbch b\u1ea3n",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = HumeColors.TextPrimary,
        )
        scenes.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { scene ->
                    Box(Modifier.weight(1f)) { SceneCard(scene, onRun) }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SceneCard(scene: SceneItem, onRun: (SceneItem) -> Unit) {
    val tinted = sceneIsTinted(scene.label)
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(if (tinted) HumeColors.OrangeSofter else Color.White)
            .clickable { onRun(scene) }
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            HumeIcons.scene(scene.label),
            contentDescription = null,
            tint = if (tinted) HumeColors.OrangeDeep else HumeColors.TextPrimary,
            modifier = Modifier.size(26.dp),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            scene.label,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = HumeColors.TextPrimary,
        )
        Text(
            sceneDescription(scene.label),
            fontSize = 13.sp,
            color = HumeColors.TextSecondary,
            lineHeight = 18.sp,
        )
    }
}

private fun sceneIsTinted(label: String): Boolean {
    val text = label.lowercase()
    return text.contains("ra kh\u1ecfi") || text.contains("away") || text.contains("leave")
}

internal fun sceneDescription(label: String): String {
    val text = label.lowercase()
    return when {
        text.contains("s\u00e1ng") || text.contains("morning") || text.contains("wake") ->
            "\u0110i\u1ec1u khi\u1ec3n thi\u1ebft b\u1ecb khi b\u1ea1n th\u1ee9c d\u1eady."
        text.contains("ng\u1ee7") || text.contains("night") || text.contains("sleep") ->
            "\u0110i\u1ec1u khi\u1ec3n thi\u1ebft b\u1ecb khi b\u1ea1n \u0111i ng\u1ee7."
        text.contains("ra kh\u1ecfi") || text.contains("away") || text.contains("leave") ->
            "\u0110i\u1ec1u khi\u1ec3n thi\u1ebft b\u1ecb khi b\u1ea1n r\u1eddi \u0111i."
        text.contains("v\u1ec1 nh\u00e0") || text.contains("home") || text.contains("arrive") ->
            "\u0110i\u1ec1u khi\u1ec3n thi\u1ebft b\u1ecb khi b\u1ea1n v\u1ec1 \u0111\u1ebfn nh\u00e0."
        else -> "Ch\u1ea1y k\u1ecbch b\u1ea3n n\u00e0y."
    }
}
