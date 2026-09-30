package com.smarthome.hume.data

import com.smarthome.hume.core.data.HomeRepository
import com.smarthome.hume.core.ha.HistoryFetcher
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.AlarmUi
import com.smarthome.hume.core.model.BatteryUi
import com.smarthome.hume.core.model.ClimateUi
import com.smarthome.hume.core.model.ConnectionState
import com.smarthome.hume.core.model.DefaultRooms
import com.smarthome.hume.core.model.DeviceKind
import com.smarthome.hume.core.model.DeviceUi
import com.smarthome.hume.core.model.HomeEntity as LegacyEntity
import com.smarthome.hume.core.model.HomeNotification
import com.smarthome.hume.core.model.HomeUiState
import com.smarthome.hume.core.model.HumeConfig
import com.smarthome.hume.core.model.RoomBubbleConfig
import com.smarthome.hume.core.model.RoomConfig
import com.smarthome.hume.core.model.RoomUi
import com.smarthome.hume.core.model.SolarDay
import com.smarthome.hume.core.model.cameraKeyForSensor
import com.smarthome.hume.core.model.FRIGATE_CAMERA_KEYS
import com.smarthome.hume.core.model.cameraNameVi
import com.smarthome.hume.core.model.frigateLabelVi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDate
import java.time.ZoneId

/**
 * Adapter: map HomeAssistantRepository cu (app module) sang HomeUiState moi.
 * Giu nguyen toan bo logic HA/WiFi hien tai; UI moi chi doc HomeRepository.
 */
class AppHomeRepository(
    private val ha: HomeAssistantRepository,
    private val scope: CoroutineScope,
    appContext: android.content.Context,
) : HomeRepository {

    private val _homeState = MutableStateFlow(HomeUiState())
    override val homeState: StateFlow<HomeUiState> = _homeState.asStateFlow()

    private val weekCache = mutableMapOf<Long, Double>()
    private val snapshots = com.smarthome.hume.core.storage.DailySnapshotStore.get(appContext)

    companion object {
        /** Event Frigate trong N phut moi duoc tong hop thanh goi y (khong co PIR). */
        private const val FRIGATE_SUGGEST_MIN = 5
    }
    private val settingsStore = com.smarthome.hume.core.storage.SettingsStore(appContext)
    private val frigateStore = com.smarthome.hume.core.frigate.FrigateStore.get(appContext)
    /** Lan cuoi goi Frigate lay nhan doi tuong (tranh spam moi lan entity doi). */
    private var motionFetchAt = 0L
    private var motionFetchIds: Set<String> = emptySet()
    /** Goi y tong hop tu trigger Frigate truc tiep (khong qua PIR). */
    private var frigateMotions: List<HomeNotification> = emptyList()

    init {
        scope.launch {
            combine(ha.entities, ha.connected) { e, c -> e to c }
                .collect { (entities, connected) ->
                    val connState = when {
                        connected -> ConnectionState.Connected
                        ha.isConnecting() -> ConnectionState.Connecting
                        else -> ConnectionState.Disconnected
                    }
                    val newState = buildState(entities, connected, connState)
                    _homeState.value = enrichMotionObjects(newState, connected)
                    // Realtime (2026-09-30): dang ky watched entities cho dashboard M3E
                    // (truoc day chi ViewModel cu goi setWatchedEntities, dashboard moi
                    // khong goi -> sensor roi vao bucket ONE_DAY, khong realtime).
                    updateWatched(newState)
                }
        }
        scope.launch {
            ha.connected.collectLatest { connected ->
                if (!connected) return@collectLatest
                refreshSolarWeek()
                while (isActive) {
                    delay(300_000L)
                    if (ha.connected.value) refreshSolarWeek()
                }
            }
        }
    }

    // ---------- actions ----------

    /**
     * 24 den trong config (RoomBubbleConfig) — dem bong den CHI dung danh sach
     * nay, khong dung light.* (2026-09-30, user yeu cau). Loai group entity
     * (vd: light.all_light, group phong ngu) de khong dem trung.
     */
    private val configuredLightIds: Set<String> by lazy {
        com.smarthome.hume.core.model.RoomBubbleConfig.all
            .flatMap { it.devices }
            .map { it.entity }
            .filter { it.startsWith("light.") }
            .toSet()
    }

    /** True neu entity la group (co attribute entity_id chua list member). */
    private fun isGroup(e: LegacyEntity): Boolean {
        val ids = e.attributes["entity_id"] as? kotlinx.serialization.json.JsonArray
        return ids != null && ids.isNotEmpty()
    }

    /** Dang ky realtime cho cac entity dashboard dang hien (2026-09-30). */
    private var lastWatched: Set<String> = emptySet()
    private fun updateWatched(s: HomeUiState) {
        val ids = HashSet<String>()
        // Tat ca thiet bi tren UI (config phong + climate) — bao gom sensor dien.
        s.searchDevices.forEach { ids.add(it.entityId) }
        s.lightsOn.forEach { ids.add(it.entityId) }
        s.alarm?.let { ids.add(it.entityId) }
        // Nang luong: PV + pin.
        ids.add(HumeConfig.PV_POWER); ids.add(HumeConfig.PV_TODAY)
        ids.add(HumeConfig.BATTERY_SOC); ids.add(HumeConfig.BATTERY_POWER)
        // An ninh: sensor cua/chuyen dong/khoi/nuoc.
        runCatching {
            ids.addAll(com.smarthome.hume.core.data.HumeGraph.get().securityRepository.sensorEntityIds)
        }
        if (ids != lastWatched) {
            lastWatched = ids
            ha.setWatchedEntities(ids)
        }
    }

    override fun toggle(entityId: String) = ha.toggle(entityId)
    override fun setLightBrightness(entityId: String, percent: Int) =
        ha.setLightBrightness(entityId, percent)
    override fun setClimateTemp(entityId: String, tempC: Double) =
        ha.setClimateTemperature(entityId, tempC)
    override fun setHvacMode(entityId: String, mode: String) = ha.setHvacMode(entityId, mode)
    override fun toggleClimate(entityId: String) = ha.toggleClimate(entityId)
    override fun alarmArm(mode: String) {
        val entity = alarmEntityId() ?: return
        ha.alarmArm(entity, mode.trim().removePrefix("arm_"), HumeConfig.ALARM_CODE)
    }
    override fun alarmDisarm() {
        val entity = alarmEntityId() ?: return
        ha.alarmDisarm(entity, HumeConfig.ALARM_CODE)
    }
    override fun refreshSolarWeek() {
        scope.launch(Dispatchers.IO) { loadSolarWeek() }
    }

    // ---------- mapping ----------

    private fun alarmEntityId(): String? {
        val ids = ha.entities.value.keys
        return when {
            ids.contains(HumeConfig.ALARM_PRIMARY) -> HumeConfig.ALARM_PRIMARY
            ids.contains(HumeConfig.ALARM_FALLBACK) -> HumeConfig.ALARM_FALLBACK
            else -> ids.firstOrNull { it.startsWith("alarm_control_panel.") }
        }
    }

    private fun buildState(
        entities: Map<String, LegacyEntity>,
        connected: Boolean,
        connectionState: ConnectionState,
    ): HomeUiState {
        val cur = _homeState.value
        // Ten + avatar nguoi dung: lay person dau tien (khong hardcode id).
        val person = entities.values.firstOrNull { it.id.startsWith("person.") }
        val personName = person
            ?.attributes?.get("friendly_name")?.jsonPrimitive?.contentOrNull
            ?.takeIf { it.isNotBlank() && !it.startsWith("person.") }
            ?: ""
        // Avatar: entity_picture cua person (HA tra ve duong dan tuong doi hoac URL day du).
        val rawPicture = person?.attributes?.get("entity_picture")?.jsonPrimitive?.contentOrNull
        val avatarUrl = rawPicture?.takeIf { it.isNotBlank() }?.let { pic ->
            if (pic.startsWith("http")) pic else ha.getBaseUrl().trimEnd('/') + pic
        }
        if (avatarUrl == null) {
            android.util.Log.d("AppHomeRepository", "Khong co avatar user (person entity_picture trong)")
        }
        // Key luu avatar theo user: giong logic ProfileScreen (user_id, fallback ten).
        val userKey = person?.attributes?.get("user_id")?.jsonPrimitive?.contentOrNull
            ?.takeIf { it.isNotBlank() }
            ?: personName
        // Publish cho tab Toi (MeScreen) dung chung key avatar voi header trang Nha.
        runCatching { com.smarthome.hume.core.data.HumeGraph.get().userKey.value = userKey }

        val pvToday = entities[HumeConfig.PV_TODAY]?.numericState
        val solarNowKw = (entities[HumeConfig.PV_POWER]?.numericState ?: 0.0) / 1000.0

        val soc = (entities[HumeConfig.BATTERY_SOC]?.numericState ?: 0.0).toInt()
        val battPowerW = entities[HumeConfig.BATTERY_POWER]?.numericState ?: 0.0
        val battPowerKw = battPowerW / 1000.0
        // Cong thuc Hume goc: backupSoc tu number.solis_s6_eh1p_backup_soc_2, mac dinh 20
        val backupSoc = (entities[HumeConfig.BACKUP_SOC]?.numericState ?: 20.0).toInt()
        // Hume goc: resting = power 0..5W -> khong hien thoi gian
        val resting = battPowerW in 0.0..5.0
        val discharging = battPowerW < 0.0
        // Sensor runtime theo huong: xa -> TIME_LEFT, sac/nghi -> TIME_TO_FULL
        val runtimeEntity = if (discharging)
            entities[HumeConfig.BATTERY_TIME_LEFT]
        else
            entities[HumeConfig.BATTERY_TIME_TO_FULL]
        // Hume goc: uu tien friendly_time attribute, fallback raw state
        val runtimeText = if (resting) null else
            runtimeEntity?.attributes?.get("friendly_time")?.toString()
                ?.trim('"')?.takeIf { it.isNotBlank() }
                ?: runtimeEntity?.state?.takeIf { it.isNotBlank() && it != "unknown" }
        val endTime = if (resting) null else runtimeEntity?.state?.let { parseDurationToEndTime(it) }

        val alarmId = alarmEntityId()
        val alarmEntity = alarmId?.let { entities[it] }
        val alarm = alarmEntity?.let {
            AlarmUi(
                entityId = alarmId,
                state = it.state,
                label = HumeConfig.alarmLabel(it.state),
                isArmed = it.state != "disarmed",
            )
        }

        val lightsOn = entities.values
            .filter { it.id in configuredLightIds && it.isOn && !isGroup(it) }
            .map { e ->
                DeviceUi(
                    entityId = e.id,
                    label = friendlyName(e),
                    sub = "",
                    iconKey = "bulb",
                    isOn = true,
                )
            }

        return HomeUiState(
            userName = personName,
            avatarUrl = avatarUrl,
            userKey = userKey,
            connected = connected,
            connectionState = connectionState,
            solarWeek = cur.solarWeek,
            solarTodayKwh = pvToday,
            solarNowKw = solarNowKw,
            battery = BatteryUi(
                soc = soc,
                powerKw = battPowerKw,
                powerW = battPowerW,
                backupSoc = backupSoc,
                timeText = runtimeText,
                endTime = endTime,
            ),
            alarm = alarm,
            lightsOn = lightsOn,
            rooms = DefaultRooms.all.map { buildRoom(it, entities) },
            notifications = buildNotifications(entities),
        ).let { st ->
            st.copy(searchDevices = buildSearchDevices(entities, st.rooms))
        }
    }

    /**
     * Gan nhan doi tuong Frigate (person/car/dog...) cho sensor chuyen dong
     * dang active, de the goi y mo ta "Có người hoạt động lúc 06:25".
     * Chi goi Frigate khi tap sensor thay doi hoac qua 60s (moi camera 1
     * request /api/events nhe, khong tai clip); event qua 15 phut thi bo qua.
     *
     * Them (2026-09-30, user: dung trigger event cua Frigate): camera co event
     * Frigate moi (<= FRIGATE_SUGGEST_MIN phut) nhung khong co sensor PIR nao
     * active -> tu tao thong bao "frigate:<cam>" de goi y dung camera do.
     */
    private suspend fun enrichMotionObjects(
        state: HomeUiState, connected: Boolean,
    ): HomeUiState {
        val motions = state.notifications.filter { it.title == "Phát hiện chuyển động" }
        val ids = motions.map { it.id }.toSet()
        if (ids.isEmpty() || !connected) {
            motionFetchIds = emptySet()
            frigateMotions = emptyList()
            return if (state.motionObjects.isEmpty()) state
            else state.copy(motionObjects = emptyMap())
        }
        val now = System.currentTimeMillis()
        // Goi y Frigate tong hop het han -> bat buoc fetch lai de loai bo.
        val syntheticsStale = frigateMotions.isNotEmpty() &&
            frigateMotions.all { (it.minutesAgo ?: 999) > FRIGATE_SUGGEST_MIN }
        if (ids == motionFetchIds && now - motionFetchAt < 60_000 && !syntheticsStale) {
            // Giu nhan cu, loc theo sensor dang active + goi y Frigate con han.
            val fresh = frigateMotions.filter { (it.minutesAgo ?: 999) <= FRIGATE_SUGGEST_MIN }
            val allIds = ids + fresh.map { it.id }
            val kept = _homeState.value.motionObjects.filterKeys { it in allIds }
            return state.copy(
                notifications = (state.notifications + fresh).take(12),
                motionObjects = kept,
            )
        }
        motionFetchIds = ids
        motionFetchAt = now
        val settings = runCatching { settingsStore.settings.first() }.getOrNull()
            ?: return state
        if (!settings.hasToken) return state
        val objects = mutableMapOf<String, String>()
        // Moi camera chi fetch 1 lan cho tat ca sensor map ve no.
        val byCam = motions.mapNotNull { n ->
            cameraKeyForSensor(n.id)?.let { cam -> cam to n.id }
        }.groupBy({ it.first }, { it.second })
        val freshSynthetics = mutableListOf<HomeNotification>()
        for (cam in FRIGATE_CAMERA_KEYS) {
            val (label, start) = runCatching {
                frigateStore.latestEvent(cam, settings.haUrl, settings.haToken)
            }.getOrNull() ?: continue
            if (label.isBlank()) continue
            val ageSec = now / 1000 - start
            if (ageSec < 0) continue
            val vi = frigateLabelVi(label)
            val sensorIds = byCam[cam]
            if (sensorIds != null) {
                // Nhan doi tuong cho sensor dang active (event <= 15 phut).
                if (ageSec <= 900) sensorIds.forEach { objects[it] = vi }
            } else if (ageSec <= FRIGATE_SUGGEST_MIN * 60) {
                // Trigger truc tiep tu Frigate, khong co PIR nao: goi y tong hop.
                val mins = (ageSec / 60).toInt()
                val nid = "frigate:$cam"
                freshSynthetics += HomeNotification(
                    id = nid,
                    title = "Phát hiện chuyển động",
                    body = cameraNameVi(cam),
                    timeText = if (mins < 1) "vừa xong" else "$mins phút trước",
                    minutesAgo = mins,
                )
                objects[nid] = vi
            }
        }
        frigateMotions = freshSynthetics
        return state.copy(
            notifications = (state.notifications + freshSynthetics).take(12),
            motionObjects = objects,
        )
    }

    /**
     * Danh sach tim kiem day du cho DeviceSearchView.
     * 1. Thiet bi tu config tinh (label dep) + climate moi phong.
     * 2. Tat ca entity light/switch/fan/cover/lock/climate con lai -> tu dong
     *    phan loai dung (dieu hoa / den / o cam / cong tac / quat / rem / khoa).
     * sub mang tu khoa loai de chip "Dieu hoa"/"O cam" luon match.
     */
    /**
     * Danh sach tim kiem (2026-09-30): CHI gom thiet bi co tren giao dien
     * (thiet bi + dieu hoa cua cac phong). Entity le khong thuoc phong nao
     * (khong hien tren UI) khong dua vao search theo yeu cau user.
     */
    private fun buildSearchDevices(
        entities: Map<String, LegacyEntity>,
        rooms: List<RoomUi>,
    ): List<DeviceUi> {
        val out = mutableListOf<DeviceUi>()
        rooms.forEach { r ->
            r.devices.forEach { d ->
                out.add(d.copy(sub = r.name))
            }
            r.climate?.let { c ->
                out.add(
                    DeviceUi(
                        entityId = c.entityId,
                        label = climateSearchLabel(entities[c.entityId]),
                        sub = r.name,
                        iconKey = "snowflake",
                        kind = DeviceKind.Climate,
                        isOn = c.isOn,
                    )
                )
            }
        }
        return out.distinctBy { it.entityId }
    }

    /** Label dieu hoa: dam bao luon chua "dieu hoa" de chip tim kiem match. */
    private fun climateSearchLabel(e: LegacyEntity?): String {
        if (e == null) return "Điều hoà"
        val n = friendlyName(e)
        return if (n.contains("điều hoà", ignoreCase = true) ||
            n.contains("điều hòa", ignoreCase = true)
        ) n else "Điều hoà"
    }

    private fun buildRoom(room: RoomConfig, entities: Map<String, LegacyEntity>): RoomUi {
        val bubble = RoomBubbleConfig.all.firstOrNull { matches(room, it) }
        val lightOn = room.lightEntity.let { entities[it]?.isOn } ?: false
        val devices = bubble?.devices.orEmpty()
            .filter { it.type == "toggle" }
            .map { d ->
                val e = entities[d.entity]
                DeviceUi(
                    entityId = d.entity,
                    label = d.label,
                    sub = d.sub,
                    iconKey = d.icon,
                    isOn = e?.isOn == true,
                    powerW = d.powerEntity?.let { entities[it]?.numericState },
                )
            }
        val climateEntity = room.climateEntity?.let { entities[it] }
        val climate = climateEntity?.let { e ->
            val modes = e.attributes["hvac_modes"]?.jsonArray
                ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                ?.filter { it != "off" }
                .orEmpty()
            ClimateUi(
                entityId = e.id,
                currentTemp = e.attributes["current_temperature"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull(),
                targetTemp = e.attributes["temperature"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull(),
                hvacMode = e.state,
                isOn = e.state != "off",
                modes = modes,
            )
        }
        return RoomUi(
            key = room.rawKey,
            name = room.name,
            iconKey = room.icon,
            tempC = entities[room.tempEntity]?.numericState,
            humidityPct = entities[room.humidityEntity]?.numericState,
            lightEntityId = room.lightEntity,
            lightOn = lightOn,
            devicesOn = devices.count { it.isOn },
            deviceCount = devices.size,
            climate = climate,
            devices = devices,
        )
    }

    private fun matches(room: RoomConfig, bubble: RoomBubbleConfig): Boolean {
        val l = bubble.label.lowercase()
        return when (room.name) {
            "Phòng Ngủ" -> "ngủ chính" in l
            "Phòng Trẻ Em" -> "trẻ em" in l
            "Phòng Thờ" -> "thờ" in l
            "Phòng Khách" -> "khách" in l
            "Phòng Tắm" -> "tắm" in l
            "Phòng Bếp" -> "bếp" in l
            "Phòng Giặt" -> "giặt" in l
            "Hành Lang" -> "hành lang" in l
            else -> false
        }
    }

    private fun friendlyName(e: LegacyEntity): String {
        val raw = e.attributes["friendly_name"]?.jsonPrimitive?.contentOrNull
            ?.takeIf { it.isNotBlank() } ?: e.id.substringAfter('.').replace('_', ' ')
        // Viet hoa ngan gon nhung ten con tieng Anh (friendly_name mac dinh
        // cua integration / fallback tu entity_id).
        return vietnameseShort(raw)
    }

    /**
     * Viet hoa ngan gon, ro nghia ten thiet bi con tieng Anh.
     * Ten da co dau tieng Viet duoc giu nguyen. Dich theo cum (dai truoc)
     * voi word-boundary; cum khong co trong tu dien giu nguyen.
     */
    private fun vietnameseShort(raw: String): String {
        val s = raw.trim()
        if (s.isEmpty()) return s
        // Da la tieng Viet (co ky tu co dau) -> giu nguyen
        if (s.any { it in '\u00c0'..'\u1ef9' }) return s
        var out = " ${s.lowercase()} "
        for ((en, vi) in VI_NAME_DICT) {
            out = out.replace(Regex("\\b${Regex.escape(en)}\\b"), vi)
        }
        // "l1".."l9" (kenh/line) -> chi giu so
        out = out.replace(Regex("\\bl([1-9])\\b"), "$1")
        out = out.replace(Regex("\\s+"), " ").trim()
        return out.replaceFirstChar { it.uppercase() }
    }

    /**
     * Tat ca sensor co mat tren giao dien (an ninh + the phong).
     * Thong bao CHI dung sensor trong danh sach nay (2026-09-30, user yeu cau).
     */
    private val uiSensorIds: Set<String> by lazy {
        val ids = mutableSetOf<String>()
        // An ninh: cua/chuyen dong/khoi/nuoc.
        runCatching {
            ids.addAll(com.smarthome.hume.core.data.HumeGraph.get().securityRepository.sensorEntityIds)
        }
        // The phong: nhiet do/do am/cua.
        com.smarthome.hume.core.model.DefaultRooms.climateRooms.forEach { r ->
            r.tempEntity?.let { ids.add(it) }
            r.humidityEntity?.let { ids.add(it) }
            r.contactEntity?.let { ids.add(it) }
        }
        com.smarthome.hume.core.model.DefaultRooms.basicRooms.forEach { r ->
            r.tempEntity?.let { ids.add(it) }
            r.humidityEntity?.let { ids.add(it) }
            r.contactEntity?.let { ids.add(it) }
        }
        com.smarthome.hume.core.model.RoomBubbleConfig.all.forEach { b ->
            b.tempEntity?.let { ids.add(it) }
            b.humidityEntity?.let { ids.add(it) }
        }
        ids
    }

    private fun buildNotifications(entities: Map<String, LegacyEntity>): List<HomeNotification> {
        val out = mutableListOf<HomeNotification>()
        entities.values
            .filter { it.id in uiSensorIds && it.id.startsWith("binary_sensor.") && it.state == "on" }
            .forEach { e ->
                val dc = e.attributes["device_class"]?.jsonPrimitive?.contentOrNull
                val name = friendlyName(e)
                val mins = e.minutesAgo()
                val time = when {
                    mins == null -> ""
                    mins < 1 -> "vừa xong"
                    mins < 60 -> "$mins phút trước"
                    else -> "${mins / 60} giờ trước"
                }
                when (dc) {
                    "door", "window", "garage_door", "opening" ->
                        out += HomeNotification(e.id, "Cửa đang mở", name, time, e.minutesAgo())
                    "motion", "occupancy", "presence" ->
                        out += HomeNotification(e.id, "Phát hiện chuyển động", name, time, e.minutesAgo())
                    "smoke" -> out += HomeNotification(e.id, "Cảnh báo khói!", name, time, e.minutesAgo())
                    "moisture" -> out += HomeNotification(e.id, "Phát hiện rò nước!", name, time, e.minutesAgo())
                }
            }
        return out.take(12)
    }

    /**
     * Pattern 3 buoc cua Hume goc (HomeViewModel.weekly):
     * 1. Ngay nao thieu -> fetch history tung ngay (song song), lay max.
     * 2. Ngay nao van thieu -> fetch 1 lan 7 ngay, trich max theo ngay,
     *    fallback = diem moi nhat truoc cuoi ngay.
     * 3. Luu DailySnapshotStore (persistent) + weekCache (memory).
     */
    private suspend fun loadSolarWeek() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val dayMs = 86_400_000L
        val labels = arrayOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
        val dayStarts = (6 downTo 1).map { ago ->
            today.minusDays(ago.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()
        }

        // Buoc 1: doc cache truoc
        val resolved = HashMap<Long, Double>()
        val missing = dayStarts.filter { start ->
            val cached = snapshots.get(HumeConfig.PV_TODAY, start) ?: weekCache[start]
            if (cached != null && cached > 0) {
                resolved[start] = cached; false
            } else true
        }

        if (missing.isNotEmpty()) {
            // Buoc 1: moi ngay thieu = 1 query nho, chay song song
            val perDay: Map<Long, Double> = kotlinx.coroutines.coroutineScope {
                val deferreds = missing.map { start ->
                    async(Dispatchers.IO) {
                        val pts = runCatching {
                            HistoryFetcher.fetchRange(HumeConfig.PV_TODAY, start, start + dayMs)
                        }.getOrDefault(emptyList())
                        // sensor "today energy" reset moi ngay -> max trong ngay = san luong ngay do
                        start to (pts.maxOfOrNull { it.value } ?: 0.0)
                    }
                }
                deferreds.awaitAll().toMap()
            }
            perDay.forEach { (start, v) ->
                if (v > 0) {
                    snapshots.set(HumeConfig.PV_TODAY, start, v)
                    weekCache[start] = v
                    resolved[start] = v
                }
            }

            // Buoc 2: ngay nao van thieu -> keo 1 lan ca tuan
            val stillMissing = missing.filter { resolved[it] == null }
            if (stillMissing.isNotEmpty()) {
                val weekPts = runCatching {
                    HistoryFetcher.fetchRange(
                        HumeConfig.PV_TODAY,
                        dayStarts.first(),
                        dayStarts.last() + dayMs,
                    )
                }.getOrDefault(emptyList())
                if (weekPts.isNotEmpty()) {
                    stillMissing.forEach { start ->
                        val dayEnd = start + dayMs
                        val inDay = weekPts.filter { it.timeMs in start until dayEnd }
                        val v = inDay.maxOfOrNull { it.value }
                            ?: weekPts.filter { it.timeMs < dayEnd }
                                .maxByOrNull { it.timeMs }?.value
                            ?: 0.0
                        if (v > 0) {
                            snapshots.set(HumeConfig.PV_TODAY, start, v)
                            weekCache[start] = v
                            resolved[start] = v
                        }
                    }
                }
            }
            snapshots.prune()
        }

        val days = dayStarts.map { start ->
            val date = java.time.Instant.ofEpochMilli(start).atZone(zone).toLocalDate()
            SolarDay(labels[date.dayOfWeek.value - 1], (resolved[start] ?: 0.0).toFloat())
        } + SolarDay(
            labels[today.dayOfWeek.value - 1],
            (ha.entities.value[HumeConfig.PV_TODAY]?.numericState ?: 0.0).toFloat(),
        )
        _homeState.value = _homeState.value.copy(solarWeek = days)
    }
}

/**
 * Tu dien Anh -> Viet ngan gon cho ten thiet bi (muc 5, 29/09).
 * Thu tu: cum dai truoc, tu don sau. Chi dung cho ten chua co dau
 * tieng Viet (ten thuong hieu nhu Solis/Aqara khong co trong nay
 * nen duoc giu nguyen).
 */
private val VI_NAME_DICT = listOf(
    // Cum chuyen biet
    "total pv power" to "tổng công suất điện mặt trời",
    "pv power" to "công suất điện mặt trời",
    "solar power" to "công suất điện mặt trời",
    "battery power" to "công suất pin",
    "battery soc" to "mức pin",
    "backup soc" to "pin dự phòng",
    "grid power" to "công suất lưới điện",
    "smart light" to "đèn thông minh",
    "smartlight" to "đèn thông minh",
    "ceiling light" to "đèn trần",
    "table lamp" to "đèn bàn",
    "desk lamp" to "đèn bàn học",
    "led strip" to "dải đèn led",
    "light strip" to "dải đèn",
    "night light" to "đèn ngủ",
    "motion sensor" to "cảm biến chuyển động",
    "door sensor" to "cảm biến cửa",
    "window sensor" to "cảm biến cửa sổ",
    "temperature sensor" to "cảm biến nhiệt độ",
    "humidity sensor" to "cảm biến độ ẩm",
    "smoke detector" to "báo khói",
    "smoke sensor" to "cảm biến khói",
    "water leak sensor" to "cảm biến rò rỉ nước",
    "leak sensor" to "cảm biến rò rỉ",
    "presence sensor" to "cảm biến hiện diện",
    "air conditioner" to "điều hòa",
    "air conditioning" to "điều hòa",
    "living room" to "phòng khách",
    "master bedroom" to "phòng ngủ chính",
    "kid bedroom" to "phòng trẻ em",
    "kids bedroom" to "phòng trẻ em",
    "bedroom" to "phòng ngủ",
    "dining room" to "phòng ăn",
    "prayer room" to "phòng thờ",
    "worship room" to "phòng thờ",
    "study room" to "phòng học",
    "kitchen" to "phòng bếp",
    "bathroom" to "phòng tắm",
    "office" to "phòng làm việc",
    "garage" to "nhà xe",
    "balcony" to "ban công",
    "terrace" to "sân thượng",
    "garden" to "sân vườn",
    "hallway" to "hành lang",
    "corridor" to "hành lang",
    "staircase" to "cầu thang",
    "stairs" to "cầu thang",
    "front door" to "cửa chính",
    "back door" to "cửa sau",
    "main door" to "cửa chính",
    "entrance" to "cửa chính",
    // Tu don
    "sensor" to "cảm biến",
    "detector" to "cảm biến",
    "lights" to "đèn",
    "light" to "đèn",
    "lamp" to "đèn",
    "bulb" to "bóng đèn",
    "switch" to "công tắc",
    "button" to "nút bấm",
    "remote" to "điều khiển",
    "dimmer" to "chiết áp",
    "plug" to "ổ cắm",
    "outlet" to "ổ cắm",
    "socket" to "ổ cắm",
    "fan" to "quạt",
    "heater" to "máy sưởi",
    "thermostat" to "điều nhiệt",
    "curtain" to "rèm",
    "blinds" to "rèm",
    "blind" to "rèm",
    "speaker" to "loa",
    "television" to "tivi",
    "tv" to "tivi",
    "camera" to "camera",
    "lock" to "khóa",
    "alarm" to "báo động",
    "siren" to "còi báo động",
    "smoke" to "khói",
    "leak" to "rò rỉ",
    "water" to "nước",
    "gas" to "gas",
    "motion" to "chuyển động",
    "presence" to "hiện diện",
    "door" to "cửa",
    "window" to "cửa sổ",
    "gate" to "cổng",
    "temperature" to "nhiệt độ",
    "humidity" to "độ ẩm",
    "temp" to "nhiệt độ",
    "power" to "công suất",
    "energy" to "điện năng",
    "voltage" to "điện áp",
    "current" to "dòng điện",
    "battery" to "pin",
    "solar" to "mặt trời",
    "pv" to "điện mặt trời",
    "grid" to "lưới điện",
    "backup" to "dự phòng",
    "total" to "tổng",
    "today" to "hôm nay",
    "daily" to "hằng ngày",
    "yesterday" to "hôm qua",
    "smart" to "thông minh",
    "ceiling" to "trần",
    "table" to "bàn",
    "desk" to "bàn",
    "wall" to "tường",
    "floor" to "sàn",
    "outdoor" to "ngoài trời",
    "indoor" to "trong nhà",
    "outside" to "ngoài trời",
    "inside" to "trong nhà",
    "front" to "trước",
    "back" to "sau",
    "left" to "trái",
    "right" to "phải",
    "main" to "chính",
    "room" to "phòng",
)

/**
 * Parse duration tu sensor HA (vi du "2:30", "1:15:30", "90") thanh gio ket thuc "hh:mm".
 * Tra ve null neu khong parse duoc.
 */
private fun parseDurationToEndTime(duration: String): String? {
    return try {
        val parts = duration.trim().split(":")
        val totalMinutes = when (parts.size) {
            3 -> parts[0].toInt() * 60 + parts[1].toInt() // h:mm:ss
            2 -> parts[0].toInt() * 60 + parts[1].toInt() // h:mm hoac m:ss
            1 -> parts[0].toDouble().toInt() // so phut
            else -> return null
        }
        val now = java.time.LocalTime.now()
        val end = now.plusMinutes(totalMinutes.toLong())
        String.format("%02d:%02d", end.hour, end.minute)
    } catch (e: Exception) {
        null
    }
}
