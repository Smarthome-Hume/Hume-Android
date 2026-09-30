package com.smarthome.hume.core.model

/** Phien dang nhap Home Assistant: 2 duong song song local + remote. */
data class AuthSession(
    val localUrl: String = "",
    val remoteUrl: String = "",
    val token: String = "",
    val avatarUrl: String = "",
) {
    val isLoggedIn: Boolean get() = token.isNotBlank()

    /** Tuong thich nguoc voi ban cu chi co 1 URL. */
    val serverUrl: String get() = localUrl.ifBlank { remoteUrl }
}

/** Doan dia chi HA co phai mang noi bo khong (de migrate ha_url cu). */
fun isLocalHaUrl(url: String): Boolean {
    val u = url.trim().lowercase()
    return u.contains("192.168.") || u.contains("10.") ||
        u.contains("172.16.") || u.contains("172.17.") || u.contains("172.18.") ||
        u.contains("172.19.") || u.contains(".local") ||
        u.contains("127.0.0.1") || u.contains("localhost")
}

/** Ket qua thu ket noi toi HA. */
sealed interface AuthResult {
    data object Success : AuthResult
    data class Error(val message: String) : AuthResult
}

/**
 * Cau hinh xem Frigate tu xa qua Cloudflare Tunnel:
 * - remoteUrl: hostname Frigate tren tunnel (vd https://frigate.haiha93.xyz)
 * - cfClientId / cfClientSecret: Cloudflare Access Service Token de app
 *   vuot qua man hinh Access (Frigate goc khong co auth).
 */
data class FrigateRemoteConfig(
    val remoteUrl: String = "",
    val cfClientId: String = "",
    val cfClientSecret: String = "",
) {
    val hasAccess: Boolean get() = cfClientId.isNotBlank() && cfClientSecret.isNotBlank()
}
