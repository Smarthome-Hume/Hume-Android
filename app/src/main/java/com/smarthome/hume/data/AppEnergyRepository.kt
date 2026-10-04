package com.smarthome.hume.data

import com.smarthome.hume.core.data.EnergyRepository
import com.smarthome.hume.core.ha.HistoryFetcher
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.BatteryControl
import com.smarthome.hume.core.model.BatteryControlKind
import com.smarthome.hume.core.model.BatteryInfo
import com.smarthome.hume.core.model.EnergyCost
import com.smarthome.hume.core.model.EnergyDevice
import com.smarthome.hume.core.model.EnergyDonutSlice
import com.smarthome.hume.core.model.EnergyFlowState
import com.smarthome.hume.core.model.EnergyPowerKind
import com.smarthome.hume.core.model.EnergyPowerRow
import com.smarthome.hume.core.model.EnergyTier
import com.smarthome.hume.core.model.EnergyUiState
import com.smarthome.hume.core.model.EnergyWeekPoint
import com.smarthome.hume.core.model.EntityToggleState
import com.smarthome.hume.core.model.HumeConfig
import com.smarthome.hume.core.model.HomeEntity as LegacyEntity
import com.smarthome.hume.core.model.LowBatteryDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToLong

/**
 * Adapter: map HomeAssistantRepository cu sang EnergyUiState moi (demo v4, tab Dien).
 * Port logic tu ui/energy cu (EnergyScreen, EnergyExtraCards, SunsynkFlowCard).
 */
class AppEnergyRepository(
    private val ha: HomeAssistantRepository,
    private val scope: CoroutineScope,
) : EnergyRepository {

    private val _state = MutableStateFlow(EnergyUiState())
    override val energyState: StateFlow<EnergyUiState> = _state.asStateFlow()

    private val weekCache = mutableMapOf<Long, Double>()

    init {
        scope.launch {
            combine(ha.entities, ha.registry, ha.labels, ha.deviceLabels) { e, r, l, dl ->
                buildState(e, r, l, dl)
            }.collect { _state.value = it }
        }
        scope.launch {
            ha.connected.collectLatest { connected ->
                if (!connected) return@collectLatest
                refreshHistory()
                while (isActive) {
                    delay(300_000L)
                    if (ha.connected.value) refreshHistory()
                }
            }
        }
    }

    // ---------- actions ----------

    override fun toggle(entityId: String) = ha.toggle(entityId)

    override fun setNumber(entityId: String, value: Double) {
        ha.callService(
            "number", "set_value",
            "{\"entity_id\":\"$entityId\",\"value\":$value}", entityId,
        )
    }

    override fun setTime(entityId: String, time: String) {
        ha.callService(
            "time", "set_value",
            "{\"entity_id\":\"$entityId\",\"time\":\"${time.trim()}\"}", entityId,
        )
    }

    override fun refreshHistory() {
        scope.launch { loadWeek() }
    }

    // ---------- build ----------

    private fun buildState(
        entities: Map<String, LegacyEntity>,
        registry: Map<String, com.smarthome.hume.core.ha.RegistryEntry>,
        labelNames: Map<String, String>,
        deviceLabels: Map<String, Set<String>>,
    ): EnergyUiState {
        fun v(id: String): Double = entities[id]?.numericState ?: 0.0

        val pvToday = v(HumeConfig.PV_TODAY)
        val homeDaily = v("sensor.energy_home_daily")
        val gridCost = v("sensor.grid_cost")
        val homeCost = v("sensor.home_cost")
        val evn = v("sensor.evn_current_unit_price").let { if (it > 0) it else 2167.0 }
        val rate = (homeCost / 147.49).roundToLong()

        val cost = EnergyCost(
            gridVnd = gridCost.roundToLong(),
            homeVnd = homeCost.roundToLong(),
            buyPrice = (homeCost / 147.49).roundToLong(),
            evnPrice = evn.roundToLong(),
            savedVnd = (evn * pvToday).roundToLong(),
            savedKwh = pvToday,
        )

        val battW = v(HumeConfig.BATTERY_POWER)
        val powerRows = listOf(
            EnergyPowerRow(EnergyPowerKind.Battery, battW),
            EnergyPowerRow(EnergyPowerKind.Solar, v(HumeConfig.PV_POWER)),
            EnergyPowerRow(EnergyPowerKind.Grid, v("sensor.aptomat_tong_power")),
            EnergyPowerRow(EnergyPowerKind.Home, v("sensor.cong_suat_nha")),
        )

        val soc = v(HumeConfig.BATTERY_SOC)
        // Hume goc: charging khi battery_power_flow > 0. User yeu cau dung sensor.battery_current_flow:
        // > 0 = dang sac, < 0 = dang xa (cung dau voi power vi P = V * I, V luon duong)
        val battCurrent = v("sensor.battery_current_flow")
        val charging = battCurrent > 0
        // Trang thai luoi dien (2026-09-30, user): nhan on/off/1/0/text tieng Viet;
        // khong doc duoc -> mac dinh co dien (mat dien la trang thai ngoai le).
        // (2026-09-30) verify that: sensor that tra "On Grid"/"Off Grid"
        // (Solis, co dau cach) -> normalize dau gach ve space truoc khi so.
        val gridStatusRaw = entities[HumeConfig.GRID_STATUS]?.state?.lowercase()
            ?.replace('-', ' ')?.replace('_', ' ') ?: ""
        val gridOn = !(gridStatusRaw == "off" || gridStatusRaw == "0" ||
            gridStatusRaw == "false" || gridStatusRaw == "no" ||
            "off grid" in gridStatusRaw || "outage" in gridStatusRaw ||
            "mất" in gridStatusRaw || "mat dien" in gridStatusRaw)
        val cb1 = v("sensor.aptomat_t1_power") / 1000.0
        val cb2 = v("sensor.aptomat_t2_power") / 1000.0
        val cb3 = v("sensor.aptomat_t3_power") / 1000.0
        val consKw = v("sensor.cong_suat_nha") / 1000.0
        val battChargeKwh = v("sensor.solis_s6_eh1p_today_battery_charge_energy_2")
        val selfUse = if (pvToday > 0.01) {
            ((homeDaily + battChargeKwh) / pvToday).coerceIn(0.0, 1.0) * 100.0
        } else 0.0
        val flow = EnergyFlowState(
            prodKw = v(HumeConfig.PV_POWER) / 1000.0,
            pv1Kw = v("sensor.solis_s6_eh1p_pv_power_1_3") / 1000.0,
            pv2Kw = v("sensor.solis_s6_eh1p_pv_power_2_3") / 1000.0,
            gridKw = v("sensor.aptomat_tong_power") / 1000.0,
            consKw = consKw,
            cb1Kw = cb1, cb2Kw = cb2, cb3Kw = cb3,
            battKw = kotlin.math.abs(battW) / 1000.0,
            battCharging = charging,
            soc = soc,
            todayKwh = pvToday,
            selfUsePct = selfUse,
            gridOn = gridOn,
            // Nang luong (kWh) tung node — port iOS commit 46c365f.
            pvKwh = pvToday,
            gridKwh = v("sensor.aptomat_tong_daily"),
            homeKwh = homeDaily,
            battChgKwh = battChargeKwh,
            battDisKwh = v("sensor.solis_s6_eh1p_today_battery_discharge_energy_2"),
        )

        val tiers = listOf(
            EnergyTier("Tầng 1", cb1), EnergyTier("Tầng 2", cb2), EnergyTier("Tầng 3", cb3),
        )
        val battery = BatteryInfo(
            soc = soc,
            powerW = battW,
            currentA = v("sensor.battery_current_flow"),
            voltageV = v("sensor.solis_s6_eh1p_battery_voltage_2"),
            chargeLimitA = v("sensor.solis_s6_eh1p_battery_charge_current_limitation_bms_2"),
            dischargeLimitA = v("sensor.solis_s6_eh1p_battery_discharge_current_limitation_bms_2"),
        )

        val (powerDevices, energyDevices) = buildDevices(entities, registry, rate)
        val donut = buildDonut(energyDevices)
        val lowBatteries = entities.values
            .filter { e ->
                e.id.endsWith("_battery") &&
                    !e.id.contains("solis") && !e.id.contains("xiaomi") && !e.id.contains("daisy") &&
                    (e.numericState ?: 100.0) < 70.0
            }
            .map { e ->
                LowBatteryDevice(
                    e.friendly().replace("Battery", "").trim(),
                    e.numericState ?: 0.0,
                )
            }
            .sortedBy { it.pct }

        // Trang thai cac entity dieu khien (cho Energy Insights + Insight popup).
        // Chi lay domain dieu khien duoc de map nhe (khong map ~1600 entity).
        val toggleStates = entities.values
            .filter { e ->
                val d = e.id.substringBefore('.')
                d == "light" || d == "switch" || d == "fan" ||
                    d == "climate" || d == "cover" || d == "lock"
            }
            .associate { e ->
                val on = if (e.id.startsWith("climate.")) {
                    // Climate: active khi khac off/unavailable/unknown (cool/heat/auto...)
                    e.state !in setOf("off", "unavailable", "unknown", "")
                } else e.state == "on"
                e.id to EntityToggleState(
                    entityId = e.id,
                    label = e.friendly(),
                    isOn = on,
                    onMinutes = if (on) e.minutesAgo() else null,
                )
            }

        return _state.value.copy(
            todayKwh = homeDaily,
            cost = cost,
            powerRows = powerRows,
            flow = flow,
            tiers = tiers,
            loadTotalKw = consKw,
            battery = battery,
            chargeControls = buildControls(entities, chargeControlSpecs),
            dischargeControls = buildControls(entities, dischargeControlSpecs),
            energyDevices = energyDevices,
            powerDevices = powerDevices,
            donut = donut.first,
            donutTotalKwh = donut.second,
            lowBatteries = lowBatteries,
            toggleStates = toggleStates,
        )
    }

    private suspend fun loadWeek() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val dayMs = 86_400_000L
        val labels = arrayOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
        val days = (6 downTo 1).map { ago ->
            val date = today.minusDays(ago.toLong())
            val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val v = weekCache.getOrPut(start) {
                runCatching {
                    HistoryFetcher.fetchRange("sensor.energy_home_daily", start, start + dayMs)
                        .maxOfOrNull { it.value } ?: 0.0
                }.getOrDefault(0.0)
            }
            EnergyWeekPoint(labels[date.dayOfWeek.value - 1], v)
        } + EnergyWeekPoint(
            // Cot hom nay cung dung nhan thu (T2..CN) nhu cac ngay khac (2026-09-30).
            labels[today.dayOfWeek.value - 1],
            withContext(Dispatchers.Main) {
                ha.entities.value["sensor.energy_home_daily"]?.numericState ?: 0.0
            },
            isToday = true,
        )
        _state.value = _state.value.copy(week = days)
    }

    // ---------- device list (port DeviceFilterList) ----------

    private fun buildDevices(
        entities: Map<String, LegacyEntity>,
        registry: Map<String, com.smarthome.hume.core.ha.RegistryEntry>,
        rate: Long,
    ): Pair<List<EnergyDevice>, List<EnergyDevice>> {
        val labelled = ha.entityIdsWithLabel(DEVICE_LIST_LABEL)
        val usingLabels = labelled.isNotEmpty()
        val candidates: List<LegacyEntity> = if (usingLabels) {
            labelled.mapNotNull { entities[it] }
        } else {
            entities.values
                .filter { e ->
                    e.id.startsWith("sensor.") && e.id.endsWith("_power") &&
                        powerNoise.none { e.id.contains(it) }
                }
                .filter { e ->
                    val entry = registry[e.id]
                    entry?.hiddenBy == null && entry?.disabledBy == null &&
                        entry?.entityCategory == null
                }
                .distinctBy { e -> registry[e.id]?.deviceId ?: e.id }
        }

        val prefixLookup = entities.keys.mapNotNull { id ->
            val i = id.indexOf("_daily_energy_")
            if (i > 0) id.substring(0, i) to id else null
        }.toMap()

        fun agoText(e: LegacyEntity): String {
            val m = e.minutesAgo()
            return when {
                m == null -> "Giám sát"
                m < 1 -> "Vừa xong"
                m < 60 -> "$m phút trước"
                else -> "${m / 60} giờ trước"
            }
        }

        val powerDevices = candidates
            .filter { it.isPowerSensor() }
            .mapNotNull { e ->
                val w = e.watts() ?: return@mapNotNull null
                if (!usingLabels && w <= 0) return@mapNotNull null
                EnergyDevice(e.id, e.friendly(), w, "W", agoText(e))
            }
            .distinctBy { it.id }
            .sortedByDescending { it.value }
            .take(POWER_TOP_COUNT)

        val energyDevices = candidates.mapNotNull { e ->
            val target = if (e.isEnergySensor()) {
                e
            } else {
                val energyId = explicitEnergyMap[e.id]
                    ?: prefixLookup[e.id.removeSuffix("_power")]
                    ?: return@mapNotNull null
                entities[energyId] ?: return@mapNotNull null
            }
            val kwh = target.kwh() ?: return@mapNotNull null
            if (kwh <= 0) return@mapNotNull null
            EnergyDevice(
                target.id, e.friendly(), kwh, "kWh", "Hôm nay",
                (kwh * rate).roundToLong(),
            )
        }
            .distinctBy { it.id }
            .sortedByDescending { it.value }

        return powerDevices to energyDevices
    }

    private fun buildDonut(energyDevices: List<EnergyDevice>): Pair<List<EnergyDonutSlice>, Double> {
        if (energyDevices.isEmpty()) return emptyList<EnergyDonutSlice>() to 0.0
        val total = energyDevices.sumOf { it.value }.coerceAtLeast(0.01)
        val top = energyDevices.take(3).map {
            EnergyDonutSlice(it.name, it.value, it.value / total)
        }
        val rest = energyDevices.drop(3).sumOf { it.value }
        val slices = if (rest > 0.005) {
            top + EnergyDonutSlice("Khác", rest, rest / total)
        } else top
        return slices to total
    }

    // ---------- battery controls ----------

    private fun buildControls(
        entities: Map<String, LegacyEntity>,
        specs: List<Triple<String, String, BatteryControlKind>>,
    ): List<BatteryControl> = specs.mapNotNull { (id, name, kind) ->
        val e = entities[id] ?: return@mapNotNull null
        val state = e.state
        if (state == "unavailable" || state == "unknown") return@mapNotNull null
        BatteryControl(
            entityId = id,
            name = name,
            kind = kind,
            state = state,
            unit = e.unitOfMeasurement().orEmpty(),
            isOn = e.isOn,
        )
    }

    companion object {
        private const val DEVICE_LIST_LABEL = "New"
        private const val POWER_TOP_COUNT = 5

        private val chargeControlSpecs = listOf(
            Triple("switch.allow_grid_to_charge_the_battery_2", "Bật/Tắt sạc AC", BatteryControlKind.Switch),
            Triple("number.solis_s6_eh1p_battery_max_charge_current_2", "Sạc DC", BatteryControlKind.Number),
            Triple("number.solis_s6_eh1p_grid_time_of_use_charge_battery_current_slot_1_2", "Sạc AC", BatteryControlKind.Number),
            Triple("number.solis_s6_eh1p_grid_time_of_use_charge_cut_off_soc_slot_1_2", "SOC kết thúc", BatteryControlKind.Number),
            Triple("switch.grid_time_of_use_charging_period_1_2", "Theo thời gian", BatteryControlKind.Switch),
            Triple("time.solis_s6_eh1p_grid_time_of_use_charge_start_slot_1_2", "Giờ bắt đầu", BatteryControlKind.Time),
            Triple("time.solis_s6_eh1p_grid_time_of_use_charge_end_slot_1_2", "Giờ kết thúc", BatteryControlKind.Time),
            Triple("number.solis_s6_eh1p_force_charge_soc_2", "Sạc bắt buộc", BatteryControlKind.Number),
            Triple(HumeConfig.BACKUP_SOC, "Pin dự trữ", BatteryControlKind.Number),
        )
        private val dischargeControlSpecs = listOf(
            Triple("number.solis_s6_eh1p_battery_max_discharge_current_2", "Xả DC", BatteryControlKind.Number),
            Triple("number.solis_s6_eh1p_off_grid_overdischarge_soc_2", "Xả mất lưới", BatteryControlKind.Number),
            Triple("number.solis_s6_eh1p_overdischarge_soc_2", "Xả quá ngưỡng", BatteryControlKind.Number),
        )

        private val powerNoise = listOf(
            "solis", "battery", "soc", "soh", "dod", "alarm", "zigbee", "hourly", "monthly",
            "daily_cooling_energy", "daily_heating_energy", "aptomat", "cooling", "heating",
            "home_power", "grid_power", "cong_suat",
        )
        private val explicitEnergyMap = mapOf(
            "sensor.air_condition_current_extrapolated_power" to "sensor.air_condition_daily_energy_ac",
            "sensor.dieu_hoa_power" to "sensor.dieu_hoa_power_energy",
        )
    }
}

// ---------- legacy entity helpers (port tu EnergyExtraCards) ----------

private fun LegacyEntity.attr(key: String): String? =
    (attributes[key] as? kotlinx.serialization.json.JsonPrimitive)?.content

private fun LegacyEntity.friendly(): String =
    attr("friendly_name") ?: id.substringAfter('.').replace('_', ' ')

private fun LegacyEntity.deviceClass(): String? = attr("device_class")

private fun LegacyEntity.unitOfMeasurement(): String? = attr("unit_of_measurement")

private fun LegacyEntity.unitRaw(): String? = unitOfMeasurement()?.trim()

private fun LegacyEntity.isPowerSensor(): Boolean =
    deviceClass() == "power" || unitRaw() in setOf("W", "kW", "mW", "MW")

private fun LegacyEntity.isEnergySensor(): Boolean =
    deviceClass() == "energy" || unitRaw() in setOf("Wh", "kWh", "MWh")

private fun LegacyEntity.watts(): Double? {
    val raw = numericState ?: return null
    return when (unitRaw()) {
        "kW" -> raw * 1000.0
        "MW" -> raw * 1_000_000.0
        "mW" -> raw / 1000.0
        else -> raw
    }
}

private fun LegacyEntity.kwh(): Double? {
    val raw = numericState ?: return null
    return when (unitRaw()) {
        "Wh" -> raw / 1000.0
        "MWh" -> raw * 1000.0
        else -> raw
    }
}
