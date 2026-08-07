package com.smarthome.hume.ui.sensors

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.core.storage.SensorRecordStore
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeShapes
import com.smarthome.hume.ui.theme.glassSurface
import kotlinx.serialization.json.JsonPrimitive

// Port of Views/Sensors/SensorsView.swift.
//
// Same data model as iOS: one row per entity, built from the entity registry
// plus the live state map, with the user-owned bits (rename, hide, freeze,
// delete) kept locally in SensorRecordStore.
//
// This sheet walks every entity in the system, so it asks the repository for
// realtime updates only while it is on screen. Closing the sheet drops those
// entities straight back to frozen instead of leaving the whole system live.
//
// Chips and icon wells use tertiarySystemFill from the theme, matching the
// SwiftUI original, so nothing stays white on a dark background.

private val domainLabels = mapOf(
    "sensor" to "C\u1ea3m bi\u1ebfn",
    "binary_sensor" to "C\u1ea3m bi\u1ebfn nh\u1ecb ph\u00e2n",
    "light" to "\u0110\u00e8n",
    "switch" to "C\u00f4ng t\u1eafc",
    "climate" to "\u0110i\u1ec1u ho\u00e0",
    "fan" to "Qu\u1ea1t",
    "cover" to "R\u00e8m/C\u1eeda",
    "media_player" to "Ph\u00e1t media",
    "automation" to "T\u1ef1 \u0111\u1ed9ng ho\u00e1",
    "scene" to "C\u1ea3nh",
    "script" to "K\u1ecbch b\u1ea3n",
    "person" to "Ng\u01b0\u1eddi",
    "device_tracker" to "Theo d\u00f5i thi\u1ebft b\u1ecb",
    "input_boolean" to "C\u00f4ng t\u1eafc \u1ea3o",
    "input_number" to "S\u1ed1 \u1ea3o",
    "number" to "S\u1ed1",
    "weather" to "Th\u1eddi ti\u1ebft",
    "camera" to "Camera",
    "sun" to "M\u1eb7t tr\u1eddi",
    "update" to "C\u1eadp nh\u1eadt",
    "button" to "N\u00fat",
    "select" to "L\u1ef1a ch\u1ecdn",
    "lock" to "Kho\u00e1",
    "vacuum" to "Robot h\u00fat b\u1ee5i",
)

private val platformLabels = mapOf(
    "mqtt" to "MQTT", "zha" to "Zigbee (ZHA)", "z2m" to "Zigbee2MQTT",
    "homekit_controller" to "HomeKit", "esphome" to "ESPHome",
    "tuya" to "Tuya / Smart Life", "xiaomi_miot" to "Xiaomi Mi Home",
    "tasmota" to "Tasmota", "shelly" to "Shelly", "broadlink" to "Broadlink",
    "hue" to "Philips Hue", "tplink" to "TP-Link Kasa", "wled" to "WLED",
    "met" to "Th\u1eddi ti\u1ebft (Met)", "sun" to "M\u1eb7t tr\u1eddi", "frigate" to "Frigate NVR",
    "solis" to "Solis Inverter", "template" to "Template", "group" to "Nh\u00f3m",
    "automation" to "T\u1ef1 \u0111\u1ed9ng ho\u00e1", "script" to "K\u1ecbch b\u1ea3n",
)

private enum class GroupMode(val label: String) {
    DOMAIN("Theo lo\u1ea1i"),
    DEVICE("Theo thi\u1ebft b\u1ecb"),
    INTEGRATION("T\u00edch h\u1ee3p"),
}

private enum class ActiveFilter(val label: String) {
    ALL("T\u1ea5t c\u1ea3"),
    DASHBOARD("Dashboard"),
    ACTIVE("Ho\u1ea1t \u0111\u1ed9ng"),
    INACTIVE("Ngo\u00e0i Dashboard"),
    FROZEN("\u0110\u00f3ng b\u0103ng"),
}

private data class SensorRow(
    val id: String,
    val name: String,
    val haName: String,
    val domain: String,
    val state: String,
    val unit: String,
    val platform: String,
    val hidden: Boolean,
    val frozen: Boolean,
    val renamed: Boolean,
    val inDashboard: Boolean,
    val active: Boolean,
    val attributes: List<Pair<String, String>>,
) {
    val valueText: String get() = if (unit.isEmpty()) state else state + " " + unit
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorsSheet(ha: HomeAssistantRepository, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SensorRecordStore.get(context) }
    val entities by ha.entities.collectAsStateWithLifecycle()
    val registry by ha.registry.collectAsStateWithLifecycle()
    val watched by ha.watchedEntityIds.collectAsStateWithLifecycle()
    val overrides by store.overrides.collectAsStateWithLifecycle()

    // Sheet nay duyet toan bo entity, nen chi cho realtime khi no dang mo.
    DisposableEffect(Unit) {
        ha.setSensorsSheetOpen(true)
        onDispose { ha.setSensorsSheetOpen(false) }
    }

    var query by remember { mutableStateOf("") }
    var groupMode by remember { mutableStateOf(GroupMode.DOMAIN) }
    var activeFilter by remember { mutableStateOf(ActiveFilter.ALL) }
    var domainFilter by remember { mutableStateOf<String?>(null) }
    var showHidden by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<SensorRow?>(null) }
    var detailTarget by remember { mutableStateOf<SensorRow?>(null) }
    var confirmRestore by remember { mutableStateOf(false) }

    val rows = entities.values.mapNotNull { entity ->
        val override = overrides[entity.id]
        if (override?.deleted == true) return@mapNotNull null
        val registryName = registry[entity.id]?.name
        val haName = registryName
            ?: entity.attributeString("friendly_name")
            ?: entity.id.substringAfter('.').replace('_', ' ')
        val state = entity.state
        SensorRow(
            id = entity.id,
            name = override?.localName ?: haName,
            haName = haName,
            domain = entity.id.substringBefore('.'),
            state = state,
            unit = entity.attributeString("unit_of_measurement").orEmpty(),
            platform = registry[entity.id]?.platform.orEmpty(),
            hidden = override?.hidden == true,
            frozen = override?.frozen == true,
            renamed = override?.localName != null,
            inDashboard = watched.contains(entity.id),
            active = state.isNotEmpty() && state != "unavailable" && state != "unknown",
            attributes = entity.attributes.map { it.key to it.value.toString().trim('"') }.sortedBy { it.first },
        )
    }

    val visible = rows.filter { showHidden || !it.hidden }
    val hiddenCount = rows.count { it.hidden }
    val domainCounts = visible.groupingBy { it.domain }.eachCount().toList().sortedByDescending { it.second }

    val filtered = visible.filter { row ->
        if (domainFilter != null && row.domain != domainFilter) return@filter false
        val passesFilter = when (activeFilter) {
            ActiveFilter.ALL -> true
            ActiveFilter.DASHBOARD -> row.inDashboard
            ActiveFilter.ACTIVE -> row.active && !row.frozen
            ActiveFilter.INACTIVE -> !row.inDashboard
            ActiveFilter.FROZEN -> row.frozen
        }
        if (!passesFilter) return@filter false
        if (query.isBlank()) return@filter true
        val q = query.lowercase()
        row.name.lowercase().contains(q) ||
            row.id.lowercase().contains(q) ||
            row.state.lowercase().contains(q) ||
            row.domain.lowercase().contains(q) ||
            domainLabels[row.domain].orEmpty().lowercase().contains(q)
    }

    // activeFirst(): dashboard+active, dashboard+idle, off dashboard, frozen.
    val ordered = filtered.sortedWith(
        compareBy<SensorRow> { row ->
            when {
                row.frozen -> 3
                row.inDashboard -> if (row.active) 0 else 1
                else -> 2
            }
        }.thenBy { it.name.lowercase() }
    )

    val groups: List<Pair<String, List<SensorRow>>> = when (groupMode) {
        GroupMode.DOMAIN -> ordered.groupBy { domainLabels[it.domain] ?: it.domain }
            .toList().sortedBy { it.first }
        GroupMode.DEVICE -> ordered.groupBy { deviceName(it.id) }
            .toList().sortedBy { it.first }
        GroupMode.INTEGRATION -> ordered.groupBy { integrationName(it.platform) }
            .toList().sortedBy { it.first }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = HumeColors.Background,
        dragHandle = null,
    ) {
        Column(Modifier.fillMaxSize().padding(bottom = 12.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "C\u1ea3m bi\u1ebfn & Thi\u1ebft b\u1ecb",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = HumeColors.TextPrimary,
                    )
                    Text(
                        "" + visible.size + " hi\u1ec3n th\u1ecb \u00b7 " + hiddenCount + " \u0111\u00e3 \u1ea9n",
                        fontSize = 11.sp,
                        color = HumeColors.TextSecondary,
                    )
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = null,
                            tint = HumeColors.TextSecondary,
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (showHidden) "\u1ea8n c\u00e1c m\u1ee5c \u0111\u00e3 \u1ea9n"
                                    else "Hi\u1ec7n \u0111\u00e3 \u1ea9n (" + hiddenCount + ")",
                                )
                            },
                            onClick = { menuOpen = false; showHidden = !showHidden },
                        )
                        DropdownMenuItem(
                            text = { Text("T\u1ea3i l\u1ea1i t\u1eeb HA") },
                            leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
                            onClick = { menuOpen = false; ha.refresh() },
                        )
                        DropdownMenuItem(
                            text = {
                                Text("Kh\u00f4i ph\u1ee5c to\u00e0n b\u1ed9 t\u1eeb HA", color = HumeColors.Red)
                            },
                            onClick = { menuOpen = false; confirmRestore = true },
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = null, tint = HumeColors.TextSecondary)
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("T\u00ecm t\u00ean, entity_id, domain\u2026") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            // Group mode segments
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GroupMode.values().forEach { mode ->
                    Chip(
                        text = mode.label,
                        active = groupMode == mode,
                        modifier = Modifier.weight(1f),
                    ) { groupMode = mode }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ActiveFilter.values().forEach { filter ->
                    Chip(text = filter.label, active = activeFilter == filter) { activeFilter = filter }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Chip(
                    text = "T\u1ea5t c\u1ea3 (" + visible.size + ")",
                    active = domainFilter == null,
                ) { domainFilter = null }
                domainCounts.forEach { pair ->
                    val label = (domainLabels[pair.first] ?: pair.first) + " (" + pair.second + ")"
                    Chip(text = label, active = domainFilter == pair.first) {
                        domainFilter = if (domainFilter == pair.first) null else pair.first
                    }
                }
            }

            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                groups.forEach { group ->
                    item(key = "h_" + group.first) {
                        Text(
                            group.first,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HumeColors.TextSecondary,
                            modifier = Modifier.padding(start = 6.dp, top = 10.dp, bottom = 2.dp),
                        )
                    }
                    items(group.second.size, key = { "r_" + group.first + "_" + group.second[it].id }) { index ->
                        val row = group.second[index]
                        SensorRowView(
                            row = row,
                            onOpen = { detailTarget = row },
                            onRename = { renameTarget = row },
                            onToggleHidden = { store.setHidden(row.id, !row.hidden) },
                            onToggleFrozen = {
                                val next = !row.frozen
                                store.setFrozen(row.id, next)
                                if (next) ha.unwatchEntity(row.id) else ha.watchEntity(row.id)
                            },
                            onDelete = { store.delete(row.id) },
                        )
                    }
                }
            }
        }
    }

    renameTarget?.let { row ->
        var text by remember(row.id) { mutableStateOf(row.name) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            containerColor = HumeColors.Card,
            title = { Text("\u0110\u1ed5i t\u00ean") },
            text = {
                Column {
                    Text("entity_id: " + row.id, fontSize = 12.sp, color = HumeColors.TextSecondary)
                    Spacer(Modifier.size(10.dp))
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        singleLine = true,
                        placeholder = { Text("T\u00ean hi\u1ec3n th\u1ecb") },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    store.setLocalName(row.id, if (text.trim() == row.haName) null else text)
                    renameTarget = null
                }) { Text("L\u01b0u") }
            },
            dismissButton = {
                TextButton(onClick = {
                    store.setLocalName(row.id, null)
                    renameTarget = null
                }) { Text("\u0110\u1eb7t l\u1ea1i t\u00ean g\u1ed1c", color = HumeColors.Red) }
            },
        )
    }

    detailTarget?.let { row ->
        AlertDialog(
            onDismissRequest = { detailTarget = null },
            containerColor = HumeColors.Card,
            title = { Text(row.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) },
            text = {
                Column(
                    Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    DetailRow("entity_id", row.id)
                    if (row.renamed) DetailRow("T\u00ean g\u1ed1c (HA)", row.haName)
                    DetailRow("Tr\u1ea1ng th\u00e1i", row.valueText)
                    DetailRow("Ph\u00e2n lo\u1ea1i", domainLabels[row.domain] ?: row.domain)
                    if (row.platform.isNotEmpty()) DetailRow("T\u00edch h\u1ee3p", integrationName(row.platform))
                    DetailRow("Dashboard", if (row.inDashboard) "C\u00f3" else "Kh\u00f4ng")
                    if (row.attributes.isNotEmpty()) {
                        Spacer(Modifier.size(6.dp))
                        Text(
                            "Thu\u1ed9c t\u00ednh",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HumeColors.TextSecondary,
                        )
                        row.attributes.forEach { attribute -> DetailRow(attribute.first, attribute.second) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { detailTarget = null }) { Text("\u0110\u00f3ng") }
            },
        )
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            containerColor = HumeColors.Card,
            title = { Text("Kh\u00f4i ph\u1ee5c to\u00e0n b\u1ed9 t\u1eeb HA?") },
            text = {
                Text(
                    "To\u00e0n b\u1ed9 c\u1ea3m bi\u1ebfn s\u1ebd hi\u1ec7n l\u1ea1i. T\u00ean \u0111\u00e3 \u0111\u1ed5i, tr\u1ea1ng th\u00e1i \u1ea9n v\u00e0 \u0111\u00f3ng b\u0103ng s\u1ebd b\u1ecb xo\u00e1.",
                    fontSize = 13.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    store.restoreAll()
                    ha.refresh()
                    confirmRestore = false
                }) { Text("Kh\u00f4i ph\u1ee5c", color = HumeColors.Red) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestore = false }) { Text("Hu\u1ef7") }
            },
        )
    }
}

@Composable
private fun SensorRowView(
    row: SensorRow,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onToggleHidden: () -> Unit,
    onToggleFrozen: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .alpha(if (row.hidden) 0.5f else 1f)
                .glassSurface(radius = 14.dp)
                .clickable { onOpen() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(HumeColors.FillTertiary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (row.frozen) Icons.Rounded.AcUnit else Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = if (row.frozen) Color(0xFF4FC3F7) else HumeColors.TextSecondary,
                        modifier = Modifier.size(15.dp),
                    )
                }
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (row.active) Color(0xFF66BB6A) else Color(0xFFEF5350)),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        row.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (row.hidden) HumeColors.TextSecondary else HumeColors.TextPrimary,
                        maxLines = 1,
                    )
                    if (row.renamed) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Rounded.Edit,
                            contentDescription = null,
                            tint = HumeColors.Orange,
                            modifier = Modifier.size(11.dp),
                        )
                    }
                    if (row.hidden) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Rounded.VisibilityOff,
                            contentDescription = null,
                            tint = HumeColors.TextSecondary,
                            modifier = Modifier.size(11.dp),
                        )
                    }
                }
                Text(row.id, fontSize = 10.sp, color = HumeColors.TextSecondary, maxLines = 1)
            }
            Text(
                row.valueText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (row.hidden) HumeColors.TextSecondary else HumeColors.Orange,
                maxLines = 1,
            )
            IconButton(onClick = { menu = true }, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Rounded.MoreVert,
                    contentDescription = null,
                    tint = HumeColors.TextSecondary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text("\u0110\u1ed5i t\u00ean") },
                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                onClick = { menu = false; onRename() },
            )
            DropdownMenuItem(
                text = { Text(if (row.hidden) "Hi\u1ec7n" else "\u1ea8n") },
                leadingIcon = {
                    Icon(
                        if (row.hidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                        contentDescription = null,
                    )
                },
                onClick = { menu = false; onToggleHidden() },
            )
            DropdownMenuItem(
                text = { Text(if (row.frozen) "B\u1eadt realtime" else "\u0110\u00f3ng b\u0103ng") },
                leadingIcon = {
                    Icon(
                        if (row.frozen) Icons.Rounded.Bolt else Icons.Rounded.AcUnit,
                        contentDescription = null,
                    )
                },
                onClick = { menu = false; onToggleFrozen() },
            )
            DropdownMenuItem(
                text = { Text("Xo\u00e1 kh\u1ecfi app", color = HumeColors.Red) },
                leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = HumeColors.Red) },
                onClick = { menu = false; onDelete() },
            )
        }
    }
}

@Composable
private fun Chip(
    text: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(HumeShapes.Pill))
            .background(if (active) HumeColors.OrangeSoft else HumeColors.FillTertiary)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize = 12.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            color = if (active) HumeColors.TextPrimary else HumeColors.TextSecondary,
            maxLines = 1,
        )
    }
}

@Composable
private fun DetailRow(key: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(key, fontSize = 12.sp, color = HumeColors.TextSecondary, modifier = Modifier.weight(1f))
        Text(
            value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = HumeColors.TextPrimary,
            modifier = Modifier.weight(1.4f),
        )
    }
}

private fun HomeEntity.attributeString(key: String): String? =
    (attributes[key] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }

/** deviceKey(for:) in SensorsView.swift: drop the trailing measurement word. */
private fun deviceName(entityId: String): String {
    val body = entityId.substringAfter('.')
    val words = body.split('_')
    val key = if (words.size <= 2) body else words.dropLast(1).joinToString("_")
    return key.split('_').joinToString(" ") { part ->
        part.replaceFirstChar { it.uppercase() }
    }
}

private fun integrationName(platform: String): String {
    if (platform.isEmpty()) return "Kh\u00f4ng x\u00e1c \u0111\u1ecbnh"
    platformLabels[platform]?.let { return it }
    return platform.split('_').joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
}
