package com.smarthome.hume.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.HumeConfig
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeIcons

/*
 * Hai chip trang thai. Da THU NHO cho can doi voi khung hinh S26U:
 *  - chieu cao 42 (truoc 48), bo goc 21
 *  - vong tron icon 30 (truoc 36), icon 17 (truoc 20)
 *  - chu 13 (truoc 14)
 */
private val LightsGold = Color(0xFFB8860B)
private val AlarmGreen = Color(0xFF4CAF50)
private val PillHeight = 42.dp
private val PillRadius = 21.dp
private val IconCircle = 30.dp

@Composable
fun StatusChipRow(
    alarmState: String?,
    lightsOn: Int,
    ha: HomeAssistantRepository,
    alarmEntity: String,
    onOpenLights: () -> Unit,
) {
    var picking by remember { mutableStateOf(false) }
    var pendingState by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(alarmState) {
        if (pendingState != null && alarmState == pendingState) pendingState = null
    }

    AnimatedContent(targetState = picking, label = "alarmPicker") { open ->
        if (open) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HumeConfig.alarmModes.forEach { (service, label, target) ->
                    val selected = alarmState == target
                    ModePill(
                        icon = modeIcon(service),
                        label = label,
                        tint = modeColor(service),
                        selected = selected,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            pendingState = target
                            if (service == "disarm") {
                                ha.alarmDisarm(alarmEntity, HumeConfig.ALARM_CODE.toString())
                            } else {
                                ha.alarmArm(
                                    alarmEntity,
                                    service.removePrefix("arm_"),
                                    HumeConfig.ALARM_CODE.toString(),
                                )
                            }
                            picking = false
                        },
                    )
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val armed = alarmState != null && alarmState != "disarmed"
                BigPill(
                    icon = HumeIcons.Alarm,
                    label = if (pendingState != null) "\u0110ang b\u1eadt..." else alarmLabel(alarmState),
                    tint = AlarmGreen,
                    active = armed,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        picking = true
                    },
                )
                BigPill(
                    icon = HumeIcons.Light,
                    label = if (lightsOn == 0) "T\u1eaft h\u1ebft" else "$lightsOn b\u00f3ng \u0111\u00e8n",
                    tint = LightsGold,
                    active = lightsOn > 0,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenLights,
                )
            }
        }
    }
}

@Composable
private fun BigPill(
    icon: ImageVector,
    label: String,
    tint: Color,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .height(PillHeight)
            .clip(RoundedCornerShape(PillRadius))
            .background(if (active) tint.copy(alpha = 0.08f) else HumeColors.Card)
            .border(
                1.dp,
                if (active) tint.copy(alpha = 0.3f) else Color.Transparent,
                RoundedCornerShape(PillRadius),
            )
            .clickable(onClick = onClick)
            .padding(start = 6.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(IconCircle).clip(CircleShape).background(HumeColors.Gray00),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (active) tint else HumeColors.Gray1000,
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.width(7.dp))
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (active) tint else HumeColors.Gray1000,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ModePill(
    icon: ImageVector,
    label: String,
    tint: Color,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(if (selected) tint else HumeColors.Gray00)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) Color.White else HumeColors.Gray1000,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) Color.White else HumeColors.Gray1000,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun modeIcon(service: String): ImageVector = when (service) {
    "arm_home" -> Icons.Rounded.Home
    "arm_away" -> Icons.Rounded.Map
    "arm_night" -> Icons.Rounded.Bedtime
    "arm_custom_bypass" -> Icons.Rounded.WbTwilight
    else -> Icons.Rounded.Shield
}

private fun modeColor(service: String): Color = when (service) {
    "arm_home" -> AlarmGreen
    "arm_away" -> Color(0xFFFF9800)
    "arm_night" -> Color(0xFF5C6BC0)
    "arm_custom_bypass" -> Color(0xFFF2D26F)
    else -> Color(0xFF757575)
}
