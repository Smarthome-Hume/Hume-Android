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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeIcons
import com.smarthome.hume.ui.theme.glassSurface
import kotlin.math.max
import kotlin.math.min

/*
 * PowerwallCardView.swift -> Android, bam sat ban goc:
 *   .frame(height: 210)   -> chieu cao CO DINH 210dp (khong con heightIn nen
 *                            the bi cao dan len so voi ban Swift)
 *   barH = 28             -> thanh bar 28dp
 *   padding top18 left20 bottom16 right20
 *
 * Thanh bar kieu Live Activity, dung HAI segment nhu Swift:
 *   - nen xam segment 1 (vung du tru = backupSoc%)
 *   - nen xam segment 2 (vung su dung = 100 - backupSoc%)
 *   - doan du tru: to dac
 *   - doan su dung: GACH CHEO ve bang Canvas, cat trong hinh vien thuoc nen
 *     khong bao gio lo goc vuong
 */
private val BatteryCardHeight = 210.dp
private val BatteryCardRadius = 36.dp
private val BarHeight = 28.dp

private val BarGreen = Color(0xFF22C55E)
private val BarOrange = Color(0xFFF97316)
private val BarSlate = Color(0xFF64748B)
private val LegendBlue = Color(0xFF3B82F6)
private val LegendGrey = Color(0xFF94A3B8)
private val DischargeAccent = Color(0xFFF9784C)

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
        Column(Modifier.fillMaxWidth()) {
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

            Spacer(Modifier.weight(1f))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        status,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = HumeColors.TextPrimary.copy(alpha = 0.9f),
                    )
                    if (!resting) {
                        Text(
                            timeText,
                            fontSize = 38.sp,
                            lineHeight = 42.sp,
                            color = HumeColors.TextPrimary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (!resting && finishTime.isNotBlank() && finishTime != "--:--") {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            "K\u1ebeT TH\u00daC L\u00daC",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = HumeColors.TextPrimary.copy(alpha = 0.5f),
                            maxLines = 1,
                        )
                        Text(
                            finishTime,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = HumeColors.TextPrimary,
                            maxLines = 1,
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

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

/**
 * dualBar trong PowerwallCardView.swift.
 *
 * Bon lop, dung thu tu ban goc:
 *   1. nen xam segment 1 (0 -> backupSoc%)
 *   2. nen xam segment 2 (backupSoc% -> 100%)
 *   3. doan du tru: to dac mau trang thai
 *   4. doan su dung: gach cheo Canvas (nen alpha .30, net alpha .75,
 *      spacing 5 + lineWidth 2.5), cat trong hinh vien thuoc.
 */
@Composable
private fun DualBar(soc: Double, backupSoc: Double, tint: Color) {
    Canvas(Modifier.fillMaxWidth().height(BarHeight)) {
        val w = size.width
        val h = size.height
        val radius = CornerRadius(h / 2f, h / 2f)
        val track = HumeColors.TextSecondary.copy(alpha = 0.25f)

        val reserve = (w * min(soc, backupSoc) / 100.0).toFloat().coerceIn(0f, w)
        val usable = (w * max(0.0, soc - backupSoc) / 100.0).toFloat().coerceIn(0f, w - reserve)
        val grayReserve = (w * backupSoc / 100.0).toFloat().coerceIn(0f, w)
        val grayUsable = w - grayReserve

        // 1 + 2: hai segment nen xam.
        if (grayReserve > 0f) {
            drawRoundRect(track, size = Size(grayReserve, h), cornerRadius = radius)
        }
        if (grayUsable > 0f) {
            drawRoundRect(
                track,
                topLeft = Offset(grayReserve, 0f),
                size = Size(grayUsable, h),
                cornerRadius = radius,
            )
        }

        // 3: doan du tru, to dac.
        if (reserve > 0f) {
            drawRoundRect(tint, size = Size(reserve, h), cornerRadius = radius)
        }

        // 4: doan su dung, gach cheo trong hinh vien thuoc.
        if (usable > 0f) {
            val shape = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(offset = Offset(reserve, 0f), size = Size(usable, h)),
                        cornerRadius = radius,
                    )
                )
            }
            clipPath(shape) {
                drawRect(
                    tint.copy(alpha = 0.30f),
                    topLeft = Offset(reserve, 0f),
                    size = Size(usable, h),
                )
                var x = reserve - h
                while (x < reserve + usable + h) {
                    drawLine(
                        color = tint.copy(alpha = 0.75f),
                        start = Offset(x, 0f),
                        end = Offset(x + h, h),
                        strokeWidth = 2.5f,
                    )
                    x += 7.5f
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            fontSize = 10.sp,
            color = HumeColors.TextPrimary.copy(alpha = 0.6f),
            maxLines = 1,
            softWrap = false,
        )
    }
}
