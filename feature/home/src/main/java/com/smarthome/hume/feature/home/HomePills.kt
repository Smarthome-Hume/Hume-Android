package com.smarthome.hume.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.model.AlarmUi
import com.smarthome.hume.core.ui.components.M3EIcons

/**
 * Cum pills trang Nha theo demo v4 (.pills/.pill/.secmodes/.smode):
 * [secPill "An ninh"] [bulbPill "n bong dang sang"]; bam secPill mo rong
 * thanh dai scroll-x hien 4 che do (O nha / Vang nha / Ban dem / Tat).
 */
@Composable
fun PillsRow(
    alarm: AlarmUi?,
    lightsOnCount: Int,
    onArm: (mode: String, label: String) -> Unit,
    onDisarm: () -> Unit,
    onLights: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    AnimatedContent(targetState = expanded, label = "pills", modifier = modifier) { ex ->
        if (!ex) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                SecPill(
                    title = "An ninh",
                    sub = alarm?.label ?: "Chưa rõ",
                    iconOn = alarm?.isArmed == true,
                    onClick = { expanded = true },
                    modifier = Modifier.weight(1f),
                )
                BulbPill(count = lightsOnCount, onClick = onLights,
                    modifier = Modifier.weight(1f))
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                SecPill(
                    title = "An ninh",
                    sub = alarm?.label ?: "Chưa rõ",
                    iconOn = alarm?.isArmed == true,
                    onClick = { expanded = false },
                    modifier = Modifier.width(150.dp),
                )
                SecurityModes(alarm = alarm, onArm = onArm, onDisarm = onDisarm)
                BulbPill(count = lightsOnCount, onClick = onLights,
                    modifier = Modifier.width(128.dp))
            }
        }
    }
}

@Composable
private fun SecPill(
    title: String,
    sub: String,
    iconOn: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PillShell(onClick = onClick, modifier = modifier) {
        PillIcon(M3EIcons.Shield, tintOn = iconOn)
        PillTexts(title = title, sub = sub)
    }
}

@Composable
private fun BulbPill(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PillShell(onClick = onClick, modifier = modifier) {
        PillIcon(M3EIcons.Light, tintOn = count > 0)
        PillTexts(
            title = if (count > 0) "$count bóng" else "Không có",
            sub = if (count > 0) "Đang sáng" else "Đèn tắt",
        )
    }
}

/** .pill: surfaceHighest, bo 28dp, padding 14dp, gap 11dp. */
@Composable
private fun PillShell(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        content()
    }
}

@Composable
private fun PillIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, tintOn: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Icon(
            icon, contentDescription = null,
            tint = if (tintOn) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun PillTexts(title: String, sub: String) {
    Column {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface)
        Text(sub, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private data class AlarmMode(val service: String, val label: String)

@Composable
private fun SecurityModes(
    alarm: AlarmUi?,
    onArm: (mode: String, label: String) -> Unit,
    onDisarm: () -> Unit,
) {
    val modes = listOf(
        AlarmMode("home", "Ở nhà") to M3EIcons.Home,
        AlarmMode("away", "Vắng nhà") to M3EIcons.FlightTakeoff,
        AlarmMode("night", "Ban đêm") to M3EIcons.Bedtime,
        AlarmMode("disarm", "Tắt") to M3EIcons.PowerSettingsNew,
    )
    modes.forEach { (m, icon) ->
        val selected = when (m.service) {
            "home" -> alarm?.state == "armed_home"
            "away" -> alarm?.state == "armed_away"
            "night" -> alarm?.state == "armed_night"
            else -> alarm?.state == "disarmed"
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .width(92.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHighest,
                )
                .clickable { if (m.service == "disarm") onDisarm() else onArm(m.service, m.label) }
                .padding(horizontal = 10.dp, vertical = 14.dp),
        ) {
            Icon(
                icon, contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Text(
                m.label,
                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
