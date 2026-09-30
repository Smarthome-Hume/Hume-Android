package com.smarthome.hume.brief

import kotlinx.serialization.Serializable

/** Du lieu brief sang: tao boi BriefWorker luc 6:00, luu cache JSON, UI chi doc. */
@Serializable
data class BriefDeviceStat(
    val name: String,
    /** Key icon Ms (vd "ac_unit"); UI map sang glyph. */
    val iconKey: String,
    val kwh: Double,
)

@Serializable
data class BriefWeather(
    /** "sunny" | "cloudy" | "rain" — UI map sang icon. */
    val kind: String,
    val conditionVi: String,
    val tempMin: Int,
    val tempMax: Int,
    val humidity: Int,
    val pvEstimateKwh: Double,
    val hasLocation: Boolean,
)

@Serializable
data class BriefDaily(
    /** "29/09" */
    val dateLabel: String,
    /** "Thứ Ba" */
    val weekdayVi: String,
    val gridImportKwh: Double,
    val gridCostVnd: Long,
    val pvKwh: Double,
    val selfSufficiencyPct: Int,
    val billingCostVnd: Long,
    val billingKwh: Double,
    val topDevices: List<BriefDeviceStat>,
    val aiInsight: String,
    val aiTip: String,
    val weather: BriefWeather,
)

@Serializable
data class BriefFloorStat(val name: String, val kwh: Double)

@Serializable
data class BriefMonthly(
    /** "9/2026" */
    val monthLabel: String,
    val gridKwh: Double,
    val costVnd: Long,
    /** Tien thuc te ca nha tieu thu trong ky (sensor.home_cost). Default 0 de doc duoc cache cu. */
    val homeCostVnd: Long = 0L,
    val pvKwh: Double,
    val savedVnd: Long,
    val floors: List<BriefFloorStat>,
    val topDevices: List<BriefDeviceStat>,
    val aiSummary: String,
)

@Serializable
data class BriefCache(
    val daily: BriefDaily? = null,
    val monthly: BriefMonthly? = null,
    val generatedAtMs: Long = 0L,
)
