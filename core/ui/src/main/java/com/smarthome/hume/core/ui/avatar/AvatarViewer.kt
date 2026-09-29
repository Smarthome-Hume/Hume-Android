package com.smarthome.hume.core.ui.avatar

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
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
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Nhan vao avatar -> popup hinh TRON phong to giua man hinh, kieu kinh lup.
 * - Khong co icon but, khong co nut X, khong nut Doi/Go: chi de ngam avatar.
 * - Mo ra: hinh tron bay tu vi tri avatar trong header -> phong to giua man hinh.
 * - Nhan vao vung mo ngoai khung tron: hinh tron tu dong thu be bay ve dung
 *   vi tri avatar trong header roi moi dong popup.
 * - Nen xung quanh mo di (noi dung trang ben duoi duoc blur + phu lop mo).
 */
@Composable
fun AvatarViewerOverlay(
    name: String,
    avatar: UserAvatar?,
    haAvatarUrl: String?,
    /** Vi tri avatar trong header, theo toa do window (de bay ve khi dong). */
    targetRect: Rect?,
    onDismiss: () -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var overlayRect by remember { mutableStateOf<Rect?>(null) }
    var opened by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }

    val circleDp = (LocalConfiguration.current.screenWidthDp * 0.78f).dp
    val circlePx = with(density) { circleDp.toPx() }

    // Hinh tron lon nam giua overlay, theo toa do window
    fun bigRect(): Rect? {
        val o = overlayRect ?: return null
        val c = o.center
        return Rect(c.x - circlePx / 2f, c.y - circlePx / 2f, c.x + circlePx / 2f, c.y + circlePx / 2f)
    }

    val rectAnim = remember { Animatable(Rect(0f, 0f, 1f, 1f), Rect.VectorConverter) }
    val scrimAlpha by animateFloatAsState(if (closing) 0f else 0.55f, label = "scrim")

    // Mo ra: tu avatar -> phong to giua man hinh
    LaunchedEffect(overlayRect, targetRect) {
        val big = bigRect() ?: return@LaunchedEffect
        if (!opened) {
            opened = true
            rectAnim.snapTo(targetRect ?: big)
            rectAnim.animateTo(
                big,
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }
    }

    fun requestClose() {
        if (closing || !opened) return
        closing = true
        scope.launch {
            val big = bigRect()
            val small = targetRect
            if (big != null && small != null) {
                rectAnim.animateTo(
                    small,
                    spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium,
                    ),
                )
            }
            onDismiss()
        }
    }

    // Ve hinh tron tai vi tri animate (doi window -> local cua overlay)
    val r = rectAnim.value
    val origin = overlayRect?.topLeft ?: Offset.Zero
    val localOffset = IntOffset(
        (r.left - origin.x).roundToInt(),
        (r.top - origin.y).roundToInt(),
    )
    val rectSize = with(density) { r.width.toDp() }

    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { overlayRect = it.boundsInWindow() }
            .background(MaterialTheme.colorScheme.background.copy(alpha = scrimAlpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = ::requestClose,
            ),
    ) {
        if (opened) {
            Box(
                Modifier
                    .offset { localOffset }
                    .size(rectSize)
                    .shadow(28.dp, CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
                    // Chan tap tren hinh tron: nhan vao avatar khong dong popup
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
                        fontSize = (rectSize.value * 0.4f).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }
            }
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
