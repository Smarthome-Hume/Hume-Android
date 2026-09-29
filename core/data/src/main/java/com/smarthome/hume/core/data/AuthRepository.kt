package com.smarthome.hume.core.data

import com.smarthome.hume.core.datastore.SessionStore
import com.smarthome.hume.core.model.AuthResult
import com.smarthome.hume.core.model.AuthSession
import com.smarthome.hume.core.network.HaAuthValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

/**
 * Dieu phoi dang nhap/dang xuat HA.
 * Chi luu token SAU KHI validate thanh cong — khong bao gio luu token sai.
 *
 * rememberMe = false: phien chi ton tai trong RAM (khong ghi DataStore/
 * EncryptedSharedPreferences), mo app lai phai dang nhap lai.
 */
class AuthRepository(
    private val sessionStore: SessionStore,
    private val validator: HaAuthValidator = HaAuthValidator(),
) {
    /** Phien tam chi giu trong RAM khi user khong tick "Ghi nho dang nhap". */
    private val memorySession = MutableStateFlow<AuthSession?>(null)

    val session: Flow<AuthSession> =
        combine(memorySession, sessionStore.session) { mem, stored -> mem ?: stored }

    suspend fun login(rawUrl: String, rawToken: String, rememberMe: Boolean = true): AuthResult {
        val result = validator.validate(rawUrl, rawToken)
        if (result is AuthResult.Success) {
            val url = HaAuthValidator.normalizeUrl(rawUrl) ?: rawUrl.trim().trimEnd('/')
            val token = rawToken.trim()
            // Lay avatar user tu HA (khong bat buoc, that bai -> bo qua)
            val avatar = runCatching { validator.fetchAvatarUrl(url, token) }
                .getOrDefault("")
            if (rememberMe) {
                memorySession.value = null
                sessionStore.save(url, token, avatar)
            } else {
                memorySession.value = AuthSession(
                    serverUrl = url,
                    token = token,
                    avatarUrl = avatar,
                )
            }
        }
        return result
    }

    suspend fun logout() {
        memorySession.value = null
        sessionStore.clear()
    }

    fun refresh() = sessionStore.refresh()
}
