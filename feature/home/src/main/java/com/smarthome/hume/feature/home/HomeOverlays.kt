package com.smarthome.hume.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.model.DeviceKind
import com.smarthome.hume.core.model.DeviceUi
import com.smarthome.hume.core.model.HomeNotification
import com.smarthome.hume.core.ui.theme.LocalHumeExtraColors
import com.smarthome.hume.core.ui.components.M3EIcons
import com.smarthome.hume.core.ui.components.M3EMotion
import com.smarthome.hume.core.ui.components.pressMorph
import com.smarthome.hume.core.ui.components.rememberHaptic
import com.smarthome.hume.core.ui.components.Ms
import com.smarthome.hume.core.ui.components.MsIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Sheet thong bao dieu kien theo demo rev12 (.sheet + .nfeed/.nfi):
 * nen surfaceContainer, grab, sub "Cua, cam bien & thiet bi moi hoat dong";
 * feed surfaceHighest bo 30px padding 8px;
 * hang: icon tron 46px primaryContainer + tieu de 13.5px/700 + sub 12px/500;
 * vao: nfin (translateY(-10px) scale(.98), .5s emphasized).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSheet(
    notifications: List<HomeNotification>,
    onDismiss: () -> Unit,
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
                .padding(horizontal = 22.dp)
                .heightIn(max = maxSheetH),
        ) {
            item {
                Text(
                    "Thông báo",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = cs.onSurface,
                )
                Text(
                    "Cửa, cảm biến & thiết bị mới hoạt động",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                )
            }
            if (notifications.isEmpty()) {
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            MsIcon(
                                Ms.notifications_off, null,
                                tint = cs.onSurfaceVariant,
                                modifier = Modifier.size(40.dp),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Không có thông báo mới",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = cs.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                item {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(30.dp))
                            .background(LocalHumeExtraColors.current.surfaceHighest)
                            .padding(8.dp),
                    ) {
                        notifications.forEachIndexed { i, n ->
                            NotifRow(n, i)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

/** Palette icon thong bao theo JS demo (npool): door primary, motion tertiary,
 * light secondary, AC info, lock error, camera violet. */
private enum class NotifKind { Door, Motion, Light, Ac, Lock, Camera, Other }

private fun notifKind(n: HomeNotification): NotifKind {
    val t = n.title.lowercase()
    return when {
        "cửa" in t -> NotifKind.Door
        "chuyển động" in t -> NotifKind.Motion
        "đèn" in t || "ổ cắm" in t -> NotifKind.Light
        "điều hoà" in t -> NotifKind.Ac
        "khoá" in t -> NotifKind.Lock
        "camera" in t -> NotifKind.Camera
        else -> NotifKind.Other
    }
}

/** Icon thong bao theo npool cua demo: door_front / door_open phan biet. */
private fun notifIcon(n: HomeNotification): String {
    val t = n.title.lowercase()
    return when (notifKind(n)) {
        NotifKind.Door -> if ("cửa sổ" in t) Ms.door_open else M3EIcons.Door
        NotifKind.Motion -> M3EIcons.Motion
        NotifKind.Light -> M3EIcons.Light
        NotifKind.Ac -> M3EIcons.Climate
        NotifKind.Lock -> Ms.lock_open
        NotifKind.Camera -> M3EIcons.Videocam
        NotifKind.Other -> M3EIcons.Bell
    }
}

@Composable
private fun notifContainer(kind: NotifKind): Color {
    val cs = MaterialTheme.colorScheme
    val extra = LocalHumeExtraColors.current
    return when (kind) {
        NotifKind.Door -> cs.primaryContainer
        NotifKind.Motion -> cs.tertiaryContainer
        NotifKind.Light -> cs.secondaryContainer
        NotifKind.Ac -> extra.infoContainer
        NotifKind.Lock -> cs.errorContainer
        NotifKind.Camera -> extra.violetContainer
        NotifKind.Other -> cs.primaryContainer
    }
}

@Composable
private fun notifOnContainer(kind: NotifKind): Color {
    val cs = MaterialTheme.colorScheme
    val extra = LocalHumeExtraColors.current
    return when (kind) {
        NotifKind.Door -> cs.onPrimaryContainer
        NotifKind.Motion -> cs.onTertiaryContainer
        NotifKind.Light -> cs.onSecondaryContainer
        NotifKind.Ac -> extra.onInfoContainer
        NotifKind.Lock -> cs.onErrorContainer
        NotifKind.Camera -> extra.onVioletContainer
        NotifKind.Other -> cs.onPrimaryContainer
    }
}

@Composable
private fun NotifRow(n: HomeNotification, index: Int) {
    val cs = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val kind = notifKind(n)
    val container = notifContainer(kind)
    val onContainer = notifOnContainer(kind)
    var vis by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 60L)
        vis = true
    }
    AnimatedVisibility(
        visible = vis,
        enter = fadeIn(tween(500, easing = M3EMotion.emphasized)) +
            slideInVertically(
                animationSpec = tween(500, easing = M3EMotion.emphasized),
            ) { with(density) { (-10).dp.roundToPx() } } +
            scaleIn(
                animationSpec = tween(500, easing = M3EMotion.emphasized),
                initialScale = 0.98f,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .padding(horizontal = 10.dp, vertical = 12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(container),
            ) {
                MsIcon(
                    notifIcon(n), null,
                    tint = onContainer,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    n.title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface,
                )
                Text(
                    n.body,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (n.timeText.isNotBlank()) {
                Text(
                    n.timeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = cs.onSurfaceVariant,
                )
            }
        }
    }
}

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
                        M3EIcons.Light, null,
                        tint = cs.primary,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${lights.size} đèn đang sáng",
                        fontSize = 24.sp,
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
 * Tim kiem thiet bi theo demo rev12 (.searchview): truot tu duoi len
 * (translateY(100%), .45s emphasized), bo top 56px, padding 14px 16px 0;
 * thanh tim kiem tran: back + input + clear;
 * nhan "TIM GAN DAY" + chips (Dieu hoa/Den ngu/Rem cua), nhan "THIET BI".
 */
@Composable
fun DeviceSearchView(
    query: String,
    onQuery: (String) -> Unit,
    results: List<DeviceUi>,
    onToggle: (String) -> Unit,
    onBack: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val haptic = rememberHaptic()
    var vis by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { vis = true }
    fun close() {
        if (!vis) return
        vis = false
        scope.launch {
            delay(450)
            onBack()
        }
    }
    AnimatedVisibility(
        visible = vis,
        enter = slideInVertically(
            animationSpec = tween(450, easing = M3EMotion.emphasized),
        ) { it },
        exit = slideOutVertically(
            animationSpec = tween(450, easing = M3EMotion.emphasizedAcc),
        ) { it } + fadeOut(tween(300)),
    ) {
        Surface(
            color = cs.surface,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 56.dp, topEnd = 56.dp))
                    .background(cs.surface)
                    .padding(top = 14.dp, start = 16.dp, end = 16.dp),
            ) {
                // .svbar: back + input tran + clear
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 10.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .pressMorph(pressedScale = 0.88f) {
                                haptic()
                                close()
                            },
                    ) {
                        MsIcon(
                            Ms.arrow_back, "Quay lại",
                            tint = cs.onSurface,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQuery,
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = cs.onSurface,
                        ),
                        cursorBrush = SolidColor(cs.primary),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (query.isEmpty()) {
                                    Text(
                                        "Tìm thiết bị…",
                                        fontSize = 16.sp,
                                        color = cs.onSurfaceVariant,
                                    )
                                }
                                inner()
                            }
                        },
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .pressMorph(pressedScale = 0.88f) {
                                haptic()
                                onQuery("")
                            },
                    ) {
                        MsIcon(
                            Ms.close, "Xóa",
                            tint = cs.onSurfaceVariant,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                Text(
                    "TÌM GẦN ĐÂY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 8.dp),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 4.dp),
                ) {
                    listOf("Điều hoà", "Đèn", "Ổ cắm").forEach { chip ->
                        SearchChip(chip) {
                            haptic()
                            onQuery(chip)
                        }
                    }
                }
                Text(
                    "THIẾT BỊ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 8.dp),
                )
                if (results.isEmpty()) {
                    Text(
                        "Không tìm thấy thiết bị",
                        fontSize = 14.sp,
                        color = cs.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                    )
                } else {
                    // Phan loai: Dieu hoa / Den / O cam
                    val acList = results.filter { it.kind == DeviceKind.Climate || it.iconKey == "snowflake" }
                    val lightList = results.filter { it.iconKey in listOf("bulb", "lightbulb", "light") }
                    val outletList = results.filter { it.iconKey in listOf("plug", "switch", "outlet") }
                    val otherList = results.filter { d ->
                        d !in acList && d !in lightList && d !in outletList
                    }
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth(),
                    ) {
                        if (acList.isNotEmpty()) {
                            item {
                                Text(
                                    "ĐIỀU HÒA",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp,
                                    color = cs.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                                )
                            }
                            itemsIndexed(acList, key = { _, d -> d.entityId }) { _, d ->
                                DeviceRow(d, onToggle = { onToggle(d.entityId) })
                            }
                        }
                        if (lightList.isNotEmpty()) {
                            item {
                                Text(
                                    "ĐÈN",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp,
                                    color = cs.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
                                )
                            }
                            itemsIndexed(lightList, key = { _, d -> d.entityId }) { _, d ->
                                DeviceRow(d, onToggle = { onToggle(d.entityId) })
                            }
                        }
                        if (outletList.isNotEmpty()) {
                            item {
                                Text(
                                    "Ổ CẮM",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp,
                                    color = cs.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
                                )
                            }
                            itemsIndexed(outletList, key = { _, d -> d.entityId }) { _, d ->
                                DeviceRow(d, onToggle = { onToggle(d.entityId) })
                            }
                        }
                        if (otherList.isNotEmpty()) {
                            itemsIndexed(otherList, key = { _, d -> d.entityId }) { _, d ->
                                DeviceRow(d, onToggle = { onToggle(d.entityId) })
                            }
                        }
                    }
                }
            }
        }
    }
}

/** .svchip: vien 1px outline, nen trong suot, 13px/600, padding 9px 16px,
 *  bo 999px; :active nen secondaryContainer + bo 12px. */
@Composable
private fun SearchChip(label: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val radius by animateDpAsState(
        targetValue = if (pressed) 12.dp else 999.dp,
        animationSpec = tween(300, easing = M3EMotion.spring),
        label = "chipR",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(radius))
            .background(if (pressed) cs.secondaryContainer else Color.Transparent)
            .border(1.dp, cs.outline, RoundedCornerShape(radius))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = cs.onSurface,
        )
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
