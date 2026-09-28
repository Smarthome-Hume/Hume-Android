package com.smarthome.hume.feature.energy

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smarthome.hume.core.model.EnergyFlowState
import com.smarthome.hume.core.ui.components.M3ECard
import com.smarthome.hume.core.ui.components.M3EIcons
import kotlin.math.roundToInt

private const val VB_W = 360f
private const val VB_H = 340f

private fun prodPath() = Path().apply {
    moveTo(124f, 50f); lineTo(146f, 50f); quadraticTo(162f, 50f, 162f, 66f); lineTo(162f, 134f)
}
private fun gridPath() = Path().apply {
    moveTo(198f, 134f); lineTo(198f, 66f); quadraticTo(198f, 50f, 214f, 50f); lineTo(236f, 50f)
}
private fun consPath() = Path().apply {
    moveTo(162f, 202f); lineTo(162f, 274f); quadraticTo(162f, 290f, 146f, 290f); lineTo(124f, 290f)
}
private fun battPath() = Path().apply {
    moveTo(198f, 202f); lineTo(198f, 274f); quadraticTo(198f, 290f, 214f, 290f); lineTo(236f, 290f)
}

/** Toc do sweep: 14s luc 0 kW -> 4s tu 3 kW tro len (theo demo). */
private fun sweepMs(powerKw: Double): Int =
    (14000 - 10000 * (powerKw / 3.0).coerceIn(0.0, 1.0)).roundToInt()

/**
 * Flow card M3E: 4 node + hub bolt o giua, sweep tren elbow track.
 * Port tu demo v4 rev12 (.flx).
 */
@Composable
fun EnergyFlowCard(flow: EnergyFlowState, modifier: Modifier = Modifier) {
    M3ECard(
        shape = RoundedCornerShape(32.dp),
        contentPadding = 0.dp,
        modifier = modifier,
    ) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 16.dp)) {
        // header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column {
                Text("Năng lượng", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
                Text("Dòng chảy thời gian thực",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E)),
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    String.format("%.1f", flow.todayKwh),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(" kWh", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(6.dp))
        FlowArea(flow)
        // footer
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Hôm nay sản xuất ${String.format("%.1f", flow.todayKwh)} kWh",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Tự dùng ${flow.selfUsePct.roundToInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold)
        }
        }
    }
}

@Composable
private fun FlowArea(flow: EnergyFlowState) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(VB_W / VB_H),
    ) {
        val w = maxWidth
        val h = maxHeight
        fun fx(x: Float): Dp = w * (x / VB_W)
        fun fy(y: Float): Dp = h * (y / VB_H)

        FlowTracks(flow)

        // nodes: 118x132px trong viewBox 360x340, cach ria 6px
        val nw = fx(118f); val nh = fy(132f)
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
                .offset(fx(VB_W - 6f - 118f), fy(6f)),
        )
        FlowNode(
            icon = M3EIcons.Home,
            tintBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            tintFg = MaterialTheme.colorScheme.primary,
            label = "Tiêu thụ", valueKw = flow.consKw,
            modifier = Modifier
                .size(nw, nh)
                .offset(fx(6f), fy(VB_H - 6f - 132f)),
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
            icon = if (flow.battCharging) M3EIcons.Battery else M3EIcons.BatteryFull,
            tintBg = battFg.copy(alpha = 0.16f),
            tintFg = battFg,
            label = "Pin", valueKw = flow.battKw,
            badge = if (flow.battCharging) "Đang sạc" else "Đang xả",
            badgeBg = battFg.copy(alpha = 0.16f),
            badgeFg = battFg,
            modifier = Modifier
                .size(nw, nh)
                .offset(fx(VB_W - 6f - 118f), fy(VB_H - 6f - 132f)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("SOC", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold)
                Text("${flow.soc.roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((flow.soc / 100).toFloat().coerceIn(0f, 1f))
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF16A34A)),
                )
            }
        }

        // hub bolt 64px o giua
        val hub = fx(64f)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(hub)
                .offset(fx(VB_W / 2 - 32f), fy(VB_H * 0.494f - 32f))
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
        ) {
            Icon(M3EIcons.Power, null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(hub * 0.47f))
        }
    }
}

@Composable
private fun FlowTracks(flow: EnergyFlowState) {
    val primary = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val transition = rememberInfiniteTransition(label = "flsweep")
    data class Track(val path: Path, val powerKw: Double, val reverse: Boolean)
    val tracks = listOf(
        Track(prodPath(), flow.prodKw, false),
        Track(gridPath(), flow.gridKw, false),
        Track(consPath(), flow.consKw, false),
        Track(battPath(), flow.battKw, !flow.battCharging),
    )
    val phases = tracks.map { t ->
        val target = if (t.reverse) 1270f else -1270f
        transition.animateFloat(
            initialValue = 0f, targetValue = target,
            animationSpec = infiniteRepeatable(
                animation = tween(sweepMs(t.powerKw), easing = LinearEasing),
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
            // glow underlay
            drawPath(
                path = path, color = primary.copy(alpha = 0.25f),
                style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round, pathEffect = dash),
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
    icon: ImageVector,
    tintBg: Color,
    tintFg: Color,
    label: String,
    valueKw: Double,
    modifier: Modifier = Modifier,
    badge: String? = null,
    badgeBg: Color = Color.Transparent,
    badgeFg: Color = Color.Transparent,
    bottom: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable { }
            .padding(10.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(tintBg),
        ) {
            Icon(icon, null, tint = tintFg, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (badge != null) {
                Spacer(Modifier.width(5.dp))
                Text(badge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = badgeFg,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(badgeBg)
                        .padding(horizontal = 7.dp, vertical = 2.dp))
            }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                String.format("%.1f", valueKw),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(" kW", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text(name, style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Box(
                modifier = Modifier
                    .weight(frac.coerceAtLeast(0.05f))
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(c),
            )
        }
    }
}
