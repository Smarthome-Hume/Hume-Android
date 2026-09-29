package com.smarthome.hume.feature.energy

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.model.EnergyFlowState
import com.smarthome.hume.core.ui.theme.LocalHumeExtraColors
import com.smarthome.hume.core.ui.components.M3ECard
import com.smarthome.hume.core.ui.components.M3EIcons
import com.smarthome.hume.core.ui.components.M3EMotion
import com.smarthome.hume.core.ui.components.blink
import com.smarthome.hume.core.ui.components.pressMorph
import com.smarthome.hume.core.ui.components.MsIcon
import java.util.Locale
import kotlin.math.roundToInt

private const val VB_W = 380f
private const val VB_H = 400f

private fun prodPath() = Path().apply {
    moveTo(156f, 62f); lineTo(166f, 62f); quadraticTo(182f, 62f, 182f, 78f); lineTo(182f, 168f)
}
private fun gridPath() = Path().apply {
    moveTo(198f, 168f); lineTo(198f, 78f); quadraticTo(198f, 62f, 214f, 62f); lineTo(224f, 62f)
}
private fun consPath() = Path().apply {
    moveTo(182f, 232f); lineTo(182f, 314f); quadraticTo(182f, 330f, 166f, 330f); lineTo(156f, 330f)
}
private fun battPath() = Path().apply {
    moveTo(198f, 232f); lineTo(198f, 314f); quadraticTo(198f, 330f, 214f, 330f); lineTo(224f, 330f)
}

/** Toc do sweep: 18/v giay, clamp 4-14s — v la kW (dung abs de gia tri am van co toc do). */
private fun sweepMs(powerKw: Double): Int =
    (minOf(14.0, maxOf(4.0, 18.0 / maxOf(0.15, kotlin.math.abs(powerKw)))) * 1000).roundToInt()

/**
 * Flow card M3E: 4 node + hub bolt o giua, sweep tren elbow track.
 * Port tu demo v4 rev12 (.flx).
 */
@Composable
fun EnergyFlowCard(
    flow: EnergyFlowState,
    ui: EnergyScreenUi,
    vm: EnergyViewModel,
    risePlayed: MutableSet<String>,
    modifier: Modifier = Modifier,
) {
    val charging = ui.battChargeOverride ?: flow.battCharging
    val soc = (flow.soc + ui.socDrift).coerceIn(5.0, 100.0)
    M3ECard(
        shape = RoundedCornerShape(32.dp),
        contentPadding = 0.dp,
        containerColor = LocalHumeExtraColors.current.surfaceHighest,
        modifier = modifier
            .riseOnce("sol-flow", 410, risePlayed)
            .shadow(12.dp, RoundedCornerShape(32.dp)),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 16.dp)) {
            // header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(
                        "Năng lượng",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    )
                    Text(
                        "Dòng chảy thời gian thực",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF22C55E))
                            .blink(1600),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        String.format(Locale.US, "%.1f", flow.todayKwh),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum",
                        ),
                    )
                    Text(
                        " kWh",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp, fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            FlowArea(flow = flow, charging = charging, soc = soc, vm = vm)
            // footer: divider border-top 1px outlineVariant (demo .flfoot)
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Spacer(Modifier.height(12.dp))
            val footStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
            val footColor = MaterialTheme.colorScheme.onSurfaceVariant
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    buildAnnotatedString {
                        append("Hôm nay sản xuất ")
                        withStyle(SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )) {
                            append("${String.format(Locale.US, "%.1f", flow.todayKwh)} kWh")
                        }
                    },
                    style = footStyle,
                    color = footColor,
                )
                Text(
                    buildAnnotatedString {
                        append("Tự dùng ")
                        withStyle(SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )) {
                            append("${flow.selfUsePct.roundToInt()}%")
                        }
                    },
                    style = footStyle,
                    color = footColor,
                )
            }
        }
    }
}

@Composable
private fun FlowArea(
    flow: EnergyFlowState,
    charging: Boolean,
    soc: Double,
    vm: EnergyViewModel,
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(VB_W / VB_H),
    ) {
        val w = maxWidth
        val h = maxHeight
        fun fx(x: Float): Dp = w * (x / VB_W)
        fun fy(y: Float): Dp = h * (y / VB_H)

        FlowTracks(flow = flow, charging = charging)

        // nodes: 150x160px trong viewBox 380x400, cach ria 6px
        val nw = fx(150f); val nh = fy(160f)
        FlowNode(
            icon = M3EIcons.SolarPower,
            tintBg = Color(0xFFF59E0B).copy(alpha = 0.16f),
            tintFg = Color(0xFFD97706),
            label = "Sản xuất", valueKw = flow.prodKw,
            modifier = Modifier
                .size(nw, nh)
                .offset(fx(6f), fy(6f)),
        ) {
            SegLegend(
                listOf("PV1" to MaterialTheme.colorScheme.primary,
                    "PV2" to MaterialTheme.colorScheme.tertiary),
            )
            SegBar(
                listOf(
                    (flow.pv1Kw / (flow.prodKw.coerceAtLeast(0.01))).toFloat() to
                        MaterialTheme.colorScheme.primary,
                    (flow.pv2Kw / (flow.prodKw.coerceAtLeast(0.01))).toFloat() to
                        MaterialTheme.colorScheme.tertiary,
                ),
            )
        }
        FlowNode(
            icon = M3EIcons.ElectricMeter,
            tintBg = Color(0xFF2F6EA3).copy(alpha = 0.16f),
            tintFg = Color(0xFF2F6EA3),
            label = "Lưới điện", valueKw = flow.gridKw,
            modifier = Modifier
                .size(nw, nh)
                .offset(fx(VB_W - 6f - 150f), fy(6f)),
        )
        FlowNode(
            icon = M3EIcons.Home,
            tintBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            tintFg = MaterialTheme.colorScheme.primary,
            label = "Tiêu thụ", valueKw = flow.consKw,
            modifier = Modifier
                .size(nw, nh)
                .offset(fx(6f), fy(VB_H - 6f - 160f)),
        ) {
            SegLegend(
                listOf("CB1" to MaterialTheme.colorScheme.primary,
                    "CB2" to MaterialTheme.colorScheme.tertiary,
                    "CB3" to MaterialTheme.colorScheme.secondary),
            )
            val tot = (flow.cb1Kw + flow.cb2Kw + flow.cb3Kw).coerceAtLeast(0.01)
            SegBar(
                listOf(
                    (flow.cb1Kw / tot).toFloat() to MaterialTheme.colorScheme.primary,
                    (flow.cb2Kw / tot).toFloat() to MaterialTheme.colorScheme.tertiary,
                    (flow.cb3Kw / tot).toFloat() to MaterialTheme.colorScheme.secondary,
                ),
            )
        }
        val battFg = Color(0xFF16A34A)
        FlowNode(
            icon = if (charging) M3EIcons.Battery else M3EIcons.BatteryFull,
            tintBg = battFg.copy(alpha = 0.16f),
            tintFg = battFg,
            label = "Pin", valueKw = flow.battKw,
            badge = if (charging) "Đang sạc" else "Đang xả",
            badgeBg = battFg.copy(alpha = 0.16f),
            badgeFg = battFg,
            modifier = Modifier
                .size(nw, nh)
                .offset(fx(VB_W - 6f - 150f), fy(VB_H - 6f - 160f)),
            onClick = { vm.toggleBattFlow() },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "SOC",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${soc.roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))
            // demo .flsocbar i{transition:width .6s ease}
            val socFrac by animateFloatAsState(
                targetValue = (soc / 100).toFloat().coerceIn(0f, 1f),
                animationSpec = tween(600),
                label = "flSoc",
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(socFrac)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF16A34A)),
                )
            }
        }

        // hub bolt 64px o giua + ping ring flping 2.2s (demo .flhub::before)
        val hub = fx(64f)
        val primary = MaterialTheme.colorScheme.primary
        val pingT = rememberInfiniteTransition(label = "flping")
        val pScale by pingT.animateFloat(
            initialValue = 1f,
            targetValue = 1.9f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 2200
                    1f at 0
                    1.9f at 1760 with LinearEasing
                    1.9f at 2200
                },
            ),
            label = "pingScale",
        )
        val pAlpha by pingT.animateFloat(
            initialValue = 0.7f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 2200
                    0.7f at 0
                    0f at 1760
                    0f at 2200
                },
            ),
            label = "pingAlpha",
        )
        Box(
            modifier = Modifier
                .size(hub)
                .offset(fx(VB_W / 2 - 32f), fy(VB_H / 2 - 32f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = pScale
                        scaleY = pScale
                        alpha = pAlpha
                    }
                    .border(2.dp, primary, CircleShape),
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            ) {
                MsIcon(
                    M3EIcons.Power, null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(hub * 0.47f),
                )
            }
        }
    }
}

@Composable
private fun FlowTracks(flow: EnergyFlowState, charging: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val transition = rememberInfiniteTransition(label = "flsweep")
    data class Track(val path: Path, val powerKw: Double, val reverse: Boolean)
    val tracks = listOf(
        Track(prodPath(), flow.prodKw, false),
        // gridPath huong hub->node; khi grid > 0 (mua dien) dong chay nguoc lai node->hub
        Track(gridPath(), flow.gridKw, flow.gridKw > 0.005),
        Track(consPath(), flow.consKw, false),
        Track(battPath(), flow.battKw, !charging),
    )
    val phases = tracks.mapIndexed { idx, t ->
        // demo: chi set lai --dur khi toc do lech >12% de tranh restart animation
        var dur by remember(idx) { mutableIntStateOf(sweepMs(t.powerKw)) }
        val ms = sweepMs(t.powerKw)
        if (kotlin.math.abs(ms - dur) / dur.toFloat() > 0.12f) dur = ms
        val target = if (t.reverse) 1270f else -1270f
        transition.animateFloat(
            initialValue = 0f, targetValue = target,
            animationSpec = infiniteRepeatable(
                animation = tween(dur, easing = LinearEasing),
            ),
            label = "sweep",
        )
    }
    Canvas(Modifier.fillMaxSize()) {
        val sx = size.width / VB_W
        val sy = size.height / VB_H
        tracks.forEachIndexed { i, t ->
            val path = Path().apply {
                addPath(t.path)
                transform(Matrix().apply { scale(sx, sy) })
            }
            drawPath(
                path = path,
                color = trackColor.copy(alpha = 0.45f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            val dash = PathEffect.dashPathEffect(floatArrayOf(70f * sx, 1200f * sx), phases[i].value * sx)
            // glow underlay (demo: drop-shadow(0 0 7px primary 70%) tren net sweep 5px)
            drawPath(
                path = path, color = primary.copy(alpha = 0.5f),
                style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round, pathEffect = dash),
            )
            drawPath(
                path = path, color = primary,
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round, pathEffect = dash),
            )
        }
    }
}

@Composable
private fun FlowNode(
    icon: String,
    tintBg: Color,
    tintFg: Color,
    label: String,
    valueKw: Double,
    modifier: Modifier = Modifier,
    badge: String? = null,
    badgeBg: Color = Color.Transparent,
    badgeFg: Color = Color.Transparent,
    onClick: (() -> Unit)? = null,
    bottom: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .pressMorph(pressedScale = 0.93f, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(tintBg),
        ) {
            MsIcon(icon, null, tint = tintFg, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 10.5.sp, fontWeight = FontWeight.Normal),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            if (badge != null) {
                Spacer(Modifier.width(5.dp))
                Text(
                    badge,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp, fontWeight = FontWeight.Bold),
                    color = badgeFg,
                    // demo .flbdir{white-space:nowrap}: badge khong duoc xuong dong
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(badgeBg)
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                )
            }
        }
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(top = 1.dp),
        ) {
            Text(
                String.format(Locale.US, "%.1f", valueKw),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum",
                ),
                maxLines = 1,
                softWrap = false,
            )
            Text(
                " kW",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp, fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
            )
        }
        Spacer(Modifier.weight(1f))
        bottom()
    }
}

@Composable
private fun SegLegend(items: List<Pair<String, Color>>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (name, c) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(c))
                Spacer(Modifier.width(3.dp))
                Text(
                    name,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
    Spacer(Modifier.height(5.dp))
}

@Composable
private fun SegBar(segs: List<Pair<Float, Color>>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        segs.forEach { (frac, c) ->
            // demo .flsegbar i{transition:width .8s emphasized}
            val w by animateFloatAsState(
                targetValue = frac.coerceAtLeast(0.05f),
                animationSpec = tween(800, easing = M3EMotion.emphasized),
                label = "segW",
            )
            Box(
                modifier = Modifier
                    .weight(w)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(c),
            )
        }
    }
}
