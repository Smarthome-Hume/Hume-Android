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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.time.Instant
import java.util.concurrent.TimeUnit

private const val TAG = "HumeHA"

/** One point of /api/history/period, used by ChartDialog. */
data class HistoryPoint(val timeMs: Long, val value: Double)

/** Domains whose changes users expect to see instantly when the entity is on screen. */
private val INTERACTIVE_DOMAINS = setOf(
    "light", "switch", "lock", "cover", "fan", "climate", "media_player",
    "alarm_control_panel", "binary_sensor", "input_boolean", "scene", "script", "automation",
)

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

    /** Entity registry and area registry, loaded once per WebSocket session. */
    private val _registry = MutableStateFlow<Map<String, RegistryEntry>>(emptyMap())
    val registry: StateFlow<Map<String, RegistryEntry>> = _registry.asStateFlow()

    private val _areas = MutableStateFlow<Map<String, String>>(emptyMap())
    val areas: StateFlow<Map<String, String>> = _areas.asStateFlow()

    /** Entity IDs currently visible in the UI. Only these get realtime treatment. */
    private val _watched = MutableStateFlow<Set<String>>(emptySet())
    val watchedEntityIds: StateFlow<Set<String>> = _watched.asStateFlow()

    private val bucketOverrides = HashMap<String, UpdateBucket>()
    private val pending = HashMap<String, HomeEntity>()
    private val lastFlush = HashMap<UpdateBucket, Long>()
    private val registryRequests = HashMap<Int, String>()

    /** Set by HumeApplication so numeric readings land in the local sensor database. */
    var sensorSink: ((String, Double, Long) -> Unit)? = null

    init {
        scope.launch {
            while (true) {
                delay(1_000)
                flushDueBuckets()
            }
        }
    }

    /* ---------------- configuration and lifecycle ---------------- */

    fun configure(url: String, token: String) {
        this.baseUrl = url.trim().trimEnd('/')
        this.token = token.trim()
        Log.i(TAG, "Configured HA baseUrl=$baseUrl tokenLength=${this.token.length}")
    }

    private val isConfigured: Boolean
        get() = baseUrl.isNotBlank() && token.isNotBlank()

    fun connect() {
        if (!isConfigured) {
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

    /** Called from MainActivity's lifecycle observer when the app becomes visible. */
    fun onAppForeground() {
        if (!isConfigured) return
        wantConnection = true
        reconnectAttempt = 0
        scope.launch { fetchInitialStates() }
        if (ws == null) openSocket()
    }

    /** Called when the app is no longer visible: drop the socket instead of burning battery. */
    fun onAppBackground() {
        wantConnection = false
        closeSocket()
        Log.i(TAG, "App backgrounded, WebSocket released")
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

    /* ---------------- watched entities and update buckets ---------------- */

    /** Tell the repository which entities are on screen right now. */
    fun setWatchedEntities(ids: Set<String>) {
        _watched.value = ids
        // Anything already queued for a watched entity should show up immediately.
        val ready = HashMap<String, HomeEntity>()
        synchronized(pending) {
            ids.forEach { id -> pending.remove(id)?.let { ready[id] = it } }
        }
        if (ready.isNotEmpty()) applyUpdates(ready)
    }

    fun watchEntity(entityId: String) = setWatchedEntities(_watched.value + entityId)

    fun unwatchEntity(entityId: String) = setWatchedEntities(_watched.value - entityId)

    /** Pin one entity to a specific bucket, e.g. a slow outdoor sensor at ONE_HOUR. */
    fun setBucket(entityId: String, bucket: UpdateBucket) {
        bucketOverrides[entityId] = bucket
    }

    fun bucketFor(entityId: String): UpdateBucket {
        bucketOverrides[entityId]?.let { return it }
        val watched = _watched.value.contains(entityId)
        val domain = entityId.substringBefore('.')
        return when {
            watched && domain in INTERACTIVE_DOMAINS -> UpdateBucket.REALTIME
            watched -> UpdateBucket.TEN_SECONDS
            domain in INTERACTIVE_DOMAINS -> UpdateBucket.THIRTY_SECONDS
            else -> UpdateBucket.FIVE_MINUTES
        }
    }

    private fun onIncomingState(entity: HomeEntity) {
        if (bucketFor(entity.id) == UpdateBucket.REALTIME) {
            applyUpdates(mapOf(entity.id to entity))
        } else {
            synchronized(pending) { pending[entity.id] = entity }
        }
    }

    private fun flushDueBuckets() {
        val now = System.currentTimeMillis()
        val ready = HashMap<String, HomeEntity>()
        synchronized(pending) {
            if (pending.isEmpty()) return
            val iterator = pending.entries.iterator()
            val touched = HashSet<UpdateBucket>()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val bucket = bucketFor(entry.key)
                val last = lastFlush[bucket] ?: 0L
                if (now - last >= bucket.intervalMs) {
                    ready[entry.key] = entry.value
                    touched += bucket
                    iterator.remove()
                }
            }
            touched.forEach { lastFlush[it] = now }
        }
        if (ready.isNotEmpty()) applyUpdates(ready)
    }

    private fun applyUpdates(updates: Map<String, HomeEntity>) {
        _entities.value = _entities.value + updates
        val sink = sensorSink ?: return
        val watched = _watched.value
        updates.values.forEach { entity ->
            val value = entity.numericState
            if (value != null && watched.contains(entity.id)) {
                sink(entity.id, value, System.currentTimeMillis())
            }
        }
    }

    /* ---------------- WebSocket ---------------- */

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
                requestRegistries()
            }
            "auth_invalid" -> {
                Log.e(TAG, "WebSocket auth_invalid: token rejected by Home Assistant")
                _lastError.value = "WebSocket: token bi tu choi (auth_invalid)"
                _connected.value = false
            }
            "event" -> {
                val newState = obj["event"]?.jsonObject?.get("data")?.jsonObject?.get("new_state") ?: return
                val ent = runCatching { json.decodeFromJsonElement(HAEntity.serializer(), newState) }.getOrNull() ?: return
                onIncomingState(ent.toHomeEntity())
            }
            "result" -> {
                val id = obj["id"]?.jsonPrimitive?.intOrNull ?: return
                when (registryRequests.remove(id)) {
                    "entities" -> parseEntityRegistry(obj["result"])
                    "areas" -> parseAreaRegistry(obj["result"])
                }
            }
        }
    }

    private fun requestRegistries() {
        val areaId = ++msgId
        registryRequests[areaId] = "areas"
        ws?.send("""{"id":$areaId,"type":"config/area_registry/list"}""")
        val entityId = ++msgId
        registryRequests[entityId] = "entities"
        ws?.send("""{"id":$entityId,"type":"config/entity_registry/list"}""")
    }

    private fun parseEntityRegistry(element: JsonElement?) {
        val array = element as? JsonArray ?: return
        val map = HashMap<String, RegistryEntry>(array.size)
        array.forEach { item ->
            val row = runCatching { item.jsonObject }.getOrNull() ?: return@forEach
            val entityId = row["entity_id"]?.jsonPrimitive?.contentOrNull ?: return@forEach
            map[entityId] = RegistryEntry(
                entityId = entityId,
                name = row["name"]?.jsonPrimitive?.contentOrNull
                    ?: row["original_name"]?.jsonPrimitive?.contentOrNull,
                areaId = row["area_id"]?.jsonPrimitive?.contentOrNull,
                deviceId = row["device_id"]?.jsonPrimitive?.contentOrNull,
                platform = row["platform"]?.jsonPrimitive?.contentOrNull,
            )
        }
        _registry.value = map
        Log.i(TAG, "Entity registry loaded: ${map.size} entries")
    }

    private fun parseAreaRegistry(element: JsonElement?) {
        val array = element as? JsonArray ?: return
        val map = HashMap<String, String>(array.size)
        array.forEach { item ->
            val row = runCatching { item.jsonObject }.getOrNull() ?: return@forEach
            val id = row["area_id"]?.jsonPrimitive?.contentOrNull ?: return@forEach
            val name = row["name"]?.jsonPrimitive?.contentOrNull ?: return@forEach
            map[id] = name
        }
        _areas.value = map
        Log.i(TAG, "Area registry loaded: ${map.size} areas")
    }

    /** Area name for an entity, resolved through the registry. */
    fun areaNameFor(entityId: String): String? {
        val area = _registry.value[entityId]?.areaId ?: return null
        return _areas.value[area]
    }

    /** Entity IDs belonging to one area, useful when building room screens from the registry. */
    fun entitiesInArea(areaId: String): List<String> =
        _registry.value.values.filter { it.areaId == areaId }.map { it.entityId }

    /* ---------------- REST ---------------- */

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
     * GET /api/history/period, ported from HomeAssistantManager.swift.
     * Non-numeric states are skipped, so this only makes sense for sensors.
     */
    suspend fun fetchHistory(entityId: String, hours: Int = 24): List<HistoryPoint> = withContext(Dispatchers.IO) {
        val startIso = Instant.ofEpochMilli(System.currentTimeMillis() - hours * 3_600_000L).toString()
        val url = "$baseUrl/api/history/period/$startIso" +
            "?filter_entity_id=$entityId&minimal_response&significant_changes_only"
        try {
            val req = Request.Builder().url(url).header("Authorization", "Bearer $token").build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                Log.i(TAG, "GET /api/history/period ($entityId) -> HTTP ${resp.code}, ${body.length} bytes")
                if (!resp.isSuccessful) return@use emptyList()
                val series = json.parseToJsonElement(body).jsonArray.firstOrNull()?.jsonArray
                    ?: return@use emptyList()
                series.mapNotNull { element ->
                    val row = element.jsonObject
                    val value = row["state"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: return@mapNotNull null
                    val stamp = row["last_changed"]?.jsonPrimitive?.contentOrNull
                        ?: row["last_updated"]?.jsonPrimitive?.contentOrNull
                        ?: return@mapNotNull null
                    val millis = runCatching { Instant.parse(stamp).toEpochMilli() }.getOrNull()
                        ?: return@mapNotNull null
                    HistoryPoint(millis, value)
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "fetchHistory failed for $url", t)
            _lastError.value = "History: ${t.message}"
            emptyList()
        }
    }

    /* ---------------- service helper ---------------- */

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

    private fun serviceData(entityId: String, extra: JsonObjectBuilder.() -> Unit = {}): String =
        buildJsonObject {
            put("entity_id", entityId)
            extra()
        }.toString()

    fun turnOn(entityId: String) =
        callService(entityId.substringBefore('.'), "turn_on", serviceData(entityId), entityId)

    fun turnOff(entityId: String) =
        callService(entityId.substringBefore('.'), "turn_off", serviceData(entityId), entityId)

    fun toggle(entityId: String) =
        callService(entityId.substringBefore('.'), "toggle", serviceData(entityId))

    fun setLightBrightness(entityId: String, percent: Int) =
        callService("light", "turn_on", serviceData(entityId) { put("brightness_pct", percent) }, entityId)

    fun setLightColorTemp(entityId: String, kelvin: Int) =
        callService("light", "turn_on", serviceData(entityId) { put("color_temp_kelvin", kelvin) }, entityId)

    fun setClimateTemperature(entityId: String, temperature: Double) =
        callService("climate", "set_temperature", serviceData(entityId) { put("temperature", temperature) })

    fun setHvacMode(entityId: String, mode: String) =
        callService("climate", "set_hvac_mode", serviceData(entityId) { put("hvac_mode", mode) })

    fun setFanMode(entityId: String, mode: String) =
        callService("climate", "set_fan_mode", serviceData(entityId) { put("fan_mode", mode) })

    fun setCoverPosition(entityId: String, position: Int) =
        callService("cover", "set_cover_position", serviceData(entityId) { put("position", position) })

    fun activateScene(entityId: String) =
        callService("scene", "turn_on", serviceData(entityId))

    fun runScript(entityId: String) =
        callService("script", "turn_on", serviceData(entityId))

    fun pressButton(entityId: String) =
        callService("button", "press", serviceData(entityId))

    fun selectOption(entityId: String, option: String) =
        callService(entityId.substringBefore('.'), "select_option", serviceData(entityId) { put("option", option) })

    fun alarmArm(entityId: String, mode: String, code: String? = null) =
        callService("alarm_control_panel", "alarm_arm_$mode", serviceData(entityId) { if (code != null) put("code", code) })

    fun alarmDisarm(entityId: String, code: String? = null) =
        callService("alarm_control_panel", "alarm_disarm", serviceData(entityId) { if (code != null) put("code", code) })

    private fun applyLocalState(entityId: String, state: String) {
        val current = _entities.value[entityId] ?: return
        _entities.value = _entities.value + (entityId to current.copy(state = state))
    }
}
