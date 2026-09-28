@file:OptIn(ExperimentalMaterial3Api::class)

package com.smarthome.hume.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.ha.HistoryPoint
import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.glassSurface
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

/** One hourly bucket, the HourPoint struct of ChartPopupView. */
private data class HourPoint(val hour: Int, val label: String, val value: Double)

/** ChartPopupView.titles: the six sensors that can open the popup. */
private val ChartTitles: Map<String, Pair<String, String>> = mapOf(
    "sensor.battery_power_flow" to ("C\u00f4ng su\u1ea5t Pin" to "W"),
    "sensor.solis_s6_eh1p_total_pv_power_2" to ("\u0110i\u1ec7n m\u1eb7t tr\u1eddi" to "W"),
    "sensor.solis_s6_eh1p_pv_today_energy_generation_2" to ("S\u1ea3n l\u01b0\u1ee3ng" to "kWh"),
    "sensor.solis_s6_eh1p_battery_soc_2" to ("Dung l\u01b0\u1ee3ng Pin" to "%"),
    "sensor.aptomat_tong_daily" to ("\u0110i\u1ec7n l\u01b0\u1edbi" to "kWh"),
    "sensor.energy_home_daily" to ("\u0110i\u1ec7n ti\u00eau th\u1ee5" to "kWh"),
)

/**
 * 24 hour popup of one sensor, ported from ChartPopupView in HomeView.swift.
 *
 * The history is averaged per local hour, drawn as bars that are green when the
 * value is positive and orange when it is negative, with a label every four
 * hours and no Y axis, inside a sheet 360dp tall.
 */
@Composable
fun ChartDialog(
    entityId: String,
    entities: Map<String, HomeEntity>,
    loadHistory: suspend (String) -> List<HistoryPoint>,
    onDismiss: () -> Unit,
) {
    var points by remember(entityId) { mutableStateOf<List<HourPoint>?>(null) }
    LaunchedEffect(entityId) { points = hourly(loadHistory(entityId)) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val entity = entities[entityId]
    val config = ChartTitles[entityId]
    val title = config?.first ?: entity?.friendly() ?: "Th\u00f4ng s\u1ed1"

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HumeColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier
                        .size(36.dp)
                        .glassSurface(radius = 18.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "\u0110\u00f3ng",
                        tint = HumeColors.TextPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            val data = points
            when {
                data == null -> Box(
                    Modifier.fillMaxWidth().height(240.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = HumeColors.Orange) }

                data.size < 2 -> Box(
                    Modifier.fillMaxWidth().height(240.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Kh\u00f4ng c\u00f3 d\u1eef li\u1ec7u chi ti\u1ebft",
                        fontSize = 14.sp,
                        color = HumeColors.TextSecondary,
                    )
                }

                else -> {
                    HourBars(data)
                    Spacer(Modifier.height(6.dp))
                    HourAxis(data)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HourBars(data: List<HourPoint>) {
    val positive = HumeColors.Green
    val negative = HumeColors.Orange
    Canvas(Modifier.fillMaxWidth().height(240.dp)) {
        val slot = size.width / data.size
        val barWidth = slot * 0.62f
        val maxAbs = max(data.maxOf { abs(it.value) }, 0.001)
        // Keep zero in the middle only when the sensor actually goes negative.
        val hasNegative = data.any { it.value < 0.0 }
        val zeroY = if (hasNegative) size.height / 2f else size.height
        val scale = (if (hasNegative) size.height / 2f else size.height) / maxAbs.toFloat()
        val corner = CornerRadius(3.dp.toPx(), 3.dp.toPx())

        data.forEachIndexed { index, point ->
            val height = max(abs(point.value).toFloat() * scale, 2f)
            val left = index * slot + (slot - barWidth) / 2f
            val top = if (point.value >= 0.0) zeroY - height else zeroY
            drawRoundRect(
                color = if (point.value >= 0.0) positive else negative,
                topLeft = Offset(left, top),
                size = Size(barWidth, height),
                cornerRadius = corner,
            )
        }
        if (hasNegative) {
            drawLine(
                HumeColors.TextSecondary.copy(alpha = 0.35f),
                Offset(0f, zeroY),
                Offset(size.width, zeroY),
                strokeWidth = 1.dp.toPx(),
            )
        }
    }
}

/** AxisMarks: only 00, 04, 08, 12, 16 and 20 get a label. */
@Composable
private fun HourAxis(data: List<HourPoint>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        data.forEach { point ->
            Text(
                if (point.hour % 4 == 0) String.format(Locale.US, "%02d", point.hour) else "",
                fontSize = 10.sp,
                color = HumeColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** load(): average every history point into its local hour bucket. */
private fun hourly(raw: List<HistoryPoint>): List<HourPoint> {
    if (raw.isEmpty()) return emptyList()
    val sums = HashMap<Int, Double>()
    val counts = HashMap<Int, Int>()
    val calendar = Calendar.getInstance()
    raw.forEach { point ->
        calendar.timeInMillis = point.timeMs
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        sums[hour] = (sums[hour] ?: 0.0) + point.value
        counts[hour] = (counts[hour] ?: 0) + 1
    }
    return (0 until 24).mapNotNull { hour ->
        val count = counts[hour] ?: return@mapNotNull null
        val average = (sums.getValue(hour) / count * 100.0).let { Math.round(it) / 100.0 }
        HourPoint(hour, String.format(Locale.US, "%02d:00", hour), average)
    }
}

/** Kept so callers that only want a colour band still compile. */
private fun Modifier.chartRow() = background(HumeColors.Card)
