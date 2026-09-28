package com.smarthome.hume.feature.me

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.hume.core.data.HumeGraph
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
}
