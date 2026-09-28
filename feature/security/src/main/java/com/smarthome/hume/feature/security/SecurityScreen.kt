package com.smarthome.hume.feature.security

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.smarthome.hume.core.model.RecordingUi
import com.smarthome.hume.core.model.SecurityUiState
import com.smarthome.hume.core.model.SensorKind
import com.smarthome.hume.core.model.SensorUi
import com.smarthome.hume.core.ui.components.EsubGroup
import com.smarthome.hume.core.ui.components.M3ECard
import com.smarthome.hume.core.ui.components.M3EIcons
import com.smarthome.hume.core.ui.components.M3EMotion
import com.smarthome.hume.core.ui.components.blink
import com.smarthome.hume.core.ui.components.pressMorph
import com.smarthome.hume.core.ui.components.rememberHaptic
import com.smarthome.hume.core.ui.theme.LocalHumeExtraColors
import kotlinx.coroutines.delay

/**
 * Tab An ninh — port 1:1 demo v4 rev12 (#page-security):
 * chon camera, feed Frigate (khoa/mo), toolbar overlay, clip gan day,
 * grid Cua/Chuyen dong/Moi truong.
 */

/**
 * Rise entrance theo demo: opacity 0->1 + translateY 22px->0,
 * 700ms emphasized decelerate, delay tuy section (.42s -> .54s).
 */
private fun Modifier.riseIn(delayMs: Int = 0): Modifier = composed {
    val density = LocalDensity.current
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (delayMs > 0) delay(delayMs.toLong())
        progress.animateTo(1f, animationSpec = tween(700, easing = M3EMotion.emphasized))
    }
    this.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * with(density) { 22.dp.toPx() }
    }
}

@Composable
fun SecurityScreen(vm: SecurityViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val selectedCam by vm.selectedCam.collectAsState()
    val clip by vm.clip.collectAsState()
    val flicker by vm.flicker.collectAsState()
    val haptic = rememberHaptic()
    val cs = MaterialTheme.colorScheme

    // "Live flicker" demo: override trang thai motion sensor duoc VM random moi 22s.
    val motionSensors = state.motionSensors.map { flicker[it.entityId] ?: it }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 96.dp),
        ) {
            // Header (demo .phdr: padding 12px 2px 6px; h2 26px/700/-0.3px; p 13px)
            Column(
                Modifier
                    .padding(start = 2.dp, end = 2.dp, top = 12.dp, bottom = 6.dp)
                    .riseIn(0),
            ) {
                Text(
                    "An ninh",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "Camera & trạng thái bảo vệ",
                    fontSize = 13.sp,
                    color = cs.onSurfaceVariant,
                )
            }

            // Camera picker (demo .esub: margin-bottom 14px; rise .42s)
            if (state.cameras.isNotEmpty()) {
                EsubGroup(
                    items = state.cameras.map { it.name },
                    selectedIndex = selectedCam,
                    onSelect = { vm.selectCamera(it); haptic() }, // demo vibrate(6) doi camera
                    modifier = Modifier
                        .fillMaxWidth()
                        .riseIn(420),
                )
                Spacer(Modifier.height(14.dp))
                val cam = state.cameras[selectedCam]
                CameraCard(vm = vm, camKey = cam.key, camName = cam.name)
                Spacer(Modifier.height(8.dp))

                // Recent recordings (rise .46s / .48s)
                SecHeader(title = "Video ghi hình gần đây", action = "Tải 10 clip", delayMs = 460)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .riseIn(480),
                ) {
                    items(recs(state, selectedCam), key = { it.id }) { rec ->
                        RecCard(rec = rec, onClick = { vm.openClip(rec) })
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            // Sensor grids
            SecHeader(title = "Cửa", delayMs = 500)
            SensorGrid(sensors = state.doorSensors)
            SecHeader(title = "Chuyển động", delayMs = 520)
            SensorGrid(sensors = motionSensors)
            SecHeader(title = "Môi trường", delayMs = 540)
            SensorGrid(sensors = state.envSensors)
        }

        // Clip viewer — Dialog full-screen: phu ca navbar (demo .clipov position:fixed z-index:200)
        clip?.let { c ->
            ClipOverlay(
                label = "${c.timeLabel} · ${c.dateLabel}",
                onClose = vm::closeClip,
            )
        }
    }
}

private fun recs(state: SecurityUiState, selectedCam: Int): List<RecordingUi> =
    state.cameras.getOrNull(selectedCam)?.let { state.recordings[it.key] }.orEmpty()

@Composable
private fun SecHeader(title: String, action: String? = null, delayMs: Int = 0) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 10.dp)
            .padding(horizontal = 4.dp)
            .riseIn(delayMs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (action != null) {
            Text(
                action,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// ---------- camera ----------

@Composable
private fun CameraCard(vm: SecurityViewModel, camKey: String, camName: String) {
    val unlocked by vm.unlocked.collectAsState()
    val haptic = rememberHaptic()
    val cs = MaterialTheme.colorScheme
    var frame by remember(camKey) { mutableStateOf(0L) }
    var failed by remember(camKey) { mutableStateOf(false) }

    LaunchedEffect(camKey, unlocked) {
        if (!unlocked) return@LaunchedEffect
        while (true) {
            frame = System.currentTimeMillis()
            delay(3000)
        }
    }

    // demo .scfeed.locked .scview: blur(16px) brightness(.8), transition .5s
    val blurDp by animateDpAsState(
        targetValue = if (unlocked) 0.dp else 16.dp,
        animationSpec = tween(500),
        label = "lockBlur",
    )
    val dimAlpha by animateFloatAsState(
        targetValue = if (unlocked) 0f else 0.2f, // brightness(.8) ~ lop den 20%
        animationSpec = tween(500),
        label = "lockDim",
    )

    M3ECard(
        shape = RoundedCornerShape(32.dp),
        contentPadding = 12.dp,
        containerColor = LocalHumeExtraColors.current.surfaceHighest, // demo .seccam: surfaceHighest
        modifier = Modifier.riseIn(440),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(22.dp))
                // demo .scfeed: gradient + 2 radial highlight
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF2B3A4A), Color(0xFF1A2430), Color(0xFF24303D)),
                    ),
                )
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.09f), Color.Transparent),
                            center = Offset(size.width * 0.2f, size.height * 0.3f),
                            radius = 120.dp.toPx(),
                        ),
                        radius = 120.dp.toPx(),
                        center = Offset(size.width * 0.2f, size.height * 0.3f),
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                            center = Offset(size.width * 0.75f, size.height * 0.7f),
                            radius = 200.dp.toPx(),
                        ),
                        radius = 200.dp.toPx(),
                        center = Offset(size.width * 0.75f, size.height * 0.7f),
                    )
                }
                .pressMorph(
                    onClick = { vm.unlock(); haptic() }, // demo vibrate(8) mo khoa
                ),
        ) {
            val context = LocalContext.current
            val url = vm.snapshotUrl(camKey)
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(if (unlocked) "$url?t=$frame" else url)
                    .memoryCachePolicy(if (unlocked) CachePolicy.DISABLED else CachePolicy.ENABLED)
                    .build(),
                contentDescription = camName,
                contentScale = ContentScale.Fit,
                onSuccess = { failed = false },
                onError = { failed = true },
                modifier = Modifier
                    .fillMaxSize()
                    .blur(blurDp),
            )
            if (dimAlpha > 0.01f) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = dimAlpha)),
                )
            }

            // LIVE badge (demo .scfeed .live: top/left 10px, padding 5px 10px, radius 8px,
            // 10px/800/ls 1px, dot 7px blink 1.4s)
            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE53935))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .blink(1400),
                )
                Text(
                    "LIVE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = Color.White,
                )
            }
            // name badge (demo .scname: radius 999px, padding 6px 12px; blur: gioi han sandbox)
            Text(
                camName,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )

            if (failed) {
                Text(
                    "Không lấy được hình từ Frigate",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else if (!unlocked) {
                // unlock overlay (demo .scunlock: 13px/600, icon 34px, gap 8px)
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        M3EIcons.Lock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(34.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Chạm để mở khoá",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                }
            }

            // Toolbar: overlay day feed, pill kinh (demo .ftoolbar absolute bottom-center)
            // surfaceContainerLowest 72% + vien trang 25%, radius 26px, padding 5px
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(cs.surfaceContainerLowest.copy(alpha = 0.72f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(26.dp))
                    .padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ToolbarBtn(
                    icon = M3EIcons.Rec,
                    label = "Ghi hình",
                    tint = cs.error, // demo .ftbtn.rec: LUON do #E53935
                    onClick = vm::toggleRec,
                )
                ToolbarBtn(icon = M3EIcons.Mic, label = "Đàm thoại", tint = cs.onSurface, onClick = {})
                ToolbarBtn(icon = M3EIcons.PhotoCamera, label = "Chụp ảnh", tint = cs.onSurface, onClick = {})
                ToolbarBtn(icon = M3EIcons.Fullscreen, label = "Toàn màn hình", tint = cs.onSurface, onClick = {})
            }
        }
    }
}

/**
 * Nut toolbar camera: 48x48 squircle radius 22px, nen transparent, icon 24px.
 * Press morph: scale .85 + nen primaryContainer + radius 22->15 (spring).
 */
@Composable
private fun ToolbarBtn(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.85f else 1f,
        animationSpec = tween(300, easing = M3EMotion.spring),
        label = "tbScale",
    )
    val radius by animateDpAsState(
        targetValue = if (pressed) 15.dp else 22.dp,
        animationSpec = tween(350, easing = M3EMotion.spring),
        label = "tbRadius",
    )
    val bg by animateColorAsState(
        targetValue = if (pressed) cs.primaryContainer else Color.Transparent,
        animationSpec = tween(250),
        label = "tbBg",
    )
    Box(
        Modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(radius))
            .background(bg)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
    }
}

// ---------- recordings ----------

@Composable
private fun RecCard(rec: RecordingUi, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier
            .width(132.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(LocalHumeExtraColors.current.surfaceHighest) // demo .rec: surfaceHighest
            .pressMorph(pressedScale = 0.94f, onClick = onClick) // demo .rec:active scale(.94)
            .padding(8.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(150f / 86f)
                .clip(RoundedCornerShape(13.dp)) // demo .recthumb radius 13px
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF33414F), Color(0xFF1C2530)),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                M3EIcons.PlayCircle,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.size(34.dp),
            )
        }
        Text(
            "${rec.timeLabel} · ${rec.dateLabel}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 7.dp),
        )
    }
}

// ---------- sensors ----------

@Composable
private fun SensorGrid(sensors: List<SensorUi>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        sensors.chunked(2).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { s ->
                    SensorCard(s, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SensorCard(s: SensorUi, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val on = s.isOn
    // demo .scard: surfaceHighest; .on: errorContainer; .warn.on: tertiaryContainer
    val bg = when {
        on && s.warn -> cs.tertiaryContainer
        on -> cs.errorContainer
        else -> LocalHumeExtraColors.current.surfaceHighest
    }
    // demo .scard.on .sic: trang 55%; .warn.on chi doi mau icon
    val iconBg = if (on) Color.White.copy(alpha = 0.55f) else cs.surfaceContainer
    val iconTint = when {
        on && s.warn -> cs.onTertiaryContainer
        on -> cs.onErrorContainer
        else -> cs.onSurfaceVariant
    }
    val nameColor = when {
        on && s.warn -> cs.onTertiaryContainer
        on -> cs.onErrorContainer
        else -> cs.onSurface
    }
    val chipLabel = when (s.kind) {
        SensorKind.Door -> if (on) "MỞ" else "ĐÓNG"
        SensorKind.Motion -> if (on) "PHÁT HIỆN" else "TRỐNG"
        else -> if (on) "BÁO ĐỘNG" else if (s.iconKey == "smoke") "Bình thường" else "An toàn"
    }
    val icon = when (s.iconKey) {
        "door" -> M3EIcons.Door
        "motion" -> M3EIcons.Motion
        "presence" -> M3EIcons.Presence
        "smoke" -> M3EIcons.Smoke
        else -> M3EIcons.Leak
    }

    Box(
        modifier
            .clip(RoundedCornerShape(26.dp))
            .background(bg),
    ) {
        Column(Modifier.padding(14.dp)) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        s.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = nameColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        s.lastChange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (on) nameColor.copy(alpha = 0.75f) else cs.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                // demo .sst: 10.5px/800/ls .8px, padding 6px 12px, radius 999px;
                // .on (ke ca warn): nen error chu trang
                Text(
                    chipLabel,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp,
                    color = if (on) Color.White else cs.onSurfaceVariant,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (on) cs.error else cs.surfaceContainer)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        // demo .neon: absolute top/right 12px, 9px, error + glow, blink 1.2s khi on
        if (on) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(17.dp)
                        .clip(CircleShape)
                        .background(cs.error.copy(alpha = 0.55f))
                        .blur(3.dp)
                        .blink(1200),
                )
                Box(
                    Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(cs.error)
                        .blink(1200),
                )
            }
        }
    }
}

// ---------- clip overlay ----------

@Composable
private fun ClipOverlay(label: String, onClose: () -> Unit) {
    // demo .clipov: position:fixed inset:0, nen #000 dac, z-index 200 (phu ca navbar)
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(0.88f)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF2B3A4A), Color(0xFF141C26)),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        M3EIcons.Videocam,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp),
                    )
                }
                Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    "Đóng",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .clickable(onClick = onClose)
                        .padding(horizontal = 28.dp, vertical = 12.dp),
                )
            }
        }
    }
}
