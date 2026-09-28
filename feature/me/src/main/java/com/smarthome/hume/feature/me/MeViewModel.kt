package com.smarthome.hume.feature.me

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.hume.core.data.AiResult
import com.smarthome.hume.core.data.HumeGraph
import com.smarthome.hume.core.datastore.AiProvider
import com.smarthome.hume.core.datastore.AiSettings
import com.smarthome.hume.core.datastore.ThemeStore
import com.smarthome.hume.core.ui.theme.M3ESeed
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Trang Toi (demo v4 rev12: dong bo, thong bao, giao dien 8 seeds). */
class MeViewModel : ViewModel() {
    private val themeStore: ThemeStore = HumeGraph.get().themeStore
    private val syncRepo = HumeGraph.get().syncRepository

    val isConnected = syncRepo.isConnected

    /** Chu ky sync theo demo: bam/tu dong -> syncing 4.1s -> xong an loader. */
    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    fun doSync() {
        if (_syncing.value) return
        viewModelScope.launch {
            _syncing.value = true
            delay(4100)
            _syncing.value = false
        }
    }

    init {
        // demo: setTimeout(doSync, 1400)
        viewModelScope.launch {
            delay(1400)
            doSync()
        }
    }

    val seed: StateFlow<M3ESeed> = themeStore.settings
        .map { s -> runCatching { M3ESeed.valueOf(s.seedName) }.getOrDefault(M3ESeed.Cam) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, M3ESeed.Cam)

    val darkMode: StateFlow<Boolean?> = themeStore.settings
        .map { s -> s.darkMode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setSeed(seed: M3ESeed) {
        viewModelScope.launch { themeStore.setSeed(seed.name) }
    }

    fun setDarkMode(dark: Boolean) {
        viewModelScope.launch { themeStore.setDarkMode(dark) }
    }

    // ---------- AI ----------

    private val aiSettingsStore = HumeGraph.get().aiSettingsStore
    private val aiRepository = HumeGraph.get().aiRepository

    val aiSettings: StateFlow<AiSettings> = aiSettingsStore.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AiSettings())

    private val _aiTesting = MutableStateFlow(false)
    val aiTesting: StateFlow<Boolean> = _aiTesting.asStateFlow()

    private val _aiTestResult = MutableStateFlow<String?>(null)
    val aiTestResult: StateFlow<String?> = _aiTestResult.asStateFlow()

    fun saveAi(
        enabled: Boolean,
        provider: AiProvider,
        baseUrl: String,
        model: String,
        apiKey: String,
    ) {
        viewModelScope.launch {
            aiSettingsStore.save(enabled, provider, baseUrl, model, apiKey)
        }
    }

    fun testAiConnection() {
        if (_aiTesting.value) return
        viewModelScope.launch {
            _aiTesting.value = true
            _aiTestResult.value = null
            val result = aiRepository.testConnection()
            _aiTestResult.value = when (result) {
                is AiResult.Ok -> result.value
                is AiResult.Err -> result.message
            }
            _aiTesting.value = false
        }
    }
}
