package com.smarthome.hume.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.HomeEntity

internal data class SceneItem(val entityId: String, val label: String, val isScript: Boolean)

/** Scenes and scripts pulled straight from the entity map, no hardcoded IDs. */
internal fun sceneItems(entities: Map<String, HomeEntity>): List<SceneItem> {
    val scenes = entities.values
        .filter { it.id.startsWith("scene.") }
        .map { SceneItem(it.id, it.friendly(), isScript = false) }
    val scripts = entities.values
        .filter { it.id.startsWith("script.") }
        .map { SceneItem(it.id, it.friendly(), isScript = true) }
    return (scenes + scripts).sortedBy { it.label.lowercase() }.take(16)
}

/** SceneSection from HomeView.swift: a horizontal strip of one-tap scenes. */
@Composable
fun SceneSection(entities: Map<String, HomeEntity>, ha: HomeAssistantRepository) {
    val items = remember(entities.size) { sceneItems(entities) }
    if (items.isEmpty()) return
    Column(Modifier.fillMaxWidth()) {
        Text(
            "Ngị cảnh",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 4.dp),
        )
        Spacer(Modifier.height(8.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp),
        ) {
            items(items, key = { it.entityId }) { item ->
                AssistChip(
                    onClick = { runScene(ha, item) },
                    label = { Text(item.label, maxLines = 1) },
                    leadingIcon = {
                        Icon(
                            if (item.isScript) Icons.Rounded.PlayArrow else Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    shape = MaterialTheme.shapes.large,
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                )
            }
        }
    }
}

internal fun runScene(ha: HomeAssistantRepository, item: SceneItem) {
    val domain = if (item.isScript) "script" else "scene"
    ha.callService(domain, "turn_on", """{"entity_id":"${item.entityId}"}""")
}
