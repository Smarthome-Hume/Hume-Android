package com.smarthome.hume.data

import com.smarthome.hume.core.data.HomeRepository
import com.smarthome.hume.core.ha.HistoryFetcher
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.AlarmUi
import com.smarthome.hume.core.model.BatteryUi
import com.smarthome.hume.core.model.ClimateUi
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

    init {
        scope.launch {
            combine(ha.entities, ha.connected) { e, c -> e to c }
                .collect { (entities, connected) ->
                    _homeState.value = buildState(entities, connected)
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

    override fun toggle(entityId: String) = ha.toggle(entityId)
    override fun setLightBrightness(entityId: String, percent: Int) =
        ha.setLightBrightness(entityId, percent)
    override fun setClimateTemp(entityId: String, tempC: Double) =
        ha.setClimateTemperature(entityId, tempC)
    override fun setHvacMode(entityId: String, mode: String) = ha.setHvacMode(entityId, mode)
    override fun toggleClimate(entityId: String) = ha.toggle(entityId)
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
    ): HomeUiState {
        val cur = _homeState.value
        // Ten nguoi dung: lay person dau tien co friendly_name hop le (khong hardcode id).
        val personName = entities.values
            .firstOrNull { it.id.startsWith("person.") }
            ?.attributes?.get("friendly_name")?.jsonPrimitive?.contentOrNull
            ?.takeIf { it.isNotBlank() && !it.startsWith("person.") }
            ?: ""

        val pvToday = entities[HumeConfig.PV_TODAY]?.numericState
        val solarNowKw = (entities[HumeConfig.PV_POWER]?.numericState ?: 0.0) / 1000.0

        val soc = (entities[HumeConfig.BATTERY_SOC]?.numericState ?: 0.0).toInt()
        val battPowerKw = (entities[HumeConfig.BATTERY_POWER]?.numericState ?: 0.0) / 1000.0
        val timeText = if (battPowerKw < -0.05)
            entities[HumeConfig.BATTERY_TIME_TO_FULL]?.state?.let { "Sạc đầy sau $it" }
        else
            entities[HumeConfig.BATTERY_TIME_LEFT]?.state?.let { "Còn $it" }

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
            .filter { it.id.startsWith("light.") && it.isOn }
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
            connected = connected,
            solarWeek = cur.solarWeek,
            solarTodayKwh = pvToday,
            solarNowKw = solarNowKw,
            battery = BatteryUi(soc = soc, powerKw = battPowerKw, timeText = timeText),
            alarm = alarm,
            lightsOn = lightsOn,
            rooms = DefaultRooms.all.map { buildRoom(it, entities) },
            notifications = buildNotifications(entities),
        )
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

    private fun friendlyName(e: LegacyEntity): String =
        e.attributes["friendly_name"]?.jsonPrimitive?.contentOrNull
            ?.takeIf { it.isNotBlank() } ?: e.id.substringAfter('.').replace('_', ' ')

    private fun buildNotifications(entities: Map<String, LegacyEntity>): List<HomeNotification> {
        val out = mutableListOf<HomeNotification>()
        entities.values
            .filter { it.id.startsWith("binary_sensor.") && it.state == "on" }
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
                        out += HomeNotification(e.id, "Cửa đang mở", name, time)
                    "motion", "occupancy", "presence" ->
                        out += HomeNotification(e.id, "Phát hiện chuyển động", name, time)
                    "smoke" -> out += HomeNotification(e.id, "Cảnh báo khói!", name, time)
                    "moisture" -> out += HomeNotification(e.id, "Phát hiện rò nước!", name, time)
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
            "Hôm nay",
            (ha.entities.value[HumeConfig.PV_TODAY]?.numericState ?: 0.0).toFloat(),
        )
        _homeState.value = _homeState.value.copy(solarWeek = days)
    }
}
