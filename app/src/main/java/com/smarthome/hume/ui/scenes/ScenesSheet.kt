package com.smarthome.hume.ui.scenes

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.core.scene.LocalScene
import com.smarthome.hume.core.scene.LocalSceneStore
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeShapes
import com.smarthome.hume.ui.theme.glassSurface
import kotlinx.serialization.json.JsonPrimitive

// Port of Views/Home/4_Scenes/ScenesView.swift.
// Two sections: local scenes (LocalSceneStore, exactly one active) and Home
// Assistant automations. Long press opens the same menu the context menu has
// on iOS: run, pin, hide, edit, delete. The plus on the automation header
// opens CreateAutomationView.

private const val PREFS = "hume_scenes"
private const val KEY_HIDDEN_AUTO = "scenes_hidden_auto"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenesSheet(ha: HomeAssistantRepository, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val store = remember { LocalSceneStore.get(context) }
    val scenes by store.scenes.collectAsStateWithLifecycle()
    val entities by ha.entities.collectAsStateWithLifecycle()

    var showHidden by remember { mutableStateOf(false) }
    var hiddenAutos by remember { mutableStateOf(loadHiddenAutos(context)) }
    var editing by remember { mutableStateOf<LocalScene?>(null) }
    var creating by remember { mutableStateOf(false) }
    var creatingAutomation by remember { mutableStateOf(false) }

    val visibleScenes = (if (showHidden) scenes else scenes.filterNot { it.isHidden })
        .sortedBy { it.sortIndex }
    val automations = entities.values
        .filter { it.id.startsWith("automation.") }
        .filter { showHidden || it.id !in hiddenAutos }
        .sortedBy { friendly(it).lowercase() }

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
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "K\u1ecbch b\u1ea3n",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = HumeColors.TextPrimary,
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { creating = true }) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "T\u1ea1o k\u1ecbch b\u1ea3n",
                        tint = HumeColors.Orange,
                        modifier = Modifier.size(22.dp),
                    )
                }
                IconButton(onClick = { showHidden = !showHidden }) {
                    Icon(
                        if (showHidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = null,
                        tint = HumeColors.TextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = null,
                        tint = HumeColors.TextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            SectionHeader("C\u1ea3nh", scenes.size, Icons.Rounded.AutoAwesome)
            if (visibleScenes.isEmpty()) {
                EmptyHint("Ch\u01b0a c\u00f3 c\u1ea3nh n\u00e0o")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    visibleScenes.forEach { scene ->
                        SceneRow(
                            scene = scene,
                            store = store,
                            ha = ha,
                            onEdit = { editing = scene },
                        )
                    }
                }
            }

            SectionHeader(
                title = "T\u1ef1 \u0111\u1ed9ng ho\u00e1",
                count = automations.size,
                icon = Icons.Rounded.Bolt,
                onAdd = { creatingAutomation = true },
            )
            if (automations.isEmpty()) {
                EmptyHint("Ch\u01b0a c\u00f3 t\u1ef1 \u0111\u1ed9ng ho\u00e1")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    automations.forEach { entity ->
                        AutomationRow(
                            entity = entity,
                            hidden = entity.id in hiddenAutos,
                            ha = ha,
                            onToggleHidden = {
                                val next = hiddenAutos.toMutableSet()
                                if (!next.remove(entity.id)) next.add(entity.id)
                                hiddenAutos = next
                                saveHiddenAutos(context, next)
                            },
                        )
                    }
                }
            }

            Text(
                "Gi\u1eef l\u00e2u m\u1ed9t th\u1ebb \u0111\u1ec3 m\u1edf menu.",
                fontSize = 11.sp,
                color = HumeColors.TextSecondary,
            )
        }
    }

    if (creating) {
        SceneEditorSheet(scene = null, ha = ha, onDismiss = { creating = false })
    }
    editing?.let { scene ->
        SceneEditorSheet(scene = scene, ha = ha, onDismiss = { editing = null })
    }
    if (creatingAutomation) {
        CreateAutomationSheet(ha = ha, onDismiss = { creatingAutomation = false })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SceneRow(
    scene: LocalScene,
    store: LocalSceneStore,
    ha: HomeAssistantRepository,
    onEdit: () -> Unit,
) {
    val accent = sceneColor(scene.colorHex)
    val active = scene.isActive
    var menu by remember { mutableStateOf(false) }

    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .alpha(if (scene.isHidden) 0.5f else 1f)
                .glassSurface(radius = HumeShapes.Element)
                .border(
                    1.5.dp,
                    if (active) accent.copy(alpha = 0.35f) else Color.Transparent,
                    RoundedCornerShape(HumeShapes.Element),
                )
                .combinedClickable(
                    onClick = { store.activate(scene.id, ha) },
                    onLongClick = { menu = true },
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    sceneIcon(scene.icon),
                    contentDescription = null,
                    tint = if (active) accent else HumeColors.TextSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        scene.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (scene.isHidden) HumeColors.TextSecondary else HumeColors.TextPrimary,
                        maxLines = 1,
                    )
                    if (!scene.isPinned) {
                        Spacer(Modifier.width(5.dp))
                        Icon(
                            Icons.Rounded.PushPin,
                            contentDescription = null,
                            tint = HumeColors.TextSecondary,
                            modifier = Modifier.size(10.dp),
                        )
                    }
                }
                Text(
                    scene.actions.size.toString() + " h\u00e0nh \u0111\u1ed9ng \u00b7 " +
                        (if (active) "\u0110ang b\u1eadt" else "\u0110ang t\u1eaft"),
                    fontSize = 12.sp,
                    color = if (active) accent else HumeColors.TextSecondary,
                )
            }
            if (active) {
                Box(Modifier.size(10.dp).background(accent, CircleShape))
            }
        }

        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text(if (active) "T\u1eaft" else "B\u1eadt") },
                onClick = { menu = false; store.activate(scene.id, ha) },
            )
            DropdownMenuItem(
                text = { Text("S\u1eeda k\u1ecbch b\u1ea3n") },
                onClick = { menu = false; onEdit() },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        if (scene.isPinned) "B\u1ecf ghim kh\u1ecfi Home"
                        else "Ghim l\u00ean Home",
                    )
                },
                onClick = { menu = false; store.setPinned(scene.id, !scene.isPinned) },
            )
            DropdownMenuItem(
                text = { Text(if (scene.isHidden) "Hi\u1ec7n" else "\u1ea8n") },
                onClick = { menu = false; store.setHidden(scene.id, !scene.isHidden) },
            )
            DropdownMenuItem(
                text = { Text("Xo\u00e1", color = HumeColors.Red) },
                onClick = { menu = false; store.delete(scene.id) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AutomationRow(
    entity: HomeEntity,
    hidden: Boolean,
    ha: HomeAssistantRepository,
    onToggleHidden: () -> Unit,
) {
    val on = entity.state == "on"
    var menu by remember { mutableStateOf(false) }

    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .alpha(if (hidden) 0.5f else 1f)
                .glassSurface(radius = HumeShapes.Element)
                .combinedClickable(onClick = { menu = true }, onLongClick = { menu = true })
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(
                        if (on) HumeColors.Green.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = if (on) HumeColors.Green else HumeColors.TextSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    friendly(entity),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HumeColors.TextPrimary,
                    maxLines = 1,
                )
                Text(
                    if (on) "\u0110ang b\u1eadt" else "\u0110ang t\u1eaft",
                    fontSize = 12.sp,
                    color = if (on) HumeColors.Green else HumeColors.TextSecondary,
                )
            }
            Text(
                "Ch\u1ea1y",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = HumeColors.Orange,
                modifier = Modifier
                    .background(HumeColors.Orange.copy(alpha = 0.12f), RoundedCornerShape(50))
                    .combinedClickable {
                        ha.callService(
                            "automation",
                            "trigger",
                            "{\"entity_id\":\"" + entity.id + "\"}",
                            entity.id,
                        )
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
            Switch(
                checked = on,
                onCheckedChange = {
                    ha.callService(
                        "automation",
                        "toggle",
                        "{\"entity_id\":\"" + entity.id + "\"}",
                        entity.id,
                    )
                },
                colors = SwitchDefaults.colors(checkedTrackColor = HumeColors.Orange),
            )
        }

        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = {
                    Text(if (on) "V\u00f4 hi\u1ec7u ho\u00e1" else "K\u00edch ho\u1ea1t")
                },
                onClick = {
                    menu = false
                    ha.callService(
                        "automation",
                        if (on) "turn_off" else "turn_on",
                        "{\"entity_id\":\"" + entity.id + "\"}",
                        entity.id,
                    )
                },
            )
            DropdownMenuItem(
                text = { Text(if (hidden) "Hi\u1ec7n" else "\u1ea8n") },
                onClick = { menu = false; onToggleHidden() },
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    icon: ImageVector,
    onAdd: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = HumeColors.Orange, modifier = Modifier.size(15.dp))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = HumeColors.TextPrimary)
        Text("(" + count + ")", fontSize = 14.sp, color = HumeColors.TextSecondary)
        if (onAdd != null) {
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onAdd) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = "T\u1ea1o t\u1ef1 \u0111\u1ed9ng ho\u00e1",
                    tint = HumeColors.Orange,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text,
        fontSize = 13.sp,
        color = HumeColors.TextSecondary,
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
    )
}

private fun friendly(entity: HomeEntity): String {
    val raw = entity.attributes["friendly_name"] as? JsonPrimitive
    return raw?.content ?: entity.id.substringAfter('.').replace('_', ' ')
}

private fun loadHiddenAutos(context: Context): Set<String> =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getStringSet(KEY_HIDDEN_AUTO, emptySet())
        ?.toSet()
        ?: emptySet()

private fun saveHiddenAutos(context: Context, value: Set<String>) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit()
        .putStringSet(KEY_HIDDEN_AUTO, value)
        .apply()
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
