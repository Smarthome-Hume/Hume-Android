package com.smarthome.hume.ui.profile

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import java.io.File

/**
 * Nhan vao avatar -> phong to kieu kinh lup: giu khung TRON, phong to giua
 * man hinh tren nen mo. Anh crop tron; video phat lap lai tat tieng trong
 * hinh tron (nhan vao de tam dung / phat tiep).
 */
@Composable
fun AvatarViewerDialog(
    name: String,
    avatar: UserAvatar?,
    haAvatarUrl: String?,
    onChange: () -> Unit,
    onRemove: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.88f))
                .clickable(onClick = onDismiss),
        ) {
            // Hieu ung phong dai pop-in
            val scale = remember { Animatable(0.7f) }
            LaunchedEffect(Unit) {
                scale.animateTo(
                    1f,
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                )
            }
            val circleSize = (LocalConfiguration.current.screenWidthDp * 0.78f).dp
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(circleSize)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                    }
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    avatar?.isVideo == true -> MagnifiedVideoAvatar(file = avatar.file)
                    avatar != null || haAvatarUrl != null -> AsyncImage(
                        model = avatar?.file ?: haAvatarUrl,
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    else -> Text(
                        name.trim().firstOrNull()?.uppercase() ?: "?",
                        fontSize = 120.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }
            }

            // Nut dong
            Box(
                Modifier
                    .statusBarsPadding()
                    .padding(12.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(onClick = onDismiss)
                    .padding(10.dp),
            ) {
                Icon(Icons.Outlined.Close, contentDescription = "Đóng", tint = Color.White)
            }

            // Thanh tac vu duoi
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onChange) {
                    Text("Đổi avatar", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
                if (onRemove != null) {
                    TextButton(onClick = onRemove) {
                        Text("Gỡ avatar", color = Color(0xFFEF9A9A), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

/**
 * Video phong to trong khung tron: lap lai, tat tieng.
 * Nhan vao hinh tron de tam dung / phat tiep.
 */
@Composable
private fun MagnifiedVideoAvatar(file: File) {
    val context = LocalContext.current
    var playing by remember { mutableStateOf(true) }
    val player = remember(file) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            playWhenReady = true
            prepare()
        }
    }
    DisposableEffect(file) { onDispose { player.release() } }
    LaunchedEffect(playing) { player.playWhenReady = playing }

    Box(
        Modifier
            .fillMaxSize()
            .clickable { playing = !playing },
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (!playing) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Phát",
                    tint = Color.White,
                    modifier = Modifier.size(64.dp),
                )
            }
        }
    }
}
