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
    /** (N-09) Refetch 12 entity rua bat o 1s + 4s sau lenh — port iOS
     * RoomExpandedCards.refreshAfterCommand(). Khong block UI. */
    fun refreshDishwasherAfterCommand()
    fun setLightBrightness(entityId: String, percent: Int)
    /** (Batch D, GAP-09) Mau RGB + hieu ung cho the den mo rong. */
    fun setLightRgb(entityId: String, r: Int, g: Int, b: Int)
    fun setLightEffect(entityId: String, effect: String)
    fun setClimateTemp(entityId: String, tempC: Double)
    fun setHvacMode(entityId: String, mode: String)
    fun toggleClimate(entityId: String)
    /** (GAP-30) Bat/tat toan bo den trong phong bang 1 lenh light.turn_on/off
     * voi entity_id = toan bo danh sach (port iOS HomeRooms.swift:241-248). */
    fun toggleRoomLights(entityIds: List<String>)
    /** mode: "home" / "away" / "night" / "custom_bypass" */
    fun alarmArm(mode: String)
    fun alarmDisarm()
    /** Tai lai bieu do 7 ngay (history cham). */
    fun refreshSolarWeek()
}
