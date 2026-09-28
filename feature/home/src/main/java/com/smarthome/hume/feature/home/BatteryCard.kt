package com.smarthome.hume.feature.home

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.model.BatteryUi
import com.smarthome.hume.core.ui.components.M3ECard
import com.smarthome.hume.core.ui.components.WavyBatteryBar

/**
 * The pin — dung demo rev12 (.batcard): header + badge trang thai,
 * so % lon, cong suat, thanh wavy, legend, footer thoi gian.
 */
@Composable
fun BatteryCard(battery: BatteryUi, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    M3ECard(
        shape = RoundedCornerShape(32.dp),
        contentPadding = 20.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        // Hang tren: tieu de + badge trang thai
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "Hiệu năng pin",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            val (badgeText, badgeBg, badgeFg) = when {
                battery.isCharging -> Triple("ĐANG SẠC", cs.tertiaryContainer, cs.onTertiaryContainer)
                battery.powerKw > 0.05 -> Triple("ĐANG XẢ", cs.secondaryContainer, cs.onSecondaryContainer)
                else -> Triple("CHỜ", cs.surfaceContainerHigh, cs.onSurfaceVariant)
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(badgeBg)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    badgeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = badgeFg,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        // So % + cong suat
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "${battery.soc}",
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = cs.onSurface,
            )
            Text(
                "%",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = cs.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp, start = 2.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.padding(bottom = 4.dp)) {
                Text(
                    "%+.1f kW".format(battery.powerKw),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (battery.isCharging) cs.tertiary else cs.primary,
                )
                Text(
                    if (battery.isCharging) "Đang sạc" else "Công suất",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = cs.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        // Thanh wavy
        WavyBatteryBar(soc = battery.soc, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        // Legend: du tru / su dung
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LegendDot(cs.primary, "Dự trữ ${battery.reservePct}%")
            LegendDot(cs.tertiary, "Sử dụng ${battery.usagePct}%")
        }
        // Footer thoi gian
        battery.timeText?.let {
            Spacer(Modifier.height(8.dp))
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
