package com.smarthome.hume.brief

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smarthome.hume.core.data.HumeGraph
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** ViewModel cho trang Brief: doc cache, refresh khi mo ma du lieu cu. */
class BriefViewModel(app: Application) : AndroidViewModel(app) {
    private var repo: BriefRepository? = null

    private val _cache = MutableStateFlow<BriefCache?>(null)
    val cache: StateFlow<BriefCache?> = _cache.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _hasNew = MutableStateFlow(false)
    val hasNew: StateFlow<Boolean> = _hasNew.asStateFlow()

    init {
        viewModelScope.launch {
            val graph = HumeGraph.get()
            val session = graph.authRepository.session.first()
            if (session.isLoggedIn) {
                repo = BriefRepository(
                    getApplication(), session.localUrl, session.remoteUrl,
                    session.token, graph.aiRepository,
                ).also {
                    _cache.value = it.loadCache()
                    _hasNew.value = it.hasNew.value
                }
            }
        }
    }

    /** Mo trang: neu brief ngay khong phai hom qua thi tao moi. Ngoai ra, neu
     * thoi tiet trong brief chua co vi tri ma quyen vi tri da duoc cap thi
     * chi thu lai phan thoi tiet (re, khong goi AI) de tu phuc hoi. */
    fun refreshIfStale() {
        val r = repo ?: return
        val yesterday = LocalDate.now(ZoneId.systemDefault()).minusDays(1)
            .format(DateTimeFormatter.ofPattern("dd/MM"))
        val c = _cache.value
        if (c?.daily?.dateLabel != yesterday) {
            refresh()
        } else if (c.daily?.weather?.hasLocation == false && r.hasLocationPermission()) {
            viewModelScope.launch {
                if (r.refreshWeatherOnly()) _cache.value = r.loadCache()
            }
        }
    }

    fun refresh() {
        val r = repo ?: return
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            try {
                r.refreshAll()
                _cache.value = r.loadCache()
                _hasNew.value = r.hasNew.value
            } finally {
                _refreshing.value = false
            }
        }
    }

    fun markSeen() {
        repo?.markSeen()
        _hasNew.value = false
    }
}
