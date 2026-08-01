package com.smarthome.hume.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smarthome.hume.core.ha.HistoryPoint
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.DefaultRooms
import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.core.model.HumeConfig
import com.smarthome.hume.core.model.RoomConfig
import com.smarthome.hume.core.scene.ManagedItem
import com.smarthome.hume.core.scene.ManagedListsStore
import com.smarthome.hume.core.storage.SensorDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

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
) : ViewModel() {

    val climateRooms: List<RoomConfig> = DefaultRooms.climateRooms
    val basicRooms: List<RoomConfig> = DefaultRooms.basicRooms
    val rooms: List<RoomConfig> = climateRooms + basicRooms

    /** Extra entity IDs the UI asked for at runtime. */
    private var extraWatched: Set<String> = emptySet()

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
        ids += extraWatched
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

    /** Seven day series for the solar card: one bar per day, highest reading wins. */
    suspend fun weekly(entityId: String): List<DayValue> {
        val points = history(entityId, 24 * 7)
        if (points.isEmpty()) return emptyList()
        val calendar = Calendar.getInstance()
        val buckets = LinkedHashMap<String, Pair<Double, Int>>()
        points.forEach { point ->
            calendar.timeInMillis = point.timeMs
            val key = calendar.get(Calendar.YEAR).toString() + "-" + calendar.get(Calendar.DAY_OF_YEAR)
            val weekday = calendar.get(Calendar.DAY_OF_WEEK)
            val previous = buckets[key]?.first ?: Double.NEGATIVE_INFINITY
            buckets[key] = maxOf(previous, point.value) to weekday
        }
        val today = Calendar.getInstance().let { it.get(Calendar.YEAR).toString() + "-" + it.get(Calendar.DAY_OF_YEAR) }
        return buckets.entries.takeLast(7).map { (key, pair) ->
            DayValue(
                label = HumeConfig.dayNames[pair.second - 1],
                value = (pair.first * 10).toInt() / 10.0,
                isToday = key == today,
            )
        }
    }
}

class HomeViewModelFactory(
    private val ha: HomeAssistantRepository,
    private val sensors: SensorDatabase,
    private val lists: ManagedListsStore,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        HomeViewModel(ha, sensors, lists) as T
}
