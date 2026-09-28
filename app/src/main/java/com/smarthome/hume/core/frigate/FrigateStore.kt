package com.smarthome.hume.core.frigate

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

// Port of Core/FrigateStore.swift.
// Frigate clips are pulled to local storage first and played from file, which
// is what the iOS build does: streaming straight from Frigate is unreliable on
// mobile. Ten clips per camera, person / car / unlabelled only.

@Serializable
data class FrigateRecording(
    val id: String,
    val camera: String,
    val label: String,
    val startTime: Double,
    val clipFile: String,
    val thumbFile: String,
)

class FrigateStore private constructor(context: Context) {

    private val dir = File(context.filesDir, "frigate").apply { mkdirs() }
    private val indexFile = File(dir, "index.json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val indexSerializer =
        MapSerializer(String.serializer(), ListSerializer(FrigateRecording.serializer()))
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _byCamera = MutableStateFlow(load())
    val byCamera: StateFlow<Map<String, List<FrigateRecording>>> = _byCamera.asStateFlow()

    private val _downloading = MutableStateFlow<Set<String>>(emptySet())
    val downloading: StateFlow<Set<String>> = _downloading.asStateFlow()

    private val _lastError = MutableStateFlow<Map<String, String>>(emptyMap())
    val lastError: StateFlow<Map<String, String>> = _lastError.asStateFlow()

    fun recordings(camera: String): List<FrigateRecording> = _byCamera.value[camera].orEmpty()

    fun clipFile(recording: FrigateRecording): File = File(dir, recording.clipFile)

    fun thumbFile(recording: FrigateRecording): File = File(dir, recording.thumbFile)

    private fun load(): Map<String, List<FrigateRecording>> {
        if (!indexFile.exists()) return emptyMap()
        return runCatching {
            json.decodeFromString(indexSerializer, indexFile.readText())
        }.getOrElse { emptyMap() }
    }

    private fun save() {
        runCatching { indexFile.writeText(json.encodeToString(indexSerializer, _byCamera.value)) }
    }

    /**
     * Fetch the newest events for one camera and cache clip + snapshot locally.
     * The candidate list mirrors the Swift one: the Home Assistant proxy with a
     * bearer token first for events, the LAN address first for the media files.
     */
    suspend fun refresh(camera: String, haUrl: String, token: String) = withContext(Dispatchers.IO) {
        if (_downloading.value.contains(camera)) return@withContext
        _downloading.value = _downloading.value + camera
        _lastError.value = _lastError.value - camera

        val proxy = haUrl.trimEnd('/') + "/frigate"
        val old = recordings(camera)

        val eventsRaw = fetchFirst(
            listOf(
                proxy + "/api/events?cameras=" + camera + "&limit=15" to token,
                proxy + "/api/events?camera=" + camera + "&limit=15" to token,
                FRIGATE + "/api/events?cameras=" + camera + "&limit=15" to null,
                FRIGATE + "/api/events?camera=" + camera + "&limit=15" to null,
            ),
        )

        val events = eventsRaw?.let { bytes ->
            runCatching {
                json.parseToJsonElement(String(bytes)) as JsonArray
            }.getOrNull()
        }

        if (events == null || events.isEmpty()) {
            _lastError.value = _lastError.value +
                (camera to "Kh\u00f4ng l\u1ea5y \u0111\u01b0\u1ee3c danh s\u00e1ch s\u1ef1 ki\u1ec7n t\u1eeb Frigate.")
            _downloading.value = _downloading.value - camera
            return@withContext
        }

        val allowed = setOf("person", "car", "vehicle", "")
        val top = events
            .mapNotNull { element ->
                val obj = element as? JsonObject ?: return@mapNotNull null
                val id = obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val label = obj["label"]?.jsonPrimitive?.content.orEmpty()
                val start = obj["start_time"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                val hasClip = obj["has_clip"]?.jsonPrimitive?.booleanOrNull ?: true
                if (!hasClip || label !in allowed) null else Triple(id, label, start)
            }
            .sortedByDescending { it.third }
            .take(10)

        val fresh = mutableListOf<FrigateRecording>()
        for ((id, label, start) in top) {
            val clipName = camera + "_" + id + ".mp4"
            val thumbName = camera + "_" + id + ".jpg"
            val clip = File(dir, clipName)
            val thumb = File(dir, thumbName)

            if (!clip.exists()) {
                fetchFirst(
                    listOf(
                        FRIGATE + "/api/events/" + id + "/clip.mp4" to null,
                        proxy + "/api/events/" + id + "/clip.mp4" to token,
                    ),
                )?.let { clip.writeBytes(it) }
            }
            if (!thumb.exists()) {
                fetchFirst(
                    listOf(
                        FRIGATE + "/api/events/" + id + "/snapshot.jpg" to null,
                        proxy + "/api/events/" + id + "/snapshot.jpg" to token,
                    ),
                )?.let { thumb.writeBytes(it) }
            }
            if (clip.exists()) {
                fresh += FrigateRecording(id, camera, label, start, clipName, thumbName)
            }
        }

        // Drop the files that fell out of the newest ten.
        val keep = fresh.flatMap { listOf(it.clipFile, it.thumbFile) }.toSet()
        old.flatMap { listOf(it.clipFile, it.thumbFile) }
            .filterNot { it in keep }
            .forEach { runCatching { File(dir, it).delete() } }

        _byCamera.value = _byCamera.value + (camera to fresh)
        if (fresh.isEmpty()) {
            _lastError.value = _lastError.value + (camera to
                "T\u1ea3i clip th\u1ea5t b\u1ea1i \u2014 kh\u00f4ng k\u1ebft n\u1ed1i \u0111\u01b0\u1ee3c Frigate (c\u00f9ng m\u1ea1ng LAN?).")
        }
        _downloading.value = _downloading.value - camera
        save()
    }

    private fun fetchFirst(candidates: List<Pair<String, String?>>): ByteArray? {
        for ((url, token) in candidates) {
            val bytes = fetch(url, token)
            if (bytes != null) return bytes
        }
        return null
    }

    private fun fetch(url: String, token: String?): ByteArray? = runCatching {
        val builder = Request.Builder().url(url)
        if (!token.isNullOrBlank()) builder.header("Authorization", "Bearer " + token)
        client.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) return null
            response.body?.bytes()
        }
    }.getOrElse {
        Log.w(TAG, "Frigate fetch failed: " + url, it)
        null
    }

    companion object {
        const val FRIGATE = "http://192.168.102.64:5000"
        private const val TAG = "HumeHA"

        @Volatile
        private var instance: FrigateStore? = null

        fun get(context: Context): FrigateStore =
            instance ?: synchronized(this) {
                instance ?: FrigateStore(context.applicationContext).also { instance = it }
            }
    }
}
