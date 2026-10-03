package com.smarthome.hume.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.ui.components.M3EIcons
import com.smarthome.hume.core.ui.components.M3EMotion
import com.smarthome.hume.core.ui.components.rememberHaptic
import com.smarthome.hume.core.ui.components.MsIcon

/**
 * FAB speed-dial trang Nha theo demo v4 (.fabwrap/.fab/.fmi + .fabscrim):
 * - Vi tri tuyet doi: parent dat right 20dp bottom 108dp (giong demo).
 * - Trigger 56dp bo 16dp primaryContainer -> mo: tron 50% primary, icon add
 *   24dp -> close 20dp, morph .45s spring.
 * - Items: opacity + translateY(18dp) scale(.7) tu goc duoi-phai, .45s spring,
 *   stagger .02/.08/.14/.2s tu item GAN nut nhat; :active brightness(.94).
 * - Scrim (.fabscrim) do parent (HomeScreen) ve khi open.
 */
@Composable
fun HomeFabMenu(
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onTurnOnLights: () -> Unit,
    onAc26: () -> Unit,
    onArmAway: () -> Unit,
    onEco: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val haptic = rememberHaptic()
    val density = LocalDensity.current
    val items = listOf(
        Triple("Bật đèn", M3EIcons.Light, onTurnOnLights),
        Triple("Điều hoà 26°", M3EIcons.Climate, onAc26),
        Triple("Bật an ninh", M3EIcons.Shield, onArmAway),
        Triple("Tiết kiệm điện", M3EIcons.Power, onEco),
    )

    Column(
        horizontalAlignment = Alignment.End,
        modifier = modifier,
    ) {
        items.forEachIndexed { i, (label, icon, action) ->
            // Stagger tu item gan nut nhat: .02/.08/.14/.2s
            val delayMs = (items.size - 1 - i) * 60 + 20
            AnimatedVisibility(
                visible = open,
                enter = fadeIn(tween(300, delayMs)) +
                    slideInVertically(
                        animationSpec = tween(450, delayMs, easing = M3EMotion.spring),
                    ) { with(density) { 18.dp.roundToPx() } } +
                    scaleIn(
                        animationSpec = tween(450, delayMs, easing = M3EMotion.spring),
                        initialScale = 0.7f,
                        transformOrigin = TransformOrigin(1f, 1f),
                    ),
                exit = fadeOut(tween(200)) +
                    slideOutVertically(
                        animationSpec = tween(300, easing = M3EMotion.emphasizedAcc),
                    ) { with(density) { 18.dp.roundToPx() } } +
                    scaleOut(
                        animationSpec = tween(300, easing = M3EMotion.emphasizedAcc),
                        targetScale = 0.7f,
                        transformOrigin = TransformOrigin(1f, 1f),
                    ),
            ) {
                FabMenuItem(
                    label = label,
                    icon = icon,
                    onClick = {
                        haptic()
                        onOpenChange(false)
                        action()
                    },
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        // Trigger
        val radius by animateDpAsState(
            targetValue = if (open) 28.dp else 16.dp,
            animationSpec = tween(450, easing = M3EMotion.spring),
            label = "fabR",
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .shadow(8.dp, RoundedCornerShape(radius))
                .clip(RoundedCornerShape(radius))
                .background(
                    if (open) cs.primary else cs.primaryContainer,
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    haptic()
                    onOpenChange(!open)
                },
        ) {
            MsIcon(
                if (open) M3EIcons.Close else M3EIcons.Add,
                contentDescription = null,
                tint = if (open) cs.onPrimary else cs.onPrimaryContainer,
                modifier = Modifier.size(if (open) 20.dp else 24.dp),
            )
        }
    }
}

@Composable
private fun FabMenuItem(
    label: String,
    icon: Any,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .height(56.dp)
            .shadow(8.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(cs.primaryContainer)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .align(Alignment.Center),
        ) {
            MsIcon(
                icon, contentDescription = null,
                tint = cs.onPrimaryContainer,
                modifier = Modifier.size(24.dp),
            )
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                color = cs.onPrimaryContainer,
            )
        }
        // :active brightness(.94)
        if (pressed) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.06f)),
            )
        }
    }
}
