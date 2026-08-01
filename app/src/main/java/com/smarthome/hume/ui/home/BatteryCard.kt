package com.smarthome.hume.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeIcons
import java.util.Locale

/** "Hi\u1ec7u n\u0103ng Pin" card with the two segment bar and its legend. */
@Composable
fun BatteryCard(
    percent: Double?,
    charging: Boolean,
    headline: String,
    trailingLabel: String,
    trailingValue: String,
) {
    val value = percent ?: 0.0
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(HumeColors.OrangeSofter)
            .padding(18.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Hi\u1ec7u n\u0103ng Pin",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HumeColors.TextPrimary,
                )
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(HumeColors.Ink),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(HumeIcons.Battery, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (charging) "\u0110ANG S\u1ea0C" else "\u0110ANG X\u1ea2",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HumeColors.TextSecondary,
                    )
                    Text(headline, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = HumeColors.TextPrimary)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(trailingLabel, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = HumeColors.TextSecondary)
                    Text(trailingValue, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HumeColors.TextPrimary)
                }
            }
            Spacer(Modifier.height(12.dp))
            SegmentedBar(value)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LegendDot(HumeColors.Orange, "D\u1ef1 tr\u1eef " + percentText(value))
                LegendDot(HumeColors.SalmonSoft, "S\u1eed d\u1ee5ng " + percentText(100.0 - value))
            }
        }
    }
}

private fun percentText(value: Double): String = String.format(Locale.US, "%.0f%%", value.coerceIn(0.0, 100.0))

@Composable
private fun SegmentedBar(percent: Double) {
    val fraction = (percent / 100.0).coerceIn(0.0, 1.0).toFloat()
    val hatch = HumeColors.SalmonSoft
    Box(
        Modifier
            .fillMaxWidth()
            .height(22.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(Color.White)
    ) {
        Canvas(Modifier.fillMaxWidth().height(22.dp)) {
            val filled = size.width * fraction
            drawRoundRect(
                color = HumeColors.Orange,
                size = androidx.compose.ui.geometry.Size(filled, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f, size.height / 2f),
            )
            // Hatched "in use" segment right after the solid one.
            val hatchWidth = (size.width - filled) * 0.55f
            if (hatchWidth > 4f) {
                drawRoundRect(
                    color = hatch.copy(alpha = 0.55f),
                    topLeft = Offset(filled, 0f),
                    size = androidx.compose.ui.geometry.Size(hatchWidth, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f, size.height / 2f),
                )
                var x = filled + 6f
                while (x < filled + hatchWidth) {
                    drawLine(
                        color = Color.White,
                        start = Offset(x, size.height),
                        end = Offset(x + size.height * 0.7f, 0f),
                        strokeWidth = 3f,
                    )
                    x += 14f
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 10.sp, color = HumeColors.TextSecondary)
    }
}
