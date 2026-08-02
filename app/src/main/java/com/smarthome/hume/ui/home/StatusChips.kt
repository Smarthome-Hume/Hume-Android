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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.HumeConfig
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeIcons

private val LightsYellow = Color(0xFFFFEB3B)
private val PillHeight = 48.dp
private val PillRadius = 24.dp

/**
 * AlarmLightsView from AlarmLights.swift.
 *
 * Two 48dp pills sit side by side: the alarm mode on the left, the number of
 * lights that are on right next to it. Tapping the alarm pill replaces the
 * whole row with the five mode buttons, exactly like the iOS version. The
 * chosen mode is only painted as active once Home Assistant reports it; until
 * then the pill says "Dang bat...", and a refused call rolls the state back
 * because callService re-reads the entity when the POST is not accepted.
 */
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

    // Home Assistant answered: drop the intermediate "arming" label.
    LaunchedEffect(alarmState) {
        if (pendingState != null && alarmState == pendingState) pendingState = null
    }

    AnimatedContent(targetState = picking, label = "alarmPicker") { open ->
        if (open) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
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
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BigPill(
                    icon = HumeIcons.Alarm,
                    label = if (pendingState != null) "\u0110ang b\u1eadt..." else alarmLabel(alarmState),
                    circleTint = HumeColors.Orange,
                    active = alarmState != null && alarmState != "disarmed",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        picking = true
                    },
                )
                BigPill(
                    icon = HumeIcons.Light,
                    label = if (lightsOn == 0) "Kh\u00f4ng b\u00f3ng n\u00e0o b\u1eadt"
                    else "$lightsOn b\u00f3ng \u0111\u00e8n",
                    circleTint = LightsYellow,
                    active = lightsOn > 0,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenLights,
                )
            }
        }
    }
}

/**
 * 48dp pill with a 34dp icon circle, the shared shape of both header cards.
 *
 * The inactive fill used to be a hardcoded white, which turned into a glaring
 * white slab in dark mode. SwiftUI paints this pill with the card surface and
 * the icon well with tertiarySystemFill, so both follow the theme here too.
 */
@Composable
private fun BigPill(
    icon: ImageVector,
    label: String,
    circleTint: Color,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .height(PillHeight)
            .clip(RoundedCornerShape(PillRadius))
            .background(if (active) circleTint.copy(alpha = 0.10f) else HumeColors.Card)
            .border(
                1.dp,
                if (active) circleTint.copy(alpha = 0.40f) else HumeColors.Divider,
                RoundedCornerShape(PillRadius),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (active) circleTint.copy(alpha = 0.22f) else HumeColors.FillTertiary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (active) circleTint else HumeColors.TextSecondary,
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = HumeColors.TextPrimary,
            maxLines = 1,
        )
    }
}

/** One of the five alarm modes, shown only while the picker is open. */
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
            .height(PillHeight)
            .clip(RoundedCornerShape(PillRadius))
            .background(if (selected) tint.copy(alpha = 0.18f) else HumeColors.Card)
            .border(
                1.dp,
                if (selected) tint.copy(alpha = 0.55f) else HumeColors.Divider,
                RoundedCornerShape(PillRadius),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) tint else HumeColors.TextSecondary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = HumeColors.TextPrimary,
                maxLines = 1,
            )
        }
    }
}

/** menuModes icons in AlarmLights.swift. */
private fun modeIcon(service: String): ImageVector = when (service) {
    "arm_home" -> Icons.Rounded.Home
    "arm_away" -> Icons.Rounded.Map
    "arm_night" -> Icons.Rounded.Bedtime
    "arm_custom_bypass" -> Icons.Rounded.WbTwilight
    else -> Icons.Rounded.Shield
}

/** menuModes colors in AlarmLights.swift. */
private fun modeColor(service: String): Color = when (service) {
    "arm_home" -> HumeColors.Green
    "arm_away" -> HumeColors.Orange
    "arm_night" -> HumeColors.TextSecondary
    "arm_custom_bypass" -> LightsYellow
    else -> HumeColors.TextSecondary
}
