package com.smarthome.hume.core.data

import com.smarthome.hume.core.model.HomeUiState
import kotlinx.coroutines.flow.StateFlow

/**
 * Nguon du lieu trang Nha. Implementation o :app (AppHomeRepository),
 * UI o :feature:home chi biet interface nay.
 */
interface HomeRepository {
    val homeState: StateFlow<HomeUiState>

    fun toggle(entityId: String)
    /** Bat/tat switch ro rang (khong lat nhu toggle). */
    fun setSwitch(entityId: String, on: Boolean)
    fun setNumber(entityId: String, value: Double)
    fun setLightBrightness(entityId: String, percent: Int)
    fun setClimateTemp(entityId: String, tempC: Double)
    fun setHvacMode(entityId: String, mode: String)
    fun toggleClimate(entityId: String)
    /** mode: "home" / "away" / "night" / "custom_bypass" */
    fun alarmArm(mode: String)
    fun alarmDisarm()
    /** Tai lai bieu do 7 ngay (history cham). */
    fun refreshSolarWeek()
}
