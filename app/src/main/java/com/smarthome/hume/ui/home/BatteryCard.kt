package com.smarthome.hume.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeIcons
import com.smarthome.hume.ui.theme.glassSurface
import kotlin.math.max
import kotlin.math.min

/** PowerwallCardView.swift: height 210, corner radius 36, bar height 28. */
private val BatteryCardHeight = 210.dp
private val BatteryCardRadius = 36.dp
private val BarHeight = 28.dp

/** Bar and legend palette, taken verbatim from PowerwallCardView.swift. */
private val BarGreen = Color(0xFF22C55E)
private val BarOrange = Color(0xFFF97316)
private val BarSlate = Color(0xFF64748B)
private val LegendBlue = Color(0xFF3B82F6)
private val LegendGrey = Color(0xFF94A3B8)
private val DischargeAccent = Color(0xFFF9784C)

/**
 * "Hi\u1ec7u n\u0103ng Pin" card ported from PowerwallCardView.swift.
 *
 * Three states drive everything: resting (power between 0 and 5 W) draws a
 * plain card with no accent at all, discharging turns the accent orange and
 * charging turns it green. The bar is the Live Activity bar: the reserve part
 * is solid and the usable part is diagonally striped, both in the bar tint,
 * over a grey track split at the reserve percentage.
 */
@Composable
fun BatteryCard(
    soc: Double,
    power: Double,
    backupSoc: Double,
    timeText: String,
    finishTime: String,
    onClick: () -> Unit = {},
) {
    val resting = power in 0.0..5.0
    val discharging = power < 0.0
    val charging = !resting && !discharging
    val status = when {
        resting -> "NGH\u1ec8"
        discharging -> "\u0110ANG X\u1ea2"
        else -> "\u0110ANG S\u1ea0C"
    }
    // accent tints the card itself; barTint is a separate palette in the original.
    val accent = if (discharging) DischargeAccent else HumeColors.Green
    val barTint = when {
        charging -> BarGreen
        discharging -> BarOrange
        else -> BarSlate
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(BatteryCardHeight)
            .glassSurface(radius = BatteryCardRadius)
            .then(
                if (resting) Modifier
                else Modifier
                    .background(accent.copy(alpha = 0.10f), RoundedCornerShape(BatteryCardRadius))
                    .border(1.dp, accent.copy(alpha = 0.40f), RoundedCornerShape(BatteryCardRadius))
            )
            .clickable(onClick = onClick)
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 16.dp)
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Hi\u1ec7u n\u0103ng Pin",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HumeColors.TextPrimary,
                )
                Box(
                    Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        HumeIcons.Battery,
                        contentDescription = null,
                        tint = HumeColors.TextPrimary,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        status,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = HumeColors.TextPrimary.copy(alpha = 0.9f),
                    )
                    // Resting hides the countdown entirely, exactly like the original.
                    if (!resting) {
                        Text(timeText, fontSize = 40.sp, color = HumeColors.TextPrimary)
                    }
                }
                if (!resting && finishTime.isNotBlank() && finishTime != "--:--") {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        // The label never changes wording in PowerwallCardView.swift.
                        Text(
                            "K\u1ebeT TH\u00daC L\u00daC",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = HumeColors.TextPrimary.copy(alpha = 0.5f),
                        )
                        Text(
                            finishTime,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = HumeColors.TextPrimary,
                        )
                    }
                }
            }

            Column {
                DualBar(soc = soc, backupSoc = backupSoc, tint = barTint)
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    LegendDot(
                        color = if (charging) BarGreen else BarSlate,
                        label = "D\u1ef1 tr\u1eef " + backupSoc.toInt() + "%",
                    )
                    LegendDot(
                        color = when {
                            charging -> LegendBlue
                            discharging -> BarOrange
                            else -> LegendGrey
                        },
                        label = "S\u1eed d\u1ee5ng " + max(0.0, soc - backupSoc).toInt() + "%",
                    )
                }
            }
        }
    }
}

/**
 * The dual bar. Grey track is split in two rounded segments at the reserve
 * mark, then the live value is drawn on top: solid up to the reserve level and
 * striped beyond it.
 */
@Composable
private fun DualBar(soc: Double, backupSoc: Double, tint: Color) {
    Canvas(Modifier.fillMaxWidth().height(BarHeight)) {
        val w = size.width
        val h = size.height
        val radius = CornerRadius(h / 2f, h / 2f)
        val reserveWidth = (w * min(soc, backupSoc) / 100.0).toFloat().coerceAtLeast(0f)
        val usableWidth = (w * max(0.0, soc - backupSoc) / 100.0).toFloat().coerceAtLeast(0f)
        val grayReserve = (w * backupSoc / 100.0).toFloat()
        val grayUsable = (w * (100.0 - backupSoc) / 100.0).toFloat()
        val track = HumeColors.TextSecondary.copy(alpha = 0.25f)

        drawRoundRect(track, size = Size(grayReserve, h), cornerRadius = radius)
        drawRoundRect(track, topLeft = Offset(grayReserve, 0f), size = Size(grayUsable, h), cornerRadius = radius)

        if (reserveWidth > 0f) {
            drawRoundRect(tint, size = Size(reserveWidth, h), cornerRadius = radius)
        }
        if (usableWidth > 0f) {
            drawRoundRect(
                color = tint.copy(alpha = 0.30f),
                topLeft = Offset(reserveWidth, 0f),
                size = Size(usableWidth, h),
                cornerRadius = radius,
            )
            drawStripes(
                left = reserveWidth,
                right = reserveWidth + usableWidth,
                height = h,
                color = tint.copy(alpha = 0.75f),
            )
        }
    }
}

/** Canvas stripes of PowerwallCardView: 2.5pt lines, 5pt gaps, 45 degrees. */
private fun DrawScope.drawStripes(left: Float, right: Float, height: Float, color: Color) {
    clipRect(left = left, top = 0f, right = right, bottom = height) {
        var x = left - height
        while (x < right + height) {
            drawLine(
                color = color,
                start = Offset(x, height),
                end = Offset(x + height, 0f),
                strokeWidth = 2.5f,
            )
            x += 7.5f
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 10.sp, color = HumeColors.TextPrimary.copy(alpha = 0.6f))
    }
}
