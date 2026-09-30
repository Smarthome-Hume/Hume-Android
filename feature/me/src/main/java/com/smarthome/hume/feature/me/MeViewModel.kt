package com.smarthome.hume.feature.me

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.hume.core.data.AiResult
import com.smarthome.hume.core.data.HumeGraph
import com.smarthome.hume.core.datastore.AiProvider
import com.smarthome.hume.core.datastore.AiSettings
import com.smarthome.hume.core.datastore.FabMenuStore
import com.smarthome.hume.core.datastore.ThemeStore
import com.smarthome.hume.core.model.FrigateRemoteConfig
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
    val fabMenuStore: FabMenuStore = HumeGraph.get().fabMenuStore

    val isConnected = syncRepo.isConnected

    /** Key avatar hien tai, dung chung voi header trang Nha. */
    val userKey: StateFlow<String> = HumeGraph.get().userKey

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
        viewModelScope.launch {
            themeStore.setCustomColor(null) // chon seed co san -> xoa mau custom
            themeStore.setSeed(seed.name)
        }
    }

    /** Ap dung mau tuy chinh truc tiep (khong snap ve seed gan nhat). */
    fun setCustomColor(argb: Long) {
        viewModelScope.launch { themeStore.setCustomColor(argb) }
    }

    fun setDarkMode(dark: Boolean?) {
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

    // ---------- Camera tu xa (Frigate qua Cloudflare) ----------

    private val sessionStore = HumeGraph.get().sessionStore

    val frigateRemote = sessionStore.frigateRemote
        .stateIn(viewModelScope, SharingStarted.Eagerly, FrigateRemoteConfig())

    fun saveFrigateRemote(url: String, cfClientId: String, cfClientSecret: String) {
        viewModelScope.launch {
            sessionStore.saveFrigateRemote(url, cfClientId, cfClientSecret)
        }
    }

    // ---------- Font chu (Google Fonts) ----------

    val fontFamily: StateFlow<String> = themeStore.settings
        .map { it.fontFamily }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "Montserrat")

    private val _fonts = MutableStateFlow<List<GoogleFontInfo>>(emptyList())
    val fonts: StateFlow<List<GoogleFontInfo>> = _fonts.asStateFlow()

    private val _fontsLoading = MutableStateFlow(false)
    val fontsLoading: StateFlow<Boolean> = _fontsLoading.asStateFlow()

    fun loadFonts() {
        if (_fonts.value.isNotEmpty() || _fontsLoading.value) return
        viewModelScope.launch {
            _fontsLoading.value = true
            _fonts.value = GoogleFontsApi.fetchFonts()
            _fontsLoading.value = false
        }
    }

    fun setFontFamily(family: String) {
        viewModelScope.launch { themeStore.setFontFamily(family) }
    }
}
