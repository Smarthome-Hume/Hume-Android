package com.smarthome.hume.ui.manage

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoorFront
import androidx.compose.material.icons.rounded.ElectricalServices
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WindPower
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.smarthome.hume.core.scene.ManagedItem
import com.smarthome.hume.core.scene.ManagedKind
import com.smarthome.hume.core.scene.ManagedListsStore
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeShapes
import com.smarthome.hume.ui.theme.glassSurface
import kotlinx.serialization.json.JsonPrimitive

// Port of Views/Manage/ManagedListView.swift + EditManagedItemView.
// The light pill and the bell on Home count exactly what lives in these two
// lists, so this screen is what drives them.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageListSheet(kind: ManagedKind, ha: HomeAssistantRepository, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val store = remember { ManagedListsStore.get(context) }
    val items by (if (kind == ManagedKind.LIGHTS) store.lights else store.notif)
        .collectAsStateWithLifecycle()
    val entities by ha.entities.collectAsStateWithLifecycle()

    var showHidden by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ManagedItem?>(null) }

    val hiddenCount = items.count { it.hidden }
    val visible = if (showHidden) items else items.filterNot { it.hidden }
    val title = if (kind == ManagedKind.LIGHTS) "Qu\u1ea3n l\u00fd \u0111\u00e8n" else "Qu\u1ea3n l\u00fd th\u00f4ng b\u00e1o"

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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = HumeColors.TextPrimary)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { picking = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = HumeColors.Orange, modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = null, tint = HumeColors.TextSecondary, modifier = Modifier.size(20.dp))
                }
            }

            if (hiddenCount > 0) {
                Row(
                    Modifier.clickable { showHidden = !showHidden },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (showHidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = null,
                        tint = HumeColors.Orange,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        (if (showHidden) "\u1ea8n " else "Hi\u1ec7n ") + hiddenCount + " m\u1ee5c \u0111\u00e3 \u1ea9n",
                        fontSize = 13.sp,
                        color = HumeColors.Orange,
                    )
                }
            }

            if (visible.isEmpty()) {
                Text(
                    "Ch\u01b0a c\u00f3 m\u1ee5c n\u00e0o \u2014 nh\u1ea5n + \u0111\u1ec3 th\u00eam.",
                    fontSize = 13.sp,
                    color = HumeColors.TextSecondary,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                )
            } else {
                visible.forEach { item ->
                    ItemCard(
                        item = item,
                        entity = entities[item.id],
                        onEdit = { editing = item },
                        onToggleHidden = { store.update(item.copy(hidden = !item.hidden), kind) },
                        onRemove = { store.remove(item.id, kind) },
                    )
                }
            }

            Text(
                "T\u1ed5ng: " + items.size +
                    " \u00b7 Hi\u1ec3n th\u1ecb: " + (items.size - hiddenCount) +
                    " \u00b7 \u0110\u00e3 \u1ea9n: " + hiddenCount,
                fontSize = 11.sp,
                color = HumeColors.TextSecondary,
            )
        }
    }

    if (picking) {
        EntityPickerDialog(
            kind = kind,
            entities = entities,
            already = items.map { it.id }.toSet(),
            onPick = { id ->
                store.add(id, kind, if (kind == ManagedKind.LIGHTS) "bulb" else "bell")
            },
            onDismiss = { picking = false },
        )
    }

    editing?.let { item ->
        EditItemDialog(
            item = item,
            entity = entities[item.id],
            onSave = { store.update(it, kind); editing = null },
            onDismiss = { editing = null },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ItemCard(
    item: ManagedItem,
    entity: HomeEntity?,
    onEdit: () -> Unit,
    onToggleHidden: () -> Unit,
    onRemove: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val state = entity?.state ?: "\u2014"

    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .alpha(if (item.hidden) 0.5f else 1f)
                .glassSurface(radius = HumeShapes.Element)
                .combinedClickable(onClick = { menu = true }, onLongClick = { menu = true })
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(42.dp).background(Color.White.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    managedIcon(item.icon),
                    contentDescription = null,
                    tint = if (item.hidden) HumeColors.TextSecondary else HumeColors.TextPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    displayName(item, entity),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (item.hidden) HumeColors.TextSecondary else HumeColors.TextPrimary,
                    maxLines = 1,
                )
                Text(item.id, fontSize = 10.sp, color = HumeColors.TextSecondary, maxLines = 1)
            }
            Text(
                state,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (state == "on") HumeColors.Green else HumeColors.Orange,
            )
        }

        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text("S\u1eeda t\u00ean / icon") },
                onClick = { menu = false; onEdit() },
            )
            DropdownMenuItem(
                text = { Text(if (item.hidden) "Hi\u1ec7n m\u1ee5c n\u00e0y" else "\u1ea8n m\u1ee5c n\u00e0y") },
                onClick = { menu = false; onToggleHidden() },
            )
            DropdownMenuItem(
                text = { Text("Xo\u00e1", color = HumeColors.Red) },
                onClick = { menu = false; onRemove() },
            )
        }
    }
}

@Composable
private fun EntityPickerDialog(
    kind: ManagedKind,
    entities: Map<String, HomeEntity>,
    already: Set<String>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val domains = if (kind == ManagedKind.LIGHTS) listOf("light.", "switch.", "group.")
    else listOf("sensor.", "binary_sensor.")
    var query by remember { mutableStateOf("") }

    val matches = entities.values
        .filter { entity -> domains.any { entity.id.startsWith(it) } }
        .filterNot { it.id in already }
        .filter { query.isBlank() || it.id.contains(query, true) || friendly(it).contains(query, true) }
        .sortedBy { friendly(it).lowercase() }
        .take(200)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Xong") } },
        title = {
            Text(
                if (kind == ManagedKind.LIGHTS) "Ch\u1ecdn \u0111\u00e8n / c\u00f4ng t\u1eafc"
                else "Ch\u1ecdn c\u1ea3m bi\u1ebfn",
            )
        },
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
                                .clickable { onPick(entity.id) }
                                .padding(vertical = 6.dp),
                        ) {
                            Text(friendly(entity), fontSize = 14.sp, color = HumeColors.TextPrimary, maxLines = 1)
                            Text(entity.id, fontSize = 10.sp, color = HumeColors.TextSecondary, maxLines = 1)
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun EditItemDialog(
    item: ManagedItem,
    entity: HomeEntity?,
    onSave: (ManagedItem) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(item.name) }
    var icon by remember { mutableStateOf(item.icon) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onSave(item.copy(name = name, icon = icon)) }) {
                Text("L\u01b0u")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hu\u1ef7") } },
        title = { Text("S\u1eeda m\u1ee5c") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(entity?.let { friendly(it) } ?: item.id) },
                    modifier = Modifier.fillMaxWidth(),
                )
                iconKeys.chunked(5).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { key ->
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .background(
                                        if (icon == key) HumeColors.Orange else Color.White.copy(alpha = 0.10f),
                                        RoundedCornerShape(12.dp),
                                    )
                                    .clickable { icon = key },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    managedIcon(key),
                                    contentDescription = null,
                                    tint = if (icon == key) Color.White else HumeColors.TextPrimary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
                Text("entity_id: " + item.id, fontSize = 12.sp, color = HumeColors.TextSecondary)
            }
        },
    )
}

private val iconKeys = listOf(
    "bulb", "lamp", "bell", "sensor", "thermometer",
    "humidity", "door", "bolt", "fan", "tv",
    "lock", "fire", "wind", "plug", "house",
)

private fun managedIcon(key: String): ImageVector = when (key) {
    "bulb", "lamp", "lightbulb" -> Icons.Rounded.Lightbulb
    "bell" -> Icons.Rounded.Notifications
    "sensor" -> Icons.Rounded.Sensors
    "thermometer" -> Icons.Rounded.Thermostat
    "humidity" -> Icons.Rounded.WaterDrop
    "door" -> Icons.Rounded.DoorFront
    "bolt" -> Icons.Rounded.Bolt
    "fan" -> Icons.Rounded.WindPower
    "tv" -> Icons.Rounded.Tv
    "lock" -> Icons.Rounded.Lock
    "fire" -> Icons.Rounded.LocalFireDepartment
    "wind" -> Icons.Rounded.Air
    "plug" -> Icons.Rounded.ElectricalServices
    "house" -> Icons.Rounded.Home
    else -> Icons.Rounded.Lightbulb
}

private fun displayName(item: ManagedItem, entity: HomeEntity?): String =
    if (item.name.isNotEmpty()) item.name else entity?.let { friendly(it) } ?: item.id

private fun friendly(entity: HomeEntity): String {
    val raw = entity.attributes["friendly_name"] as? JsonPrimitive
    return raw?.content ?: entity.id.substringAfter('.').replace('_', ' ')
}
