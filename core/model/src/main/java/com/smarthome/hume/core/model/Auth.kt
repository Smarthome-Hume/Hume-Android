package com.smarthome.hume.core.model

/** Phien dang nhap Home Assistant. */
data class AuthSession(
    val serverUrl: String = "",
    val token: String = "",
    val avatarUrl: String = "",
) {
    val isLoggedIn: Boolean get() = token.isNotBlank()
}

/** Ket qua thu ket noi toi HA. */
sealed interface AuthResult {
    data object Success : AuthResult
    data class Error(val message: String) : AuthResult
}
