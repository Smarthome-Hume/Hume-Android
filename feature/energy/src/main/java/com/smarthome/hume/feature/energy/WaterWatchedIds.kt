package com.smarthome.hume.feature.energy

/**
 * Watched set cua tab Nuoc (RO) — v81 muc 7 (don grab-bag).
 *
 * CHU SO HUU: tab Nuoc trong Energy (`AppEnergyRepository` doc nhom nay
 * cho WaterUi). Truoc day 8 id nay chi nam rai rac trong
 * `AppHomeRepository.updateWatched` (Batch B/C) + `AppEnergyRepository`
 * (doc truc tiep literal) — gio tap hop ve 1 cho, store chi union.
 *
 * KHONG bao gom `sensor.r_ro_m_01_3` / `_5` / `_6`: khong co noi nao doc
 * (chi `_2` nhiet do, `_4` lit hom nay, `_7` % loc, `_8` lit con lai +
 * TDS `_01` duoc dung).
 */
object WaterWatchedIds {

    /** 5 sensor RO: TDS, nhiet do, lit hom nay, % loc, lit con lai. */
    val sensorIds: Set<String> = setOf(
        "sensor.r_ro_m_01", // TDS
        "sensor.r_ro_m_01_2", // Nhiet do nuoc
        "sensor.r_ro_m_01_4", // Lit loc hom nay (reset hang ngay)
        "sensor.r_ro_m_01_7", // % loc con lai
        "sensor.r_ro_m_01_8", // Lit loc con lai
    )

    /** 3 binary_sensor: bom, nuoc vao, nuoc su dung. */
    val binarySensorIds: Set<String> = setOf(
        "binary_sensor.r_ro_m_01", // Bom
        "binary_sensor.r_ro_m_01_2", // Nuoc vao
        "binary_sensor.r_ro_m_01_3", // Nuoc su dung
    )

    /** Toan bo 8 entity tab Nuoc can watch. */
    val all: Set<String> = sensorIds + binarySensorIds
}
