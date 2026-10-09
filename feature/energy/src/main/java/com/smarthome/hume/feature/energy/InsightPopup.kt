package com.smarthome.hume.feature.energy

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.model.BatteryInfo
import com.smarthome.hume.core.model.EnergyCost
import com.smarthome.hume.core.model.EntityToggleState
import com.smarthome.hume.core.ui.components.M3EIcons
import com.smarthome.hume.core.ui.components.Ms
import com.smarthome.hume.core.ui.components.MsIcon
import com.smarthome.hume.core.ui.components.WavyBatteryBar
import com.smarthome.hume.core.ui.components.pressMorph
import com.smarthome.hume.core.ui.components.rememberHaptic
import com.smarthome.hume.core.ui.theme.LocalHumeExtraColors
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Popup TẠI CHỖ cho action của gợi ý năng lượng (mục 14).
 * Port từ iOS `InsightPopup.swift` (pattern overlay giống CameraOverlayView):
 * - Nền: fullscreen dim, tap để đóng
 * - Card: giữa màn hình, scale spring 0.3 → 1.0 (iOS: spring response 0.35, damping 0.8)
 *
 * Hiệu năng 120Hz: dùng AnimatedVisibility (đồng bộ vsync, transform trên
 * graphicsLayer — không recompose mỗi frame). Không dùng delay cho animation.
 */
/**
 * Popup TẠI CHỖ cho action của gợi ý năng lượng (mục 14).
 * Port từ iOS `InsightPopup.swift` (pattern overlay giống CameraOverlayView):
 * - Nền: fullscreen dim, tap để đóng
 * - Card: giữa màn hình, PHONG RA TỪ VỊ TRÍ NÚT BẤM (port iOS 58a3542,
 *   không dùng scaleEffect)
 *
 * Hiệu năng 120Hz: dùng Animatable (đồng bộ vsync, không recompose mỗi frame).
 */
@Composable
fun InsightPopupOverlay(
    popup: InsightPopup?,
    battery: BatteryInfo,
    toggleStates: Map<String, EntityToggleState>,
    onToggle: (entityId: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Rect (dp, toa do root) cua nut bam de popup phong ra tu do
     * (port iOS 58a3542). Null = phong tu diem giua.
     */
    sourceRectDp: androidx.compose.ui.geometry.Rect? = null,
    cost: com.smarthome.hume.core.model.EnergyCost? = null,
) {
    // Giữ popup trong composition suốt exit animation rồi mới null (cleanup state,
    // không phải animation từng frame nên delay ngắn ở đây là chấp nhận được).
    var visible by remember(popup) { mutableStateOf(popup != null) }
    LaunchedEffect(visible) {
        if (!visible && popup != null) {
            delay(280)
            onDismiss()
        }
    }
    fun dismiss() {
        visible = false
    }

    com.smarthome.hume.core.ui.components.M3EPopupOverlay(
        visible = visible,
        onDismissRequest = ::dismiss,
        modifier = modifier,
        scrimAlpha = 0.55f,
        sourceRectDp = sourceRectDp,
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                // Viền kính mờ như iOS (.stroke white 50%)
                .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                // Chặn tap xuyên qua thẻ làm đóng popup
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            when (popup) {
                is InsightPopup.Device -> InsightDeviceCard(
                    popup = popup,
                    toggleStates = toggleStates,
                    onToggle = onToggle,
                    onClose = ::dismiss,
                )
                InsightPopup.Battery -> InsightBatteryCard(
                    battery = battery,
                    onClose = ::dismiss,
                )
                InsightPopup.CostDetail -> cost?.let {
                    InsightCostCard(
                        cost = it,
                        onClose = ::dismiss,
                    )
                }
                null -> {}
            }
        }
    }
}

/** Card thiết bị: tắt/bật nhanh + biểu đồ (port iOS InsightDeviceCard). */
@Composable
private fun InsightDeviceCard(
    popup: InsightPopup.Device,
    toggleStates: Map<String, EntityToggleState>,
    onToggle: (entityId: String) -> Unit,
    onClose: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val haptic = rememberHaptic()
    // Trạng thái thực từ HA; đảo lạc quan sau khi bấm (giống iOS đọc ha.state trực tiếp)
    var isOn by remember(popup.entityId, toggleStates[popup.entityId]?.isOn) {
        mutableStateOf(toggleStates[popup.entityId]?.isOn ?: false)
    }

    Column(Modifier.padding(20.dp)) {
        // Header + nút đóng
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                popup.label,
                style = MaterialTheme.typography.headlineSmall,
                color = cs.onSurface,
                modifier = Modifier.weight(1f),
            )
            MsIcon(
                Ms.close, "Đóng",
                tint = cs.onSurfaceVariant,
                modifier = Modifier
                    .size(24.dp)
                    .pressMorph(pressedScale = 0.85f, onClick = onClose),
            )
        }
        Spacer(Modifier.height(16.dp))
        // Trạng thái
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(cs.surfaceContainer)
                .padding(16.dp),
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (isOn) LocalHumeExtraColors.current.success
                        else Color.Gray,
                    ),
            )
            Spacer(Modifier.size(10.dp))
            Text(
                if (isOn) "Đang bật" else "Đang tắt",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurface,
            )
        }
        Spacer(Modifier.height(16.dp))
        // Nút tắt/bật nhanh
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isOn) cs.error else cs.primary)
                .pressMorph(pressedScale = 0.96f) {
                    haptic()
                    onToggle(popup.entityId)
                    isOn = !isOn
                }
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            MsIcon(
                if (isOn) Ms.power_settings_new else Ms.bolt, null,
                tint = if (isOn) cs.onError else cs.onPrimary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text(
                if (isOn) "Tắt ngay" else "Bật lên",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (isOn) cs.onError else cs.onPrimary,
            )
        }
        Spacer(Modifier.height(16.dp))
        // Biểu đồ năng lượng (placeholder như iOS)
        Text(
            "Năng lượng hôm nay",
            style = MaterialTheme.typography.titleSmall,
            color = cs.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(cs.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Biểu đồ đang tải...",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
            )
        }
    }
}

/** Card pin: % pin + nút sạc nhanh (port iOS InsightBatteryCard). */
@Composable
private fun InsightBatteryCard(
    battery: BatteryInfo,
    onClose: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val soc = battery.soc.roundToInt().coerceIn(0, 100)
    val battW = battery.powerW

    Column(Modifier.padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Pin lưu trữ",
                style = MaterialTheme.typography.headlineSmall,
                color = cs.onSurface,
                modifier = Modifier.weight(1f),
            )
            MsIcon(
                Ms.close, "Đóng",
                tint = cs.onSurfaceVariant,
                modifier = Modifier
                    .size(24.dp)
                    .pressMorph(pressedScale = 0.85f, onClick = onClose),
            )
        }
        Spacer(Modifier.height(8.dp))
        // Số % lớn (iOS: 48pt bold)
        Text(
            "$soc%",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = cs.onSurface,
        )
        Text(
            when {
                battW > 0 -> "Đang sạc ${battW.toInt()}W"
                battW < 0 -> "Đang xả ${(-battW).toInt()}W"
                else -> "Đứng yên"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        WavyBatteryBar(
            soc = soc,
            reserveLimit = 20,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(16.dp))
        // Nút sạc nhanh (iOS: stub đóng popup)
        InsightPopupButton(
            label = "Sạc nhanh",
            glyph = Ms.bolt,
            primary = true,
            onClick = onClose,
        )
        Spacer(Modifier.height(10.dp))
        InsightPopupButton(
            label = "Dùng pin cho nhà",
            glyph = M3EIcons.Home,
            primary = false,
            onClick = onClose,
        )
    }
}

/** Nút trong popup insight (primary = nền primary, phụ = surfaceContainer). */
@Composable
private fun InsightPopupButton(
    label: String,
    glyph: String,
    primary: Boolean,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val haptic = rememberHaptic()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (primary) cs.primary else cs.surfaceContainer)
            .pressMorph(pressedScale = 0.96f) { haptic(); onClick() }
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        MsIcon(
            glyph, null,
            tint = if (primary) cs.onPrimary else cs.onSurface,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (primary) cs.onPrimary else cs.onSurface,
        )
    }
}

/**
 * Card chi tiet tiet kiem (port iOS 5f176f9 InsightCostCard):
 * so tiet kiem lon + chi phi thuc te + tien dien EVN.
 */
@Composable
private fun InsightCostCard(
    cost: EnergyCost,
    onClose: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val saving = cost.homeVnd - cost.gridVnd

    fun fmtVnd(v: Long): String {
        return v.toString().reversed().chunked(3).joinToString(".").reversed() + "đ"
    }

    Column(Modifier.padding(20.dp)) {
        // Header + nut dong
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Chi tiết tiết kiệm",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurface,
                modifier = Modifier.weight(1f),
            )
            MsIcon(
                Ms.close, null,
                tint = cs.onSurfaceVariant,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClose,
                    ),
            )
        }
        Spacer(Modifier.height(16.dp))
        // So tiet kiem lon
        Text(
            fmtVnd(saving),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2E7D32),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Text(
            "tháng này",
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(16.dp))
        // Chi tiet
        CostRow("Chi phí thực tế", fmtVnd(cost.homeVnd))
        Spacer(Modifier.height(12.dp))
        CostRow("Tiền điện phải trả EVN", fmtVnd(cost.gridVnd))
    }
}

@Composable
private fun CostRow(label: String, value: String) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = cs.onSurface,
        )
    }
}
