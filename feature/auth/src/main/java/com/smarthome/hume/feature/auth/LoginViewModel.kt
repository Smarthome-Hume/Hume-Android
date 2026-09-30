package com.smarthome.hume.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.smarthome.hume.core.data.AuthRepository
import com.smarthome.hume.core.data.HumeGraph
import com.smarthome.hume.core.model.AuthResult
import com.smarthome.hume.core.model.isLocalHaUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject

/** 3 cach nhap token. */
enum class TokenEntryMode { Manual, Qr, Scan }

/** IP mac dinh cho duong noi bo (user khong sua thi dung luon). */
const val DEFAULT_LOCAL_URL = "http://192.168.102.22:8123"

/** Domain mac dinh cho duong tu xa qua Cloudflare. */
const val DEFAULT_REMOTE_URL = "https://haiha93.xyz"

data class LoginUiState(
    val localUrl: String = DEFAULT_LOCAL_URL,
    val remoteUrl: String = DEFAULT_REMOTE_URL,
    val token: String = "",
    val tokenVisible: Boolean = false,
    val entryMode: TokenEntryMode = TokenEntryMode.Manual,
    val rememberMe: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
) {
    val canSubmit: Boolean get() =
        (localUrl.isNotBlank() || remoteUrl.isNotBlank()) && token.isNotBlank() && !isLoading

    /**
     * Payload JSON cho ma QR: thiet bi khac quet de dang nhap.
     * null khi chua du URL + token.
     */
    val qrPayload: String? get() =
        if ((localUrl.isNotBlank() || remoteUrl.isNotBlank()) && token.isNotBlank()) {
            JSONObject()
                .put("localUrl", localUrl.trim())
                .put("remoteUrl", remoteUrl.trim())
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
            _uiState.update {
                it.copy(
                    localUrl = saved.localUrl.ifBlank { DEFAULT_LOCAL_URL },
                    remoteUrl = saved.remoteUrl.ifBlank { DEFAULT_REMOTE_URL },
                )
            }
        }
    }

    fun onLocalUrlChange(v: String) = _uiState.update { it.copy(localUrl = v, error = null) }
    fun onRemoteUrlChange(v: String) = _uiState.update { it.copy(remoteUrl = v, error = null) }
    fun onTokenChange(v: String) = _uiState.update { it.copy(token = v, error = null) }
    fun onToggleTokenVisibility() = _uiState.update { it.copy(tokenVisible = !it.tokenVisible) }
    fun onEntryModeChange(m: TokenEntryMode) =
        _uiState.update { it.copy(entryMode = m, error = null, notice = null) }
    fun onRememberMeChange(v: Boolean) = _uiState.update { it.copy(rememberMe = v) }
    fun onDismissError() = _uiState.update { it.copy(error = null) }
    fun onDismissNotice() = _uiState.update { it.copy(notice = null) }
    fun onCopiedQr() = _uiState.update { it.copy(notice = "Đã sao chép nội dung mã QR.") }

    /** URL hop le: trong duoc (neu duong kia co), khong thi phai bat dau bang http(s)://. */
    private fun urlError(label: String, url: String): String? {
        val t = url.trim()
        return when {
            t.isBlank() -> null
            !t.startsWith("http://") && !t.startsWith("https://") ->
                "Địa chỉ $label phải bắt đầu bằng http:// hoặc https://"
            else -> null
        }
    }

    fun onLogin() {
        val s = _uiState.value
        if (s.isLoading) return
        if (s.localUrl.isBlank() && s.remoteUrl.isBlank()) {
            _uiState.update { it.copy(error = "Nhập địa chỉ máy chủ (nội bộ hoặc domain).") }
            return
        }
        urlError("nội bộ", s.localUrl)?.let { msg ->
            _uiState.update { it.copy(error = msg) }
            return
        }
        urlError("domain", s.remoteUrl)?.let { msg ->
            _uiState.update { it.copy(error = msg) }
            return
        }
        if (s.token.isBlank()) {
            _uiState.update { it.copy(error = "Nhập Long-Lived Access Token.") }
            return
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            // Thu local truoc, that bai thi thu remote — chi can 1 duong toi la duoc.
            when (val r = authRepository.login(s.localUrl, s.remoteUrl, s.token, s.rememberMe)) {
                is AuthResult.Success -> _uiState.update { it.copy(isLoading = false) }
                is AuthResult.Error -> _uiState.update { it.copy(isLoading = false, error = r.message) }
            }
        }
    }

    /**
     * Xu ly chuoi quet duoc tu camera.
     * Uu tien JSON moi {"localUrl":..., "remoteUrl":..., "token":...};
     * JSON cu {"url":..., "token":...} thi doan local/remote theo dia chi.
     * Neu khong parse duoc thi doan: bat dau bang http -> URL, con lai -> token.
     */
    fun onScanResult(raw: String) {
        val text = raw.trim()
        if (text.isEmpty()) return
        var localUrl = ""
        var remoteUrl = ""
        var token = ""
        runCatching {
            val o = JSONObject(text)
            localUrl = o.optString("localUrl")
            remoteUrl = o.optString("remoteUrl")
            token = o.optString("token")
            // JSON cu: chi co "url"
            o.optString("url").takeIf { it.isNotBlank() }?.let { oldUrl ->
                if (isLocalHaUrl(oldUrl)) localUrl = oldUrl else remoteUrl = oldUrl
            }
        }
        if ((localUrl.isNotBlank() || remoteUrl.isNotBlank()) && token.isNotBlank()) {
            _uiState.update {
                it.copy(
                    localUrl = localUrl.ifBlank { it.localUrl },
                    remoteUrl = remoteUrl.ifBlank { it.remoteUrl },
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
                val key = if (isLocalHaUrl(text)) it.copy(localUrl = text) else it.copy(remoteUrl = text)
                key.copy(
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
