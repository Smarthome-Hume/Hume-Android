package com.smarthome.hume.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.model.AlarmUi
import com.smarthome.hume.core.ui.theme.LocalHumeExtraColors
import com.smarthome.hume.core.ui.components.M3EIcons
import com.smarthome.hume.core.ui.components.M3EMotion
import com.smarthome.hume.core.ui.components.pressMorph
import com.smarthome.hume.core.ui.components.rememberHaptic
import com.smarthome.hume.core.ui.components.MsIcon
import kotlinx.coroutines.delay

/**
 * Cum pills trang Nha theo demo v4 (.pills/.pill/.secmodes/.smode):
 * [secPill "An ninh"] [bulbPill "n bong dang sang"]; bam secPill mo rong
 * thanh hang scroll-x (easing spring, khong swap cung).
 */
@Composable
fun PillsRow(
    alarm: AlarmUi?,
    lightsOnCount: Int,
    onArm: (mode: String, label: String) -> Unit,
    onDisarm: () -> Unit,
    onLights: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val haptic = rememberHaptic()

    AnimatedContent(
        targetState = expanded,
        transitionSpec = {
            (fadeIn(tween(450, easing = M3EMotion.emphasized)) +
                expandHorizontally(
                    animationSpec = tween(450, easing = M3EMotion.spring),
                    expandFrom = Alignment.Start,
                )) togetherWith
                (fadeOut(tween(300)) +
                    shrinkHorizontally(
                        animationSpec = tween(450, easing = M3EMotion.spring),
                        shrinkTowards = Alignment.Start,
                    )) using SizeTransform(clip = false)
        },
        label = "pills",
        modifier = modifier,
    ) { ex ->
        if (!ex) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                SecPill(
                    alarm = alarm,
                    onClick = { haptic(); expanded = true },
                    modifier = Modifier.weight(1f),
                )
                BulbPill(
                    count = lightsOnCount,
                    onClick = { haptic(); onLights() },
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                SecPill(
                    alarm = alarm,
                    onClick = { haptic(); expanded = false },
                    modifier = Modifier.width(150.dp),
                )
                SecurityModes(alarm = alarm, onArm = onArm, onDisarm = onDisarm)
                BulbPill(
                    count = lightsOnCount,
                    onClick = { haptic(); onLights() },
                    modifier = Modifier.width(128.dp),
                )
            }
        }
    }
}

@Composable
private fun SecPill(
    alarm: AlarmUi?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extra = LocalHumeExtraColors.current
    val armed = alarm?.isArmed == true
    PillShell(onClick = onClick, modifier = modifier) {
        PillIcon(
            icon = M3EIcons.Shield,
            container = if (armed) extra.successContainer
            else MaterialTheme.colorScheme.surfaceContainer,
            tint = if (armed) extra.onSuccessContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PillTexts(
            title = "An ninh",
            sub = alarm?.label ?: "Chưa rõ",
        )
    }
}

@Composable
private fun BulbPill(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    PillShell(onClick = onClick, modifier = modifier) {
        PillIcon(
            icon = M3EIcons.Light,
            container = cs.tertiaryContainer,
            tint = cs.onTertiaryContainer,
        )
        PillTexts(
            title = if (count > 0) "$count bóng" else "Không có",
            sub = if (count > 0) "Đang sáng" else "Đèn tắt",
        )
    }
}

/**
 * .pill: surfaceHighest, bo 28px, padding 14px, gap 11px.
 * :active{scale(.93); radius 18px; bg primaryContainer} spring.
 */
@Composable
private fun PillShell(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = tween(300, easing = M3EMotion.spring),
        label = "pillScale",
    )
    val radius by animateDpAsState(
        targetValue = if (pressed) 18.dp else 28.dp,
        animationSpec = tween(450, easing = M3EMotion.spring),
        label = "pillRadius",
    )
    val bg by animateColorAsState(
        targetValue = if (pressed) cs.primaryContainer else cs.surfaceContainerHighest,
        animationSpec = tween(300),
        label = "pillBg",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(radius))
            .background(bg)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(14.dp),
    ) {
        content()
    }
}

@Composable
private fun PillIcon(
    icon: String,
    container: androidx.compose.ui.graphics.Color,
    tint: androidx.compose.ui.graphics.Color,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(container),
    ) {
        MsIcon(
            icon, contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun PillTexts(title: String, sub: String) {
    Column {
        Text(
            title,
            fontSize = 14.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            sub,
            fontSize = 12.sp, fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 1.dp),
        )
    }
}

private data class AlarmMode(val service: String, val label: String)

/**
 * 4 che do an ninh (.smode): rong 92px, bo 26px, padding 14px 10px,
 * icon 24px, nhan 12px/700; chon = primaryContainer;
 * vao: smIn .45s spring + stagger .06/.12/.18s; :active scale(.92).
 */
@Composable
private fun SecurityModes(
    alarm: AlarmUi?,
    onArm: (mode: String, label: String) -> Unit,
    onDisarm: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val haptic = rememberHaptic()
    val density = LocalDensity.current
    val modes = listOf(
        AlarmMode("home", "Ở nhà") to M3EIcons.Home,
        AlarmMode("away", "Vắng nhà") to M3EIcons.FlightTakeoff,
        AlarmMode("night", "Ban đêm") to M3EIcons.Bedtime,
        AlarmMode("disarm", "Tắt") to M3EIcons.PowerSettingsNew,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        modes.forEachIndexed { idx, (m, icon) ->
            val selected = when (m.service) {
                "home" -> alarm?.state == "armed_home"
                "away" -> alarm?.state == "armed_away"
                "night" -> alarm?.state == "armed_night"
                else -> alarm?.state == "disarmed"
            }
            var vis by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(idx * 60L)
                vis = true
            }
            AnimatedVisibility(
                visible = vis,
                enter = fadeIn(tween(450, easing = M3EMotion.spring)) +
                    androidx.compose.animation.slideInHorizontally(
                        animationSpec = tween(450, easing = M3EMotion.spring),
                    ) { with(density) { 18.dp.roundToPx() } } +
                    scaleIn(
                        animationSpec = tween(450, easing = M3EMotion.spring),
                        initialScale = 0.9f,
                    ),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .width(92.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            if (selected) cs.primaryContainer
                            else cs.surfaceContainerHighest,
                        )
                        .pressMorph(pressedScale = 0.92f) {
                            haptic()
                            if (m.service == "disarm") onDisarm() else onArm(m.service, m.label)
                        }
                        .padding(horizontal = 10.dp, vertical = 14.dp),
                ) {
                    MsIcon(
                        icon, contentDescription = null,
                        tint = if (selected) cs.onPrimaryContainer
                        else cs.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        m.label,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        color = if (selected) cs.onPrimaryContainer
                        else cs.onSurface,
                    )
                }
            }
        }
    }
}
