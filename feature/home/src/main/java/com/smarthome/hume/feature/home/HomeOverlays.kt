package com.smarthome.hume.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.graphics.graphicsLayer
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
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Medium,
                    color = cs.onSurface,
                )
                Text(
                    "Cửa, cảm biến & thiết bị mới hoạt động",
                    style = MaterialTheme.typography.bodySmall,
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
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = cs.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                // User 2026-09-29: moi thong bao la 1 the rieng, khong gom
                // chung vao 1 the.
                itemsIndexed(notifications) { i, n ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .background(LocalHumeExtraColors.current.surfaceHighest)
                            .padding(8.dp),
                    ) {
                        NotifRow(n, i)
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
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
                // Chuong dung vector rong tu ve (2026-09-30); cac icon khac dung glyph.
                if (notifKind(n) == NotifKind.Other) {
                    androidx.compose.material3.Icon(
                        imageVector = M3EIcons.BellVector,
                        contentDescription = null,
                        tint = onContainer,
                        modifier = Modifier.size(24.dp),
                    )
                } else {
                    MsIcon(
                        notifIcon(n), null,
                        tint = onContainer,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    n.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface,
                )
                Text(
                    n.body,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (n.timeText.isNotBlank()) {
                Text(
                    n.timeText,
                    style = MaterialTheme.typography.labelSmall,
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
            // Header search nam DUOI dai mo top (2026-09-30): top inset =
            // status bar + 20dp, cach mep tren thiet bi ~20px, khong tran
            // len status bar; dai mo chi phu vung status bar phia tren.
            val statusBarTop = WindowInsets.statusBars.asPaddingValues()
                .calculateTopPadding()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 56.dp, topEnd = 56.dp))
                    .background(cs.surface)
                    .padding(top = statusBarTop + 20.dp, start = 16.dp, end = 16.dp),
            ) {
                // .svbar M3E (2026-09-30, user yeu cau): back + input + clear
                // boc chung trong 1 thanh tonal surfaceContainerHighest,
                // bo 28dp; nut back/X giu pressMorph scale .88.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(cs.surfaceContainerHighest)
                        .padding(horizontal = 4.dp, vertical = 4.dp),
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
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = cs.onSurface,
                        ),
                        cursorBrush = SolidColor(cs.primary),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (query.isEmpty()) {
                                    Text(
                                        "Tìm thiết bị…",
                                        style = MaterialTheme.typography.titleMedium,
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
                    style = MaterialTheme.typography.labelSmall,
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
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 8.dp),
                )
                if (results.isEmpty()) {
                    Text(
                        "Không tìm thấy thiết bị",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                    )
                } else {
                    // Phan loai theo DOMAIN entity + iconKey (2026-09-30):
                    // loc iconKey thuan lam den icon "sun"/"desk" rot vao
                    // otherList khong header, nhin nhu lan vao O cam.
                    // Dieu hoa: climate; Den: domain light (ke ca sun/desk);
                    // O cam: plug/outlet that; Cong tac: switch con lai.
                    fun domainOf(d: DeviceUi) = d.entityId.substringBefore('.')
                    fun isOutletId(id: String): Boolean {
                        val l = id.lowercase()
                        return listOf("plug", "outlet", "socket", "o_cam", "ocam")
                            .any { it in l }
                    }
                    val lightKeys = listOf("bulb", "lightbulb", "light", "sun", "desk")
                    val acList = results.filter {
                        it.kind == DeviceKind.Climate || domainOf(it) == "climate"
                    }
                    val lightList = results.filter { d ->
                        d !in acList &&
                            (domainOf(d) == "light" || d.iconKey in lightKeys)
                    }
                    val outletList = results.filter { d ->
                        d !in acList && d !in lightList &&
                            (d.iconKey == "plug" || d.iconKey == "outlet" ||
                                (domainOf(d) == "switch" && isOutletId(d.entityId)))
                    }
                    val switchList = results.filter { d ->
                        d !in acList && d !in lightList && d !in outletList &&
                            domainOf(d) == "switch"
                    }
                    val otherList = results.filter { d ->
                        d !in acList && d !in lightList && d !in outletList &&
                            d !in switchList
                    }
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        // Quy tac chung (2026-09-30, user): the cuoi cach navbar
                        // noi ~20px -> bottom 140.dp giong trang Nha. Truoc day
                        // thieu nen khong cuon het noi dung len tren duoc.
                        contentPadding = PaddingValues(bottom = 140.dp),
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth(),
                    ) {
                        if (acList.isNotEmpty()) {
                            item {
                                Text(
                                    "ĐIỀU HÒA",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp,
                                    color = cs.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                                )
                            }
                            itemsIndexed(acList, key = { _, d -> d.entityId }) { _, d ->
                                DeviceRow(
                                    d,
                                    onToggle = { onToggle(d.entityId) },
                                    roomLabel = d.sub,
                                )
                            }
                        }
                        if (lightList.isNotEmpty()) {
                            item {
                                Text(
                                    "ĐÈN",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp,
                                    color = cs.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
                                )
                            }
                            itemsIndexed(lightList, key = { _, d -> d.entityId }) { _, d ->
                                DeviceRow(
                                    d,
                                    onToggle = { onToggle(d.entityId) },
                                    roomLabel = d.sub,
                                )
                            }
                        }
                        if (outletList.isNotEmpty()) {
                            item {
                                Text(
                                    "Ổ CẮM",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp,
                                    color = cs.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
                                )
                            }
                            itemsIndexed(outletList, key = { _, d -> d.entityId }) { _, d ->
                                DeviceRow(
                                    d,
                                    onToggle = { onToggle(d.entityId) },
                                    roomLabel = d.sub,
                                )
                            }
                        }
                        if (switchList.isNotEmpty()) {
                            item {
                                Text(
                                    "CÔNG TẮC",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp,
                                    color = cs.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
                                )
                            }
                            itemsIndexed(switchList, key = { _, d -> d.entityId }) { _, d ->
                                DeviceRow(
                                    d,
                                    onToggle = { onToggle(d.entityId) },
                                    roomLabel = d.sub,
                                )
                            }
                        }
                        if (otherList.isNotEmpty()) {
                            itemsIndexed(otherList, key = { _, d -> d.entityId }) { _, d ->
                                DeviceRow(
                                    d,
                                    onToggle = { onToggle(d.entityId) },
                                    roomLabel = d.sub,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** .svchip M3E (2026-09-30, user yeu cau): nen tonal surfaceContainerHighest
 *  (bo vien outline kieu baseline), nhan -> secondaryContainer + bo goc
 *  999->12dp + scale .95, easing spring giong cac nut M3E khac. */
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
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = tween(300, easing = M3EMotion.spring),
        label = "chipScale",
    )
    val bg by animateColorAsState(
        targetValue = if (pressed) cs.secondaryContainer
        else cs.surfaceContainerHighest,
        animationSpec = tween(300),
        label = "chipBg",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(radius))
            .background(bg)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
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
