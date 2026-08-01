package com.smarthome.hume.ui.energy

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.ElectricalServices
import androidx.compose.material.icons.rounded.House
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.smarthome.hume.ui.theme.GlassCard
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeShapes
import com.smarthome.hume.ui.theme.glassPill
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val Blue = Color(0xFF2196F3)
private val Brick = Color(0xFFEB5F34)
private val Yellow = Color(0xFFFFEB3B)
private val GreenSave = Color(0xFF3BA776)

/** Power bars in the consumption tab (EnergyView.swift). */
private val powerBars = listOf(
    Triple("sensor.battery_power_flow", "C\u00f4ng su\u1ea5t pin", HumeColors.Green),
    Triple(HumeConfig.PV_POWER, "\u0110i\u1ec7n m\u1eb7t tr\u1eddi", Yellow),
    Triple("sensor.aptomat_tong_power", "\u0110i\u1ec7n l\u01b0\u1edbi", Blue),
    Triple("sensor.cong_suat_nha", "C\u00f4ng su\u1ea5t nh\u00e0", Brick),
)

/** Load distribution, same power sensors the SwiftUI device list uses. */
private val loadSensors = listOf(
    "sensor.air_condition_current_extrapolated_power" to "\u0110i\u1ec1u h\u00f2a l\u1edbn",
    "sensor.dieu_hoa_spare_room_power" to "\u0110i\u1ec1u h\u00f2a tr\u1ebb",
    "sensor.dieu_hoa_power" to "\u0110i\u1ec1u h\u00f2a th\u1edd",
    "sensor.cong_tac_nong_lanh_power" to "N\u00f3ng l\u1ea1nh",
    "sensor.o_cam_bep_tu_power" to "B\u1ebfp t\u1eeb",
    "sensor.o_cam_tu_lanh_power" to "T\u1ee7 l\u1ea1nh",
    "sensor.o_cam_may_giat_power" to "M\u00e1y gi\u1eb7t",
    "sensor.o_cam_may_say_power" to "M\u00e1y s\u1ea5y",
    "sensor.o_cam_may_rua_bat_power" to "M\u00e1y r\u1eeda b\u00e1t",
    "sensor.o_cam_noi_chien_power" to "N\u1ed3i chi\u00ean",
    "sensor.o_cam_ban_lam_viec_power" to "B\u00e0n l\u00e0m vi\u1ec7c",
    "sensor.o_cam_ngoai_vi_power" to "\u1ed4 c\u1eafm ngo\u00e0i",
)

/** chargeIds in EnergyView.swift */
private val chargeControls = listOf<Triple<String, String, String>>(
    Triple("switch.allow_grid_to_charge_the_battery_2", "B\u1eadt/T\u1eaft s\u1ea1c AC", "switch"),
    Triple("number.solis_s6_eh1p_battery_max_charge_current_2", "S\u1ea1c DC", "number"),
    Triple("number.solis_s6_eh1p_grid_time_of_use_charge_battery_current_slot_1_2", "S\u1ea1c AC", "number"),
    Triple("number.solis_s6_eh1p_grid_time_of_use_charge_cut_off_soc_slot_1_2", "SOC k\u1ebft th\u00fac", "number"),
    Triple("switch.grid_time_of_use_charging_period_1_2", "Theo th\u1eddi gian", "switch"),
    Triple("number.solis_s6_eh1p_force_charge_soc_2", "S\u1ea1c b\u1eaft bu\u1ed9c", "number"),
    Triple(HumeConfig.BACKUP_SOC, "Pin d\u1ef1 tr\u1eef", "number"),
)

/** dischargeIds in EnergyView.swift */
private val dischargeControls = listOf(
    Triple("number.solis_s6_eh1p_battery_max_discharge_current_2", "X\u1ea3 DC", "number"),
    Triple("number.solis_s6_eh1p_off_grid_overdischarge_soc_2", "X\u1ea3 m\u1ea5t l\u01b0\u1edbi", "number"),
    Triple("number.solis_s6_eh1p_overdischarge_soc_2", "X\u1ea3 qu\u00e1 ng\u01b0\u1ee1ng", "number"),
)

@Composable
fun EnergyScreen(ha: HomeAssistantRepository) {
    val entities by ha.entities.collectAsState()
    var subTab by remember { mutableStateOf("consumption") }

    Column(
        Modifier
            .fillMaxSize()
            .background(HumeColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text("N\u0103ng l\u01b0\u1ee3ng", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = HumeColors.TextPrimary)
        Spacer(Modifier.height(12.dp))

        // Segmented control on a floating glass pill, One UI 8.5 style.
        Row(
            Modifier
                .fillMaxWidth()
                .glassPill(22.dp)
                .padding(4.dp),
        ) {
            listOf(
                "consumption" to "Ti\u00eau th\u1ee5",
                "analysis" to "Ph\u00e2n t\u00edch",
                "solar" to "\u0110i\u1ec7n m\u1eb7t tr\u1eddi",
            ).forEach { (key, label) ->
                val active = subTab == key
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (active) HumeColors.Orange else Color.Transparent)
                        .clickable { subTab = key }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (active) Color.White else HumeColors.TextSecondary,
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        when (subTab) {
            "consumption" -> ConsumptionTab(entities)
            "analysis" -> AnalysisTab(entities)
            else -> SolarTab(entities, ha)
        }
        Spacer(Modifier.height(48.dp))
    }
}

/* ------------------------- tab 1: consumption ------------------------- */

@Composable
private fun ConsumptionTab(entities: Map<String, HomeEntity>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard(
            Modifier.weight(1f),
            Icons.Rounded.ElectricalServices,
            "\u0110i\u1ec7n l\u01b0\u1edbi",
            vnd(entities.value("sensor.grid_cost")),
            "VND",
            Blue,
        )
        StatCard(
            Modifier.weight(1f),
            Icons.Rounded.House,
            "\u0110i\u1ec7n ti\u00eau th\u1ee5",
            vnd(entities.value("sensor.home_cost")),
            "VND",
            Brick,
        )
    }
    Spacer(Modifier.height(10.dp))

    // priceRow in EnergyView.swift
    val homeCost = entities.value("sensor.home_cost") ?: 0.0
    val evn = entities.value("sensor.evn_current_unit_price") ?: 2167.0
    val solar = entities.value(HumeConfig.PV_TODAY) ?: 0.0
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PriceBox(Modifier.weight(1f), "Gi\u00e1 mua", group(Math.round(homeCost / 147.49)), "\u0111/kWh", "HC/147.5kWh", Blue)
        PriceBox(Modifier.weight(1f), "Gi\u00e1 EVN", group(evn.toLong()), "\u0111/kWh", "B\u1eadc hi\u1ec7n t\u1ea1i", HumeColors.Red)
        PriceBox(
            Modifier.weight(1f),
            "Ti\u1ebft ki\u1ec7m",
            "+" + group(Math.round(evn * solar)),
            "\u0111",
            String.format(Locale.US, "%.1fkWh\u00d7EVN", solar),
            GreenSave,
        )
    }
    Spacer(Modifier.height(14.dp))

    Panel {
        Text("C\u00f4ng su\u1ea5t ho\u1ea1t \u0111\u1ed9ng", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = HumeColors.TextPrimary)
        Spacer(Modifier.height(10.dp))
        powerBars.forEach { (id, label, color) ->
            PowerBar(label, entities.value(id), color)
            Spacer(Modifier.height(10.dp))
        }
    }
}

/* ------------------------- tab 2: analysis ------------------------- */

@Composable
private fun AnalysisTab(entities: Map<String, HomeEntity>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard(
            Modifier.weight(1f),
            Icons.Rounded.ElectricalServices,
            "\u0110i\u1ec7n l\u01b0\u1edbi",
            fmt(entities.value("sensor.grid_import_billing_2")),
            "kWh",
            Blue,
        )
        StatCard(
            Modifier.weight(1f),
            Icons.Rounded.House,
            "\u0110i\u1ec7n ti\u00eau th\u1ee5",
            fmt(entities.value("sensor.home_import_billing")),
            "kWh",
            Brick,
        )
    }
    Spacer(Modifier.height(14.dp))

    val readings = loadSensors
        .mapNotNull { (id, name) -> entities.value(id)?.let { name to it } }
        .filter { it.second > 0 }
        .sortedByDescending { it.second }
    val total = readings.sumOf { it.second }

    Panel {
        Text("Ph\u00e2n b\u1ed5 t\u1ea3i", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = HumeColors.TextPrimary)
        Spacer(Modifier.height(10.dp))
        if (readings.isEmpty()) {
            Text("Kh\u00f4ng c\u00f3 thi\u1ebft b\u1ecb n\u00e0o \u0111ang ti\u00eau th\u1ee5", fontSize = 13.sp, color = HumeColors.TextSecondary)
        } else {
            readings.forEach { (name, watts) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(name, fontSize = 13.sp, color = HumeColors.TextPrimary, modifier = Modifier.width(120.dp), maxLines = 1)
                    Box(
                        Modifier
                            .weight(1f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(HumeColors.Divider),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth((watts / total).toFloat().coerceIn(0f, 1f))
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(HumeColors.Orange),
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        watts.toInt().toString() + "W",
                        fontSize = 12.sp,
                        color = HumeColors.TextSecondary,
                        modifier = Modifier.width(54.dp),
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "T\u1ed5ng " + total.toInt() + " W",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = HumeColors.TextPrimary,
            )
        }
    }
}

/* ------------------------- tab 3: solar ------------------------- */

@Composable
private fun SolarTab(entities: Map<String, HomeEntity>, ha: HomeAssistantRepository) {
    ControlGroup("S\u1ea1c Pin", chargeControls, entities, ha)
    Spacer(Modifier.height(12.dp))
    ControlGroup("X\u1ea3 Pin", dischargeControls, entities, ha)
    Spacer(Modifier.height(12.dp))

    Panel {
        Text("Th\u00f4ng s\u1ed1 inverter", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = HumeColors.TextPrimary)
        Spacer(Modifier.height(8.dp))
        listOf(
            HumeConfig.PV_POWER to "C\u00f4ng su\u1ea5t PV",
            HumeConfig.PV_TODAY to "S\u1ea3n l\u01b0\u1ee3ng h\u00f4m nay",
            HumeConfig.BATTERY_SOC to "Dung l\u01b0\u1ee3ng pin",
            HumeConfig.BATTERY_POWER to "C\u00f4ng su\u1ea5t pin",
            "sensor.aptomat_tong_power" to "C\u00f4ng su\u1ea5t l\u01b0\u1edbi",
            "sensor.cong_suat_nha" to "C\u00f4ng su\u1ea5t nh\u00e0",
        ).forEach { (id, label) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Text(label, fontSize = 13.sp, color = HumeColors.TextSecondary, modifier = Modifier.weight(1f))
                Text(
                    fmt(entities.value(id)) + " " + (entities[id]?.attrString("unit_of_measurement").orEmpty()),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = HumeColors.TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun ControlGroup(
    title: String,
    controls: List<Triple<String, String, String>>,
    entities: Map<String, HomeEntity>,
    ha: HomeAssistantRepository,
) {
    val available = controls.filter { (id, _, _) ->
        val state = entities[id]?.state
        state != null && state != "unavailable" && state != "unknown"
    }
    if (available.isEmpty()) return
    Panel {
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = HumeColors.TextPrimary)
        Spacer(Modifier.height(6.dp))
        available.forEach { (id, label, kind) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontSize = 14.sp, color = HumeColors.TextPrimary, modifier = Modifier.weight(1f))
                if (kind == "switch") {
                    val on = entities[id]?.isOn == true
                    Switch(checked = on, onCheckedChange = { if (on) ha.turnOff(id) else ha.turnOn(id) })
                } else {
                    val current = entities.value(id) ?: 0.0
                    val step = entities[id]?.attrString("step")?.toDoubleOrNull() ?: 1.0
                    val min = entities[id]?.attrString("min")?.toDoubleOrNull() ?: 0.0
                    val max = entities[id]?.attrString("max")?.toDoubleOrNull() ?: Double.MAX_VALUE
                    StepperRow(
                        value = fmt(current),
                        onMinus = { setNumber(ha, id, (current - step).coerceAtLeast(min)) },
                        onPlus = { setNumber(ha, id, (current + step).coerceAtMost(max)) },
                    )
                }
            }
        }
    }
}

/**
 * number.set_value, exactly the call EnergyView.swift makes. Values are sent as
 * plain numbers, and the touched entity is re-read by callService afterwards.
 */
private fun setNumber(ha: HomeAssistantRepository, entityId: String, value: Double) {
    ha.callService(
        "number",
        "set_value",
        "{\"entity_id\":\"" + entityId + "\",\"value\":" + value + "}",
        entityId,
    )
}

@Composable
private fun StepperRow(value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RoundIconButton(Icons.Rounded.Remove, onMinus)
        Text(
            value,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = HumeColors.TextPrimary,
            modifier = Modifier.width(64.dp),
        )
        RoundIconButton(Icons.Rounded.Add, onPlus)
    }
}

@Composable
private fun RoundIconButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(HumeColors.Background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = HumeColors.TextPrimary, modifier = Modifier.size(18.dp))
    }
}

/* ------------------------- shared atoms ------------------------- */

/** Every panel on this screen is the shared glass surface. */
@Composable
private fun Panel(content: @Composable ColumnScope.() -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        radius = HumeShapes.Panel,
        padding = PaddingValues(16.dp),
        content = content,
    )
}

@Composable
private fun StatCard(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    unit: String,
    color: Color,
) {
    GlassCard(modifier = modifier, radius = HumeShapes.Card, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(30.dp).clip(CircleShape).background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(label, fontSize = 12.sp, color = HumeColors.TextSecondary)
        }
        Spacer(Modifier.height(8.dp))
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = HumeColors.TextPrimary, maxLines = 1)
        Text(unit, fontSize = 11.sp, color = HumeColors.TextSecondary)
    }
}

@Composable
private fun PriceBox(
    modifier: Modifier,
    label: String,
    value: String,
    unit: String,
    sub: String,
    color: Color,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.30f), RoundedCornerShape(22.dp))
            .padding(10.dp),
    ) {
        Text(label, fontSize = 11.sp, color = HumeColors.TextSecondary)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
        Text(unit, fontSize = 10.sp, color = HumeColors.TextSecondary)
        Text(sub, fontSize = 9.sp, color = HumeColors.TextSecondary, maxLines = 1)
    }
}

@Composable
private fun PowerBar(label: String, watts: Double?, color: Color) {
    val value = watts ?: 0.0
    val fraction by animateFloatAsState(
        targetValue = (kotlin.math.abs(value) / 7000.0).toFloat().coerceIn(0f, 1f),
        label = "powerBar",
    )
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, fontSize = 12.sp, color = HumeColors.TextSecondary, modifier = Modifier.weight(1f))
            Text(value.toInt().toString() + " W", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = HumeColors.TextPrimary)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(HumeColors.Divider),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color),
            )
        }
    }
}

/* ------------------------- helpers ------------------------- */

private fun Map<String, HomeEntity>.value(id: String): Double? = this[id]?.numericState

private fun HomeEntity.attrString(key: String): String? =
    (attributes[key] as? kotlinx.serialization.json.JsonPrimitive)?.content

private fun fmt(value: Double?): String = when {
    value == null -> "--"
    kotlin.math.abs(value) >= 100 -> String.format(Locale.US, "%.0f", value)
    else -> String.format(Locale.US, "%.1f", value)
}

private fun vnd(value: Double?): String = if (value == null) "--" else group(value.toLong())

/** Int.formattedVND in the SwiftUI app: dot separated thousands. */
private fun group(value: Long): String {
    val symbols = DecimalFormatSymbols(Locale.US).apply { groupingSeparator = '.' }
    return DecimalFormat("#,###", symbols).format(value)
}
