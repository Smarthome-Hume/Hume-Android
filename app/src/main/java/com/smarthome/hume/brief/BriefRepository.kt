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
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlinx.coroutines.CoroutineScope
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
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.json.JSONObject

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
        pruneMonthlyForNewMonth(json.decodeFromString<BriefCache>(cacheFile.readText()))
    }.getOrNull()

    /**
     * Nguyen tac: sang mung 1 xoa ban "da chot" cua thang truoc.
     * Ban thang chi de xem tu ngay 28 den het thang (cung thang lich voi
     * ngay 27 chot ky). Ngoai khoang do -> an di (refreshAll se build lai
     * khi den ky).
     */
    private fun pruneMonthlyForNewMonth(c: BriefCache): BriefCache {
        val m = c.monthly ?: return c
        val today = LocalDate.now(ZoneId.systemDefault())
        val (schS, schE) = scheduledPeriod(today)
        val visible = today.monthValue == schE.monthValue && today.year == schE.year
        if (visible && m.periodKey == "${schS}_${schE}") return c
        val pruned = c.copy(monthly = null)
        runCatching { saveCache(pruned) }
        return pruned
    }

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
        var monthlyLive = _cache.value?.monthlyLive
        val (schS, schE) = scheduledPeriod(today)
        val closedKey = "${schS}_${schE}"
        // Ban theo lich: ky dien da chot (vd thang 9 = 28/8-27/9). Chi giu de
        // xem tu ngay 28 den het thang; sang mung 1 thi xoa (nguyen tac cua
        // user). So sanh bang periodKey de cache cu thang duong lich
        // ("9/2026") khong bi nham la da build.
        val visible = today.monthValue == schE.monthValue && today.year == schE.year
        monthly = if (!visible) {
            null
        } else if (monthly?.periodKey != closedKey) {
            // Ngay 27 dung so lieu live (sensor chua reset), tu ngay 28 dung
            // last_period / lich su tai luc chot.
            buildClosedMonthly(today)
        } else {
            monthly
        }
        // Ban on-demand cua ky dang chay: xoa khi da sang ky moi.
        val (curS, curE) = billingPeriod(today)
        if (monthlyLive?.periodKey != "${curS}_${curE}") {
            monthlyLive = null
        }
        // User chu dong bam "Tao ngay": tong hop ky dang chay, luu RIENG biet.
        if (forceMonthly) {
            monthlyLive = buildLiveMonthly()
        }
        if (daily != null) {
            saveCache(BriefCache(daily = daily, monthly = monthly, monthlyLive = monthlyLive, generatedAtMs = System.currentTimeMillis()))
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

        // Ten thiet bi: uu tien friendly_name tu HA (dong bo voi cac trang khac).
        val nameMap = friendlyNameMap()
        val devices = DEVICE_SENSORS.map { (eid, fallbackName, icon) ->
            async { Triple(nameMap[eid] ?: fallbackName, icon, yesterdayDelta(eid, yesterday)) }
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

    /** Nhan thang cua ky dien theo thang ket thuc, vd ky 28/8-27/9 -> "9/2026". */
    private fun billingLabel(today: LocalDate = LocalDate.now(ZoneId.systemDefault())): String {
        val (_, e) = billingPeriod(today)
        return "${e.monthValue}/${e.year}"
    }

    /** Ky dien lien truoc (da chot). */
    private fun prevBillingPeriod(today: LocalDate): Pair<LocalDate, LocalDate> {
        val (curS, _) = billingPeriod(today)
        return curS.minusMonths(1) to curS.minusDays(1)
    }

    /**
     * Ky dien de tong hop theo lich: ky dang chot neu hom nay la ngay 27
     * (dung so lieu live), nguoc lai la ky vua chot (dung last_period/lich su).
     */
    private fun scheduledPeriod(today: LocalDate): Pair<LocalDate, LocalDate> {
        val (s, e) = billingPeriod(today)
        return if (today.dayOfMonth == 27) s to e else prevBillingPeriod(today)
    }

    /**
     * Tong hop KY DANG CHAY (vd 28/9-27/10): doc truc tiep sensor live.
     * Do user bam "Tao ngay" chu dong tao, luu rieng biet voi ban theo lich.
     */
    private suspend fun buildLiveMonthly(): BriefMonthly? = supervisorScope {
        val today = LocalDate.now(ZoneId.systemDefault())
        val (cycleStart, cycleEnd) = billingPeriod(today)
        buildMonthlyCommon(
            label = billingLabel(today),
            periodKey = "${cycleStart}_${cycleEnd}",
            gridKwh = { getState(E.GRID_IMPORT_BILLING) ?: getState(E.EVN_MONTHLY) },
            costVnd = { getState(E.GRID_COST)?.roundToLong() },
            homeCostVnd = { getState(E.HOME_COST)?.roundToLong() ?: 0L },
            // PV/tang theo ky 28->27: lay tu long-term statistics (khong bi purge).
            pvKwh = { statisticsSums(listOf(E.PV_TOTAL), cycleStart, today)[E.PV_TOTAL] ?: 0.0 },
            floors = {
                val ids = listOf(E.T1_MONTHLY, E.T2_MONTHLY, E.T3_MONTHLY)
                val m = statisticsSums(ids, cycleStart, today)
                listOf("Tầng 1" to E.T1_MONTHLY, "Tầng 2" to E.T2_MONTHLY, "Tầng 3" to E.T3_MONTHLY)
                    .map { (name, eid) -> BriefFloorStat(name, r1(m[eid] ?: 0.0)) }
                    .sortedByDescending { it.kwh }
            },
            dataStart = cycleStart, dataEnd = today,
        )
    }

    /**
     * Tong hop KY DA CHOT theo lich (vd "9/2026" = 28/8-27/9).
     * Neu hom nay van trong ky (chua toi 28, sensor chua reset): doc live.
     * Nguoc lai: dung last_period / lich su tai thoi diem chot ky.
     */
    private suspend fun buildClosedMonthly(today: LocalDate): BriefMonthly? = supervisorScope {
        val (pS, pE) = scheduledPeriod(today)
        val label = "${pE.monthValue}/${pE.year}"
        val zone = ZoneId.systemDefault()
        // Ngay 27: ky dang chot, sensor chua reset -> doc live.
        // Tu ngay 28: ky da chot -> dung last_period / lich su tai luc chot.
        val useLive = today.dayOfMonth == 27
        val pEndMs = pE.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        buildMonthlyCommon(
            label = label,
            periodKey = "${pS}_${pE}",
            gridKwh = {
                if (useLive) getState(E.GRID_IMPORT_BILLING)
                else getAttr(E.GRID_IMPORT_BILLING, "last_period")
                    ?: stateAt(E.GRID_IMPORT_BILLING, pEndMs)
            },
            costVnd = {
                (if (useLive) getState(E.GRID_COST) else stateAt(E.GRID_COST, pEndMs))
                    ?.roundToLong()
            },
            homeCostVnd = {
                (if (useLive) getState(E.HOME_COST) else stateAt(E.HOME_COST, pEndMs))
                    ?.roundToLong() ?: 0L
            },
            pvKwh = { statisticsSums(listOf(E.PV_TOTAL), pS, pE)[E.PV_TOTAL] ?: 0.0 },
            floors = {
                val ids = listOf(E.T1_MONTHLY, E.T2_MONTHLY, E.T3_MONTHLY)
                val m = statisticsSums(ids, pS, pE)
                listOf("Tầng 1" to E.T1_MONTHLY, "Tầng 2" to E.T2_MONTHLY, "Tầng 3" to E.T3_MONTHLY)
                    .map { (name, eid) -> BriefFloorStat(name, r1(m[eid] ?: 0.0)) }
                    .sortedByDescending { it.kwh }
            },
            dataStart = pS, dataEnd = pE,
        )
    }

    /** Khung chung: ghep so lieu thanh BriefMonthly (tinh savingsPct, goi AI). */
    private suspend fun buildMonthlyCommon(
        label: String,
        periodKey: String,
        gridKwh: suspend () -> Double?,
        costVnd: suspend () -> Long?,
        homeCostVnd: suspend () -> Long,
        pvKwh: suspend () -> Double,
        floors: suspend CoroutineScope.() -> List<BriefFloorStat>,
        dataStart: LocalDate, dataEnd: LocalDate,
    ): BriefMonthly? = supervisorScope {
        val gkwh = gridKwh() ?: return@supervisorScope null
        val unitPrice = getState(E.UNIT_PRICE) ?: 2167.0
        val pv = pvKwh()
        // Tien dien: lay truc tiep tu sensor.grid_cost (user xac nhan 2026-09-30:
        // data tien theo ky). Chi tu tinh khi sensor khong co du lieu.
        val cost = costVnd() ?: (gkwh * unitPrice * 1.10).roundToLong()
        // Tien thuc te ca nha tieu thu trong ky (tinh tu sensor.energy_home).
        val homeCost = homeCostVnd()
        // Ti le tiet kiem nho PV: (tien thuc te tieu thu - tien tra EVN) / tien thuc te.
        val savingsPct = if (homeCost > 0) {
            ((homeCost - cost).coerceAtLeast(0).toDouble() / homeCost * 100)
                .roundToInt().coerceIn(0, 100)
        } else 0
        val fl = floors()
        // Top thiet bi ky: DEVICE_SENSORS la sensor daily -> lay tong tu
        // long-term statistics (chinh xac ca khi history da bi purge).
        // Ten: uu tien friendly_name tu HA (dong bo voi cac trang khac).
        val nameMap = friendlyNameMap()
        val statMap = statisticsSums(DEVICE_SENSORS.map { it.first }, dataStart, dataEnd)
        val devices = DEVICE_SENSORS
            .map { (eid, fallbackName, icon) ->
                Triple(nameMap[eid] ?: fallbackName, icon, statMap[eid] ?: 0.0)
            }
            .filter { it.third > 0.5 }
            .sortedByDescending { it.third }
            .take(3)
            .map { BriefDeviceStat(it.first, it.second, (it.third * 10).roundToInt() / 10.0) }

        val summary = aiMonthly(label, gkwh, cost, pv, fl, devices)

        BriefMonthly(
            monthLabel = label,
            periodKey = periodKey,
            gridKwh = r1(gkwh),
            costVnd = cost,
            homeCostVnd = homeCost,
            savingsPct = savingsPct,
            pvKwh = r1(pv),
            savedVnd = (pv * unitPrice).roundToLong(),
            floors = fl,
            topDevices = devices,
            aiSummary = summary,
        )
    }

    // ---------------- HA ----------------

    /** Doc 1 state so; local truoc, remote sau (chi fallback khi loi ket noi). */
    private suspend fun getState(entityId: String): Double? {
        val st = fetchEntityJson(entityId)
            ?.get("state")?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull }
        return st?.toDoubleOrNull()
    }

    /** Doc mot attribute so cua entity (vd "last_period" cua utility_meter). */
    private suspend fun getAttr(entityId: String, attr: String): Double? {
        val v = fetchEntityJson(entityId)
            ?.get("attributes")?.let { it as? kotlinx.serialization.json.JsonObject }
            ?.get(attr)?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull }
        return v?.toDoubleOrNull()
    }

    private suspend fun fetchEntityJson(entityId: String): kotlinx.serialization.json.JsonObject? =
        withContext(Dispatchers.IO) {
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
                        return@withContext json.parseToJsonElement(body).let {
                            it as? kotlinx.serialization.json.JsonObject
                        }
                    }
                } catch (t: Exception) {
                    Log.w(tag, "fetch $entityId via $base failed: ${t.message}")
                    // thu base tiep theo
                }
            }
            null
        }

    /**
     * Map entity_id -> friendly_name (1 request /api/states).
     * Dung de dong bo ten thiet bi trong Brief voi cac trang khac
     * (cac trang lay ten truc tiep tu HA, khong hardcode).
     */
    private suspend fun friendlyNameMap(): Map<String, String> = withContext(Dispatchers.IO) {
        val bases = listOf(localUrl, remoteUrl).map { it.trim().trimEnd('/') }
            .filter { it.isNotBlank() }.distinct()
        for (base in bases) {
            try {
                val req = Request.Builder()
                    .url("$base/api/states")
                    .header("Authorization", "Bearer $token")
                    .build()
                http.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext emptyMap()
                    val body = resp.body?.string().orEmpty()
                    val arr = json.parseToJsonElement(body)
                        as? kotlinx.serialization.json.JsonArray
                        ?: return@withContext emptyMap()
                    return@withContext arr.mapNotNull { el ->
                        val obj = el as? kotlinx.serialization.json.JsonObject
                            ?: return@mapNotNull null
                        val eid = (obj["entity_id"]
                            as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull
                            ?: return@mapNotNull null
                        val fname = (obj["attributes"]
                            as? kotlinx.serialization.json.JsonObject)
                            ?.get("friendly_name")
                            ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull }
                            ?.trim()?.takeIf { it.isNotBlank() }
                            ?: return@mapNotNull null
                        eid to fname
                    }.toMap()
                }
            } catch (t: Exception) {
                Log.w(tag, "friendlyNameMap via $base failed: ${t.message}")
                // thu base tiep theo
            }
        }
        emptyMap()
    }

    /** Gia tri cua sensor tai mot thoi diem (diem lich su gan nhat truoc atMs). */
    private suspend fun stateAt(entityId: String, atMs: Long): Double? {
        ensureHistoryConfigured()
        return try {
            HistoryFetcher.fetchRange(entityId, atMs - 3_600_000L, atMs).lastOrNull()?.value
        } catch (t: Exception) {
            Log.w(tag, "stateAt $entityId failed: ${t.message}")
            null
        }
    }

    /** San luong tang trong 1 ngay: hieu so cuoi - dau cua sensor tich luy. */
    private suspend fun yesterdayDelta(entityId: String, day: LocalDate): Double {
        val zone = ZoneId.systemDefault()
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
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

    /**
     * Tong san luong cac sensor trong [from, to] (bao ca 2 dau) lay tu
     * long-term statistics cua HA (giữ vĩnh viễn, khong bi purge nhu history).
     * Dung cho PV/tang/thiet bi theo ky thanh toan 28->27.
     * Tra ve map entityId -> kWh (chi nhung sensor co du lieu).
     */
    private suspend fun statisticsSums(
        entityIds: List<String>, from: LocalDate, to: LocalDate,
    ): Map<String, Double> {
        if (entityIds.isEmpty()) return emptyMap()
        val zone = ZoneId.systemDefault()
        val fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val startIso = from.atStartOfDay(zone).toOffsetDateTime().format(fmt)
        val endIso = to.plusDays(1).atStartOfDay(zone).toOffsetDateTime().format(fmt)
        val bases = listOf(localUrl, remoteUrl).map { it.trim().trimEnd('/') }
        for (base in bases) {
            val wsUrl = base.replaceFirst(Regex("^http"), "ws") + "/api/websocket"
            val r = wsStatisticsSums(wsUrl, entityIds, startIso, endIso)
            if (r != null) return r
        }
        return emptyMap()
    }

    private suspend fun wsStatisticsSums(
        wsUrl: String, entityIds: List<String>, startIso: String, endIso: String,
    ): Map<String, Double>? = withTimeoutOrNull(20_000) {
        suspendCancellableCoroutine { cont ->
            var settled = false
            fun settle(v: Map<String, Double>?) {
                if (!settled) { settled = true; cont.resume(v) }
            }
            var ws: WebSocket? = null
            val req = Request.Builder().url(wsUrl).build()
            ws = http.newWebSocket(req, object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val o = JSONObject(text)
                        when (o.optString("type")) {
                            "auth_required" -> webSocket.send(
                                JSONObject().put("type", "auth")
                                    .put("access_token", token).toString()
                            )
                            "auth_ok" -> webSocket.send(
                                JSONObject()
                                    .put("id", 1)
                                    .put("type", "recorder/statistics_during_period")
                                    .put("start_time", startIso)
                                    .put("end_time", endIso)
                                    .put("statistic_ids", JSONArray(entityIds))
                                    .put("period", "day")
                                    .put("types", JSONArray().put("change"))
                                    .toString()
                            )
                            "result" -> {
                                val out = mutableMapOf<String, Double>()
                                if (o.optBoolean("success", false)) {
                                    val result = o.optJSONObject("result")
                                    for (eid in entityIds) {
                                        val arr = result?.optJSONArray(eid) ?: continue
                                        var sum = 0.0
                                        var found = false
                                        for (i in 0 until arr.length()) {
                                            val c = arr.optJSONObject(i)?.optDouble("change", Double.NaN)
                                            if (c != null && !c.isNaN() && c >= 0) {
                                                sum += c; found = true
                                            }
                                        }
                                        if (found) out[eid] = sum
                                    }
                                }
                                webSocket.close(1000, "done")
                                settle(out)
                            }
                        }
                    } catch (t: Exception) {
                        Log.w(tag, "ws stats parse: ${t.message}")
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.w(tag, "ws stats $wsUrl: ${t.message}")
                    settle(null)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    settle(null)
                }
            })
            cont.invokeOnCancellation { ws?.close(1000, "cancel") }
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
        const val PV_TOTAL = "sensor.solis_s6_eh1p_pv_total_energy_generation_2"
        const val GRID_COST = "sensor.grid_cost"
        const val HOME_COST = "sensor.home_cost"
        const val GRID_IMPORT_BILLING = "sensor.grid_import_billing"
        const val EVN_MONTHLY = "sensor.aptomat_evn_monthly"
        const val T1_MONTHLY = "sensor.aptomat_t1_energy_monthly"
        const val T2_MONTHLY = "sensor.aptomat_t2_energy_monthly"
        const val T3_MONTHLY = "sensor.aptomat_t3_energy_monthly"
        const val UNIT_PRICE = "sensor.evn_current_unit_price"
    }

    /** Sensor dien nang ngay cua tung thiet bi -> ten hien thi + icon Ms.
     * Ten mac dinh (fallback) dong bo voi friendly_name tren HA 2026-09-30;
     * khi build se uu tien lay friendly_name truc tiep tu HA. */
    private val DEVICE_SENSORS = listOf(
        Triple("sensor.dieu_hoa_daily_energy_climatic", "A/C phòng trẻ em", "ac_unit"),
        Triple("sensor.air_condition_daily_energy_ac", "A/C phòng ngủ chính", "ac_unit"),
        Triple("sensor.o_cam_bep_tu_daily_energy_stove", "Bếp từ", "cooking"),
        Triple("sensor.cong_tac_nong_lanh_daily_energy_boiler", "Nóng lạnh", "whatshot"),
        Triple("sensor.o_cam_tu_lanh_daily_energy_fridge", "Tủ lạnh", "soup_kitchen"),
        Triple("sensor.o_cam_may_rua_bat_daily_energy_dishwasher", "Máy rửa bát", "dishwasher"),
        Triple("sensor.o_cam_may_say_daily_energy_dryer", "Máy sấy", "local_laundry_service"),
        Triple("sensor.o_cam_phong_giat_daily_energy_washing", "Máy giặt", "local_laundry_service"),
        Triple("sensor.o_cam_noi_chien_daily_energy_oven", "Oven", "cooking"),
        Triple("sensor.o_cam_ban_lam_viec_daily_energy_table", "Bàn học", "desk"),
        Triple("sensor.o_cam_ngoai_vi_daily_energy_bicycle", "Sạc xe điện", "power"),
        Triple("sensor.o_cam_tuong_phong_ngu_lon_daily_energy_wall", "Wall", "power"),
    )
}
