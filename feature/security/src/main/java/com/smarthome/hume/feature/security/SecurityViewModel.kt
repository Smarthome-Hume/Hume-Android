package com.smarthome.hume.feature.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.hume.core.data.HumeGraph
import com.smarthome.hume.core.data.SecurityRepository
import com.smarthome.hume.core.model.RecordingUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/** Trang An ninh (demo v4 rev12: camera Frigate + sensor grid + clip viewer). */
class SecurityViewModel : ViewModel() {
    private val repo: SecurityRepository = HumeGraph.get().securityRepository

    val state = repo.securityState

    private val _selectedCam = MutableStateFlow(0)
    val selectedCam: StateFlow<Int> = _selectedCam.asStateFlow()

    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    private val _clip = MutableStateFlow<RecordingUi?>(null)
    val clip: StateFlow<RecordingUi?> = _clip.asStateFlow()

    /** Fullscreen HLS live player: Pair(camKey, camName) — port iOS LiveNativePlayer (#13). */
    private val _livePlayer = MutableStateFlow<Pair<String, String>?>(null)
    val livePlayer: StateFlow<Pair<String, String>?> = _livePlayer.asStateFlow()

    /**
     * ID clip cho tu phat khi tai xong (port iOS onChange isDownloading → onPlay, #15).
     * Bam the clip chua co file → tai roi tu mo player.
     */
    private val _pendingAutoplay = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            state.collect { s ->
                val pending = _pendingAutoplay.value ?: return@collect
                val rec = s.recordings.values.flatten().find { it.id == pending }
                val ready = rec?.clipPath?.let(::File)?.takeIf { it.exists() } != null
                if (ready && rec != null) {
                    _pendingAutoplay.value = null
                    _clip.value = rec
                }
            }
        }
    }

    fun selectCamera(index: Int) {
        _selectedCam.value = index
        _unlocked.value = false
        refreshRecordings()
    }

    /** Tai lai 10 clip moi nhat cua camera dang chon tu Frigate. */
    fun refreshRecordings() {
        state.value.cameras.getOrNull(_selectedCam.value)?.let { repo.refreshRecordings(it.key) }
    }

    fun unlock() {
        _unlocked.value = true
    }

    fun snapshotUrl(cameraKey: String): String = repo.snapshotUrl(cameraKey)

    /** null khi remote — UI fallback ve snapshot polling. */
    fun rtspUrl(cameraKey: String): String? = repo.rtspUrl(cameraKey)

    /** MJPEG live stream (port iOS liveStreamURL) — chay ca local lan remote. */
    fun mjpegUrl(cameraKey: String): String = repo.mjpegUrl(cameraKey)

    /** HLS live stream qua go2rtc (port 1984) — chi local, null khi remote (#13). */
    fun hlsUrl(cameraKey: String): String? = repo.hlsUrl(cameraKey)

    fun openLivePlayer(camKey: String, camName: String) {
        _livePlayer.value = camKey to camName
    }

    fun closeLivePlayer() {
        _livePlayer.value = null
    }

    /**
     * Bam the clip: co file → phat ngay; chua co → tai roi tu phat (#15,
     * port iOS M3ERecCard tap-to-download-then-autoplay).
     */
    fun onRecCardClick(rec: RecordingUi) {
        val ready = rec.clipPath?.let(::File)?.takeIf { it.exists() } != null
        if (ready) {
            openClip(rec)
            return
        }
        if (_pendingAutoplay.value == rec.id) return // dang tai, cho xong
        _pendingAutoplay.value = rec.id
        val camKey = state.value.cameras.getOrNull(_selectedCam.value)?.key ?: return
        repo.downloadClip(camKey, rec.id)
    }

    fun openClip(clip: RecordingUi) {
        _clip.value = clip
    }

    fun closeClip() {
        _clip.value = null
    }
}
