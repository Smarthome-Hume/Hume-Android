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

    val rooms: List<RoomConfig> = DefaultRooms.climateRooms + DefaultRooms.basicRooms

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

    /** Home Assistant history first, local sensor database as the offline fallback. */
    suspend fun history(entityId: String, hours: Int = 24): List<HistoryPoint> {
        val remote = ha.fetchHistory(entityId, hours)
        if (remote.isNotEmpty()) return remote
        return withContext(Dispatchers.IO) {
            sensors.history(entityId, System.currentTimeMillis() - hours * 3_600_000L)
        }
    }
}

class HomeViewModelFactory(
    private val ha: HomeAssistantRepository,
    private val sensors: SensorDatabase,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(ha, sensors) as T
}
