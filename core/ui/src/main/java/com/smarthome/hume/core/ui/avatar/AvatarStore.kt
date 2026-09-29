package com.smarthome.hume.core.ui.avatar

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import androidx.annotation.RawRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/** Avatar da luu cua mot user: file anh hoac video ngan. */
data class UserAvatar(val file: File, val isVideo: Boolean)

/**
 * Luu avatar (anh, hoac video ngan hon 1 phut) theo tung user vao bo nho
 * rieng cua app. Key SharedPreferences gan theo userId -> avatar mac dinh
 * "lay theo user": doi user la doi avatar.
 */
class AvatarStore(private val context: Context) {

    private val prefs = context.getSharedPreferences("hume_profile", Context.MODE_PRIVATE)
    private val dir = File(context.filesDir, "avatars").also { it.mkdirs() }

    private val _avatars = MutableStateFlow<Map<String, UserAvatar>>(emptyMap())
    val avatars: StateFlow<Map<String, UserAvatar>> = _avatars.asStateFlow()

    private fun keyPath(userId: String) = "avatar_path_$userId"
    private fun keyVideo(userId: String) = "avatar_video_$userId"

    fun avatarFor(userId: String): UserAvatar? = _avatars.value[userId]

    /** Nap avatar da luu (goi khi mo trang / doi user). */
    fun load(userId: String) {
        val path = prefs.getString(keyPath(userId), null)
        val file = path?.let(::File)?.takeIf { it.exists() }
        if (path != null && file == null) {
            // File mat (user xoa data thu cong): don key rac.
            prefs.edit().remove(keyPath(userId)).remove(keyVideo(userId)).apply()
        }
        _avatars.value = if (file != null) {
            _avatars.value + (userId to UserAvatar(file, prefs.getBoolean(keyVideo(userId), false)))
        } else {
            _avatars.value - userId
        }
    }

    /** Do dai video (ms); -1 neu khong doc duoc. */
    fun videoDurationMs(uri: Uri): Long {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(context, uri)
            r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: -1
        } catch (e: Exception) {
            Log.w("AvatarStore", "Cannot read video duration", e)
            -1
        } finally {
            r.release()
        }
    }

    /**
     * Luu avatar moi: copy vao bo nho app, xoa ban cu.
     * Video phai ngan hon [maxDurationMs] (mac dinh 60_000ms = 1 phut).
     */
    suspend fun saveAvatar(
        userId: String,
        source: Uri,
        isVideo: Boolean,
        maxDurationMs: Long = 60_000,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (isVideo) {
                val dur = videoDurationMs(source)
                if (dur < 0) return@withContext Result.failure(IllegalStateException("Không đọc được video"))
                if (dur >= maxDurationMs) {
                    return@withContext Result.failure(IllegalArgumentException("Video phải ngắn hơn 1 phút"))
                }
            }
            val ext = if (isVideo) "mp4" else "jpg"
            val safeId = userId.replace(Regex("[^A-Za-z0-9_-]"), "_").take(32).ifEmpty { "user" }
            val dest = File(dir, "avatar_${safeId}_${System.currentTimeMillis()}.$ext")
            context.contentResolver.openInputStream(source)?.use { input ->
                dest.outputStream().use { input.copyTo(it) }
            } ?: return@withContext Result.failure(IllegalStateException("Không mở được file đã chọn"))
            _avatars.value[userId]?.file?.takeIf { it.exists() && it != dest }?.delete()
            prefs.edit()
                .putString(keyPath(userId), dest.absolutePath)
                .putBoolean(keyVideo(userId), isVideo)
                .apply()
            _avatars.value = _avatars.value + (userId to UserAvatar(dest, isVideo))
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w("AvatarStore", "saveAvatar failed", e)
            Result.failure(e)
        }
    }

    /** Xoa avatar -> ve avatar mac dinh cua user. */
    suspend fun clearAvatar(userId: String): Unit = withContext(Dispatchers.IO) {
        _avatars.value[userId]?.file?.takeIf { it.exists() }?.delete()
        prefs.edit().remove(keyPath(userId)).remove(keyVideo(userId)).apply()
        _avatars.value = _avatars.value - userId
    }

    /**
     * Luu video co san trong app (R.raw) lam avatar: khong can chon file,
     * khong kiem tra do dai (asset cua app da duoc chon loc).
     */
    suspend fun saveRawVideo(
        userId: String,
        @RawRes resId: Int,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val safeId = userId.replace(Regex("[^A-Za-z0-9_-]"), "_").take(32).ifEmpty { "user" }
            val dest = File(dir, "avatar_${safeId}_${System.currentTimeMillis()}.mp4")
            context.resources.openRawResource(resId).use { input ->
                dest.outputStream().use { input.copyTo(it) }
            }
            _avatars.value[userId]?.file?.takeIf { it.exists() && it != dest }?.delete()
            prefs.edit()
                .putString(keyPath(userId), dest.absolutePath)
                .putBoolean(keyVideo(userId), true)
                .apply()
            _avatars.value = _avatars.value + (userId to UserAvatar(dest, true))
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w("AvatarStore", "saveRawVideo failed", e)
            Result.failure(e)
        }
    }
}
