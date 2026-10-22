package com.smarthome.hume.feature.home

import androidx.compose.foundation.background
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import com.smarthome.hume.core.ui.components.M3ECardGap
import com.smarthome.hume.core.ui.components.M3EIcons
import com.smarthome.hume.core.ui.components.M3ESectionTitle
import com.smarthome.hume.core.ui.components.M3ETextField
import com.smarthome.hume.core.ui.components.Ms
import com.smarthome.hume.core.ui.components.MsIcon
import com.smarthome.hume.core.ui.components.pressMorph
import com.smarthome.hume.core.ui.components.riseIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CancellationException
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
    // (2026-10-14, audit #8): doi thu tu thiet bi trong phong
    suspend fun moveDevice(roomId: String, from: Int, to: Int)
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
    override suspend fun moveDevice(roomId: String, from: Int, to: Int) = mutate { list ->
        list.map { room ->
            if (room.id != roomId) room
            else {
                val devices = room.devices.toMutableList()
                if (from !in devices.indices || to !in devices.indices || from == to) room
                else {
                    val item = devices.removeAt(from)
                    devices.add(to, item)
                    room.copy(devices = devices)
                }
            }
        }
    }
}

// =====================================================================
// Khung M3E dung chung cho toan bo luong Quan ly phong:
// - Header: nut Back Box 40dp bo 14dp + pill tieu de primaryContainer
//   bo 28dp + pill "Them" (khi co).
// - Thanh Luu/Huy can day cho cac man edit.
// - Sheet xac nhan (khoi phuc / xoa) dang ModalBottomSheet bo tren 44dp.
// =====================================================================

/**
 * Header chuan M3E (thay khung Material3 mac dinh): Back 40dp bo 14dp
 * surfaceContainerHighest pressMorph(0.88); tieu de trong pill
 * primaryContainer bo 28dp, chu 18sp SemiBold onPrimaryContainer;
 * action Them (neu co) la pill primaryContainer bo 16dp + icon add.
 */
@Composable
fun RoomMgmtHeader(
    title: String,
    onBack: () -> Unit,
    onAdd: (() -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(cs.surfaceContainerHighest)
                .pressMorph(pressedScale = 0.88f, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            MsIcon(
                Ms.arrow_back,
                contentDescription = "Quay lại",
                modifier = Modifier.size(20.dp),
                tint = cs.onSurface,
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(28.dp))
                .background(cs.primaryContainer)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(
                title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = cs.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onAdd != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(cs.primaryContainer)
                    .pressMorph(pressedScale = 0.88f, onClick = onAdd)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MsIcon(
                    Ms.add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = cs.onPrimaryContainer,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Thêm",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onPrimaryContainer,
                )
            }
        }
    }
}

/**
 * Thanh Luu/Huy can day cho man edit: Huy (weight 1) + Luu (weight 2),
 * cao 52dp bo 20dp, chu bodyLarge SemiBold, pressMorph.
 */
@Composable
fun RoomMgmtSaveBar(
    saveEnabled: Boolean,
    saving: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(cs.surfaceContainerHighest)
                .pressMorph(pressedScale = 0.97f) { if (!saving) onCancel() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Huỷ",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurface,
            )
        }
        Box(
            modifier = Modifier
                .weight(2f)
                .height(52.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (saveEnabled) cs.primary else cs.surfaceContainerHigh)
                .pressMorph(pressedScale = 0.97f) { if (saveEnabled && !saving) onSave() },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Lưu",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (saveEnabled) cs.onPrimary else cs.onSurfaceVariant,
            )
        }
    }
}

/**
 * Sheet xac nhan chuan M3E (thay dialog mac dinh): ModalBottomSheet
 * surfaceContainerHighest, bo tren 44dp, nut dang pill nhu thanh Luu;
 * hanh dong pha huy dung mau error.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomMgmtConfirmSheet(
    title: String,
    message: String,
    confirmText: String,
    destructive: Boolean = true,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 44.dp, topEnd = 44.dp),
        containerColor = cs.surfaceContainerHighest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(cs.surfaceContainer)
                        .pressMorph(pressedScale = 0.97f, onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Huỷ",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onSurface,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (destructive) cs.error else cs.primary)
                        .pressMorph(pressedScale = 0.97f, onClick = onConfirm),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        confirmText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (destructive) cs.onError else cs.onPrimary,
                    )
                }
            }
        }
    }
}

/** Nut Sua nho trong the: Box 40dp bo 14dp primaryContainer + icon edit 20dp.
 * Cung he mau voi icon container dau the (user 2026-10-22: mau phai dong nhat). */
@Composable
private fun EditButton(onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(cs.primaryContainer)
            .pressMorph(pressedScale = 0.88f, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        MsIcon(
            Ms.edit,
            contentDescription = "Sửa",
            modifier = Modifier.size(20.dp),
            tint = cs.onPrimaryContainer,
        )
    }
}

/** Cum nut len/xuong gon trong the: icon 18dp trong container nho 28dp. */
@Composable
private fun ReorderButtons(
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(cs.primaryContainer)
                .pressMorph(pressedScale = 0.88f) { if (canMoveUp) onMoveUp() },
            contentAlignment = Alignment.Center,
        ) {
            MsIcon(
                Ms.keyboard_arrow_up,
                contentDescription = "Lên",
                modifier = Modifier.size(18.dp),
                tint = if (canMoveUp) cs.onPrimaryContainer
                else cs.onPrimaryContainer.copy(alpha = 0.3f),
            )
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(cs.primaryContainer)
                .pressMorph(pressedScale = 0.88f) { if (canMoveDown) onMoveDown() },
            contentAlignment = Alignment.Center,
        ) {
            MsIcon(
                Ms.keyboard_arrow_down,
                contentDescription = "Xuống",
                modifier = Modifier.size(18.dp),
                tint = if (canMoveDown) cs.onPrimaryContainer
                else cs.onPrimaryContainer.copy(alpha = 0.3f),
            )
        }
    }
}

/** Nen khi vuot xoa: the bo 28dp errorContainer + icon delete. */
@Composable
private fun SwipeDeleteBackground() {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp))
            .background(cs.errorContainer)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        MsIcon(
            Ms.delete,
            contentDescription = "Xoá",
            modifier = Modifier.size(24.dp),
            tint = cs.error,
        )
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
    pickerEntities: StateFlow<Map<String, PickerEntity>>? = null,
) {
    val cs = MaterialTheme.colorScheme
    val rooms by store.roomsFlow.collectAsState()
    val scope = rememberCoroutineScope()

    var showAddRoom by remember { mutableStateOf(false) }
    var editingRoom by remember { mutableStateOf<DynRoom?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var detailRoomId by remember { mutableStateOf<String?>(null) }
    var pendingDeleteRoom by remember { mutableStateOf<DynRoom?>(null) }
    // Tang key de tao lai swipe state sau khi huy xoa (hang tro ve cho nghi).
    var swipeResetKey by remember { mutableStateOf(0) }

    val detailId = detailRoomId
    if (detailId != null) {
        RoomDetailScreen(
            store = store,
            roomId = detailId,
            pickerEntities = pickerEntities,
            onBack = { detailRoomId = null },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background),
    ) {
        RoomMgmtHeader(
            title = "Quản lý phòng",
            onBack = onBack,
            onAdd = { showAddRoom = true },
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 18.dp, end = 18.dp, top = 6.dp, bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(M3ECardGap),
        ) {
            itemsIndexed(rooms, key = { _, room -> room.id }) { index, room ->
                key(swipeResetKey) {
                    val dismissState = rememberSwipeToDismissBoxState()
                    LaunchedEffect(dismissState.currentValue) {
                        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                            pendingDeleteRoom = room
                        }
                    }
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = { SwipeDeleteBackground() },
                    ) {
                        RoomRow(
                            room = room,
                            canMoveUp = index > 0,
                            canMoveDown = index < rooms.size - 1,
                            onMoveUp = { scope.launch { store.moveRoom(index, index - 1) } },
                            onMoveDown = { scope.launch { store.moveRoom(index, index + 1) } },
                            onEdit = { editingRoom = room },
                            onClick = { detailRoomId = room.id },
                            entranceDelay = index * 60,
                        )
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .riseIn(rooms.size * 60)
                        .clip(RoundedCornerShape(28.dp))
                        .background(cs.errorContainer)
                        .pressMorph(pressedScale = 0.97f) { showResetConfirm = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Khôi phục mặc định",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onErrorContainer,
                    )
                }
            }
        }
    }

    if (showAddRoom) {
        FullScreenDialog(onDismiss = { showAddRoom = false }) {
            RoomEditScreen(store = store, room = null, pickerEntities = pickerEntities, onDismiss = { showAddRoom = false })
        }
    }
    val roomToEdit = editingRoom
    if (roomToEdit != null) {
        FullScreenDialog(onDismiss = { editingRoom = null }) {
            RoomEditScreen(store = store, room = roomToEdit, pickerEntities = pickerEntities, onDismiss = { editingRoom = null })
        }
    }
    if (showResetConfirm) {
        RoomMgmtConfirmSheet(
            title = "Khôi phục mặc định?",
            message = "Mọi thay đổi về phòng và thiết bị sẽ mất. Tiếp tục?",
            confirmText = "Khôi phục",
            onConfirm = {
                showResetConfirm = false
                scope.launch { store.resetToDefaults() }
            },
            onDismiss = { showResetConfirm = false },
        )
    }
    val roomToDelete = pendingDeleteRoom
    if (roomToDelete != null) {
        RoomMgmtConfirmSheet(
            title = "Xoá phòng?",
            message = "Xoá phòng “${roomToDelete.name}” và toàn bộ thiết bị trong phòng?",
            confirmText = "Xoá",
            onConfirm = {
                pendingDeleteRoom = null
                swipeResetKey++
                scope.launch { store.deleteRoom(roomToDelete.id) }
            },
            onDismiss = {
                pendingDeleteRoom = null
                swipeResetKey++
            },
        )
    }
}

/**
 * RoomRow theo nguyen tac M3E Hume:
 * - The 28dp surfaceContainerHighest, padding 16x14dp, khong shadow
 * - Icon 48dp bo 16dp primaryContainer, nam giua icon-background
 * - Reorder gom trong the, nut Sua dang Box 40dp
 * - Entrance animation staggered (riseIn)
 */
@Composable
private fun RoomRow(
    room: DynRoom,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onClick: () -> Unit,
    entranceDelay: Int = 0,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .riseIn(entranceDelay)
            .clip(RoundedCornerShape(28.dp))
            .background(cs.surfaceContainerHighest)
            .pressMorph(pressedScale = 0.97f, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(cs.primaryContainer),
        ) {
            MsIcon(
                M3EIcons.room(room.icon),
                null,
                modifier = Modifier.size(26.dp),
                tint = cs.onPrimaryContainer,
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                room.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = cs.onSurface,
            )
            Text(
                "${room.devices.size} thiết bị",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (canMoveUp || canMoveDown) {
            ReorderButtons(
                canMoveUp = canMoveUp,
                canMoveDown = canMoveDown,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
            )
            Spacer(Modifier.width(8.dp))
        }
        EditButton(onClick = onEdit)
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
    pickerEntities: StateFlow<Map<String, PickerEntity>>? = null,
    onBack: () -> Unit = {},
) {
    val cs = MaterialTheme.colorScheme
    val rooms by store.roomsFlow.collectAsState()
    val scope = rememberCoroutineScope()
    // Port iOS liveRoom: doc lai tu store moi lan render.
    val room = rooms.firstOrNull { it.id == roomId }

    var showAddDevice by remember { mutableStateOf(false) }
    var editingDevice by remember { mutableStateOf<DynDevice?>(null) }
    var pendingDeleteDevice by remember { mutableStateOf<DynDevice?>(null) }
    var swipeResetKey by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background),
    ) {
        RoomMgmtHeader(
            title = room?.name ?: "Phòng",
            onBack = onBack,
            onAdd = { showAddDevice = true },
        )
        val devices = room?.devices.orEmpty()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 18.dp, end = 18.dp, top = 6.dp, bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(M3ECardGap),
        ) {
            item {
                M3ESectionTitle("Thiết bị (${devices.size})")
            }
            itemsIndexed(devices, key = { _, d -> d.id }) { index, device ->
                key(swipeResetKey) {
                    val dismissState = rememberSwipeToDismissBoxState()
                    LaunchedEffect(dismissState.currentValue) {
                        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                            pendingDeleteDevice = device
                        }
                    }
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = { SwipeDeleteBackground() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        DeviceRow(
                            device = device,
                            canMoveUp = index > 0,
                            canMoveDown = index < devices.size - 1,
                            onMoveUp = {
                                scope.launch { store.moveDevice(roomId, index, index - 1) }
                            },
                            onMoveDown = {
                                scope.launch { store.moveDevice(roomId, index, index + 1) }
                            },
                            onEdit = { editingDevice = device },
                            entranceDelay = index * 50,
                        )
                    }
                }
            }
        }
    }

    if (showAddDevice) {
        FullScreenDialog(onDismiss = { showAddDevice = false }) {
            RoomDeviceEditScreen(store = store, roomId = roomId, device = null, pickerEntities = pickerEntities, onDismiss = { showAddDevice = false })
        }
    }
    val deviceToEdit = editingDevice
    if (deviceToEdit != null) {
        FullScreenDialog(onDismiss = { editingDevice = null }) {
            RoomDeviceEditScreen(store = store, roomId = roomId, device = deviceToEdit, pickerEntities = pickerEntities, onDismiss = { editingDevice = null })
        }
    }
    val deviceToDelete = pendingDeleteDevice
    if (deviceToDelete != null) {
        RoomMgmtConfirmSheet(
            title = "Xoá thiết bị?",
            message = "Xoá thiết bị “${deviceToDelete.name.ifBlank { deviceToDelete.entityId }}” khỏi phòng?",
            confirmText = "Xoá",
            onConfirm = {
                pendingDeleteDevice = null
                swipeResetKey++
                scope.launch { store.deleteDevice(roomId, deviceToDelete.id) }
            },
            onDismiss = {
                pendingDeleteDevice = null
                swipeResetKey++
            },
        )
    }
}

/**
 * DeviceRow theo M3E: the 28dp surfaceContainerHighest, icon 48dp bo 16dp
 * surfaceContainer/onSurfaceVariant, reorder gom trong the, nut Sua Box 40dp.
 */
@Composable
private fun DeviceRow(
    device: DynDevice,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    entranceDelay: Int = 0,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .riseIn(entranceDelay)
            .clip(RoundedCornerShape(28.dp))
            .background(cs.surfaceContainerHighest)
            .pressMorph(pressedScale = 0.97f, onClick = onEdit)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(cs.surfaceContainer),
        ) {
            MsIcon(
                deviceGlyph(device),
                null,
                modifier = Modifier.size(24.dp),
                tint = cs.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                device.name.ifBlank { device.entityId },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = cs.onSurface,
            )
            if (device.entityId.isNotBlank()) {
                Text(
                    device.entityId,
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        if (canMoveUp || canMoveDown) {
            ReorderButtons(
                canMoveUp = canMoveUp,
                canMoveDown = canMoveDown,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
            )
            Spacer(Modifier.width(8.dp))
        }
        EditButton(onClick = onEdit)
    }
}

/** Icon hien thi theo loai thiet bi (cho den khi IconPicker that co). */
private fun deviceGlyph(device: DynDevice): Any = when (device.type) {
    "light" -> Ms.lightbulb
    "climate" -> M3EIcons.Climate
    "fan" -> M3EIcons.FanFa
    "cover" -> M3EIcons.Door
    "lock" -> M3EIcons.Lock
    else -> M3EIcons.Plug
}

/**
 * Field chon sensor/entity: UI dung [PickerField] chuan, bam mo
 * EntityPickerScreen (port iOS SensorField).
 */
@Composable
private fun SensorPickerField(
    label: String,
    entityId: String,
    onEntityChange: (String) -> Unit,
    pickerEntities: StateFlow<Map<String, PickerEntity>>?,
    exclude: Set<String> = emptySet(),
    domains: List<String> = listOf("sensor"),
) {
    var showPicker by remember { mutableStateOf(false) }
    val cs = MaterialTheme.colorScheme

    PickerField(
        label = label,
        value = entityId,
        onClick = { showPicker = true },
    )

    if (showPicker && pickerEntities != null) {
        Dialog(
            onDismissRequest = { showPicker = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(cs.background)
            ) {
                EntityPickerScreen(
                    title = label,
                    domains = domains,
                    entities = pickerEntities,
                    exclude = exclude,
                    initialSelection = entityId,
                    onSelect = {
                        onEntityChange(it)
                        showPicker = false
                    },
                    onDismiss = { showPicker = false },
                )
            }
        }
    }
}

// =====================================================================
// RoomEditScreen — them/sua phong (port iOS RoomEditView)
// =====================================================================

@Composable
fun RoomEditScreen(
    store: RoomStore,
    room: DynRoom?,
    pickerEntities: StateFlow<Map<String, PickerEntity>>? = null,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val rooms by store.roomsFlow.collectAsState()
    val scope = rememberCoroutineScope()

    var saving by remember(room?.id) { mutableStateOf(false) }
    var saveError by remember(room?.id) { mutableStateOf<String?>(null) }
    var name by remember(room?.id) { mutableStateOf(room?.name ?: "") }
    var icon by remember(room?.id) { mutableStateOf(room?.icon ?: "home") }
    var tempEntity by remember(room?.id) { mutableStateOf(room?.tempEntity ?: "") }
    var humidityEntity by remember(room?.id) { mutableStateOf(room?.humidityEntity ?: "") }

    // Sensor nhiet/am da dung o phong khac → an khoi picker (port iOS usedSensorEntities).
    val usedSensorEntities: Set<String> = remember(rooms, room?.id) {
        buildSet {
            for (r in rooms) {
                if (r.id == room?.id) continue
                if (r.tempEntity.isNotBlank()) add(r.tempEntity)
                if (r.humidityEntity.isNotBlank()) add(r.humidityEntity)
            }
        }
    }

    fun save() {
        if (saving) return
        saving = true
        saveError = null
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
            try {
                if (room == null) store.addRoom(newRoom) else store.updateRoom(newRoom)
                onDismiss()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                saveError = "Không lưu được phòng. Vui lòng thử lại."
            } finally {
                saving = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background),
    ) {
        RoomMgmtHeader(
            title = if (room == null) "Thêm phòng" else "Sửa phòng",
            onBack = onDismiss,
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = 18.dp, end = 18.dp, top = 6.dp, bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(M3ECardGap),
        ) {
            saveError?.let { message ->
                item { Text(message, color = cs.error) }
            }
            item {
                M3ETextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Tên phòng",
                    placeholder = "Ví dụ: Phòng khách",
                )
            }
            item {
                Column {
                    M3ESectionTitle("Icon")
                    Spacer(Modifier.height(10.dp))
                    IconPicker(
                        selection = icon,
                        onSelectionChange = { icon = it },
                    )
                }
            }
            item {
                SensorPickerField(
                    label = "Sensor nhiệt độ",
                    entityId = tempEntity,
                    onEntityChange = { tempEntity = it },
                    pickerEntities = pickerEntities,
                    exclude = usedSensorEntities,
                )
            }
            item {
                SensorPickerField(
                    label = "Sensor độ ẩm",
                    entityId = humidityEntity,
                    onEntityChange = { humidityEntity = it },
                    pickerEntities = pickerEntities,
                    exclude = usedSensorEntities,
                )
            }
        }
        RoomMgmtSaveBar(
            saveEnabled = name.isNotBlank(),
            saving = saving,
            onCancel = onDismiss,
            onSave = { save() },
        )
    }
}

// =====================================================================
// DeviceEditScreen — them/sua thiet bi (port iOS DeviceEditView)
// =====================================================================

@Composable
private fun RoomDeviceEditScreen(
    store: RoomStore,
    roomId: String,
    device: DynDevice?,
    pickerEntities: StateFlow<Map<String, PickerEntity>>?,
    onDismiss: () -> Unit,
) {
    val rooms by store.roomsFlow.collectAsState()
    val fallback = remember { MutableStateFlow<Map<String, PickerEntity>>(emptyMap()) }
    val otherDevices = rooms.flatMap { it.devices }.filter { it.id != device?.id }
    DeviceEditScreen(
        initial = device,
        entities = pickerEntities ?: fallback,
        usedDeviceEntities = otherDevices.map { it.entityId }.toSet(),
        usedPowerEntities = otherDevices.mapNotNull { it.powerEntity }.toSet(),
        onSave = { updated ->
            if (device == null) store.addDevice(roomId, updated)
            else store.updateDevice(roomId, updated)
        },
        onDismiss = onDismiss,
    )
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
