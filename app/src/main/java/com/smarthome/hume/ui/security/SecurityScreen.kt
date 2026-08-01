package com.smarthome.hume.ui.security

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.DoorFront
import androidx.compose.material.icons.rounded.House
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.core.model.HumeConfig
import com.smarthome.hume.ui.theme.HumeColors

private data class SensorDef(val id: String, val name: String, val icon: ImageVector, val alert: Color? = null)

/** DOOR_SENSORS in SecurityView.swift */
private val doorSensors = listOf(
    SensorDef("binary_sensor.cam_bien_cua_kinh_contact", "C\u1eeda k\u00ednh", Icons.Rounded.DoorFront),
    SensorDef("binary_sensor.cam_bien_cua_phong_ngu_chinh_contact", "C\u1eeda ph\u00f2ng ng\u1ee7 ch\u00ednh", Icons.Rounded.DoorFront),
    SensorDef("binary_sensor.cam_bien_cua_phong_ngu_be_contact", "C\u1eeda ph\u00f2ng ng\u1ee7 b\u00e9", Icons.Rounded.DoorFront),
    SensorDef("binary_sensor.cam_bien_cua_phong_tam_contact", "C\u1eeda ph\u00f2ng t\u1eafm", Icons.Rounded.DoorFront),
    SensorDef("binary_sensor.cam_bien_cua_ban_cong_tt2_contact", "C\u1eeda ban c\u00f4ng T2", Icons.Rounded.DoorFront),
    SensorDef("binary_sensor.cam_bien_ban_cong_t3_contact", "C\u1eeda ban c\u00f4ng T3", Icons.Rounded.DoorFront),
)

/** MOTION_SENSORS in SecurityView.swift */
private val motionSensors = listOf(
    SensorDef("binary_sensor.cam_bien_pir_t1_occupancy", "PIR T\u1ea7ng 1", Icons.Rounded.DirectionsWalk),
    SensorDef("binary_sensor.cam_bien_pir_t2_occupancy", "PIR T\u1ea7ng 2", Icons.Rounded.DirectionsWalk),
    SensorDef("binary_sensor.cam_bien_pir_t3_occupancy", "PIR T\u1ea7ng 3", Icons.Rounded.DirectionsWalk),
    SensorDef("binary_sensor.cam_bien_hien_dien_presence", "C\u1ea3m bi\u1ebfn hi\u1ec7n di\u1ec7n", Icons.Rounded.DirectionsWalk),
    SensorDef("binary_sensor.cam_bien_pir_phong_tho_occupancy", "PIR ph\u00f2ng th\u1edd", Icons.Rounded.DirectionsWalk),
    SensorDef("binary_sensor.cam_bien_tuong_t2_occupancy", "C\u1ea3m bi\u1ebfn t\u01b0\u1eddng T2", Icons.Rounded.DirectionsWalk),
)

/** ENV_SENSORS in SecurityView.swift */
private val envSensors = listOf(
    SensorDef("binary_sensor.cam_bien_khoi_smoke", "Kh\u00f3i", Icons.Rounded.Whatshot, Color(0xFFFF6D00)),
    SensorDef("binary_sensor.cam_bien_nuoc_water_leak", "R\u00f2 r\u1ec9 n\u01b0\u1edbc", Icons.Rounded.WaterDrop, Color(0xFF2196F3)),
)

@Composable
fun SecurityScreen(ha: HomeAssistantRepository) {
    val entities by ha.entities.collectAsState()
    val alarmId = if (entities.containsKey(HumeConfig.ALARM_PRIMARY)) HumeConfig.ALARM_PRIMARY else HumeConfig.ALARM_FALLBACK
    val alarmState = entities[alarmId]?.state ?: "unknown"

    Column(
        Modifier
            .fillMaxSize()
            .background(HumeColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text("An ninh", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = HumeColors.TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text(HumeConfig.alarmLabel(alarmState), fontSize = 14.sp, color = HumeColors.TextSecondary)
        Spacer(Modifier.height(14.dp))

        // Five alarm modes, same order and payload as AlarmLights.swift.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HumeConfig.alarmModes.forEach { (service, label, activeState) ->
                val active = alarmState == activeState
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (active) HumeColors.SceneGreenBg else Color.White)
                        .border(
                            1.dp,
                            if (active) HumeColors.SceneGreen else HumeColors.Divider,
                            RoundedCornerShape(20.dp),
                        )
                        .clickable {
                            if (service == "disarm") {
                                ha.alarmDisarm(alarmId, HumeConfig.ALARM_CODE)
                            } else {
                                ha.alarmArm(alarmId, service.removePrefix("arm_"), HumeConfig.ALARM_CODE)
                            }
                        }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        alarmModeIcon(service),
                        contentDescription = null,
                        tint = if (active) HumeColors.SceneGreen else HumeColors.TextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        label,
                        fontSize = 11.sp,
                        color = if (active) HumeColors.SceneGreen else HumeColors.TextSecondary,
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        SensorSection("C\u1eeda", doorSensors, entities)
        SensorSection("Chuy\u1ec3n \u0111\u1ed9ng", motionSensors, entities)
        SensorSection("M\u00f4i tr\u01b0\u1eddng", envSensors, entities)
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun SensorSection(title: String, sensors: List<SensorDef>, entities: Map<String, HomeEntity>) {
    Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = HumeColors.TextPrimary)
    Spacer(Modifier.height(8.dp))
    sensors.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { sensor ->
                Box(Modifier.weight(1f)) { BinarySensorCard(sensor, entities[sensor.id]) }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun BinarySensorCard(sensor: SensorDef, entity: HomeEntity?) {
    val isOn = entity?.isOn == true
    val accent = sensor.alert ?: HumeColors.Orange
    val minutes = entity?.minutesAgo()
    Row(
        Modifier
            .fillMaxWidth()
            .height(76.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (isOn) accent.copy(alpha = 0.10f) else Color.White)
            .border(1.dp, if (isOn) accent.copy(alpha = 0.40f) else HumeColors.Divider, RoundedCornerShape(24.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(HumeColors.Background),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                sensor.icon,
                contentDescription = null,
                tint = if (isOn) accent else HumeColors.TextSecondary,
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(sensor.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = HumeColors.TextPrimary, maxLines = 2)
            Text(
                when {
                    entity == null -> "kh\u00f4ng r\u00f5"
                    isOn -> "M\u1edf"
                    else -> "\u0110\u00f3ng"
                } + (minutes?.let { " \u00b7 " + agoLabel(it) } ?: ""),
                fontSize = 11.sp,
                color = HumeColors.TextSecondary,
            )
        }
    }
}

/** agoText() in DoorCardView.swift */
private fun agoLabel(minutes: Int): String = when {
    minutes < 1 -> "V\u1eeba xong"
    minutes < 60 -> minutes.toString() + " ph\u00fat tr\u01b0\u1edbc"
    else -> (minutes / 60).toString() + " gi\u1edd tr\u01b0\u1edbc"
}

private fun alarmModeIcon(service: String): ImageVector = when (service) {
    "disarm" -> Icons.Rounded.Shield
    "arm_home" -> Icons.Rounded.House
    "arm_away" -> Icons.Rounded.Map
    "arm_night" -> Icons.Rounded.NightsStay
    else -> Icons.Rounded.WbTwilight
}
