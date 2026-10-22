package com.smarthome.hume.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.ui.components.M3ECardGap
import com.smarthome.hume.core.ui.components.M3ETextField
import com.smarthome.hume.core.ui.components.Ms
import com.smarthome.hume.core.ui.components.MsIcon
import com.smarthome.hume.core.ui.components.pressMorph
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
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
        // (2026-10-14, audit #5): them 3 loai
        "media_player" -> "media_player"
        "vacuum" -> "vacuum"
        "humidifier" -> "humidifier"
        else -> "toggle" // light, switch, ...
    }

/** Nhãn tiếng Việt cho chip chọn loại (user chọn lại được nếu auto-detect sai). */
val DEVICE_TYPE_LABELS: Map<String, String> = mapOf(
    "toggle" to "Công tắc/Đèn",
    "fan" to "Quạt",
    "climate" to "Điều hoà",
    "cover" to "Rèm/Mành",
    "lock" to "Khoá",
    // (2026-10-14, audit #5): them 3 loai nhu iOS
    "media_player" to "TV/Loa",
    "vacuum" to "Robot hút bụi",
    "humidifier" to "Máy tạo ẩm",
)

/** Domain mo trong picker thiet bi: du 9 domain khop iOS. */
private val DEVICE_PICKER_DOMAINS: List<String> = listOf(
    "light", "switch", "fan", "climate", "cover", "lock",
    "media_player", "vacuum", "humidifier",
)

// ---------------------------------------------------------------------------
// PickerField — o chon entity/sensor chuan M3E (gop SensorPickerField +
// EntityField cu): nhan Bold bodySmall ngoai-tren nhu M3ETextField, hop
// bo 20dp surfaceContainer vien outlineVariant 1dp, value bodyMedium,
// cuoi co Box 28dp bo 10dp + chevron_right 18dp.
// ---------------------------------------------------------------------------

/**
 * Ô chọn entity/sensor: bấm để mở picker thay vì gõ tay entity ID.
 * Port iOS `SensorField`.
 */
@Composable
fun PickerField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Chạm để chọn",
) {
    val cs = MaterialTheme.colorScheme
    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = cs.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cs.surfaceContainer)
                .border(
                    width = 1.dp,
                    color = cs.outlineVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(20.dp),
                )
                .pressMorph(pressedScale = 0.98f, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                value.ifBlank { placeholder },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (value.isBlank()) cs.onSurfaceVariant else cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(cs.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                MsIcon(
                    Ms.chevron_right,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = cs.onSurfaceVariant,
                )
            }
        }
    }
}

/** Header muc dang viet hoa: bodySmall SemiBold onSurfaceVariant, ls 0.4sp. */
@Composable
private fun MgmtSectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.4.sp,
        modifier = Modifier.padding(start = 4.dp),
    )
}

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
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeviceEditScreen(
    initial: DynDevice?,
    entities: StateFlow<Map<String, PickerEntity>>,
    usedDeviceEntities: Set<String> = emptySet(),
    usedPowerEntities: Set<String> = emptySet(),
    onSave: suspend (DynDevice) -> Unit,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var saving by remember(initial) { mutableStateOf(false) }
    var saveError by remember(initial) { mutableStateOf<String?>(null) }
    var entityId by remember(initial) { mutableStateOf(initial?.entityId ?: "") }
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var sub by remember(initial) { mutableStateOf(initial?.sub ?: "") }
    var icon by remember(initial) { mutableStateOf(initial?.icon ?: "lightbulb") }
    var type by remember(initial) { mutableStateOf(initial?.type ?: "toggle") }
    var powerEntity by remember(initial) { mutableStateOf(initial?.powerEntity ?: "") }
    var pickerFor by remember { mutableStateOf<String?>(null) } // null | "device" | "power"
    val entityMap by entities.collectAsState()

    val canSave = entityId.isNotBlank() && name.isNotBlank()

    fun save() {
        if (saving || !canSave) return
        saving = true
        saveError = null
        val base = initial ?: DynDevice(icon = "lightbulb")
        val updated = base.copy(
            type = type, entityId = entityId.trim(),
            name = name.trim(), sub = sub.trim(), icon = icon,
            powerEntity = powerEntity.trim().takeIf { it.isNotBlank() },
        )
        scope.launch {
            try {
                onSave(updated)
                onDismiss()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                saveError = "Không lưu được thiết bị. Vui lòng thử lại."
            } finally {
                saving = false
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(cs.background),
        ) {
            RoomMgmtHeader(
                title = if (initial == null) "Thêm thiết bị" else "Sửa thiết bị",
                onBack = onDismiss,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(M3ECardGap),
            ) {
                saveError?.let { Text(it, color = cs.error) }
                MgmtSectionHeader("THIẾT BỊ")
                PickerField(
                    label = "Thiết bị",
                    value = entityId,
                    onClick = { pickerFor = "device" },
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
                IconPicker(selection = icon, onSelectionChange = { icon = it })
                // Loai thiet bi: chips FlowRow thay dropdown (M3E).
                Column {
                    Text(
                        "Loại",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        DEVICE_TYPE_LABELS.forEach { (key, label) ->
                            val selected = type == key
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (selected) cs.secondaryContainer
                                        else cs.surfaceContainer,
                                    )
                                    .pressMorph(pressedScale = 0.94f) { type = key }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (selected) {
                                    MsIcon(
                                        Ms.check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = cs.onSecondaryContainer,
                                    )
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(
                                    label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (selected) cs.onSecondaryContainer
                                    else cs.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                MgmtSectionHeader("CÔNG SUẤT")
                PickerField(
                    label = "Sensor công suất (tuỳ chọn)",
                    value = powerEntity,
                    onClick = { pickerFor = "power" },
                )
                Spacer(Modifier.height(10.dp))
            }
            RoomMgmtSaveBar(
                saveEnabled = canSave,
                saving = saving,
                onCancel = onDismiss,
                onSave = { save() },
            )
        }
        // Picker hiện dạng overlay full-screen (tương đương NavigationStack push bên iOS).
        when (pickerFor) {
            "device" -> EntityPickerScreen(
                title = "Chọn thiết bị",
                domains = DEVICE_PICKER_DOMAINS,
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
    val cs = MaterialTheme.colorScheme
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background),
    ) {
        RoomMgmtHeader(title = title, onBack = onDismiss)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
        ) {
            M3ETextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = "Tìm kiếm",
                placeholder = searchPrompt,
                leadingIcon = {
                    MsIcon(
                        Ms.search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = cs.onSurfaceVariant,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(M3ECardGap))
            if (initialSelection.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(cs.errorContainer)
                        .pressMorph(pressedScale = 0.97f) {
                            onSelect("")
                            onDismiss()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MsIcon(
                            Ms.close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = cs.onErrorContainer,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Xoá lựa chọn",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = cs.onErrorContainer,
                        )
                    }
                }
                Spacer(Modifier.height(M3ECardGap))
            }
            // Danh sach trong 1 the 28dp; dong bo 20dp, dong dang chon
            // nen primaryContainer + check trong vong tron.
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(cs.surfaceContainerHighest)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                if (shown.isEmpty()) {
                    item {
                        Text(
                            "Không có entity phù hợp",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        )
                    }
                }
                items(shown, key = { it.id }) { e ->
                    val selected = e.id == initialSelection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) cs.primaryContainer else Color.Transparent)
                            .pressMorph(pressedScale = 0.98f) {
                                onSelect(e.id)
                                onDismiss()
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                e.friendlyName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = if (selected) cs.onPrimaryContainer else cs.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                e.id,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selected) {
                                    cs.onPrimaryContainer.copy(alpha = 0.7f)
                                } else {
                                    cs.onSurfaceVariant
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(cs.primary),
                                contentAlignment = Alignment.Center,
                            ) {
                                MsIcon(
                                    Ms.check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = cs.onPrimary,
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
