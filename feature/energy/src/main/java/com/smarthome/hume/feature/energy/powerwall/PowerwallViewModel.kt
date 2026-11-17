package com.smarthome.hume.feature.energy.powerwall

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.smarthome.hume.core.data.HomeRepository
import com.smarthome.hume.core.data.HumeGraph
import com.smarthome.hume.core.model.HomeEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
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
 * foreground chi la gia tri ban dau. (Sua comment stale: ban truoc ghi
 * N-12 chua land — nay da land.)
 *
 * GAP CON LAI SO VOI iOS (ghi ro, chua sua — can doi interface
 * HomeRepository o :core:data, ngoai pham vi file nay): iOS
 * GroupEntityReader.seed() chay lai moi khi mo trang + moi khi
 * ha.isConnected chuyen sang true (lay gia tri cuoi biet ngay lap tuc).
 * Android o day chi phan ung theo emission cua entityStates: neu luc mo
 * trang ma _entities con rong (vd app vua khoi dong, snapshot /api/states
 * chua ve / that bai) thi the van "-" cho toi khi emission ke tiep toi,
 * khong co lenh refetch rieng khi mo trang. Tam giai: PowerwallScreen
 * giu WebView an toi khi push data dau tien chay xong (co fallback 4s),
 * nen user khong thay trang thai "-" do nua; muon het han thi them
 * refresh rieng cho 205 entity nay vao HomeRepository (port iOS
 * HomeAssistantManager.refreshStates(ids:)).
 *
 * Throttle day data 2s (port iOS Coordinator.dataThrottle): GroupEntityReader
 * da chan moi entity 2s nhung 205 sensor doi lech nhau nen payload doi
 * nhieu lan/giay va trang rebuild innerHTML lien tuc -> the nhay. Day toi
 * da 1 lan/2s, luon flush gia tri CUOI (StateFlow conflate cac gia tri
 * trung gian trong luc delay). Theme day ngay (hiem).
 */
class PowerwallViewModel(
    private val repo: HomeRepository,
) : ViewModel() {

    /** Trang thai tho HA theo entityId (nhu iOS GroupEntityReader). */
    val entityStates: StateFlow<Map<String, HomeEntity>> = repo.entityStates

    /** Payload {sys, packs} da build (sheet native doc truc tiep tu day). */
    private val _payload = MutableStateFlow(PowerwallLiveData.PowerwallPayload())
    val payload: StateFlow<PowerwallLiveData.PowerwallPayload> = _payload.asStateFlow()

    /** JSON cho window.__humePowerwallLive (da throttle 2s, chi doi that). */
    private val _payloadJson = MutableStateFlow<String?>(null)
    val payloadJson: StateFlow<String?> = _payloadJson.asStateFlow()

    init {
        viewModelScope.launch {
            var lastEmitMs = 0L
            entityStates
                .map { PowerwallLiveData.buildPayloadFromEntities(it) }
                .distinctUntilChanged()
                .collect { built ->
                    _payload.value = built
                    val json = PowerwallLiveData.payloadJson(built)
                    if (json == _payloadJson.value) return@collect
                    val now = SystemClock.elapsedRealtime()
                    val wait = DATA_THROTTLE_MS - (now - lastEmitMs)
                    if (wait > 0) delay(wait)
                    _payloadJson.value = json
                    lastEmitMs = SystemClock.elapsedRealtime()
                }
        }
    }

    companion object {
        /** iOS Coordinator.dataThrottle = 2.0s (dong nhat GroupEntityReader.uiThrottle). */
        const val DATA_THROTTLE_MS = 2_000L

        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer { PowerwallViewModel(HumeGraph.get().homeRepository) }
        }
    }
}
