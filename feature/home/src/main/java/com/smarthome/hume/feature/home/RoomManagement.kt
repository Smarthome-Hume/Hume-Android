package com.smarthome.hume.feature.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.smarthome.hume.core.ui.components.M3EIcons
import com.smarthome.hume.core.ui.components.M3ETextField
import com.smarthome.hume.core.ui.components.MsIcon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

// =====================================================================
// PLACEHOLDER — data models + RoomStore interface.
// TODO(agent RoomStore): thay bang implementation that (DataStore/UserDefaults),
// giu nguyen ten ham de RoomManagement.kt khong phai sua.
// =====================================================================

/** Thiet bi trong 1 phong (port iOS DeviceConfig + id). */
data class DynDevice(
    val id: String = UUID.randomUUID().toString(),
    val type: String = "toggle", // toggle | fan | climate | cover | lock
    val entity: String = "",
    val label: String = "",
    val sub: String = "",
    val icon: String = "",
    val powerEntity: String? = null,
)

/** Phong (port iOS RoomBubbleConfig + id). */
data class DynRoom(
    val id: String = UUID.randomUUID().toString(),
    val key: String = "",
    val label: String = "",
    val icon: String = "home",
    val tempEntity: String = "",
    val humidityEntity: String = "",
    val devices: List<DynDevice> = emptyList(),
)

/** Interface RoomStore — agent khac se implement that. */
interface RoomStore {
    val roomsFlow: StateFlow<List<DynRoom>>
    fun addRoom(room: DynRoom)
    fun updateRoom(room: DynRoom)
    fun deleteRoom(id: String)
    fun moveRoom(from: Int, to: Int)
    fun resetToDefaults()
    fun addDevice(device: DynDevice, toRoomId: String)
    fun updateDevice(device: DynDevice, inRoomId: String)
    fun deleteDevice(deviceId: String, fromRoomId: String)
}

/** Stub in-memory de UI chay duoc truoc khi RoomStore that co. */
class StubRoomStore : RoomStore {
    private val _rooms = MutableStateFlow(
        listOf(
            DynRoom(label = "Phòng khách", key = "Phòng<br>khách", icon = "sofa"),
            DynRoom(label = "Phòng ngủ", key = "Phòng<br>ngủ", icon = "bed"),
        )
    )
    override val roomsFlow: StateFlow<List<DynRoom>> = _rooms

    private fun mutate(f: (List<DynRoom>) -> List<DynRoom>) {
        _rooms.value = f(_rooms.value)
    }

    override fun addRoom(room: DynRoom) = mutate { it + room }
    override fun updateRoom(room: DynRoom) =
        mutate { list -> list.map { if (it.id == room.id) room else it } }
    override fun deleteRoom(id: String) = mutate { list -> list.filterNot { it.id == id } }
    override fun moveRoom(from: Int, to: Int) = mutate { list ->
        if (from !in list.indices || to !in list.indices || from == to) return@mutate list
        list.toMutableList().also { it.add(to, it.removeAt(from)) }
    }
    override fun resetToDefaults() = mutate {
        listOf(
            DynRoom(label = "Phòng khách", key = "Phòng<br>khách", icon = "sofa"),
            DynRoom(label = "Phòng ngủ", key = "Phòng<br>ngủ", icon = "bed"),
        )
    }
    override fun addDevice(device: DynDevice, toRoomId: String) = mutate { list ->
        list.map { if (it.id == toRoomId) it.copy(devices = it.devices + device) else it }
    }
    override fun updateDevice(device: DynDevice, inRoomId: String) = mutate { list ->
        list.map { room ->
            if (room.id == inRoomId) room.copy(devices = room.devices.map { if (it.id == device.id) device else it })
            else room
        }
    }
    override fun deleteDevice(deviceId: String, fromRoomId: String) = mutate { list ->
        list.map { room ->
            if (room.id == fromRoomId) room.copy(devices = room.devices.filterNot { it.id == deviceId })
            else room
        }
    }
}

// =====================================================================
// RoomManagementScreen — danh sach phong (port iOS RoomManagementView)
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomManagementScreen(
    store: RoomStore,
    onBack: () -> Unit = {},
) {
    val cs = MaterialTheme.colorScheme
    val rooms by store.roomsFlow.collectAsState()

    var showAddRoom by remember { mutableStateOf(false) }
    var editingRoom by remember { mutableStateOf<DynRoom?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var detailRoomId by remember { mutableStateOf<String?>(null) }

    val detailId = detailRoomId
    if (detailId != null) {
        RoomDetailScreen(
            store = store,
            roomId = detailId,
            onBack = { detailRoomId = null },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Quản lý phòng",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddRoom = true }) {
                        Icon(Icons.Outlined.Add, "Thêm phòng")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(rooms, key = { _, room -> room.id }) { index, room ->
                val dismissState = rememberSwipeToDismissBoxState()
                LaunchedEffect(dismissState.currentValue) {
                    if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                        store.deleteRoom(room.id)
                    }
                }
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(28.dp))
                                .background(cs.errorContainer)
                                .padding(horizontal = 24.dp),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            Icon(Icons.Outlined.Delete, "Xoá", tint = cs.error)
                        }
                    },
                ) {
                    RoomRow(
                        room = room,
                        canMoveUp = index > 0,
                        canMoveDown = index < rooms.size - 1,
                        onMoveUp = { store.moveRoom(index, index - 1) },
                        onMoveDown = { store.moveRoom(index, index + 1) },
                        onEdit = { editingRoom = room },
                        onClick = { detailRoomId = room.id },
                    )
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = { showResetConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "Khôi phục mặc định",
                        color = cs.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }

    if (showAddRoom) {
        FullScreenDialog(onDismiss = { showAddRoom = false }) {
            RoomEditScreen(store = store, room = null, onDismiss = { showAddRoom = false })
        }
    }
    val roomToEdit = editingRoom
    if (roomToEdit != null) {
        FullScreenDialog(onDismiss = { editingRoom = null }) {
            RoomEditScreen(store = store, room = roomToEdit, onDismiss = { editingRoom = null })
        }
    }
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Khôi phục?") },
            text = { Text("Mọi thay đổi sẽ mất. Tiếp tục?") },
            confirmButton = {
                TextButton(
                    onClick = { showResetConfirm = false; store.resetToDefaults() },
                ) {
                    Text("Khôi phục", color = cs.error, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Huỷ") }
            },
        )
    }
}

@Composable
private fun RoomRow(
    room: DynRoom,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(cs.surfaceContainerHighest)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MsIcon(
            M3EIcons.room(room.icon),
            null,
            modifier = Modifier.size(28.dp),
            tint = cs.primary,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                room.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${room.devices.size} thiết bị",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
            )
        }
        if (canMoveUp) {
            IconButton(onClick = onMoveUp, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.KeyboardArrowUp, "Lên", modifier = Modifier.size(20.dp))
            }
        }
        if (canMoveDown) {
            IconButton(onClick = onMoveDown, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.KeyboardArrowDown, "Xuống", modifier = Modifier.size(20.dp))
            }
        }
        IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Outlined.Edit, "Sửa", modifier = Modifier.size(20.dp))
        }
    }
}

// =====================================================================
// RoomDetailScreen — danh sach thiet bi cua phong (port iOS RoomDetailEditView)
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomDetailScreen(
    store: RoomStore,
    roomId: String,
    onBack: () -> Unit = {},
) {
    val cs = MaterialTheme.colorScheme
    val rooms by store.roomsFlow.collectAsState()
    val room = rooms.firstOrNull { it.id == roomId }

    var showAddDevice by remember { mutableStateOf(false) }
    var editingDevice by remember { mutableStateOf<DynDevice?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        room?.label ?: "Phòng",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDevice = true }) {
                        Icon(Icons.Outlined.Add, "Thêm thiết bị")
                    }
                },
            )
        },
    ) { padding ->
        val devices = room?.devices.orEmpty()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Thiết bị (${devices.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurfaceVariant,
                )
            }
            itemsIndexed(devices, key = { _, d -> d.id }) { _, device ->
                val dismissState = rememberSwipeToDismissBoxState()
                LaunchedEffect(dismissState.currentValue) {
                    if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                        store.deleteDevice(device.id, roomId)
                    }
                }
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(28.dp))
                                .background(cs.errorContainer)
                                .padding(horizontal = 24.dp),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            Icon(Icons.Outlined.Delete, "Xoá", tint = cs.error)
                        }
                    },
                ) {
                    DeviceRow(
                        device = device,
                        onEdit = { editingDevice = device },
                    )
                }
            }
        }
    }

    if (showAddDevice) {
        FullScreenDialog(onDismiss = { showAddDevice = false }) {
            DeviceEditScreen(store = store, roomId = roomId, device = null, onDismiss = { showAddDevice = false })
        }
    }
    val deviceToEdit = editingDevice
    if (deviceToEdit != null) {
        FullScreenDialog(onDismiss = { editingDevice = null }) {
            DeviceEditScreen(store = store, roomId = roomId, device = deviceToEdit, onDismiss = { editingDevice = null })
        }
    }
}

@Composable
private fun DeviceRow(
    device: DynDevice,
    onEdit: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(cs.surfaceContainerHighest)
            .clickable(onClick = onEdit)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MsIcon(
            deviceGlyph(device),
            null,
            modifier = Modifier.size(28.dp),
            tint = cs.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                device.label,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                device.entity,
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Outlined.Edit, "Sửa", modifier = Modifier.size(20.dp))
        }
    }
}

/** Icon hien thi theo loai thiet bi (cho den khi IconPicker that co). */
private fun deviceGlyph(device: DynDevice): Any = when (device.type) {
    "climate" -> M3EIcons.Climate
    "fan" -> M3EIcons.FanFa
    "cover" -> M3EIcons.Door
    "lock" -> M3EIcons.Lock
    else -> M3EIcons.Plug
}

// =====================================================================
// RoomEditScreen — them/sua phong (port iOS RoomEditView)
// =====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomEditScreen(
    store: RoomStore,
    room: DynRoom?,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val rooms by store.roomsFlow.collectAsState()

    var label by remember(room?.id) { mutableStateOf(room?.label ?: "") }
    // TODO(agent IconPicker): thay TextField bang IconPicker that.
    var icon by remember(room?.id) { mutableStateOf(room?.icon ?: "home") }
    // TODO(agent EntityPickerScreen): thay TextField bang EntityPickerScreen that.
    var tempEntity by remember(room?.id) { mutableStateOf(room?.tempEntity ?: "") }
    var humidityEntity by remember(room?.id) { mutableStateOf(room?.humidityEntity ?: "") }

    // Sensor nhiet/am da dung o phong khac → an khoi picker (port iOS usedSensorEntities).
    // TODO(agent EntityPickerScreen): truyen vao EntityPickerScreen qua param exclude.
    val usedSensorEntities: Set<String> = remember(rooms, room?.id) {
        buildSet {
            for (r in rooms) {
                if (r.id == room?.id) continue
                if (r.tempEntity.isNotBlank()) add(r.tempEntity)
                if (r.humidityEntity.isNotBlank()) add(r.humidityEntity)
            }
        }
    }
    // (tam thoi: chi dung de khoi warning unused khi chua co picker that)
    @Suppress("UNUSED_VARIABLE") val unusedSensorsForPicker = usedSensorEntities

    fun save() {
        val newRoom = DynRoom(
            id = room?.id ?: UUID.randomUUID().toString(),
            // Port iOS: key = label thay space bang <br>
            key = label.replace(" ", "<br>"),
            label = label.trim(),
            icon = icon.trim().ifBlank { "home" },
            tempEntity = tempEntity.trim(),
            humidityEntity = humidityEntity.trim(),
            devices = room?.devices.orEmpty(),
        )
        if (room == null) store.addRoom(newRoom) else store.updateRoom(newRoom)
        onDismiss()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (room == null) "Thêm phòng" else "Sửa phòng",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onDismiss) { Text("Huỷ") }
                },
                actions = {
                    TextButton(
                        onClick = { save() },
                        enabled = label.isNotBlank(),
                    ) {
                        Text("Lưu", fontWeight = FontWeight.SemiBold)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                M3ETextField(
                    value = label,
                    onValueChange = { label = it },
                    label = "Tên phòng",
                    placeholder = "Ví dụ: Phòng khách",
                )
            }
            item {
                M3ETextField(
                    value = icon,
                    onValueChange = { icon = it },
                    label = "Icon",
                    placeholder = "Tạm dùng TextField — sẽ thay bằng IconPicker",
                )
            }
            item {
                M3ETextField(
                    value = tempEntity,
                    onValueChange = { tempEntity = it },
                    label = "Sensor nhiệt độ",
                    placeholder = "Tạm dùng TextField — sẽ thay bằng EntityPickerScreen",
                )
            }
            item {
                M3ETextField(
                    value = humidityEntity,
                    onValueChange = { humidityEntity = it },
                    label = "Sensor độ ẩm",
                    placeholder = "Tạm dùng TextField — sẽ thay bằng EntityPickerScreen",
                )
            }
        }
    }
}

// =====================================================================
// DeviceEditScreen — them/sua thiet bi (port iOS DeviceEditView)
// =====================================================================

private val DeviceTypes = listOf(
    "toggle" to "Công tắc/Đèn",
    "fan" to "Quạt",
    "climate" to "Điều hoà",
    "cover" to "Rèm/Mành",
    "lock" to "Khoá",
)

/** Tu dong chon loai theo domain cua entity (port iOS onSelect). */
private fun deviceTypeForEntity(entity: String): String = when (entity.substringBefore(".").trim().lowercase()) {
    "climate" -> "climate"
    "cover" -> "cover"
    "lock" -> "lock"
    "fan" -> "fan"
    else -> "toggle"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceEditScreen(
    store: RoomStore,
    roomId: String,
    device: DynDevice?,
    onDismiss: () -> Unit,
) {
    var entity by remember(device?.id) { mutableStateOf(device?.entity ?: "") }
    var label by remember(device?.id) { mutableStateOf(device?.label ?: "") }
    var sub by remember(device?.id) { mutableStateOf(device?.sub ?: "") }
    // TODO(agent IconPicker): thay TextField bang IconPicker that.
    var icon by remember(device?.id) { mutableStateOf(device?.icon ?: "") }
    var type by remember(device?.id) { mutableStateOf(device?.type ?: "toggle") }
    // TODO(agent EntityPickerScreen): thay TextField bang EntityPickerScreen that.
    var powerEntity by remember(device?.id) { mutableStateOf(device?.powerEntity ?: "") }
    var typeTouched by remember(device?.id) { mutableStateOf(false) }

    val typeLabel = DeviceTypes.firstOrNull { it.first == type }?.second ?: type
    var typeMenuOpen by remember { mutableStateOf(false) }

    fun save() {
        val power = powerEntity.trim().ifBlank { null }
        val newDevice = DynDevice(
            id = device?.id ?: UUID.randomUUID().toString(),
            type = type,
            entity = entity.trim(),
            label = label.trim(),
            sub = sub.trim(),
            icon = icon.trim(),
            powerEntity = power,
        )
        if (device == null) store.addDevice(newDevice, roomId) else store.updateDevice(newDevice, roomId)
        onDismiss()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (device == null) "Thêm thiết bị" else "Sửa thiết bị",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onDismiss) { Text("Huỷ") }
                },
                actions = {
                    TextButton(
                        onClick = { save() },
                        enabled = entity.isNotBlank() && label.isNotBlank(),
                    ) {
                        Text("Lưu", fontWeight = FontWeight.SemiBold)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                M3ETextField(
                    value = entity,
                    onValueChange = {
                        entity = it
                        // Tu dong chon loai theo domain (neu user chua chon tay)
                        if (!typeTouched) type = deviceTypeForEntity(it)
                    },
                    label = "Thiết bị (entity)",
                    placeholder = "vd: light.phong_khach — sẽ thay bằng EntityPickerScreen",
                )
            }
            item {
                M3ETextField(
                    value = label,
                    onValueChange = { label = it },
                    label = "Tên hiển thị",
                )
            }
            item {
                M3ETextField(
                    value = sub,
                    onValueChange = { sub = it },
                    label = "Mô tả",
                )
            }
            item {
                M3ETextField(
                    value = icon,
                    onValueChange = { icon = it },
                    label = "Icon",
                    placeholder = "Tạm dùng TextField — sẽ thay bằng IconPicker",
                )
            }
            item {
                ExposedDropdownMenuBox(
                    expanded = typeMenuOpen,
                    onExpandedChange = { typeMenuOpen = it },
                ) {
                    M3ETextField(
                        value = typeLabel,
                        onValueChange = {},
                        label = "Loại",
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeMenuOpen) },
                        modifier = Modifier.menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = typeMenuOpen,
                        onDismissRequest = { typeMenuOpen = false },
                    ) {
                        for ((value, typeName) in DeviceTypes) {
                            DropdownMenuItem(
                                text = { Text(typeName) },
                                onClick = {
                                    type = value
                                    typeTouched = true
                                    typeMenuOpen = false
                                },
                                trailingIcon = if (value == type) {
                                    { MsIcon(M3EIcons.Check, null, modifier = Modifier.size(20.dp)) }
                                } else null,
                            )
                        }
                    }
                }
            }
            item {
                M3ETextField(
                    value = powerEntity,
                    onValueChange = { powerEntity = it },
                    label = "Sensor công suất (tuỳ chọn)",
                    placeholder = "Tạm dùng TextField — sẽ thay bằng EntityPickerScreen",
                )
            }
        }
    }
}

/** Dialog full-screen dung cho cac man hinh edit (port iOS .sheet). */
@Composable
private fun FullScreenDialog(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            content()
        }
    }
}
