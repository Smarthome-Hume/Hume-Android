package com.smarthome.hume.feature.me

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

/**
 * Danh sach Google Fonts tu nguon cong khai (google-webfonts-helper API).
 * Loc uu tien font ho tro tieng Viet vi UI dung tieng Viet.
 */
data class GoogleFontInfo(
    val id: String,
    val family: String,
    val category: String,
    val supportsVietnamese: Boolean,
    val popularity: Int,
)

object GoogleFontsApi {
    private var cache: List<GoogleFontInfo>? = null

    /**
     * Lay danh sach font, sap xep: ho tro Viet len truoc, roi theo do pho bien.
     * Cache trong RAM cho lan sau.
     */
    suspend fun fetchFonts(): List<GoogleFontInfo> = withContext(Dispatchers.IO) {
        cache?.let { return@withContext it }
        val result = runCatching {
            val url = URL("https://gwfh.mranftl.com/api/fonts")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 20000
                setRequestProperty("Accept", "application/json")
            }
            try {
                val body = conn.inputStream.bufferedReader().readText()
                parseFonts(body)
            } finally {
                conn.disconnect()
            }
        }.getOrElse { emptyList() }
        val sorted = result.sortedWith(
            compareBy({ !it.supportsVietnamese }, { it.popularity })
        )
        cache = sorted
        sorted
    }

    private fun parseFonts(body: String): List<GoogleFontInfo> {
        val arr = JSONArray(body)
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            val subsets = o.optJSONArray("subsets")?.let { s ->
                List(s.length()) { j -> s.optString(j) }
            } ?: emptyList()
            GoogleFontInfo(
                id = o.optString("id"),
                family = o.optString("family"),
                category = o.optString("category"),
                supportsVietnamese = "vietnamese" in subsets,
                popularity = o.optInt("popularity", Int.MAX_VALUE),
            )
        }.filter { it.family.isNotBlank() }
    }
}
