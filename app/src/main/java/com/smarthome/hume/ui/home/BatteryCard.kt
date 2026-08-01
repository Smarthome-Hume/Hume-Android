package com.smarthome.hume.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeIcons
import kotlin.math.max
import kotlin.math.min

/**
 * "Hi\u1ec7u n\u0103ng Pin" card ported from PowerwallCardView.swift:
 * reserve segment solid, usable segment diagonally striped, three states.
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
    val accent = when {
        charging -> HumeColors.Green
        discharging -> HumeColors.Orange
        else -> HumeColors.TextSecondary
    }

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(if (resting) Color.White else accent.copy(alpha = 0.10f))
            .border(1.dp, if (resting) HumeColors.Divider else accent.copy(alpha = 0.40f), RoundedCornerShape(30.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Hi\u1ec7u n\u0103ng Pin",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HumeColors.TextPrimary,
                )
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(HumeColors.Background),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(HumeIcons.Battery, contentDescription = null, tint = HumeColors.TextPrimary, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(status, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = HumeColors.TextSecondary)
                    if (!resting) {
                        Text(timeText, fontSize = 36.sp, color = HumeColors.TextPrimary)
                    }
                }
                if (!resting && finishTime.isNotBlank() && finishTime != "--:--") {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            if (charging) "\u0110\u1ea6Y L\u00daC" else "K\u1ebeT TH\u00daC L\u00daC",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = HumeColors.TextSecondary,
                        )
                        Text(finishTime, fontSize = 19.sp, fontWeight = FontWeight.Medium, color = HumeColors.TextPrimary)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            DualBar(soc = soc, backupSoc = backupSoc, tint = accent)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LegendDot(
                    color = if (charging) HumeColors.Green else HumeColors.TextSecondary,
                    label = "D\u1ef1 tr\u1eef " + backupSoc.toInt() + "%",
                )
                LegendDot(
                    color = when {
                        charging -> HumeColors.Blue
                        discharging -> HumeColors.Orange
                        else -> HumeColors.TextSecondary
                    },
                    label = "S\u1eed d\u1ee5ng " + max(0.0, soc - backupSoc).toInt() + "%",
                )
            }
        }
    }
}

@Composable
private fun DualBar(soc: Double, backupSoc: Double, tint: Color) {
    val height = 28.dp
    Canvas(Modifier.fillMaxWidth().height(height)) {
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
            clipRect(reserveWidth, 0f, reserveWidth + usableWidth, h) {
                var x = reserveWidth - h
                while (x < reserveWidth + usableWidth + h) {
                    drawLine(
                        color = tint.copy(alpha = 0.75f),
                        start = Offset(x, h),
                        end = Offset(x + h, 0f),
                        strokeWidth = 2.5f,
                    )
                    x += 7.5f
                }
            }
        }
    }
}

private inline fun androidx.compose.ui.graphics.drawscope.DrawScope.clipRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    block: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit,
) {
    androidx.compose.ui.graphics.drawscope.clipRect(left, top, right, bottom) { block() }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 10.sp, color = HumeColors.TextSecondary)
    }
}
