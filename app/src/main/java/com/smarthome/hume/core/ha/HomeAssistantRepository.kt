package com.smarthome.hume.core.ha

import android.os.SystemClock
import android.util.Log
import com.smarthome.hume.core.model.HAEntity
import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.core.model.HumeTab
import com.smarthome.hume.core.model.RoomBubbleConfig
import com.smarthome.hume.core.model.toHomeEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
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
import java.io.File
import java.io.IOException
import java.time.Instant
import java.util.concurrent.TimeUnit

private const val TAG = "HumeHA"

/** One point of /api/history/period, used by ChartDialog. */
data class HistoryPoint(val timeMs: Long, val value: Double)

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
    /** Chon duong noi HA: local (WireGuard/WiFi) uu tien, rot ve remote (Cloudflare). */
    val endpoint = HaEndpointResolver()
    private var token = ""

    /** True while the app wants a live socket. Used to decide whether to auto-reconnect. */
    private var wantConnection = false
    private var reconnectAttempt = 0
    private var lastNetSwitchMs = 0L

    private val _entities = MutableStateFlow<Map<String, HomeEntity>>(emptyMap())
    val entities: StateFlow<Map<String, HomeEntity>> = _entities.asStateFlow()

    /**
     * entityUpdated in HomeAssistantManager.swift. One event per entity that
     * really changed, so a card can listen to its own entity instead of
     * recomposing on every change of the whole entity map (EntityReader).
     */
    private val _entityUpdated = MutableSharedFlow<String>(extraBufferCapacity = 256)
    val entityUpdated: SharedFlow<String> = _entityUpdated.asSharedFlow()

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

    /**
     * Label registry: label_id -> the name shown in the Home Assistant UI.
     * Labels are how the user decides which entities a screen is allowed to
     * list, instead of the app guessing from entity_id spelling.
     */
    private val _labels = MutableStateFlow<Map<String, String>>(emptyMap())
    val labels: StateFlow<Map<String, String>> = _labels.asStateFlow()

    /**
     * device_id -> label IDs. Home Assistant does not copy a device label down
     * onto its entities, so a label put on the device has to be resolved here.
     */
    private val _deviceLabels = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val deviceLabels: StateFlow<Map<String, Set<String>>> = _deviceLabels.asStateFlow()

    /**
     * Entity IDs the UI is actually rendering right now. This is the single
     * source of truth for the update policy: an entity in this set is realtime,
     * anything else is frozen for the UI.
     */
    private val _watched = MutableStateFlow<Set<String>>(emptySet())
    val watchedEntityIds: StateFlow<Set<String>> = _watched.asStateFlow()

    /** True while the Sensors sheet is open: it lists everything, so everything is realtime. */
    private val _sensorsSheetOpen = MutableStateFlow(false)
    val sensorsSheetOpen: StateFlow<Boolean> = _sensorsSheetOpen.asStateFlow()

    private val bucketOverrides = HashMap<String, UpdateBucket>()
    private val pending = HashMap<String, HomeEntity>()
    /**
     * (GAP-40) iOS HomeAssistantManager.lastFlushSlot — interval -> slot
     * wall-clock da flush gan nhat. Tick 1s chi flush bucket nao vua qua
     * moc wall-clock (xem bucketSlot), khong phai sau moi intervalMs ke tu
     * lan flush truoc.
     */
    private val lastFlushSlot = HashMap<UpdateBucket, Long>()
    private val registryRequests = HashMap<Int, String>()

    /**
     * (GAP-41c) Port iOS EntityObservation.GroupEntityReader uiThrottle
     * (2.0s) — xem publishRealtime(). Chi ap dung cho sensor.* khong phai
     * control; prefix control copy dung iOS (ke ca number.* la dieu khien).
     */
    private val uiThrottleLock = Any()
    private val lastUiPublish = HashMap<String, Long>()
    private val throttledLatest = HashMap<String, HomeEntity>()
    private val pendingUiThrottle = HashMap<String, Job>()

    /**
     * Coalescing timer. Only alive while the app is in the foreground: the old
     * version ran a while(true) loop for the whole process lifetime, which kept
     * waking the CPU every second even with the screen off.
     */
    private var flushJob: Job? = null

    /**
     * Tab the user is looking at. Kept so callers stay source compatible, but it
     * no longer gates updates: freezing by tab made screens go stale, and the
     * watched set already describes what is on screen.
     */
    private var activeTab: HumeTab = HumeTab.Home

    /**
     * Room bubble currently open. Kept for the same reason as activeTab: opening
     * a room no longer freezes the other rooms.
     */
    private var activeRoomKey: String? = null

    /** entityId -> room keys, built once from RoomBubbleConfig like entityRoomMap. */
    private val entityRoomMap: Map<String, Set<String>> by lazy {
        val map = HashMap<String, MutableSet<String>>()
        RoomBubbleConfig.all.forEach { config ->
            listOfNotNull(config.tempEntity, config.humidityEntity).forEach { id ->
                map.getOrPut(id) { HashSet() }.add(config.key)
            }
            config.devices.forEach { device ->
                map.getOrPut(device.entity) { HashSet() }.add(config.key)
                device.powerEntity?.let { map.getOrPut(it) { HashSet() }.add(config.key) }
            }
        }
        map
    }

    /**
     * Entities the user just acted on, with the moment the optimistic value was
     * written. HomeAssistantManager.swift flips state locally and lets the real
     * event overwrite it later; this map is what stops a stale REST snapshot
     * from undoing that flip in the meantime.
     */
    private val optimisticUntil = HashMap<String, Long>()

    /** Set by HumeApplication so numeric readings land in the local sensor database. */
    var sensorSink: ((String, Double, Long) -> Unit)? = null

    /**
     * (GAP-42) Sensor nhiet do TempAlert dang theo doi — port iOS
     * TempAlertConfig.sensor (HomeAssistantManager.swift:233, bucket
     * .trigger). TempAlertWatcher.start() cap nhat theo cau hinh nguoi
     * dung; mac dinh la cam bien phong ngu lon.
     */
    @Volatile
    var tempAlertSensorId: String = TempAlertConfig.DEFAULT_SENSOR

    /**
     * (GAP-39) Thu muc file noi bo (Application.filesDir), do HumeApplication
     * gan — cung cho voi cach sensorSink duoc gan. Dung de doc/ghi
     * states_snapshot.json (port iOS HomeAssistantManager.statesSnapshotURL).
     * null = chua gan -> seed/snapshot tam tat (khong crash).
     */
    var snapshotDir: File? = null

    /* ---------------- configuration and lifecycle ---------------- */

    fun configure(localUrl: String, remoteUrl: String, token: String) {
        endpoint.configure(localUrl, remoteUrl)
        this.token = token.trim()
        Log.i(TAG, "Configured HA local=${endpoint.localUrl} remote=${endpoint.remoteUrl} tokenLength=${this.token.length}")
    }

    /** Tuong thich nguoc: 1 URL duy nhat (coi la local). */
    fun configure(url: String, token: String) = configure(localUrl = url, remoteUrl = "", token = token)

    private val isConfigured: Boolean
        get() = token.isNotBlank() &&
            (endpoint.localUrl.isNotBlank() || endpoint.remoteUrl.isNotBlank())

    /** Base URL dang dung (de resolve entity_picture tuong doi). */
    fun getBaseUrl(): String = endpoint.currentBaseUrl

    /** True khi da cau hinh nhung chua ket noi (dang ket noi). */
    fun isConnecting(): Boolean = isConfigured && !_connected.value

    fun connect() {
        if (!isConfigured) {
            Log.w(TAG, "connect() skipped: endpoint or token is empty")
            return
        }
        wantConnection = true
        reconnectAttempt = 0
        closeSocket()
        scope.launch {
            val base = endpoint.resolve()
            // (GAP-39) Seed tu states_snapshot.json truoc khi goi network,
            // chi khi _entities dang rong (xem loadStatesSnapshot).
            loadStatesSnapshot()
            fetchInitialStates()
            openSocket(base)
        }
        startFlushLoop()
    }

    fun disconnect() {
        wantConnection = false
        stopFlushLoop()
        closeSocket()
    }

    /** Called from MainActivity's lifecycle observer when the app becomes visible. */
    fun onAppForeground() {
        if (!isConfigured) return
        wantConnection = true
        reconnectAttempt = 0
        scope.launch {
            val base = endpoint.resolve()
            fetchInitialStates()
            if (ws == null) openSocket(base)
        }
        startFlushLoop()
    }

    /** Called when the app is no longer visible: drop the socket instead of burning battery. */
    fun onAppBackground() {
        wantConnection = false
        stopFlushLoop()
        closeSocket()
        Log.i(TAG, "App backgrounded, WebSocket released")
    }

    /**
     * Goi tu ConnectivityManager callback khi doi mang (bat/tat WireGuard, WiFi <-> 4G):
     * probe lai de tu chuyen local <-> remote. Debounce de tranh flap.
     */
    fun noteNetworkChanged() {
        if (!isConfigured || !wantConnection) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastNetSwitchMs < 2_000) return
        lastNetSwitchMs = now
        Log.i(TAG, "Network changed -> re-resolve HA endpoint")
        endpoint.invalidate()
        reconnectAttempt = 0
        closeSocket()
        scope.launch {
            delay(800) // cho VPN/WiFi on dinh truoc khi probe
            if (!wantConnection || !isActive) return@launch
            val base = endpoint.resolve()
            fetchInitialStates()
            openSocket(base)
        }
    }

    private fun startFlushLoop() {
        if (flushJob?.isActive == true) return
        // (GAP-40) iOS startBucketScheduler: khoi tao slot hien tai cua moi
        // bucket de lan flush dau tien roi vao moc wall-clock ke tiep, khong
        // flush ngay lap tuc khi loop start.
        val nowMs = System.currentTimeMillis()
        UpdateBucket.values().forEach { lastFlushSlot[it] = bucketSlot(it, nowMs) }
        flushJob = scope.launch {
            while (isActive) {
                delay(1_000)
                val empty = synchronized(pending) { pending.isEmpty() }
                if (!empty) flushDueBuckets()
            }
        }
    }

    private fun stopFlushLoop() {
        flushJob?.cancel()
        flushJob = null
        // (GAP-41c) iOS GroupEntityReader.cancelPendingUpdates onDisappear:
        // khong con flush trailing khi app ngat ket noi / xuong background.
        synchronized(uiThrottleLock) {
            pendingUiThrottle.values.forEach { it.cancel() }
            pendingUiThrottle.clear()
            throttledLatest.clear()
        }
    }

    private fun closeSocket() {
        ws?.close(1000, "bye")
        ws = null
        _connected.value = false
    }

    private fun openSocket(base: String) {
        val wsUrl = HaEndpointResolver.wsUrl(base)
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
            // Probe lai duong truyen: VPN vua tat -> rot ve remote, VPN vua bat -> ve local.
            endpoint.invalidate()
            val base = endpoint.resolve()
            // Refresh over REST too, so the UI stays correct even if the socket keeps failing.
            fetchInitialStates()
            openSocket(base)
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

    /**
     * Called by SensorsSheet when it opens and closes. The sheet walks every
     * entity in the system, so while it is open everything is realtime; closing
     * it drops those entities straight back to frozen.
     */
    fun setSensorsSheetOpen(open: Boolean) {
        if (open == _sensorsSheetOpen.value) return
        _sensorsSheetOpen.value = open
        Log.i(TAG, "Sensors sheet open -> $open")
        if (open) releaseVisiblePending()
    }

    /**
     * setActiveTab(): kept for source compatibility. Tabs no longer freeze
     * anything, but switching tabs is still a good moment to publish whatever
     * was queued.
     */
    fun setActiveTab(tab: HumeTab) {
        if (tab == activeTab) return
        activeTab = tab
        Log.i(TAG, "Active tab -> $tab")
        releaseVisiblePending()
    }

    /** setActiveRoom(): kept for source compatibility. Opening a room freezes nothing. */
    fun setActiveRoom(key: String?) {
        if (key == activeRoomKey) return
        activeRoomKey = key
        Log.i(TAG, "Active room -> ${key ?: "none"}")
        releaseVisiblePending()
    }

    /** reloadEntitiesFromCache(): publish everything that is now realtime. */
    private fun releaseVisiblePending() {
        val ready = HashMap<String, HomeEntity>()
        synchronized(pending) {
            val iterator = pending.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (bucketFor(entry.key) == UpdateBucket.REALTIME) {
                    ready[entry.key] = entry.value
                    iterator.remove()
                }
            }
        }
        if (ready.isNotEmpty()) applyUpdates(ready)
    }

    /** Pin one entity to a specific bucket, e.g. a slow outdoor sensor at ONE_HOUR. */
    fun setBucket(entityId: String, bucket: UpdateBucket) {
        bucketOverrides[entityId] = bucket
    }

    /**
     * Port 1:1 iOS HomeAssistantManager.updateBucket(for:)
     * (HomeAssistantManager.swift:175-256).
     *
     * Thu tu uu tien (nhanh nhat wins trong cung 1 entity khong xay ra —
     * ham return o nhanh khop dau tien):
     *  1. Sensors sheet mo -> REALTIME (Android, giu nguyen)
     *  2. bucketOverrides -> bucket da pin (Android, giu nguyen)
     *  3. Chua watched -> ONE_DAY (freeze cua Android, giu nguyen)
     *  4. scene.* -> REALTIME (iOS .trigger: khong auto-flush theo slot;
     *     o Android REALTIME tuong duong vi event duoc publish ngay, nhu
     *     iOS dua trigger vao pendingUpdates + liveTrigger)
     *  5. input_number.* -> ONE_HOUR (iOS .h1)
     *  6. sensor.frigate_* -> REALTIME (iOS)
     *  7. 5 sensor rua bat (DishwasherLiveManager.watchedEntities) ->
     *     REALTIME (N-10, giu nguyen)
     *  8. Solis status_string_3 + meter_active_power -> REALTIME
     *     (E6, giu nguyen; iOS cung co 2 nhanh nay)
     *  9. 6 telemetry pin Solis -> FIVE_MINUTES (iOS .s60 = bucket 300s)
     * 10. 16 sensor cong suat exact + suffix _power/_power_2 -> TEN_SECONDS
     * 11. TempAlert sensor dang theo doi -> REALTIME (iOS .trigger, GAP-42)
     *     + _temperature/_humidity + _pv_today_energy_generation_2 +
     *     _today_energy -> THIRTY_MINUTES (iOS .min30)
     * 12. _daily / _energy_daily / _daily_energy / grid_cost / home_cost /
     *     _billing(_2) / grid_import_billing_2 / home_import_billing ->
     *     ONE_HOUR (iOS .h1)
     * 13. sensor.evn_current_unit_price -> TWO_HOURS (iOS .h2)
     * 14. _yesterday_ -> ONE_DAY (iOS .h24)
     * 15. Mac dinh: sensor.* -> TEN_SECONDS (iOS .s10); domain khac ->
     *     REALTIME (iOS: chi sensor/input_number moi vao bucket, con lai
     *     realtime — switch/light/binary_sensor/alarm/climate...).
     *
     * (GAP-42) Nhanh TempAlertConfig.sensor -> trigger cua iOS
     * (HomeAssistantManager.swift:233) nay da port: sensor dang theo doi
     * (TempAlertConfig.DEFAULT_SENSOR mac dinh) ve REALTIME — tuong duong
     * .trigger o Android vi event duoc publish ngay (xem nhanh scene.*).
     * Cac sensor _temperature khac van roi nhanh 11 (THIRTY_MINUTES).
     */
    fun bucketFor(entityId: String): UpdateBucket {
        // Sheet chi tiet cam bien mo: muon realtime het de xem so nhay —
        // uu tien truoc override.
        if (_sensorsSheetOpen.value) return UpdateBucket.REALTIME
        bucketOverrides[entityId]?.let { return it }
        if (!_watched.value.contains(entityId)) return UpdateBucket.ONE_DAY
        // iOS: scene -> trigger (khong auto-flush; chi chay khi scene chay
        // qua WS — o Android tuong duong REALTIME: publish ngay).
        if (entityId.startsWith("scene.")) return UpdateBucket.REALTIME
        // iOS: input_number — automation-managed accumulation -> 1h.
        if (entityId.startsWith("input_number.")) return UpdateBucket.ONE_HOUR
        // iOS: Frigate — security detection, immediate.
        if (entityId.startsWith("sensor.frigate_")) return UpdateBucket.REALTIME
        // (N-10, port iOS HomeAssistantManager.updateBucket) The rua bat:
        // 5 sensor texgio phai REALTIME de nut va so tren the doi theo
        // ngay (khong roi bucket TEN_SECONDS cua sensor.*). 7 entity
        // con lai cua the (switch/number) da REALTIME theo nhanh
        // discrete o cuoi ham, nen khong can liet ke them.
        if (entityId == "sensor.texgio_tgfpcm795b_g_trang_thai" ||
            entityId == "sensor.texgio_tgfpcm795b_g_giai_doan_rua" ||
            entityId == "sensor.texgio_tgfpcm795b_g_thoi_gian_con_lai" ||
            entityId == "sensor.texgio_tgfpcm795b_g_nhiet_do" ||
            entityId == "sensor.texgio_tgfpcm795b_g_chuong_trinh"
        ) return UpdateBucket.REALTIME
        // (2026-10-22, E6 — port iOS 3af820b): trang thai luoi + cong suat
        // meter phai REALTIME; de TEN_SECONDS thi badge Mat dien/Cap dien,
        // switch household<->backup va node Luoi tre toi 10s so voi iOS.
        if (entityId == "sensor.solis_s6_eh1p_status_string_3" ||
            entityId == "sensor.solis_s6_eh1p_meter_active_power"
        ) return UpdateBucket.REALTIME
        // iOS: 5 min — battery health telemetry (doi cham, uu tien UI thap).
        // .s60 cua iOS la bucket 300s (flushBucket case 300 -> bucket60s).
        if (entityId in MIN5_EXACT) return UpdateBucket.FIVE_MINUTES
        // iOS: 10s — cong suat realtime (solar PV, pin, luoi, thiet bi).
        if (entityId in S10_EXACT) return UpdateBucket.TEN_SECONDS
        // iOS: 10s — tat ca sensor _power / _power_2 (aptomat, thiet bi).
        if (entityId.hasSuffix("_power") || entityId.hasSuffix("_power_2")) {
            return UpdateBucket.TEN_SECONDS
        }
        // (GAP-42) iOS HomeAssistantManager.swift:233 — cam bien nhiet do
        // phong dang theo doi (TempAlertConfig.sensor) -> .trigger, de
        // canh bao nong/lanh kip thoi. O Android .trigger tuong duong
        // REALTIME (publish ngay, nhu nhanh scene.* o tren). Dat TRUOC
        // nhanh _temperature chung (THIRTY_MINUTES) de khong bi nuot.
        // tempAlertSensorId do TempAlertWatcher.start() cap nhat theo
        // cau hinh nguoi dung; mac dinh la TempAlertConfig.DEFAULT_SENSOR.
        if (entityId == tempAlertSensorId) return UpdateBucket.REALTIME
        // iOS: nhiet do / do am -> 30 phut.
        if (entityId.contains("_temperature") || entityId.contains("_humidity")) {
            return UpdateBucket.THIRTY_MINUTES
        }
        if (entityId.hasSuffix("_pv_today_energy_generation_2")) {
            return UpdateBucket.THIRTY_MINUTES
        }
        if (entityId.contains("_today_energy")) return UpdateBucket.THIRTY_MINUTES
        // iOS: 1h — daily-energy kWh accumulators, cost sensors, billing.
        if (entityId.hasSuffix("_daily")) return UpdateBucket.ONE_HOUR
        if (entityId.contains("_energy_daily") || entityId.contains("_daily_energy")) {
            return UpdateBucket.ONE_HOUR
        }
        if (entityId == "sensor.grid_cost" || entityId == "sensor.home_cost") {
            return UpdateBucket.ONE_HOUR
        }
        if (entityId.hasSuffix("_billing") || entityId.hasSuffix("_billing_2")) {
            return UpdateBucket.ONE_HOUR
        }
        if (entityId == "sensor.grid_import_billing_2" ||
            entityId == "sensor.home_import_billing"
        ) return UpdateBucket.ONE_HOUR
        // iOS: 2h — EVN price (doi toi da vai lan moi chu ky).
        if (entityId == "sensor.evn_current_unit_price") return UpdateBucket.TWO_HOURS
        // iOS: 24h — yesterday historical data (chi doi luc nua dem).
        if (entityId.contains("_yesterday_")) return UpdateBucket.ONE_DAY
        // iOS: mac dinh sensor -> s10 (truoc day realtime -> moi sensor la
        // deu vao duong nhanh, gay lag); domain khac sensor/input_number
        // (switch/light/...) -> realtime.
        return if (entityId.startsWith("sensor.")) UpdateBucket.TEN_SECONDS
        else UpdateBucket.REALTIME
    }

    private fun onIncomingState(entity: HomeEntity) {
        // Background logging runs for every entity, frozen or not.
        logToSink(entity)
        if (bucketFor(entity.id) != UpdateBucket.REALTIME) {
            synchronized(pending) { pending[entity.id] = entity }
        } else {
            publishRealtime(entity)
        }
    }

    /**
     * (GAP-41c) Port iOS EntityObservation.GroupEntityReader uiThrottle
     * (2.0s): sensor.* khong phai control chi publish toi da 1 lan / 2s;
     * event den trong cua so cho thi giu gia tri MOI NHAT roi flush trailing
     * khi het 2s (giong iOS pendingUiUpdates Task). Cung state cuoi, chi it
     * rebuild hon — khong doi hanh vi hien thi. Entity dieu khien
     * (CONTROL_PREFIXES, gom number.*) va cac domain discrete khac publish
     * ngay lap tuc. logToSink o onIncomingState khong bi throttle (lich su
     * sensor van ghi du).
     */
    private fun publishRealtime(entity: HomeEntity) {
        val id = entity.id
        val throttleable = (id.startsWith("sensor.") || id.startsWith("number.")) &&
            CONTROL_PREFIXES.none { id.startsWith(it) }
        if (!throttleable) {
            applyUpdates(mapOf(id to entity))
            return
        }
        val now = SystemClock.elapsedRealtime()
        val last = synchronized(uiThrottleLock) { lastUiPublish[id] ?: 0L }
        if (now - last >= UI_THROTTLE_MS) {
            synchronized(uiThrottleLock) {
                lastUiPublish[id] = now
                pendingUiThrottle.remove(id)?.cancel()
                throttledLatest.remove(id)
            }
            applyUpdates(mapOf(id to entity))
            return
        }
        synchronized(uiThrottleLock) {
            throttledLatest[id] = entity
            if (pendingUiThrottle.containsKey(id)) return
            val waitMs = UI_THROTTLE_MS - (now - last)
            pendingUiThrottle[id] = scope.launch {
                delay(waitMs)
                val latest = synchronized(uiThrottleLock) {
                    pendingUiThrottle.remove(id)
                    lastUiPublish[id] = SystemClock.elapsedRealtime()
                    throttledLatest.remove(id)
                }
                if (latest != null) applyUpdates(mapOf(latest.id to latest))
            }
        }
    }

    /**
     * Write a numeric reading to the local sensor database even when the entity
     * is frozen for the UI, so history charts and daily snapshots keep filling
     * up for entities no screen is currently rendering.
     *
     * SensorDatabase cham dia, va ham nay duoc goi tu thread cua WebSocket,
     * nen viec ghi phai day sang Dispatchers.IO.
     */
    private fun logToSink(entity: HomeEntity) {
        val sink = sensorSink ?: return
        val value = entity.numericState ?: return
        val stamp = System.currentTimeMillis()
        scope.launch { runCatching { sink(entity.id, value, stamp) } }
    }

    /**
     * (GAP-40) Port iOS HomeAssistantManager.bucketSlot(for:at:) — slot hien
     * tai cua 1 bucket theo wall-clock local: 10s -> :00,:10...; 300s -> moc
     * 5 phut; 3600s -> dau gio; 86400s -> dau ngay. REALTIME khong co slot
     * (khong bao gio vao pending) -> tra 0 phong thu.
     */
    private fun bucketSlot(bucket: UpdateBucket, nowMs: Long): Long {
        if (bucket == UpdateBucket.REALTIME) return 0L
        val zone = java.time.ZoneId.systemDefault()
        val zoned = java.time.Instant.ofEpochMilli(nowMs).atZone(zone)
        if (bucket == UpdateBucket.ONE_DAY) {
            // iOS: startOfDay — so ngay tu epoch theo gio dia phuong.
            return zoned.toLocalDate().toEpochDay()
        }
        val intervalSec = bucket.intervalMs / 1_000L
        return (zoned.toLocalTime().toSecondOfDay() / intervalSec).toLong()
    }

    /**
     * (GAP-40) Port iOS startBucketScheduler/flushBucket: tick 1s chi flush
     * bucket nao vua qua moc wall-clock (slot doi so voi lan flush truoc),
     * khong phai cu sau intervalMs ke tu lan flush truoc (khong drift tich
     * luy nhu truoc). Gia tri moi nhat cua moi entity trong bucket duoc
     * flush; bucket chua qua moc giu nguyen trong pending.
     */
    private fun flushDueBuckets() {
        val nowMs = System.currentTimeMillis()
        val ready = HashMap<String, HomeEntity>()
        synchronized(pending) {
            if (pending.isEmpty()) return
            val iterator = pending.entries.iterator()
            val touched = HashSet<UpdateBucket>()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val bucket = bucketFor(entry.key)
                // REALTIME khong bao gio vao pending; phong thu thi flush ngay.
                if (bucket == UpdateBucket.REALTIME ||
                    lastFlushSlot[bucket] != bucketSlot(bucket, nowMs)
                ) {
                    ready[entry.key] = entry.value
                    touched += bucket
                    iterator.remove()
                }
            }
            touched.forEach { lastFlushSlot[it] = bucketSlot(it, nowMs) }
        }
        if (ready.isNotEmpty()) applyUpdates(ready)
    }

    /**
     * applyBatch() in HomeAssistantManager.swift. Three rules matter here:
     * an update whose state and attributes are unchanged is dropped instead of
     * being republished, a real event always clears the optimistic marker for
     * that entity because reality has now spoken, and every entity that really
     * changed is announced on entityUpdated.
     */
    private fun applyUpdates(updates: Map<String, HomeEntity>) {
        val current = _entities.value
        val changed = HashMap<String, HomeEntity>(updates.size)
        updates.forEach { (id, entity) ->
            val previous = current[id]
            synchronized(optimisticUntil) { optimisticUntil.remove(id) }
            if (previous == null ||
                previous.state != entity.state ||
                previous.attributes != entity.attributes
            ) {
                changed[id] = entity
            }
        }
        if (changed.isEmpty()) return
        _entities.value = current + changed
        changed.keys.forEach { _entityUpdated.tryEmit(it) }
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
                // Build JSON bang builder de token co ky tu dac biet (") cung khong vo JSON.
                val authMsg = buildJsonObject {
                    put("type", "auth")
                    put("access_token", token)
                }.toString()
                ws?.send(authMsg)
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
                // (GAP-38) Port iOS HomeAssistantManager.swift:760 —
                // disconnect(), khong giu retry token hong: wantConnection
                // ve false nen scheduleReconnect/onFailure khong noi lai;
                // chi noi lai khi dang nhap lai / doi token (connect()).
                // onClosed(1000) tu closeSocket() cung khong reconnect.
                disconnect()
            }
            "event" -> {
                // (GAP-41a) Port iOS v2.8 "LOC TRUOC KHI GIAI MA": parse
                // entity_id + state truoc, chi decode full HAEntity
                // (attributes) khi entity critical / Frigate / watched.
                // Entity khong ai xem: lich su sensor chi can id + so nen
                // van ghi qua logToSink; state doi thi van decode day du
                // de _entities khong stale; chi attribute doi (state giong
                // nhau) thi bo qua — applyUpdates cung drop truong hop
                // state+attributes deu khong doi, va iOS drop luon event
                // nay. PumpAlert (binary_sensor) la critical nen khong anh
                // huong.
                val data = obj["event"]?.jsonObject?.get("data")?.jsonObject ?: return
                val entityId = data["entity_id"]?.jsonPrimitive?.contentOrNull ?: return
                val newStateObj = data["new_state"]?.jsonObject ?: return
                val state = newStateObj["state"]?.jsonPrimitive?.contentOrNull ?: return
                val isCritical = entityId.startsWith("alarm_control_panel.") ||
                    entityId.startsWith("binary_sensor.")
                val isFrigate = entityId.startsWith("sensor.frigate_")
                val isWatched = _watched.value.contains(entityId)
                if (isCritical || isFrigate || isWatched) {
                    val ent = runCatching {
                        json.decodeFromJsonElement(HAEntity.serializer(), newStateObj)
                    }.getOrNull() ?: return
                    onIncomingState(ent.toHomeEntity())
                    return
                }
                val cached = _entities.value[entityId]
                if (cached == null || cached.state != state) {
                    val ent = runCatching {
                        json.decodeFromJsonElement(HAEntity.serializer(), newStateObj)
                    }.getOrNull() ?: return
                    onIncomingState(ent.toHomeEntity())
                } else {
                    // State khong doi: chi ghi lich su sensor, khong decode
                    // attributes (attributes cua entity khong ai xem co the
                    // stale toi lan REST snapshot ke — iOS cung drop event).
                    logToSink(HomeEntity(id = entityId, state = state))
                }
            }
            "result" -> {
                val id = obj["id"]?.jsonPrimitive?.intOrNull ?: return
                when (registryRequests.remove(id)) {
                    "entities" -> parseEntityRegistry(obj["result"])
                    "areas" -> parseAreaRegistry(obj["result"])
                    "labels" -> parseLabelRegistry(obj["result"])
                    "devices" -> parseDeviceRegistry(obj["result"])
                }
            }
        }
    }

    /**
     * Four registries are pulled once per session. Labels come before entities
     * only for readability; every parser writes its own StateFlow, so the order
     * the answers arrive in does not matter.
     */
    private fun requestRegistries() {
        val areaId = ++msgId
        registryRequests[areaId] = "areas"
        ws?.send("""{"id":$areaId,"type":"config/area_registry/list"}""")
        val labelId = ++msgId
        registryRequests[labelId] = "labels"
        ws?.send("""{"id":$labelId,"type":"config/label_registry/list"}""")
        val deviceId = ++msgId
        registryRequests[deviceId] = "devices"
        ws?.send("""{"id":$deviceId,"type":"config/device_registry/list"}""")
        val entityId = ++msgId
        registryRequests[entityId] = "entities"
        ws?.send("""{"id":$entityId,"type":"config/entity_registry/list"}""")
    }

    /** Read a JSON array of plain strings, skipping anything that is not one. */
    private fun stringSet(element: JsonElement?): Set<String> {
        val array = element as? JsonArray ?: return emptySet()
        return array.mapNotNull { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull() }.toSet()
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
                labels = stringSet(row["labels"]),
                hiddenBy = row["hidden_by"]?.jsonPrimitive?.contentOrNull,
                disabledBy = row["disabled_by"]?.jsonPrimitive?.contentOrNull,
                entityCategory = row["entity_category"]?.jsonPrimitive?.contentOrNull,
            )
        }
        _registry.value = map
        val labelled = map.values.count { it.labels.isNotEmpty() }
        Log.i(TAG, "Entity registry loaded: ${map.size} entries, $labelled carry a label")
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

    /** config/label_registry/list, available since Home Assistant 2024.4. */
    private fun parseLabelRegistry(element: JsonElement?) {
        val array = element as? JsonArray ?: return
        val map = HashMap<String, String>(array.size)
        array.forEach { item ->
            val row = runCatching { item.jsonObject }.getOrNull() ?: return@forEach
            val id = row["label_id"]?.jsonPrimitive?.contentOrNull ?: return@forEach
            val name = row["name"]?.jsonPrimitive?.contentOrNull ?: return@forEach
            map[id] = name
        }
        _labels.value = map
        Log.i(TAG, "Label registry loaded: ${map.size} labels ${map.values.joinToString(prefix = "[", postfix = "]")}")
    }

    /**
     * config/device_registry/list, kept only for the labels. A label dropped on
     * a device in the Home Assistant UI does not appear on that device's
     * entities, so both sides have to be checked when resolving a label.
     */
    private fun parseDeviceRegistry(element: JsonElement?) {
        val array = element as? JsonArray ?: return
        val map = HashMap<String, Set<String>>()
        array.forEach { item ->
            val row = runCatching { item.jsonObject }.getOrNull() ?: return@forEach
            val id = row["id"]?.jsonPrimitive?.contentOrNull ?: return@forEach
            val labels = stringSet(row["labels"])
            if (labels.isNotEmpty()) map[id] = labels
        }
        _deviceLabels.value = map
        Log.i(TAG, "Device registry loaded: ${map.size} devices carry a label")
    }

    /** Area name for an entity, resolved through the registry. */
    fun areaNameFor(entityId: String): String? {
        val area = _registry.value[entityId]?.areaId ?: return null
        return _areas.value[area]
    }

    /** Entity IDs belonging to one area, useful when building room screens from the registry. */
    fun entitiesInArea(areaId: String): List<String> =
        _registry.value.values.filter { it.areaId == areaId }.map { it.entityId }

    /** Label names on an entity, counting the labels of the device it belongs to. */
    fun labelNamesFor(entityId: String): Set<String> {
        val entry = _registry.value[entityId] ?: return emptySet()
        val names = _labels.value
        val fromDevice = entry.deviceId?.let { _deviceLabels.value[it] }.orEmpty()
        return (entry.labels + fromDevice).mapNotNull { names[it] }.toSet()
    }

    /**
     * Every entity carrying a label, matched on the name shown in the Home
     * Assistant UI and ignoring case.
     *
     * This is how a screen asks Home Assistant which entities it may list,
     * instead of guessing from how an entity_id happens to be spelled. Entities
     * hidden or disabled in Home Assistant are left out even when labelled,
     * because hiding one of two duplicates is exactly how the user says which
     * copy is the real one.
     */
    fun entityIdsWithLabel(labelName: String): Set<String> {
        val wanted = _labels.value.filterValues { it.equals(labelName, ignoreCase = true) }.keys
        if (wanted.isEmpty()) return emptySet()
        val devices = _deviceLabels.value
        return _registry.value.values
            .filter { entry ->
                if (entry.hiddenBy != null || entry.disabledBy != null) return@filter false
                val fromDevice = entry.deviceId?.let { devices[it] }.orEmpty()
                (entry.labels + fromDevice).any { it in wanted }
            }
            .map { it.entityId }
            .toSet()
    }

    /* ---------------- REST ---------------- */

    /** Reload every entity over REST. Safe to call at any time. */
    fun refresh() {
        scope.launch { fetchInitialStates() }
    }

    /**
     * (GAP-39) Port iOS HomeAssistantManager.loadStatesSnapshot() —
     * doc states_snapshot.json (raw /api/states lan truoc) de UI co data
     * ngay khi mo app, khong cho network. Chi seed khi _entities dang
     * rong (tranh de data cu len data tuoi); WS/REST sync de len sau.
     * snapshotDir null (chua duoc HumeApplication gan) -> bo qua, khong crash.
     */
    private fun loadStatesSnapshot() {
        val dir = snapshotDir ?: return
        val file = File(dir, STATES_SNAPSHOT_FILE)
        if (!file.exists()) return
        runCatching {
            val text = file.readText()
            val arr = json.decodeFromString<List<HAEntity>>(text)
            if (arr.isEmpty()) return
            if (_entities.value.isNotEmpty()) return
            _entities.value = arr.associate { it.entityId to it.toHomeEntity() }
            Log.i(TAG, "Seeded ${_entities.value.size} entities from $STATES_SNAPSHOT_FILE")
        }.onFailure { Log.w(TAG, "loadStatesSnapshot failed: ${it.message}") }
    }

    suspend fun fetchInitialStates() = withContext(Dispatchers.IO) {
        val url = "${endpoint.currentBaseUrl}/api/states"
        try {
            val req = Request.Builder().url(url).header("Authorization", "Bearer $token").build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                Log.i(TAG, "GET /api/states -> HTTP ${resp.code}, ${body.length} bytes")
                if (!resp.isSuccessful) {
                    _lastError.value = "REST ${resp.code}: ${body.take(200)}"
                    return@use
                }
                // (GAP-39) Port iOS fetchInitialStates: luu raw JSON
                // /api/states xuong states_snapshot.json (ghi atomic qua
                // file tam + rename) de lan mo app sau seed tu dia.
                snapshotDir?.let { dir ->
                    runCatching {
                        val tmp = File(dir, "$STATES_SNAPSHOT_FILE.tmp")
                        tmp.writeText(body)
                        tmp.renameTo(File(dir, STATES_SNAPSHOT_FILE))
                    }.onFailure { Log.w(TAG, "saveStatesSnapshot failed: ${it.message}") }
                }
                val arr = json.decodeFromString<List<HAEntity>>(body)
                val snapshot = arr.associate { it.entityId to it.toHomeEntity() }
                _entities.value = mergeSnapshot(snapshot)
                _lastError.value = null
                Log.i(TAG, "Loaded ${arr.size} entities")
            }
        } catch (t: Throwable) {
            Log.e(TAG, "fetchInitialStates failed for $url", t)
            _lastError.value = "REST: ${t.message}"
        }
    }

    /**
     * A full REST snapshot is older than an optimistic flip the user just made,
     * so entities inside the optimistic window keep the local value. Everything
     * else takes the server value. Unchanged rows keep their previous instance
     * so Compose can skip them.
     */
    private fun mergeSnapshot(snapshot: Map<String, HomeEntity>): Map<String, HomeEntity> {
        val current = _entities.value
        if (current.isEmpty()) return snapshot
        val now = System.currentTimeMillis()
        val merged = HashMap<String, HomeEntity>(snapshot.size)
        snapshot.forEach { (id, fresh) ->
            val previous = current[id]
            val pendingSince = synchronized(optimisticUntil) { optimisticUntil[id] }
            val keepLocal = previous != null &&
                pendingSince != null &&
                now - pendingSince < OPTIMISTIC_WINDOW_MS &&
                previous.state != fresh.state
            merged[id] = when {
                keepLocal -> previous!!
                previous != null && previous.state == fresh.state &&
                    previous.attributes == fresh.attributes -> previous
                else -> fresh
            }
        }
        return merged
    }

    /** Re-read a single entity instead of pulling the whole state table. */
    private suspend fun fetchEntityState(entityId: String) = withContext(Dispatchers.IO) {
        val url = "${endpoint.currentBaseUrl}/api/states/$entityId"
        try {
            val req = Request.Builder().url(url).header("Authorization", "Bearer $token").build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use
                val body = resp.body?.string().orEmpty()
                val entity = json.decodeFromString(HAEntity.serializer(), body).toHomeEntity()
                synchronized(optimisticUntil) { optimisticUntil.remove(entityId) }
                applyUpdates(mapOf(entity.id to entity))
            }
        } catch (t: Throwable) {
            Log.e(TAG, "fetchEntityState failed for $url", t)
        }
    }

    /**
     * (N-09) 12 entity the rua bat — 1 nguon su that duy nhat cho refetch
     * sau lenh, port iOS DishwasherLiveManager.cardEntities (5 sensor +
     * 7 switch/number). Cung danh sach voi AppHomeRepository (watched) va
     * RoomExpandedCards.DishwasherPrograms.cardEntityIds.
     */
    val dishwasherEntityIds: Set<String> = setOf(
        "sensor.texgio_tgfpcm795b_g_trang_thai",
        "sensor.texgio_tgfpcm795b_g_giai_doan_rua",
        "sensor.texgio_tgfpcm795b_g_thoi_gian_con_lai",
        "sensor.texgio_tgfpcm795b_g_nhiet_do",
        "sensor.texgio_tgfpcm795b_g_chuong_trinh",
        "switch.texgio_tgfpcm795b_g_may_rua_bat",
        "switch.texgio_tgfpcm795b_g_bat_dau",
        "switch.texgio_tgfpcm795b_g_hoi_nuoc",
        "switch.texgio_tgfpcm795b_g_chong_dia",
        "switch.texgio_tgfpcm795b_g_khoa_tre_em",
        "number.texgio_tgfpcm795b_g_hen_gio",
        "number.texgio_tgfpcm795b_g_nuoc_mem",
    )

    /**
     * (N-09) Port iOS HomeAssistantManager.refreshStates(ids:) — GET
     * /api/states, loc dung 12 entity rua bat roi merge. applyUpdates chi
     * phat entityUpdated cho entity doi state/attributes (nhu iOS: chi
     * send khi prev != state), entity khong doi giu nguyen instance.
     */
    suspend fun refreshDishwasherStates() = withContext(Dispatchers.IO) {
        val url = "${endpoint.currentBaseUrl}/api/states"
        try {
            val req = Request.Builder().url(url).header("Authorization", "Bearer $token").build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use
                val body = resp.body?.string().orEmpty()
                val arr = json.decodeFromString<List<HAEntity>>(body)
                val fresh = arr.filter { it.entityId in dishwasherEntityIds }
                    .associate { it.entityId to it.toHomeEntity() }
                if (fresh.isNotEmpty()) applyUpdates(fresh)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "refreshDishwasherStates failed for $url", t)
        }
    }

    /**
     * (N-09) Port iOS RoomExpandedCards.refreshAfterCommand() — fetch lai
     * state that o 1s + 4s sau moi lenh rua bat, chay nen tren scope IO,
     * khong block UI.
     */
    fun refreshDishwasherAfterCommand() {
        scope.launch {
            delay(1_000)
            refreshDishwasherStates()
            delay(3_000)
            refreshDishwasherStates()
        }
    }

    /**
     * GET /api/history/period, ported from HomeAssistantManager.swift.
     * Non-numeric states are skipped, so this only makes sense for sensors.
     */
    suspend fun fetchHistory(entityId: String, hours: Int = 24): List<HistoryPoint> = withContext(Dispatchers.IO) {
        val startIso = Instant.ofEpochMilli(System.currentTimeMillis() - hours * 3_600_000L).toString()
        val url = "${endpoint.currentBaseUrl}/api/history/period/$startIso" +
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
     * Only the touched entity is re-read afterwards; pulling the entire state
     * table on every tap was both wasteful and able to resurrect a stale value.
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
            val url = "${endpoint.currentBaseUrl}/api/services/$domain/$service"
            var accepted = false
            var httpCode: Int? = null
            var networkError: Throwable? = null
            // (GAP-44) Port iOS HomeAssistantManager.restFallback "gui bu
            // dung 1 lan": retry dung 1 lan sau ~500ms CHI khi loi
            // network/timeout (IOException — gom SocketTimeoutException,
            // UnknownHostException). HTTP tra ve (ke ca 4xx/5xx) va loi
            // khac khong retry: 4xx retry cung hong nhu cu.
            for (attempt in 0..1) {
                try {
                    val body = dataJson.toRequestBody("application/json".toMediaType())
                    val req = Request.Builder().url(url)
                        .header("Authorization", "Bearer $token")
                        .post(body)
                        .build()
                    client.newCall(req).execute().use { resp ->
                        val text = resp.body?.string().orEmpty()
                        Log.i(TAG, "POST /api/services/$domain/$service -> HTTP ${resp.code}, body=${text.take(200)}")
                        httpCode = resp.code
                        accepted = resp.isSuccessful
                        if (!resp.isSuccessful) {
                            _lastError.value = "Service ${resp.code}: ${text.take(200)}"
                        }
                    }
                    break // HTTP da tra ve -> khong retry.
                } catch (t: IOException) {
                    networkError = t
                    Log.w(TAG, "callService network error (attempt ${attempt + 1}/2): ${t.message}")
                    if (attempt == 0) delay(500)
                } catch (t: Throwable) {
                    Log.e(TAG, "callService failed for $url", t)
                    _lastError.value = "Service: ${t.message}"
                    break
                }
            }
            if (httpCode == null && networkError != null) {
                _lastError.value = "Service: ${networkError.message}"
            }
            // Home Assistant refused the call, so roll the optimistic value back at once.
            if (!accepted && optimisticEntityId != null) {
                fetchEntityState(optimisticEntityId)
                return@launch
            }
            // Give Home Assistant a moment to apply the change, then resync.
            delay(400)
            when {
                optimisticEntityId != null -> fetchEntityState(optimisticEntityId)
                // No socket means no state_changed event, so a full pull is the only way back.
                !_connected.value -> fetchInitialStates()
            }
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

    /**
     * toggle() in HomeAssistantManager.swift flips the cached state before the
     * request leaves the device, so the card reacts on the same frame as the tap.
     */
    fun toggle(entityId: String) {
        val flipped = if (_entities.value[entityId]?.isOn == true) "off" else "on"
        applyLocalState(entityId, flipped)
        callService(entityId.substringBefore('.'), "toggle", serviceData(entityId), entityId)
    }

    /**
     * Climate KHONG co service "toggle" trong HA. Tat -> turn_off; bat lai ->
     * set_hvac_mode cool (GAP-28, port iOS HomeRooms.swift:602-611, comment
     * "Port toggleClimate"). DOI SO VOI CU: truoc day bat lai goi
     * climate.turn_on (ve mode cu / mode mac dinh cua HA); tu day LUON cool.
     */
    fun toggleClimate(entityId: String) {
        val currentState = _entities.value[entityId]?.state ?: "off"
        val isOn = currentState != "off"
        if (isOn) {
            applyLocalState(entityId, "off")
            callService("climate", "turn_off", serviceData(entityId), entityId)
        } else {
            // (GAP-28) Bat lai LUON ve cool — khong ve mode cu nhu truoc.
            applyLocalState(entityId, "cool")
            callService(
                "climate", "set_hvac_mode",
                serviceData(entityId) { put("hvac_mode", "cool") }, entityId,
            )
        }
    }

    /**
     * (GAP-30) Bat/tat TOAN BO den trong phong bang 1 lenh duy nhat — port
     * iOS HomeRooms.swift:241-248 (toggleRoomLights): entity_id = toan bo
     * lightIds, KHONG loc theo trang thai; bat/tat theo trang thai chung:
     * bat ky den nao dang bat -> turn_off toan bo, nguoc lai -> turn_on
     * toan bo.
     */
    fun toggleRoomLights(entityIds: List<String>) {
        if (entityIds.isEmpty()) return
        val anyOn = entityIds.any { _entities.value[it]?.isOn == true }
        val service = if (anyOn) "turn_off" else "turn_on"
        val expected = if (anyOn) "off" else "on"
        // Flip lac quan tung den (callService chi ho tro 1 optimisticEntityId).
        entityIds.forEach { applyLocalState(it, expected) }
        val data = buildJsonObject {
            put("entity_id", buildJsonArray { entityIds.forEach { add(it) } })
        }.toString()
        callService("light", service, data)
    }

    fun setLightBrightness(entityId: String, percent: Int) =
        callService("light", "turn_on", serviceData(entityId) { put("brightness_pct", percent) }, entityId)

    fun setLightColorTemp(entityId: String, kelvin: Int) =
        callService("light", "turn_on", serviceData(entityId) { put("color_temp_kelvin", kelvin) }, entityId)

    /**
     * (Batch D, GAP-09) Gui mau RGB + hieu ung cho den — iOS LightExpandedView
     * goi light.turn_on voi rgb_color / effect (HA tu doi sang xy cho
     * smartlight). Khong them color_temp vao the mo rong (iOS cung khong co).
     */
    fun setLightRgb(entityId: String, r: Int, g: Int, b: Int) =
        callService(
            "light", "turn_on",
            serviceData(entityId) {
                put(
                    "rgb_color",
                    buildJsonArray {
                        add(r); add(g); add(b)
                    },
                )
            },
            entityId,
        )

    fun setLightEffect(entityId: String, effect: String) =
        callService(
            "light", "turn_on",
            serviceData(entityId) { put("effect", effect) },
            entityId,
        )

    /**
     * (Batch D, GAP-08) Gui datapoint cho thiet bi LocalTuya — dung format
     * JSON iOS goi (RoomExpandedCards.swift): {"device_id": ..., "dp": N,
     * "value": ...}. Chi gui ma chuong trinh DA BIET; ma chua ro khong
     * bao gio goi ham nay (UI chi thu lai).
     */
    fun setLocalTuyaDp(deviceId: String, dp: Int, value: String) =
        callService(
            "localtuya", "set_dp",
            buildJsonObject {
                put("device_id", deviceId)
                put("dp", dp)
                put("value", value)
            }.toString(),
        )

    /** The stepper must move on the tap, so write the target attribute locally first. */
    fun setClimateTemperature(entityId: String, temperature: Double) {
        applyLocalAttribute(entityId, "temperature", JsonPrimitive(temperature))
        callService("climate", "set_temperature", serviceData(entityId) { put("temperature", temperature) }, entityId)
    }

    fun setHvacMode(entityId: String, mode: String) {
        applyLocalState(entityId, mode)
        callService("climate", "set_hvac_mode", serviceData(entityId) { put("hvac_mode", mode) }, entityId)
    }

    fun setFanMode(entityId: String, mode: String) {
        applyLocalAttribute(entityId, "fan_mode", JsonPrimitive(mode))
        callService("climate", "set_fan_mode", serviceData(entityId) { put("fan_mode", mode) }, entityId)
    }

    fun setCoverPosition(entityId: String, position: Int) =
        callService("cover", "set_cover_position", serviceData(entityId) { put("position", position) }, entityId)

    fun pressButton(entityId: String) =
        callService("button", "press", serviceData(entityId))

    fun selectOption(entityId: String, option: String) {
        applyLocalState(entityId, option)
        callService(entityId.substringBefore('.'), "select_option", serviceData(entityId) { put("option", option) }, entityId)
    }

    fun alarmArm(entityId: String, mode: String, code: String? = null) =
        callService(
            "alarm_control_panel",
            "alarm_arm_$mode",
            serviceData(entityId) { if (code != null) put("code", code) },
            entityId,
        )

    fun alarmDisarm(entityId: String, code: String? = null) =
        callService(
            "alarm_control_panel",
            "alarm_disarm",
            serviceData(entityId) { if (code != null) put("code", code) },
            entityId,
        )

    /** optimistic() in HomeAssistantManager.swift. */
    private fun applyLocalState(entityId: String, state: String) {
        val current = _entities.value[entityId] ?: return
        if (current.state == state) return
        synchronized(optimisticUntil) { optimisticUntil[entityId] = System.currentTimeMillis() }
        _entities.value = _entities.value + (entityId to current.copy(state = state))
        _entityUpdated.tryEmit(entityId)
    }

    /** setClimateTemperature() in HomeAssistantManager.swift writes the attribute in place. */
    private fun applyLocalAttribute(entityId: String, key: String, value: JsonElement) {
        val current = _entities.value[entityId] ?: return
        if (current.attributes[key] == value) return
        synchronized(optimisticUntil) { optimisticUntil[entityId] = System.currentTimeMillis() }
        val attributes = current.attributes.toMutableMap().apply { this[key] = value }
        _entities.value = _entities.value + (entityId to current.copy(attributes = attributes))
        _entityUpdated.tryEmit(entityId)
    }

    /* ---------------- automation config API (GAP-42) ---------------- */

    /**
     * Port iOS HomeAssistantManager.createAutomation
     * (HomeAssistantManager.swift:1218-1240): POST
     * /api/config/automation/config/app_<epoch giay> voi body
     * {alias, trigger, condition: [], action, mode: "single"}.
     * Tra true khi HTTP 2xx. iOS sinh id theo giay — 2 automation tao
     * cung 1 giay se ghi de nhau; giu dung hanh vi iOS, khong tu sua.
     * HA automation config API doi token co quyen config (giong thong
     * bao loi ScenesView.swift hien khi tao that bai). Chi 1 attempt
     * nhu iOS (khong ap retry GAP-44 — day khong phai service call).
     * [triggers]/[actions] la JSON dung dinh dang HA automation, vd
     * trigger numeric_state cua ScenesView.save(): {"platform":
     * "numeric_state", "entity_id": ..., "above": ...}.
     */
    suspend fun createAutomation(
        alias: String,
        triggers: JsonArray,
        actions: JsonArray,
    ): Boolean {
        val id = "app_${System.currentTimeMillis() / 1000}"
        val body = buildJsonObject {
            put("alias", alias)
            put("trigger", triggers)
            put("condition", buildJsonArray { })
            put("action", actions)
            put("mode", "single")
        }.toString()
        return postAutomationConfig(id, body)
    }

    /**
     * Port iOS HomeAssistantManager.createScheduledAutomation
     * (HomeAssistantManager.swift:1256-1280): tao/ghi de automation voi
     * id do app quan ly (de bat/tat/xoa theo lich ngu canh —
     * SceneScheduleStore.swift:83,104 dung deleteAutomation cung id nay).
     * Khac createAutomation: body co them "id" va condition do caller
     * truyen (vd loc thu trong tuan).
     */
    suspend fun createScheduledAutomation(
        configId: String,
        alias: String,
        triggers: JsonArray,
        conditions: JsonArray,
        actions: JsonArray,
    ): Boolean {
        val body = buildJsonObject {
            put("id", configId)
            put("alias", alias)
            put("trigger", triggers)
            put("condition", conditions)
            put("action", actions)
            put("mode", "single")
        }.toString()
        return postAutomationConfig(configId, body)
    }

    private suspend fun postAutomationConfig(configId: String, bodyJson: String): Boolean =
        withContext(Dispatchers.IO) {
            val url = "${endpoint.currentBaseUrl}/api/config/automation/config/$configId"
            try {
                val body = bodyJson.toRequestBody("application/json".toMediaType())
                val req = Request.Builder().url(url)
                    .header("Authorization", "Bearer $token")
                    .post(body)
                    .build()
                client.newCall(req).execute().use { resp ->
                    Log.i(TAG, "POST /api/config/automation/config/$configId -> HTTP ${resp.code}")
                    if (!resp.isSuccessful) {
                        _lastError.value =
                            "Automation ${resp.code}: ${resp.body?.string().orEmpty().take(200)}"
                    }
                    resp.isSuccessful
                }
            } catch (t: Throwable) {
                Log.e(TAG, "postAutomationConfig failed for $url", t)
                _lastError.value = "Automation: ${t.message}"
                false
            }
        }

    /**
     * Port iOS HomeAssistantManager.deleteAutomation
     * (HomeAssistantManager.swift:1241-1253): DELETE
     * /api/config/automation/config/<configId>. configId lay tu
     * attribute "id" cua automation (xem SceneScheduleStore.swift,
     * ScenesView.swift:306,349). Tra true khi HTTP 2xx.
     */
    suspend fun deleteAutomation(configId: String): Boolean = withContext(Dispatchers.IO) {
        val url = "${endpoint.currentBaseUrl}/api/config/automation/config/$configId"
        try {
            val req = Request.Builder().url(url)
                .header("Authorization", "Bearer $token")
                .delete()
                .build()
            client.newCall(req).execute().use { resp ->
                Log.i(TAG, "DELETE /api/config/automation/config/$configId -> HTTP ${resp.code}")
                if (!resp.isSuccessful) {
                    _lastError.value =
                        "Automation ${resp.code}: ${resp.body?.string().orEmpty().take(200)}"
                }
                resp.isSuccessful
            }
        } catch (t: Throwable) {
            Log.e(TAG, "deleteAutomation failed for $url", t)
            _lastError.value = "Automation: ${t.message}"
            false
        }
    }

    private companion object {
        /** How long a local flip outranks a REST snapshot. */
        const val OPTIMISTIC_WINDOW_MS = 5_000L

        /** (GAP-39) Ten file snapshot /api/states — giong iOS states_snapshot.json. */
        const val STATES_SNAPSHOT_FILE = "states_snapshot.json"

        /** (GAP-41c) Port iOS EntityObservation.uiThrottle = 2.0s. */
        const val UI_THROTTLE_MS = 2_000L

        /** (GAP-40) iOS updateBucket min5Exact — copy nguyen 6 entity. */
        val MIN5_EXACT: Set<String> = setOf(
            "sensor.solis_s6_eh1p_battery_soc_2",
            "sensor.solis_s6_eh1p_today_battery_charge_energy_2",
            "sensor.solis_s6_eh1p_today_battery_discharge_energy_2",
            "sensor.solis_s6_eh1p_battery_voltage_2",
            "sensor.solis_s6_eh1p_battery_charge_current_limitation_bms_2",
            "sensor.solis_s6_eh1p_battery_discharge_current_limitation_bms_2",
        )

        /** (GAP-40) iOS updateBucket s10Exact — copy nguyen 16 entity. */
        val S10_EXACT: Set<String> = setOf(
            "sensor.battery_current_flow",
            "sensor.battery_power_flow",
            "sensor.cong_suat_nha",
            "sensor.dashboard_active_card",
            "sensor.dashboard_active_card_2",
            "sensor.grid_power",
            "sensor.o_cam_may_giat_power",
            "sensor.o_cam_may_say_power",
            "sensor.battery_time_to_full_2",
            "sensor.thoi_gian_pin_con_lai",
            "sensor.solis_s6_eh1p_pv_power_1_3",
            "sensor.solis_s6_eh1p_pv_power_2_3",
            "sensor.solis_s6_eh1p_pv_voltage_1_2",
            "sensor.solis_s6_eh1p_pv_voltage_2_2",
            "sensor.solis_s6_eh1p_pv_current_1_2",
            "sensor.solis_s6_eh1p_pv_current_2_2",
        )

        /**
         * (GAP-41c) Port iOS EntityObservation.controlPrefixes — copy DUNG
         * danh sach hien tai cua iOS (da gom number.* tu delta c2e12e6):
         * entity dieu khien luon update tuc thi, khong throttle. Vi
         * number.* la control nen thuc te chi sensor.* bi throttle.
         */
        val CONTROL_PREFIXES: List<String> = listOf(
            "light.", "switch.", "fan.", "cover.", "lock.", "climate.",
            "binary_sensor.", "alarm_control_panel.", "number.",
        )
    }
}
