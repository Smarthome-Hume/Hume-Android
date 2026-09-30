package com.smarthome.hume.core.ha

import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** App dang noi HA qua duong nao (de hien thi / debug). */
enum class HaEndpointMode { LOCAL, REMOTE }

/**
 * Chon duong noi Home Assistant theo 2 phuong an song song:
 * - LOCAL: http://192.168.102.22:8123 qua WireGuard/WiFi nha (nhanh, khong qua internet).
 * - REMOTE: https://haiha93.xyz qua Cloudflare (khong can VPN).
 *
 * resolve(): probe local voi timeout ngan; toi duoc thi dung local,
 * khong thi rot ve remote. Ket qua duoc cache 60s.
 * invalidate(): goi khi doi mang / bat-tat VPN / that bai ket noi
 * de lan resolve ke tiep probe lai tu dau.
 */
class HaEndpointResolver {

    @Volatile var localUrl: String = ""
    @Volatile var remoteUrl: String = ""

    private val probeClient = OkHttpClient.Builder()
        .connectTimeout(1_500, TimeUnit.MILLISECONDS)
        .readTimeout(1_500, TimeUnit.MILLISECONDS)
        .callTimeout(3_000, TimeUnit.MILLISECONDS)
        .build()

    private val mutex = Mutex()
    @Volatile private var cachedBase = ""
    @Volatile private var cachedAt = 0L

    private val _mode = MutableStateFlow<HaEndpointMode?>(null)
    val mode: StateFlow<HaEndpointMode?> = _mode.asStateFlow()

    /** Base URL gan nhat da resolve duoc (local mac dinh neu chua resolve). */
    val currentBaseUrl: String
        get() = cachedBase.ifBlank { localUrl.ifBlank { remoteUrl } }

    val currentMode: HaEndpointMode? get() = _mode.value

    fun configure(local: String, remote: String) {
        val l = local.trim().trimEnd('/')
        val r = remote.trim().trimEnd('/')
        if (l != localUrl || r != remoteUrl) {
            localUrl = l
            remoteUrl = r
            invalidate()
            Log.i(TAG, "Configured local=$l remote=$r")
        }
    }

    fun invalidate() {
        cachedBase = ""
        cachedAt = 0L
    }

    suspend fun resolve(): String = mutex.withLock {
        val now = SystemClock.elapsedRealtime()
        if (cachedBase.isNotBlank() && now - cachedAt < CACHE_TTL_MS) return cachedBase
        val local = localUrl
        if (local.isNotBlank() && probeOk(local)) {
            cachedBase = local
            cachedAt = now
            _mode.value = HaEndpointMode.LOCAL
            Log.i(TAG, "resolve -> LOCAL $local")
            return local
        }
        val remote = remoteUrl
        cachedBase = remote
        cachedAt = now
        _mode.value = if (remote.isNotBlank()) HaEndpointMode.REMOTE else null
        Log.i(TAG, "resolve -> REMOTE $remote (local khong toi duoc)")
        remote
    }

    /**
     * GET /api/ khong can auth: 200 ("API running") hoac 401 deu chung to
     * duong truyen toi duoc may chu.
     */
    private fun probeOk(base: String): Boolean = runCatching {
        val req = Request.Builder().url(base.trimEnd('/') + "/api/").get().build()
        probeClient.newCall(req).execute().use { it.code == 200 || it.code == 401 }
    }.getOrDefault(false)

    companion object {
        private const val TAG = "HaEndpoint"
        private const val CACHE_TTL_MS = 60_000L

        fun wsUrl(base: String): String = base
            .replaceFirst("http://", "ws://")
            .replaceFirst("https://", "wss://") + "/api/websocket"
    }
}
