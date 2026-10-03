package com.smarthome.hume.feature.energy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.smarthome.hume.core.data.EnergyRepository
import com.smarthome.hume.core.data.HumeGraph
import com.smarthome.hume.core.model.EnergyUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class EnergySubTab { Cons, Solar }
enum class DeviceMode { Power, Energy }

data class EnergyScreenUi(
    val tab: EnergySubTab = EnergySubTab.Cons,
    val deviceMode: DeviceMode = DeviceMode.Power,
    val chargeOpen: Boolean = false,
    val dischargeOpen: Boolean = false,
    val snackbar: String? = null,
)

class EnergyViewModel(
    private val repo: EnergyRepository,
) : ViewModel() {

    val state: StateFlow<EnergyUiState> = repo.energyState

    private val _ui = MutableStateFlow(EnergyScreenUi())
    val ui: StateFlow<EnergyScreenUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch { repo.refreshHistory() }
    }

    fun setTab(tab: EnergySubTab) = _ui.update { it.copy(tab = tab) }
    fun setDeviceMode(mode: DeviceMode) = _ui.update { it.copy(deviceMode = mode) }
    fun toggleCharge() = _ui.update { it.copy(chargeOpen = !it.chargeOpen) }
    fun toggleDischarge() = _ui.update { it.copy(dischargeOpen = !it.dischargeOpen) }

    fun toggle(entityId: String) = repo.toggle(entityId)
    fun setNumber(entityId: String, value: Double) = repo.setNumber(entityId, value)
    fun setTime(entityId: String, time: String) = repo.setTime(entityId, time)

    fun showSnack(msg: String) = _ui.update { it.copy(snackbar = msg) }
    fun clearSnack() = _ui.update { it.copy(snackbar = null) }

    companion object {
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer { EnergyViewModel(HumeGraph.get().energyRepository) }
        }
    }
}
