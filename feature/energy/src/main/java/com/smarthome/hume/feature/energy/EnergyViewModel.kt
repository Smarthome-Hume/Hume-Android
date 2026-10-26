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

// Thu tu theo iOS (M3EEnergyView allCases: solar -> electric -> water).
// Batch C them Water (GAP-01); mac dinh van la Dien luoi nhu iOS
// (.electric), khong phai tab dau tien.
enum class EnergySubTab { Solar, Electric, Water }
enum class DeviceMode { Power, Energy }

data class EnergyScreenUi(
    val tab: EnergySubTab = EnergySubTab.Electric,
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
    /** Quet lai thiet bi moi 15s (port iOS DevicesCard.task). */
    fun refreshDevices() = repo.refreshDevices()
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
