package com.smarthome.hume.feature.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smarthome.hume.core.ui.components.M3ETextField
import kotlinx.coroutines.flow.StateFlow

// ---------------------------------------------------------------------------
// Models
// ---------------------------------------------------------------------------

/**
 * Dòng entity tối thiểu mà picker cần: entity_id + tên hiển thị.
 *
 * App layer map từ `ha.entities` (Map<String, HomeEntity> của app module):
 * ```
 * ha.entities.map { map -> map.map { (id, e) ->
 *     PickerEntity(id, entityFriendlyName(id,
 *         e.attributes["friendly_name"]?.jsonPrimitive?.contentOrNull))
 * } }
 * ```
 */
data class PickerEntity(
    val id: String,
    val friendlyName: String,
)

/**
 * Form thêm/sửa thiết bị dùng thẳng [DynDevice] (RoomManagement.kt — port iOS
 * DeviceConfig + id). Thêm mới: `DynDevice()` tự sinh id; sửa: `copy()` giữ id cũ.
 *
 * Helper port từ iOS `HAEntity.friendlyName`:
 * lấy từ `attributes["friendly_name"]`, fallback về entity_id.
 *
 * Ghi chú module: `HomeEntity` (id/state/attributes) nằm ở app module
 * (`com.smarthome.hume.core.model`), mà `:feature:home` không phụ thuộc app
 * module nên không thể viết extension trực tiếp trên class đó ở đây.
 * Hàm này nhận thẳng giá trị friendly_name để app layer gọi khi map
 * `ha.entities` -> [PickerEntity] (xem docstring ở trên).
 */
fun entityFriendlyName(id: String, friendlyNameAttr: String?): String =
    friendlyNameAttr?.takeIf { it.isNotBlank() } ?: id

/**
 * Tự động nhận diện loại thiết bị theo domain của entity — port 1:1
 * switch trong iOS `DeviceEditView.onSelect`.
 */
fun detectDeviceType(entityId: String): String =
    when (entityId.substringBefore(".")) {
        "climate" -> "climate"
        "cover" -> "cover"
        "lock" -> "lock"
        "fan" -> "fan"
        else -> "toggle" // light, switch, ...
    }

/** Nhãn tiếng Việt cho dropdown chọn loại (user chọn lại được nếu auto-detect sai). */
val DEVICE_TYPE_LABELS: Map<String, String> = mapOf(
    "toggle" to "Công tắc/Đèn",
    "fan" to "Quạt",
    "climate" to "Điều hoà",
    "cover" to "Rèm/Mành",
    "lock" to "Khoá",
)

// ---------------------------------------------------------------------------
// DeviceEditScreen — port iOS DeviceEditView
// ---------------------------------------------------------------------------

/**
 * Màn hình thêm/sửa thiết bị trong phòng.
 *
 * @param initial null = thêm mới; non-null = sửa (prefill từ device cũ).
 * @param entities StateFlow Map entity_id -> [PickerEntity] (app layer map từ ha.entities).
 * @param usedDeviceEntities entity đã dùng ở thiết bị khác -> ẩn khỏi picker thiết bị.
 * @param usedPowerEntities sensor công suất đã dùng ở thiết bị khác -> ẩn khỏi picker sensor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceEditScreen(
    initial: DynDevice?,
    entities: StateFlow<Map<String, PickerEntity>>,
    usedDeviceEntities: Set<String> = emptySet(),
    usedPowerEntities: Set<String> = emptySet(),
    onSave: (DynDevice) -> Unit,
    onDismiss: () -> Unit,
) {
    var entityId by remember(initial) { mutableStateOf(initial?.entityId ?: "") }
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var sub by remember(initial) { mutableStateOf(initial?.sub ?: "") }
    var icon by remember(initial) { mutableStateOf(initial?.icon ?: "lightbulb") }
    var type by remember(initial) { mutableStateOf(initial?.type ?: "toggle") }
    var powerEntity by remember(initial) { mutableStateOf(initial?.powerEntity ?: "") }
    var pickerFor by remember { mutableStateOf<String?>(null) } // null | "device" | "power"
    var typeMenuOpen by remember { mutableStateOf(false) }
    val entityMap by entities.collectAsState()

    val canSave = entityId.isNotBlank() && name.isNotBlank()

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (initial == null) "Thêm thiết bị" else "Sửa thiết bị") },
                    navigationIcon = {
                        TextButton(onClick = onDismiss) { Text("Huỷ") }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                val base = initial ?: DynDevice(icon = "lightbulb")
                                onSave(
                                    base.copy(
                                        type = type,
                                        entityId = entityId,
                                        name = name,
                                        sub = sub,
                                        icon = icon,
                                        powerEntity = powerEntity.takeIf { it.isNotBlank() },
                                    )
                                )
                                onDismiss()
                            },
                            enabled = canSave,
                        ) { Text("Lưu") }
                    },
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SectionTitle("Thiết bị")
                EntityField(
                    label = "Thiết bị",
                    value = entityId,
                    onOpen = { pickerFor = "device" },
                )
                M3ETextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Tên hiển thị",
                )
                M3ETextField(
                    value = sub,
                    onValueChange = { sub = it },
                    label = "Mô tả",
                )
                // Icon picker tạm dùng TextField (agent khác làm component riêng).
                M3ETextField(
                    value = icon,
                    onValueChange = { icon = it },
                    label = "Icon",
                )
                ExposedDropdownMenuBox(
                    expanded = typeMenuOpen,
                    onExpandedChange = { typeMenuOpen = it },
                ) {
                    M3ETextField(
                        value = DEVICE_TYPE_LABELS[type] ?: type,
                        onValueChange = {},
                        label = "Loại",
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuOpen)
                        },
                        modifier = Modifier.menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = typeMenuOpen,
                        onDismissRequest = { typeMenuOpen = false },
                    ) {
                        DEVICE_TYPE_LABELS.forEach { (key, text) ->
                            DropdownMenuItem(
                                text = { Text(text) },
                                onClick = {
                                    type = key
                                    typeMenuOpen = false
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                SectionTitle("Công suất")
                EntityField(
                    label = "Sensor công suất (tuỳ chọn)",
                    value = powerEntity,
                    onOpen = { pickerFor = "power" },
                )
            }
        }
        // Picker hiện dạng overlay full-screen (tương đương NavigationStack push bên iOS).
        when (pickerFor) {
            "device" -> EntityPickerScreen(
                title = "Chọn thiết bị",
                domains = listOf("light", "switch", "fan", "climate", "cover", "lock"),
                entities = entities,
                exclude = usedDeviceEntities,
                initialSelection = entityId,
                searchPrompt = "Tìm thiết bị",
                onSelect = { id ->
                    entityId = id
                    if (id.isNotEmpty()) {
                        // Tự động nhận diện loại theo domain.
                        type = detectDeviceType(id)
                        // Tự điền tên hiển thị nếu đang trống.
                        if (name.isBlank()) {
                            name = entityMap[id]?.friendlyName ?: id
                        }
                    }
                    pickerFor = null
                },
                onDismiss = { pickerFor = null },
            )
            "power" -> EntityPickerScreen(
                title = "Chọn sensor",
                domains = listOf("sensor"),
                entities = entities,
                exclude = usedPowerEntities,
                initialSelection = powerEntity,
                searchPrompt = "Tìm sensor",
                onSelect = {
                    powerEntity = it
                    pickerFor = null
                },
                onDismiss = { pickerFor = null },
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

// ---------------------------------------------------------------------------
// EntityField — port iOS SensorField
// ---------------------------------------------------------------------------

/**
 * Ô chọn entity/sensor: bấm để mở picker thay vì gõ tay entity ID.
 * Port iOS `SensorField` (picker được hoist lên DeviceEditScreen qua [onOpen]).
 */
@Composable
fun EntityField(
    label: String,
    value: String,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(vertical = 8.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (value.isEmpty()) "Chạm để chọn" else value,
            style = MaterialTheme.typography.bodyLarge,
            color = if (value.isEmpty()) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
        )
    }
}

// ---------------------------------------------------------------------------
// EntityPickerScreen — port iOS SensorPickerView
// ---------------------------------------------------------------------------

/**
 * Picker chọn entity từ danh sách HA.
 *
 * @param domains lọc theo domain (prefix match: "light.", "switch.", ...).
 * @param exclude entity đã dùng ở chỗ khác -> ẩn khỏi list (trừ lựa chọn hiện tại).
 * @param initialSelection entity đang chọn ("" = chưa chọn) — hiện nút "Xoá lựa chọn".
 * @param onSelect callback khi bấm chọn (kể cả xoá -> "") — caller tự đóng đã xử lý ở đây.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntityPickerScreen(
    title: String,
    domains: List<String>,
    entities: StateFlow<Map<String, PickerEntity>>,
    exclude: Set<String> = emptySet(),
    initialSelection: String = "",
    searchPrompt: String = "Tìm kiếm",
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val all by entities.collectAsState()
    var searchText by remember { mutableStateOf("") }
    val shown = remember(all, domains, exclude, initialSelection, searchText) {
        val q = searchText.trim().lowercase()
        all.values
            .filter { e -> domains.any { d -> e.id.startsWith("$d.") } }
            .filter { e -> e.id == initialSelection || e.id !in exclude }
            .filter { e ->
                q.isEmpty() ||
                    e.id.lowercase().contains(q) ||
                    e.friendlyName.lowercase().contains(q)
            }
            .sortedBy { it.friendlyName.lowercase() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    TextButton(onClick = onDismiss) { Text("Đóng") }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            M3ETextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = searchPrompt,
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                if (initialSelection.isNotEmpty()) {
                    item {
                        Text(
                            "Xoá lựa chọn",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect("")
                                    onDismiss()
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                        )
                        HorizontalDivider()
                    }
                }
                items(shown, key = { it.id }) { e ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(e.id)
                                onDismiss()
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                e.friendlyName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                e.id,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (e.id == initialSelection) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}
