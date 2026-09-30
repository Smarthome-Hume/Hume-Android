package com.smarthome.hume.feature.me

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.datastore.FabActionConfig
import com.smarthome.hume.core.datastore.FabFunction
import com.smarthome.hume.core.datastore.FabMenuStore
import com.smarthome.hume.core.ui.components.M3EIcons
import com.smarthome.hume.core.ui.components.M3ETextField
import com.smarthome.hume.core.ui.components.MsIcon
import kotlinx.coroutines.launch

/**
 * The "FAB menu" trong tab Toi: nhan vao mo trang tuy chinh cho tung chuc nang
 * cua FAB menu (nhap entity + chon trang thai).
 */
@Composable
fun FabMenuCard(
    store: FabMenuStore,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    var showSheet by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier
            .clip(RoundedCornerShape(28.dp))
            .background(cs.surfaceContainerHigh)
            .clickable(interactionSource = interaction, indication = null) { showSheet = true }
            .padding(20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(cs.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                MsIcon(M3EIcons.Fab, null, tint = cs.onPrimaryContainer, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "FAB menu",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface,
                )
            }
            MsIcon(M3EIcons.ChevronRight, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }

    if (showSheet) {
        FabMenuConfigSheet(store = store, onDismiss = { showSheet = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FabMenuConfigSheet(
    store: FabMenuStore,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 44.dp, topEnd = 44.dp),
        containerColor = cs.surfaceContainerHigh,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                "FAB menu",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = cs.onSurface,
            )
            Text(
                "Nhập entity và chọn trạng thái cho từng chức năng.",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
            )
            FabFunction.entries.forEach { func ->
                FabFunctionConfigRow(func = func, store = store)
            }
            // Nut Dong
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(cs.primary)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Xong",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = cs.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun FabFunctionConfigRow(
    func: FabFunction,
    store: FabMenuStore,
) {
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val config by store.configFlow(func).collectAsState(initial = FabActionConfig())
    var entityId by remember(config) { mutableStateOf(config.entityId) }
    var targetState by remember(config) { mutableStateOf(config.targetState) }
    var dirty by remember { mutableStateOf(false) }

    fun doSave() {
        scope.launch {
            store.saveConfig(func, FabActionConfig(entityId.trim(), targetState))
            dirty = false
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cs.surfaceContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            func.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = cs.onSurface,
        )
        // Entity ID
        M3ETextField(
            value = entityId,
            onValueChange = { entityId = it; dirty = true },
            label = "Entity ID",
            placeholder = "vd: light.phong_khach",
            modifier = Modifier.fillMaxWidth(),
        )
        // Trang thai + nut Luu
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StateDropdown(
                value = targetState,
                onSelect = { targetState = it; dirty = true },
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier
                    .width(72.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (dirty) cs.primary else cs.surfaceContainerHigh)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = ::doSave,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Lưu",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (dirty) cs.onPrimary else cs.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StateDropdown(
    value: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf("on", "off", "toggle")
    val labels = mapOf("on" to "Bật", "off" to "Tắt", "toggle" to "Đảo trạng thái")
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        M3ETextField(
            value = labels[value] ?: value,
            onValueChange = {},
            readOnly = true,
            label = "Trạng thái",
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { opt ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(labels[opt] ?: opt, maxLines = 1) },
                    onClick = { expanded = false; onSelect(opt) },
                )
            }
        }
    }
}
