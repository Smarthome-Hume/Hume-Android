package com.smarthome.hume.ui.scenes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Blinds
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Power
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material.icons.rounded.Weekend
import androidx.compose.material.icons.rounded.WindPower
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.core.scene.LocalScene
import com.smarthome.hume.core.scene.LocalSceneAction
import com.smarthome.hume.core.scene.LocalSceneStore
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeShapes
import com.smarthome.hume.ui.theme.glassSurface
import kotlinx.serialization.json.JsonPrimitive

// Port of Views/Home/4_Scenes/SceneEditorView.swift.
// A scene is a name, an icon, a colour, the pin flag and an ordered list of
// service calls. Alarm entities get the Alarmo mode list, everything else gets
// turn_on / turn_off / toggle, exactly like the pickers on iOS.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SceneEditorSheet(
    scene: LocalScene?,
    ha: HomeAssistantRepository,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val store = remember { LocalSceneStore.get(context) }
    val entities by ha.entities.collectAsState()

    var name by remember { mutableStateOf(scene?.name.orEmpty()) }
    var icon by remember { mutableStateOf(scene?.icon ?: "sparkles") }
    var colorHex by remember { mutableStateOf(scene?.colorHex ?: "#f9784c") }
    var pinned by remember { mutableStateOf(scene?.isPinned ?: true) }
    val actions = remember { scene?.actions.orEmpty().toMutableStateList() }
    var picking by remember { mutableStateOf(false) }

    val accent = hexColor(colorHex)

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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (scene == null) "T\u1ea1o k\u1ecbch b\u1ea3n" else "S\u1eeda k\u1ecbch b\u1ea3n",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = HumeColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        val trimmed = name.trim()
                        if (trimmed.isNotEmpty()) {
                            if (scene == null) {
                                store.add(
                                    LocalScene(
                                        name = trimmed,
                                        icon = icon,
                                        colorHex = colorHex,
                                        isPinned = pinned,
                                        actions = actions.toList(),
                                    ),
                                )
                            } else {
                                store.update(
                                    scene.copy(
                                        name = trimmed,
                                        icon = icon,
                                        colorHex = colorHex,
                                        isPinned = pinned,
                                        actions = actions.toList(),
                                    ),
                                )
                            }
                            onDismiss()
                        }
                    },
                ) {
                    Text("L\u01b0u", color = HumeColors.Orange, fontWeight = FontWeight.SemiBold)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = null, tint = HumeColors.TextSecondary, modifier = Modifier.size(20.dp))
                }
            }

            SectionTitle("T\u00ean k\u1ecbch b\u1ea3n")
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text("VD: V\u1ec1 nh\u00e0 bu\u1ed5i t\u1ed1i") },
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle("Bi\u1ec3u t\u01b0\u1ee3ng")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                sceneIconKeys.chunked(5).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { key ->
                            Box(
                                Modifier
                                    .size(46.dp)
                                    .background(
                                        if (icon == key) accent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                                        RoundedCornerShape(12.dp),
                                    )
                                    .border(
                                        1.5.dp,
                                        if (icon == key) accent else Color.Transparent,
                                        RoundedCornerShape(12.dp),
                                    )
                                    .clickable { icon = key },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    editorIcon(key),
                                    contentDescription = null,
                                    tint = if (icon == key) accent else HumeColors.TextSecondary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }

            SectionTitle("M\u00e0u s\u1eafc")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                sceneColors.forEach { hex ->
                    Box(
                        Modifier
                            .size(30.dp)
                            .background(hexColor(hex), CircleShape)
                            .border(
                                2.dp,
                                if (colorHex == hex) HumeColors.TextPrimary else Color.Transparent,
                                CircleShape,
                            )
                            .clickable { colorHex = hex },
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Ghim l\u00ean m\u00e0n h\u00ecnh Home",
                    fontSize = 15.sp,
                    color = HumeColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = pinned,
                    onCheckedChange = { pinned = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = HumeColors.Orange),
                )
            }

            SectionTitle("H\u00e0nh \u0111\u1ed9ng (" + actions.size + ")")
            if (actions.isEmpty()) {
                Text(
                    "Ch\u01b0a c\u00f3 h\u00e0nh \u0111\u1ed9ng \u2014 nh\u1ea5n th\u00eam thi\u1ebft b\u1ecb.",
                    fontSize = 13.sp,
                    color = HumeColors.TextSecondary,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    actions.forEachIndexed { index, action ->
                        ActionRow(
                            action = action,
                            onService = { svc -> actions[index] = action.copy(service = svc) },
                            onRemove = { actions.removeAt(index) },
                        )
                    }
                }
            }

            Row(
                Modifier.clickable { picking = true },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = HumeColors.Orange, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Th\u00eam thi\u1ebft b\u1ecb", fontSize = 14.sp, color = HumeColors.Orange)
            }

            Text(
                "Khi k\u00edch ho\u1ea1t k\u1ecbch b\u1ea3n, app g\u1eedi l\u1ec7nh \u0111\u1ebfn t\u1eebng thi\u1ebft b\u1ecb theo th\u1ee9 t\u1ef1 tr\u00ean.",
                fontSize = 11.sp,
                color = HumeColors.TextSecondary,
            )
        }
    }

    if (picking) {
        SceneEntityPicker(
            entities = entities,
            onPick = { entity ->
                val service = if (entity.id.startsWith("alarm_control_panel.")) "alarm_disarm" else "turn_on"
                actions.add(
                    LocalSceneAction(
                        entityId = entity.id,
                        entityName = friendlyName(entity),
                        service = service,
                    ),
                )
            },
            onDismiss = { picking = false },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = HumeColors.TextSecondary)
}

@Composable
private fun ActionRow(
    action: LocalSceneAction,
    onService: (String) -> Unit,
    onRemove: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val isAlarm = action.entityId.startsWith("alarm_control_panel.")
    val options = if (isAlarm) alarmServices else deviceServices

    Row(
        Modifier
            .fillMaxWidth()
            .glassSurface(radius = HumeShapes.Element)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            domainIcon(action.entityId),
            contentDescription = null,
            tint = HumeColors.TextSecondary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                action.entityName.ifEmpty { action.entityId },
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = HumeColors.TextPrimary,
                maxLines = 1,
            )
            Text(action.entityId, fontSize = 10.sp, color = HumeColors.TextSecondary, maxLines = 1)
        }
        Box {
            Text(
                options.firstOrNull { it.second == action.service }?.first ?: action.service,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = HumeColors.Orange,
                modifier = Modifier.clickable { menu = true }.padding(horizontal = 8.dp, vertical = 4.dp),
            )
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.first) },
                        onClick = { menu = false; onService(option.second) },
                    )
                }
            }
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Rounded.Close, contentDescription = null, tint = HumeColors.TextSecondary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun SceneEntityPicker(
    entities: Map<String, HomeEntity>,
    onPick: (HomeEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val matches = entities.values
        .filter { entity -> pickerDomains.any { entity.id.startsWith(it) } }
        .filter { query.isBlank() || it.id.contains(query, true) || friendlyName(it).contains(query, true) }
        .sortedBy { friendlyName(it).lowercase() }
        .take(200)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Xong") } },
        title = { Text("Ch\u1ecdn thi\u1ebft b\u1ecb") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    label = { Text("T\u00ecm") },
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(
                    Modifier.fillMaxWidth().heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(matches, key = { it.id }) { entity ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(entity) }
                                .padding(vertical = 6.dp),
                        ) {
                            Text(friendlyName(entity), fontSize = 14.sp, color = HumeColors.TextPrimary, maxLines = 1)
                            Text(entity.id, fontSize = 10.sp, color = HumeColors.TextSecondary, maxLines = 1)
                        }
                    }
                }
            }
        },
    )
}

private val pickerDomains = listOf(
    "light.", "switch.", "climate.", "fan.", "cover.", "media_player.",
    "input_boolean.", "alarm_control_panel.",
)

private val deviceServices = listOf(
    "B\u1eadt" to "turn_on",
    "T\u1eaft" to "turn_off",
    "Chuy\u1ec3n" to "toggle",
)

private val alarmServices = listOf(
    "T\u1eaft b\u00e1o \u0111\u1ed9ng" to "alarm_disarm",
    "\u1ede nh\u00e0" to "alarm_arm_home",
    "\u0110i v\u1eafng" to "alarm_arm_away",
    "Ban \u0111\u00eam" to "alarm_arm_night",
    "\u0110i ngh\u1ec9" to "alarm_arm_vacation",
    "Tu\u1ef3 ch\u1ec9nh" to "alarm_arm_custom_bypass",
)

private val sceneColors = listOf(
    "#f9784c", "#73b9f2", "#ad99e6", "#f2d26f",
    "#66d19e", "#f285c9", "#f28073", "#f2b573",
)

private val sceneIconKeys = listOf(
    "sunrise", "moon", "house", "walk", "sparkles",
    "bolt", "leaf", "flame", "music", "game",
    "food", "coffee", "sofa", "bed", "car",
    "plane", "heart", "star", "bell", "lock",
)

private fun editorIcon(key: String): ImageVector = when (key) {
    "sunrise" -> Icons.Rounded.WbTwilight
    "moon" -> Icons.Rounded.Bedtime
    "house" -> Icons.Rounded.Home
    "walk" -> Icons.Rounded.DirectionsWalk
    "sparkles" -> Icons.Rounded.AutoAwesome
    "bolt" -> Icons.Rounded.Bolt
    "leaf" -> Icons.Rounded.WindPower
    "flame" -> Icons.Rounded.LocalFireDepartment
    "music" -> Icons.Rounded.MusicNote
    "game" -> Icons.Rounded.SportsEsports
    "food" -> Icons.Rounded.Restaurant
    "coffee" -> Icons.Rounded.LocalCafe
    "sofa" -> Icons.Rounded.Weekend
    "bed" -> Icons.Rounded.Bedtime
    "car" -> Icons.Rounded.DirectionsCar
    "plane" -> Icons.Rounded.Flight
    "heart" -> Icons.Rounded.Favorite
    "star" -> Icons.Rounded.Star
    "bell" -> Icons.Rounded.Notifications
    "lock" -> Icons.Rounded.Lock
    else -> Icons.Rounded.AutoAwesome
}

private fun domainIcon(entityId: String): ImageVector = when (entityId.substringBefore('.')) {
    "light" -> Icons.Rounded.AutoAwesome
    "switch" -> Icons.Rounded.Power
    "climate" -> Icons.Rounded.Thermostat
    "fan" -> Icons.Rounded.AcUnit
    "cover" -> Icons.Rounded.Blinds
    "media_player" -> Icons.Rounded.Tv
    "alarm_control_panel" -> Icons.Rounded.Shield
    else -> Icons.Rounded.Bolt
}

private fun hexColor(hex: String): Color {
    val cleaned = hex.removePrefix("#")
    val value = cleaned.toLongOrNull(16) ?: return HumeColors.Orange
    return if (cleaned.length <= 6) Color(value or 0xFF000000L) else Color(value)
}

private fun friendlyName(entity: HomeEntity): String {
    val raw = entity.attributes["friendly_name"] as? JsonPrimitive
    return raw?.content ?: entity.id.substringAfter('.').replace('_', ' ')
}

@Suppress("unused")
private val unusedList = mutableStateListOf<String>()
