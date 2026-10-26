package com.smarthome.hume.core.model

/** Mot cot trong bieu do tuan (demo v4: wcard). */
data class EnergyWeekPoint(
    val label: String,
    val kwh: Double,
    val isToday: Boolean = false,
)

/** The chi phi dien (demo v4: cost card). */
data class EnergyCost(
    val gridVnd: Long = 0,
    val homeVnd: Long = 0,
    val buyPrice: Long = 0,
    val evnPrice: Long = 0,
    val savedVnd: Long = 0,
    val savedKwh: Double = 0.0,
)

enum class EnergyPowerKind { Battery, Solar, Grid, Home }

/** Mot hang trong the "Cong suat hoat dong" (chuan hoa theo 7000 W). */
data class EnergyPowerRow(
    val kind: EnergyPowerKind,
    val watts: Double,
)

/** Du lieu cho flow card nang luong (demo v4: secEnergy). */
data class EnergyFlowState(
    val prodKw: Double = 0.0,
    val pv1Kw: Double = 0.0,
    val pv2Kw: Double = 0.0,
    val gridKw: Double = 0.0,
    val consKw: Double = 0.0,
    val cb1Kw: Double = 0.0,
    val cb2Kw: Double = 0.0,
    val cb3Kw: Double = 0.0,
    val battKw: Double = 0.0,
    val battCharging: Boolean = true,
    val soc: Double = 0.0,
    val todayKwh: Double = 0.0,
    val selfUsePct: Double = 0.0,
    /** true = luoi co dien; false = mat dien (chay qua cong backup). */
    val gridOn: Boolean = true,
    /** Trang thai inverter tu sensor.solis_s6_eh1p_status_string_3 (port iOS 007d8ce). */
    val inverterStatus: String = "",
    // Nang luong (kWh) tung node — port iOS commit 46c365f.
    /** San luong PV hom nay (kWh). */
    val pvKwh: Double = 0.0,
    /** Dien luoi mua hom nay (kWh). */
    val gridKwh: Double = 0.0,
    /** Nha tieu thu hom nay (kWh). */
    val homeKwh: Double = 0.0,
    /** Pin sac vao hom nay (kWh). */
    val battChgKwh: Double = 0.0,
    /** Pin xa ra hom nay (kWh). */
    val battDisKwh: Double = 0.0,
)

/** Mot dong trong the "Thiet bi tieu thu". */
data class EnergyDevice(
    val id: String,
    val name: String,
    val value: Double,
    val unit: String,
    val ago: String,
    val costVnd: Long? = null,
)

/** Mot lat cat trong donut "Co cau tieu thu". */
data class EnergyDonutSlice(
    val name: String,
    val kwh: Double,
    val fraction: Double,
)

data class LowBatteryDevice(
    val name: String,
    val pct: Double,
)

data class EnergyTier(
    val name: String,
    val kw: Double,
)

data class BatteryInfo(
    val soc: Double = 0.0,
    val powerW: Double = 0.0,
    val currentA: Double = 0.0,
    val voltageV: Double = 0.0,
    val chargeLimitA: Double = 0.0,
    val dischargeLimitA: Double = 0.0,
)

enum class BatteryControlKind { Switch, Number, Time }

/** Mot hang dieu khien pin trong expander Sac/Xa pin. */
data class BatteryControl(
    val entityId: String,
    val name: String,
    val kind: BatteryControlKind,
    val state: String = "",
    val unit: String = "",
    val isOn: Boolean = false,
)

/**
 * Du lieu sub-tab Nuoc RO (port iOS EnergyWaterTab.swift, GAP-01).
 * Entity ID THAT tren HA .22 (integration rice_home, may Rice Home R-RO-M-01):
 * HA sinh ID theo ten thiet bi + hau to so theo thu tu dang ky — KHONG phai
 * dang r_ro_m_01_<key>. Dung "sua" lai: entity dang <key> khong ton tai.
 *   TDS -> sensor.r_ro_m_01, nhiet do -> sensor.r_ro_m_01_2,
 *   lit hom nay -> sensor.r_ro_m_01_4, loi 1 con lai -> sensor.r_ro_m_01_7,
 *   loi 1 da loc -> sensor.r_ro_m_01_8, bom -> binary_sensor.r_ro_m_01,
 *   nuoc cap -> binary_sensor.r_ro_m_01_2, dang dung -> binary_sensor.r_ro_m_01_3.
 * Loi 4-5 KHONG co du lieu (may khong bao) nen khong hien, giong iOS.
 */
data class WaterUiState(
    val tempC: Double = 0.0,
    val todayLiters: Double = 0.0,
    val tds: Double = 0.0,
    val filterPct: Double = 0.0,
    val filterLiters: Double = 0.0,
    val pumpOn: Boolean = false,
    val waterInOn: Boolean = false,
    val waterUseOn: Boolean = false,
    /** So phut ke tu last_changed (null neu khong ro) — cho dong "X phut truoc". */
    val filterMinutesAgo: Int? = null,
    val pumpMinutesAgo: Int? = null,
    val waterInMinutesAgo: Int? = null,
    val waterUseMinutesAgo: Int? = null,
)

/** Trang thai rut gon 1 entity dieu khien duoc (phuc vu Energy Insights + Insight popup). */
data class EntityToggleState(
    val entityId: String,
    val label: String,
    val isOn: Boolean,
    /** So phut ke tu last_changed (null neu khong ro). */
    val onMinutes: Int? = null,
)

data class EnergyUiState(
    val week: List<EnergyWeekPoint> = emptyList(),
    val todayKwh: Double = 0.0,
    val cost: EnergyCost = EnergyCost(),
    val powerRows: List<EnergyPowerRow> = emptyList(),
    val flow: EnergyFlowState = EnergyFlowState(),
    val tiers: List<EnergyTier> = emptyList(),
    val loadTotalKw: Double = 0.0,
    val battery: BatteryInfo = BatteryInfo(),
    val chargeControls: List<BatteryControl> = emptyList(),
    val dischargeControls: List<BatteryControl> = emptyList(),
    /** Che do Nang luong: kWh hom nay + VND (gi nguyen danh sach). */
    val energyDevices: List<EnergyDevice> = emptyList(),
    /** Che do Cong suat: top 5 W. */
    val powerDevices: List<EnergyDevice> = emptyList(),
    val donut: List<EnergyDonutSlice> = emptyList(),
    val donutTotalKwh: Double = 0.0,
    val lowBatteries: List<LowBatteryDevice> = emptyList(),
    /** Trang thai cac entity dieu khien (light/switch/fan/climate/cover/lock) — cho Insights. */
    val toggleStates: Map<String, EntityToggleState> = emptyMap(),
    /** Du lieu sub-tab Nuoc RO (GAP-01). */
    val water: WaterUiState = WaterUiState(),
    /**
     * Bieu do tuan nuoc: 7 cot (6 ngay qua + hom nay), gia tri = max lit trong
     * ngay cua sensor.r_ro_m_01_4 (port iOS loadWaterWeekMax). Truong `kwh`
     * cua EnergyWeekPoint o day mang lit — don vi hien thi do WeekChartD(unit="L")
     * quyet dinh, khong phai ten truong.
     */
    val waterWeek: List<EnergyWeekPoint> = emptyList(),
)
