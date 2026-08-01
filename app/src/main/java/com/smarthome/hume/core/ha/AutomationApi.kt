package com.smarthome.hume.core.ha

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Android side of `createAutomation` in HomeAssistantManager.swift.
 *
 * Home Assistant stores UI automations through the config API, so this posts a
 * raw config document to /api/config/automation/config/<id>. The id is a
 * timestamp, exactly like the iOS build, and HA reloads automations by itself
 * after the write. Returns false when the config integration is disabled or the
 * token lacks admin rights, which is what the original surfaces to the user.
 */
object AutomationApi {

    private const val TAG = "HumeHA"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonType = "application/json".toMediaType()

    suspend fun createAutomation(
        haUrl: String,
        token: String,
        alias: String,
        triggerJson: String,
        actionJson: String,
    ): Boolean = withContext(Dispatchers.IO) {
        if (haUrl.isBlank() || token.isBlank()) return@withContext false
        val id = System.currentTimeMillis().toString()
        val body = "{\"id\":\"" + id + "\",\"alias\":" + quote(alias) +
            ",\"description\":\"\",\"mode\":\"single\",\"trigger\":[" + triggerJson +
            "],\"condition\":[],\"action\":[" + actionJson + "]}"

        val request = Request.Builder()
            .url(haUrl.trimEnd('/') + "/api/config/automation/config/" + id)
            .header("Authorization", "Bearer " + token)
            .post(body.toRequestBody(jsonType))
            .build()

        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "createAutomation failed: " + response.code)
                }
                response.isSuccessful
            }
        }.getOrElse {
            Log.w(TAG, "createAutomation error", it)
            false
        }
    }

    /** Minimal JSON string escaping so Vietnamese aliases survive the POST. */
    fun quote(value: String): String {
        val sb = StringBuilder("\"")
        value.forEach { c ->
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(c)
            }
        }
        return sb.append('"').toString()
    }
}
