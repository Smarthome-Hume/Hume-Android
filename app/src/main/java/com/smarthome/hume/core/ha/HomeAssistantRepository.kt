package com.smarthome.hume.core.ha

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
    private val client = OkHttpClient.Builder().pingInterval(30, TimeUnit.SECONDS).build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var ws: WebSocket? = null
    private var msgId = 1
    private var baseUrl = ""
    private var token = ""

    private val _entities = MutableStateFlow<Map<String, HomeEntity>>(emptyMap())
    val entities: StateFlow<Map<String, HomeEntity>> = _entities.asStateFlow()
    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    fun configure(url: String, token: String) { baseUrl = url.trimEnd('/'); this.token = token }
    fun connect() {
        if (baseUrl.isBlank() || token.isBlank()) return
        disconnect()
        val wsUrl = baseUrl.replaceFirst("http://", "ws://").replaceFirst("https://", "wss://") + "/api/websocket"
        ws = client.newWebSocket(Request.Builder().url(wsUrl).build(), listener)
    }
    fun disconnect() { ws?.close(1000, "bye"); ws = null; _connected.value = false }

    private val listener = object : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) { handleWs(text) }
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { _connected.value = false }
        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { _connected.value = false }
    }

    private fun handleWs(text: String) {
        val obj = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return
        when (obj["type"]?.jsonPrimitive?.contentOrNull) {
            "auth_required" -> ws?.send("""{"type":"auth","access_token":"$token"}""")
            "auth_ok" -> {
                _connected.value = true
                ws?.send("""{"id":${++msgId},"type":"subscribe_events","event_type":"state_changed"}""")
                scope.launch { fetchInitialStates() }
            }
            "auth_invalid" -> _connected.value = false
            "event" -> {
                val newState = obj["event"]?.jsonObject?.get("data")?.jsonObject?.get("new_state") ?: return
                val ent = runCatching { json.decodeFromJsonElement(HAEntity.serializer(), newState) }.getOrNull() ?: return
                _entities.value = _entities.value + (ent.entityId to ent.toHomeEntity())
            }
        }
    }

    suspend fun fetchInitialStates() {
        val req = Request.Builder().url("$baseUrl/api/states").header("Authorization", "Bearer $token").build()
        val body = client.newCall(req).execute().body?.string() ?: return
        val arr = runCatching { json.decodeFromString<List<HAEntity>>(body) }.getOrDefault(emptyList())
        _entities.value = arr.associate { it.entityId to it.toHomeEntity() }
    }

    fun callService(domain: String, service: String, dataJson: String) {
        scope.launch {
            val body = dataJson.toRequestBody("application/json".toMediaType())
            val req = Request.Builder().url("$baseUrl/api/services/$domain/$service").header("Authorization", "Bearer $token").post(body).build()
            client.newCall(req).execute().close()
        }
    }
}
