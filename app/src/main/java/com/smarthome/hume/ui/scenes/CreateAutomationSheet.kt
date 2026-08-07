package com.smarthome.hume.ui.scenes

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.ha.AutomationApi
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.core.storage.HumeSettings
import com.smarthome.hume.core.storage.SettingsStore
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeShapes
import com.smarthome.hume.ui.theme.glassSurface
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** TrigOp in ScenesView.swift */
private enum class TrigOp(val label: String) {
    Above("L\u1edbn h\u01a1n"),
    Below("Nh\u1ecf h\u01a1n"),
    TurnsOn("Chuy\u1ec3n sang B\u1eacT"),
    TurnsOff("Chuy\u1ec3n sang T\u1eaeT"),
}

/** domains of the trigger picker sheet */
private val triggerDomains = listOf(
    "sensor.", "binary_sensor.", "device_tracker.", "person.", "input_number.", "input_boolean.",
)

/** domains of the action picker sheet */
private val actionDomains = listOf(
    "light.", "switch.", "fan.", "climate.", "input_boolean.", "scene.", "script.", "media_player.", "cover.",
)

/** HomeEntity.friendlyName in Models.swift */
private fun HomeEntity?.friendly(fallback: String): String {
    val raw = (this?.attributes?.get("friendly_name") as? JsonPrimitive)?.contentOrNull
    return if (raw.isNullOrBlank()) fallback else raw
}

/**
 * CreateAutomationView in ScenesView.swift. Two sections, KHI (the trigger) and
 * TH\u00cc (the action), then a POST to the Home Assistant config API. Numeric
 * comparisons send a numeric_state trigger; the on / off options send a state
 * trigger, matching the Swift `save()` switch one for one.
 *
 * Unselected option chips use the theme fill so they stay legible in dark mode;
 * only the selected chip is solid orange with white text.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAutomationSheet(ha: HomeAssistantRepository, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore(context) }
    val settings by settingsStore.settings.collectAsState(initial = HumeSettings())
    val entities by ha.entities.collectAsState()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf("") }
    var trigEntity by remember { mutableStateOf("") }
    var trigOp by remember { mutableStateOf(TrigOp.Above) }
    var trigValue by remember { mutableStateOf("") }
    var actEntity by remember { mutableStateOf("") }
    var actOn by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var resultMsg by remember { mutableStateOf<String?>(null) }
    var picking by remember { mutableStateOf<String?>(null) }

    val needsValue = trigOp == TrigOp.Above || trigOp == TrigOp.Below
    val canSave = name.isNotBlank() && trigEntity.isNotEmpty() && actEntity.isNotEmpty() &&
        (!needsValue || trigValue.toDoubleOrNull() != null)

    fun nameOf(id: String): String = entities[id].friendly(id)

    fun save() {
        saving = true
        resultMsg = null
        val trigger = when (trigOp) {
            TrigOp.Above -> "{\"platform\":\"numeric_state\",\"entity_id\":" + AutomationApi.quote(trigEntity) +
                ",\"above\":" + (trigValue.toDoubleOrNull() ?: 0.0) + "}"
            TrigOp.Below -> "{\"platform\":\"numeric_state\",\"entity_id\":" + AutomationApi.quote(trigEntity) +
                ",\"below\":" + (trigValue.toDoubleOrNull() ?: 0.0) + "}"
            TrigOp.TurnsOn -> "{\"platform\":\"state\",\"entity_id\":" + AutomationApi.quote(trigEntity) + ",\"to\":\"on\"}"
            TrigOp.TurnsOff -> "{\"platform\":\"state\",\"entity_id\":" + AutomationApi.quote(trigEntity) + ",\"to\":\"off\"}"
        }
        val domain = actEntity.substringBefore('.', "homeassistant")
        val service = domain + "." + (if (actOn) "turn_on" else "turn_off")
        val action = "{\"service\":" + AutomationApi.quote(service) +
            ",\"target\":{\"entity_id\":" + AutomationApi.quote(actEntity) + "}}"

        scope.launch {
            val ok = AutomationApi.createAutomation(
                haUrl = settings.haUrl,
                token = settings.haToken,
                alias = name.trim(),
                triggerJson = trigger,
                actionJson = action,
            )
            saving = false
            if (ok) {
                ha.refresh()
                onDismiss()
            } else {
                resultMsg = "Kh\u00f4ng t\u1ea1o \u0111\u01b0\u1ee3c. HA c\u00f3 th\u1ec3 ch\u01b0a b\u1eadt t\u00edch h\u1ee3p c\u1ea5u h\u00ecnh (config). H\u00e3y ki\u1ec3m tra quy\u1ec1n token."
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = HumeColors.Background,
        shape = RoundedCornerShape(topStart = HumeShapes.Sheet, topEnd = HumeShapes.Sheet),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "T\u1ea1o t\u1ef1 \u0111\u1ed9ng ho\u00e1",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = HumeColors.TextPrimary,
                )
                Spacer(Modifier.weight(1f))
                if (saving) {
                    Text(
                        "\u0110ang l\u01b0u\u2026",
                        fontSize = 13.sp,
                        color = HumeColors.TextSecondary,
                    )
                } else if (canSave) {
                    TextButton(onClick = { save() }) {
                        Text("L\u01b0u", color = HumeColors.Orange, fontWeight = FontWeight.SemiBold)
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "\u0110\u00f3ng", tint = HumeColors.TextSecondary)
                }
            }

            // Section "T\u00ean"
            SectionLabel("T\u00ean")
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("VD: B\u1eadt qu\u1ea1t khi n\u00f3ng") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(HumeShapes.Element),
            )

            // Section "KHI"
            SectionLabel("KHI (\u0111i\u1ec1u ki\u1ec7n)")
            Column(
                Modifier.fillMaxWidth().glassSurface(radius = HumeShapes.Element).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PickRow(
                    label = "C\u1ea3m bi\u1ebfn",
                    value = if (trigEntity.isEmpty()) "Ch\u1ecdn c\u1ea3m bi\u1ebfn" else nameOf(trigEntity),
                ) { picking = "trigger" }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    TrigOp.entries.forEach { op ->
                        val active = op == trigOp
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) HumeColors.Orange else HumeColors.FillTertiary)
                                .clickable { trigOp = op }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                op.label,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (active) Color.White else HumeColors.TextSecondary,
                            )
                        }
                    }
                }

                if (needsValue) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Gi\u00e1 tr\u1ecb", fontSize = 14.sp, color = HumeColors.TextPrimary)
                        Spacer(Modifier.weight(1f))
                        OutlinedTextField(
                            value = trigValue,
                            onValueChange = { trigValue = it },
                            placeholder = { Text("0") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.width(110.dp),
                            shape = RoundedCornerShape(HumeShapes.Element),
                        )
                    }
                }
            }

            // Section "TH\u00cc"
            SectionLabel("TH\u00cc (h\u00e0nh \u0111\u1ed9ng)")
            Column(
                Modifier.fillMaxWidth().glassSurface(radius = HumeShapes.Element).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PickRow(
                    label = "Thi\u1ebft b\u1ecb",
                    value = if (actEntity.isEmpty()) "Ch\u1ecdn thi\u1ebft b\u1ecb" else nameOf(actEntity),
                ) { picking = "action" }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(true to "B\u1eadt", false to "T\u1eaft").forEach { pair ->
                        val active = pair.first == actOn
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) HumeColors.Orange else HumeColors.FillTertiary)
                                .clickable { actOn = pair.first }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                pair.second,
                                fontSize = 13.sp,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (active) Color.White else HumeColors.TextSecondary,
                            )
                        }
                    }
                }
            }

            resultMsg?.let {
                Text(it, fontSize = 13.sp, color = HumeColors.TextSecondary)
            }

            Text(
                "T\u1ef1 \u0111\u1ed9ng ho\u00e1 \u0111\u01b0\u1ee3c ghi th\u1eb3ng v\u00e0o Home Assistant, hi\u1ec7n ngay trong m\u1ee5c T\u1ef1 \u0111\u1ed9ng ho\u00e1.",
                fontSize = 11.sp,
                color = HumeColors.TextSecondary,
            )
        }
    }

    picking?.let { mode ->
        val domains = if (mode == "trigger") triggerDomains else actionDomains
        AutomationEntityPicker(
            title = if (mode == "trigger") "Ch\u1ecdn c\u1ea3m bi\u1ebfn" else "Ch\u1ecdn thi\u1ebft b\u1ecb",
            entityIds = entities.keys.filter { id -> domains.any { id.startsWith(it) } },
            nameOf = { nameOf(it) },
            stateOf = { entities[it]?.state.orEmpty() },
            onPick = {
                if (mode == "trigger") trigEntity = it else actEntity = it
                picking = null
            },
            onDismiss = { picking = null },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = HumeColors.TextSecondary,
    )
}

/** rowPick(label:value:) in ScenesView.swift */
@Composable
private fun PickRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 14.sp, color = HumeColors.TextPrimary)
        Spacer(Modifier.weight(1f))
        Text(value, fontSize = 13.sp, color = HumeColors.TextSecondary, maxLines = 1)
        Spacer(Modifier.width(4.dp))
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = HumeColors.TextSecondary,
            modifier = Modifier.size(14.dp),
        )
    }
}

/** EntityPickerView, single-select mode. */
@Composable
private fun AutomationEntityPicker(
    title: String,
    entityIds: List<String>,
    nameOf: (String) -> String,
    stateOf: (String) -> String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, entityIds) {
        entityIds
            .filter {
                query.isBlank() ||
                    it.contains(query, ignoreCase = true) ||
                    nameOf(it).contains(query, ignoreCase = true)
            }
            .sortedBy { nameOf(it).lowercase() }
            .take(200)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HumeColors.Card,
        title = { Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("T\u00ecm t\u00ean, entity_id\u2026") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(HumeShapes.Element),
                )
                Spacer(Modifier.size(8.dp))
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(filtered, key = { it }) { id ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(id) }
                                .padding(vertical = 8.dp),
                        ) {
                            Text(
                                nameOf(id),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = HumeColors.TextPrimary,
                                maxLines = 1,
                            )
                            Text(
                                id + " \u00b7 " + stateOf(id),
                                fontSize = 11.sp,
                                color = HumeColors.TextSecondary,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Hu\u1ef7", color = HumeColors.TextSecondary)
            }
        },
    )
}
