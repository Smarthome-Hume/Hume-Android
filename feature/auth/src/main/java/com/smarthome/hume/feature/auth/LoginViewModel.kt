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

data class LoginUiState(
    val serverUrl: String = "",
    val token: String = "",
    val tokenVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    val canSubmit: Boolean get() = serverUrl.isNotBlank() && token.isNotBlank() && !isLoading
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
    fun onDismissError() = _uiState.update { it.copy(error = null) }

    fun onLogin() {
        val s = _uiState.value
        if (!s.canSubmit) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val r = authRepository.login(s.serverUrl, s.token)) {
                is AuthResult.Success -> _uiState.update { it.copy(isLoading = false) }
                is AuthResult.Error -> _uiState.update { it.copy(isLoading = false, error = r.message) }
            }
        }
    }

    companion object {
        fun factory(): ViewModelProvider.Factory = viewModelFactory {
            initializer { LoginViewModel(HumeGraph.get().authRepository) }
        }
    }
}
