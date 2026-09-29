package com.smarthome.hume.ui.profile

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import java.io.File

/**
 * Avatar tron trong the chu nha.
 * Thu tu uu tien: anh/video user upload > anh HA > chu cai dau ten (mac dinh theo user).
 * Nhan vao avatar -> mo phong to; nhan badge may anh -> doi avatar.
 */
@Composable
fun ProfileAvatar(
    name: String,
    avatar: UserAvatar?,
    haAvatarUrl: String?,
    onTap: () -> Unit,
    onEdit: () -> Unit,
    size: Dp = 60.dp,
    /** Bao vi tri avatar theo toa do window (de viewer bay ve khi dong). */
    onPositioned: ((Rect) -> Unit)? = null,
) {
    Box(
        Modifier
            .size(size)
            .onGloballyPositioned { onPositioned?.invoke(it.boundsInWindow()) },
    ) {
        Box(
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.2f))
                .clickable(onClick = onTap),
            contentAlignment = Alignment.Center,
        ) {
            when {
                avatar != null && !avatar.isVideo -> {
                    AsyncImage(
                        model = avatar.file,
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(size).clip(CircleShape),
                    )
                }
                avatar != null && avatar.isVideo -> {
                    LoopingVideoAvatar(
                        file = avatar.file,
                        modifier = Modifier.size(size).clip(CircleShape),
                    )
                }
                haAvatarUrl != null -> {
                    AsyncImage(
                        model = haAvatarUrl,
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(size).clip(CircleShape),
                    )
                }
                else -> {
                    Text(
                        name.trim().firstOrNull()?.uppercase() ?: "?",
                        fontSize = (size.value * 0.42f).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
        }
        // Badge doi avatar
        Box(
            Modifier
                .size(22.dp)
                .offset(x = 2.dp, y = 2.dp)
                .align(Alignment.BottomEnd)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onEdit),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.PhotoCamera,
                contentDescription = "Đổi avatar",
                tint = Color.White,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

/** Video avatar nho: phat lap lai, tat tieng, crop tron. */
@Composable
private fun LoopingVideoAvatar(file: File, modifier: Modifier = Modifier) {
    val context = LocalContext.current
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
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        },
        modifier = modifier,
    )
}
