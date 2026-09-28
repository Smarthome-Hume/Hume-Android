package com.smarthome.hume.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.model.AlarmUi
import com.smarthome.hume.core.model.domain.SecurityMode
import com.smarthome.hume.core.ui.components.M3EIcons
import com.smarthome.hume.core.ui.components.M3EMotion
import com.smarthome.hume.core.ui.components.Ms
import com.smarthome.hume.core.ui.components.MsIcon
import com.smarthome.hume.core.ui.components.pressMorph
import com.smarthome.hume.core.ui.components.rememberHaptic
import com.smarthome.hume.core.ui.theme.LocalHumeExtraColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Hang pills trang Nha theo demo rev12 (.pills/.pill/.secmodes/.smode):
 * - .pills{gap:10px;mb:14px}: [secPill .pill][secmodes][bulbPill .pill].
 * - .pill: flex:1, surfaceHighest, bo 28px, padding 14px, gap 11px;
 *   .pic 44px tron; .pl 14px/700; .ps 12px/500.
 * - SecPill doi theo mode (chi .pic + icon + ps): CFG nhu demo.
 * - Expanded (.pills.secon): ca hang scroll-x; secpill min-width 150px,
 *   bulbPill min-width 128px; .secmodes{gap:10px}.
 * - .smode: card DOC 92px, bo 26px, padding 14px 10px, gap 12px;
 *   icon 24px + nhan 12px/700; chon = primaryContainer;
 *   vao: smIn (translateX 18px + scale .9, .45s spring), stagger 0/.06/.12/.18s;
 *   :active scale(.92). Chon xong tu thu gon sau 1000ms.
 */
@Composable
fun PillsRow(
    alarm: AlarmUi?,
    lightsOnCount: Int,
    securityExpanded: Boolean,
    onToggleSecurity: () -> Unit,
    onAutoCollapse: () -> Unit,
    onArm: (mode: String, label: String) -> Unit,
    onDisarm: () -> Unit,
    onLights: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = rememberHaptic()
    val mode = when (alarm?.state) {
        "armed_home" -> SecurityMode.Home
        "armed_away" -> SecurityMode.Away
        "armed_night" -> SecurityMode.Night
        else -> SecurityMode.Off
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (securityExpanded) Modifier.horizontalScroll(rememberScrollState())
                else Modifier,
            ),
    ) {
        SecPill(
            mode = mode,
            onClick = onToggleSecurity,
            // Chieu cao co dinh = SecModeCard (80dp) de 3 the bang nhau khi mo rong
            modifier = (if (securityExpanded) Modifier.widthIn(min = 150.dp)
            else Modifier.weight(1f)).height(80.dp),
        )
        // .secmodes: chi hien khi expanded
        AnimatedVisibility(
            visible = securityExpanded,
            enter = fadeIn(tween(450, easing = M3EMotion.emphasized)),
            exit = fadeOut(tween(300)),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SecurityMode.entries.forEachIndexed { i, m ->
                    SecModeCard(
                        mode = m,
                        selected = m == mode,
                        entranceDelay = i * 60,
                        onClick = {
                            haptic()
                            if (m == SecurityMode.Off) onDisarm()
                            else onArm(
                                when (m) {
                                    SecurityMode.Home -> "home"
                                    SecurityMode.Away -> "away"
                                    else -> "night"
                                },
                                when (m) {
                                    SecurityMode.Home -> "Ở nhà"
                                    SecurityMode.Away -> "Vắng nhà"
                                    else -> "Ban đêm"
                                },
                            )
                        },
                        onAutoCollapse = onAutoCollapse,
                    )
                }
            }
        }
        BulbPill(
            count = lightsOnCount,
            onClick = onLights,
            modifier = if (securityExpanded) Modifier.widthIn(min = 128.dp)
            else Modifier.weight(1f),
        )
    }
}

/** .pill.secpill: nen surfaceHighest co dinh; chi .pic/icon/ps doi theo mode.
 * :active = scale(.93) + bo 28->18 + nen primaryContainer (nhu demo). */
@Composable
private fun SecPill(
    mode: SecurityMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val extra = LocalHumeExtraColors.current
    val haptic = rememberHaptic()
    val (icon, picBg, picFg, ps) = when (mode) {
        SecurityMode.Home -> Quad(
            M3EIcons.Shield, extra.successContainer,
            extra.onSuccessContainer, "Đang bật")
        SecurityMode.Away -> Quad(
            M3EIcons.FlightTakeoff, cs.tertiaryContainer,
            cs.onTertiaryContainer, "Vắng nhà · đã khóa")
        SecurityMode.Night -> Quad(
            M3EIcons.Bedtime, cs.secondaryContainer,
            cs.onSecondaryContainer, "Ban đêm · giám sát")
        SecurityMode.Off -> Quad(
            M3EIcons.PowerSettingsNew, cs.surfaceContainer,
            cs.onSurfaceVariant, "Đã tắt")
    }
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
        targetValue = if (pressed) cs.primaryContainer else extra.surfaceHighest,
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
            ) {
                haptic()
                onClick()
            }
            .padding(14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(picBg),
        ) {
            MsIcon(icon, null, tint = picFg, modifier = Modifier.size(24.dp))
        }
        Column {
            Text(
                "An ninh",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = cs.onSurface,
            )
            Text(
                ps,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = cs.onSurfaceVariant,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
    }
}

/** .pill#bulbPill: khong co chevron trong HTML; :active giong secpill. */
@Composable
private fun BulbPill(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val extra = LocalHumeExtraColors.current
    val haptic = rememberHaptic()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f,
        animationSpec = tween(300, easing = M3EMotion.spring),
        label = "bulbScale",
    )
    val radius by animateDpAsState(
        targetValue = if (pressed) 18.dp else 28.dp,
        animationSpec = tween(450, easing = M3EMotion.spring),
        label = "bulbRadius",
    )
    val bg by animateColorAsState(
        targetValue = if (pressed) cs.primaryContainer else extra.surfaceHighest,
        animationSpec = tween(300),
        label = "bulbBg",
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
            ) {
                haptic()
                onClick()
            }
            .padding(14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(cs.tertiaryContainer),
        ) {
            MsIcon(
                M3EIcons.Light, null,
                tint = cs.onTertiaryContainer,
                modifier = Modifier.size(24.dp),
            )
        }
        Column {
            Text(
                if (count > 0) "$count bóng" else "Không có",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = cs.onSurface,
            )
            Text(
                if (count > 0) "Đang sáng" else "Đèn tắt",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = cs.onSurfaceVariant,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
    }
}

/**
 * .smode: card doc 92px; chon = primaryContainer; :active scale(.92);
 * vao smIn .45s spring + stagger; chon xong tu thu gon sau 1000ms.
 */
@Composable
private fun SecModeCard(
    mode: SecurityMode,
    selected: Boolean,
    entranceDelay: Int,
    onClick: () -> Unit,
    onAutoCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val extra = LocalHumeExtraColors.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var vis by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(entranceDelay.toLong())
        vis = true
    }
    AnimatedVisibility(
        visible = vis,
        // smIn: translateX(18px) + scale(.9) + fade, .45s spring
        enter = fadeIn(tween(450, easing = M3EMotion.emphasized)) +
            slideInHorizontally(
                animationSpec = tween(450, easing = M3EMotion.spring),
            ) { with(density) { 18.dp.roundToPx() } } +
            scaleIn(
                animationSpec = tween(450, easing = M3EMotion.spring),
                initialScale = 0.9f,
            ),
        exit = fadeOut(tween(200)),
    ) {
        val fg = if (selected) cs.onPrimaryContainer else cs.onSurfaceVariant
        val labelColor = if (selected) cs.onPrimaryContainer else cs.onSurface
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = modifier
                .width(92.dp)
                .height(80.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(if (selected) cs.primaryContainer else extra.surfaceHighest)
                .pressMorph(
                    pressedScale = 0.92f,
                    onClick = {
                        onClick()
                        scope.launch {
                            delay(1000)
                            onAutoCollapse()
                        }
                    },
                )
                .padding(horizontal = 10.dp, vertical = 14.dp),
        ) {
            MsIcon(
                when (mode) {
                    SecurityMode.Home -> Ms.home
                    SecurityMode.Away -> M3EIcons.FlightTakeoff
                    SecurityMode.Night -> M3EIcons.Bedtime
                    SecurityMode.Off -> M3EIcons.PowerSettingsNew
                },
                null,
                tint = fg,
                modifier = Modifier.size(24.dp),
            )
            Text(
                when (mode) {
                    SecurityMode.Home -> "Ở nhà"
                    SecurityMode.Away -> "Vắng nhà"
                    SecurityMode.Night -> "Ban đêm"
                    SecurityMode.Off -> "Tắt"
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = labelColor,
            )
        }
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
