package com.smarthome.hume.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smarthome.hume.core.model.DeviceUi
import com.smarthome.hume.core.ui.components.Ms
import com.smarthome.hume.core.ui.components.MsIcon
import com.smarthome.hume.core.ui.theme.M3ETokens

/** Sheet den dang sang — the noi giong RoomSheet/PackSheet (fix 3d): container M3 trong suot + RectangleShape, the cach deu 3 canh 8dp, bo 20dp ca 4 goc, top gap 183dp. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LightsSheet(
    lights: List<DeviceUi>,
    onDismiss: () -> Unit,
    onToggle: (String) -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    // The noi giong RoomSheet (fix 3d): top gap 183dp, the cao = screenH - 183 - 8, cach deu 3 canh 8dp.
    val screenH = LocalConfiguration.current.screenHeightDp.dp
    val sheetMargin = 8.dp
    val topGap = 183.dp
    val cardH = screenH - topGap - sheetMargin
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        // (fix 3d, nhu RoomSheet) Container M3 TRONG SUOT + hinh chu nhat:
        // the NHIN THAY ve BEN TRONG content. Scrim + vuot dong cua M3 giu nguyen.
        shape = RectangleShape,
        containerColor = Color.Transparent,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        // Khong dung dragHandle slot cua M3: GrabHandle ve trong the (can giua o dinh).
        dragHandle = null,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .height(screenH - topGap),
        ) {
            // The noi: rong = screenW - 2*8, cao = screenH - 183 - 8,
            // bo 20dp CA 4 GOC, nen surfaceContainer.
            Box(
                Modifier
                    .padding(horizontal = sheetMargin)
                    .fillMaxWidth()
                    .height(cardH)
                    .clip(RoundedCornerShape(M3ETokens.Corner.SheetInner))
                    .background(cs.surfaceContainer),
            ) {
                Column(Modifier.fillMaxSize()) {
                    Box(
                        Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        SheetGrabHandle()
                    }
                    LazyColumn(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .weight(1f),
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
                                    // Chuan hoa 18/11: title sheet (LightsSheet)
                                    // -> SheetTitle 24/29,3 (giu 24sp, chi
                                    // LH 32 -> 29,3).
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontSize = M3ETokens.Text.SheetTitle.fontSize,
                                        lineHeight = M3ETokens.Text.SheetTitle.lineHeight,
                                    ),
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
            // Spacer TRONG SUOT duoi the: day the cach mep duoi dung 8dp.
            Spacer(Modifier.height(sheetMargin))
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


