package com.smarthome.hume.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.smarthome.hume.core.data.HomeRepository
import com.smarthome.hume.core.data.HumeGraph
import com.smarthome.hume.core.model.DeviceUi
import com.smarthome.hume.core.model.HomeUiState
import com.smarthome.hume.core.model.RoomUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** UI-only state (chon phong, sheet, tim kiem) — du lieu that o HomeRepository. */
data class HomeScreenUi(
    val searchOpen: Boolean = false,
    val searchQuery: String = "",
    val selectedRoom: RoomUi? = null,
    val notifOpen: Boolean = false,
    val lightsOpen: Boolean = false,
    val securityExpanded: Boolean = false,
    val snackbar: String? = null,
)

class HomeViewModel(
    private val repo: HomeRepository,
) : ViewModel() {

    val state: StateFlow<HomeUiState> = repo.homeState

    private val _ui = MutableStateFlow(HomeScreenUi())
    val ui: StateFlow<HomeScreenUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch { repo.refreshSolarWeek() }
    }

    // ----- actions (uy thac xuong repo) -----
    fun toggle(entityId: String) = repo.toggle(entityId)
    fun setBrightness(entityId: String, percent: Int) = repo.setLightBrightness(entityId, percent)
    fun setClimateTemp(entityId: String, temp: Double) = repo.setClimateTemp(entityId, temp)
    fun setHvacMode(entityId: String, mode: String) = repo.setHvacMode(entityId, mode)
    fun toggleClimate(entityId: String) = repo.toggleClimate(entityId)

    fun armAlarm(mode: String, label: String) {
        repo.alarmArm(mode)
        showSnack("Đã kích hoạt: $label")
    }
    fun disarmAlarm() {
        repo.alarmDisarm()
        showSnack("Đã tắt báo động")
    }

    fun turnOffAllLights() {
        val ids = state.value.lightsOn.map { it.entityId }
        ids.forEach { repo.toggle(it) }
        showSnack("Đã tắt ${ids.size} đèn")
    }

    // ----- ui state -----
    fun openSearch(v: Boolean) = _ui.update { it.copy(searchOpen = v, searchQuery = "") }
    fun onSearchQuery(q: String) = _ui.update { it.copy(searchQuery = q) }
    fun selectRoom(room: RoomUi?) = _ui.update { it.copy(selectedRoom = room) }
    fun openNotif(v: Boolean) = _ui.update { it.copy(notifOpen = v) }
    fun openLights(v: Boolean) = _ui.update { it.copy(lightsOpen = v) }
    fun toggleSecurity() = _ui.update { it.copy(securityExpanded = !it.securityExpanded) }
    fun showSnack(msg: String) = _ui.update { it.copy(snackbar = msg) }
    fun clearSnack() = _ui.update { it.copy(snackbar = null) }

    /** Tat ca thiet bi toggle duoc (den/cong tac) de tim kiem. */
    fun searchableDevices(): List<DeviceUi> {
        val rooms = state.value.rooms
        return rooms.flatMap { r ->
            r.devices.map { d -> d.copy(sub = r.name) }
        }.distinctBy { it.entityId }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(HumeGraph.get().homeRepository) }
        }
    }
}
