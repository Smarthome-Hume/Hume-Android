package com.smarthome.hume.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smarthome.hume.core.ha.HistoryPoint
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.DefaultRooms
import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.core.model.HumeConfig
import com.smarthome.hume.core.model.RoomBubbleConfig
import com.smarthome.hume.core.model.RoomConfig
import com.smarthome.hume.core.scene.ManagedItem
import com.smarthome.hume.core.scene.ManagedListsStore
import com.smarthome.hume.core.storage.DailySnapshotStore
import com.smarthome.hume.core.storage.SensorDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Everything the home dashboard renders, in one immutable snapshot. */
data class HomeUiState(
    val entities: Map<String, HomeEntity> = emptyMap(),
    val connected: Boolean = false,
    val error: String? = null,
    val lightsOn: Int = 0,
    val alertCount: Int = 0,
) {
    val entityCount: Int get() = entities.size
}

/**
 * Home dashboard state holder. Composables no longer touch the repository
 * directly, which is what Phase 2 of the porting plan asks for.
 */
class HomeViewModel(
    private val ha: HomeAssistantRepository,
    private val sensors: SensorDatabase,
    private val lists: ManagedListsStore,
    private val snapshots: DailySnapshotStore,
) : ViewModel() {

    val climateRooms: List<RoomConfig> = DefaultRooms.climateRooms
    val basicRooms: List<RoomConfig> = DefaultRooms.basicRooms
    val rooms: List<RoomConfig> = climateRooms + basicRooms

    /** Extra entity IDs the UI asked for at runtime. */
    private var extraWatched: Set<String> = emptySet()

    /** Entities of the room sheet that is open right now, if any. */
    private var activeRoomKey: String? = null
    private var roomWatched: Set<String> = emptySet()

    /** Exposed for the bottom sheets, which still issue their own service calls. */
    val repository: HomeAssistantRepository get() = ha

    val uiState: StateFlow<HomeUiState> =
        combine(
            ha.entities,
            ha.connected,
            ha.lastError,
            lists.lights,
            lists.notif,
        ) { entities, connected, error, lights, notif ->
            HomeUiState(
                entities = entities,
                connected = connected,
                error = error,
                // AlarmLights.swift counts the managed light list, not the rooms.
                lightsOn = countOn(lights, entities),
                // GreetingHeaderView counts the managed notification list only.
                alertCount = countOn(notif, entities),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun countOn(items: List<ManagedItem>, entities: Map<String, HomeEntity>): Int =
        items.count { !it.hidden && entities[it.id]?.isOn == true }

    init {
        ha.setWatchedEntities(watchedIds())
        viewModelScope.launch { withContext(Dispatchers.IO) { sensors.prune(30) } }
    }

    /** Only these entities get realtime updates; the other ~1580 are throttled. */
    private fun watchedIds(): Set<String> {
        val ids = mutableSetOf(HumeConfig.ALARM_PRIMARY, HumeConfig.ALARM_FALLBACK)
        rooms.forEach { room ->
            ids += room.lightEntity
            ids += room.tempEntity
            ids += room.humidityEntity
            room.contactEntity?.let { ids += it }
            room.climateEntity?.let { ids += it }
        }
        ids += EnergyDetect.watchedIds()
        // The header pills read these, so they must be realtime too.
        ids += lists.watchedIds()
        ids += roomWatched
        ids += extraWatched
        return ids
    }

    /**
     * setActiveRoom() in HomeAssistantManager.swift. While a room sheet is open
     * every device inside that room is realtime and every other room is frozen;
     * closing the sheet releases both sides, so the throttling buckets keep
     * protecting the other ~1580 entities.
     */
    fun setActiveRoom(room: RoomConfig?) {
        val key = room?.rawKey
        if (key == activeRoomKey) return
        activeRoomKey = key
        roomWatched = if (room == null) emptySet() else roomEntityIds(room)
        // Watch first, then freeze: the open room must already be realtime when
        // the repository replays whatever it queued for that room.
        ha.setWatchedEntities(watchedIds())
        ha.setActiveRoom(key)
    }

    /** Every entity BubbleRoomView renders for one room. */
    private fun roomEntityIds(room: RoomConfig): Set<String> {
        val ids = mutableSetOf(room.lightEntity, room.tempEntity, room.humidityEntity)
        room.contactEntity?.let { ids += it }
        room.climateEntity?.let { ids += it }
        RoomBubbleConfig.find(room.rawKey)?.let { config ->
            config.tempEntity?.let { ids += it }
            config.humidityEntity?.let { ids += it }
            config.devices.forEach { device ->
                ids += device.entity
                device.powerEntity?.let { ids += it }
            }
        }
        return ids
    }

    /** Register additional realtime entities (energy cards, popups, charts). */
    fun watchEntities(ids: Set<String>) {
        if (ids.isEmpty() || extraWatched.containsAll(ids)) return
        extraWatched = extraWatched + ids
        ha.setWatchedEntities(watchedIds())
    }

    /** Kept for the sensor tiles: everything they render must be realtime. */
    fun watchEnergySensors(entities: Map<String, HomeEntity>) {
        val present = EnergyDetect.watchedIds().filter { entities.containsKey(it) }.toSet()
        watchEntities(present)
    }

    override fun onCleared() {
        ha.setActiveRoom(null)
        ha.setWatchedEntities(emptySet())
        super.onCleared()
    }

    fun refresh() = ha.refresh()

    fun toggleRoomLight(room: RoomConfig) =
        toggleLight(ha, room, uiState.value.entities)

    fun setLight(entityId: String, on: Boolean) = setLight(ha, entityId, on)

    fun allLightsOff() = setAllLights(ha, rooms, false)

    /** Step the air conditioner target temperature from the room card. */
    fun adjustTarget(room: RoomConfig, delta: Double) {
        val climate = room.climateEntity ?: return
        val current = uiState.value.entities.attr(climate, "temperature")?.toDoubleOrNull() ?: 26.0
        ha.setClimateTemperature(climate, (current + delta).coerceIn(16.0, 32.0))
    }

    /** Home Assistant history first, local sensor database as the offline fallback. */
    suspend fun history(entityId: String, hours: Int = 24): List<HistoryPoint> {
        val remote = ha.fetchHistory(entityId, hours)
        if (remote.isNotEmpty()) return remote
        return withContext(Dispatchers.IO) {
            sensors.history(entityId, System.currentTimeMillis() - hours * 3_600_000L)
        }
    }

    /**
     * Seven day series for the solar card, following SolarEnergyCardView.swift:
     * the six past days are immutable, so they are cached in DailySnapshotStore
     * and only fetched from history when a day is missing. Today is not cached,
     * the caller reads it live from the websocket state.
     */
    suspend fun weekly(entityId: String): List<DayValue> {
        val now = System.currentTimeMillis()
        val dayMs = 24L * 60L * 60L * 1000L
        val missing = (1..6)
            .map { DailySnapshotStore.startOfDay(now, -it) }
            .filter { snapshots.get(entityId, it) == null }

        if (missing.isNotEmpty()) {
            val points = history(entityId, 24 * 7)
            missing.forEach { dayStart ->
                val dayPoints = points.filter { it.timeMs in dayStart until (dayStart + dayMs) }
                // A daily counter peaks right before midnight, so the maximum of
                // the day is the total for that day.
                if (dayPoints.isNotEmpty()) snapshots.set(entityId, dayStart, dayPoints.maxOf { it.value })
            }
            snapshots.prune()
        }

        val past = (6 downTo 1).map { offset ->
            val dayStart = DailySnapshotStore.startOfDay(now, -offset)
            DayValue(
                label = DailySnapshotStore.dayLabel(dayStart),
                value = ((snapshots.get(entityId, dayStart) ?: 0.0) * 10).toInt() / 10.0,
                isToday = false,
            )
        }
        val today = ha.entities.value[entityId]?.numericState ?: 0.0
        return past + DayValue(
            label = DailySnapshotStore.dayLabel(now),
            value = (today * 10).toInt() / 10.0,
            isToday = true,
        )
    }
}

class HomeViewModelFactory(
    private val ha: HomeAssistantRepository,
    private val sensors: SensorDatabase,
    private val lists: ManagedListsStore,
    private val snapshots: DailySnapshotStore,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        HomeViewModel(ha, sensors, lists, snapshots) as T
}
