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
data class Snack(
    val msg: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
)

data class HomeScreenUi(
    val searchOpen: Boolean = false,
    val searchQuery: String = "",
    val selectedRoom: RoomUi? = null,
    val notifOpen: Boolean = false,
    val lightsOpen: Boolean = false,
    val securityExpanded: Boolean = false,
    val snackbar: Snack? = null,
    val isRefreshing: Boolean = false,
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
        val n = turnOffAllLightsSilent()
        showSnack("Đã tắt $n đèn")
    }

    private fun turnOffAllLightsSilent(): Int {
        val ids = state.value.lightsOn.map { it.entityId }
        ids.forEach { repo.toggle(it) }
        return ids.size
    }

    /** FAB "Bat den": bat den cac phong dang tat + snackbar demo "Da bat den". */
    fun turnOnAllLights() {
        val ids = state.value.rooms.mapNotNull { r ->
            r.lightEntityId?.takeIf { !r.lightOn }
        }.distinct()
        ids.forEach { repo.toggle(it) }
        showSnack("Đã bật đèn", "Hoàn tác") {
            ids.forEach { repo.toggle(it) }
            showSnack("Đã hoàn tác")
        }
    }

    /** FAB "Dieu hoa 26°": dat 26° cho moi dieu hoa + snackbar demo. */
    fun ac26() {
        val climates = state.value.rooms.mapNotNull { it.climate?.entityId }
        climates.forEach { repo.setClimateTemp(it, 26.0) }
        showSnack("Đã bật điều hoà 26°", "Hoàn tác") {
            showSnack("Đã hoàn tác")
        }
    }

    /** FAB "Bat an ninh": arm away + snackbar demo "Da bat an ninh". */
    fun armAwayQuick() {
        repo.alarmArm("away")
        showSnack("Đã bật an ninh", "Hoàn tác") {
            repo.alarmDisarm()
            showSnack("Đã hoàn tác")
        }
    }

    /** FAB: tiet kiem dien — goi SAU dialog xac nhan "Bat tiet kiem dien?". */
    fun ecoMode() {
        turnOffAllLightsSilent()
        showSnack("Đã bật tiết kiệm điện", "Hoàn tác") {
            showSnack("Đã tắt tiết kiệm điện")
        }
    }

    /** Pull-to-refresh: tai lai du lieu (demo #ptr morphloader). */
    fun refresh() {
        _ui.update { it.copy(isRefreshing = true) }
        repo.refreshSolarWeek()
        viewModelScope.launch {
            kotlinx.coroutines.delay(1200)
            _ui.update { it.copy(isRefreshing = false) }
        }
    }

    // ----- ui state -----
    fun openSearch(v: Boolean) = _ui.update { it.copy(searchOpen = v, searchQuery = "") }
    fun onSearchQuery(q: String) = _ui.update { it.copy(searchQuery = q) }
    fun selectRoom(room: RoomUi?) = _ui.update { it.copy(selectedRoom = room) }
    fun openNotif(v: Boolean) = _ui.update { it.copy(notifOpen = v) }
    fun openLights(v: Boolean) = _ui.update { it.copy(lightsOpen = v) }
    fun toggleSecurity() = _ui.update { it.copy(securityExpanded = !it.securityExpanded) }
    fun showSnack(msg: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) =
        _ui.update { it.copy(snackbar = Snack(msg, actionLabel, onAction)) }
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
