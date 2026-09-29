package com.smarthome.hume.feature.security

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import com.smarthome.hume.core.ui.components.Ms
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.smarthome.hume.core.model.RecordingUi
import com.smarthome.hume.core.model.SecurityUiState
import com.smarthome.hume.core.model.SensorKind
import com.smarthome.hume.core.model.SensorUi
import com.smarthome.hume.core.ui.camera.CameraFeedCard
import com.smarthome.hume.core.ui.components.EsubGroup
import com.smarthome.hume.core.ui.components.M3ECard
import com.smarthome.hume.core.ui.components.M3EIcons
import com.smarthome.hume.core.ui.components.M3EMotion
import com.smarthome.hume.core.ui.components.pressMorph
import com.smarthome.hume.core.ui.theme.LocalHumeExtraColors
import com.smarthome.hume.core.ui.components.rememberHaptic
import com.smarthome.hume.core.ui.components.MarqueeText
import com.smarthome.hume.core.ui.components.MsIcon
import java.io.File
import kotlinx.coroutines.delay

/**
 * Tab An ninh — port 1:1 demo v4 rev12 (#page-security):
 * chon camera, feed Frigate (khoa/mo), clip gan day,
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
fun SecurityScreen(
    vm: SecurityViewModel = viewModel(),
    onDownloadClip: ((RecordingUi) -> Unit)? = null,
    onShareClip: ((RecordingUi) -> Unit)? = null,
) {
    val state by vm.state.collectAsState()
    val selectedCam by vm.selectedCam.collectAsState()
    val clip by vm.clip.collectAsState()
    val haptic = rememberHaptic()
    val cs = MaterialTheme.colorScheme

    // Sensor hien data that tu HA (repo). Da xoa "live flicker" demo
    // (random toggle + "Vua xong" moi 22s) ngay 2026-09-30.
    val motionSensors = state.motionSensors

    Box(Modifier.fillMaxSize()) {
        // Full-bleed tran duoi status bar trong suot (2026-09-30): khong
        // statusBarsPadding o modifier; inset status bar la Spacer dau tien
        // de scroll lien mach (nhu tab Nha).
        val statusBarTop =
            WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 130.dp),
        ) {
            Spacer(Modifier.height(statusBarTop + 8.dp))
            // Header: chi title duoc boc nen (subtitle de ngoai, khong nen)
            Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp)) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(28.dp))
                        .background(cs.primaryContainer)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .riseIn(0),
                ) {
                    Text(
                        "An ninh",
                        color = cs.onPrimaryContainer,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp,
                    )
                }
            }

            // Camera picker (demo .esub: margin-bottom 14px; rise .42s)
            // Hien icon thay chu vi khong du chieu dai
            if (state.cameras.isNotEmpty()) {
                EsubGroup(
                    items = state.cameras.map { it.name },
                    icons = state.cameras.map { cam ->
                        M3EIcons.room(when (cam.key) {
                            "living" -> "sofa"
                            "kitchen" -> "kitchen"
                            "outdoor" -> "home"
                            "server" -> "sparkles"
                            "bedroom" -> "bed"
                            else -> "home"
                        })
                    },
                    selectedIndex = selectedCam,
                    onSelect = { vm.selectCamera(it); haptic() }, // demo vibrate(6) doi camera
                    modifier = Modifier
                        .fillMaxWidth()
                        .riseIn(420),
                    // demo .dvsegi: 12px/600, padding 8px 10px, icon check 16px
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    itemPadding = PaddingValues(vertical = 8.dp, horizontal = 10.dp),
                    checkSize = 16.dp,
                )
                Spacer(Modifier.height(14.dp))
                val cam = state.cameras[selectedCam]
                CameraCard(vm = vm, camKey = cam.key, camName = cam.name)
                Spacer(Modifier.height(8.dp))

                // Recent recordings (rise .46s / .48s)
                SecHeader(
                    title = "Video ghi hình gần đây",
                    action = "Tải 10 clip",
                    delayMs = 460,
                    onAction = {
                        haptic()
                        vm.refreshRecordings()
                    },
                    actionLoading = state.downloadingCams.contains(cam.key),
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    // demo .reclist: padding 2px 2px 6px
                    contentPadding = PaddingValues(start = 2.dp, end = 2.dp, top = 2.dp, bottom = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .riseIn(480),
                ) {
                    items(recs(state, selectedCam), key = { it.id }) { rec ->
                        RecCard(
                            rec = rec,
                            onClick = { vm.openClip(rec) },
                            onDownload = onDownloadClip?.let { { it(rec) } },
                        )
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
                clip = c,
                onClose = vm::closeClip,
                onDownload = onDownloadClip?.let { { it(c) } },
                onShare = onShareClip?.let { { it(c) } },
            )
        }
    }
}

private fun recs(state: SecurityUiState, selectedCam: Int): List<RecordingUi> =
    state.cameras.getOrNull(selectedCam)?.let { state.recordings[it.key] }.orEmpty()

@Composable
private fun SecHeader(
    title: String,
    action: String? = null,
    delayMs: Int = 0,
    onAction: (() -> Unit)? = null,
    actionLoading: Boolean = false,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 12.dp)
            .padding(horizontal = 4.dp)
            .riseIn(delayMs),
        horizontalArrangement = Arrangement.SpaceBetween,
        // demo .sec: align-items baseline
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            title,
            // demo .sec h3: 16px/700/ls -.1px
            style = MaterialTheme.typography.titleMedium.copy(letterSpacing = (-0.1).sp),
            fontWeight = FontWeight.Bold,
        )
        if (actionLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        } else if (action != null) {
            Text(
                action,
                // demo .secmore: 12px/700/ls 0
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(
                    enabled = onAction != null,
                    onClick = { onAction?.invoke() },
                ),
            )
        }
    }
}

// ---------- camera ----------

@Composable
private fun CameraCard(vm: SecurityViewModel, camKey: String, camName: String) {
    val unlocked by vm.unlocked.collectAsState()
    CameraFeedCard(
        camKey = camKey,
        camName = camName,
        snapshotUrl = vm.snapshotUrl(camKey),
        unlocked = unlocked,
        onUnlock = { vm.unlock() },
        modifier = Modifier.riseIn(440),
    )
}

// ---------- recordings ----------

@Composable
private fun RecCard(
    rec: RecordingUi,
    onClick: () -> Unit,
    onDownload: (() -> Unit)? = null,
) {
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
            MsIcon(
                M3EIcons.PlayCircle,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.size(34.dp),
            )
            // Nut download goc phai tren thumbnail
            if (onDownload != null && rec.clipPath != null) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(onClick = onDownload),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = M3EIcons.Download,
                        contentDescription = "Tải xuống",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
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
    // Active: dung primaryContainer/tertiaryContainer de doi mau theo theme
    // (khong dung errorContainer vi Material quy dinh error luon do).
    // Inactive: the surfaceHighest, icon/pill = surfaceContainer.
    val cardBg = when {
        on && s.warn -> cs.tertiaryContainer
        on -> cs.primaryContainer
        else -> LocalHumeExtraColors.current.surfaceHighest
    }
    val iconBg = when {
        on -> androidx.compose.ui.graphics.Color.White.copy(alpha = 0.55f)
        else -> cs.surfaceContainer
    }
    val iconTint = when {
        on && s.warn -> cs.onTertiaryContainer
        on -> cs.onPrimaryContainer
        else -> cs.onSurfaceVariant
    }
    val nameColor = when {
        on && s.warn -> cs.onTertiaryContainer
        on -> cs.onPrimaryContainer
        else -> cs.onSurface
    }
    val timeColor = when {
        on && s.warn -> cs.onTertiaryContainer.copy(alpha = 0.75f)
        on -> cs.onPrimaryContainer.copy(alpha = 0.75f)
        else -> cs.onSurfaceVariant
    }
    // Pill trang thai: active = nen dac (primary/tertiary) + chu trang/onContainer;
    // inactive = surfaceContainer + onSurfaceVariant.
    val pillBg = when {
        on && s.warn -> cs.tertiary
        on -> cs.primary
        else -> cs.surfaceContainer
    }
    val pillText = when {
        on && s.warn -> cs.onTertiary
        on -> cs.onPrimary
        else -> cs.onSurfaceVariant
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
            .background(cardBg),
    ) {
        // Layout 2 hang:
        // - Hang 1: [icon tron 44.dp | ten ellipsis] — ten dai thi an
        //   bang dau "…" ngay khi cham mep (theo demo, khong chay marquee).
        // - Hang 2: status pill doc lap mot dong (nen rieng, doi mau khi active)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center,
                ) {
                    MsIcon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
                }
                Column(Modifier.weight(1f)) {
                    // Ten sensor dai: chay marquee (theo yeu cau user 29/09)
                    MarqueeText(
                        text = s.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = nameColor,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        s.lastChange,
                        modifier = Modifier.padding(start = 12.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = timeColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                    )
                }
            }
            // demo .sst: 10.5px/800/ls .8px, padding 6px 12px, radius 999px
            Text(
                chipLabel,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp,
                textAlign = TextAlign.Center,
                color = pillText,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(pillBg)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

// ---------- clip overlay ----------

/**
 * Player video ghi hinh that: play mp4 da tai ve local (FrigateStore.clipPath)
 * bang ExoPlayer + PlayerView (media3-ui) qua AndroidView.
 */
@Composable
private fun ClipOverlay(
    clip: RecordingUi,
    onClose: () -> Unit,
    onDownload: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
) {
    val context = LocalContext.current

    // ExoPlayer that: tao 1 lan theo file, release ngay khi dialog dong.
    // File khong ton tai (hoac chua tai xong) -> player = null, hien message thay vi crash.
    val player = remember(clip.clipPath) {
        val file = clip.clipPath?.let(::File)?.takeIf { it.exists() }
        file?.let {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(Uri.fromFile(it)))
                prepare()
                playWhenReady = true
            }
        }
    }
    DisposableEffect(player) {
        onDispose { player?.release() }
    }

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
                    when {
                        player != null -> AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    this.player = player
                                    // Controller mac dinh: co nut play/pause, seek, fullscreen.
                                    useController = true
                                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                                }
                            },
                            update = { it.player = player },
                            modifier = Modifier.fillMaxSize(),
                        )
                        clip.clipPath == null -> Text(
                            "Đang tải...",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        else -> Text(
                            "Không tìm thấy file video",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
                // demo .ct: margin-bottom 12px (gap 18 + 12 = 30px toi nut)
                Text(
                    "${clip.timeLabel} · ${clip.dateLabel}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                // Hang nut chuc nang: Tai xuong | Chia se | Dong
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onDownload != null && clip.clipPath != null) {
                        Text(
                            "Tải xuống",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.14f))
                                .clickable(onClick = onDownload)
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                        )
                    }
                    if (onShare != null && clip.clipPath != null) {
                        Text(
                            "Chia sẻ",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.14f))
                                .clickable(onClick = onShare)
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                        )
                    }
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
}
