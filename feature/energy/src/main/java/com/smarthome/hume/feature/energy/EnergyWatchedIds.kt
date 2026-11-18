package com.smarthome.hume.feature.energy

import com.smarthome.hume.core.model.HumeConfig

/**
 * Watched set cua tab Nang luong — v81 muc 7 (don grab-bag).
 *
 * CHU SO HUU: tab Nang luong (`AppEnergyRepository` doc cac nhom nay cho
 * EnergyFlow / Donut / Cost / BatteryInfo / dieu khien sac-xa / tab Nuoc).
 * Truoc day cac id nay chi nam rai rac trong `AppHomeRepository.updateWatched`
 * (Flow tab Dien, Batch A cost, Batch B1 dieu khien sac/xa, Batch C nuoc RO)
 * + literal doc truc tiep trong `AppEnergyRepository` — gio tap hop ve 1
 * cho, store chi union (`AppHomeRepository.updateWatched` se doi sang doc
 * tu day — worker tab-scoped, khong phai commit nay).
 *
 * LUU Y:
 * - Donut KHONG co list tinh: no quet dong tat ca `sensor.*_power` trong
 * entities (xem `AppEnergyRepository.donutFor`) nen khong liet ke o day;
 * chi 4 id trong [donutMappedIds] la map cung (explicitEnergyMap) donut
 * can de doi power -> energy.
 * - `sensor.battery_current_flow` + `sensor.solis_s6_eh1p_status_string_3`
 * doc CHUNG voi the pin Home (chieu sac/xa + mat luoi) — chu chinh la
 * [HomeBatteryWatchedIds.sharedIds], o day chi cong them, khong lap
 * literal.
 * - [HumeConfig.BACKUP_SOC] vua la dieu khien sac trong [chargeControlIds]
 * vua la limit hieu dung cua the pin Home — dung hang so chung, khong
 * lap literal.
 */
object EnergyWatchedIds {

    /** 16 sensor flow tab Dien (PV / luoi / nha / pin / aptomat). */
    val flowIds: Set<String> = setOf(
        "sensor.aptomat_tong_power",
        "sensor.cong_suat_nha",
        "sensor.solis_s6_eh1p_pv_power_1_3",
        "sensor.solis_s6_eh1p_pv_power_2_3",
        "sensor.aptomat_t1_power",
        "sensor.aptomat_t2_power",
        "sensor.aptomat_t3_power",
        "sensor.energy_home_daily",
        "sensor.solis_s6_eh1p_today_battery_charge_energy_2",
        "sensor.solis_s6_eh1p_meter_active_power",
        "sensor.solis_s6_eh1p_household_load_power_2",
        "sensor.solis_s6_eh1p_backup_load_power_2",
        "sensor.solis_s6_eh1p_household_load_today_energy_2",
        "sensor.solis_s6_eh1p_backup_load_today_energy_2",
        "sensor.solis_s6_eh1p_today_battery_discharge_energy_2",
        "sensor.aptomat_tong_daily",
    ) + HomeBatteryWatchedIds.sharedIds

    /** 5 sensor chi phi / gia dien (the Chi phi). */
    val costIds: Set<String> = setOf(
        "sensor.grid_cost",
        "sensor.home_cost",
        "sensor.evn_current_unit_price",
        "sensor.grid_import_billing",
        "sensor.home_import_billing",
    )

    /** 3 sensor thong tin pin (dien ap + gioi han dong BMS). */
    val batteryInfoIds: Set<String> = setOf(
        "sensor.solis_s6_eh1p_battery_voltage_2",
        "sensor.solis_s6_eh1p_battery_charge_current_limitation_bms_2",
        "sensor.solis_s6_eh1p_battery_discharge_current_limitation_bms_2",
    )

    /** 9 dieu khien sac (2 expander Sac pin + BACKUP_SOC). */
    val chargeControlIds: Set<String> = setOf(
        "switch.allow_grid_to_charge_the_battery_2",
        "number.solis_s6_eh1p_battery_max_charge_current_2",
        "number.solis_s6_eh1p_grid_time_of_use_charge_battery_current_slot_1_2",
        "number.solis_s6_eh1p_grid_time_of_use_charge_cut_off_soc_slot_1_2",
        "switch.grid_time_of_use_charging_period_1_2",
        "time.solis_s6_eh1p_grid_time_of_use_charge_start_slot_1_2",
        "time.solis_s6_eh1p_grid_time_of_use_charge_end_slot_1_2",
        "number.solis_s6_eh1p_force_charge_soc_2",
        HumeConfig.BACKUP_SOC,
    )

    /** 3 dieu khien xa (expander Xa pin). */
    val dischargeControlIds: Set<String> = setOf(
        "number.solis_s6_eh1p_battery_max_discharge_current_2",
        "number.solis_s6_eh1p_off_grid_overdischarge_soc_2",
        // Nguong ep xa khi mat dien — chu chinh la the pin Home (limit
        // hieu dung luc xa), o day chi cong them, khong lap literal.
        HomeBatteryWatchedIds.OVERDISCHARGE_SOC,
    )

    /** 4 id map cung power -> energy cua Donut (explicitEnergyMap). */
    val donutMappedIds: Set<String> = setOf(
        "sensor.air_condition_current_extrapolated_power",
        "sensor.air_condition_daily_energy_ac",
        "sensor.dieu_hoa_power",
        "sensor.dieu_hoa_power_energy",
    )

    /** Toan bo entity tab Nang luong can watch (50 id duy nhat). */
    val all: Set<String> =
        flowIds + costIds + batteryInfoIds + chargeControlIds +
            dischargeControlIds + donutMappedIds + WaterWatchedIds.all
}
