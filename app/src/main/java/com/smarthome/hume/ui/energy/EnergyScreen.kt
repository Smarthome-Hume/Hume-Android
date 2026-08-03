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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ElectricalServices
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.House
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.rotate
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
import com.smarthome.hume.ui.theme.HumeIcons
import com.smarthome.hume.ui.theme.HumeShapes
import com.smarthome.hume.ui.theme.glassPill
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

private val Blue = Color(0xFF2196F3)
private val Brick = Color(0xFFEB5F34)
private val Yellow = Color(0xFFFFEB3B)
private val GreenSave = Color(0xFF3BA776)

/** Bottom room for the floating navigation pill (its height plus the gesture bar). */
private val NavBarRoom = 130.dp

/** Power bars of the consumption tab, in the order of EnergyView.swift. */
private data class PowerBarSpec(val entityId: String, val color: Color, val dynamic: Boolean = false)

private val powerBars = listOf(
    PowerBarSpec("sensor.battery_power_flow", HumeColors.Green, dynamic = true),
    PowerBarSpec(HumeConfig.PV_POWER, Yellow),
    PowerBarSpec("sensor.aptomat_tong_power", Blue),
    PowerBarSpec("sensor.cong_suat_nha", Brick),
)

/** chargeIds in EnergyView.swift, in the same order and with the same labels. */
private val chargeControls = listOf(
    Triple("switch.allow_grid_to_charge_the_battery_2", "B\u1eadt/ T\u1eaft s\u1ea1c AC", "switch"),
    Triple("number.solis_s6_eh1p_battery_max_charge_current_2", "S\u1ea1c DC", "number"),
    Triple("number.solis_s6_eh1p_grid_time_of_use_charge_battery_current_slot_1_2", "S\u1ea1c AC", "number"),
    Triple("number.solis_s6_eh1p_grid_time_of_use_charge_cut_off_soc_slot_1_2", "SOC k\u1ebft th\u00fac", "number"),
    Triple("switch.grid_time_of_use_charging_period_1_2", "Theo th\u1eddi gian", "switch"),
    Triple("number.solis_s6_eh1p_force_charge_soc_2", "S\u1ea1c b\u1eaft bu\u1ed9c", "number"),
    Triple(HumeConfig.BACKUP_SOC, "Pin d\u1ef1 tr\u1eef", "number"),
)

/** dischargeIds in EnergyView.swift. */
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
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
    ) {
        Text(
            "N\u0103ng l\u01b0\u1ee3ng",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = HumeColors.TextPrimary,
            maxLines = 1,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 12.dp),
        )

        // Segmented picker, the glassSegmentBG pill on iOS.
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
                        maxLines = 1,
                        softWrap = false,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (active) Color.White else HumeColors.TextSecondary,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        when (subTab) {
            "consumption" -> ConsumptionTab(entities, ha)
            "analysis" -> AnalysisTab(entities, ha)
            else -> SolarTab(entities, ha)
        }
        Spacer(Modifier.height(NavBarRoom))
    }
}

/* ------------------------- tab 1: consumption ------------------------- */

@Composable
private fun ConsumptionTab(entities: Map<String, HomeEntity>, ha: HomeAssistantRepository) {
    // Group 1: the 7 day chart of energy_home_daily.
    Panel(radius = HumeShapes.Panel) {
        EnergyWeekChart(ha, "N\u0103ng l\u01b0\u1ee3ng s\u1eed d\u1ee5ng", "sensor.energy_home_daily", "kWh")
    }
    Spacer(Modifier.height(12.dp))

    // Group 2: two neon stats, the price row and the active power bars.
    Panel(radius = HumeShapes.Panel) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeonStat(
                Modifier.weight(1f),
                Icons.Outlined.ElectricalServices,
                "\u0110i\u1ec7n l\u01b0\u1edbi",
                vnd(entities.value("sensor.grid_cost")),
                "VND",
                Blue,
            )
            NeonStat(
                Modifier.weight(1f),
                Icons.Outlined.House,
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
            PriceBox(Modifier.weight(1f), "Gi\u00e1 mua", group((homeCost / 147.49).roundToLong()), "\u0111/kWh", "HC/147.5kWh", Blue)
            PriceBox(Modifier.weight(1f), "Gi\u00e1 EVN", group(evn.toLong()), "\u0111/kWh", "B\u1eadc hi\u1ec7n t\u1ea1i", HumeColors.Red)
            PriceBox(
                Modifier.weight(1f),
                "Ti\u1ebft ki\u1ec7m",
                "+" + group((evn * solar).roundToLong()),
                "\u0111",
                String.format(Locale.US, "%.1fkWh\u00d7EVN", solar),
                GreenSave,
            )
        }
        Spacer(Modifier.height(10.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(25.dp))
                .background(HumeColors.Card)
                .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
        ) {
            Text("C\u00f4ng su\u1ea5t ho\u1ea1t \u0111\u1ed9ng", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = HumeColors.TextPrimary)
            Spacer(Modifier.height(8.dp))
            powerBars.forEach { spec ->
                PowerBar(entities.value(spec.entityId), spec)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
    Spacer(Modifier.height(12.dp))

    // Group 3: weak batteries + yesterday device breakdown.
    Panel(radius = HumeShapes.Panel) {
        BatteryLowList(entities)
        Spacer(Modifier.height(10.dp))
        YesterdayDeviceCard(ha)
    }
}

/* ------------------------- tab 2: analysis ------------------------- */

@Composable
private fun AnalysisTab(entities: Map<String, HomeEntity>, ha: HomeAssistantRepository) {
    Panel(radius = HumeShapes.Panel) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeonStat(
                Modifier.weight(1f),
                Icons.Outlined.ElectricalServices,
                "\u0110i\u1ec7n l\u01b0\u1edbi",
                fmt(entities.value("sensor.grid_import_billing_2")),
                "kWh",
                Blue,
            )
            NeonStat(
                Modifier.weight(1f),
                Icons.Outlined.House,
                "\u0110i\u1ec7n ti\u00eau th\u1ee5",
                fmt(entities.value("sensor.home_import_billing")),
                "kWh",
                Brick,
            )
        }
    }
    Spacer(Modifier.height(12.dp))

    Panel(radius = 42.dp) {
        EnergyFlowAndMix(entities)
    }
    Spacer(Modifier.height(12.dp))

    Panel(radius = HumeShapes.Panel) {
        DeviceFilterList(entities, ha)
    }
}

/* ------------------------- tab 3: solar ------------------------- */

@Composable
private fun SolarTab(entities: Map<String, HomeEntity>, ha: HomeAssistantRepository) {
    ExpanderGroup("S\u1ea1c Pin", chargeControls, entities, ha)
    Spacer(Modifier.height(12.dp))
    ExpanderGroup("X\u1ea3 Pin", dischargeControls, entities, ha)
    Spacer(Modifier.height(12.dp))

    // humeCard(cornerRadius: 44) around the animated flow diagram.
    Panel(radius = 44.dp) {
        SunsynkAnimatedFlowCard(ha)
    }
    Spacer(Modifier.height(12.dp))

    Panel(radius = 24.dp, padding = PaddingValues(20.dp)) {
        SunsynkStaticCard(entities)
    }
}

/** ExpanderCardView + ToggleRowView + NumberRowView from EnergyView.swift. */
@Composable
private fun ExpanderGroup(
    title: String,
    controls: List<Triple<String, String, String>>,
    entities: Map<String, HomeEntity>,
    ha: HomeAssistantRepository,
) {
    // isAvailable(id) on iOS: hide the whole group when nothing reports a state.
    val available = controls.filter { (id, _, _) ->
        val state = entities[id]?.state
        state != null && state != "unavailable" && state != "unknown"
    }
    if (available.isEmpty()) return
    var open by remember(title) { mutableStateOf(false) }

    Panel(radius = 25.dp, padding = PaddingValues(0.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { open = !open }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = HumeColors.TextPrimary, modifier = Modifier.weight(1f))
            Icon(
                Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = HumeColors.TextSecondary,
                modifier = Modifier.rotate(if (open) 180f else 0f),
            )
        }
        if (open) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(HumeColors.Divider))
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                available.forEach { (id, label, kind) ->
                    if (kind == "switch") {
                        ToggleRow(label, entities[id]?.isOn == true) { ha.toggle(id) }
                    } else {
                        NumberRow(label, entities[id], ha, id)
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(name: String, checked: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, fontSize = 14.sp, color = HumeColors.TextPrimary, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(checkedTrackColor = HumeColors.Orange),
        )
    }
}

/**
 * NumberRowView: tap the value, type a new one, confirm with the check mark.
 * The service call is number.set_value, exactly like iOS.
 */
@Composable
private fun NumberRow(
    name: String,
    entity: HomeEntity?,
    ha: HomeAssistantRepository,
    entityId: String,
) {
    var editing by remember(entityId) { mutableStateOf(false) }
    var input by remember(entityId) { mutableStateOf("") }
    val state = entity?.state.orEmpty()
    val unit = entity?.attr("unit_of_measurement").orEmpty()

    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(name, fontSize = 14.sp, color = HumeColors.TextPrimary, modifier = Modifier.weight(1f))
        if (editing) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                singleLine = true,
                modifier = Modifier.width(110.dp),
            )
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(GreenSave.copy(alpha = 0.16f))
                    .clickable {
                        editing = false
                        input.replace(',', '.').toDoubleOrNull()?.let { setNumber(ha, entityId, it) }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Check, contentDescription = null, tint = GreenSave, modifier = Modifier.size(18.dp))
            }
        } else {
            Row(
                Modifier.clickable { input = state; editing = true },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(state, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = HumeColors.TextPrimary)
                if (unit.isNotEmpty()) {
                    Spacer(Modifier.width(4.dp))
                    Text(unit, fontSize = 11.sp, color = HumeColors.TextSecondary)
                }
            }
        }
    }
}

/** number.set_value, the call EnergyView.swift makes. */
private fun setNumber(ha: HomeAssistantRepository, entityId: String, value: Double) {
    ha.callService(
        "number",
        "set_value",
        "{\"entity_id\":\"" + entityId + "\",\"value\":" + value + "}",
        entityId,
    )
}

/* ------------------------- shared atoms ------------------------- */

@Composable
private fun Panel(
    radius: androidx.compose.ui.unit.Dp = HumeShapes.Panel,
    padding: PaddingValues = PaddingValues(10.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        radius = radius,
        padding = padding,
        content = content,
    )
}

/**
 * NeonGlassStat: icon circle, label, tinted value, coloured ring.
 *
 * The value row must never wrap: on a narrow half-width box the unit used to
 * break into one letter per line ("V / N / D").
 */
@Composable
private fun NeonStat(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    unit: String,
    color: Color,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(25.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.40f), RoundedCornerShape(25.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(HumeColors.Card),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 12.sp, color = HumeColors.TextSecondary, maxLines = 1, softWrap = false)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                    maxLines = 1,
                    softWrap = false,
                )
                Spacer(Modifier.width(2.dp))
                Text(unit, fontSize = 11.sp, color = HumeColors.TextSecondary, maxLines = 1, softWrap = false)
            }
        }
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
            .clip(RoundedCornerShape(HumeShapes.Element))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.40f), RoundedCornerShape(HumeShapes.Element))
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, fontSize = 10.sp, color = HumeColors.TextPrimary, maxLines = 1, softWrap = false)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = color, maxLines = 1, softWrap = false)
            Text(unit, fontSize = 10.sp, color = HumeColors.TextSecondary, maxLines = 1, softWrap = false)
        }
        Text(sub, fontSize = 9.sp, color = HumeColors.TextSecondary, maxLines = 1, softWrap = false)
    }
}

/**
 * EnergyBarView: icon circle plus a 42 dp capsule whose fill is the power over
 * 7000 W. The battery bar is dynamic, so a negative flow turns it orange.
 */
@Composable
private fun PowerBar(watts: Double?, spec: PowerBarSpec) {
    val value = watts ?: 0.0
    val fraction by animateFloatAsState(
        targetValue = (abs(value) / 7000.0).toFloat().coerceIn(0f, 1f),
        label = "powerBar",
    )
    val color = if (spec.dynamic && value < 0) HumeColors.Orange else spec.color

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(42.dp).clip(CircleShape).background(color.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                HumeIcons.forEntity(spec.entityId),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .weight(1f)
                .height(42.dp)
                .clip(RoundedCornerShape(21.dp))
                .background(HumeColors.Divider),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(42.dp)
                    .clip(RoundedCornerShape(21.dp))
                    .background(color),
            )
            Text(
                value.toInt().toString() + " W",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(horizontal = 10.dp, vertical = 2.dp),
            )
        }
    }
}

/* ------------------------- helpers ------------------------- */

private fun Map<String, HomeEntity>.value(id: String): Double? = this[id]?.numericState

private fun fmt(value: Double?): String = when {
    value == null -> "--"
    abs(value) >= 100 -> String.format(Locale.US, "%.0f", value)
    else -> String.format(Locale.US, "%.1f", value)
}

private fun vnd(value: Double?): String = if (value == null) "--" else group(value.toLong())

/** Int.formattedVND in the SwiftUI app: dot separated thousands. */
private fun group(value: Long): String {
    val symbols = DecimalFormatSymbols(Locale.US).apply { groupingSeparator = '.' }
    return DecimalFormat("#,###", symbols).format(value)
}
