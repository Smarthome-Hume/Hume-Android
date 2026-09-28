package com.smarthome.hume.ui.energy

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.ArrowDropUp
import androidx.compose.material.icons.rounded.ElectricalServices
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.ui.theme.HumeColors
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/* =====================================================================
 *  BATTERY SENSOR LIST  (BatterySensorList in EnergyView.swift)
 *  Every *_battery sensor below 70 %, two per row, red under 20 %.
 * ===================================================================== */

@Composable
fun BatteryLowList(entities: Map<String, HomeEntity>) {
    val low = entities.values
        .filter { entity ->
            val id = entity.id
            id.endsWith("_battery") &&
                !id.contains("solis") && !id.contains("xiaomi") && !id.contains("daisy") &&
                (entity.numericState ?: 100.0) < 70.0
        }
        .map { entity ->
            val name = entity.friendly().replace("Battery", "").trim()
            name to (entity.numericState ?: 0.0)
        }
        .sortedBy { it.second }
    if (low.isEmpty()) return

    Column(Modifier.fillMaxWidth()) {
        Text(
            "Pin y\u1ebfu (" + low.size + ")",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = HumeColors.TextPrimary,
        )
        Spacer(Modifier.height(8.dp))
        low.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { (name, level) ->
                    Row(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(HumeColors.Background)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            name.split(" ").take(2).joinToString(" "),
                            fontSize = 12.sp,
                            color = HumeColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            level.toInt().toString() + "%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (level < 20) Color(0xFFF44336) else HumeColors.Orange,
                        )
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/* =====================================================================
 *  YESTERDAY DEVICE CARD  (Views/Energy/YesterdayDeviceCard.swift)
 *  Grid cost today vs yesterday, yesterday grid kWh, battery discharge
 *  and the top daily-energy devices, all priced with home_cost / 147.49.
 * ===================================================================== */

private data class YesterdayItem(val name: String, val value: Double, val unit: String, val cost: Long)

private data class YesterdayData(
    val gridCostToday: Double = 0.0,
    val gridCostYesterday: Double = 0.0,
    val grid: Double = 0.0,
    val batteryDischarge: Double = 0.0,
    val items: List<YesterdayItem> = emptyList(),
    val loading: Boolean = true,
)

private val yesterdayNoise = listOf(
    "solis", "battery", "soc", "soh", "dod", "alarm", "zigbee", "hourly", "monthly", "aptomat",
)
private val explicitEnergyIds = setOf(
    "sensor.air_condition_daily_energy_ac",
    "sensor.dieu_hoa_power_energy",
)

@Composable
fun YesterdayDeviceCard(ha: HomeAssistantRepository) {
    val entities by ha.entities.collectAsState()
    var data by remember { mutableStateOf(YesterdayData()) }

    val rate = ((entities["sensor.home_cost"]?.numericState ?: 0.0) / 147.49).roundToLong()
    val liveGridCost = entities["sensor.grid_cost"]?.numericState ?: 0.0

    LaunchedEffect(Unit) {
        val now = System.currentTimeMillis()
        val todayStart = com.smarthome.hume.core.storage.DailySnapshotStore.startOfDay(now)
        val dayMs = 24L * 60L * 60L * 1000L

        suspend fun endOfDay(entityId: String, daysAgo: Int): Double {
            val start = todayStart - daysAgo * dayMs
            val end = start + dayMs
            val points = runCatching { ha.fetchHistory(entityId, hours = 24 * (daysAgo + 1)) }
                .getOrDefault(emptyList())
            return points.filter { it.timeMs in start until end }.lastOrNull()?.value ?: 0.0
        }

        val gridCostEndYesterday = endOfDay("sensor.grid_cost", 1)
        val gridCostEndPrev = endOfDay("sensor.grid_cost", 2)
        val yesterdayGrid = endOfDay("sensor.aptomat_tong_daily", 1)
        val batteryId = "sensor.solis_s6_eh1p_yesterday_battery_discharge_energy_2"
        val battery = ha.entities.value[batteryId]?.numericState ?: endOfDay(batteryId, 1)

        val candidates = ha.entities.value.values.filter { entity ->
            val id = entity.id
            if (!id.startsWith("sensor.")) return@filter false
            if (id in explicitEnergyIds) return@filter true
            if (yesterdayNoise.any { id.contains(it) }) return@filter false
            id.contains("_daily_energy") || id.contains("_energy_daily")
        }.take(12)

        val rows = candidates.mapNotNull { entity ->
            val points = runCatching { ha.fetchHistory(entity.id, hours = 48) }.getOrDefault(emptyList())
            val value = points.filter { it.timeMs in (todayStart - dayMs) until todayStart }.lastOrNull()?.value
            if (value == null || value <= 0) return@mapNotNull null
            YesterdayItem(
                name = entity.friendly(),
                value = value,
                unit = entity.attr("unit_of_measurement") ?: "kWh",
                cost = (value * rate).roundToLong(),
            )
        }.sortedByDescending { it.value }.take(6)

        data = YesterdayData(
            gridCostToday = (liveGridCost - gridCostEndYesterday).coerceAtLeast(0.0),
            gridCostYesterday = (gridCostEndYesterday - gridCostEndPrev).coerceAtLeast(0.0),
            grid = yesterdayGrid,
            batteryDischarge = battery,
            items = rows,
            loading = false,
        )
    }

    Column(Modifier.fillMaxWidth()) {
        Text("Thi\u1ebft b\u1ecb s\u1eed d\u1ee5ng", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = HumeColors.TextPrimary)
        Text("D\u1eef li\u1ec7u h\u00f4m qua", fontSize = 11.sp, color = HumeColors.TextSecondary)
        Spacer(Modifier.height(10.dp))

        // Grid cost card with the up/down badge from the SwiftUI header.
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(25.dp))
                .background(HumeColors.Background)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(HumeColors.OrangeSofter),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.ElectricalServices, contentDescription = null, tint = HumeColors.Orange, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Ti\u1ec1n \u0111i\u1ec7n l\u01b0\u1edbi", fontSize = 12.sp, color = HumeColors.TextSecondary)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        vndGroup(data.gridCostToday.roundToLong()),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HumeColors.TextPrimary,
                    )
                    Spacer(Modifier.width(3.dp))
                    Text("VND", fontSize = 12.sp, color = HumeColors.TextSecondary)
                }
            }
            if (data.gridCostYesterday > 0) {
                val pct = if (data.gridCostToday > 0) {
                    (data.gridCostToday - data.gridCostYesterday) / data.gridCostYesterday * 100
                } else 0.0
                val up = pct > 0
                val tint = if (up) Color(0xFFEF5350) else Color(0xFF66BB6A)
                Row(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(tint.copy(alpha = 0.15f))
                        .border(1.dp, tint.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (up) Icons.Rounded.ArrowDropUp else Icons.Rounded.ArrowDropDown,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        String.format(Locale.US, "%.1f%%", abs(pct)),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = tint,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        YesterdayRow("\u0110i\u1ec7n l\u01b0\u1edbi", data.grid, "kWh", (data.grid * rate).roundToLong())
        Spacer(Modifier.height(6.dp))
        YesterdayRow("Pin S6", data.batteryDischarge, "kWh", (data.batteryDischarge * rate).roundToLong())
        Spacer(Modifier.height(8.dp))

        when {
            data.loading -> Text(
                "\u0110ang t\u1ea3i d\u1eef li\u1ec7u h\u00f4m qua\u2026",
                fontSize = 13.sp,
                color = HumeColors.TextSecondary,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            data.items.isEmpty() -> Text(
                "Kh\u00f4ng c\u00f3 d\u1eef li\u1ec7u ng\u00e0y h\u00f4m qua",
                fontSize = 13.sp,
                color = HumeColors.TextSecondary,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            else -> data.items.forEach { item ->
                YesterdayRow(item.name, item.value, item.unit, item.cost)
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun YesterdayRow(name: String, value: Double, unit: String, cost: Long) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(HumeColors.Background)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = HumeColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(String.format(Locale.US, "%.1f", value), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = HumeColors.TextPrimary)
            Text(unit, fontSize = 10.sp, color = HumeColors.TextSecondary)
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.widthIn(min = 65.dp)) {
            Text(vndGroup(cost), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = HumeColors.TextSecondary)
            Text("VND", fontSize = 10.sp, color = HumeColors.TextSecondary)
        }
    }
}

/* =====================================================================
 *  ENERGY FLOW & MIX  (EnergyFlowAndMix in EnergyView.swift)
 *  Morning / afternoon / evening split of energy_home_daily plus the
 *  three breaker meters, both drawn as a segment bar + stat row.
 * ===================================================================== */

@Composable
fun EnergyFlowAndMix(entities: Map<String, HomeEntity>) {
    fun value(id: String): Double = entities[id]?.numericState ?: 0.0

    val total = value("sensor.energy_home_daily")
    val noon = value("input_number.energy_at_noon")
    val evening = value("input_number.energy_at_evening")
    val hour = com.smarthome.hume.core.storage.DailySnapshotStore.calendar()
        .get(java.util.Calendar.HOUR_OF_DAY)

    val flow: List<Triple<String, Double, Color>> = when {
        hour < 12 -> listOf(
            Triple("S\u00e1ng", total, Color(0xFFFFB74D)),
            Triple("Chi\u1ec1u", 0.0, Color(0xFFFF9800)),
            Triple("T\u1ed1i", 0.0, Color(0xFFE65100)),
        )
        hour < 18 -> {
            val morning = minOf(if (noon > 0) noon else total * 0.5, total)
            listOf(
                Triple("S\u00e1ng", morning, Color(0xFFFFB74D)),
                Triple("Chi\u1ec1u", (total - morning).coerceAtLeast(0.0), Color(0xFFFF9800)),
                Triple("T\u1ed1i", 0.0, Color(0xFFE65100)),
            )
        }
        else -> {
            val morning = minOf(if (noon > 0) noon else total * 0.35, total)
            val afternoon = minOf(
                if (evening > noon) evening - noon else (total - morning).coerceAtLeast(0.0) * 0.55,
                total - morning,
            )
            listOf(
                Triple("S\u00e1ng", morning, Color(0xFFFFB74D)),
                Triple("Chi\u1ec1u", afternoon, Color(0xFFFF9800)),
                Triple("T\u1ed1i", (total - morning - afternoon).coerceAtLeast(0.0), Color(0xFFE65100)),
            )
        }
    }

    val breakers = listOf(
        Triple("CB1", value("sensor.aptomat_t1_energy_daily"), Color(0xFFFF5722)),
        Triple("CB2", value("sensor.aptomat_t2_energy_daily"), Color(0xFFFF9800)),
        Triple("CB3", value("sensor.aptomat_t3_energy_daily"), Color(0xFF9E9E9E)),
    )
    val breakerTotal = maxOf(breakers.sumOf { it.second }, 0.01)

    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("D\u00f2ng \u0111i\u1ec7n & T\u1ec9 l\u1ec7", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = HumeColors.TextPrimary)
            Spacer(Modifier.width(6.dp))
            Text("h\u00f4m nay", fontSize = 12.sp, color = HumeColors.TextSecondary)
        }
        Spacer(Modifier.height(12.dp))
        SegmentBar(flow, flow.sumOf { it.second })
        Spacer(Modifier.height(8.dp))
        StatRow(flow)
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(HumeColors.Divider))
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ph\u00e2n b\u1ed5 t\u1ea3i", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = HumeColors.TextPrimary, modifier = Modifier.weight(1f))
            Text(String.format(Locale.US, "%.1f kWh", breakerTotal), fontSize = 12.sp, color = HumeColors.TextSecondary)
        }
        Spacer(Modifier.height(10.dp))
        SegmentBar(breakers, breakerTotal)
        Spacer(Modifier.height(8.dp))
        StatRow(breakers)
    }
}

@Composable
private fun SegmentBar(items: List<Triple<String, Double, Color>>, total: Double) {
    Row(
        Modifier.fillMaxWidth().height(30.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEach { (_, value, color) ->
            val fraction = if (total > 0) (value / total).toFloat() else 0f
            if (fraction > 0.001f) {
                Box(
                    Modifier
                        .weight(fraction)
                        .height(30.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(color.copy(alpha = 0.18f))
                        .border(1.dp, color.copy(alpha = 0.53f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (fraction > 0.08f) {
                        Text(
                            (fraction * 100).toInt().toString() + "%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = HumeColors.TextPrimary,
                        )
                    }
                }
            }
        }
    }
}

/*
 * The Sang / Chieu / Toi va CB1 / CB2 / CB3.
 *
 * TRUOC: khong ghim chieu cao, chi co padding(vertical = 8) + ba dong chu
 * 9/15/9sp -> mot o cao khoang 60dp, nhin rat tho.
 * NAY: cao CUNG 44dp, chu 9/14/8sp co lineHeight ghim, ba dong duoc can giua
 * theo chieu doc nen khong con khoang trong thua o tren va duoi.
 */
private val StatCellHeight = 44.dp

@Composable
private fun StatRow(items: List<Triple<String, Double, Color>>) {
    val total = maxOf(items.sumOf { it.second }, 0.01)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { (label, value, color) ->
            Column(
                Modifier
                    .weight(1f)
                    .height(StatCellHeight)
                    .clip(RoundedCornerShape(14.dp))
                    .background(HumeColors.Background)
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    label,
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = color,
                    maxLines = 1,
                )
                Text(
                    String.format(Locale.US, "%.2f", value),
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HumeColors.TextPrimary,
                    maxLines = 1,
                )
                Text(
                    (value / total * 100).toInt().toString() + "%",
                    fontSize = 8.sp,
                    lineHeight = 9.sp,
                    color = HumeColors.TextSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

/* =====================================================================
 *  DEVICE FILTER LIST  (DeviceFilterList in EnergyView.swift)
 *
 *  DANH SACH DO HOME ASSISTANT QUYET DINH.
 *
 *  TRUOC: the tu quet moi entity sensor.*_power roi loai bot bang mot
 *  danh sach tu khoa nhieu (powerNoise). Hai entity khac id nhung do cung
 *  mot o cam vat ly deu lot qua, nen danh sach ra hai dong trung nhau
 *  (vi du "Ban hoc" va "O cam ban lam viec Cong suat"), va o che do Nang
 *  luong thi tien dien con bi dem hai lan.
 *
 *  NAY: chi entity mang label DEVICE_LIST_LABEL trong Home Assistant moi
 *  duoc len the. Muon them hay bot mot thiet bi thi gan/bo label ben HA,
 *  khong phai sua code nua. Entity bi an hoac bi tat ben HA luon bi loai,
 *  ke ca khi con dinh label.
 *
 *  MOT LABEL DUNG CHO CA HAI LOAI SENSOR. Nguoi dung gan label New cho ca
 *  sensor cong suat (W, kW) lan sensor dien nang (Wh, kWh), nen the phai tu
 *  tach hai loai theo device_class / unit_of_measurement:
 *    - Che do Cong suat: chi lay sensor cong suat, quy het ve W, va chi giu
 *      POWER_TOP_COUNT thiet bi dang an nhieu dien nhat.
 *    - Che do Nang luong: chi lay sensor dien nang, quy het ve kWh; sensor
 *      cong suat duoc ghep sang sensor dien nang tuong ung neu tim thay.
 *      Che do nay giu nguyen ca danh sach vi con cong tien dien.
 *  Neu khong tach nhu vay thi mot danh sach se lan ca kW lan kWh, so lieu
 *  khong the so sanh voi nhau va tien dien tinh sai.
 *
 *  Khi chua co entity nao mang label (HA cu hon 2024.4, hoac registry chua
 *  ve kip), the quay ve bo loc ten cu nhung co them hai lop chan trung:
 *  bo entity an/tat/diagnostic, va moi device_id chi giu mot entity.
 * ===================================================================== */

/** Ten label ben Home Assistant danh dau entity duoc phep len the Thiet bi. */
private const val DEVICE_LIST_LABEL = "New"

/** So dong toi da o che do Cong suat, xep theo W giam dan. */
private const val POWER_TOP_COUNT = 5

private val powerUnits = setOf("W", "kW", "mW", "MW")
private val energyUnits = setOf("Wh", "kWh", "MWh")

private fun HomeEntity.unitRaw(): String? = attr("unit_of_measurement")?.trim()

private fun HomeEntity.isPowerSensor(): Boolean =
    attr("device_class") == "power" || unitRaw() in powerUnits

private fun HomeEntity.isEnergySensor(): Boolean =
    attr("device_class") == "energy" || unitRaw() in energyUnits

/** Quy moi sensor cong suat ve W de ca danh sach dung chung mot don vi. */
private fun HomeEntity.watts(): Double? {
    val raw = numericState ?: return null
    return when (unitRaw()) {
        "kW" -> raw * 1000.0
        "MW" -> raw * 1000000.0
        "mW" -> raw / 1000.0
        else -> raw
    }
}

/** Quy moi sensor dien nang ve kWh de con nhan duoc voi don gia dien. */
private fun HomeEntity.kwh(): Double? {
    val raw = numericState ?: return null
    return when (unitRaw()) {
        "Wh" -> raw / 1000.0
        "MWh" -> raw * 1000.0
        else -> raw
    }
}

private val powerNoise = listOf(
    "solis", "battery", "soc", "soh", "dod", "alarm", "zigbee", "hourly", "monthly",
    "daily_cooling_energy", "daily_heating_energy", "aptomat", "cooling", "heating",
    "home_power", "grid_power", "cong_suat",
)

private val explicitEnergyMap = mapOf(
    "sensor.air_condition_current_extrapolated_power" to "sensor.air_condition_daily_energy_ac",
    "sensor.dieu_hoa_power" to "sensor.dieu_hoa_power_energy",
)

@Composable
fun DeviceFilterList(entities: Map<String, HomeEntity>, ha: HomeAssistantRepository) {
    var mode by remember { mutableStateOf("power") }
    var menu by remember { mutableStateOf(false) }

    val registry by ha.registry.collectAsState()
    val labelNames by ha.labels.collectAsState()
    val deviceLabels by ha.deviceLabels.collectAsState()

    // Danh sach id do label quyet dinh. Doi theo registry nen chi tinh lai khi
    // Home Assistant gui registry moi, khong phai moi lan mot sensor nhay so.
    val labelled = remember(registry, labelNames, deviceLabels) {
        ha.entityIdsWithLabel(DEVICE_LIST_LABEL)
    }
    val usingLabels = labelled.isNotEmpty()

    val rate = ((entities["sensor.home_cost"]?.numericState ?: 0.0) / 147.49).roundToLong()

    val candidates: List<HomeEntity> = remember(entities, labelled, registry) {
        if (usingLabels) {
            labelled.mapNotNull { entities[it] }
        } else {
            entities.values
                .filter { entity ->
                    entity.id.startsWith("sensor.") && entity.id.endsWith("_power") &&
                        powerNoise.none { entity.id.contains(it) }
                }
                .filter { entity ->
                    val entry = registry[entity.id]
                    entry?.hiddenBy == null && entry?.disabledBy == null &&
                        entry?.entityCategory == null
                }
                // Hai entity cua cung mot thiet bi chi duoc lay mot.
                .distinctBy { entity -> registry[entity.id]?.deviceId ?: entity.id }
        }
    }

    // Energy mode pairs each power sensor with its *_daily_energy_* counterpart.
    val prefixLookup = entities.keys.mapNotNull { id ->
        val index = id.indexOf("_daily_energy_")
        if (index > 0) id.substring(0, index) to id else null
    }.toMap()

    data class Item(val id: String, val name: String, val value: Double, val unit: String, val ago: String, val cost: Long?)

    val items = if (mode == "power") {
        candidates
            // Sensor dien nang khong co cho o day, neu khong the se lan kWh vao kW.
            .filter { it.isPowerSensor() }
            .mapNotNull { entity ->
                val value = entity.watts() ?: return@mapNotNull null
                // Voi danh sach theo label thi thiet bi dang tat van phai hien 0 W,
                // vi chinh nguoi dung da chon no. Bo loc ten cu thi van bo qua so 0.
                if (!usingLabels && value <= 0) return@mapNotNull null
                val minutes = entity.minutesAgo()
                val ago = when {
                    minutes == null -> "Gi\u00e1m s\u00e1t"
                    minutes < 1 -> "V\u1eeba xong"
                    minutes < 60 -> minutes.toString() + " ph\u00fat tr\u01b0\u1edbc"
                    else -> (minutes / 60).toString() + " gi\u1edd tr\u01b0\u1edbc"
                }
                Item(entity.id, entity.friendly(), value, "W", ago, null)
            }
            .distinctBy { it.id }
            .sortedByDescending { it.value }
            // Chi giu top 5 thiet bi dang keo nhieu dien nhat.
            .take(POWER_TOP_COUNT)
    } else {
        candidates.mapNotNull { entity ->
            // Entity da la sensor dien nang thi dung thang, khong phai ghep gi.
            val target = if (entity.isEnergySensor()) {
                entity
            } else {
                val energyId = explicitEnergyMap[entity.id]
                    ?: prefixLookup[entity.id.removeSuffix("_power")]
                    ?: return@mapNotNull null
                entities[energyId] ?: return@mapNotNull null
            }
            val value = target.kwh() ?: return@mapNotNull null
            if (value <= 0) return@mapNotNull null
            Item(
                target.id,
                entity.friendly(),
                value,
                "kWh",
                "H\u00f4m nay",
                (value * rate).roundToLong(),
            )
        }
            // Hai sensor cong suat tro chung mot sensor nang luong chi ra mot dong,
            // neu khong tien dien se bi cong doi.
            .distinctBy { it.id }
            .sortedByDescending { it.value }
    }

    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Thi\u1ebft b\u1ecb",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = HumeColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            Box {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(HumeColors.Background)
                        .clickable { menu = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (mode == "power") "C\u00f4ng su\u1ea5t" else "N\u0103ng l\u01b0\u1ee3ng",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = HumeColors.TextPrimary,
                    )
                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = HumeColors.TextSecondary, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("C\u00f4ng su\u1ea5t") }, onClick = { mode = "power"; menu = false })
                    DropdownMenuItem(text = { Text("N\u0103ng l\u01b0\u1ee3ng") }, onClick = { mode = "energy"; menu = false })
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        if (items.isEmpty()) {
            Text(
                "Kh\u00f4ng c\u00f3 d\u1eef li\u1ec7u",
                fontSize = 13.sp,
                color = HumeColors.TextSecondary,
                modifier = Modifier.padding(vertical = 20.dp),
            )
        } else {
            items.forEach { item ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(HumeColors.Background)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(item.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = HumeColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(item.ago, fontSize = 11.sp, color = HumeColors.TextSecondary)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(String.format(Locale.US, "%.1f", item.value), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = HumeColors.Orange)
                        Text(item.unit, fontSize = 11.sp, color = HumeColors.TextSecondary)
                    }
                    if (item.cost != null) {
                        Spacer(Modifier.width(10.dp))
                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.widthIn(min = 64.dp)) {
                            Text(vndGroup(item.cost), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF3BA776))
                            Text("VND", fontSize = 11.sp, color = HumeColors.TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

/* ------------------------------ helpers ------------------------------ */

internal fun HomeEntity.friendly(): String =
    attr("friendly_name") ?: id.substringAfter('.').replace('_', ' ')

internal fun HomeEntity.attr(key: String): String? =
    (attributes[key] as? kotlinx.serialization.json.JsonPrimitive)?.content

internal fun vndGroup(value: Long): String {
    val symbols = java.text.DecimalFormatSymbols(Locale.US).apply { groupingSeparator = '.' }
    return java.text.DecimalFormat("#,###", symbols).format(value)
}
