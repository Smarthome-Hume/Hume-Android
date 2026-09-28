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
    /** Ghi de che do Sac/Xa khi user bam node pin tren flow card (demo: toggle flNodeBatt). */
    val battChargeOverride: Boolean? = null,
    /** Do lech SOC tich luy tu tick 2.8s khi dang override (+0.4 sac / -0.3 xa). */
    val socDrift: Double = 0.0,
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

    /** Bam node pin tren flow card: doi Sac/Xa (badge, icon, chieu sweep) — demo flNodeBatt. */
    fun toggleBattFlow() {
        val cur = _ui.value.battChargeOverride ?: state.value.flow.battCharging
        _ui.update { it.copy(battChargeOverride = !cur, socDrift = 0.0) }
        ensureSocTicker()
    }

    private var socJob: kotlinx.coroutines.Job? = null

    /** Tick 2.8s: SOC +0.4%/tick khi sac, -0.3%/tick khi xa (chi khi user dang override). */
    private fun ensureSocTicker() {
        if (socJob != null) return
        socJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(2800)
                _ui.update { u ->
                    val ov = u.battChargeOverride ?: return@update u
                    val next = (state.value.flow.soc + u.socDrift +
                        if (ov) 0.4 else -0.3).coerceIn(5.0, 100.0)
                    u.copy(socDrift = next - state.value.flow.soc)
                }
            }
        }
    }

    fun showSnack(msg: String) = _ui.update { it.copy(snackbar = msg) }
    fun clearSnack() = _ui.update { it.copy(snackbar = null) }

    companion object {
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer { EnergyViewModel(HumeGraph.get().energyRepository) }
        }
    }
}
