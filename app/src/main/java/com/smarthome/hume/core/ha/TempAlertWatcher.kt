package com.smarthome.hume.core.ha

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG_TEMP = "TempAlert"

/**
 * Watch + danh gia nguong canh bao nhiet do — phan "trigger" cua
 * TempAlert tren iOS.
 *
 * Co che iOS (doc code, khong doan):
 * - HomeAssistantManager.swift:555-556 dua TempAlertConfig.sensor +
 *   humiditySensor vao watched set; dong 233 dua sensor vao bucket
 *   .trigger de event ve realtime.
 * - AutomationSettingsView mo ta hanh vi: "Hien Live Activity bieu do
 *   khi phong da chon vuot nguong nong (hoac xuong nguong lanh). Tu tat
 *   sau 5 phut."
 * - Khong co code iOS nao tao automation HA cho TempAlert, cung khong
 *   co ham danh gia nguong tap trung trong Core (nguong chi doc o
 *   TempAlertConfig.current + UI) — nen watcher nay chi danh gia nguong
 *   cuc bo va phat trang thai qua [alertVisible]/[alertKind], KHONG tao
 *   automation, KHONG tu post notification. Hien chua co UI nao doc
 *   2 flow nay.
 *
 * Edge-trigger nhu canh bao that: chi phat 1 lan khi vuot qua nguong,
 * re-arm khi nhiet do tro lai trong dai (nong: < hot; lanh: > cold).
 * Nong duoc uu tien khi ca 2 nguong cung vuot (khong xay ra voi
 * mac dinh hot 37 / cold 16).
 */
object TempAlertWatcher {

    enum class Kind { HOT, COLD }

    /** Co canh bao nhiet do dang hien hay khong. */
    private val _alertVisible = MutableStateFlow(false)
    val alertVisible: StateFlow<Boolean> = _alertVisible.asStateFlow()

    /** Loai canh bao dang hien, null khi khong co canh bao. */
    private val _alertKind = MutableStateFlow<Kind?>(null)
    val alertKind: StateFlow<Kind?> = _alertKind.asStateFlow()

    /** Nhiet do ghi nhan luc canh bao phat (de UI hien "37°C" chang han). */
    private val _alertValue = MutableStateFlow<Double?>(null)
    val alertValue: StateFlow<Double?> = _alertValue.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var collectJob: Job? = null
    private var repo: HomeAssistantRepository? = null
    private var config: TempAlertConfig? = null

    /** Da vuot nguong nong/lanh va chua re-arm (ve lai trong dai). */
    private var hotActive = false
    private var coldActive = false

    /**
     * Bat watch: (1) cap [HomeAssistantRepository.tempAlertSensorId]
     * theo cau hinh de bucketFor() dua sensor ve REALTIME (port iOS
     * .trigger); (2) dua sensor + humidity vao watched set (port iOS
     * rebuildWatchedSet §4b); (3) lang nghe entityUpdated va danh gia
     * nguong moi khi sensor doi. Goi tu MainActivity.onStart (cung cho
     * voi cac monitor foreground khac). Goi lai an toan (stop cu truoc).
     */
    fun start(context: Context, repository: HomeAssistantRepository) {
        stop()
        val cfg = TempAlertConfig.load(context.applicationContext)
        config = cfg
        repo = repository
        repository.tempAlertSensorId = cfg.sensor
        repository.watchEntity(cfg.sensor)
        repository.watchEntity(cfg.humiditySensor)
        Log.i(TAG_TEMP, "Watching ${cfg.sensor} (hot=${cfg.hot}, cold=${cfg.cold}, enabled=${cfg.enabled})")
        // Danh gia ngay state hien co (REST snapshot/seed) thay vi doi event WS dau tien.
        repository.entities.value[cfg.sensor]?.numericState?.let { evaluate(it) }
        collectJob = scope.launch {
            repository.entityUpdated.collect { id ->
                if (id == cfg.sensor) {
                    repository.entities.value[id]?.numericState?.let { evaluate(it) }
                }
            }
        }
    }

    /** Dung watch khi xuong background (socket cung ngat o onAppBackground). */
    fun stop() {
        collectJob?.cancel()
        collectJob = null
        repo = null
        hotActive = false
        coldActive = false
        _alertVisible.value = false
        _alertKind.value = null
        _alertValue.value = null
    }

    /** Danh gia 1 gia tri nhiet do moi — goi truc tiep duoc tu test. */
    fun evaluate(value: Double) {
        val cfg = config ?: return
        if (!cfg.enabled) return
        // Nguong nong: vuot >= hot thi phat, re-arm khi < hot.
        if (value >= cfg.hot) {
            if (!hotActive) {
                hotActive = true
                coldActive = false
                fire(Kind.HOT, value)
            }
        } else if (hotActive) {
            hotActive = false
            clearIf(Kind.HOT)
        }
        // Nguong lanh (chi khi bat): xuong <= cold thi phat, re-arm khi > cold.
        if (cfg.coldEnabled) {
            if (value <= cfg.cold) {
                if (!coldActive) {
                    coldActive = true
                    hotActive = false
                    fire(Kind.COLD, value)
                }
            } else if (coldActive) {
                coldActive = false
                clearIf(Kind.COLD)
            }
        }
    }

    private fun fire(kind: Kind, value: Double) {
        _alertKind.value = kind
        _alertValue.value = value
        _alertVisible.value = true
        Log.i(TAG_TEMP, "Temp alert $kind at $value°C (hot=${config?.hot}, cold=${config?.cold})")
    }

    private fun clearIf(kind: Kind) {
        if (_alertKind.value == kind) {
            _alertKind.value = null
            _alertValue.value = null
            _alertVisible.value = false
            Log.i(TAG_TEMP, "Temp alert $kind cleared")
        }
    }
}
