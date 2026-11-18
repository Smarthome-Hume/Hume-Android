package com.smarthome.hume.feature.energy

import com.smarthome.hume.core.model.HumeConfig
import com.smarthome.hume.feature.energy.powerwall.PowerwallLiveData

/**
 * Watched set cua the pin Home — v81 muc 7 (don grab-bag).
 *
 * CHU SO HUU: the pin Home (`AppHomeRepository.buildState` — cong thuc
 * thoi gian sac/xa `batteryTimeCalc` + SOC / cong suat / limit hieu dung,
 * hien thi o `feature/home/BatteryCard`). Truoc day 31 + 8 id nay chi nam
 * rai rac trong `AppHomeRepository.updateWatched` (N-03 + PV/pin +
 * BACKUP_SOC) — gio tap hop ve 1 cho, store chi union
 * (`AppHomeRepository.updateWatched` se doi sang doc tu day — worker
 * tab-scoped, khong phai commit nay).
 *
 * - [timeCalcIds]: 31 entity toi thieu cho cong thuc thoi gian sac/xa,
 *   port iOS `PowerwallLiveData.timeCalcEntityIds`. LA TAP CON cua
 *   [PowerwallLiveData.entityIds] (205) — 11 setting lay truc tiep tu
 *   [PowerwallLiveData.settingEntityIds] (khong lap literal), 20 id con
 *   lai liet ke tuong minh de dem duoc 31.
 * - [solisIds]: 8 id Solis the pin doc (PV + pin + limit hieu dung).
 * - [sharedIds]: 2 id doc CHUNG voi tab Nang luong (chieu sac/xa +
 *   mat luoi) — chu chinh o day, [EnergyWatchedIds] cong them chu
 *   khong lap literal.
 */
object HomeBatteryWatchedIds {

    /** 31 entity toi thieu cho cong thuc thoi gian sac/xa (N-03). */
    val timeCalcIds: Set<String> = buildSet {
        val master = "sensor.powerwall_bms_master_"
        add(master + "py_system_soc")
        add(master + "py_system_voltage")
        add(master + "py_system_current")
        add(master + "cell_max")
        for (pack in listOf("master", "pack_1", "pack_3", "pack_4")) {
            for (key in listOf(
                "full_charge_cap",
                "remaining_cap",
                "charge_current_limit",
                "discharge_current_limit",
            )) {
                add("sensor.powerwall_bms_${pack}_$key")
            }
        }
        // Setting BMS live tren master — dung chung
        // PowerwallLiveData.settingEntityIds, khong lap lai literal.
        addAll(PowerwallLiveData.settingEntityIds)
    }

    /** 2 id Solis doc chung the pin Home + tab Nang luong. */
    val sharedIds: Set<String> = setOf(
        "sensor.battery_current_flow", // Chieu sac/xa (dong pin)
        "sensor.solis_s6_eh1p_status_string_3", // Trang thai inverter / mat luoi
    )

    /** Nguong ep xa khi mat dien — limit hieu dung luc xa cua the pin Home
     * (cung la 1 dieu khien xa trong [EnergyWatchedIds.dischargeControlIds],
     * o do chi cong them hang so nay, khong lap literal). */
    const val OVERDISCHARGE_SOC = "number.solis_s6_eh1p_overdischarge_soc_2"

    /** 8 id Solis the pin Home doc (PV + pin + limit hieu dung). */
    val solisIds: Set<String> = setOf(
        HumeConfig.PV_POWER,
        HumeConfig.PV_TODAY,
        HumeConfig.BATTERY_SOC,
        HumeConfig.BATTERY_POWER,
        HumeConfig.BACKUP_SOC,
        OVERDISCHARGE_SOC, // Nguong ep xa khi mat dien
    ) + sharedIds

    /** Toan bo 39 entity the pin Home can watch (31 + 8, khong trung nhau). */
    val all: Set<String> = timeCalcIds + solisIds
}
