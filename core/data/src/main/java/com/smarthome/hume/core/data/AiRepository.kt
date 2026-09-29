package com.smarthome.hume.core.data

import com.smarthome.hume.core.datastore.AiProvider
import com.smarthome.hume.core.datastore.AiSettings
import com.smarthome.hume.core.datastore.AiSettingsStore
import com.smarthome.hume.core.model.HomeUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/** Mot goi y tu AI (map vao Tip cua SuggestCard). */
data class AiTip(
    val key: String,
    val title: String,
    val sub: String,
    val action: String,
)

/** Ket qua goi AI: Thanh cong | Loi (chuoi tieng Viet hien UI). */
sealed interface AiResult<out T> {
    data class Ok<T>(val value: T) : AiResult<T>
    data class Err(val message: String) : AiResult<Nothing>
}

interface AiRepository {
    /** Chat don gian, tra ve text tra loi. */
    suspend fun chat(systemPrompt: String, userPrompt: String): AiResult<String>

    /** Test cau hinh hien tai: goi 1 cau sieu ngan, tra ve loi tieng Viet neu that bai. */
    suspend fun testConnection(): AiResult<String>

    /**
     * Phan tich trang thai nha -> danh sach goi y.
     * Tra ve emptyList() khi: chua cau hinh, loi mang, parse that bai
     * -> caller fallback ve rule-based tips.
     * KHONG gui: token HA, entity_id, ten user, toa do. Chi gui trang thai
     * thiet bi da an danh hoa (ten phong, on/off, so lieu).
     */
    suspend fun analyzeHome(state: HomeUiState): List<AiTip>
}

class AiRepositoryImpl(
    private val aiSettingsStore: AiSettingsStore,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build(),
) : AiRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun chat(systemPrompt: String, userPrompt: String): AiResult<String> =
        withContext(Dispatchers.IO) {
            val settings = aiSettingsStore.settings.firstConfigured()
                ?: return@withContext AiResult.Err("Chưa cấu hình AI. Mở tab Tôi → AI để nhập thông tin.")
            val apiKey = aiSettingsStore.readApiKey()
            if (apiKey.isBlank()) {
                return@withContext AiResult.Err("Chưa nhập API key.")
            }
            postChat(settings, apiKey, systemPrompt, userPrompt, maxTokens = 600)
        }

    override suspend fun testConnection(): AiResult<String> = withContext(Dispatchers.IO) {
        val settings = aiSettingsStore.settings.firstConfigured()
            ?: return@withContext AiResult.Err("Chưa cấu hình AI.")
        val apiKey = aiSettingsStore.readApiKey()
        if (apiKey.isBlank()) return@withContext AiResult.Err("Chưa nhập API key.")
        when (val r = postChat(
            settings, apiKey,
            systemPrompt = "Bạn là trợ lý kiểm tra kết nối. Chỉ trả lời đúng chuỗi: OK",
            userPrompt = "ping",
            maxTokens = 10,
        )) {
            is AiResult.Ok -> AiResult.Ok("Kết nối thành công (${settings.provider.label} · ${settings.effectiveModel}).")
            is AiResult.Err -> r
        }
    }

    override suspend fun analyzeHome(state: HomeUiState): List<AiTip> = withContext(Dispatchers.IO) {
        val settings = aiSettingsStore.settings.firstConfigured() ?: return@withContext emptyList()
        if (!settings.isConfigured) return@withContext emptyList()
        val apiKey = aiSettingsStore.readApiKey()
        if (apiKey.isBlank()) return@withContext emptyList()
        val userPrompt = buildHomePrompt(state)
        val result = postChat(settings, apiKey, SYSTEM_PROMPT, userPrompt, maxTokens = 500)
        if (result is AiResult.Ok) parseTips(result.value) else emptyList()
    }

    // ---------- prompt ----------

    /** Lay settings hien tai; null neu AI chua bat. */
    private suspend fun Flow<AiSettings>.firstConfigured(): AiSettings? {
        val s = first()
        return s.takeIf { it.enabled }
    }

    private fun buildHomePrompt(s: HomeUiState): String {
        val sb = StringBuilder()
        sb.appendLine("Trạng thái nhà hiện tại:")
        sb.appendLine("- Điện mặt trời đang phát: ${"%.1f".format(s.solarNowKw)} kW")
        s.solarTodayKwh?.let { sb.appendLine("- Sản lượng hôm nay: ${"%.1f".format(it)} kWh") }
        val bat = s.battery
        sb.appendLine("- Pin lưu trữ: ${bat.soc}% (${if (bat.isCharging) "đang sạc" else "đang xả/không đổi"})")
        s.alarm?.let { sb.appendLine("- An ninh: ${it.label}") }
        val onDevices = s.lightsOn.size
        sb.appendLine("- Số đèn đang bật: $onDevices")
        s.rooms.forEach { r ->
            val parts = mutableListOf<String>()
            if (r.lightOn) parts.add("đèn bật")
            r.tempC?.let { parts.add("${"%.0f".format(it)}°C") }
            r.humidityPct?.let { parts.add("ẩm $it%") }
            r.climate?.takeIf { it.isOn }?.let {
                parts.add("điều hòa ${it.targetTemp?.toInt() ?: "?"}°")
            }
            if (parts.isNotEmpty()) sb.appendLine("- ${r.name}: ${parts.joinToString(", ")}")
        }
        val doors = s.notifications.filter { it.title.contains("Cửa") }
        doors.forEach { sb.appendLine("- ${it.title}: ${it.body}") }
        s.notifications.filter { it.title == "Phát hiện chuyển động" }
            .forEach { sb.appendLine("- Đang có chuyển động: ${it.body} (${it.timeText})") }
        return sb.toString()
    }

    // ---------- HTTP ----------

    private fun postChat(
        settings: AiSettings,
        apiKey: String,
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int,
    ): AiResult<String> {
        return try {
            if (settings.provider == AiProvider.Anthropic) {
                postAnthropic(settings, apiKey, systemPrompt, userPrompt, maxTokens)
            } else {
                postOpenAiCompatible(settings, apiKey, systemPrompt, userPrompt, maxTokens)
            }
        } catch (e: java.net.UnknownHostException) {
            AiResult.Err("Không tìm thấy máy chủ AI. Kiểm tra mạng hoặc Base URL.")
        } catch (e: java.net.SocketTimeoutException) {
            AiResult.Err("AI phản hồi quá chậm. Thử lại sau.")
        } catch (e: java.net.ConnectException) {
            AiResult.Err("Không kết nối được tới máy chủ AI.")
        } catch (e: Exception) {
            AiResult.Err("Lỗi gọi AI: ${e.message ?: "không xác định"}")
        }
    }

    /** OpenAI / Google Gemini (endpoint openai-compat) / Custom. */
    private fun postOpenAiCompatible(
        settings: AiSettings,
        apiKey: String,
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int,
    ): AiResult<String> {
        val body = buildJsonObject {
            put("model", settings.effectiveModel)
            put("max_tokens", maxTokens)
            put("temperature", 0.7)
            putJsonArray("messages") {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                add(buildJsonObject {
                    put("role", "user")
                    put("content", userPrompt)
                })
            }
        }
        val req = Request.Builder()
            .url("${settings.baseUrl}/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (resp.code == 401) return AiResult.Err("API key không đúng hoặc đã hết hạn.")
            if (resp.code == 404) return AiResult.Err("Không tìm thấy model \"${settings.effectiveModel}\". Kiểm tra tên model.")
            if (resp.code == 429) return AiResult.Err("Bị giới hạn tần suất (429). Thử lại sau.")
            if (!resp.isSuccessful) return AiResult.Err("Máy chủ AI trả về lỗi HTTP ${resp.code}.")
            val content = extractOpenAiContent(text)
                ?: return AiResult.Err("Không đọc được phản hồi AI.")
            return AiResult.Ok(content)
        }
    }

    /** Anthropic native Messages API. */
    private fun postAnthropic(
        settings: AiSettings,
        apiKey: String,
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int,
    ): AiResult<String> {
        val body = buildJsonObject {
            put("model", settings.effectiveModel)
            put("max_tokens", maxTokens)
            put("system", systemPrompt)
            putJsonArray("messages") {
                add(buildJsonObject {
                    put("role", "user")
                    put("content", userPrompt)
                })
            }
        }
        val req = Request.Builder()
            .url("${settings.baseUrl}/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(req).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (resp.code == 401) return AiResult.Err("API key không đúng hoặc đã hết hạn.")
            if (resp.code == 404) return AiResult.Err("Không tìm thấy model \"${settings.effectiveModel}\".")
            if (resp.code == 429) return AiResult.Err("Bị giới hạn tần suất (429). Thử lại sau.")
            if (!resp.isSuccessful) return AiResult.Err("Máy chủ AI trả về lỗi HTTP ${resp.code}.")
            val content = extractAnthropicContent(text)
                ?: return AiResult.Err("Không đọc được phản hồi AI.")
            return AiResult.Ok(content)
        }
    }

    private fun extractOpenAiContent(text: String): String? = try {
        json.parseToJsonElement(text).jsonObject["choices"]
            ?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("message")?.jsonObject
            ?.get("content")?.jsonPrimitive?.content?.trim()
            ?.ifBlank { null }
    } catch (e: Exception) {
        null
    }

    private fun extractAnthropicContent(text: String): String? = try {
        json.parseToJsonElement(text).jsonObject["content"]
            ?.jsonArray?.firstOrNull()?.jsonObject
            ?.get("text")?.jsonPrimitive?.content?.trim()
            ?.ifBlank { null }
    } catch (e: Exception) {
        null
    }

    // ---------- parse tips ----------

    /**
     * Parse JSON tu AI: [{"title":"...","sub":"...","action":"..."}] (1-3 goi y).
     * Chat het suc chiu dung: boc trong ```json, text thua xung quanh.
     */
    private fun parseTips(raw: String): List<AiTip> {
        val cleaned = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()
        val start = cleaned.indexOf('[')
        val end = cleaned.lastIndexOf(']')
        if (start < 0 || end <= start) return emptyList()
        return try {
            val arr = json.parseToJsonElement(cleaned.substring(start, end + 1))
            check(arr is JsonArray)
            arr.take(3).mapIndexedNotNull { i, el ->
                if (el !is JsonObject) return@mapIndexedNotNull null
                val title = el["title"]?.jsonPrimitive?.content?.trim().orEmpty()
                val sub = el["sub"]?.jsonPrimitive?.content?.trim().orEmpty()
                if (title.isBlank()) return@mapIndexedNotNull null
                AiTip(
                    key = "ai_$i",
                    title = title.take(60),
                    sub = sub.take(120),
                    action = el["action"]?.jsonPrimitive?.content?.trim()
                        .orEmpty().ifBlank { "Đã hiểu" }.take(12),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        private val SYSTEM_PROMPT = """
Bạn là trợ lý nhà thông minh Hume. Dựa trên trạng thái nhà, đưa ra 1-3 gợi ý NGẮN GỌN, thiết thực bằng tiếng Việt để tiết kiệm điện, tăng an toàn, tiện nghi.
Nếu đang có chuyển động ở phòng nào, ưu tiên gợi ý xem camera phòng đó (action "Xem camera").
Chỉ trả về JSON thuần (không markdown, không giải thích thêm), đúng định dạng:
[{"title":"Tiêu đề ngắn","sub":"Mô tả 1 câu, có số liệu cụ thể nếu được","action":"Nhãn nút ≤4 từ"}]
Ví dụ: [{"title":"Pin còn 18%","sub":"Hạn chế tải nặng, chờ nắng lên sau 10h.","action":"Xem pin"}]
Nếu mọi thứ ổn, gợi ý 1 việc tối ưu nhỏ (ví dụ hẹn giờ, vệ sinh tấm pin).
""".trimIndent()
    }
}
