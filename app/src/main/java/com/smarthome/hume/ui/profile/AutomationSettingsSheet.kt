package com.smarthome.hume.ui.profile

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeShapes
import com.smarthome.hume.ui.theme.glassSurface

/**
 * Port of Views/Profile/AutomationSettingsView.swift.
 *
 * Same groups and same preference keys as the iOS @AppStorage values, so the
 * two apps describe the same behaviour: proactive solar hints, the abnormal
 * consumption threshold, the temperature alert with its watched room, and the
 * morning digest. The Live Activity, HomeKit sync and geofence groups are
 * iOS-only and are left out.
 *
 * Nhom "Hen gio ngu canh" va hai cong tac chay kich ban theo gio da bi go bo
 * cung voi toan bo nhanh kich ban (scene / schedule) trong Dot 3.
 *
 * Unselected room rows and minute chips use the theme fill, so the sheet reads
 * correctly in dark mode instead of showing white slabs.
 */
private val tempRooms = listOf(
    "Ph\u00f2ng ng\u1ee7 l\u1edbn" to "sensor.cam_bien_moi_truong_phong_ngu_lon_temperature",
    "Ph\u00f2ng ng\u1ee7 b\u00e9" to "sensor.cam_bien_moi_truong_phong_ngu_be_temperature",
    "Ph\u00f2ng gi\u1eb7t" to "sensor.cam_bien_moi_truong_phong_giat_temperature",
    "Nh\u00e0 t\u1eafm T2" to "sensor.cam_bien_moi_truong_nha_tam_t2_temperature",
    "H\u00e0nh lang T2" to "sensor.cam_bien_moi_truong_hanh_lang_t2_temperature",
    "T\u1ea7ng 1" to "sensor.cam_bien_moi_truong_t1_temperature",
    "T\u1ea7ng 3" to "sensor.cam_bien_moi_truong_t3_temperature",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationSettingsSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("hume_automation", Context.MODE_PRIVATE) }

    var proactive by remember { mutableStateOf(prefs.getBoolean("sg_proactive_enabled", true)) }
    var anomaly by remember { mutableStateOf(prefs.getBoolean("sg_anomaly_enabled", true)) }
    var highLoad by remember { mutableStateOf(prefs.getInt("sg_high_load_w", 5000)) }
    var tempAlert by remember { mutableStateOf(prefs.getBoolean("temp_alert_enabled", true)) }
    var tempSensor by remember {
        mutableStateOf(prefs.getString("temp_alert_sensor", tempRooms.first().second).orEmpty())
    }
    var tempHot by remember { mutableStateOf(prefs.getInt("temp_alert_hot", 37)) }
    var coldOn by remember { mutableStateOf(prefs.getBoolean("temp_cold_alert_enabled", false)) }
    var tempCold by remember { mutableStateOf(prefs.getInt("temp_alert_cold", 16)) }
    var digest by remember { mutableStateOf(prefs.getBoolean("sg_digest_enabled", true)) }
    var digestHour by remember { mutableStateOf(prefs.getInt("sg_digest_hour", 7)) }
    var digestMinute by remember { mutableStateOf(prefs.getInt("sg_digest_minute", 0)) }

    fun putBool(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = HumeColors.Background,
        dragHandle = null,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = HumeColors.Orange,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "T\u1ef1 \u0111\u1ed9ng & c\u1ea3nh b\u00e1o",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = HumeColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = null, tint = HumeColors.TextSecondary)
                }
            }

            SettingGroup {
                ToggleRow(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "G\u1ee3i \u00fd ch\u1ee7 \u0111\u1ed9ng",
                    sub = "B\u00e1o khi d\u01b0 \u0111i\u1ec7n m\u1eb7t tr\u1eddi \u0111\u1ec3 t\u1eadn d\u1ee5ng (b\u00ecnh n\u00f3ng l\u1ea1nh, m\u00e1y gi\u1eb7t\u2026).",
                    checked = proactive,
                ) { proactive = it; putBool("sg_proactive_enabled", it) }
            }

            SettingGroup {
                ToggleRow(
                    icon = Icons.Rounded.Warning,
                    title = "C\u1ea3nh b\u00e1o ti\u00eau th\u1ee5 b\u1ea5t th\u01b0\u1eddng",
                    sub = "B\u00e1o khi \u0111i\u1ec7n cao k\u00e9o d\u00e0i (>10 ph\u00fat) ho\u1eb7c cao l\u00fac \u0111\u00eam khuya.",
                    checked = anomaly,
                ) { anomaly = it; putBool("sg_anomaly_enabled", it) }
                if (anomaly) {
                    Divider()
                    ValueStepper(
                        label = "Ng\u01b0\u1ee1ng c\u1ea3nh b\u00e1o",
                        value = String.format("%.1f kW", highLoad / 1000.0),
                        onDown = {
                            if (highLoad > 2000) { highLoad -= 500; putInt("sg_high_load_w", highLoad) }
                        },
                        onUp = {
                            if (highLoad < 10000) { highLoad += 500; putInt("sg_high_load_w", highLoad) }
                        },
                    )
                }
            }

            SettingGroup {
                ToggleRow(
                    icon = Icons.Rounded.Thermostat,
                    title = "C\u1ea3nh b\u00e1o nhi\u1ec7t \u0111\u1ed9",
                    sub = "B\u00e1o khi ph\u00f2ng \u0111\u00e3 ch\u1ecdn v\u01b0\u1ee3t ng\u01b0\u1ee1ng n\u00f3ng (ho\u1eb7c xu\u1ed1ng ng\u01b0\u1ee1ng l\u1ea1nh).",
                    checked = tempAlert,
                ) { tempAlert = it; putBool("temp_alert_enabled", it) }
                if (tempAlert) {
                    Divider()
                    Text(
                        "Ph\u00f2ng theo d\u00f5i",
                        fontSize = 14.sp,
                        color = HumeColors.TextPrimary,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        tempRooms.forEach { room ->
                            val selected = room.second == tempSensor
                            Text(
                                room.first,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) HumeColors.Orange else HumeColors.TextPrimary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (selected) HumeColors.OrangeSoft else HumeColors.FillTertiary,
                                    )
                                    .clickable {
                                        tempSensor = room.second
                                        prefs.edit().putString("temp_alert_sensor", room.second).apply()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                            )
                        }
                    }
                    Divider()
                    ValueStepper(
                        label = "Ng\u01b0\u1ee1ng n\u00f3ng",
                        value = tempHot.toString() + "\u00b0C",
                        onDown = { if (tempHot > 30) { tempHot -= 1; putInt("temp_alert_hot", tempHot) } },
                        onUp = { if (tempHot < 45) { tempHot += 1; putInt("temp_alert_hot", tempHot) } },
                    )
                    Divider()
                    ToggleRow(
                        icon = Icons.Rounded.AcUnit,
                        title = "C\u1ea3nh b\u00e1o l\u1ea1nh",
                        sub = "B\u00e1o khi nhi\u1ec7t \u0111\u1ed9 xu\u1ed1ng d\u01b0\u1edbi ng\u01b0\u1ee1ng (v\u00ed d\u1ee5 tr\u1eddi r\u00e9t).",
                        checked = coldOn,
                    ) { coldOn = it; putBool("temp_cold_alert_enabled", it) }
                    if (coldOn) {
                        ValueStepper(
                            label = "Ng\u01b0\u1ee1ng l\u1ea1nh",
                            value = tempCold.toString() + "\u00b0C",
                            accent = Color(0xFF4C9BF9),
                            onDown = { if (tempCold > 5) { tempCold -= 1; putInt("temp_alert_cold", tempCold) } },
                            onUp = { if (tempCold < 25) { tempCold += 1; putInt("temp_alert_cold", tempCold) } },
                        )
                    }
                }
            }

            SettingGroup {
                ToggleRow(
                    icon = Icons.Rounded.WbSunny,
                    title = "B\u00e1o c\u00e1o bu\u1ed5i s\u00e1ng",
                    sub = "Th\u00f4ng b\u00e1o t\u1ed5ng quan pin, chi ph\u00ed \u0111i\u1ec7n, \u0111\u00e8n \u0111ang b\u1eadt m\u1ed7i s\u00e1ng.",
                    checked = digest,
                ) { digest = it; putBool("sg_digest_enabled", it) }
                if (digest) {
                    Divider()
                    ValueStepper(
                        label = "Gi\u1edd g\u1eedi",
                        value = pad(digestHour) + ":" + pad(digestMinute),
                        onDown = {
                            digestHour = if (digestHour == 0) 23 else digestHour - 1
                            putInt("sg_digest_hour", digestHour)
                        },
                        onUp = {
                            digestHour = if (digestHour == 23) 0 else digestHour + 1
                            putInt("sg_digest_hour", digestHour)
                        },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0, 15, 30, 45).forEach { value ->
                            val on = digestMinute == value
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (on) HumeColors.Orange else HumeColors.FillTertiary)
                                    .clickable { digestMinute = value; putInt("sg_digest_minute", value) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    pad(value),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (on) Color.White else HumeColors.TextSecondary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingGroup(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().glassSurface(radius = HumeShapes.Sheet).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        content()
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    sub: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Icon(
            icon,
            contentDescription = null,
            tint = HumeColors.Orange,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = HumeColors.TextPrimary)
            Text(sub, fontSize = 12.sp, color = HumeColors.TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = HumeColors.Orange),
        )
    }
}

@Composable
private fun ValueStepper(
    label: String,
    value: String,
    accent: Color = HumeColors.Orange,
    onDown: () -> Unit,
    onUp: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 14.sp, color = HumeColors.TextPrimary, modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = accent)
        Spacer(Modifier.width(10.dp))
        Icon(
            Icons.Rounded.KeyboardArrowDown,
            contentDescription = null,
            tint = HumeColors.TextSecondary,
            modifier = Modifier.size(24.dp).clickable(onClick = onDown),
        )
        Icon(
            Icons.Rounded.KeyboardArrowUp,
            contentDescription = null,
            tint = HumeColors.TextSecondary,
            modifier = Modifier.size(24.dp).clickable(onClick = onUp),
        )
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(HumeColors.Divider))
}

private fun pad(value: Int): String = if (value < 10) "0" + value else value.toString()
