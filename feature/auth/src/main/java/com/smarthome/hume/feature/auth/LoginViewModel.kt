package com.smarthome.hume.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.smarthome.hume.core.data.AuthRepository
import com.smarthome.hume.core.data.HumeGraph
import com.smarthome.hume.core.model.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

/** Segment chon loai dia chi may chu. */
enum class ServerMode { Local, Domain }

/** 3 cach nhap token. */
enum class TokenEntryMode { Manual, Qr, Scan }

/** IP mac dinh cho che do Noi bo (user khong sua thi dung luon). */
const val DEFAULT_LOCAL_URL = "http://192.168.102.22:8123"

data class LoginUiState(
    val serverUrl: String = DEFAULT_LOCAL_URL,
    val token: String = "",
    val tokenVisible: Boolean = false,
    val serverMode: ServerMode = ServerMode.Local,
    val entryMode: TokenEntryMode = TokenEntryMode.Manual,
    val rememberMe: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
) {
    val canSubmit: Boolean get() = serverUrl.isNotBlank() && token.isNotBlank() && !isLoading

    /**
     * Payload JSON cho ma QR: thiet bi khac quet de dang nhap.
     * null khi chua du URL + token.
     */
    val qrPayload: String? get() =
        if (serverUrl.isNotBlank() && token.isNotBlank()) {
            JSONObject()
                .put("url", serverUrl.trim())
                .put("token", token.trim())
                .toString()
        } else null
}

class LoginViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = authRepository.session.first()
            if (saved.serverUrl.isNotBlank()) {
                _uiState.update { it.copy(serverUrl = saved.serverUrl) }
            }
        }
    }

    fun onUrlChange(v: String) = _uiState.update { it.copy(serverUrl = v, error = null) }
    fun onTokenChange(v: String) = _uiState.update { it.copy(token = v, error = null) }
    fun onToggleTokenVisibility() = _uiState.update { it.copy(tokenVisible = !it.tokenVisible) }

    /**
     * Doi che do Noi bo/Domain: neu URL dang trong hoac dang la default cua che do cu
     * thi tu dong dien default cua che do moi (Local -> IP mac dinh, Domain -> trong).
     */
    fun onServerModeChange(m: ServerMode) = _uiState.update { s ->
        val oldDefault = if (s.serverMode == ServerMode.Local) DEFAULT_LOCAL_URL else ""
        val newUrl = if (s.serverUrl.isBlank() || s.serverUrl.trim() == oldDefault) {
            if (m == ServerMode.Local) DEFAULT_LOCAL_URL else ""
        } else s.serverUrl
        s.copy(serverMode = m, serverUrl = newUrl, error = null)
    }
    fun onEntryModeChange(m: TokenEntryMode) =
        _uiState.update { it.copy(entryMode = m, error = null, notice = null) }
    fun onRememberMeChange(v: Boolean) = _uiState.update { it.copy(rememberMe = v) }
    fun onDismissError() = _uiState.update { it.copy(error = null) }
    fun onDismissNotice() = _uiState.update { it.copy(notice = null) }
    fun onCopiedQr() = _uiState.update { it.copy(notice = "Đã sao chép nội dung mã QR.") }

    /** URL hop le phai bat dau bang http:// hoac https://. */
    private fun urlError(url: String): String? {
        val t = url.trim()
        return when {
            t.isBlank() -> "Nhập địa chỉ máy chủ Home Assistant."
            !t.startsWith("http://") && !t.startsWith("https://") ->
                "Địa chỉ phải bắt đầu bằng http:// hoặc https://"
            else -> null
        }
    }

    fun onLogin() {
        val s = _uiState.value
        if (s.isLoading) return
        urlError(s.serverUrl)?.let { msg ->
            _uiState.update { it.copy(error = msg) }
            return
        }
        if (s.token.isBlank()) {
            _uiState.update { it.copy(error = "Nhập Long-Lived Access Token.") }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val r = authRepository.login(s.serverUrl, s.token, s.rememberMe)) {
                is AuthResult.Success -> _uiState.update { it.copy(isLoading = false) }
                is AuthResult.Error -> _uiState.update { it.copy(isLoading = false, error = r.message) }
            }
        }
    }

    /**
     * Xu ly chuoi quet duoc tu camera.
     * Uu tien JSON {"url":..., "token":...}; neu khong parse duoc thi doan:
     * bat dau bang http -> URL, con lai -> token.
     */
    fun onScanResult(raw: String) {
        val text = raw.trim()
        if (text.isEmpty()) return
        var url = ""
        var token = ""
        runCatching {
            val o = JSONObject(text)
            url = o.optString("url")
            token = o.optString("token")
        }
        if (url.isNotBlank() && token.isNotBlank()) {
            _uiState.update {
                it.copy(
                    serverUrl = url,
                    token = token,
                    entryMode = TokenEntryMode.Manual,
                    error = null,
                    notice = "Đã quét mã QR thành công.",
                )
            }
            return
        }
        if (text.startsWith("http://") || text.startsWith("https://")) {
            _uiState.update {
                it.copy(
                    serverUrl = text,
                    entryMode = TokenEntryMode.Manual,
                    error = null,
                    notice = "Đã điền địa chỉ máy chủ từ mã QR.",
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    token = text,
                    entryMode = TokenEntryMode.Manual,
                    error = null,
                    notice = "Đã điền token từ mã QR.",
                )
            }
        }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer { LoginViewModel(HumeGraph.get().authRepository) }
        }
    }
}
