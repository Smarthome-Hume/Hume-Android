package com.smarthome.hume.feature.energy.powerwall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.smarthome.hume.core.data.HomeRepository
import com.smarthome.hume.core.data.HumeGraph
import com.smarthome.hume.core.model.HomeEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch

/**
 * ViewModel trang Powerwall 3D (N-02) — port iOS `GroupEntityReader` +
 * `PowerwallWebView.Coordinator` (PowerwallSimulationView.swift).
 *
 * Nguon so lieu: [HomeRepository.entityStates] — cung flow tho HA ma the
 * mo rong trong phong dung (port iOS GroupEntityReader). Tap 205 entity
 * [PowerwallLiveData.entityIds] DA duoc dang ky vao watched set (N-12,
 * AppHomeRepository: ids.addAll(POWERWALL_ENTITY_IDS)) nen cac entity nay
 * tick live theo WebSocket; snapshot REST /api/states luc ket noi / len
 * foreground chi la gia tri ban dau.
 *
 * (v81) SEED — port iOS `GroupEntityReader.seed()` + `Coordinator.didFinish`
 * pre-seed: [seedFromCache] build payload ngay tu [HomeRepository.entityStates]
 * hien tai khi ViewModel duoc tao (truoc khi WebView load xong), thay vi
 * cho emission WS tiep theo. Nhờ vậy lúc mở trang WebView nhận được số
 * cũ có ngay (diệt cửa sổ "-" lúc mới mở). Payload rỗng (chưa có entity
 * Powerwall nào trong cache) thì KHÔNG đẩy "{}" — giữ `payloadJson = null`
 * để WebView không fade-in với toàn "-" (port iOS v81: "states rỗng thì
 * không đẩy payload toàn unavailable — chờ seed / push live").
 *
 * (v81) NHỊP + DISTINCT — distinctUntilChanged chạy TRƯỚC KHI build: chỉ
 * build payload khi đúng 205 entity BMS đổi state/attributes (so bằng
 * [powerwallStatesEqual], bỏ `lastChanged`/`lastUpdated` như dedup trong
 * `HomeAssistantRepository.applyUpdates`); payload đổi mới serialize
 * JSON; JSON đổi mới post vào webview, tối đa 1 lần / [PAYLOAD_CADENCE_MS]
 * nhờ operator [sample] (không còn `delay` tay trong collect — delay cũ
 * chặn cả collector, giá trị trung gian chỉ được flush sau khi hết delay).
 * `_payload` cho sheet native vẫn cập nhật ngay mỗi lần payload đổi
 * (không theo nhịp webview).
 *
 * GIOI HAN SEED CON LAI (ghi ro, khong giau): seed chi doc duoc nhung gi
 * DA CO trong `entityStates`. Neu luc mo trang `_entities` con rong (vd
 * app vua khoi dong, snapshot /api/states chua ve) thi khong co lenh
 * refetch rieng cho 205 entity nay — refetch that (port iOS
 * `HomeAssistantManager.refreshStates(ids:)`) can them method vao
 * [HomeRepository] + implement o AppHomeRepository/HomeAssistantRepository,
 * ngoai pham vi file nay (2 file do worker khac so huu). Tam thoi truong
 * hop do van cho emission cua `fetchInitialStates()` luc ket noi (dang
 * chay san o tang repo) + fallback 4s o PowerwallScreen.
 */
class PowerwallViewModel(
    private val repo: HomeRepository,
) : ViewModel() {

    /** Trang thai tho HA theo entityId (nhu iOS GroupEntityReader). */
    val entityStates: StateFlow<Map<String, HomeEntity>> = repo.entityStates

    /** Payload {sys, packs} da build (sheet native doc truc tiep tu day). */
    private val _payload = MutableStateFlow(PowerwallLiveData.PowerwallPayload())
    val payload: StateFlow<PowerwallLiveData.PowerwallPayload> = _payload.asStateFlow()

    /** JSON cho window.__humePowerwallLive (da sample 2s, chi doi that). */
    private val _payloadJson = MutableStateFlow<String?>(null)
    val payloadJson: StateFlow<String?> = _payloadJson.asStateFlow()

    init {
        seedFromCache()
        viewModelScope.launch {
            entityStates
                // Chỉ quan tâm 205 entity BMS: emission nào không chạm tới
                // tập này thì dừng ngay tại đây, khỏi build payload.
                .map { states -> states.filterKeys { it in PowerwallLiveData.entityIdsSet } }
                .distinctUntilChanged(::powerwallStatesEqual)
                .map { PowerwallLiveData.buildPayloadFromEntities(it) }
                .distinctUntilChanged()
                // Sheet native: tươi ngay, không theo nhịp webview.
                .onEach { _payload.value = it }
                .mapNotNull { payload -> payloadJsonOrNull(payload) }
                .distinctUntilChanged()
                // Nhịp đẩy webview: tối đa 1 lần/2s, luôn lấy giá trị mới
                // nhất (conflate) — operator chuẩn, không delay tay.
                .sample(PAYLOAD_CADENCE_MS)
                .collect { json ->
                    if (json != _payloadJson.value) _payloadJson.value = json
                }
        }
    }

    /**
     * Seed đồng bộ từ cache hiện tại (port iOS GroupEntityReader.seed):
     * build payload từ `entityStates.value` ngay lúc mở trang. Payload
     * rỗng thì thôi — giữ `payloadJson = null`, chờ push live.
     */
    private fun seedFromCache() {
        val states = repo.entityStates.value
            .filterKeys { it in PowerwallLiveData.entityIdsSet }
        if (states.isEmpty()) return
        val built = PowerwallLiveData.buildPayloadFromEntities(states)
        if (built.isEmpty) return
        _payload.value = built
        _payloadJson.value = PowerwallLiveData.payloadJson(built)
    }

    companion object {
        /**
         * Nhịp đẩy payload vào webview — KHÔNG phải throttle tầng:
         * 2s là nhịp thiết kế của tween HTML 600ms (iOS v66), khai báo
         * tường minh là `payloadCadence` (port iOS
         * `UpdatePolicy.powerwallPayloadCadence` = 2.0s). Cùng giá trị
         * với `UpdatePolicy.POWERWALL_PAYLOAD_CADENCE_MS` bên :app; VM
         * không import được nó vì :feature:energy không depend :app —
         * ngày UpdatePolicy chuyển xuống :core:model thì đọc từ đó.
         */
        const val PAYLOAD_CADENCE_MS = 2_000L

        @Deprecated(
            "Dùng PAYLOAD_CADENCE_MS — cùng giá trị, đổi tên cho rõ nghĩa " +
                "(port iOS UpdatePolicy.powerwallPayloadCadence).",
            ReplaceWith("PAYLOAD_CADENCE_MS"),
        )
        const val DATA_THROTTLE_MS = PAYLOAD_CADENCE_MS

        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer { PowerwallViewModel(HumeGraph.get().homeRepository) }
        }

        /**
         * So sánh 2 map con Powerwall theo state + attributes (bỏ
         * `lastChanged`/`lastUpdated` — HA đổi 2 field này mỗi event kể
         * cả khi state giống; dùng `equals` mặc định của [HomeEntity]
         * (data class) sẽ emit thừa). Cùng luật dedup của
         * `HomeAssistantRepository.applyUpdates()`.
         */
        internal fun powerwallStatesEqual(
            a: Map<String, HomeEntity>,
            b: Map<String, HomeEntity>,
        ): Boolean {
            if (a.keys != b.keys) return false
            return a.all { (id, entity) ->
                val other = b[id] ?: return@all false
                other.state == entity.state && other.attributes == entity.attributes
            }
        }

        /** Payload rỗng (chưa có số) thì không serialize — giữ null. */
        internal fun payloadJsonOrNull(
            payload: PowerwallLiveData.PowerwallPayload,
        ): String? =
            if (payload.isEmpty) null else PowerwallLiveData.payloadJson(payload)
    }
}
