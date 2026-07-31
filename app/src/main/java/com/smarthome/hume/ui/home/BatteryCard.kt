package com.smarthome.hume.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeIcons

/**
 * "Hi\u1ec7u n\u0103ng Pin" card: charge state, headline time and a rounded progress bar.
 */
@Composable
fun BatteryCard(
    percent: Double?,
    charging: Boolean,
    headline: String,
    trailingLabel: String,
    trailingValue: String,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(HumeColors.OrangeSofter, HumeColors.OrangeSoft)
                )
            )
            .padding(22.dp)
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Hi\u1ec7u n\u0103ng Pin",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HumeColors.TextPrimary,
                )
                Box(
                    Modifier.size(46.dp).clip(CircleShape).background(HumeColors.Orange.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(HumeIcons.Battery, contentDescription = null, tint = HumeColors.OrangeDeep, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (charging) "\u0110ANG S\u1ea0C" else "\u0110ANG X\u1ea2",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HumeColors.TextSecondary,
                    )
                    Text(
                        headline,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = HumeColors.TextPrimary,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(trailingLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = HumeColors.TextSecondary)
                    Text(trailingValue, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = HumeColors.TextPrimary)
                }
            }
            Spacer(Modifier.height(16.dp))
            BatteryBar(percent)
        }
    }
}

@Composable
private fun BatteryBar(percent: Double?) {
    val fraction = ((percent ?: 0.0) / 100.0).coerceIn(0.0, 1.0).toFloat()
    Box(
        Modifier
            .fillMaxWidth()
            .height(38.dp)
            .clip(RoundedCornerShape(19.dp))
            .background(Color.White)
    ) {
        if (fraction > 0f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(19.dp))
                    .background(
                        Brush.horizontalGradient(listOf(HumeColors.Orange, Color(0xFFFFA476)))
                    ),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    if (percent == null) "--" else String.format(java.util.Locale.US, "%.0f%%", percent),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(start = 18.dp),
                )
            }
        } else {
            Text(
                "--",
                color = HumeColors.TextSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(start = 18.dp),
            )
        }
    }
}
