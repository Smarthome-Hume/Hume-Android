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
    /** "9/2026" — ky dien ket thuc trong thang 9 (28/8-27/9). */
    val monthLabel: String,
    /** Khoa dinh danh ky, vd "2026-08-28_2026-09-27". "" = cache cu -> luon build lai. */
    val periodKey: String = "",
    val gridKwh: Double,
    val costVnd: Long,
    /** Tien thuc te ca nha tieu thu trong ky (sensor.home_cost). Default 0 de doc duoc cache cu. */
    val homeCostVnd: Long = 0L,
    /** Ti le % tiet kiem duoc nho dien mat troi = (homeCost - gridCost) / homeCost. */
    val savingsPct: Int = 0,
    val pvKwh: Double,
    val savedVnd: Long,
    val floors: List<BriefFloorStat>,
    val topDevices: List<BriefDeviceStat>,
    val aiSummary: String,
)

@Serializable
data class BriefCache(
    val daily: BriefDaily? = null,
    /** Tong hop thang theo lich (ky dien da chot, vd "9/2026" = 28/8-27/9). */
    val monthly: BriefMonthly? = null,
    /** Tong hop ky dang chay do user bam "Tao ngay" (rieng biet). */
    val monthlyLive: BriefMonthly? = null,
    val generatedAtMs: Long = 0L,
)
