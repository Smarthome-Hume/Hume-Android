package com.smarthome.hume.core.ha

import android.util.Log
import com.smarthome.hume.core.model.HAEntity
import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.core.model.toHomeEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class HomeAssistantRepository {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var ws: WebSocket? = null
    private var msgId = 1
    private var baseUrl = ""
    private var token = ""

    private val _entities = MutableStateFlow<Map<String, HomeEntity>>(emptyMap())
    val entities: StateFlow<Map<String, HomeEntity>> = _entities.asStateFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val _status = MutableStateFlow("Chưa kết nối")
    val status: StateFlow<String> = _status.asStateFlow()

    fun configure(url: String, token: String) {
        baseUrl = normalizeBaseUrl(url)
        this.token = token.trim()
    }

    fun connect() {
        if (baseUrl.isBlank() || token.isBlank()) {
            _status.value = "Thiếu URL hoặc token"
            return
        }

        disconnect()
        _status.value = "Đang tải entities..."

        // Load REST states immediately. Do not wait for WebSocket auth_ok, because
        // WS can be blocked by proxy/LAN while REST still works.
        scope.launch { fetchInitialStates() }

        val wsUrl = baseUrl
            .replaceFirst("http://", "ws://")
            .replaceFirst("https://", "wss://") + "/api/websocket"
        ws = client.newWebSocket(Request.Builder().url(wsUrl).build(), listener)
    }

    fun disconnect() {
        ws?.close(1000, "bye")
        ws = null
        _connected.value = false
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket opened")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d(TAG, "WS ${text.take(300)}")
            handleWs(text)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket failed", t)
            _connected.value = false
            if (_entities.value.isEmpty()) _status.value = "WebSocket lỗi: ${t.message ?: "unknown"}"
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed: $code $reason")
            _connected.value = false
        }
    }

    private fun handleWs(text: String) {
        val obj = runCatching { json.parseToJsonElement(text).jsonObject }
            .onFailure { Log.e(TAG, "WS JSON parse failed", it) }
            .getOrNull() ?: return

        when (obj["type"]?.jsonPrimitive?.contentOrNull) {
            "auth_required" -> ws?.send("""{"type":"auth","access_token":"$token"}""")
            "auth_ok" -> {
                _connected.value = true
                _status.value = "Realtime đã kết nối"
                ws?.send("""{"id":${++msgId},"type":"subscribe_events","event_type":"state_changed"}""")
                scope.launch { fetchInitialStates() }
            }
            "auth_invalid" -> {
                _connected.value = false
                _status.value = "Token Home Assistant không hợp lệ"
            }
            "event" -> {
                val newState = obj["event"]?.jsonObject
                    ?.get("data")?.jsonObject
                    ?.get("new_state") ?: return
                val ent = runCatching { json.decodeFromJsonElement(HAEntity.serializer(), newState) }
                    .onFailure { Log.e(TAG, "Decode WS entity failed", it) }
                    .getOrNull() ?: return
                _entities.value = _entities.value + (ent.entityId to ent.toHomeEntity())
            }
        }
    }

    suspend fun fetchInitialStates() {
        try {
            val currentBase = baseUrl
            val currentToken = token
            val req = Request.Builder()
                .url("$currentBase/api/states")
                .header("Authorization", "Bearer $currentToken")
                .header("Content-Type", "application/json")
                .build()

            client.newCall(req).execute().use { response ->
                val body = response.body?.string().orEmpty()
                Log.d(TAG, "GET /api/states -> HTTP ${response.code}, body=${body.take(300)}")

                if (!response.isSuccessful) {
                    _status.value = "Không tải được entities: HTTP ${response.code}"
                    return
                }

                val arr = runCatching { json.decodeFromString<List<HAEntity>>(body) }
                    .onFailure {
                        Log.e(TAG, "Decode /api/states failed", it)
                        _status.value = "Lỗi đọc dữ liệu entities: ${it.message ?: "parse failed"}"
                    }
                    .getOrNull() ?: return

                _entities.value = arr.associate { it.entityId to it.toHomeEntity() }
                _status.value = "Đã tải ${arr.size} entities"
                Log.d(TAG, "Loaded entities=${arr.size}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchInitialStates failed", e)
            _status.value = "Lỗi kết nối Home Assistant: ${e.message ?: e.javaClass.simpleName}"
        }
    }

    fun callService(domain: String, service: String, dataJson: String) {
        scope.launch {
            try {
                val body = dataJson.toRequestBody("application/json".toMediaType())
                val req = Request.Builder()
                    .url("$baseUrl/api/services/$domain/$service")
                    .header("Authorization", "Bearer $token")
                    .header("Content-Type", "application/json")
                    .post(body)
                    .build()
                client.newCall(req).execute().use { response ->
                    Log.d(TAG, "POST /api/services/$domain/$service -> HTTP ${response.code}")
                    if (!response.isSuccessful) _status.value = "Service lỗi: HTTP ${response.code}"
                }
            } catch (e: Exception) {
                Log.e(TAG, "callService failed", e)
                _status.value = "Service lỗi: ${e.message ?: e.javaClass.simpleName}"
            }
        }
    }

    private fun normalizeBaseUrl(input: String): String {
        val trimmed = input.trim().trimEnd('/')
        if (trimmed.isBlank()) return ""
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "http://$trimmed"
    }

    companion object {
        private const val TAG = "HumeHA"
    }
}
