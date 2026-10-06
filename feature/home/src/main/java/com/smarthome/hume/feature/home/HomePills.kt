package com.smarthome.hume.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
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

private val CompactPillHorizontalInset = 12.dp
private val CompactPillVerticalInset = 10.dp
private val CompactPillIconSlot = 36.dp
private val CompactPillContentGap = 8.dp
private val ExpandedPillInset = 6.dp
private val ExpandedPillIconSlot = 20.dp
private val ExpandedPillContentGap = 4.dp

/**
 * Hang pills trang Nha theo demo rev12 (.pills/.pill/.secmodes/.smode):
 * - .pills{gap:10px;mb:14px}: [secPill .pill][secmodes][bulbPill .pill].
 * - .pill: flex:1, surfaceHighest, bo 28px, padding 14px, gap 11px;
 *   .pic 44px tron; .pl 14px/700; .ps 12px/500.
 * - SecPill doi theo mode (chi .pic + icon + ps): CFG nhu demo.
 * - Expanded (.pills.secon): ca hang scroll-x; secpill 100dp, bulb 84dp,
 *   cung inset 6dp/icon slot 20dp/text gap 4dp/vertical padding 6dp.
 * - Compact: hai pill toi da 136dp, neo vao hai mep trong; label dai ellipsis.
 *   Cac card mode giu 88x60dp; .secmodes{gap:10px}.
 * - .smode: card 88x60dp, bo 26px, padding deu 8dp, gap 4dp;
 *   icon 20dp + nhan 11sp; chon = primaryContainer;
 *   vao smIn + stagger, :active scale(.92),
 *   chon xong tu thu gon sau 1000ms.
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
    var idleActivityRevision by remember { mutableIntStateOf(0) }
    var securityInteractionActive by remember { mutableStateOf(false) }
    var modeSelectionPending by remember { mutableStateOf(false) }
    val latestOnAutoCollapse by rememberUpdatedState(onAutoCollapse)
    LaunchedEffect(securityExpanded) {
        if (!securityExpanded) modeSelectionPending = false
    }
    // The effect is cancelled when collapsed or removed from composition.
    // Pointer activity pauses the timeout until release, then starts a fresh 2s.
    LaunchedEffect(
        securityExpanded,
        idleActivityRevision,
        securityInteractionActive,
        modeSelectionPending,
    ) {
        if (!securityExpanded || securityInteractionActive || modeSelectionPending) {
            return@LaunchedEffect
        }
        delay(2_000)
        latestOnAutoCollapse()
    }
    val haptic = rememberHaptic()
    val mode = when (alarm?.state) {
        "armed_home" -> SecurityMode.Home
        "armed_away" -> SecurityMode.Away
        "armed_night" -> SecurityMode.Night
        else -> SecurityMode.Off
    }
    // Chuoi va cham khi thu gon (port iOS HomePills.swift compactTask, 2026-10-04).
    // Phase 1 (0-300ms): cum secmodes exit (fadeOut 300 + shrink 350) -> bulb
    //   pill truot ve sat pill an ninh nho layout tu dich chuyen.
    // Tai diem sat nhat (t=300ms): NEN bulbSquash 1.0 -> 0.82 trong 120ms
    //   easeIn, nhu dap vao tuong (fire-and-forget de Phase 2 bat dau dung
    //   t=700ms nhu iOS).
    // Phase 2 (t=700ms): compact=true DONG THOI dan squash ve 1.0 bang
    //   spring 1.2s (tween 1200 + M3EMotion.spring, co overshoot = nay) ->
    //   pill bay ve phai vua dan vua nay, ve toi noi la vua het nay.
    // Mo rong: compact=false ngay + squash ve 1.0; doi lai giua chung thi
    //   LaunchedEffect restart se huy chuoi dang chay (nhu iOS cancel task).
    var compact by remember { mutableStateOf(!securityExpanded) }
    var wasExpanded by remember { mutableStateOf(securityExpanded) }
    val bulbSquashAnim = remember { Animatable(1f) }
    LaunchedEffect(securityExpanded) {
        val collapsing = wasExpanded && !securityExpanded
        wasExpanded = securityExpanded
        if (securityExpanded) {
            compact = false
            launch {
                bulbSquashAnim.animateTo(
                    1f, tween(300, easing = M3EMotion.spring),
                )
            }
        } else if (collapsing) {
            delay(300)
            launch {
                bulbSquashAnim.animateTo(
                    0.82f, tween(120, easing = FastOutLinearInEasing),
                )
            }
            delay(400)
            compact = true
            bulbSquashAnim.animateTo(
                1f, tween(1200, easing = M3EMotion.spring),
            )
        } else {
            // Lan dau composition da o trang thai dong: khong chay chuoi
            compact = true
        }
    }
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val halfPill = (maxWidth - 10.dp) / 2
        val compactPillWidth = minOf(halfPill, 136.dp)
        val secW by animateDpAsState(
            targetValue = if (compact) compactPillWidth else 100.dp,
            animationSpec = tween(300, easing = M3EMotion.emphasized),
            label = "secPillW",
        )
        // Expanded gon theo noi dung; compact dung cung mot be rong toi da 136dp.
        val bulbW by animateDpAsState(
            targetValue = if (compact) compactPillWidth else 84.dp,
            animationSpec = tween(300, easing = M3EMotion.emphasized),
            label = "bulbPillW",
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (compact) {
                Arrangement.SpaceBetween
            } else {
                Arrangement.spacedBy(10.dp)
            },
            modifier = Modifier
                .then(
                    // Preserve bdfd18c full-bleed Security row while keeping the
                    // redesigned compact/expanded pill grouping intact.
                    if (!compact) Modifier
                        .offset(x = (-18).dp)
                        .width(maxWidth + 36.dp)
                        .horizontalScroll(rememberScrollState())
                    else Modifier.fillMaxWidth(),
                ),
        ) {
            // This wrapper deliberately excludes the adjacent Light pill.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (compact) Arrangement.Start
                else Arrangement.spacedBy(10.dp),
                modifier = Modifier.pointerInput(securityExpanded) {
                    if (securityExpanded) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            if (!securityInteractionActive) {
                                securityInteractionActive = true
                                idleActivityRevision++
                            }
                            try {
                                var pointerPressed: Boolean
                                do {
                                    val event = awaitPointerEvent()
                                    pointerPressed = event.changes.any { it.pressed }
                                } while (pointerPressed)
                            } finally {
                                securityInteractionActive = false
                                idleActivityRevision++
                            }
                        }
                    }
                },
            ) {
                SecPill(
                    mode = mode,
                    onClick = onToggleSecurity,
                    expanded = !compact,
                    // Chieu cao co dinh 60dp de pill thu gon canh card expanded.
                    modifier = Modifier.width(secW).height(60.dp),
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
                                    // Keep the existing 1s mode-selection collapse;
                                    // cancel the idle timer so the two cannot race.
                                    modeSelectionPending = true
                                    idleActivityRevision++
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
        }
            BulbPill(
                count = lightsOnCount,
                onClick = onLights,
                // He so squash tu chuoi va cham khi thu gon (port iOS bulbSquash)
                squash = bulbSquashAnim.value,
                expanded = !compact,
                modifier = Modifier.width(bulbW).height(60.dp),
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
    expanded: Boolean = false,
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
            M3EIcons.Shield, cs.surfaceContainer,
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
    val pillModifier = modifier
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

    if (expanded) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ExpandedPillContentGap),
            modifier = pillModifier.padding(ExpandedPillInset),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(ExpandedPillIconSlot)
                    .clip(CircleShape)
                    .background(picBg),
            ) {
                MsIcon(icon, null, tint = picFg, modifier = Modifier.size(16.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "An ninh",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface,
                )
                Text(
                    ps,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        letterSpacing = 0.sp,
                    ),
                    fontWeight = FontWeight.Medium,
                    color = cs.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
    } else {
        CompactPillContent(
            title = "An ninh",
            status = ps,
            icon = icon,
            iconBackground = picBg,
            iconTint = picFg,
            modifier = pillModifier,
        )
    }
}

@Composable
private fun CompactPillContent(
    title: String,
    status: String,
    icon: Any,
    iconBackground: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CompactPillContentGap),
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = CompactPillHorizontalInset,
                vertical = CompactPillVerticalInset,
            ),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(CompactPillIconSlot)
                .clip(CircleShape)
                .background(iconBackground),
        ) {
            MsIcon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                status,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 1.dp),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** .pill#bulbPill: khong co chevron trong HTML; :active giong secpill.
 *
 * Port iOS PillPressStyle + bulbSquash (2026-10-04):
 * - Nhan: scaleX = scaleY = 0.93 (ghi de squash, nhu iOS p ? 0.93).
 * - Khong nhan: scaleX theo [squash], scaleY bu Poisson
 *   1 + (1 - squash) * 0.5 (nen ngang -> phinh doc).
 * - Nen pill ve bang [SquashPillShape]: khi squash < 1.0, canh tren/duoi
 *   cong loi bang bezier (hieu ung cao su).
 * - Noi dung can giua pill (iOS: frame maxWidth .infinity, center).
 */
@Composable
private fun BulbPill(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    squash: Float = 1f,
    expanded: Boolean = false,
) {
    val cs = MaterialTheme.colorScheme
    val extra = LocalHumeExtraColors.current
    val haptic = rememberHaptic()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scaleX by animateFloatAsState(
        targetValue = if (pressed) 0.93f else squash,
        animationSpec = tween(300, easing = M3EMotion.spring),
        label = "bulbScaleX",
    )
    val scaleY by animateFloatAsState(
        targetValue = if (pressed) 0.93f else 1f + (1f - squash) * 0.5f,
        animationSpec = tween(450, easing = M3EMotion.spring),
        label = "bulbScaleY",
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
    // iOS: khi nhan thi shape squash reset ve 1.0 (p ? 1.0 : squash)
    val shapeSquash = if (pressed) 1f else squash
    val cardModifier = modifier
        .graphicsLayer(scaleX = scaleX, scaleY = scaleY)
        .clip(SquashPillShape(squash = shapeSquash, cornerRadius = radius))
        .background(bg)
        .clickable(
            interactionSource = interaction,
            indication = null,
        ) {
            haptic()
            onClick()
        }

    if (expanded) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ExpandedPillContentGap),
            modifier = cardModifier.padding(ExpandedPillInset),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(ExpandedPillIconSlot),
            ) {
                MsIcon(
                    Ms.lightbulb,
                    null,
                    tint = if (count > 0) cs.onSurface else cs.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                if (count > 0) "$count bóng bật" else "Đèn tắt",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    letterSpacing = 0.sp,
                ),
                fontWeight = FontWeight.SemiBold,
                color = if (count > 0) cs.onSurface else cs.onSurfaceVariant,
                textAlign = TextAlign.Start,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    } else {
        CompactPillContent(
            title = if (count > 0) "$count bóng" else "Không có",
            status = if (count > 0) "Đang sáng" else "Đèn tắt",
            icon = Ms.lightbulb,
            iconBackground = cs.tertiaryContainer,
            iconTint = cs.onTertiaryContainer,
            modifier = cardModifier,
        )
    }
}

/**
 * .smode: card 88x60dp; chon = primaryContainer; :active scale(.92);
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
                .width(88.dp)
                .height(60.dp)
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
                // Dem deu 8dp ca bon mep de noi dung khop light-detail tile.
                .padding(8.dp),
        ) {
            MsIcon(
                when (mode) {
                    SecurityMode.Home -> Ms.home
                    SecurityMode.Away -> M3EIcons.FlightTakeoff
                    SecurityMode.Night -> M3EIcons.Bedtime
                    SecurityMode.Off -> M3EIcons.Shield
                },
                null,
                tint = fg,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                when (mode) {
                    SecurityMode.Home -> "Ở nhà"
                    SecurityMode.Away -> "Vắng nhà"
                    SecurityMode.Night -> "Ban đêm"
                    SecurityMode.Off -> "Tắt"
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    letterSpacing = 0.sp,
                ),
                fontWeight = FontWeight.SemiBold,
                color = labelColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

/**
 * Shape pill bien dang theo he so squash (port iOS SquashPillShape,
 * 2026-10-04) — mo phong cao su:
 * - squash = 1.0: pill binh thuong, bo goc = [cornerRadius].
 * - squash < 1.0: nen ngang + phinh doc (Poisson): canh tren/duoi cong loi
 *   bang bezier, ban kinh goc giam nhe de khong bi tu.
 */
private class SquashPillShape(
    private val squash: Float,
    private val cornerRadius: Dp,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val w = size.width
        val h = size.height
        val k = squash.coerceIn(0f, 1f)
        // Do phinh doc (Poisson): cang nen cang phinh
        val bulge = (1f - k) * h * 0.25f
        // Ban kinh goc: giu nguyen khi binh thuong, giam nhe khi nen
        val cr = with(density) { cornerRadius.toPx() } * (0.7f + 0.3f * k)
        val midX = w / 2f
        // Ve pill: bat dau giua canh trai, di theo chieu kim dong ho
        val path = Path().apply {
            // Goc trai-tren
            moveTo(cr, 0f)
            // Canh tren: cong loi len khi bi nen
            quadraticBezierTo(midX, -bulge * 2f, w - cr, 0f)
            // Goc phai-tren
            quadraticBezierTo(w, 0f, w, cr)
            // Canh phai: thang dung
            lineTo(w, h - cr)
            // Goc phai-duoi
            quadraticBezierTo(w, h, w - cr, h)
            // Canh duoi: cong loi xuong khi bi nen
            quadraticBezierTo(midX, h + bulge * 2f, cr, h)
            // Goc trai-duoi
            quadraticBezierTo(0f, h, 0f, h - cr)
            // Canh trai: thang dung
            lineTo(0f, cr)
            // Goc trai-tren (dong)
            quadraticBezierTo(0f, 0f, cr, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}
