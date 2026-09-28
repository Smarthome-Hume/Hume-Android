package com.smarthome.hume.feature.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.hume.core.data.HumeGraph
import com.smarthome.hume.core.data.SecurityRepository
import com.smarthome.hume.core.model.RecordingUi
import com.smarthome.hume.core.model.SensorUi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    /**
     * "Live flicker" theo demo (setInterval 22s): random toggle 1 cam bien
     * chuyen dong + "Vua xong". Map entityId -> SensorUi da override;
     * screen merge len state goc tu repo.
     */
    private val _flicker = MutableStateFlow<Map<String, SensorUi>>(emptyMap())
    val flicker: StateFlow<Map<String, SensorUi>> = _flicker.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                delay(22_000)
                val sensors = state.value.motionSensors
                if (sensors.isEmpty()) continue
                val s = sensors.random()
                val shown = _flicker.value[s.entityId] ?: s
                _flicker.value = _flicker.value + (s.entityId to shown.copy(
                    isOn = !shown.isOn,
                    lastChange = "Vừa xong",
                ))
            }
        }
    }

    fun selectCamera(index: Int) {
        _selectedCam.value = index
        _unlocked.value = false
        state.value.cameras.getOrNull(index)?.let { repo.refreshRecordings(it.key) }
    }

    fun unlock() {
        _unlocked.value = true
    }

    fun snapshotUrl(cameraKey: String): String = repo.snapshotUrl(cameraKey)

    fun openClip(clip: RecordingUi) {
        _clip.value = clip
    }

    fun closeClip() {
        _clip.value = null
    }
}
