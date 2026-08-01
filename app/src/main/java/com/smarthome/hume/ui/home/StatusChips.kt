package com.smarthome.hume.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeIcons

/** Two compact pills: alarm mode on the left, light count on the right. */
@Composable
fun StatusChipRow(
    alarmState: String?,
    lightsOn: Int,
    onOpenAlarm: () -> Unit,
    onOpenLights: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusChip(
            icon = HumeIcons.Alarm,
            label = alarmLabel(alarmState),
            container = HumeColors.ChipPink,
            iconTint = HumeColors.Orange,
            onClick = onOpenAlarm,
        )
        StatusChip(
            icon = HumeIcons.Light,
            label = if (lightsOn == 0) "Kh\u00f4ng b\u00f3ng n\u00e0o b\u1eadt" else "$lightsOn b\u00f3ng \u0111\u00e8n",
            container = HumeColors.ChipYellow,
            iconTint = HumeColors.ChipYellowIcon,
            onClick = onOpenLights,
        )
    }
}

@Composable
private fun StatusChip(
    icon: ImageVector,
    label: String,
    container: Color,
    iconTint: Color,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(7.dp))
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = HumeColors.TextPrimary,
            maxLines = 1,
        )
    }
}
