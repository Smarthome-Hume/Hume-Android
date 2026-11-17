package com.smarthome.hume.feature.energy.powerwall

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.core.ui.theme.HumeExtraColors
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Live HA data + M3E theme cho trang mo phong Powerwall 3D — port 1:1 iOS
 * `Hume/Powerwall/PowerwallLiveData.swift` (v70) + `BatterySettings` trong
 * `Hume/M3E/Tabs/BatteryTimeCalc.swift`.
 *
 * Anh xa pack (theo friendly_name tren HA + anh that cua user):
 *   master = BMS4 (cum tren cung, co LCD), pack_1 = Slave 1 (BMS0),
 *   pack_3 = Slave 3 (BMS2), pack_4 = Slave 4 (BMS3).
 *   pack_2 = Slave 2 (BMS1) la slot TRONG — sensor toan "unknown", khong render.
 *
 * Du lieu KHONG lay tu snapshot nhung trong bundle: khi mo trang, app doc
 * sensor powerwall_bms_* tu HomeRepository.entityStates (snapshot REST luc
 * mo + WebSocket sau do) roi day vao WebView qua
 * window.__humePowerwallLive (bridge trong index.html). Mau sac the theo
 * theme app qua window.__humePowerwallTheme.
 *
 * WATCH FULL (N-12, da port): danh sach [entityIds] la nguon su that duy nhat
 * cho tap watched (205 entity = 4 pack x 46 key + 4 id master + 6 system
 * + 11 setting), dang ky vao updateWatched qua POWERWALL_ENTITY_IDS trong
 * AppHomeRepository — cac entity nay tick live theo WebSocket; snapshot
 * REST /api/states luc mo trang chi la gia tri ban dau, khong phai nguon
 * duy nhat. (Sua comment stale: ban truoc ghi "chua watch full" khi N-12
 * chua land; N-12 da land cung dot port nay.)
 */
object PowerwallLiveData {

    // MARK: - Entity IDs

    /** Prefix HA cua tung pack hien thi (key trang -> prefix entity). */
    val unitPrefixes: List<Triple<String, String, String>> = listOf(
        Triple("master", "sensor.powerwall_bms_master_", "Master"),
        Triple("s1", "sensor.powerwall_bms_pack_1_", "Slave 1"),
        Triple("s3", "sensor.powerwall_bms_pack_3_", "Slave 3"),
        Triple("s4", "sensor.powerwall_bms_pack_4_", "Slave 4"),
    )

    /** Key sensor dung chung cho moi pack (theo ESP32 page). */
    private val packKeys: List<String> = listOf(
        "soc", "soh", "voltage", "current",
        "temp_avg", "temp_cell_1", "temp_cell_2", "temp_cell_3", "temp_max", "temp_min",
        "cell_max", "cell_min", "cell_spread",
        "status", "error_id", "output",
        "full_charge_cap", "remaining_cap",
        "charge_current_limit", "charge_voltage_limit", "discharge_current_limit",
        "adc_battery_volt", "adc_fuse_volt", "adc_pack_volt",
        "board_temp", "fet_temp", "charge_fet", "discharge_fet", "precharge_fet",
        "firmware_version",
    ) + (1..16).map { "cell_$it" }

    /**
     * Setting BMS live tren master (doc that tu BMS, KHONG hardcode).
     * Da kiem chung 19:44 16/11 tren iOS: khop 100% so hardcode cu trong app.
     * charge_voltage_limit da co trong packKeys nen KHONG lap lai o day.
     * setting_overvoltage + py_dchg_voltage_limit dang unknown nen khong lay.
     *
     * Khai bao TRUOC [entityIds]: initializer cua [entityIds] dung
     * [settingEntityIds], ma property trong object chi khoi tao theo thu tu
     * khai bao — de sau thi kotlinc bao "must be initialized".
     */
    val settingEntityIds: List<String> = run {
        val mp = "sensor.powerwall_bms_master_"
        listOf(
            "setting_charge_cv", "setting_cutoff_soc", "setting_cutoff_c_rate",
            "setting_charge_limit_c_rate", "setting_discharge_cutoff_soc",
            "setting_discharge_limit_c_rate", "setting_cell_max_volt",
            "setting_cell_min_volt", "setting_cutoff_volt", "setting_undervoltage",
            "py_chg_current_limit",
        ).map { mp + it }
    }

    /** Toan bo entity can watch (N-12 dang ky vao watched set). */
    val entityIds: List<String> = buildList {
        for ((_, prefix, _) in unitPrefixes) {
            for (key in packKeys) add(prefix + key)
        }
        // Master-only: id cua cell/temp cao-thap (slave tu suy tu danh sach).
        for (key in listOf("temp_max_cell_id", "temp_min_cell_id", "cell_max_id", "cell_min_id")) {
            add("sensor.powerwall_bms_master_$key")
        }
        // System-level (inverter-facing, Pylontech).
        for (key in listOf(
            "py_system_soc", "py_system_soh", "py_system_voltage",
            "py_system_current", "py_protection_flags", "py_alarm_flags",
        )) {
            add("sensor.powerwall_bms_master_$key")
        }
        addAll(settingEntityIds)
    }

    // MARK: - Payload {sys, packs} cho window.__humePowerwallLive

    /**
     * Payload bat bien cho sheet native + JSON cho WebView. Gia tri thieu
     * duoc BO QUA (khong gui null): bridge ben trang chi merge field co mat,
     * giu lai gia tri cu. Cells: null theo tung o (JSON null).
     */
    data class PowerwallPayload(
        val sys: Map<String, Any?> = emptyMap(),
        val packs: Map<String, Map<String, Any?>> = emptyMap(),
    ) {
        val isEmpty: Boolean get() = sys.isEmpty() && packs.isEmpty()
    }

    /** Build payload tu map entityId -> state tho HA. */
    fun buildPayload(states: Map<String, String>): PowerwallPayload {
        fun num(state: String?): Double? {
            if (state.isNullOrEmpty() || state == "unknown" || state == "unavailable") return null
            return state.toDoubleOrNull()
        }
        fun numAt(prefix: String, key: String): Double? = num(states[prefix + key])

        val packs = LinkedHashMap<String, Map<String, Any?>>()
        var totalFcc = 0.0; var totalFccCount = 0
        var totalRemain = 0.0; var totalRemainCount = 0
        // v52 (4): gioi han dong sac/xa HE THONG = tong 4 pack (master chi
        // bao gioi han cua rieng no, 31.2 A — khong phai tong he thong).
        var totalLimC = 0.0; var totalLimD = 0.0

        for ((key, prefix, _) in unitPrefixes) {
            fun n(k: String): Double? = numAt(prefix, k)
            fun i(k: String): Int? = n(k)?.toInt()
            val pack = LinkedHashMap<String, Any?>()
            n("soc")?.let { pack["soc"] = it }
            n("soh")?.let { pack["soh"] = it }
            n("voltage")?.let { pack["volt"] = it }
            n("current")?.let { curr ->
                pack["curr"] = curr
                pack["mode"] = if (curr < -0.05) "Đang xả" else if (curr > 0.05) "Đang sạc" else "Nghỉ"
                // Cong suat pack = ap × dong (khop snapshot: 52.25 × -1.4 = -73.15).
                n("voltage")?.let { volt -> pack["power"] = round2(volt * curr) }
            }
            n("temp_avg")?.let { pack["tavg"] = it }
            n("cell_spread")?.let { pack["diff"] = it }
            n("temp_cell_1")?.let { pack["tc1"] = it }
            n("temp_cell_2")?.let { pack["tc2"] = it }
            n("temp_cell_3")?.let { pack["tc3"] = it }
            n("temp_max")?.let { pack["tmax"] = it }
            n("temp_min")?.let { pack["tmin"] = it }
            n("cell_max")?.let { pack["cmax"] = it }
            n("cell_min")?.let { pack["cmin"] = it }
            // id cao/thap: master doc sensor, slave suy tu danh sach (1-based).
            val tc = listOf(n("temp_cell_1"), n("temp_cell_2"), n("temp_cell_3"))
            val cells: List<Double?> = (1..16).map { n("cell_$it") }
            if (key == "master") {
                i("temp_max_cell_id")?.let { pack["idTMax"] = it }
                i("temp_min_cell_id")?.let { pack["idTMin"] = it }
                i("cell_max_id")?.let { pack["idUMax"] = it }
                i("cell_min_id")?.let { pack["idUMin"] = it }
            } else {
                argmax(tc)?.let { pack["idTMax"] = it + 1 }
                argmin(tc)?.let { pack["idTMin"] = it + 1 }
                argmax(cells)?.let { pack["idUMax"] = it + 1 }
                argmin(cells)?.let { pack["idUMin"] = it + 1 }
            }
            i("status")?.let { pack["sts"] = if (it == 1) "Đã kết nối" else "Mất kết nối" }
            i("error_id")?.let { pack["err"] = it }
            pack["cells"] = cells
            // stat (chuoi hien thi nhu ESP32 page).
            val stat = LinkedHashMap<String, Any?>()
            stat["type"] = if (key == "master") "Master" else "Slave"
            states[prefix + "output"]?.let { stat["out"] = it }
            n("full_charge_cap")?.let {
                stat["fcc"] = fmt(it, 2) + " Ah"
                totalFcc += it; totalFccCount++
            }
            n("remaining_cap")?.let {
                stat["rm"] = fmt(it, 2) + " Ah"
                totalRemain += it; totalRemainCount++
            }
            n("charge_current_limit")?.let { stat["limC"] = fmt(it, 1) + " A"; totalLimC += it }
            n("charge_voltage_limit")?.let { stat["limCV"] = fmt(it, 1) + " V" }
            n("discharge_current_limit")?.let { stat["limD"] = fmt(it, 1) + " A"; totalLimD += it }
            n("adc_battery_volt")?.let { stat["adcB"] = fmt(it, 2) + " V" }
            n("adc_fuse_volt")?.let { stat["adcF"] = fmt(it, 2) + " V" }
            n("adc_pack_volt")?.let { stat["adcP"] = fmt(it, 2) + " V" }
            n("board_temp")?.let { stat["tBoard"] = fmt(it, 0) + " °C" }
            n("fet_temp")?.let { stat["tFet"] = fmt(it, 0) + " °C" }
            i("charge_fet")?.let { stat["fetC"] = if (it == 1) "Bật" else "Tắt" }
            i("discharge_fet")?.let { stat["fetD"] = if (it == 1) "Bật" else "Tắt" }
            i("precharge_fet")?.let { stat["fetR"] = if (it == 1) "Bật" else "Tắt" }
            // firmware "HW5 SW1.0.3 Boot1.0.1" -> [HW5, SW1.0.3, Boot1.0.1]
            val fw = states[prefix + "firmware_version"]?.split(" ") ?: emptyList()
            stat["fw"] = listOf(
                fw.getOrElse(0) { "" }, fw.getOrElse(1) { "" }, fw.getOrElse(2) { "" },
            )
            if (stat.isNotEmpty()) pack["stat"] = stat
            if (pack.isNotEmpty()) packs[key] = pack
        }

        // System = gia tri Pylontech master bao len inverter + tong cac pack.
        val sys = LinkedHashMap<String, Any?>()
        val mp = "sensor.powerwall_bms_master_"
        fun sysNum(key: String): Double? = num(states[mp + key])
        sysNum("py_system_soc")?.let { sys["soc"] = it }
        sysNum("py_system_soh")?.let { sys["soh"] = it }
        sysNum("py_system_voltage")?.let { sys["volt"] = it }
        // v60 (16/11): firmware ESP32 v1.0.23 da dao dau py_system_current o
        // goc cho cung quy uoc voi cac pack CAN1 (sac = duong, xa = am),
        // nen app hien thang gia tri firmware gui len, KHONG dao dau nua.
        sysNum("py_system_current")?.let { sys["curr"] = it }
        // Cong suat he thong = ap he thong × dong he thong.
        val sysVolt = sysNum("py_system_voltage")
        val sysCurr = sysNum("py_system_current")
        if (sysVolt != null && sysCurr != null) sys["power"] = round1(sysVolt * sysCurr)
        sysNum("cell_max")?.let { sys["cellMax"] = it }
        sysNum("cell_min")?.let { sys["cellMin"] = it }
        sysNum("temp_max")?.let { sys["tempMax"] = it }
        sys["tempMaxUnit"] = 4 // packLabels[4] = Master (nhiet he thong lay tu master)
        num(states[mp + "temp_max_cell_id"])?.toInt()?.let { sys["idTMax"] = it }
        sysNum("temp_min")?.let { sys["tempMin"] = it }
        sys["tempMinUnit"] = 4
        num(states[mp + "temp_min_cell_id"])?.toInt()?.let { sys["idTMin"] = it }
        // Tong nang luong: Σ dung luong day × 51.2V danh dinh; con lai: Σ con lai × ap live.
        // 51.2V GIU NGUYEN o day: nang luong DANH DINH theo chuan nameplate
        // cua pack (user 19:45 16/11), khong phai so tu dat.
        if (totalFccCount > 0) sys["kwhTotal"] = round2(totalFcc * 51.2 / 1000)
        if (totalRemainCount > 0 && sysVolt != null) {
            sys["kwhRemain"] = round2(totalRemain * sysVolt / 1000)
        }
        // v70: tong dung luong Ah (cho cong thuc thoi gian: cua so SOC + C-rate).
        if (totalFccCount > 0) sys["fccAh"] = round2(totalFcc)
        if (totalRemainCount > 0) sys["rmAh"] = round2(totalRemain)
        // v52 (4): gioi han dong sac/xa he thong = tong 4 pack (live 16/11:
        // 31.2 + 31.2 + 29.4 + 31.2 = 123.0 A moi chieu).
        if (totalLimC > 0) sys["limC"] = round1(totalLimC)
        if (totalLimD > 0) sys["limD"] = round1(totalLimD)
        // v70: setting BMS live (master) — so CHINH cho cong thuc thoi gian
        // + the setting + nhan pack; entity nao thieu / unknown thi ben nhan
        // dung fallback hardcode cu (da kiem chung 19:44 16/11 khop 100% live).
        sysNum("setting_charge_cv")?.let { sys["setCV"] = it }
        sysNum("setting_cutoff_soc")?.let { sys["setCutSOC"] = it }
        sysNum("setting_cutoff_c_rate")?.let { sys["setCutC"] = it }
        sysNum("setting_charge_limit_c_rate")?.let { sys["setLimC_C"] = it }
        sysNum("setting_discharge_cutoff_soc")?.let { sys["setDisCutSOC"] = it }
        sysNum("setting_discharge_limit_c_rate")?.let { sys["setDisLimC_C"] = it }
        sysNum("setting_cell_max_volt")?.let { sys["setCellMax"] = it }
        sysNum("setting_cell_min_volt")?.let { sys["setCellMin"] = it }
        sysNum("setting_cutoff_volt")?.let { sys["setCutV"] = it }
        sysNum("setting_undervoltage")?.let { sys["setUVP"] = it }
        sysNum("charge_voltage_limit")?.let { sys["chgVoltLim"] = it }
        sysNum("py_chg_current_limit")?.let { sys["limCSys"] = it }
        // Ma loi he thong: ErrID master (0 = khong loi). Protection/alarm flags
        // la bitmask rieng, trang khong hien thi dang ma nen khong gop vao day.
        num(states[mp + "error_id"])?.toInt()?.let { sys["err"] = it }

        return PowerwallPayload(sys = sys, packs = packs)
    }

    /** Tien ich: build payload truc tiep tu map HomeEntity (nhu GroupEntityReader). */
    fun buildPayloadFromEntities(entities: Map<String, HomeEntity>): PowerwallPayload =
        buildPayload(entities.mapValues { (_, e) -> e.state })

    /** Serialize payload thanh JSON cho window.__humePowerwallLive (chi field co mat). */
    fun payloadJson(payload: PowerwallPayload): String {
        val root = JSONObject()
        if (payload.sys.isNotEmpty()) root.put("sys", JSONObject(payload.sys.toJsonMap()))
        if (payload.packs.isNotEmpty()) {
            val packsObj = JSONObject()
            for ((k, v) in payload.packs) packsObj.put(k, JSONObject(v.toJsonMap()))
            root.put("packs", packsObj)
        }
        return root.toString()
    }

    /** Map<String, Any?> -> Map dung duoc cho JSONObject (List -> JSONArray, null -> NULL). */
    private fun Map<String, Any?>.toJsonMap(): Map<String, Any?> {
        val out = LinkedHashMap<String, Any?>()
        for ((k, v) in this) {
            out[k] = when (v) {
                is List<*> -> JSONArray().apply {
                    for (item in v) put(item ?: JSONObject.NULL)
                }
                is Map<*, *> -> JSONObject().apply {
                    @Suppress("UNCHECKED_CAST")
                    for ((mk, mv) in (v as Map<String, Any?>)) {
                        if (mv is List<*>) {
                            put(mk, JSONArray().apply {
                                for (item in mv) put(item ?: JSONObject.NULL)
                            })
                        } else if (mv != null) put(mk, mv)
                    }
                }
                null -> JSONObject.NULL
                else -> v
            }
        }
        return out
    }

    // MARK: - Theme vars cho window.__humePowerwallTheme

    /**
     * Bien CSS cua trang <- token M3E hien tai. Nen the/chu/pill/nut/thanh
     * tien trinh theo theme app; nen khung 3D (--scene-bg/-floor/-wall) CUNG
     * theo theme (user 16/11: nen 3D toi tren app sang bi loan mau). Light:
     * nen = surfaceContainerHighest (mau the app), san/tuong toi dan de
     * cum pin trang van noi + co chieu sau.
     *
     * `dark` duoc suy tu do sang that cua surface (khong doc setting): dung
     * ca khi user ep sang/toi trai voi he thong.
     */
    fun themeVars(
        colors: ColorScheme,
        extra: HumeExtraColors,
        dark: Boolean = colors.surface.luminance() < 0.5f,
    ): Map<String, String> {
        val amber = if (dark) "#F59E0B" else "#D98A1F" // mau canh bao ngu nghia, giu nhu trang goc
        val sceneBg = if (dark) "#191c18" else hex(extra.surfaceHighest)
        val sceneFloor = if (dark) "#2A2721" else hexDarkened(extra.surfaceHighest, 0.18)
        val sceneWall = if (dark) "#33302A" else hexDarkened(extra.surfaceHighest, 0.08)
        return mapOf(
            // Lop nen (dung ngoai .espui: khung canh, sel-tag, nut reset).
            "bg" to hex(colors.surface),
            "surface" to hex(extra.surfaceHighest),
            "surface2" to hex(extra.surfaceHigh),
            "ink" to hex(colors.onSurface),
            "ink2" to hex(colors.onSurfaceVariant),
            "line" to hex(colors.outlineVariant),
            "green" to hex(extra.success),
            "green-deep" to hex(extra.onSuccessContainer),
            "green-soft" to hex(extra.successContainer),
            "amber" to amber,
            "red" to hex(colors.error),
            "shadow" to if (dark) "0 12px 32px rgba(0,0,0,.5)" else "0 12px 32px rgba(18,38,24,.13)",
            // v50: phu modal settings — trang 0.45 sang / den 0.45 toi, y het
            // popup camera trang Nha. Modal pack giu phu xam cu (khong dung var nay).
            "scrim" to if (dark) "rgba(0,0,0,.45)" else "rgba(255,255,255,.45)",
            "scene-bg" to sceneBg,
            "scene-floor" to sceneFloor,
            "scene-wall" to sceneWall,
            // Lop the (trong .espui: the he thong/pack, modal, pill, nut, thanh).
            "card" to hex(extra.surfaceHighest),
            "card2" to hex(extra.surfaceHigh),
            "surf-cont" to hex(colors.surfaceContainer),
            "text" to hex(colors.onSurface),
            "muted" to hex(colors.onSurfaceVariant),
            "primary" to hex(colors.primary),
            "primary-c" to hex(colors.primaryContainer),
            "on-primary-c" to hex(colors.onPrimaryContainer),
            "sec-cont" to hex(colors.secondaryContainer),
            "on-sec-cont" to hex(colors.onSecondaryContainer),
            "chip" to hex(extra.surfaceLow),
            "ok" to hex(extra.success),
            "warn" to amber,
            "err" to hex(colors.error),
            "outline" to hex(colors.outline),
        )
    }

    // MARK: - Helpers

    /** Format so kieu ESP32 page: d=2 -> "52.20", d=0 -> "30". Locale US de
     * dau thap phan luon la "." nhu iOS (String(format:) tren may user). */
    fun fmt(v: Double, d: Int): String =
        if (d == 0) v.roundToInt().toString() else String.format(Locale.US, "%.${d}f", v)

    private fun round1(v: Double): Double = (v * 10).roundToInt() / 10.0
    private fun round2(v: Double): Double = (v * 100).roundToInt() / 100.0

    private fun argmax(xs: List<Double?>): Int? {
        var best: Pair<Int, Double>? = null
        for ((i, v) in xs.withIndex()) {
            if (v == null) continue
            if (best == null || v > best.second) best = i to v
        }
        return best?.first
    }

    private fun argmin(xs: List<Double?>): Int? {
        var best: Pair<Int, Double>? = null
        for ((i, v) in xs.withIndex()) {
            if (v == null) continue
            if (best == null || v < best.second) best = i to v
        }
        return best?.first
    }

    private fun hex(color: Color): String {
        val r = (color.red * 255).roundToInt().coerceIn(0, 255)
        val g = (color.green * 255).roundToInt().coerceIn(0, 255)
        val b = (color.blue * 255).roundToInt().coerceIn(0, 255)
        return "#%02X%02X%02X".format(r, g, b)
    }

    /** Hex cua mau toi di `f` (0..1): nhan RGB voi (1-f). Dung cho san/tuong 3D o light theme. */
    private fun hexDarkened(color: Color, f: Double): String {
        val m = (1 - f).toFloat()
        return hex(Color(color.red * m, color.green * m, color.blue * m, color.alpha))
    }
}

/**
 * Gioi han sac/xa BMS — port 1:1 iOS `BatterySettings` (BatteryTimeCalc.swift).
 * Doc LIVE tu payload sys (PowerwallLiveData.payload); field nao thieu thi
 * giu fallback (ghi ro trong comment tung field — da kiem chung 19:44 16/11
 * khop 100% live).
 */
data class BatterySettings(
    val chargeCV: Double = 54.4, // setting_charge_cv (V)
    val cutoffSOC: Double = 97.0, // setting_cutoff_soc (%)
    val cutoffCRate: Double = 0.05, // setting_cutoff_c_rate (C)
    val chargeLimitCRate: Double = 0.30, // setting_charge_limit_c_rate (C)
    val dischargeCutoffSOC: Double = 5.0, // setting_discharge_cutoff_soc (%)
    val dischargeLimitCRate: Double = 0.30, // setting_discharge_limit_c_rate (C)
    val cellMaxVolt: Double = 3.400, // setting_cell_max_volt (V)
    val cellMinVolt: Double = 3.000, // setting_cell_min_volt (V)
    val cutoffVolt: Double = 48.0, // setting_cutoff_volt (V)
    val undervoltage: Double = 40.0, // setting_undervoltage (V)
) {
    companion object {
        fun from(sys: Map<String, Any?>): BatterySettings {
            fun num(key: String): Double? = (sys[key] as? Number)?.toDouble()
            var s = BatterySettings()
            num("setCV")?.let { s = s.copy(chargeCV = it) }
            num("setCutSOC")?.let { s = s.copy(cutoffSOC = it) }
            num("setCutC")?.let { s = s.copy(cutoffCRate = it) }
            num("setLimC_C")?.let { s = s.copy(chargeLimitCRate = it) }
            num("setDisCutSOC")?.let { s = s.copy(dischargeCutoffSOC = it) }
            num("setDisLimC_C")?.let { s = s.copy(dischargeLimitCRate = it) }
            num("setCellMax")?.let { s = s.copy(cellMaxVolt = it) }
            num("setCellMin")?.let { s = s.copy(cellMinVolt = it) }
            num("setCutV")?.let { s = s.copy(cutoffVolt = it) }
            num("setUVP")?.let { s = s.copy(undervoltage = it) }
            return s
        }
    }

    /** Dinh dang hien thi dung do chinh xac cua tung setting (cho the setting). */
    fun formatted(): List<Pair<String, String>> = listOf(
        "Điện áp sạc CV" to String.format(Locale.US, "%.1f V", chargeCV),
        "Ngắt sạc ở SOC" to String.format(Locale.US, "%.0f %%", cutoffSOC),
        "Ngắt sạc ở C-rate" to String.format(Locale.US, "%.2f C", cutoffCRate),
        "Giới hạn dòng sạc" to String.format(Locale.US, "%.2f C", chargeLimitCRate),
        "Điện áp cell tối đa" to String.format(Locale.US, "%.3f V", cellMaxVolt),
        "Ngắt xả ở SOC" to String.format(Locale.US, "%.0f %%", dischargeCutoffSOC),
        "Giới hạn dòng xả" to String.format(Locale.US, "%.2f C", dischargeLimitCRate),
        "Điện áp ngắt xả" to String.format(Locale.US, "%.1f V", cutoffVolt),
        "Điện áp cell tối thiểu" to String.format(Locale.US, "%.3f V", cellMinVolt),
        "Ngưỡng thấp áp" to String.format(Locale.US, "%.1f V", undervoltage),
    )
}
