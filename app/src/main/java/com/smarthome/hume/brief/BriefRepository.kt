package com.smarthome.hume.brief

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.smarthome.hume.core.data.AiRepository
import com.smarthome.hume.core.data.AiResult
import com.smarthome.hume.core.ha.HaEndpointResolver
import com.smarthome.hume.core.ha.HistoryFetcher
import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Brief sang: gom so lieu nang luong tu HA + thoi tiet Met.no (theo vi tri)
 * -> nho AI soan nhan dinh -> luu cache JSON. Duoc goi boi BriefWorker (6:00)
 * va khi user mo trang Brief ma cache cu.
 *
 * localUrl/remoteUrl tro ve CUNG 1 HA nen entity_id giong nhau; chi can
 * fallback ket noi.
 */
class BriefRepository(
    private val context: Context,
    private val localUrl: String,
    private val remoteUrl: String,
    private val token: String,
    private val ai: AiRepository,
) {
    private val tag = "HumeBrief"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val vi = Locale("vi", "VN")

    companion object {
        /** conditionVi cua BriefWeather khi chua lay duoc du lieu that. */
        const val WEATHER_NO_DATA = "Chưa có dữ liệu"
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .build()

    private val cacheFile = File(context.filesDir, "brief_cache.json")
    private val prefs = context.getSharedPreferences("brief_prefs", Context.MODE_PRIVATE)

    private val _cache = MutableStateFlow(loadCache())
    val cache: StateFlow<BriefCache?> = _cache.asStateFlow()

    private val _hasNew = MutableStateFlow(prefs.getBoolean("brief_new", false))
    val hasNew: StateFlow<Boolean> = _hasNew.asStateFlow()

    fun loadCache(): BriefCache? = runCatching {
        if (!cacheFile.exists()) return null
        json.decodeFromString<BriefCache>(cacheFile.readText())
    }.getOrNull()

    fun markSeen() {
        prefs.edit().putBoolean("brief_new", false).apply()
        _hasNew.value = false
    }

    // ---------------- public ----------------

    /**
     * Worker 6:00 goi: brief ngay (hom qua) + brief ky (neu hom nay ngay 27,
     * ngay cuoi ky chot dien).
     * [forceMonthly] = true khi user bam "Tao ngay" o tab Thang: build luon
     * brief thang hien tai (luy ke den hom nay) thay vi cho den mung 1.
     */
    suspend fun refreshAll(forceMonthly: Boolean = false) {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val daily = buildDaily(today.minusDays(1))
        var monthly = _cache.value?.monthly
        val curLabel = billingLabel(today)
        // Ky chot dien EVN: 28 thang truoc -> 27 thang nay. Build brief ky
        // vao ngay 27 (so lieu gan chot nhat); sensor reset 00:00 ngay 28
        // nen khong build vao ngay 28 (so lieu da ve ~0).
        if (today.dayOfMonth == 27) {
            monthly = buildMonthly()
        } else if (forceMonthly && monthly?.monthLabel != curLabel) {
            monthly = buildMonthly()
        }
        if (daily != null) {
            saveCache(BriefCache(daily = daily, monthly = monthly, generatedAtMs = System.currentTimeMillis()))
            prefs.edit().putBoolean("brief_new", true).apply()
            _hasNew.value = true
        }
    }

    /**
     * Chi fetch lai thoi tiet roi va vao brief ngay hien co (khong goi AI,
     * khong cham HA). Tra ve true neu lay duoc vi tri + du lieu moi.
     * Dung de tu phuc hoi khi mo trang ma brief cu chua co thoi tiet.
     */
    suspend fun refreshWeatherOnly(): Boolean {
        val w = fetchWeather()
        if (!w.hasLocation) return false
        val c = loadCache() ?: return false
        val daily = c.daily ?: return false
        saveCache(c.copy(daily = daily.copy(weather = w)))
        return true
    }

    // ---------------- daily ----------------

    private suspend fun buildDaily(yesterday: LocalDate): BriefDaily? = supervisorScope {
        val importKwh = getState(E.YESTERDAY_IMPORT) ?: return@supervisorScope null
        val pvKwh = getState(E.YESTERDAY_PV) ?: 0.0
        val unitPrice = getState(E.UNIT_PRICE) ?: 2167.0
        val costVnd = (importKwh * unitPrice * 1.10).roundToLong()
        val total = importKwh + pvKwh
        val selfPct = if (total > 0) (pvKwh / total * 100).roundToInt() else 0

        val billingCost = getState(E.GRID_COST)?.roundToLong() ?: 0L
        val billingKwh = getState(E.GRID_IMPORT_BILLING) ?: 0.0

        val devices = DEVICE_SENSORS.map { (eid, name, icon) ->
            async { Triple(name, icon, yesterdayDelta(eid, yesterday)) }
        }.awaitAll()
            .filter { it.third > 0.05 }
            .sortedByDescending { it.third }
            .take(3)
            .map { BriefDeviceStat(it.first, it.second, (it.third * 10).roundToInt() / 10.0) }

        val weather = fetchWeather()

        val (insight, tip) = aiDaily(yesterday, importKwh, costVnd, pvKwh, selfPct, devices, weather)

        BriefDaily(
            dateLabel = yesterday.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM")),
            weekdayVi = yesterday.dayOfWeek.getDisplayName(TextStyle.FULL, vi)
                .replaceFirstChar { it.uppercase() },
            gridImportKwh = r1(importKwh),
            gridCostVnd = costVnd,
            pvKwh = r1(pvKwh),
            selfSufficiencyPct = selfPct,
            billingCostVnd = billingCost,
            billingKwh = r1(billingKwh),
            topDevices = devices,
            aiInsight = insight,
            aiTip = tip,
            weather = weather,
        )
    }

    // ---------------- monthly ----------------

    /**
     * Ky chot dien EVN: tu ngay 28 thang truoc den ngay 27 thang nay.
     * Tra ve cap (ngayBatDau, ngayKetThuc) cua ky chua [today].
     */
    private fun billingPeriod(today: LocalDate): Pair<LocalDate, LocalDate> =
        if (today.dayOfMonth >= 28) {
            today.withDayOfMonth(28) to today.plusMonths(1).withDayOfMonth(27)
        } else {
            today.minusMonths(1).withDayOfMonth(28) to today.withDayOfMonth(27)
        }

    /** Nhan ky dien, vd "28/9 – 27/10". */
    private fun billingLabel(today: LocalDate = LocalDate.now(ZoneId.systemDefault())): String {
        val (s, e) = billingPeriod(today)
        return "${s.dayOfMonth}/${s.monthValue} – ${e.dayOfMonth}/${e.monthValue}"
    }

    private suspend fun buildMonthly(): BriefMonthly? = supervisorScope {
        // kWh mua EVN theo KY CHOT (28 -> 27), khong dung sensor thang duong lich.
        val gridKwh = getState(E.GRID_IMPORT_BILLING) ?: getState(E.EVN_MONTHLY)
            ?: return@supervisorScope null
        val unitPrice = getState(E.UNIT_PRICE) ?: 2167.0
        val pvKwh = getState(E.PV_MONTH) ?: 0.0
        // Tien dien thang: lay truc tiep tu sensor.grid_cost (user xac nhan
        // 2026-09-30: day la data tien theo thang). Chi tu tinh khi sensor
        // khong co du lieu.
        val costVnd = getState(E.GRID_COST)?.roundToLong()
            ?: (gridKwh * unitPrice * 1.10).roundToLong()
        // Tien thuc te ca nha tieu thu trong ky (tinh tu sensor.energy_home).
        val homeCostVnd = getState(E.HOME_COST)?.roundToLong() ?: 0L
        // Ti le tiet kiem nho PV: (tien thuc te tieu thu - tien tra EVN) / tien thuc te.
        val savingsPct = if (homeCostVnd > 0) {
            ((homeCostVnd - costVnd).coerceAtLeast(0).toDouble() / homeCostVnd * 100)
                .roundToInt().coerceIn(0, 100)
        } else 0
        val floors = listOf("Tầng 1" to E.T1_MONTHLY, "Tầng 2" to E.T2_MONTHLY, "Tầng 3" to E.T3_MONTHLY)
            .map { (name, eid) -> async { BriefFloorStat(name, r1(getState(eid) ?: 0.0)) } }
            .awaitAll()
            .sortedByDescending { it.kwh }
        // Top thiet bi ky: delta lich su trong ky chot (28 -> 27) cho tung thiet bi.
        val (periodStart, periodEnd) = billingPeriod(LocalDate.now(ZoneId.systemDefault()))
        val devices = DEVICE_SENSORS.map { (eid, name, icon) ->
            async { Triple(name, icon, rangeDelta(eid, periodStart, periodEnd)) }
        }.awaitAll()
            .filter { it.third > 0.5 }
            .sortedByDescending { it.third }
            .take(3)
            .map { BriefDeviceStat(it.first, it.second, (it.third * 10).roundToInt() / 10.0) }

        val summary = aiMonthly(billingLabel(), gridKwh, costVnd, pvKwh, floors, devices)

        BriefMonthly(
            monthLabel = billingLabel(),
            gridKwh = r1(gridKwh),
            costVnd = costVnd,
            homeCostVnd = homeCostVnd,
            savingsPct = savingsPct,
            pvKwh = r1(pvKwh),
            savedVnd = (pvKwh * unitPrice).roundToLong(),
            floors = floors,
            topDevices = devices,
            aiSummary = summary,
        )
    }

    // ---------------- HA ----------------

    /** Doc 1 state so; local truoc, remote sau (chi fallback khi loi ket noi). */
    private suspend fun getState(entityId: String): Double? = withContext(Dispatchers.IO) {
        val bases = listOf(localUrl, remoteUrl).map { it.trim().trimEnd('/') }
            .filter { it.isNotBlank() }.distinct()
        for (base in bases) {
            try {
                val req = Request.Builder()
                    .url("$base/api/states/$entityId")
                    .header("Authorization", "Bearer $token")
                    .build()
                http.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext null
                    val body = resp.body?.string().orEmpty()
                    val st = json.parseToJsonElement(body).let {
                        it as? kotlinx.serialization.json.JsonObject
                    }?.get("state")?.let {
                        (it as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull
                    }
                    val v = st?.toDoubleOrNull()
                    if (v != null) return@withContext v
                    return@withContext null
                }
            } catch (t: Exception) {
                Log.w(tag, "getState $entityId via $base failed: ${t.message}")
                // thu base tiep theo
            }
        }
        null
    }

    /** San luong tang trong 1 ngay: hieu so cuoi - dau cua sensor tich luy. */
    private suspend fun yesterdayDelta(entityId: String, day: LocalDate): Double {
        val zone = ZoneId.systemDefault()
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return rangeDeltaMs(entityId, start, end)
    }

    private suspend fun rangeDelta(entityId: String, from: LocalDate, to: LocalDate): Double {
        val zone = ZoneId.systemDefault()
        val start = from.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = to.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return rangeDeltaMs(entityId, start, end)
    }

    private suspend fun rangeDeltaMs(entityId: String, startMs: Long, endMs: Long): Double {
        ensureHistoryConfigured()
        return try {
            val pts = HistoryFetcher.fetchRange(entityId, startMs, endMs)
            if (pts.size < 2) 0.0 else (pts.last().value - pts.first().value).coerceAtLeast(0.0)
        } catch (t: Exception) {
            Log.w(tag, "history $entityId failed: ${t.message}")
            0.0
        }
    }

    private fun ensureHistoryConfigured() {
        if (!HistoryFetcher.isConfigured) {
            val r = HaEndpointResolver()
            r.configure(localUrl, remoteUrl)
            HistoryFetcher.configure(r, token)
        }
    }

    // ---------------- weather (Met.no) ----------------

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private suspend fun currentLatLon(): Pair<Double, Double>? {
        val savedLat = prefs.getFloat("last_lat", Float.NaN)
        val savedLon = prefs.getFloat("last_lon", Float.NaN)
        val saved = if (!savedLat.isNaN() && !savedLon.isNaN()) savedLat.toDouble() to savedLon.toDouble() else null
        if (!hasLocationPermission()) return saved
        val fused = LocationServices.getFusedLocationProviderClient(context)
        fun rememberLoc(loc: Pair<Double, Double>) {
            prefs.edit()
                .putFloat("last_lat", loc.first.toFloat())
                .putFloat("last_lon", loc.second.toFloat())
                .apply()
        }
        // B1: Vi tri tuoi (toi da 8s). getCurrentLocation hay tra null khi may
        // idle / trong nha / chua co fix — khong duoc dung o day.
        val fresh: Pair<Double, Double>? = withTimeoutOrNull(8000) {
            suspendCancellableCoroutine { cont ->
                val cts = CancellationTokenSource()
                cont.invokeOnCancellation { cts.cancel() }
                try {
                    fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                        .addOnSuccessListener { l ->
                            if (!cont.isCompleted) cont.resume(l?.let { it.latitude to it.longitude })
                        }
                        .addOnFailureListener { if (!cont.isCompleted) cont.resume(null) }
                        .addOnCanceledListener { if (!cont.isCompleted) cont.resume(null) }
                } catch (t: Throwable) {
                    if (!cont.isCompleted) cont.resume(null)
                }
            }
        }
        if (fresh != null) {
            rememberLoc(fresh)
            return fresh
        }
        // B2: Vi tri cache cua Play Services — gan nhu luon co neu may tung
        // dung vi tri, du cu vai gio van du dung cho thoi tiet khu vuc.
        val last: Pair<Double, Double>? = runCatching {
            suspendCancellableCoroutine { cont ->
                try {
                    fused.lastLocation
                        .addOnSuccessListener { l ->
                            if (!cont.isCompleted) cont.resume(l?.let { it.latitude to it.longitude })
                        }
                        .addOnFailureListener { if (!cont.isCompleted) cont.resume(null) }
                } catch (t: Throwable) {
                    if (!cont.isCompleted) cont.resume(null)
                }
            }
        }.getOrNull()
        if (last != null) {
            rememberLoc(last)
            return last
        }
        // B3: Vi tri lan cuoi lay duoc (da luu).
        return saved
    }

    private suspend fun fetchWeather(): BriefWeather = withContext(Dispatchers.IO) {
        // Moi duong that bai -> hasLocation=false de lan mo Brief sau tu retry
        // (refreshIfStale chi thu lai khi hasLocation=false). Chi duong parse
        // thanh cong moi tra hasLocation=true.
        val fallback = BriefWeather("cloudy", WEATHER_NO_DATA, 0, 0, 0, 0.0, false)
        val (lat, lon) = currentLatLon() ?: run {
            Log.w(tag, "weather: no location (chua cap quyen hoac tat dinh vi he thong)")
            return@withContext fallback
        }
        try {
            val url = "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=%.4f&lon=%.4f"
                .format(Locale.US, lat, lon)
            val req = Request.Builder().url(url)
                .header("User-Agent", "HumeAndroid/1.0 (https://github.com/Smarthome-Hume/Hume-Android)")
                .build()
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.w(tag, "weather http ${resp.code}")
                    return@withContext fallback
                }
                val root = json.parseToJsonElement(resp.body?.string().orEmpty()).let {
                    (it as? kotlinx.serialization.json.JsonObject) ?: return@withContext fallback
                }
                val zone = ZoneId.systemDefault()
                val today = LocalDate.now(zone)
                val series = root["properties"]?.let { (it as? kotlinx.serialization.json.JsonObject)?.get("timeseries") }
                    ?.let { it as? kotlinx.serialization.json.JsonArray } ?: return@withContext fallback

                data class P(val dt: java.time.ZonedDateTime, val temp: Double, val hum: Int, val cloud: Double, val symbol: String?)
                val points = series.mapNotNull { el ->
                    val o = el as? kotlinx.serialization.json.JsonObject ?: return@mapNotNull null
                    val timeStr = o["time"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull } ?: return@mapNotNull null
                    val dt = runCatching { java.time.Instant.parse(timeStr).atZone(zone) }.getOrNull() ?: return@mapNotNull null
                    val data = o["data"] as? kotlinx.serialization.json.JsonObject ?: return@mapNotNull null
                    val details = (data["instant"] as? kotlinx.serialization.json.JsonObject)?.get("details") as? kotlinx.serialization.json.JsonObject
                    val temp = details?.get("air_temperature")?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull?.toDoubleOrNull() } ?: return@mapNotNull null
                    val hum = details?.get("relative_humidity")?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull?.toDoubleOrNull() }?.roundToInt() ?: 0
                    val cloud = details?.get("cloud_area_fraction")?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull?.toDoubleOrNull() } ?: 50.0
                    val sym = (data["next_6_hours"] as? kotlinx.serialization.json.JsonObject)?.get("summary")
                        ?.let { (it as? kotlinx.serialization.json.JsonObject)?.get("symbol_code") }
                        ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull }
                    P(dt, temp, hum, cloud, sym)
                }.filter { it.dt.toLocalDate() == today }
                if (points.isEmpty()) return@withContext fallback

                val tMin = points.minOf { it.temp }.roundToInt()
                val tMax = points.maxOf { it.temp }.roundToInt()
                val hum = points.minByOrNull { kotlin.math.abs(it.dt.hour - 12) }?.hum ?: 0
                val symbol = points.firstNotNullOfOrNull { it.symbol } ?: "cloudy"
                val dayCloud = points.filter { it.dt.hour in 6..18 }.map { it.cloud }
                val cloudAvg = if (dayCloud.isNotEmpty()) dayCloud.average() else 50.0
                val pvEst = (21.0 * (1.0 - cloudAvg / 100.0 * 0.75)).coerceIn(2.0, 22.0)
                val (kind, condVi) = symbolToVi(symbol)
                BriefWeather(kind, condVi, tMin, tMax, hum, r1(pvEst), true)
            }
        } catch (t: Exception) {
            Log.w(tag, "weather failed: ${t.message}")
            fallback
        }
    }

    private fun symbolToVi(symbol: String): Pair<String, String> {
        val s = symbol.lowercase()
        return when {
            "thunder" in s -> "rain" to "Dông"
            "heavyrain" in s -> "rain" to "Mưa to"
            "rain" in s -> "rain" to if ("light" in s || "shower" in s) "Mưa rào nhẹ" else "Mưa"
            "sleet" in s || "snow" in s -> "rain" to "Mưa tuyết"
            "fog" in s -> "cloudy" to "Sương mù"
            "cloudy" in s -> "cloudy" to "Nhiều mây"
            "partlycloudy" in s -> "cloudy" to "Mây rải rác"
            "fair" in s -> "sunny" to "Ít mây"
            "clearsky" in s -> "sunny" to "Trời quang"
            else -> "cloudy" to "Nhiều mây"
        }
    }

    // ---------------- AI ----------------

    @Serializable
    private data class AiDaily(val insight: String = "", val tip: String = "")

    @Serializable
    private data class AiMonthly(val summary: String = "")

    private val aiSystem = "Bạn là trợ lý năng lượng cho app smarthome Hume. Viết tiếng Việt, ngắn gọn, ấm áp, thực tế. " +
        "Chỉ trả về JSON hợp lệ, không thêm chữ nào khác. " +
        "TUYỆT ĐỐI không nhắc tới điện thoại, máy tính, Samsung hay thiết bị di động."

    private fun parseAiJson(raw: String): String? {
        val s = raw.indexOf('{')
        val e = raw.lastIndexOf('}')
        if (s < 0 || e <= s) return null
        return raw.substring(s, e + 1)
    }

    private suspend fun aiDaily(
        day: LocalDate, importKwh: Double, costVnd: Long, pvKwh: Double,
        selfPct: Int, devices: List<BriefDeviceStat>, weather: BriefWeather,
    ): Pair<String, String> {
        val devStr = devices.joinToString("; ") { "${it.name} ${r1(it.kwh)} kWh" }
        val user = "Số liệu hôm qua (${day.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"))}): " +
            "mua EVN ${r1(importKwh)} kWh (khoảng ${fmtVnd(costVnd)}), PV sản xuất ${r1(pvKwh)} kWh, tự chủ $selfPct%. " +
            "Top thiết bị: $devStr. " +
            "Thời tiết hôm nay: ${weather.conditionVi}, ${weather.tempMin}–${weather.tempMax}°C, PV dự kiến ~${r1(weather.pvEstimateKwh)} kWh. " +
            "Trả JSON: {\"insight\": \"2 câu nhận định\", \"tip\": \"1 lời khuyên cụ thể, có con số tiết kiệm ước tính\"}"
        val fallbackInsight = "Hôm qua nhà dùng ${r1(importKwh)} kWh từ EVN (khoảng ${fmtVnd(costVnd)}), giàn PV sản xuất ${r1(pvKwh)} kWh."
        val fallbackTip = if (devices.isNotEmpty())
            "Thiết bị tốn nhất là ${devices[0].name} (${r1(devices[0].kwh)} kWh). Hạn chế dùng giờ cao điểm 17h–20h để giảm tiền điện."
        else "Theo dõi thêm vài ngày để có lời khuyên chính xác hơn."
        return when (val r = ai.chat(aiSystem, user)) {
            is AiResult.Ok -> {
                val j = parseAiJson(r.value)?.let { runCatching { json.decodeFromString<AiDaily>(it) }.getOrNull() }
                val insight = j?.insight?.takeIf { it.isNotBlank() } ?: fallbackInsight
                val tip = j?.tip?.takeIf { it.isNotBlank() } ?: fallbackTip
                // Loc cung: khong bao gio de tip nhac toi dien thoai lot qua UI.
                insight to tip
            }
            is AiResult.Err -> fallbackInsight to fallbackTip
        }
    }

    private suspend fun aiMonthly(
        periodLabel: String, gridKwh: Double, costVnd: Long, pvKwh: Double,
        floors: List<BriefFloorStat>, devices: List<BriefDeviceStat>,
    ): String {
        val floorStr = floors.joinToString("; ") { "${it.name} ${r1(it.kwh)} kWh" }
        val devStr = devices.joinToString("; ") { "${it.name} ${r1(it.kwh)} kWh" }
        val user = "Số liệu kỳ chốt điện $periodLabel: mua EVN ${r1(gridKwh)} kWh (khoảng ${fmtVnd(costVnd)}), " +
            "PV sản xuất ${r1(pvKwh)} kWh. Theo tầng: $floorStr. Top thiết bị: $devStr. " +
            "Trả JSON: {\"summary\": \"3-4 câu tổng kết + 1 dự báo/gợi ý cho kỳ tới\"}"
        val fallback = "Kỳ $periodLabel nhà mua ${r1(gridKwh)} kWh từ EVN (khoảng ${fmtVnd(costVnd)}), " +
            "PV sản xuất ${r1(pvKwh)} kWh." +
            (floors.firstOrNull()?.let { " ${it.name} tiêu thụ nhiều nhất (${r1(it.kwh)} kWh)." } ?: "")
        return when (val r = ai.chat(aiSystem, user)) {
            is AiResult.Ok -> {
                parseAiJson(r.value)?.let { runCatching { json.decodeFromString<AiMonthly>(it) }.getOrNull() }
                    ?.summary?.takeIf { it.isNotBlank() } ?: fallback
            }
            is AiResult.Err -> fallback
        }
    }

    // ---------------- cache ----------------

    private fun saveCache(c: BriefCache) {
        _cache.value = c
        runCatching { cacheFile.writeText(json.encodeToString(c)) }
            .onFailure { Log.w(tag, "save cache failed: ${it.message}") }
    }

    // ---------------- helpers ----------------

    private fun r1(v: Double) = (v * 10).roundToInt() / 10.0

    private fun fmtVnd(v: Long): String =
        "%,d".format(Locale.US, v).replace(',', '.') + "đ"

    /** Entity IDs nang luong tren HA nha user (verified 2026-09-30). */
    private object E {
        const val YESTERDAY_IMPORT = "sensor.solis_s6_eh1p_yesterday_energy_imported_from_grid_2"
        const val YESTERDAY_PV = "sensor.solis_s6_eh1p_pv_yesterday_energy_generation_2"
        const val PV_MONTH = "sensor.solis_s6_eh1p_pv_current_month_energy_generation_2"
        const val GRID_COST = "sensor.grid_cost"
        const val HOME_COST = "sensor.home_cost"
        const val GRID_IMPORT_BILLING = "sensor.grid_import_billing"
        const val EVN_MONTHLY = "sensor.aptomat_evn_monthly"
        const val T1_MONTHLY = "sensor.aptomat_t1_energy_monthly"
        const val T2_MONTHLY = "sensor.aptomat_t2_energy_monthly"
        const val T3_MONTHLY = "sensor.aptomat_t3_energy_monthly"
        const val UNIT_PRICE = "sensor.evn_current_unit_price"
    }

    /** Sensor dien nang ngay cua tung thiet bi -> ten hien thi + icon Ms. */
    private val DEVICE_SENSORS = listOf(
        Triple("sensor.dieu_hoa_daily_energy_climatic", "Điều hòa", "ac_unit"),
        Triple("sensor.air_condition_daily_energy_ac", "Điều hòa tầng 2", "ac_unit"),
        Triple("sensor.o_cam_bep_tu_daily_energy_stove", "Bếp từ", "cooking"),
        Triple("sensor.cong_tac_nong_lanh_daily_energy_boiler", "Bình nóng lạnh", "whatshot"),
        Triple("sensor.o_cam_tu_lanh_daily_energy_fridge", "Tủ lạnh", "soup_kitchen"),
        Triple("sensor.o_cam_may_rua_bat_daily_energy_dishwasher", "Máy rửa bát", "dishwasher"),
        Triple("sensor.o_cam_may_say_daily_energy_dryer", "Máy sấy", "local_laundry_service"),
        Triple("sensor.o_cam_phong_giat_daily_energy_washing", "Phòng giặt", "local_laundry_service"),
        Triple("sensor.o_cam_noi_chien_daily_energy_oven", "Nồi chiên", "cooking"),
        Triple("sensor.o_cam_ban_lam_viec_daily_energy_table", "Bàn làm việc", "desk"),
        Triple("sensor.o_cam_ngoai_vi_daily_energy_bicycle", "Ổ cắm ngoại vi", "power"),
        Triple("sensor.o_cam_tuong_phong_ngu_lon_daily_energy_wall", "Ổ cắm tường PN", "power"),
    )
}
