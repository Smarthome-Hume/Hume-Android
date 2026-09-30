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

    suspend fun login(rawUrl: String, rawToken: String, rememberMe: Boolean = true): AuthResult =
        login(rawLocalUrl = rawUrl, rawRemoteUrl = "", rawToken = rawToken, rememberMe = rememberMe)

    /**
     * Dang nhap voi 2 duong song song: thu local truoc (nhanh), that bai thi thu remote.
     * Chi can 1 trong 2 toi duoc la dang nhap thanh cong; luu ca 2 URL de app tu chuyen.
     */
    suspend fun login(
        rawLocalUrl: String,
        rawRemoteUrl: String,
        rawToken: String,
        rememberMe: Boolean = true,
    ): AuthResult {
        val token = rawToken.trim()
        if (token.isBlank()) return AuthResult.Error("Nhập Long-Lived Access Token.")
        val local = HaAuthValidator.normalizeUrl(rawLocalUrl) ?: rawLocalUrl.trim().trimEnd('/')
        val remote = HaAuthValidator.normalizeUrl(rawRemoteUrl) ?: rawRemoteUrl.trim().trimEnd('/')
        val candidates = listOf(local, remote).filter { it.isNotBlank() }.distinct()
        if (candidates.isEmpty()) return AuthResult.Error("Nhập địa chỉ máy chủ (nội bộ hoặc domain).")
        var lastError: AuthResult.Error? = null
        for (url in candidates) {
            when (val result = validator.validate(url, token)) {
                is AuthResult.Success -> {
                    // Lay avatar user tu HA (khong bat buoc, that bai -> bo qua)
                    val avatar = runCatching { validator.fetchAvatarUrl(url, token) }
                        .getOrDefault("")
                    if (rememberMe) {
                        memorySession.value = null
                        sessionStore.save(local, remote, token, avatar)
                    } else {
                        memorySession.value = AuthSession(
                            localUrl = local,
                            remoteUrl = remote,
                            token = token,
                            avatarUrl = avatar,
                        )
                    }
                    return AuthResult.Success
                }
                is AuthResult.Error -> lastError = result
            }
        }
        return lastError ?: AuthResult.Error("Không kết nối được máy chủ.")
    }

    suspend fun logout() {
        memorySession.value = null
        sessionStore.clear()
    }

    fun refresh() = sessionStore.refresh()
}
