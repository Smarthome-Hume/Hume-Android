package com.smarthome.hume.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smarthome.hume.core.model.DeviceUi
import com.smarthome.hume.core.ui.components.Ms
import com.smarthome.hume.core.ui.components.MsIcon

/** Sheet den dang sang. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LightsSheet(
    lights: List<DeviceUi>,
    onDismiss: () -> Unit,
    onToggle: (String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 44.dp, topEnd = 44.dp),
        containerColor = cs.surfaceContainer,
        dragHandle = { SheetGrabHandle() },
    ) {
        // Gioi han chieu cao sheet 85% man hinh (cach top 15% > 20px)
        val maxSheetH = (LocalConfiguration.current.screenHeightDp * 0.85f).dp
        LazyColumn(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .heightIn(max = maxSheetH),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MsIcon(
                        Ms.lightbulb, null,
                        tint = cs.primary,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${lights.size} đèn đang sáng",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Medium,
                        color = cs.onSurface,
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
            itemsIndexed(lights, key = { _, d -> d.entityId }) { _, d ->
                DeviceRow(d, onToggle = { onToggle(d.entityId) })
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

/**
 * Tay cam sheet theo demo (.grab): 44x5, bo 3px, mau outline,
 * margin 4px auto 16px. M3 khong co GrabHandle public.
 */
@Composable
private fun SheetGrabHandle() {
    Box(
        Modifier
            .padding(top = 4.dp, bottom = 16.dp)
            .size(44.dp, 5.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.outline),
    )
}


