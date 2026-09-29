package com.smarthome.hume.ui.profile

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
 * Nhan vao avatar -> popup hinh TRON phong to giua man hinh, kieu kinh lup.
 * - Khong co icon but, khong co nut Doi/Go: chi de ngam avatar.
 * - Nen xung quanh mo di (noi dung trang ben duoi duoc blur + phu lop mo).
 * - Nhan ra ngoai hoac nut X de dong.
 */
@Composable
fun AvatarViewerOverlay(
    name: String,
    avatar: UserAvatar?,
    haAvatarUrl: String?,
    onDismiss: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
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
                .shadow(28.dp, CircleShape)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
                // Chan tap tren hinh tron: khong dong popup khi nhan vao avatar
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
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
    }
}

/**
 * Video phong to trong khung tron: lap lai vo han, tat tieng.
 * Chi de ngam hieu ung phong dai - khong co tuong tac.
 */
@Composable
private fun MagnifiedVideoAvatar(file: File) {
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
        modifier = Modifier.fillMaxSize(),
    )
}
