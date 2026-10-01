package com.smarthome.hume.core.ui.camera

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.smarthome.hume.core.ui.components.M3ECard
import com.smarthome.hume.core.ui.components.M3EIcons
import com.smarthome.hume.core.ui.components.MsIcon
import com.smarthome.hume.core.ui.components.blink
import com.smarthome.hume.core.ui.components.pressMorph
import com.smarthome.hume.core.ui.components.rememberHaptic
import com.smarthome.hume.core.ui.theme.LocalHumeExtraColors
import kotlinx.coroutines.delay

/**
 * The camera Frigate dung chung: tab An ninh + popup tren trang Nha.
 * Dual-path (2026-10-01): local (WireGuard) thi stream RTSP lien tuc bang
 * ExoPlayer; remote (Cloudflare Tunnel khong cho RTSP) hoac rtspUrl null
 * thi fallback ve snapshot refresh moi 3s. Khoa thi blur + overlay
 * "Cham de mo khoa" (khong stream khi khoa).
 */
@Composable
fun CameraFeedCard(
    camKey: String,
    camName: String,
    snapshotUrl: String,
    unlocked: Boolean,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier,
    rtspUrl: String? = null,
) {
    val context = LocalContext.current
    val haptic = rememberHaptic()
    var frame by remember(camKey) { mutableStateOf(0L) }

    // Chi poll snapshot khi khong stream RTSP.
    val streaming = unlocked && rtspUrl != null
    LaunchedEffect(camKey, unlocked, streaming) {
        if (!unlocked || streaming) return@LaunchedEffect
        while (true) {
            frame = System.currentTimeMillis()
            delay(3000)
        }
    }

    // Player RTSP: chi tao khi da mo khoa + co URL (local).
    val player = remember(rtspUrl, unlocked) {
        if (!streaming) null else ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(rtspUrl!!))
            repeatMode = Player.REPEAT_MODE_OFF
            volume = 0f
            playWhenReady = true
            prepare()
        }
    }
    DisposableEffect(rtspUrl, unlocked) { onDispose { player?.release() } }

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
        modifier = modifier,
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
                // demo scfeed keyboard: Enter/Space mo khoa
                .focusable()
                .onKeyEvent {
                    if ((it.key == Key.Enter || it.key == Key.Spacebar) && it.type == KeyEventType.KeyUp) {
                        onUnlock(); haptic(); true
                    } else false
                }
                .pressMorph(
                    onClick = { onUnlock(); haptic() }, // demo vibrate(8) mo khoa
                ),
        ) {
            val url = snapshotUrl
            // demo .scview .ms: placeholder videocam 64px trang 35%, luon render duoi anh
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                MsIcon(
                    M3EIcons.Videocam,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.size(64.dp),
                )
            }
            if (player != null) {
                // Local: stream RTSP lien tuc.
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    },
                    update = { it.player = player },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(if (unlocked) "$url?t=$frame" else url)
                        // Downsample ve kich thuoc hien thi (fix nong may 2026-09-30):
                        // truoc day decode full-res + upload texture moi 3s.
                        .size(960, 540)
                        .memoryCachePolicy(if (unlocked) CachePolicy.DISABLED else CachePolicy.ENABLED)
                        .build(),
                    contentDescription = camName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(blurDp),
                )
            }
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
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Color.White,
                )
            }
            // name badge (demo .scname: radius 999px, padding 6px 12px; blur: gioi han sandbox)
            Text(
                camName,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )

            if (!unlocked) {
                // unlock overlay (demo .scunlock: 13px/600, icon 34px, gap 8px)
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    MsIcon(
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
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            // (da bo floating toolbar theo yeu cau user)
        }
    }
}
