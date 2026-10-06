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
)
