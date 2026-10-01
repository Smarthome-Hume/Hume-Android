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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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
import com.smarthome.hume.core.ui.components.HorizontalBatteryIcon
import com.smarthome.hume.core.ui.components.M3ECard
import com.smarthome.hume.core.ui.components.M3EIcons
import com.smarthome.hume.core.ui.components.M3EMotion
import com.smarthome.hume.core.ui.components.blink
import com.smarthome.hume.core.ui.components.pressMorph
import com.smarthome.hume.core.ui.components.MsIcon
import com.smarthome.hume.core.ui.components.toSmartPowerParts
import java.util.Locale
import kotlin.math.roundToInt

private const val VB_W = 380f
private const val VB_H = 430f
// Node 150 rong de mep trong cach xa hub 64px: kenh line 68px cho elbow
// thoang (2026-09-30, user: node ap sat inverter lam line bi bop).
// Giam chieu cao node 190->175 (2026-10-01, user: the can doi hon).
private const val NODE_W = 150f
private const val NODE_H = 175f

private fun prodPath() = Path().apply {
    moveTo(146f, 62f); lineTo(166f, 62f); quadraticTo(176f, 62f, 176f, 78f); lineTo(176f, 198f)
}
private fun gridPath() = Path().apply {
    moveTo(204f, 198f); lineTo(204f, 78f); quadraticTo(204f, 62f, 214f, 62f); lineTo(234f, 62f)
}
private fun consPath() = Path().apply {
    moveTo(176f, 262f); lineTo(176f, 374f); quadraticTo(176f, 390f, 166f, 390f); lineTo(146f, 390f)
}
private fun battPath() = Path().apply {
    moveTo(204f, 262f); lineTo(204f, 374f); quadraticTo(204f, 390f, 214f, 390f); lineTo(234f, 390f)
}

/** Toc do sweep: 18/v giay, clamp 4-14s — v la kW (dung abs de gia tri am van co toc do). */
private fun sweepMs(powerKw: Double): Int =
    (minOf(14.0, maxOf(4.0, 18.0 / maxOf(0.15, kotlin.math.abs(powerKw)))) * 1000).roundToInt()

/**
 * Don vi linh hoat (2026-09-30, user): |W| < 1000 -> hien W (0 so le),
 * >= 1000W -> hien kW (1 so le). Tra ve (so, don vi) de style rieng.
 */
private fun powerNumUnit(watts: Double): Pair<String, String> {
    return watts.toSmartPowerParts()
}

/**
 * Co dong chay = gia tri HIEN THI khac 0.
 * Fix bug (2026-09-30): truoc day nguong animation la 5W trong khi hien thi
 * lam tron den 0.1kW -> gia tri 5-50W hien "0.0 kW" nhung duong van chay.
 * Gio: hien "0 W" <=> khong chay, khong con lech.
 */
private fun hasPowerFlow(watts: Double): Boolean = watts.roundToInt() != 0

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
                        style = MaterialTheme.typography.titleLarge.copy(),
                    )
                    // Bo chu thich "Dong chay thoi gian thuc" (2026-09-30, user yeu cau).
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
                            fontWeight = FontWeight.SemiBold,
                            fontFeatureSettings = "tnum",
                        ),
                    )
                    Text(
                        " kWh",
                        style = MaterialTheme.typography.bodyMedium.copy(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            FlowArea(flow = flow, charging = charging, soc = soc, vm = vm)
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

        // nodes: 150x190px trong viewBox 380x460, cach ria 6px
        val nw = fx(NODE_W); val nh = fy(NODE_H)
        FlowNode(
            icon = { MsIcon(M3EIcons.SolarPower, null, tint = Color(0xFFD97706), modifier = Modifier.size(24.dp)) },
            tintBg = Color(0xFFF59E0B).copy(alpha = 0.16f),
            tintFg = Color(0xFFD97706),
            label = "Sản xuất", valueKw = flow.prodKw,
            modifier = Modifier
                .size(nw, nh)
                .offset(fx(6f), fy(6f)),
        ) {
            val prodTot = flow.prodKw.coerceAtLeast(0.01)
            SegBar(
                items = listOf(
                    "PV1" to flow.pv1Kw,
                    "PV2" to flow.pv2Kw,
                ),
                total = prodTot,
            )
        }
        // Trang thai luoi dien: pill tonal M3E DAT DUOI CUNG node (2026-09-30,
        // user) — de cung hang icon bi tran (node ~146dp, icon+badge can ~159dp).
        // Mat dien = do cung (#BA1A1A).
        val gridBadgeFg = if (flow.gridOn) LocalHumeExtraColors.current.success else Color(0xFFBA1A1A)
        FlowNode(
            icon = { MsIcon(M3EIcons.ElectricMeter, null, tint = Color(0xFF2F6EA3), modifier = Modifier.size(24.dp)) },
            tintBg = Color(0xFF2F6EA3).copy(alpha = 0.16f),
            tintFg = Color(0xFF2F6EA3),
            label = "Lưới điện", valueKw = flow.gridKw,
            modifier = Modifier
                .size(nw, nh)
                .offset(fx(VB_W - 6f - NODE_W), fy(6f)),
        ) {
            // Pill trang thai luoi: can giua ngang trong node (2026-09-30, user).
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (flow.gridOn) "Cấp điện" else "Mất điện",
                    style = MaterialTheme.typography.labelSmall.copy(),
                    color = gridBadgeFg,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(gridBadgeFg.copy(alpha = 0.16f))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                )
            }
        }
        FlowNode(
            icon = { MsIcon(M3EIcons.Home, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) },
            tintBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            tintFg = MaterialTheme.colorScheme.primary,
            // Mat dien: tai chay qua cong backup -> doi ten node (2026-09-30, user).
            label = if (flow.gridOn) "Tiêu thụ" else "Cổng phụ",
            valueKw = flow.consKw,
            modifier = Modifier
                .size(nw, nh)
                .offset(fx(6f), fy(VB_H - 6f - NODE_H)),
        ) {
            val tot = (flow.cb1Kw + flow.cb2Kw + flow.cb3Kw).coerceAtLeast(0.01)
            SegBar(
                items = listOf(
                    "CB1" to flow.cb1Kw,
                    "CB2" to flow.cb2Kw,
                    "CB3" to flow.cb3Kw,
                ),
                total = tot,
            )
        }
        // Khi pin xa: chu "Dang xa" + icon pin mau da cam (2026-09-30, user yeu cau).
        val battFg = if (charging) LocalHumeExtraColors.current.success else Color(0xFFF97316)
        FlowNode(
            icon = {
                HorizontalBatteryIcon(
                    soc = soc.roundToInt(),
                    color = battFg,
                    modifier = Modifier.size(width = 30.dp, height = 18.dp),
                )
            },
            tintBg = battFg.copy(alpha = 0.16f),
            tintFg = battFg,
            label = "Pin", valueKw = flow.battKw,
            badge = if (charging) "Đang sạc" else "Đang xả",
            badgeBg = battFg.copy(alpha = 0.16f),
            badgeFg = battFg,
            modifier = Modifier
                .size(nw, nh)
                .offset(fx(VB_W - 6f - NODE_W), fy(VB_H - 6f - NODE_H)),
            onClick = { vm.toggleBattFlow() },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "SOC",
                    style = MaterialTheme.typography.labelSmall.copy( fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${soc.roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy( fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(6.dp))
            // demo .flsocbar i{transition:width .6s ease}
            val socFrac by animateFloatAsState(
                targetValue = (soc / 100).toFloat().coerceIn(0f, 1f),
                animationSpec = tween(600),
                label = "flSoc",
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(socFrac)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(LocalHumeExtraColors.current.success),
                )
            }
        }

        // hub inverter 64px o giua + ping ring flping 2.2s (demo .flhub::before)
        // Dong nang luong chay tu 4 node ve inverter.
        // Ping ring chi pulse khi co dong nang luong that (>= 5W o 1 track)
        // — khong flow (ban dem) thi khong tao infinite transition (fix 2026-09-30).
        val hub = fx(64f)
        val primary = MaterialTheme.colorScheme.primary
        val hasFlow = listOf(flow.prodKw, flow.gridKw, flow.consKw, flow.battKw)
            .any { kotlin.math.abs(it) >= 0.005 }
        Box(
            modifier = Modifier
                .size(hub)
                .offset(fx(VB_W / 2 - 32f), fy(VB_H / 2 - 32f)),
        ) {
            if (hasFlow) {
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
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = pScale
                            scaleY = pScale
                            alpha = pAlpha
                        }
                        .border(2.dp, primary, CircleShape),
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            ) {
                // Inverter: hop bien tan + song sine AC (thay cho bolt)
                InverterGlyph(
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(hub * 0.52f),
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
    data class Track(val path: Path, val powerW: Double, val reverse: Boolean)
    val tracks = listOf(
        Track(prodPath(), flow.prodKw * 1000, false),
        // gridPath huong hub->node; khi grid > 0 (mua dien) dong chay nguoc lai node->hub
        Track(gridPath(), flow.gridKw * 1000, (flow.gridKw * 1000).roundToInt() > 0),
        Track(consPath(), flow.consKw * 1000, false),
        Track(battPath(), flow.battKw * 1000, !charging),
    )
    val phases = tracks.mapIndexed { idx, t ->
        // Khong co dong nang luong (hien thi "0 W") -> phase tinh 0, khong chay
        // animation vo han (fix nong may 2026-09-30, nhat la ban dem PV = 0).
        if (!hasPowerFlow(t.powerW)) {
            remember(idx) { mutableFloatStateOf(0f) }
        } else {
            // demo: chi set lai --dur khi toc do lech >12% de tranh restart animation
            var dur by remember(idx) { mutableIntStateOf(sweepMs(t.powerW / 1000.0)) }
            val ms = sweepMs(t.powerW / 1000.0)
            if (kotlin.math.abs(ms - dur) / dur.toFloat() > 0.12f) dur = ms
            val target = if (t.reverse) 1270f else -1270f
            transition.animateFloat(
                initialValue = 0f, targetValue = target,
                animationSpec = infiniteRepeatable(
                    animation = tween(dur, easing = LinearEasing),
                ),
                label = "sweep$idx",
            )
        }
    }
    Canvas(Modifier.fillMaxSize()) {
        val sx = size.width / VB_W
        val sy = size.height / VB_H
        // Do day net chay theo gia tri: lon nhat 4.5.dp (= 5.dp hien tai - 10%), nho nhat 1.5.dp
        val maxP = tracks.maxOf { kotlin.math.abs(it.powerW) }.coerceAtLeast(0.01)
        tracks.forEachIndexed { i, t ->
            val frac = (kotlin.math.abs(t.powerW) / maxP).toFloat().coerceIn(0.08f, 1f)
            val sweepW = 1.5.dp + 3.dp * frac
            val glowW = sweepW * 2.2f
            val path = Path().apply {
                addPath(t.path)
                transform(Matrix().apply { scale(sx, sy) })
            }
            drawPath(
                path = path,
                color = trackColor.copy(alpha = 0.45f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            // Khong co dong (hien "0 W") -> chi ve track mo, khong ve net sweep
            // (ca doan dash tinh cung khong hien).
            if (!hasPowerFlow(t.powerW)) return@forEachIndexed
            val dash = PathEffect.dashPathEffect(floatArrayOf(70f * sx, 1200f * sx), phases[i].value * sx)
            // glow underlay (demo: drop-shadow(0 0 7px primary 70%) tren net sweep 5px)
            drawPath(
                path = path, color = primary.copy(alpha = 0.5f),
                style = Stroke(width = glowW.toPx(), cap = StrokeCap.Round, pathEffect = dash),
            )
            drawPath(
                path = path, color = primary,
                style = Stroke(width = sweepW.toPx(), cap = StrokeCap.Round, pathEffect = dash),
            )
        }
    }
}

@Composable
private fun FlowNode(
    icon: @Composable () -> Unit,
    tintBg: Color,
    tintFg: Color,
    label: String,
    valueKw: Double,
    modifier: Modifier = Modifier,
    badge: String? = null,
    badgeBg: Color = Color.Transparent,
    badgeFg: Color = Color.Transparent,
    // Badge trang thai nam CUNG HANG voi icon (2026-09-30, user; vd trang thai
    // luoi dien) — pill tonal M3E.
    iconBadge: String? = null,
    iconBadgeBg: Color = Color.Transparent,
    iconBadgeFg: Color = Color.Transparent,
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(tintBg),
            ) {
                Box(
                    modifier = Modifier.size(28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    icon()
                }
            }
            if (iconBadge != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    iconBadge,
                    style = MaterialTheme.typography.labelSmall.copy(),
                    color = iconBadgeFg,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(iconBadgeBg)
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                )
            }
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
                        fontSize = 9.sp, fontWeight = FontWeight.SemiBold),
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
            modifier = Modifier.padding(top = 1.dp),
        ) {
            // Don vi linh hoat (2026-09-30, user): < 1000W hien W, >= 1000W hien kW.
            val (num, unit) = powerNumUnit(valueKw * 1000)
            Text(
                num,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum",
                ),
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                " $unit",
                style = MaterialTheme.typography.bodySmall.copy(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.alignByBaseline(),
            )
        }
        Spacer(Modifier.weight(1f))
        bottom()
    }
}

/**
 * Segment bar giong HTML .segbar/.segleg: thanh ngang 4dp chia doan theo ty le,
 * ben duoi la legend cham mau + label (PV1/PV2, CB1/CB2/CB3).
 */
@Composable
private fun SegBar(
    items: List<Pair<String, Double>>,
    total: Double,
) {
    val cs = MaterialTheme.colorScheme
    val safeTotal = total.coerceAtLeast(0.01)
    // Mau segment theo ty le gia tri: cao = dam (primary), thap = nhat (primaryContainer)
    val maxKw = items.maxOfOrNull { it.second }?.coerceAtLeast(0.01) ?: 0.01
    fun segColor(kw: Double): Color {
        val t = (kw / maxKw).toFloat().coerceIn(0f, 1f)
        return androidx.compose.ui.graphics.lerp(cs.primaryContainer, cs.primary, 0.25f + 0.75f * t)
    }
    Column {
        // Thanh segment: cac doan mau dam/nhat theo gia tri, gap 2dp, min-width tuong duong 8px
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(cs.surfaceContainerHigh),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items.forEach { (_, kw) ->
                val frac by animateFloatAsState(
                    targetValue = (kw / safeTotal).toFloat().coerceIn(0f, 1f),
                    animationSpec = tween(800, easing = M3EMotion.emphasized),
                    label = "segFrac",
                )
                Box(
                    modifier = Modifier
                        .weight(frac.coerceAtLeast(0.001f))
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(segColor(kw)),
                )
            }
        }
        // Legend: chi ten, khong cham mau
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items.forEach { (name, _) ->
                Text(
                    name,
                    style = MaterialTheme.typography.labelSmall.copy( fontWeight = FontWeight.SemiBold),
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

/**
 * Glyph inverter ve tay: hop bien tan (rounded rect) + song sine AC ben trong.
 * Dung o hub giua thay cho bolt — dong nang luong chay tu cac node ve inverter.
 */
@Composable
private fun InverterGlyph(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = (w * 0.09f).coerceAtLeast(2.dp.toPx())
        // Hop bien tan
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.08f, h * 0.12f),
            size = androidx.compose.ui.geometry.Size(w * 0.84f, h * 0.76f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.12f),
            style = Stroke(width = strokeW),
        )
        // Song sine AC ben trong hop
        val wavePath = Path().apply {
            val x0 = w * 0.20f
            val x1 = w * 0.80f
            val cy = h * 0.50f
            val amp = h * 0.16f
            moveTo(x0, cy)
            // 1.5 chu ky sine
            val steps = 48
            for (i in 1..steps) {
                val t = i.toFloat() / steps
                val x = x0 + (x1 - x0) * t
                val y = cy - amp * kotlin.math.sin(t * 3f * kotlin.math.PI.toFloat()).toFloat()
                lineTo(x, y)
            }
        }
        drawPath(
            wavePath,
            color = tint,
            style = Stroke(width = strokeW * 0.85f, cap = StrokeCap.Round),
        )
    }
}
