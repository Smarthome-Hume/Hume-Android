package com.smarthome.hume.core.ui.wallpaper

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Hinh nen app — port iOS WallpaperManager/MeWallpaperCard.
 * User chon anh tu thu vien -> copy vao bo nho app -> hien lam nen ca app
 * (ca sang/toi). Nut "Go" xoa ve mac dinh.
 */
class WallpaperStore(private val context: Context) {

    private val prefs = context.getSharedPreferences("hume_wallpaper", Context.MODE_PRIVATE)
    private val dir = File(context.filesDir, "wallpaper").also { it.mkdirs() }

    private val _wallpaper = MutableStateFlow<File?>(null)
    val wallpaper: StateFlow<File?> = _wallpaper.asStateFlow()

    companion object {
        private const val KEY_PATH = "wallpaper_path"
    }

    /** Nap hinh nen da luu (goi khi mo app). */
    fun load() {
        val path = prefs.getString(KEY_PATH, null)
        val file = path?.let(::File)?.takeIf { it.exists() }
        if (path != null && file == null) {
            prefs.edit().remove(KEY_PATH).apply()
        }
        _wallpaper.value = file
    }

    /** Luu anh tu Uri (copy vao bo nho app de song khi Uri goc mat). */
    suspend fun setWallpaper(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val dest = File(dir, "wallpaper.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: throw IllegalStateException("Không đọc được ảnh")
            prefs.edit().putString(KEY_PATH, dest.absolutePath).apply()
            _wallpaper.value = dest
        }
    }

    /** Xoa hinh nen ve mac dinh. */
    fun reset() {
        _wallpaper.value?.delete()
        prefs.edit().remove(KEY_PATH).apply()
        _wallpaper.value = null
    }

    fun hasWallpaper(): Boolean = _wallpaper.value != null
}
