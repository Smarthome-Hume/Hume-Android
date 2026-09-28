package com.smarthome.hume.core.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import com.smarthome.hume.core.ui.theme.LocalHumeExtraColors
import androidx.compose.ui.unit.dp

/**
 * Switch M3E custom theo demo (.tgl):
 * 52x32, vien 2px outline, track surfaceHighest khi tat / primary khi bat;
 * knob 16dp -> 20dp + icon check (opacity/scale spring) khi bat;
 * active: knob scaleX 1.15.
 * (outlined-only icon, khong fill.)
 */
@Composable
fun M3ESwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val knobSize by animateDpAsState(
        if (checked) 20.dp else 16.dp,
        tween(300, easing = M3EMotion.spring),
        label = "swKnobSize",
    )
    // demo: tat (left 6, top 6) -> bat (left 24, top 4)
    val knobX by animateDpAsState(
        if (checked) 24.dp else 6.dp,
        tween(450, easing = M3EMotion.spring),
        label = "swKnobX",
    )
    val knobY by animateDpAsState(
        if (checked) 4.dp else 6.dp,
        tween(300, easing = M3EMotion.spring),
        label = "swKnobY",
    )
    val checkAlpha by animateFloatAsState(
        if (checked) 1f else 0f,
        tween(200),
        label = "swCheckAlpha",
    )
    val checkScale by animateFloatAsState(
        if (checked) 1f else 0.5f,
        tween(300, easing = M3EMotion.spring),
        label = "swCheckScale",
    )
    val pressSX by animateFloatAsState(
        if (pressed) 1.15f else 1f,
        tween(300, easing = M3EMotion.spring),
        label = "swPressSX",
    )

    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .size(52.dp, 32.dp)
            .clip(shape)
            .background(
                if (checked) MaterialTheme.colorScheme.primary
                else LocalHumeExtraColors.current.surfaceHighest
            )
            .border(
                2.dp,
                if (checked) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                shape,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = { onCheckedChange(!checked) },
            ),
    ) {
        Box(
            Modifier
                .offset(x = knobX, y = knobY)
                .size(knobSize)
                .graphicsLayer { scaleX = pressSX }
                .clip(CircleShape)
                .background(
                    if (checked) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.outline
                ),
            contentAlignment = Alignment.Center,
        ) {
            MsIcon(
                M3EIcons.Check,
                null,
                modifier = Modifier
                    .size(13.dp)
                    .scale(checkScale)
                    .alpha(checkAlpha),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
