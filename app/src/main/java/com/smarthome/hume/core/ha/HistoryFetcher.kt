package com.smarthome.hume.core.ha

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.time.Instant
import java.util.concurrent.TimeUnit

/*
 * SUA LOI "THE NANG LUONG MAT DATA 7 NGAY".
 *
 * Truoc day bieu do goi fetchHistory(entityId, 24*7): MOT request keo ca 168
 * gio ve mot luc, tren client co readTimeout 20s. Voi Home Assistant chay tren
 * may yeu (recorder SQLite nhieu thang du lieu) truy van nay rat nang, thuong
 * qua 20s -> OkHttp huy -> tra ve rong -> ca 6 ngay qua khu deu bang 0.
 *
 * Cach lam moi:
 *  - Hoi TUNG NGAY MOT (start_time + end_time gioi han dung 24h). Bay truy van
 *    nho thay vi mot truy van khong lo; ngay nao loi thi chi mat ngay do.
 *  - readTimeout rieng 60s cho history, khong dung chung voi client realtime.
 *  - Bo `significant_changes_only`: voi bo dem nang luong tang dan, HA co the
 *    coi cac buoc tang la "khong dang ke" va cat mat dinh cuoi ngay.
 *  - Them `no_attributes` de payload nho lai dang ke.
 */
object HistoryFetcher {
    private const val TAG = "HumeHistory"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var baseUrl: String = ""

    @Volatile
    private var token: String = ""

    val isConfigured: Boolean
        get() = baseUrl.isNotBlank() && token.isNotBlank()

    fun configure(url: String, token: String) {
        this.baseUrl = url.trim().trimEnd('/')
        this.token = token.trim()
    }

    /** Lich su cua mot khoang thoi gian bat ky. Rong neu goi that bai. */
    suspend fun fetchRange(entityId: String, startMs: Long, endMs: Long): List<HistoryPoint> =
        withContext(Dispatchers.IO) {
            if (!isConfigured) return@withContext emptyList()
            val start = Instant.ofEpochMilli(startMs).toString()
            val end = URLEncoder.encode(Instant.ofEpochMilli(endMs).toString(), "UTF-8")
            val url = "$baseUrl/api/history/period/$start" +
                "?filter_entity_id=$entityId&end_time=$end&minimal_response&no_attributes"
            try {
                val req = Request.Builder().url(url)
                    .header("Authorization", "Bearer $token")
                    .build()
                client.newCall(req).execute().use { resp ->
                    val body = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        Log.w(TAG, "history $entityId -> HTTP ${resp.code}: ${body.take(160)}")
                        return@use emptyList()
                    }
                    val series = json.parseToJsonElement(body).jsonArray.firstOrNull()?.jsonArray
                        ?: return@use emptyList()
                    series.mapNotNull { element ->
                        val row = element.jsonObject
                        val value = row["state"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
                            ?: return@mapNotNull null
                        val stamp = row["last_changed"]?.jsonPrimitive?.contentOrNull
                            ?: row["last_updated"]?.jsonPrimitive?.contentOrNull
                            ?: return@mapNotNull null
                        val millis = runCatching { Instant.parse(stamp).toEpochMilli() }.getOrNull()
                            ?: return@mapNotNull null
                        HistoryPoint(millis, value)
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "history $entityId failed: ${t.message}")
                emptyList()
            }
        }
}
