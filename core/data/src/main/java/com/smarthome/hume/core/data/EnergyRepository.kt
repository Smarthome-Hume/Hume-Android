package com.smarthome.hume.core.data

import com.smarthome.hume.core.model.EnergyUiState
import kotlinx.coroutines.flow.StateFlow

/** Nguon du lieu cho tab Dien (demo v4) — UI chi doc state nay. */
interface EnergyRepository {
    val energyState: StateFlow<EnergyUiState>
    fun toggle(entityId: String)
    fun setNumber(entityId: String, value: Double)
    fun setTime(entityId: String, time: String)
    fun refreshHistory()
}
