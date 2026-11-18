package com.smarthome.hume.core.ha

import com.smarthome.hume.core.model.HomeEntity

/**
 * UpdatePolicy — v81, port 1:1 iOS `Hume/Core/UpdatePolicy.swift` (v81, 85f91a2).
 *
 * Mot bang phan loai DUY NHAT cho nhip render toi da cua tung entity.
 * Tang = toc do RENDER toi da, khong phai toc do poll: toc do thuc =
 * min(toc do WS ve, toc do tang). Gia tri khong doi thi khong ve lai
 * (distinctUntilChanged — xem [entityEquals]) o moi tang.
 *
 * Tang dung o 2 cho (cung 1 bang):
 *   - Tang doc: [HomeAssistantRepository.publishRealtime] throttle theo
 *     tung entity thay cho uiThrottle 2.0s phang cu; [donutFor] trong
 *     AppEnergyRepository chi rebuild donut khi input doi + het nhip tang.
 *   - Tang WS: [HomeAssistantRepository.bucketFor] se suy ra tu bang nay
 *     o worker WS (chua lam o day — bucket hien tai giu nguyen).
 *
 * LUU Y khi sua pattern: "power" xuat hien trong "powerwall" — KHONG
 * dung contains("power") tran (se keo ca 205 entity BMS ve T1).
 * Dung hau to/doan "_power", "_current", "_voltage" de chi khop
 * sensor cong suat/dong/ap that (giong iOS).
 */
enum class UpdateTier(val throttleMs: Long) {
    /** T0 — 0s: dieu khien/trang thai, phan hoi cung frame. */
    INSTANT(0L),

    /** T1 — 1s: cong suat/dong/ap (tween 600ms noi thanh lien tuc). */
    FAST(1_000L),

    /** T2 — 5s: SOC/nhiet/kWh tong (doi cham, mat khong phan biet 1s/5s). */
    NORMAL(5_000L),

    /** T3 — 60s/chi khi doi: kWh ngay/thang, chi phi, gia, lich su. */
    SLOW(60_000L),
}

object UpdatePolicy {

    /**
     * Nhip day payload Powerwall vao webview — KHONG phai throttle tang:
     * 2s la nhip thiet ke cua tween HTML 600ms (iOS v66), khai bao tuong
     * minh de khong ai nham la magic number. Port iOS
     * `UpdatePolicy.powerwallPayloadCadence`. Worker Powerwall so huu
     * (PowerwallViewModel.DATA_THROTTLE_MS) — khai bao o day de vong sau
     * doc tu policy, KHONG dung o day.
     */
    const val POWERWALL_PAYLOAD_CADENCE_MS = 2_000L

    /**
     * Sensor cong suat/dong/ap can ve nhanh nhung khong khop pattern
     * chung (ten tieng Viet / ten dem thiet bi). Port iOS
     * `UpdatePolicy.fastExact`.
     */
    private val FAST_EXACT: Set<String> = setOf(
        "sensor.cong_suat_nha", // cong suat nha (ten TV, khong co "power")
        "sensor.dashboard_active_card", // so thiet bi dang chay
        "sensor.dashboard_active_card_2",
    )

    /**
     * Sensor doi cham nhung khong khop pattern T2/T3 (dat day de khoi
     * roi ve default nhanh). Port iOS `UpdatePolicy.slowExact`.
     */
    private val SLOW_EXACT: Set<String> = setOf(
        "sensor.evn_current_unit_price", // gia EVN (doi theo ky tinh tien)
    )

    /**
     * 5 sensor the rua bat Texgio — trang thai/chuong trinh/giai doan
     * phai ve nhanh de nut va so tren the doi theo. Android khong co
     * DishwasherLiveManager nhu iOS nen liet ke literal tai day (copy
     * tu nhanh bucketFor trong HomeAssistantRepository).
     */
    val DISHWASHER_SENSOR_IDS: Set<String> = setOf(
        "sensor.texgio_tgfpcm795b_g_trang_thai",
        "sensor.texgio_tgfpcm795b_g_giai_doan_rua",
        "sensor.texgio_tgfpcm795b_g_thoi_gian_con_lai",
        "sensor.texgio_tgfpcm795b_g_nhiet_do",
        "sensor.texgio_tgfpcm795b_g_chuong_trinh",
    )

    /** Trang thai inverter Solis — bao mat dien phai tuc thoi (port iOS). */
    const val SOLIS_STATUS_ID = "sensor.solis_s6_eh1p_status_string_3"

    /**
     * Tang render cua 1 entity.
     *
     * @param tempAlertSensorId cam bien nhiet do TempAlert dang theo doi
     * (HomeAssistantRepository.tempAlertSensorId, do TempAlertWatcher cap
     * nhat theo cau hinh nguoi dung; mac dinh
     * TempAlertConfig.DEFAULT_SENSOR). iOS doc thang TempAlertConfig.sensor;
     * Android truyen vao vi gia tri nay doi luc chay. Null = khong biet,
     * sensor do roi ve T2 nhu cam bien nhiet do thuong.
     */
    fun tierFor(entityId: String, tempAlertSensorId: String? = null): UpdateTier {
        val dot = entityId.indexOf('.')
        if (dot < 0) return UpdateTier.INSTANT
        val domain = entityId.substring(0, dot)

        // ── T0: dieu khien + trang thai + hien dien ──────────────────
        when (domain) {
            "light", "switch", "fan", "cover", "lock", "climate",
            "alarm_control_panel", "binary_sensor", "number", "scene",
            "person" -> return UpdateTier.INSTANT
            "sensor", "input_number" -> { /* xuong pattern ben duoi */ }
            else -> return UpdateTier.INSTANT
        }

        // Frigate: phat hien an ninh — tuc thi.
        if (entityId.startsWith("sensor.frigate_")) return UpdateTier.INSTANT
        // The rua bat: 5 sensor tren.
        if (entityId in DISHWASHER_SENSOR_IDS) return UpdateTier.INSTANT
        // Trang thai inverter Solis — bao mat dien phai tuc thoi.
        if (entityId == SOLIS_STATUS_ID) return UpdateTier.INSTANT
        // Cam bien canh bao nhiet do phong da chon — tuc thi.
        if (tempAlertSensorId != null && entityId == tempAlertSensorId) {
            return UpdateTier.INSTANT
        }

        // ── T1: cong suat/dong/ap ───────────────────────────────────
        if (entityId in FAST_EXACT) return UpdateTier.FAST
        if (entityId.endsWith("_power") || entityId.endsWith("_power_2") ||
            entityId.endsWith("_power_3") || entityId.contains("_power_") ||
            entityId.contains("_current") || entityId.contains("_voltage")
        ) {
            return UpdateTier.FAST
        }

        // ── T3: kWh ngay/thang, chi phi, gia, lich su, thong tin tinh ─
        if (entityId in SLOW_EXACT) return UpdateTier.SLOW
        if (entityId.endsWith("_daily") ||
            entityId.contains("_daily_energy") || entityId.contains("_energy_daily") ||
            entityId.contains("_today_energy") ||
            entityId == "sensor.grid_cost" || entityId == "sensor.home_cost" ||
            entityId.endsWith("_billing") || entityId.endsWith("_billing_2") ||
            entityId.contains("_yesterday_") ||
            entityId.contains("firmware") || entityId.contains("_version")
        ) {
            return UpdateTier.SLOW
        }
        if (domain == "input_number") return UpdateTier.SLOW

        // ── T2: con lai — SOC/SOH/nhiet/do am/kWh tong/cell ──────────
        // (mac dinh sensor: nhiet do, SOC, cell, TDS, lit loc, thoi gian
        // con lai… deu doi cham; 5s du tuoi ma giam ~80% lan ve so voi 1s).
        return UpdateTier.NORMAL
    }

    /** Nhip render toi da (ms) cua 1 entity: 0 / 1000 / 5000 / 60000. */
    fun throttleMsFor(entityId: String, tempAlertSensorId: String? = null): Long =
        tierFor(entityId, tempAlertSensorId).throttleMs

    /**
     * distinctUntilChanged cho moi tang (ENTITY-STORE §2.2).
     * HomeEntity la data class co lastChanged/lastUpdated nen equals
     * mac dinh van doi moi event ke ca khi state giong nhau — chi so
     * state + attributes nhu dedup trong HomeAssistantRepository
     * .applyUpdates(). Null == null la bang nhau.
     */
    fun entityEquals(a: HomeEntity?, b: HomeEntity?): Boolean {
        if (a === b) return true
        if (a == null || b == null) return false
        return a.state == b.state && a.attributes == b.attributes
    }

    /**
     * Doi availability (T0, iOS §2): `unavailable` <-> co gia tri la
     * chuyen trang thai hiem nen luon publish ngay bat ke tang cua
     * entity (distinctUntilChanged lam no gan nhu mien phi).
     */
    fun availabilityChanged(oldState: String?, newState: String): Boolean =
        (oldState == "unavailable") != (newState == "unavailable")
}
