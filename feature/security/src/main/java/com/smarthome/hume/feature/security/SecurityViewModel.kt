package com.smarthome.hume.feature.security

import androidx.lifecycle.ViewModel
import com.smarthome.hume.core.data.HumeGraph
import com.smarthome.hume.core.data.SecurityRepository
import com.smarthome.hume.core.model.RecordingUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    fun openClip(clip: RecordingUi) {
        _clip.value = clip
    }

    fun closeClip() {
        _clip.value = null
    }
}
