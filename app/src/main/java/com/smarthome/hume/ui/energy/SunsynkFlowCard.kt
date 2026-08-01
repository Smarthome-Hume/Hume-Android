package com.smarthome.hume.ui.energy

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.ui.theme.HumeColors
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/* =====================================================================
 *  SUNSYNK ANIMATED FLOW CARD  (Views/Energy/SunsynkFlowCard.swift)
 *  Five ring nodes (PV, grid, load, battery, backup) around the inverter,
 *  joined by orthogonal pipes with a neon dot that runs faster the more
 *  power flows. Exactly the sensors and thresholds the SwiftUI card uses.
 * ===================================================================== */

private const val THRESHOLD = 5.0
private const val MAX_PV = 5000.0
private const val MAX_GRID = 7000.0
private const val MAX_LOAD = 7000.0
private const val MAX_BACKUP = 3000.0

private val cPv = Color(0xFFFFC20D)
private val cGridBuy = Color(0xFFF54336)
private val cLoad = Color(0xFF806EEB)
private val cBatCharge = Color(0xFF21B366)
private val cBatDischarge = Color(0xFFCC5900)
private val cInverter = Color(0xFFE6A60D)
private val cGray = Color(0x73938A85)
private val cBackup = Color(0xD9666666)

@Composable
fun SunsynkAnimatedFlowCard(ha: HomeAssistantRepository) {
    val entities by ha.entities.collectAsState()
    val connected by ha.connected.collectAsState()
    val measurer = rememberTextMeasurer()

    fun d(id: String): Double = entities[id]?.numericState ?: 0.0

    val pv = d("sensor.solis_s6_eh1p_total_pv_power_2")
    val grid = d("sensor.aptomat_tong_power")
    val load = d("sensor.cong_suat_nha")
    val battery = d("sensor.battery_power_flow")
    val backup = d("sensor.solis_s6_eh1p_backup_load_power_2")
    val soc = d("sensor.solis_s6_eh1p_battery_soc_2")
    val voltage = d("sensor.solis_s6_eh1p_battery_voltage_2")
    val chargeLimit = d("sensor.solis_s6_eh1p_battery_charge_current_limitation_bms_2")
    val dischargeLimit = d("sensor.solis_s6_eh1p_battery_discharge_current_limitation_bms_2")
    val batMax = maxOf(chargeLimit, dischargeLimit) * maxOf(voltage, 48.0)

    val idle = pv <= THRESHOLD && abs(grid) <= THRESHOLD && load <= THRESHOLD &&
        abs(battery) <= THRESHOLD && backup <= THRESHOLD
    val paused = idle || !connected

    // The TimelineView(.animation, paused:) clock on iOS.
    var time by remember { mutableStateOf(0f) }
    LaunchedEffect(paused) {
        if (paused) return@LaunchedEffect
        val start = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            time = (now - start) / 1_000_000_000f
        }
    }

    val socColor = when {
        soc > 80 -> HumeColors.Green
        soc > 50 -> HumeColors.Orange
        else -> Color(0xFFF44336)
    }

    Column(Modifier.fillMaxWidth().padding(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.WbSunny, contentDescription = null, tint = HumeColors.Orange, modifier = Modifier.size(15.dp))
            Spacer(Modifier.size(6.dp))
            Text("S\u01a1 \u0111\u1ed3 n\u0103ng l\u01b0\u1ee3ng", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = HumeColors.TextPrimary, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.BatteryFull, contentDescription = null, tint = socColor, modifier = Modifier.size(14.dp))
            Spacer(Modifier.size(3.dp))
            Text(soc.toInt().toString() + "%", fontSize = 13.sp, color = HumeColors.TextSecondary)
            Spacer(Modifier.size(6.dp))
            Box(Modifier.size(7.dp).clip(CircleShape).background(if (connected) HumeColors.Green else HumeColors.TextSecondary))
        }
        Spacer(Modifier.height(18.dp))

        Canvas(Modifier.fillMaxWidth().height(411.dp)) {
            drawFlow(measurer, time, pv, grid, load, battery, backup, batMax)
        }

        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth()) {
            DailyCell(Modifier.weight(1f), Icons.Rounded.WbSunny, HumeColors.Orange, "S\u1ea3n l\u01b0\u1ee3ng", d("sensor.solis_s6_eh1p_pv_today_energy_generation_2"))
            DailyCell(Modifier.weight(1f), Icons.Rounded.ArrowDownward, HumeColors.Green, "S\u1ea1c", d("sensor.solis_s6_eh1p_today_battery_charge_energy_2"))
            DailyCell(Modifier.weight(1f), Icons.Rounded.ArrowUpward, Color(0xFF3380FF), "X\u1ea3", d("sensor.solis_s6_eh1p_today_battery_discharge_energy_2"))
        }
    }
}

@Composable
private fun DailyCell(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, label: String, value: Double) {
    Column(modifier.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
            Spacer(Modifier.size(3.dp))
            Text(String.format(Locale.US, "%.2f", value), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HumeColors.TextPrimary)
            Spacer(Modifier.size(2.dp))
            Text("kWh", fontSize = 10.sp, color = HumeColors.TextSecondary)
        }
        Text(label, fontSize = 10.sp, color = HumeColors.TextSecondary)
    }
}

/* --------------------------- canvas drawing --------------------------- */

private fun DrawScope.drawFlow(
    measurer: TextMeasurer,
    t: Float,
    pv: Double,
    grid: Double,
    load: Double,
    battery: Double,
    backup: Double,
    batMax: Double,
) {
    val w = size.width
    val h = size.height
    val nr = (w * 0.115f).coerceIn(35f, 42f)
    val lw = 2.5.dp.toPx()
    val cr = 14f
    val dotSize = 4f
    val xL = w * 0.16f
    val xC = w * 0.50f
    val xR = w * 0.84f
    val yT = h * 0.14f
    val yM = h * 0.50f
    val yB = h - nr - 22f
    val xPL = xC - nr * 0.5f
    val xPR = xC + nr * 0.5f

    val pvOn = pv > THRESHOLD
    val gridBuy = grid > THRESHOLD
    val loadOn = load > THRESHOLD
    val charging = battery > THRESHOLD
    val discharging = battery < -THRESHOLD
    val backupOn = backup > THRESHOLD
    val batColor = if (charging) cBatCharge else if (discharging) cBatDischarge else cGray

    // PV -> inverter
    pipe(t, Offset(xL + nr, yT), Offset(xPL, yT), Offset(xPL, yM), cr, pvOn, if (pvOn) cPv else cGray, pv, MAX_PV, lw, dotSize)
    // Grid -> inverter
    pipe(t, Offset(xR - nr, yT), Offset(xPR, yT), Offset(xPR, yM), cr, gridBuy, if (gridBuy) cGridBuy else cGray, abs(grid), MAX_GRID, lw, dotSize)
    // Inverter -> load
    straight(t, Offset(xPR, yM), Offset(xR - nr, yM), loadOn, if (loadOn) cLoad else cGray, load, MAX_LOAD, lw, dotSize)
    // Battery, direction follows the sign like SwiftUI
    if (charging) {
        pipe(t, Offset(xPL, yM), Offset(xPL, yB), Offset(xL + nr, yB), cr, true, cBatCharge, battery, batMax, lw, dotSize)
    } else {
        pipe(t, Offset(xL + nr, yB), Offset(xPL, yB), Offset(xPL, yM), cr, discharging, if (discharging) cBatDischarge else cGray, abs(battery), batMax, lw, dotSize)
    }
    // Inverter -> backup port
    pipe(t, Offset(xPR, yM), Offset(xPR, yB), Offset(xR - nr, yB), cr, backupOn, if (backupOn) cBackup else cGray, backup, MAX_BACKUP, lw, dotSize)

    ringNode(measurer, Offset(xL, yT), nr, if (pvOn) cPv else Color.Transparent, min(1.0, pv / MAX_PV), pv, "Quang \u0111i\u1ec7n", null)
    ringNode(measurer, Offset(xR, yT), nr, if (gridBuy) cGridBuy else Color.Transparent, min(1.0, abs(grid) / MAX_GRID), abs(grid), "L\u01b0\u1edbi \u0111i\u1ec7n", if (gridBuy) "Mua" else null)
    ringNode(measurer, Offset(xR, yM), nr, if (loadOn) cLoad else Color.Transparent, min(1.0, load / MAX_LOAD), load, "Ph\u1ee5 t\u1ea3i", null)
    ringNode(measurer, Offset(xL, yB), nr, if (charging || discharging) batColor else Color.Transparent, min(1.0, abs(battery) / maxOf(batMax, 100.0)), abs(battery), "B\u1ed9 l\u01b0u tr\u1eef", null)
    ringNode(measurer, Offset(xR, yB), nr, if (backupOn) cBackup else Color.Transparent, min(1.0, backup / MAX_BACKUP), backup, "C\u1ed5ng ph\u1ee5", null)

    // Inverter in the middle
    drawCircle(HumeColors.Card, radius = nr, center = Offset(xC, yM))
    drawCircle(cInverter, radius = nr, center = Offset(xC, yM), style = Stroke(width = 3f))
    val invLayout = measurer.measure("INV", TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = cInverter))
    drawText(invLayout, topLeft = Offset(xC - invLayout.size.width / 2f, yM - invLayout.size.height / 2f))
}

/** orthoPipe + drawSingleArrowOnL from the SwiftUI card. */
private fun DrawScope.pipe(
    t: Float,
    from: Offset,
    corner: Offset,
    to: Offset,
    cr: Float,
    active: Boolean,
    color: Color,
    power: Double,
    maxW: Double,
    lw: Float,
    dotSize: Float,
) {
    val path = Path()
    val horizontalFirst = abs(corner.y - from.y) < 1f
    if (horizontalFirst) {
        val r = minOf(cr, abs(corner.x - from.x) * 0.45f, abs(to.y - corner.y) * 0.45f).coerceAtLeast(0f)
        val sx = if (corner.x > from.x) 1f else -1f
        val sy = if (to.y > corner.y) 1f else -1f
        path.moveTo(from.x, from.y)
        path.lineTo(corner.x - sx * r, from.y)
        path.quadraticBezierTo(corner.x, corner.y, corner.x, corner.y + sy * r)
        path.lineTo(to.x, to.y)
    } else {
        val r = minOf(cr, abs(corner.y - from.y) * 0.45f, abs(to.x - corner.x) * 0.45f).coerceAtLeast(0f)
        val sy = if (corner.y > from.y) 1f else -1f
        val sx = if (to.x > corner.x) 1f else -1f
        path.moveTo(from.x, from.y)
        path.lineTo(from.x, corner.y - sy * r)
        path.quadraticBezierTo(corner.x, corner.y, corner.x + sx * r, corner.y)
        path.lineTo(to.x, to.y)
    }
    drawPath(path, color.copy(alpha = 0.30f), style = Stroke(width = lw))
    if (!active) return

    val speed = (abs(power) / maxW * 3.0).coerceIn(0.5, 3.0)
    if (horizontalFirst) {
        val r = minOf(cr, abs(corner.x - from.x) * 0.45f, abs(to.y - corner.y) * 0.45f).coerceAtLeast(0f)
        val sx = if (corner.x > from.x) 1f else -1f
        val sy = if (to.y > corner.y) 1f else -1f
        val hLen = (abs(corner.x - from.x) - r).coerceAtLeast(0f)
        val vLen = (abs(to.y - corner.y) - r).coerceAtLeast(0f)
        val total = hLen + vLen
        if (total <= 0f) return
        val phase = ((t * speed * 60).toFloat()) % total
        val point = if (phase <= hLen) Offset(from.x + sx * phase, from.y)
        else Offset(corner.x, corner.y + sy * (phase - hLen))
        neonDot(point, dotSize, color)
    } else {
        val r = minOf(cr, abs(corner.y - from.y) * 0.45f, abs(to.x - corner.x) * 0.45f).coerceAtLeast(0f)
        val sy = if (corner.y > from.y) 1f else -1f
        val sx = if (to.x > corner.x) 1f else -1f
        val vLen = (abs(corner.y - from.y) - r).coerceAtLeast(0f)
        val hLen = (abs(to.x - corner.x) - r).coerceAtLeast(0f)
        val total = vLen + hLen
        if (total <= 0f) return
        val phase = ((t * speed * 60).toFloat()) % total
        val point = if (phase <= vLen) Offset(from.x, from.y + sy * phase)
        else Offset(corner.x + sx * (phase - vLen), corner.y)
        neonDot(point, dotSize, color)
    }
}

private fun DrawScope.straight(
    t: Float,
    from: Offset,
    to: Offset,
    active: Boolean,
    color: Color,
    power: Double,
    maxW: Double,
    lw: Float,
    dotSize: Float,
) {
    drawLine(color.copy(alpha = 0.30f), from, to, strokeWidth = lw)
    if (!active) return
    val dx = to.x - from.x
    val dy = to.y - from.y
    val length = sqrt(dx * dx + dy * dy)
    if (length <= 0f) return
    atan2(dy, dx)
    val speed = (abs(power) / maxW * 3.0).coerceIn(0.5, 3.0)
    val phase = ((t * speed * 60).toFloat()) % length
    neonDot(Offset(from.x + dx / length * phase, from.y + dy / length * phase), dotSize, color)
}

/** Four stacked circles, the glow of drawNeonDot. */
private fun DrawScope.neonDot(center: Offset, size: Float, color: Color) {
    val r = size * 0.6f
    drawCircle(color.copy(alpha = 0.08f), radius = r * 3, center = center)
    drawCircle(color.copy(alpha = 0.20f), radius = r * 2, center = center)
    drawCircle(color.copy(alpha = 0.60f), radius = r, center = center)
    drawCircle(color, radius = r * 0.5f, center = center)
}

private fun DrawScope.ringNode(
    measurer: TextMeasurer,
    center: Offset,
    r: Float,
    arcColor: Color,
    fraction: Double,
    watt: Double,
    label: String,
    sub: String?,
) {
    drawCircle(HumeColors.Card, radius = r, center = center)
    drawCircle(HumeColors.Divider, radius = r, center = center, style = Stroke(width = 2.5f))
    if (fraction > 0.01) {
        drawArc(
            color = arcColor,
            startAngle = -90f,
            sweepAngle = (360 * fraction).toFloat(),
            useCenter = false,
            topLeft = Offset(center.x - r, center.y - r),
            size = Size(r * 2, r * 2),
            style = Stroke(width = 2.5f),
        )
    }
    val wattLayout = measurer.measure(
        watt.roundToInt().toString() + " W",
        TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HumeColors.TextPrimary),
    )
    drawText(wattLayout, topLeft = Offset(center.x - wattLayout.size.width / 2f, center.y - wattLayout.size.height / 2f))

    if (sub != null) {
        val subLayout = measurer.measure(sub, TextStyle(fontSize = 9.sp, color = HumeColors.TextSecondary))
        drawText(subLayout, topLeft = Offset(center.x - subLayout.size.width / 2f, center.y + r * 0.42f))
    }
    val labelLayout = measurer.measure(label, TextStyle(fontSize = 11.sp, color = HumeColors.TextSecondary))
    drawText(labelLayout, topLeft = Offset(center.x - labelLayout.size.width / 2f, center.y + r + 6f))
    Rect(Offset.Zero, Size(0f, 0f))
}

/* =====================================================================
 *  SUNSYNK STATIC CARD — floors, battery limits, inverter efficiency, PV strings
 * ===================================================================== */

@Composable
fun SunsynkStaticCard(entities: Map<String, HomeEntity>) {
    fun d(id: String): Double = entities[id]?.numericState ?: 0.0

    val pv = d("sensor.solis_s6_eh1p_total_pv_power_2")
    val battery = d("sensor.battery_power_flow")
    val load = d("sensor.cong_suat_nha")
    val grid = d("sensor.aptomat_tong_power")
    val input = (pv - battery).coerceAtLeast(0.0)
    val output = (load - grid.coerceAtLeast(0.0)).coerceAtLeast(0.0) + (-grid).coerceAtLeast(0.0)
    val efficiency = if (input > 50) min(output / input * 100, 100.0) else 0.0

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MiniStat(Modifier.weight(1f), "T\u1ea7ng 1", d("sensor.aptomat_t1_power").toInt().toString() + "W")
            MiniStat(Modifier.weight(1f), "T\u1ea7ng 2", d("sensor.aptomat_t2_power").toInt().toString() + "W")
            MiniStat(Modifier.weight(1f), "T\u1ea7ng 3", d("sensor.aptomat_t3_power").toInt().toString() + "W")
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MiniStat(Modifier.weight(1f), "C\u00f4ng su\u1ea5t", abs(battery).toInt().toString() + " W")
            MiniStat(
                Modifier.weight(1f),
                "D\u00f2ng / \u00c1p",
                String.format(Locale.US, "%.1fA / %.1fV", d("sensor.battery_current_flow"), d("sensor.solis_s6_eh1p_battery_voltage_2")),
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MiniStat(Modifier.weight(1f), "S\u1ea1c gi\u1edbi h\u1ea1n", String.format(Locale.US, "%.1fA", d("sensor.solis_s6_eh1p_battery_charge_current_limitation_bms_2")))
            MiniStat(Modifier.weight(1f), "X\u1ea3 gi\u1edbi h\u1ea1n", String.format(Locale.US, "%.1fA", d("sensor.solis_s6_eh1p_battery_discharge_current_limitation_bms_2")))
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(HumeColors.Divider))
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Pin n\u0103ng l\u01b0\u1ee3ng", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = HumeColors.TextSecondary, modifier = Modifier.weight(1f))
            Text(String.format(Locale.US, "%.1f%%", efficiency), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = HumeColors.TextPrimary)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PvBox(Modifier.weight(1f), "PV1", d("sensor.solis_s6_eh1p_pv_power_1_3"), d("sensor.solis_s6_eh1p_pv_voltage_1_2"), d("sensor.solis_s6_eh1p_pv_current_1_2"))
            PvBox(Modifier.weight(1f), "PV2", d("sensor.solis_s6_eh1p_pv_power_2_3"), d("sensor.solis_s6_eh1p_pv_voltage_2_2"), d("sensor.solis_s6_eh1p_pv_current_2_2"))
        }
    }
}

@Composable
private fun MiniStat(modifier: Modifier, label: String, value: String) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(HumeColors.Background)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, fontSize = 10.sp, color = HumeColors.TextSecondary)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = HumeColors.TextPrimary)
    }
}

@Composable
private fun PvBox(modifier: Modifier, label: String, power: Double, volt: Double, current: Double) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(HumeColors.Background)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 10.sp, color = HumeColors.TextPrimary, modifier = Modifier.weight(1f))
            Text(
                power.toInt().toString() + " W",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (power > 0) HumeColors.Orange else HumeColors.TextSecondary,
            )
        }
        Text(String.format(Locale.US, "%dV / %.1fA", volt.toInt(), current), fontSize = 9.sp, color = HumeColors.TextSecondary)
    }
}
