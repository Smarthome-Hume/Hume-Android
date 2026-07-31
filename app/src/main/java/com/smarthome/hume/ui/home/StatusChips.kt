package com.smarthome.hume.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeIcons

/** The two pills under the header: alarm mode on the left, lights on the right. */
@Composable
fun StatusChipRow(
    alarmState: String?,
    lightsOn: Int,
    onOpenAlarm: () -> Unit,
    onOpenLights: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusChip(
            icon = HumeIcons.Night,
            label = alarmLabel(alarmState),
            iconBackground = HumeColors.ChipBlue,
            iconTint = HumeColors.ChipBlueIcon,
            container = HumeColors.ChipBlue.copy(alpha = 0.55f),
            borderColor = Color.Transparent,
            modifier = Modifier.weight(1f),
            onClick = onOpenAlarm,
        )
        StatusChip(
            icon = HumeIcons.Light,
            label = if (lightsOn == 0) "Kh\u00f4ng b\u00f3ng n\u00e0o b\u1eadt" else "$lightsOn b\u00f3ng \u0111\u00e8n",
            iconBackground = Color.Transparent,
            iconTint = HumeColors.Amber,
            container = Color.White,
            borderColor = HumeColors.Amber.copy(alpha = 0.6f),
            modifier = Modifier.weight(1f),
            onClick = onOpenLights,
        )
    }
}

@Composable
private fun StatusChip(
    icon: ImageVector,
    label: String,
    iconBackground: Color,
    iconTint: Color,
    container: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(28.dp))
            .background(container)
            .border(1.5.dp, borderColor, RoundedCornerShape(28.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
        )
    }
}
