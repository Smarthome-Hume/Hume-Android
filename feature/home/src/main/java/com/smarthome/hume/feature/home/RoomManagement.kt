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
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import java.util.UUID

// =====================================================================
// PLACEHOLDER — data models + RoomStore interface.
//
// RoomStore THAT da co o app module: `com.smarthome.hume.data.RoomStore`
// (commit b11eea6) nhung feature/home KHONG phu thuoc app module nen
// khong import duoc. Interface duoi day MIRROR chinh xac API cua store
// that (suspend CRUD, cung ten ham/thu tu param) de parent agent chi can
// doi `store: RoomStore` thanh store that + map DynRoom/DynDevice ->
// ManagedRoom/ManagedDevice (field giong nhau tung cai).
// TODO(agent RoomStore): dua RoomStore xuong core:data (hoac viet adapter)
// roi xoa placeholder nay.
// =====================================================================

/** Thiet bi trong 1 phong — mirror `ManagedDevice` (app module). */
data class DynDevice(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val entityId: String = "",
    val type: String = "toggle", // toggle | fan | climate | cover | lock
    val icon: String = "",
    val sub: String = "",
    val powerEntity: String? = null,
)

/** Phong — mirror `ManagedRoom` (app module). */
data class DynRoom(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val icon: String = "home",
    val tempEntity: String = "",
    val humidityEntity: String = "",
    val devices: List<DynDevice> = emptyList(),
    val sortOrder: Int = 0,
)

/** Mirror API `com.smarthome.hume.data.RoomStore` (suspend CRUD). */
interface RoomStore {
    val roomsFlow: StateFlow<List<DynRoom>>
    suspend fun addRoom(room: DynRoom)
    suspend fun updateRoom(room: DynRoom)
    suspend fun deleteRoom(roomId: String)
    suspend fun moveRoom(from: Int, to: Int)
    suspend fun resetToDefaults()
    suspend fun addDevice(roomId: String, device: DynDevice)
    suspend fun updateDevice(roomId: String, device: DynDevice)
    suspend fun deleteDevice(roomId: String, deviceId: String)
}

/** Stub in-memory de UI chay duoc truoc khi wire store that. */
class StubRoomStore : RoomStore {
    private val _rooms = MutableStateFlow(
        listOf(
            DynRoom(name = "Phòng khách", icon = "sofa", sortOrder = 0),
            DynRoom(name = "Phòng ngủ", icon = "bed", sortOrder = 1),
        )
    )
    override val roomsFlow: StateFlow<List<DynRoom>> = _rooms

    private fun mutate(f: (List<DynRoom>) -> List<DynRoom>) {
        _rooms.value = f(_rooms.value.sortedBy { it.sortOrder })
            .mapIndexed { i, r -> r.copy(sortOrder = i) }
    }

    override suspend fun addRoom(room: DynRoom) = mutate { it + room }
    override suspend fun updateRoom(room: DynRoom) =
        mutate { list -> list.map { if (it.id == room.id) room else it } }
    override suspend fun deleteRoom(roomId: String) = mutate { list -> list.filterNot { it.id == roomId } }
    override suspend fun moveRoom(from: Int, to: Int) = mutate { list ->
        if (from !in list.indices || to !in list.indices || from == to) return@mutate list
        list.toMutableList().also { it.add(to, it.removeAt(from)) }
    }
    override suspend fun resetToDefaults() = mutate {
        listOf(
            DynRoom(name = "Phòng khách", icon = "sofa"),
            DynRoom(name = "Phòng ngủ", icon = "bed"),
        )
    }
    override suspend fun addDevice(roomId: String, device: DynDevice) = mutate { list ->
        list.map { if (it.id == roomId) it.copy(devices = it.devices + device) else it }
    }
    override suspend fun updateDevice(roomId: String, device: DynDevice) = mutate { list ->
        list.map { room ->
            if (room.id == roomId) room.copy(devices = room.devices.map { if (it.id == device.id) device else it })
            else room
        }
    }
    override suspend fun deleteDevice(roomId: String, deviceId: String) = mutate { list ->
        list.map { room ->
            if (room.id == roomId) room.copy(devices = room.devices.filterNot { it.id == deviceId })
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
    val scope = rememberCoroutineScope()

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
                        scope.launch { store.deleteRoom(room.id) }
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
                        onMoveUp = { scope.launch { store.moveRoom(index, index - 1) } },
                        onMoveDown = { scope.launch { store.moveRoom(index, index + 1) } },
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
                    onClick = {
                        showResetConfirm = false
                        scope.launch { store.resetToDefaults() }
                    },
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
                room.name,
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
    val scope = rememberCoroutineScope()
    // Port iOS liveRoom: doc lai tu store moi lan render.
    val room = rooms.firstOrNull { it.id == roomId }

    var showAddDevice by remember { mutableStateOf(false) }
    var editingDevice by remember { mutableStateOf<DynDevice?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        room?.name ?: "Phòng",
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
                        scope.launch { store.deleteDevice(roomId, device.id) }
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
                device.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                device.entityId,
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
    val rooms by store.roomsFlow.collectAsState()
    val scope = rememberCoroutineScope()

    var name by remember(room?.id) { mutableStateOf(room?.name ?: "") }
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
    // Tam giu bien de khong unused cho den khi EntityPickerScreen that co.
    @Suppress("UNUSED_VARIABLE")
    val sensorsExcludedFromPicker = usedSensorEntities

    fun save() {
        val newRoom = DynRoom(
            id = room?.id ?: UUID.randomUUID().toString(),
            name = name.trim(),
            icon = icon.trim().ifBlank { "home" },
            tempEntity = tempEntity.trim(),
            humidityEntity = humidityEntity.trim(),
            devices = room?.devices.orEmpty(),
            sortOrder = room?.sortOrder ?: Int.MAX_VALUE,
        )
        scope.launch {
            if (room == null) store.addRoom(newRoom) else store.updateRoom(newRoom)
        }
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
                        enabled = name.isNotBlank(),
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
                    value = name,
                    onValueChange = { name = it },
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
private fun deviceTypeForEntity(entityId: String): String = when (entityId.substringBefore(".").trim().lowercase()) {
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
    val scope = rememberCoroutineScope()

    var entityId by remember(device?.id) { mutableStateOf(device?.entityId ?: "") }
    var name by remember(device?.id) { mutableStateOf(device?.name ?: "") }
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
        val newDevice = DynDevice(
            id = device?.id ?: UUID.randomUUID().toString(),
            name = name.trim(),
            entityId = entityId.trim(),
            type = type,
            icon = icon.trim(),
            sub = sub.trim(),
            powerEntity = powerEntity.trim().ifBlank { null },
        )
        scope.launch {
            if (device == null) store.addDevice(roomId, newDevice) else store.updateDevice(roomId, newDevice)
        }
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
                        enabled = entityId.isNotBlank() && name.isNotBlank(),
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
                    value = entityId,
                    onValueChange = {
                        entityId = it
                        // Tu dong chon loai theo domain (neu user chua chon tay)
                        if (!typeTouched) type = deviceTypeForEntity(it)
                    },
                    label = "Thiết bị (entity)",
                    placeholder = "vd: light.phong_khach — sẽ thay bằng EntityPickerScreen",
                )
            }
            item {
                M3ETextField(
                    value = name,
                    onValueChange = { name = it },
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
