package com.smarthome.hume.core.data

import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.core.model.HomeUiState
import kotlinx.coroutines.flow.StateFlow

/**
 * Nguon du lieu trang Nha. Implementation o :app (AppHomeRepository),
 * UI o :feature:home chi biet interface nay.
 */
interface HomeRepository {
    val homeState: StateFlow<HomeUiState>

    /** Trang thai tho HA theo entityId — the mo rong trong phong (rua bat /
     * den / generic) doc sensor + attribute truc tiep tu day, nhu iOS
     * GroupEntityReader. Implementation o :app: ha.entities. */
    val entityStates: StateFlow<Map<String, HomeEntity>>

    fun toggle(entityId: String)
    /** Bat/tat switch ro rang (khong lat nhu toggle). */
    fun setSwitch(entityId: String, on: Boolean)
    fun setNumber(entityId: String, value: Double)
    /** (Batch D, GAP-08) Gui datapoint LocalTuya (chi ma da biet). */
    fun setLocalTuyaDp(deviceId: String, dp: Int, value: String)
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
