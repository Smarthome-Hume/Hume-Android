package com.smarthome.hume.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.style.TextOverflow
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
    // Two-phase collapse (2026-09-30, fix nhay + khung khi tu thu gon).
    // Video frame-by-frame (t=8.44->8.47, 1 frame): khi securityExpanded=false,
    // cung 1 frame Row mat horizontalScroll + 2 pill widthIn->weight(1f) trong
    // khi cum mode exit van chiem ~400dp layout -> 2 pill bi don ve ~6dp
    // (bien mat), cum mode nhay trai ~150dp = "nhay"; roi treo fade 300ms =
    // "khung". -> compact chi bat SAU khi exit xong (370ms); width 2 pill
    // animate muot 150/128 <-> nua man hinh bang animateDpAsState, khong
    // snap frame nao. Chieu MO cung het snap (truoc day mo la weight->min-width
    // ngay lap tuc).
    var compact by remember { mutableStateOf(!securityExpanded) }
    LaunchedEffect(securityExpanded) {
        if (securityExpanded) compact = false
        else { delay(370); compact = true }
    }
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val halfPill = (maxWidth - 10.dp) / 2
        val secW by animateDpAsState(
            targetValue = if (compact) halfPill else 150.dp,
            animationSpec = tween(300, easing = M3EMotion.emphasized),
            label = "secPillW",
        )
        val bulbW by animateDpAsState(
            targetValue = if (compact) halfPill else 128.dp,
            animationSpec = tween(300, easing = M3EMotion.emphasized),
            label = "bulbPillW",
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    // Khi mo rong (compact=false, ca trong luc exit dang chay):
                    // giu horizontalScroll + width min de layout on dinh
                    if (!compact) Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 2.dp)
                    else Modifier,
                ),
        ) {
            SecPill(
                mode = mode,
                onClick = onToggleSecurity,
                // Chieu cao co dinh = SecModeCard (80dp) de 3 the bang nhau khi mo rong
                modifier = Modifier.widthIn(min = secW).height(80.dp),
            )
        // .secmodes: chi hien khi expanded. Exit shrink layout width
        // (fadeOut + shrinkHorizontally) de cum mode thu dan 400->0dp;
        // nho two-phase o tren, Row van giu layout expanded trong luc exit
        // nen 2 pill khong bi don width.
        AnimatedVisibility(
            visible = securityExpanded,
            enter = fadeIn(tween(450, easing = M3EMotion.emphasized)),
            exit = fadeOut(tween(300)) + shrinkHorizontally(
                animationSpec = tween(350, easing = M3EMotion.emphasizedAcc),
                shrinkTowards = Alignment.Start,
            ),
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
                modifier = Modifier.widthIn(min = bulbW),
            )
        }
    }
}

/** .pill.secpill: nen surfaceHighest co dinh; chi .pic/icon/ps doi theo mode.
 * :active = scale(.93) + bo 28->18 + nen primaryContainer (nhu demo).
 * (2026-09-30) revert animateContentSize: no canh tranh voi exit animation
 * cua secmodes + horizontalScroll, gay remeasure moi frame khi scroll. */
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
            // Can giua doc de chu khong bi don xuong day
            verticalArrangement = Arrangement.Center,
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
                // Padding can doi 2 dau + gap nho: 10+24+6+16+10=66 < 80, chu khong bi cat
                .padding(horizontal = 10.dp, vertical = 10.dp),
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
            Spacer(Modifier.height(6.dp))
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
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
