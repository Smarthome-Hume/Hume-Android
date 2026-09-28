package com.smarthome.hume.core.data

import com.smarthome.hume.core.datastore.SessionStore
import com.smarthome.hume.core.model.AuthResult
import com.smarthome.hume.core.model.AuthSession
import com.smarthome.hume.core.network.HaAuthValidator
import kotlinx.coroutines.flow.Flow

/**
 * Dieu phoi dang nhap/dang xuat HA.
 * Chi luu token SAU KHI validate thanh cong — khong bao gio luu token sai.
 */
class AuthRepository(
    private val sessionStore: SessionStore,
    private val validator: HaAuthValidator = HaAuthValidator(),
) {
    val session: Flow<AuthSession> = sessionStore.session

    suspend fun login(rawUrl: String, rawToken: String): AuthResult {
        val result = validator.validate(rawUrl, rawToken)
        if (result is AuthResult.Success) {
            val url = HaAuthValidator.normalizeUrl(rawUrl) ?: rawUrl.trim().trimEnd('/')
            sessionStore.save(url, rawToken.trim())
        }
        return result
    }

    suspend fun logout() {
        sessionStore.clear()
    }

    fun refresh() = sessionStore.refresh()
}
