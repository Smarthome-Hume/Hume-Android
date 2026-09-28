package com.smarthome.hume.core.network

import com.smarthome.hume.core.model.AuthResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLHandshakeException

/**
 * Kiem tra URL + token co noi duoc Home Assistant khong.
 * GET {url}/api/ kem Bearer token -> 200 + {"message":"API running"}.
 */
class HaAuthValidator(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun validate(rawUrl: String, rawToken: String): AuthResult = withContext(Dispatchers.IO) {
        val url = normalizeUrl(rawUrl)
            ?: return@withContext AuthResult.Error("Địa chỉ máy chủ chưa đúng. Ví dụ: http://192.168.1.10:8123")
        val token = rawToken.trim()
        if (token.isEmpty()) {
            return@withContext AuthResult.Error("Hãy dán Long-Lived Access Token của Home Assistant.")
        }
        try {
            val req = Request.Builder()
                .url("$url/api/")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                return@withContext when {
                    resp.code == 200 && isApiRunning(body) -> AuthResult.Success
                    resp.code == 401 -> AuthResult.Error("Token không đúng hoặc đã hết hạn. Hãy tạo token mới trong HA.")
                    resp.code == 404 -> AuthResult.Error("Địa chỉ này không phải máy chủ Home Assistant.")
                    else -> AuthResult.Error("Máy chủ trả về lỗi HTTP ${resp.code}.")
                }
            }
        } catch (e: UnknownHostException) {
            AuthResult.Error("Không tìm thấy máy chủ. Kiểm tra địa chỉ hoặc kết nối mạng.")
        } catch (e: ConnectException) {
            AuthResult.Error("Không kết nối được tới $url. Máy chủ có đang bật không?")
        } catch (e: SocketTimeoutException) {
            AuthResult.Error("Máy chủ phản hồi quá chậm. Thử lại sau.")
        } catch (e: SSLHandshakeException) {
            AuthResult.Error("Lỗi chứng chỉ bảo mật (HTTPS). Kiểm tra lại địa chỉ https.")
        } catch (e: IllegalArgumentException) {
            AuthResult.Error("Địa chỉ máy chủ chưa đúng định dạng.")
        } catch (e: Exception) {
            AuthResult.Error("Không kết nối được: ${e.message ?: "lỗi không xác định"}")
        }
    }

    private fun isApiRunning(body: String): Boolean = try {
        Json.parseToJsonElement(body)
            .let { it is kotlinx.serialization.json.JsonObject && it["message"]?.toString()?.contains("API running") == true }
    } catch (e: Exception) {
        false
    }

    /**
     * Lay avatar user tu HA: GET /api/auth/current_user -> user id,
     * roi tim person entity co attributes.user_id khop -> attributes.entity_picture.
     * Tra ve URL tuyet doi hoac "" neu khong co.
     */
    suspend fun fetchAvatarUrl(rawUrl: String, rawToken: String): String =
        withContext(Dispatchers.IO) {
            val url = normalizeUrl(rawUrl) ?: return@withContext ""
            val token = rawToken.trim()
            if (token.isEmpty()) return@withContext ""
            try {
                val userId = client.newCall(
                    Request.Builder()
                        .url("$url/api/auth/current_user")
                        .header("Authorization", "Bearer $token")
                        .get().build(),
                ).execute().use { resp ->
                    if (resp.code != 200) return@withContext ""
                    val obj = Json.parseToJsonElement(resp.body?.string().orEmpty())
                        as? kotlinx.serialization.json.JsonObject ?: return@withContext ""
                    obj["id"]?.toString()?.trim('"').orEmpty()
                }
                if (userId.isEmpty()) return@withContext ""
                client.newCall(
                    Request.Builder()
                        .url("$url/api/states")
                        .header("Authorization", "Bearer $token")
                        .get().build(),
                ).execute().use { resp ->
                    if (resp.code != 200) return@withContext ""
                    val arr = Json.parseToJsonElement(resp.body?.string().orEmpty())
                        as? kotlinx.serialization.json.JsonArray ?: return@withContext ""
                    for (el in arr) {
                        val obj = el as? kotlinx.serialization.json.JsonObject ?: continue
                        val eid = obj["entity_id"]?.toString()?.trim('"').orEmpty()
                        if (!eid.startsWith("person.")) continue
                        val attrs = obj["attributes"]
                            as? kotlinx.serialization.json.JsonObject ?: continue
                        if (attrs["user_id"]?.toString()?.trim('"') != userId) continue
                        val pic = attrs["entity_picture"]?.toString()?.trim('"').orEmpty()
                        if (pic.isBlank()) return@withContext ""
                        // entity_picture thuong la "/api/image/..." -> ghep base URL
                        return@withContext if (pic.startsWith("http")) pic else "$url$pic"
                    }
                    ""
                }
            } catch (e: Exception) {
                ""
            }
        }

    companion object {
        /** Chuan hoa URL: them http:// neu thieu scheme, bo dau / thua. */
        fun normalizeUrl(raw: String): String? {
            var u = raw.trim().trimEnd('/')
            if (u.isEmpty()) return null
            if (!u.contains("://")) u = "http://$u"
            return try {
                val parsed = java.net.URI(u)
                if (parsed.host.isNullOrBlank()) null else u
            } catch (e: Exception) {
                null
            }
        }
    }
}
