package com.smarthome.hume.core.ha

import android.util.Log
import com.smarthome.hume.core.model.HAEntity
import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.core.model.toHomeEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

private const val TAG = "HumeHA"

class HomeAssistantRepository {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var ws: WebSocket? = null
    private var msgId = 1
    private var baseUrl = ""
    private var token = ""

    /** True while the app wants a live socket. Used to decide whether to auto-reconnect. */
    private var wantConnection = false
    private var reconnectAttempt = 0

    private val _entities = MutableStateFlow<Map<String, HomeEntity>>(emptyMap())
    val entities: StateFlow<Map<String, HomeEntity>> = _entities.asStateFlow()

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    /** Last REST / WebSocket / service-call error, or null when everything is healthy. */
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    fun configure(url: String, token: String) {
        this.baseUrl = url.trim().trimEnd('/')
        this.token = token.trim()
        Log.i(TAG, "Configured HA baseUrl=$baseUrl tokenLength=${this.token.length}")
    }

    fun connect() {
        if (baseUrl.isBlank() || token.isBlank()) {
            Log.w(TAG, "connect() skipped: baseUrl or token is empty")
            return
        }
        wantConnection = true
        reconnectAttempt = 0
        closeSocket()
        scope.launch { fetchInitialStates() }
        openSocket()
    }

    fun disconnect() {
        wantConnection = false
        closeSocket()
    }

    private fun closeSocket() {
        ws?.close(1000, "bye")
        ws = null
        _connected.value = false
    }

    private fun openSocket() {
        val wsUrl = baseUrl
            .replaceFirst("http://", "ws://")
            .replaceFirst("https://", "wss://") + "/api/websocket"
        Log.i(TAG, "WebSocket connecting to $wsUrl")
        ws = client.newWebSocket(Request.Builder().url(wsUrl).build(), listener)
    }

    private fun scheduleReconnect(reason: String) {
        _connected.value = false
        if (!wantConnection) return
        val attempt = ++reconnectAttempt
        val delayMs = minOf(30_000L, 2_000L * attempt)
        Log.w(TAG, "WebSocket down ($reason). Reconnecting in ${delayMs}ms, attempt $attempt")
        scope.launch {
            delay(delayMs)
            if (!wantConnection) return@launch
            // Refresh over REST too, so the UI stays correct even if the socket keeps failing.
            fetchInitialStates()
            openSocket()
        }
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.i(TAG, "WebSocket opened, HTTP ${response.code}")
        }

        override fun onMessage(webSocket: WebSocket, text: String) = handleWs(text)

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket failed: ${t.javaClass.simpleName}: ${t.message}", t)
            _lastError.value = "WebSocket: ${t.message}"
            scheduleReconnect("failure")
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.w(TAG, "WebSocket closed: $code $reason")
            if (code != 1000) scheduleReconnect("closed $code")
        }
    }

    private fun handleWs(text: String) {
        val obj = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return
        when (obj["type"]?.jsonPrimitive?.contentOrNull) {
            "auth_required" -> {
                Log.i(TAG, "WebSocket auth_required, sending token")
                ws?.send("""{"type":"auth","access_token":"$token"}""")
            }
            "auth_ok" -> {
                Log.i(TAG, "WebSocket auth_ok, subscribing to state_changed")
                reconnectAttempt = 0
                _connected.value = true
                _lastError.value = null
                ws?.send("""{"id":${++msgId},"type":"subscribe_events","event_type":"state_changed"}""")
            }
            "auth_invalid" -> {
                Log.e(TAG, "WebSocket auth_invalid: token rejected by Home Assistant")
                _lastError.value = "WebSocket: token bi tu choi (auth_invalid)"
                _connected.value = false
            }
            "event" -> {
                val newState = obj["event"]?.jsonObject?.get("data")?.jsonObject?.get("new_state") ?: return
                val ent = runCatching { json.decodeFromJsonElement(HAEntity.serializer(), newState) }.getOrNull() ?: return
                _entities.value = _entities.value + (ent.entityId to ent.toHomeEntity())
            }
        }
    }

    /** Reload every entity over REST. Safe to call at any time. */
    fun refresh() {
        scope.launch { fetchInitialStates() }
    }

    suspend fun fetchInitialStates() = withContext(Dispatchers.IO) {
        val url = "$baseUrl/api/states"
        try {
            val req = Request.Builder().url(url).header("Authorization", "Bearer $token").build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                Log.i(TAG, "GET /api/states -> HTTP ${resp.code}, ${body.length} bytes")
                if (!resp.isSuccessful) {
                    _lastError.value = "REST ${resp.code}: ${body.take(200)}"
                    return@use
                }
                val arr = json.decodeFromString<List<HAEntity>>(body)
                _entities.value = arr.associate { it.entityId to it.toHomeEntity() }
                _lastError.value = null
                Log.i(TAG, "Loaded ${arr.size} entities")
            }
        } catch (t: Throwable) {
            Log.e(TAG, "fetchInitialStates failed for $url", t)
            _lastError.value = "REST: ${t.message}"
        }
    }

    /**
     * Call a Home Assistant service.
     *
     * [optimisticEntityId] lets the UI flip immediately instead of waiting for a
     * state_changed event, which never arrives while the WebSocket is down.
     * A REST refresh always follows so the displayed state converges on reality.
     */
    fun callService(
        domain: String,
        service: String,
        dataJson: String,
        optimisticEntityId: String? = null,
    ) {
        val expectedState = when (service) {
            "turn_on" -> "on"
            "turn_off" -> "off"
            else -> null
        }
        if (optimisticEntityId != null && expectedState != null) {
            applyLocalState(optimisticEntityId, expectedState)
        }
        scope.launch {
            val url = "$baseUrl/api/services/$domain/$service"
            try {
                val body = dataJson.toRequestBody("application/json".toMediaType())
                val req = Request.Builder().url(url)
                    .header("Authorization", "Bearer $token")
                    .post(body)
                    .build()
                client.newCall(req).execute().use { resp ->
                    val text = resp.body?.string().orEmpty()
                    Log.i(TAG, "POST /api/services/$domain/$service -> HTTP ${resp.code}, body=${text.take(200)}")
                    if (!resp.isSuccessful) {
                        _lastError.value = "Service ${resp.code}: ${text.take(200)}"
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "callService failed for $url", t)
                _lastError.value = "Service: ${t.message}"
            }
            // Give Home Assistant a moment to apply the change, then resync.
            delay(400)
            fetchInitialStates()
        }
    }

    private fun applyLocalState(entityId: String, state: String) {
        val current = _entities.value[entityId] ?: return
        _entities.value = _entities.value + (entityId to current.copy(state = state))
    }
}
