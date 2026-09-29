package com.smarthome.hume.feature.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smarthome.hume.core.model.HomeNotification
import com.smarthome.hume.core.model.RoomUi
import com.smarthome.hume.core.ui.components.M3EMotion
import kotlinx.coroutines.delay

/**
 * Hieu ung vao .rise cua demo rev12: GIU LAYOUT ON DINH (giong CSS
 * transform/opacity khong doi layout), chi animate alpha 0->1 +
 * translationY 22dp->0 trong 700ms easing emphasized cubic-bezier(.05,.7,.1,1),
 * bat dau sau delayMs (stagger .02/.14/.155/.165/.2/.22/.24/.34s + room cards
 * 360ms + 30ms/card).
 */
fun Modifier.riseIn(delayMs: Int): Modifier = composed {
    var target by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    LaunchedEffect(delayMs) {
        delay(delayMs.toLong())
        target = 1f
    }
    val p by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(700, easing = M3EMotion.emphasized),
        label = "rise",
    )
    this.graphicsLayer {
        alpha = p
        translationY = with(density) { 22.dp.toPx() } * (1f - p)
    }
}

/**
 * Press morph theo demo rev12: khi nhan, scale xuong [pressedScale] DONG THOI
 * giam bo goc tu [corner] ve [pressedCorner] (giam 1 nac, khong nhay pill ->
 * chu nhat), easing spring. Dung thay clickable mac dinh cho moi the.
 * CHI tao interaction/clickable khi onClick != null (the tinh nhu .dev
 * khong co hieu ung nhan).
 */
fun Modifier.pressMorphCard(
    pressedScale: Float = 0.95f,
    corner: Dp = 28.dp,
    pressedCorner: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
): Modifier = composed {
    if (onClick == null) {
        // The tinh: chi clip bo goc, khong interaction/clickable
        return@composed this.clip(RoundedCornerShape(corner))
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = tween(300, easing = M3EMotion.spring),
        label = "pmScale",
    )
    val radius by animateDpAsState(
        targetValue = if (pressed) pressedCorner else corner,
        animationSpec = tween(450, easing = M3EMotion.spring),
        label = "pmRadius",
    )
    this
        .graphicsLayer(scaleX = scale, scaleY = scale)
        .clip(RoundedCornerShape(radius))
        .clickable(
            interactionSource = interaction,
            indication = null,
            onClick = onClick,
        )
}

/**
 * Cham bao cua mo (.rdot2) tren the phong.
 * GIOI HAN DU LIEU: model hien tai khong co trang thai contact theo phong,
 * nen map tu thong bao "Cua dang mo" vao phong theo ten (best-effort).
 */
fun doorOpenForRoom(room: RoomUi, notifications: List<HomeNotification>): Boolean =
    notifications.any { n ->
        n.title.contains("Cửa", ignoreCase = true) &&
            (n.body.contains(room.name, ignoreCase = true) ||
                room.name.contains(n.body, ignoreCase = true))
    }

/**
 * Trang thai contact ("open"/null) cho 1 thiet bi trong sheet phong.
 * GIOI HAN DU LIEU: model chi co Toggle/Climate; cam bien cua duoc nhan dien
 * qua iconKey "door" va map trang thai mo tu thong bao (best-effort).
 * Tra ve null neu khong phai cam bien cua.
 */
fun contactOpenForDevice(
    iconKey: String,
    label: String,
    notifications: List<HomeNotification>,
): Boolean? {
    if (iconKey != "door") return null
    val open = notifications.any { n ->
        n.title.contains("Cửa", ignoreCase = true) &&
            (n.body.contains(label, ignoreCase = true) ||
                label.contains(n.body, ignoreCase = true))
    }
    return open
}

/**
 * Fling dam hon cho trang Nha: giam van toc quan tinh con [factor]
 * (mac dinh 0.6) -> cuon cham, dam, do hon; giong cam giac cac ban truoc
 * (tren 120Hz fling mac dinh thay nhanh).
 */
private class DampedFlingBehavior(
    private val base: FlingBehavior,
    private val factor: Float,
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        val outer = this
        return with(base) { outer.performFling(initialVelocity * factor) }
    }
}

@Composable
fun rememberDampedFlingBehavior(factor: Float = 0.6f): FlingBehavior {
    val base = ScrollableDefaults.flingBehavior()
    return remember(base, factor) { DampedFlingBehavior(base, factor) }
}
