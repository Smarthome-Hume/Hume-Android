package com.smarthome.hume.feature.energy

import com.smarthome.hume.core.model.EnergyPowerKind
import com.smarthome.hume.core.model.EnergyUiState
import com.smarthome.hume.core.ui.components.Ms
import java.util.Calendar
import java.util.Locale

/**
 * Energy Insights — gợi ý năng lượng có mục đích rõ ràng.
 *
 * Mỗi gợi ý thuộc đúng 1 nhóm:
 * - [InsightCategory.Warning] (Cảnh báo): thiết bị bật quá lâu, tiêu thụ bất
 *   thường giờ cao điểm, pin xả mạnh / yếu ban đêm.
 * - [InsightCategory.Saving] (Tiết kiệm): dời tải khỏi giờ cao điểm, tận dụng
 *   solar dư, tối ưu thời điểm sạc pin.
 * - [InsightCategory.Info] (Thông tin): tổng kết solar/chi phí ngày-tháng,
 *   tình trạng pin qua đêm.
 *
 * Quy tắc nội dung (theo yêu cầu user 2026-10-05):
 * - Tiêu đề = TÊN THIẾT BỊ / chủ thể cụ thể, không cắt ngắn (card dùng marquee).
 * - Lý do PHẢI có số liệu thật từ [EnergyUiState]: thời gian bật, kWh, W, %.
 * - Ước tính tiền luôn ghi rõ là ước tính, tính từ giá EVN thực tế.
 *
 * "AI local" (không cần cloud):
 * - Ưu tiên theo số tiền tiết kiệm được ([EnergyInsight.savingVnd]).
 * - Chống spam: mỗi loại gợi ý có key ổn định, hiện tối đa 1 lần mỗi 6 giờ
 *   ([InsightDeduper]); mỗi rule chỉ ra tối đa 1 gợi ý nghiêm trọng nhất.
 */

/** Nhóm gợi ý — phạm vi rõ ràng cho từng thẻ. */
enum class InsightCategory { Warning, Saving, Info }

/** Mức độ nghiêm trọng (giữ để tương thích màu icon trong EnergyInsightsCard). */
enum class InsightSeverity { Tip, Warning, Info }

/** Popup tại chỗ mở từ action của gợi ý (mục 14, port iOS InsightPopup). */
sealed interface InsightPopup {
    /** Popup thiết bị: tắt/bật nhanh + thông tin. */
    data class Device(val entityId: String, val label: String) : InsightPopup
    /** Popup pin: % pin + nút sạc nhanh. */
    data object Battery : InsightPopup
}

/** Đích đến của nút action trong thẻ gợi ý. */
sealed interface InsightActionTarget {
    /** Mở popup thiết bị NGAY TẠI CHỖ (không điều hướng). */
    data class DevicePopup(val entityId: String, val label: String) : InsightActionTarget
    /** Mở popup pin NGAY TẠI CHỖ (không điều hướng). */
    data object BatteryPopup : InsightActionTarget
    /** Chuyển sub-tab năng lượng (Tiêu thụ / Điện mặt trời). */
    data class GoTab(val tab: EnergySubTab) : InsightActionTarget
}

/**
 * Một nút hành động trong thẻ gợi ý (port iOS InsightAction).
 * Mỗi thẻ hỗ trợ NHIỀU nút (mục 15): nút chính (isPrimary) + nút phụ.
 */
data class InsightAction(
    val label: String,
    /** Glyph Ms (vd Ms.power_settings_new); "" = không icon. */
    val glyph: String = "",
    val isPrimary: Boolean = true,
    val target: InsightActionTarget,
) {
    companion object {
        /** Action mở popup thiết bị. */
        fun devicePopup(
            entityId: String,
            label: String,
            deviceLabel: String,
            isPrimary: Boolean = true,
        ) = InsightAction(
            label = label,
            glyph = Ms.power_settings_new,
            isPrimary = isPrimary,
            target = InsightActionTarget.DevicePopup(entityId, deviceLabel),
        )

        /** Action mở popup pin. */
        fun batteryPopup(
            label: String = "Điều khiển pin",
            isPrimary: Boolean = true,
        ) = InsightAction(
            label = label,
            glyph = Ms.battery_charging_full,
            isPrimary = isPrimary,
            target = InsightActionTarget.BatteryPopup,
        )
    }
}

/** Một gợi ý năng lượng. */
data class EnergyInsight(
    /** Glyph Ms hiện ở icon box. */
    val glyph: String,
    /** TÊN cụ thể (thiết bị / chủ thể) — không cắt ngắn, card dùng marquee. */
    val title: String,
    /** Lý do CÓ SỐ LIỆU thật: thời gian, kWh, W, %. */
    val detail: String,
    /** Ước tính tiết kiệm (vd "~15.000đ/tháng"); null = không hiện. */
    val saving: String? = null,
    /** Nhóm gợi ý: Cảnh báo / Tiết kiệm / Thông tin. */
    val category: InsightCategory = InsightCategory.Info,
    /** Nhiều nút action (mục 15). */
    val actions: List<InsightAction> = emptyList(),
    /** VND tiết kiệm ước tính — dùng để sắp xếp ưu tiên (0 = không ước tính). */
    val savingVnd: Long = 0,
    /** Key ổn định để dedup/cooldown ("" = không dedup). */
    val dedupKey: String = "",
) {
    /** Map category -> severity cho màu icon trong card (giữ tương thích). */
    val severity: InsightSeverity
        get() = when (category) {
            InsightCategory.Warning -> InsightSeverity.Warning
            InsightCategory.Saving -> InsightSeverity.Tip
            InsightCategory.Info -> InsightSeverity.Info
        }

    /** Nhãn nhóm hiển thị trên thẻ. */
    val categoryLabel: String
        get() = when (category) {
            InsightCategory.Warning -> "Cảnh báo"
            InsightCategory.Saving -> "Tiết kiệm"
            InsightCategory.Info -> "Thông tin"
        }
}

/**
 * Chống spam gợi ý: mỗi key chỉ hiện 1 lần mỗi [COOLDOWN_MS].
 * Singleton theo session (không persist — app restart thì gợi ý lại từ đầu).
 */
object InsightDeduper {
    private const val COOLDOWN_MS = 6L * 60 * 60 * 1000 // 6 giờ
    private val shownAt = mutableMapOf<String, Long>()

    /** true = được hiện (và đánh dấu đã hiện); false = đang trong cooldown. */
    fun pass(key: String): Boolean {
        val now = System.currentTimeMillis()
        val last = shownAt[key]
        return if (last == null || now - last >= COOLDOWN_MS) {
            shownAt[key] = now
            true
        } else {
            false
        }
    }

    fun reset() {
        shownAt.clear()
    }
}

/**
 * Sinh gợi ý từ dữ liệu năng lượng hiện tại (rule-based + phân tích local).
 */
object EnergyInsightsManager {

    private const val PEAK_START_HOUR = 17
    private const val PEAK_END_HOUR = 22
    /** Thiết bị bật quá lâu: >= 3 tiếng. */
    private const val LONG_RUN_MINUTES = 180

    private fun hourNow(): Int =
        Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    private fun priceVnd(state: EnergyUiState): Long =
        state.cost.evnPrice.takeIf { it > 0 } ?: 3000L

    /** 1500000 -> "1.500.000đ". */
    private fun fmtVnd(v: Long): String =
        "%,d".format(v).replace(',', '.') + "đ"

    /** 200 -> "3h20p", 45 -> "45p". */
    private fun fmtDuration(min: Int): String {
        val h = min / 60
        val m = min % 60
        return if (h > 0) "${h}h" + (if (m > 0) "${m}p" else "") else "${m}p"
    }

    /**
     * Ước tính kWh thiết bị đã dùng trong [minutes] phút từ công suất hiện tại
     * (khớp tên thân thiện). null = không có sensor công suất để ước tính.
     */
    private fun estimateKwh(state: EnergyUiState, label: String, minutes: Int): Double? {
        val watts = state.powerDevices.firstOrNull { d ->
            d.name.contains(label, ignoreCase = true) ||
                label.contains(d.name, ignoreCase = true)
        }?.value ?: return null
        if (watts <= 0) return null
        return watts * minutes / 60.0 / 1000.0
    }

    fun generateInsights(state: EnergyUiState): List<EnergyInsight> {
        val all = mutableListOf<EnergyInsight>()
        // Cảnh báo
        all += longRunningDevice(state)
        all += batteryDischarging(state)
        all += peakHeavyLoad(state)
        all += gridWhileBatteryFull(state)
        all += nightLowBattery(state)
        all += lowBatteryDevices(state)
        // Tiết kiệm
        all += washerPeak(state)
        all += dryerPeak(state)
        all += solarSurplus(state)
        all += morningCharge(state)
        all += eveningTopup(state)
        // Thông tin
        all += costSummary(state)
        all += solarToday(state)
        all += nightBatteryOk(state)
        return all
            .filter { it.dedupKey.isEmpty() || InsightDeduper.pass(it.dedupKey) }
            .sortedWith(
                compareBy<EnergyInsight> { it.category.ordinal }
                    .thenByDescending { it.savingVnd },
            )
    }

    // ================= CẢNH BÁO =================

    /** Thiết bị bật quá lâu — lấy 1 thiết bị tốn kém nhất. */
    private fun longRunningDevice(state: EnergyUiState): List<EnergyInsight> {
        val p = priceVnd(state)
        val best = state.toggleStates.values
            .filter { it.isOn && (it.onMinutes ?: 0) >= LONG_RUN_MINUTES }
            .map { t ->
                val min = t.onMinutes ?: 0
                val kwh = estimateKwh(state, t.label, min)
                val vnd = kwh?.let { (it * p).toLong() } ?: 0L
                Triple(t, kwh, vnd)
            }
            .maxByOrNull { it.third }
            ?: return emptyList()
        val (t, kwh, vnd) = best
        val dur = fmtDuration(t.onMinutes ?: 0)
        val detail = if (kwh != null && kwh >= 0.05) {
            "Đã bật $dur • Ước tốn ${"%.1f".format(Locale.US, kwh)} kWh (~${fmtVnd(vnd)})"
        } else {
            "Đã bật $dur — kiểm tra xem có cần thiết không?"
        }
        return listOf(
            EnergyInsight(
                glyph = Ms.lightbulb,
                title = t.label,
                detail = detail,
                saving = if (vnd > 0) "Tắt ngay tiết kiệm ~${fmtVnd(vnd)}" else null,
                category = InsightCategory.Warning,
                savingVnd = vnd,
                dedupKey = "longrun:${t.entityId}",
                actions = listOf(
                    InsightAction.devicePopup(
                        entityId = t.entityId,
                        label = "Tắt ngay",
                        deviceLabel = t.label,
                    ),
                ),
            ),
        )
    }

    /** Pin đang xả mạnh. */
    private fun batteryDischarging(state: EnergyUiState): List<EnergyInsight> {
        val soc = state.flow.soc
        val battW = state.powerRows.firstOrNull { it.kind == EnergyPowerKind.Battery }?.watts ?: 0.0
        if (battW >= -3000) return emptyList()
        return listOf(
            EnergyInsight(
                glyph = Ms.battery_charging_full,
                title = "Pin đang xả mạnh",
                detail = "Đang xả ${(-battW).toInt()}W • Pin còn ${soc.toInt()}% — giảm tải để pin dùng được qua đêm",
                category = InsightCategory.Warning,
                dedupKey = "batt:discharge",
                actions = listOf(InsightAction.batteryPopup("Điều khiển pin")),
            ),
        )
    }

    /** Thiết bị công suất lớn chạy giờ cao điểm. */
    private fun peakHeavyLoad(state: EnergyUiState): List<EnergyInsight> {
        val hour = hourNow()
        if (hour < PEAK_START_HOUR || hour >= PEAK_END_HOUR) return emptyList()
        val p = priceVnd(state)
        val d = state.powerDevices
            .filter { it.value > 1500 }
            .maxByOrNull { it.value }
            ?: return emptyList()
        // Ước tiết kiệm khi dời 1h dùng sang giờ thấp điểm (chênh ~40% giá).
        val saveVnd = (d.value / 1000.0 * p * 0.4).toLong()
        return listOf(
            EnergyInsight(
                glyph = Ms.bolt,
                title = d.name,
                detail = "Đang dùng ${d.value.toInt()}W giờ cao điểm (17–22h) — giá điện cao",
                saving = "Dời sang sau 22h, ước rẻ hơn ~${fmtVnd(saveVnd)}/giờ",
                category = InsightCategory.Warning,
                savingVnd = saveVnd,
                dedupKey = "peak:heavy:${d.id}",
            ),
        )
    }

    /** Đang mua điện lưới trong khi pin còn nhiều. */
    private fun gridWhileBatteryFull(state: EnergyUiState): List<EnergyInsight> {
        val soc = state.flow.soc
        val gridW = state.flow.gridKw * 1000
        if (gridW <= 1000 || soc <= 60) return emptyList()
        return listOf(
            EnergyInsight(
                glyph = Ms.bolt,
                title = "Đang mua điện lưới",
                detail = "Đang lấy ${gridW.toInt()}W từ lưới trong khi pin còn ${soc.toInt()}% — kiểm tra cài đặt xả pin",
                category = InsightCategory.Warning,
                dedupKey = "grid:battfull",
                actions = listOf(InsightAction.batteryPopup("Ưu tiên dùng pin")),
            ),
        )
    }

    /** Ban đêm pin thấp. */
    private fun nightLowBattery(state: EnergyUiState): List<EnergyInsight> {
        val hour = hourNow()
        if (hour < 19 && hour >= 6) return emptyList()
        val soc = state.flow.soc
        if (soc >= 50) return emptyList()
        return listOf(
            EnergyInsight(
                glyph = Ms.bedtime,
                title = "Pin thấp ban đêm",
                detail = "Pin chỉ ${soc.toInt()}% — tắt bớt thiết bị không cần thiết để qua đêm",
                category = InsightCategory.Warning,
                dedupKey = "night:lowbatt",
                actions = listOf(InsightAction.batteryPopup("Điều khiển pin")),
            ),
        )
    }

    /** Pin yếu ở sensor/thiết bị (LowBatteryDevice từ HA). */
    private fun lowBatteryDevices(state: EnergyUiState): List<EnergyInsight> {
        val worst = state.lowBatteries.minByOrNull { it.pct } ?: return emptyList()
        return listOf(
            EnergyInsight(
                glyph = Ms.battery_charging_full,
                title = worst.name,
                detail = "Pin thiết bị còn ${worst.pct.toInt()}% — sắp hết pin, cần thay/sạc",
                category = InsightCategory.Warning,
                dedupKey = "lowbatt:${worst.name}",
            ),
        )
    }

    // ================= TIẾT KIỆM =================

    private fun powerOf(state: EnergyUiState, vararg keys: String): Double =
        (state.powerDevices + state.energyDevices)
            .firstOrNull { d -> keys.any { k -> d.name.contains(k, ignoreCase = true) } }
            ?.value ?: 0.0

    private fun switchOf(state: EnergyUiState, vararg idParts: String): String? =
        state.toggleStates.keys.firstOrNull { id ->
            idParts.any { p -> id.contains(p, ignoreCase = true) }
        }

    /** Máy giặt chạy giờ cao điểm. */
    private fun washerPeak(state: EnergyUiState): List<EnergyInsight> {
        val hour = hourNow()
        if (hour < PEAK_START_HOUR || hour >= PEAK_END_HOUR) return emptyList()
        val w = powerOf(state, "giặt")
        if (w <= 5) return emptyList()
        val p = priceVnd(state)
        // 1 mẻ giặt ~1 kWh; chênh giá cao điểm ~40%.
        val saveVnd = (1.0 * p * 0.4).toLong()
        val sw = switchOf(state, "may_giat")
        return listOf(
            EnergyInsight(
                glyph = Ms.local_laundry_service,
                title = "Máy giặt",
                detail = "Đang chạy ${w.toInt()}W giờ cao điểm — nên giặt sau 22h",
                saving = "Ước tiết kiệm ~${fmtVnd(saveVnd)}/mẻ",
                category = InsightCategory.Saving,
                savingVnd = saveVnd,
                dedupKey = "peak:washer",
                actions = if (sw != null) listOf(
                    InsightAction.devicePopup(sw, "Tạm dừng", "Máy giặt"),
                ) else emptyList(),
            ),
        )
    }

    /** Máy sấy chạy giờ cao điểm. */
    private fun dryerPeak(state: EnergyUiState): List<EnergyInsight> {
        val hour = hourNow()
        if (hour < PEAK_START_HOUR || hour >= PEAK_END_HOUR) return emptyList()
        val w = powerOf(state, "sấy")
        if (w <= 5) return emptyList()
        val p = priceVnd(state)
        // 1 mẻ sấy ~2.5 kWh; chênh giá cao điểm ~40%.
        val saveVnd = (2.5 * p * 0.4).toLong()
        val sw = switchOf(state, "may_say")
        return listOf(
            EnergyInsight(
                glyph = Ms.local_laundry_service,
                title = "Máy sấy",
                detail = "Đang chạy ${w.toInt()}W giờ cao điểm — máy sấy rất tốn điện, nên sấy sau 22h",
                saving = "Ước tiết kiệm ~${fmtVnd(saveVnd)}/mẻ",
                category = InsightCategory.Saving,
                savingVnd = saveVnd,
                dedupKey = "peak:dryer",
                actions = if (sw != null) listOf(
                    InsightAction.devicePopup(sw, "Tạm dừng", "Máy sấy"),
                ) else emptyList(),
            ),
        )
    }

    /** Solar dư mà pin đã đầy. */
    private fun solarSurplus(state: EnergyUiState): List<EnergyInsight> {
        val hour = hourNow()
        if (hour < 8 || hour > 16) return emptyList()
        val pvW = state.flow.prodKw * 1000
        val loadW = state.flow.consKw * 1000
        val soc = state.flow.soc
        if (pvW <= 2000 || soc <= 95 || loadW >= pvW * 0.5) return emptyList()
        val p = priceVnd(state)
        val surplusKw = (pvW - loadW) / 1000.0
        val sunHours = when (hour) {
            in 8..11 -> 4.0
            in 12..14 -> 2.5
            else -> 1.0
        }
        val saveVnd = (surplusKw * sunHours * p).toLong()
        return listOf(
            EnergyInsight(
                glyph = Ms.wb_sunny,
                title = "Đang dư điện mặt trời",
                detail = "Dư ${(pvW - loadW).toInt()}W • Pin đã ${soc.toInt()}% — bật thêm thiết bị để không phí điện solar",
                saving = "Tận dụng hết, ước ~${fmtVnd(saveVnd)}",
                category = InsightCategory.Saving,
                savingVnd = saveVnd,
                dedupKey = "solar:surplus",
                actions = listOf(
                    InsightAction(
                        label = "Xem thiết bị",
                        glyph = Ms.bolt,
                        isPrimary = true,
                        target = InsightActionTarget.GoTab(EnergySubTab.Cons),
                    ),
                    InsightAction(
                        label = "Xem solar",
                        glyph = Ms.wb_sunny,
                        isPrimary = false,
                        target = InsightActionTarget.GoTab(EnergySubTab.Solar),
                    ),
                ),
            ),
        )
    }

    /** Sáng sớm pin thấp — chờ nắng sạc miễn phí. */
    private fun morningCharge(state: EnergyUiState): List<EnergyInsight> {
        val hour = hourNow()
        if (hour !in 6..9) return emptyList()
        val soc = state.flow.soc
        val pvW = state.flow.prodKw * 1000
        if (soc >= 40 || pvW >= 500) return emptyList()
        return listOf(
            EnergyInsight(
                glyph = Ms.wb_sunny,
                title = "Pin yếu buổi sáng",
                detail = "Pin ${soc.toInt()}% • Nắng sắp lên — hạn chế tải nặng, chờ sạc solar miễn phí",
                category = InsightCategory.Saving,
                dedupKey = "charge:morning",
                actions = listOf(
                    InsightAction(
                        label = "Xem solar",
                        glyph = Ms.wb_sunny,
                        isPrimary = true,
                        target = InsightActionTarget.GoTab(EnergySubTab.Solar),
                    ),
                ),
            ),
        )
    }

    /** Chiều nắng sắp tắt — tranh thủ sạc pin. */
    private fun eveningTopup(state: EnergyUiState): List<EnergyInsight> {
        val hour = hourNow()
        if (hour !in 15..17) return emptyList()
        val soc = state.flow.soc
        val pvW = state.flow.prodKw * 1000
        if (soc >= 80 || pvW <= 1000) return emptyList()
        return listOf(
            EnergyInsight(
                glyph = Ms.wb_sunny,
                title = "Tranh thủ sạc pin",
                detail = "Nắng còn ${pvW.toInt()}W nhưng sắp tắt • Pin ${soc.toInt()}% — giảm tải để dồn sạc cho buổi tối",
                category = InsightCategory.Saving,
                dedupKey = "charge:evening",
                actions = listOf(InsightAction.batteryPopup("Ưu tiên sạc pin")),
            ),
        )
    }

    // ================= THÔNG TIN =================

    /** Tổng kết tiết kiệm tháng. */
    private fun costSummary(state: EnergyUiState): List<EnergyInsight> {
        val saving = state.cost.homeVnd - state.cost.gridVnd
        if (saving <= 50_000) return emptyList()
        return listOf(
            EnergyInsight(
                glyph = Ms.donut_large,
                title = "Tiết kiệm tháng này",
                detail = "Đã tiết kiệm ${fmtVnd(saving)} nhờ điện mặt trời + pin lưu trữ",
                category = InsightCategory.Info,
                dedupKey = "cost:monthly",
                actions = listOf(
                    InsightAction(
                        label = "Xem chi tiết",
                        glyph = Ms.donut_large,
                        target = InsightActionTarget.GoTab(EnergySubTab.Cons),
                    ),
                ),
            ),
        )
    }

    /** Tổng kết solar hôm nay. */
    private fun solarToday(state: EnergyUiState): List<EnergyInsight> {
        val pvKwh = state.flow.pvKwh
        if (pvKwh < 0.5) return emptyList()
        val gridKwh = state.flow.gridKwh
        val extra = if (gridKwh > 0.5) " • Mua lưới ${"%.1f".format(Locale.US, gridKwh)} kWh" else ""
        return listOf(
            EnergyInsight(
                glyph = Ms.wb_sunny,
                title = "Solar hôm nay",
                detail = "Đã tạo ${"%.1f".format(Locale.US, pvKwh)} kWh$extra",
                category = InsightCategory.Info,
                dedupKey = "info:solar",
                actions = listOf(
                    InsightAction(
                        label = "Xem solar",
                        glyph = Ms.wb_sunny,
                        target = InsightActionTarget.GoTab(EnergySubTab.Solar),
                    ),
                ),
            ),
        )
    }

    /** Pin đủ qua đêm. */
    private fun nightBatteryOk(state: EnergyUiState): List<EnergyInsight> {
        val hour = hourNow()
        if (hour < 19 && hour >= 6) return emptyList()
        val soc = state.flow.soc
        if (soc < 80) return emptyList()
        return listOf(
            EnergyInsight(
                glyph = Ms.bedtime,
                title = "Pin đủ qua đêm",
                detail = "Pin ${soc.toInt()}% — thoải mái dùng đến sáng, không cần tiết kiệm",
                category = InsightCategory.Info,
                dedupKey = "night:ok",
            ),
        )
    }
}
