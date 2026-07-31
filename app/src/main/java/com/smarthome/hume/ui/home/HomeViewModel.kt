package com.smarthome.hume.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smarthome.hume.core.ha.HistoryPoint
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.DefaultRooms
import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.core.model.RoomConfig
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
) : ViewModel() {

    val climateRooms: List<RoomConfig> = DefaultRooms.climateRooms
    val basicRooms: List<RoomConfig> = DefaultRooms.basicRooms
    val rooms: List<RoomConfig> = climateRooms + basicRooms

    /** Exposed for the bottom sheets, which still issue their own service calls. */
    val repository: HomeAssistantRepository get() = ha

    val uiState: StateFlow<HomeUiState> =
        combine(ha.entities, ha.connected, ha.lastError) { entities, connected, error ->
            HomeUiState(
                entities = entities,
                connected = connected,
                error = error,
                lightsOn = rooms.count { entities[it.lightEntity]?.isOn == true },
                alertCount = homeAlerts(entities).size,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        ha.setWatchedEntities(watchedIds())
        viewModelScope.launch { withContext(Dispatchers.IO) { sensors.prune(30) } }
    }

    /** Only these entities get realtime updates; the other ~1580 are throttled. */
    private fun watchedIds(): Set<String> {
        val ids = mutableSetOf(ALARM_ENTITY)
        rooms.forEach { room ->
            ids += room.lightEntity
            ids += room.tempEntity
            ids += room.humidityEntity
            room.contactEntity?.let { ids += it }
            room.climateEntity?.let { ids += it }
        }
        return ids
    }

    /** Add the detected energy sensors to the realtime set once they are known. */
    fun watchEnergySensors(entities: Map<String, HomeEntity>) {
        val extra = listOfNotNull(
            EnergyDetect.solarPower(entities)?.entityId,
            EnergyDetect.dailyEnergy(entities)?.entityId,
            EnergyDetect.gridPower(entities)?.entityId,
            EnergyDetect.loadPower(entities)?.entityId,
            EnergyDetect.battery(entities)?.entityId,
        )
        if (extra.isEmpty()) return
        ha.setWatchedEntities(watchedIds() + extra)
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

    fun activateScene(item: SceneItem) = runScene(ha, item)

    /** Step the air conditioner target temperature from the room card. */
    fun adjustTarget(room: RoomConfig, delta: Double) {
        val climate = room.climateEntity ?: return
        val current = uiState.value.entities.attr(climate, "temperature")?.toDoubleOrNull() ?: return
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
            DayValue(label = weekdayLabel(pair.second), value = pair.first, isToday = key == today)
        }
    }

    private fun weekdayLabel(dayOfWeek: Int): String = when (dayOfWeek) {
        Calendar.MONDAY -> "T2"
        Calendar.TUESDAY -> "T3"
        Calendar.WEDNESDAY -> "T4"
        Calendar.THURSDAY -> "T5"
        Calendar.FRIDAY -> "T6"
        Calendar.SATURDAY -> "T7"
        else -> "CN"
    }
}

class HomeViewModelFactory(
    private val ha: HomeAssistantRepository,
    private val sensors: SensorDatabase,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(ha, sensors) as T
}
