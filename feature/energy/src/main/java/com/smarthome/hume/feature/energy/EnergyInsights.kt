package com.smarthome.hume.feature.energy

import com.smarthome.hume.core.model.EnergyUiState
import com.smarthome.hume.core.ui.components.Ms

/**
 * Energy Insights — gợi ý tiết kiệm điện kiểu Aqara Energy Butler.
 * Port từ iOS `EnergyInsightsManager.swift` (rule-based, chạy local).
 *
 * Các loại insight:
 * 1. Thiết bị quên tắt (bật quá lâu)
 * 2. Chạy giờ cao điểm (máy giặt/sấy lúc 17-22h)
 * 3. Pin sạc/xả tối ưu theo giờ
 * 4. Solar dư không dùng hết
 * 5. Chi phí dự báo vượt ngưỡng
 */

/** Mức độ nghiêm trọng của gợi ý (port iOS EnergyInsight.Severity). */
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

/** Một gợi ý tiết kiệm điện (port iOS EnergyInsight). */
data class EnergyInsight(
    /** Glyph Ms hiện ở icon box. */
    val glyph: String,
    val title: String,
    val detail: String,
    /** Ước tính tiết kiệm (vd "~15.000đ/tháng"); null = không hiện. */
    val saving: String? = null,
    val severity: InsightSeverity = InsightSeverity.Info,
    /** Nhiều nút action (mục 15). */
    val actions: List<InsightAction> = emptyList(),
)

/**
 * Sinh gợi ý từ dữ liệu năng lượng hiện tại (rule-based).
 * Port logic từ iOS `EnergyInsightsManager.generateInsights`.
 */
object EnergyInsightsManager {

    private const val PEAK_START_HOUR = 17
    private const val PEAK_END_HOUR = 22
    /** Thiết bị bật quá lâu: >= 8 tiếng. */
    private const val LONG_RUN_MINUTES = 480

    fun generateInsights(state: EnergyUiState): List<EnergyInsight> {
        val out = mutableListOf<EnergyInsight>()
        out += checkBatteryHealth(state)
        out += checkBatteryCharging(state)
        out += checkLongRunningDevices(state)
        out += checkPeakHourUsage(state)
        out += checkSolar(state)
        out += checkCost(state)
        out += checkDailyOverview(state)
        out += checkDeviceEnergy(state)
        out += checkSystemOptimization(state)
        return out
    }

    // ---------- 1. Sức khoẻ pin ----------

    private fun checkBatteryHealth(state: EnergyUiState): List<EnergyInsight> {
        val out = mutableListOf<EnergyInsight>()
        val soc = state.flow.soc
        // powerRows: Battery (W, âm = đang xả)
        val battW = state.powerRows.firstOrNull {
            it.kind == com.smarthome.hume.core.model.EnergyPowerKind.Battery
        }?.watts ?: 0.0

        // Pin đang xả mạnh
        if (battW < -3000) {
            out += EnergyInsight(
                glyph = Ms.battery_charging_full,
                title = "Pin đang xả ${(-battW).toInt()}W",
                detail = "Còn ${soc.toInt()}%. Giảm tải để pin dùng được lâu hơn qua đêm.",
                severity = InsightSeverity.Warning,
                actions = listOf(
                    InsightAction.batteryPopup("Tắt bớt thiết bị"),
                ),
            )
        }
        // Pin đầy mà vẫn sạc (lãng phí solar)
        if (soc >= 99 && state.flow.battCharging && battW > 100) {
            out += EnergyInsight(
                glyph = Ms.battery_charging_full,
                title = "Pin đã đầy 100%",
                detail = "Đang sạc thừa ${battW.toInt()}W. Bật thêm thiết bị để dùng điện solar trực tiếp.",
                severity = InsightSeverity.Info,
                actions = listOf(
                    InsightAction.batteryPopup("Dùng điện dư"),
                ),
            )
        }
        return out
    }

    // ---------- 2. Tối ưu sạc pin theo giờ ----------

    private fun checkBatteryCharging(state: EnergyUiState): List<EnergyInsight> {
        val out = mutableListOf<EnergyInsight>()
        val soc = state.flow.soc
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val pvW = state.flow.prodKw * 1000

        // Sáng sớm (6-9h): pin thấp, nắng sắp lên → chờ sạc solar
        if (hour in 6..9 && soc < 40 && pvW < 500) {
            out += EnergyInsight(
                glyph = Ms.wb_sunny,
                title = "Pin ${soc.toInt()}% — chờ nắng sạc",
                detail = "Trời sắp nắng. Hạn chế dùng thiết bị nặng để dành pin sạc từ solar (miễn phí).",
                saving = "Tiết kiệm ~10.000đ",
                severity = InsightSeverity.Tip,
                actions = listOf(
                    InsightAction(
                        label = "Xem dự báo nắng",
                        glyph = Ms.wb_sunny,
                        isPrimary = true,
                        target = InsightActionTarget.GoTab(EnergySubTab.Solar),
                    ),
                    InsightAction(
                        label = "Tắt bớt thiết bị",
                        glyph = Ms.power_settings_new,
                        isPrimary = false,
                        target = InsightActionTarget.GoTab(EnergySubTab.Cons),
                    ),
                ),
            )
        }
        // Chiều (15-17h): pin chưa đầy, nắng sắp tắt → tranh thủ sạc
        if (hour in 15..17 && soc < 80 && pvW > 1000) {
            out += EnergyInsight(
                glyph = Ms.wb_sunny,
                title = "Tranh thủ sạc pin: ${soc.toInt()}%",
                detail = "Nắng còn ${pvW.toInt()}W nhưng sắp tắt. Tắt bớt thiết bị để dồn sạc pin cho tối.",
                severity = InsightSeverity.Tip,
                actions = listOf(
                    InsightAction.batteryPopup("Sạc nhanh"),
                ),
            )
        }
        // Tối (18-22h): giờ cao điểm, nên dùng pin
        if (hour in 18..22 && soc > 50) {
            val gridW = state.flow.gridKw * 1000
            if (gridW > 500) {
                out += EnergyInsight(
                    glyph = Ms.bolt,
                    title = "Đang dùng ${gridW.toInt()}W điện lưới giờ cao điểm",
                    detail = "Pin còn ${soc.toInt()}%. Chuyển sang dùng pin để tránh giá cao điểm.",
                    saving = "~5.000đ/giờ",
                    severity = InsightSeverity.Warning,
                    actions = listOf(
                        InsightAction.batteryPopup("Dùng pin"),
                    ),
                )
            }
        }
        return out
    }

    // ---------- 3. Thiết bị bật quá lâu (quên tắt) ----------

    private fun checkLongRunningDevices(state: EnergyUiState): List<EnergyInsight> {
        return state.toggleStates.values
            .filter { it.isOn && (it.onMinutes ?: 0) >= LONG_RUN_MINUTES }
            .sortedByDescending { it.onMinutes }
            .take(3)
            .map { dev ->
                val hours = (dev.onMinutes ?: 0) / 60
                EnergyInsight(
                    glyph = Ms.lightbulb,
                    title = "${dev.label} bật đã $hours tiếng",
                    detail = "Có thể bạn quên tắt?",
                    severity = InsightSeverity.Warning,
                    actions = listOf(
                        InsightAction.devicePopup(
                            entityId = dev.entityId,
                            label = "Tắt ngay",
                            deviceLabel = dev.label,
                        ),
                    ),
                )
            }
    }

    // ---------- 4. Giờ cao điểm: máy giặt/sấy ----------

    private fun checkPeakHourUsage(state: EnergyUiState): List<EnergyInsight> {
        val out = mutableListOf<EnergyInsight>()
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        if (hour < PEAK_START_HOUR || hour >= PEAK_END_HOUR) return out

        // Công suất máy giặt/sấy từ danh sách thiết bị (tên thân thiện tiếng Việt)
        fun powerOf(vararg keys: String): Double =
            (state.powerDevices + state.energyDevices)
                .firstOrNull { d -> keys.any { k -> d.name.contains(k, ignoreCase = true) } }
                ?.value ?: 0.0

        // Tìm switch điều khiển để mở popup tắt nhanh
        // (không đoán id: tìm thật trong toggleStates)
        fun switchOf(vararg idParts: String): String? =
            state.toggleStates.keys.firstOrNull { id ->
                idParts.any { p -> id.contains(p, ignoreCase = true) }
            }

        val washerW = powerOf("giặt")
        if (washerW > 5) {
            val sw = switchOf("may_giat")
            out += EnergyInsight(
                glyph = Ms.local_laundry_service,
                title = "Máy giặt đang chạy giờ cao điểm",
                detail = "Giá điện cao điểm gấp ~1.5x. Gợi ý chạy sau 22h.",
                saving = "~20.000đ/lần giặt",
                severity = InsightSeverity.Tip,
                actions = if (sw != null) listOf(
                    InsightAction.devicePopup(sw, "Tạm dừng", "Máy giặt")
                ) else emptyList(),
            )
        }
        val dryerW = powerOf("sấy")
        if (dryerW > 5) {
            val sw = switchOf("may_say")
            out += EnergyInsight(
                glyph = Ms.local_laundry_service,
                title = "Máy sấy đang chạy giờ cao điểm",
                detail = "Máy sấy tốn nhiều điện. Gợi ý chạy sau 22h.",
                saving = "~30.000đ/lần sấy",
                severity = InsightSeverity.Tip,
                actions = if (sw != null) listOf(
                    InsightAction.devicePopup(sw, "Tạm dừng", "Máy sấy")
                ) else emptyList(),
            )
        }
        return out
    }

    // ---------- 5. Solar dư ----------

    private fun checkSolar(state: EnergyUiState): List<EnergyInsight> {
        val out = mutableListOf<EnergyInsight>()
        val pvW = state.flow.prodKw * 1000
        val loadW = state.flow.consKw * 1000
        val soc = state.flow.soc

        if (pvW > 2000 && soc > 95 && loadW < pvW * 0.5) {
            val surplus = (pvW - loadW).toInt()
            out += EnergyInsight(
                glyph = Ms.wb_sunny,
                title = "Đang dư ${surplus}W điện mặt trời",
                detail = "Pin đã đầy ${soc.toInt()}%. Bật thêm thiết bị để tận dụng?",
                severity = InsightSeverity.Info,
                actions = listOf(
                    InsightAction(
                        label = "Bật thiết bị",
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
            )
        }
        return out
    }

    // ---------- 6. Chi phí ----------

    private fun checkCost(state: EnergyUiState): List<EnergyInsight> {
        val out = mutableListOf<EnergyInsight>()
        val saving = state.cost.homeVnd - state.cost.gridVnd
        if (saving > 50_000) {
            out += EnergyInsight(
                glyph = Ms.donut_large,
                title = "Tiết kiệm ${"%,d".format(saving)}đ tháng này",
                detail = "Nhờ điện mặt trời + pin lưu trữ.",
                severity = InsightSeverity.Info,
                actions = listOf(
                    InsightAction(
                        label = "Xem chi tiết",
                        glyph = Ms.donut_large,
                        target = InsightActionTarget.GoTab(EnergySubTab.Cons),
                    ),
                ),
            )
        }
        return out
    }

    // ---------- 7. Tổng quan ngày ----------

    private fun checkDailyOverview(state: EnergyUiState): List<EnergyInsight> {
        val out = mutableListOf<EnergyInsight>()
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val pvToday = state.flow.pvKwh
        val soc = state.flow.soc

        if (hour in 6..9 && pvToday > 0) {
            out += EnergyInsight(
                glyph = Ms.wb_sunny,
                title = "Solar buổi sáng: ${"%.1f".format(pvToday)} kWh",
                detail = "Nắng đang lên, tranh thủ sạc pin đầy trước trưa.",
                severity = InsightSeverity.Info,
            )
        }
        if (hour in 15..17) {
            val pvW = state.flow.prodKw * 1000
            if (pvW < 500 && soc < 80) {
                out += EnergyInsight(
                    glyph = Ms.wb_sunny,
                    title = "Chiều nay nắng yếu (${pvW.toInt()}W)",
                    detail = "Pin mới ${soc.toInt()}% — hạn chế tải nặng tối nay.",
                    severity = InsightSeverity.Warning,
                )
            }
        }
        if (hour >= 19 || hour < 6) {
            if (soc >= 80) {
                out += EnergyInsight(
                    glyph = Ms.bedtime,
                    title = "Pin ${soc.toInt()}% — đủ qua đêm",
                    detail = "Yên tâm dùng, không cần tiết kiệm.",
                    severity = InsightSeverity.Info,
                )
            } else if (soc < 50) {
                out += EnergyInsight(
                    glyph = Ms.bedtime,
                    title = "Pin chỉ ${soc.toInt()}% — đêm nay cẩn thận",
                    detail = "Tắt bớt thiết bị không cần thiết.",
                    severity = InsightSeverity.Warning,
                )
            }
        }
        return out
    }

    // ---------- 8. Thiết bị tiêu thụ cao ----------

    private fun checkDeviceEnergy(state: EnergyUiState): List<EnergyInsight> {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        val isPeak = hour in PEAK_START_HOUR until PEAK_END_HOUR
        return state.powerDevices
            .filter { it.value > 1000 }
            .sortedByDescending { it.value }
            .take(3)
            .map { d ->
                if (isPeak) {
                    EnergyInsight(
                        glyph = Ms.bolt,
                        title = "${d.name}: ${d.value.toInt()}W giờ cao điểm",
                        detail = "Đang dùng điện lưới giá cao — cân nhắc dời sang giờ thấp điểm.",
                        severity = InsightSeverity.Warning,
                    )
                } else {
                    EnergyInsight(
                        glyph = Ms.bolt,
                        title = "${d.name} đang dùng ${d.value.toInt()}W",
                        detail = "Thiết bị tiêu thụ lớn nhất hiện tại.",
                        severity = InsightSeverity.Info,
                    )
                }
            }
    }

    // ---------- 9. Tối ưu hệ thống ----------

    private fun checkSystemOptimization(state: EnergyUiState): List<EnergyInsight> {
        val out = mutableListOf<EnergyInsight>()
        val soc = state.flow.soc
        val pvW = state.flow.prodKw * 1000
        val loadW = state.flow.consKw * 1000
        val gridW = state.flow.gridKw * 1000

        // Solar dư mà pin đầy → nên dùng thêm tải
        if (pvW > loadW + 500 && soc >= 95) {
            out += EnergyInsight(
                glyph = Ms.wb_sunny,
                title = "Solar dư ${(pvW - loadW).toInt()}W",
                detail = "Pin đã đầy — bật thêm thiết bị để không phí điện solar.",
                severity = InsightSeverity.Info,
            )
        }
        // Đang mua điện lưới trong khi pin còn nhiều
        if (gridW > 1000 && soc > 60) {
            out += EnergyInsight(
                glyph = Ms.bolt,
                title = "Đang mua ${gridW.toInt()}W từ lưới",
                detail = "Pin còn ${soc.toInt()}% — kiểm tra cài đặt xả pin.",
                severity = InsightSeverity.Warning,
            )
        }
        return out
    }
}
