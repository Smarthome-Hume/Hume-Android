package com.smarthome.hume.core.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * View hien thi MJPEG live stream (multipart/x-mixed-replace) —
 * port tu iOS `MjpegStreamView` (HomeSuggest.swift).
 *
 * - Doc stream tren background thread, parse JPEG theo marker SOI (FFD8) → EOI (FFD9)
 * - Chi giu frame MOI NHAT (bo frame cu neu decode chua kip — chong khung)
 * - Decode tren single-thread executor rieng (khong block doc stream)
 * - Buffer gioi han 10MB (nhu iOS)
 * - Loi mang/HTTP → goi [onError] tren main thread (UI tu fallback ve snapshot)
 *
 * Su dung:
 * ```
 * AndroidView(
 *     factory = { ctx ->
 *         MjpegView(ctx).apply {
 *             setOnErrorListener { mjpegFailed = true }
 *             start(mjpegUrl)
 *         }
 *     },
 *     update = { it.start(mjpegUrl) },
 *     onRelease = { it.stop() },
 * )
 * ```
 */
class MjpegView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private var streamThread: Thread? = null
    private var decodeExecutor = Executors.newSingleThreadExecutor()

    /** true khi dang decode — frame moi cho doi (chi giu latest). */
    private val isDecoding = AtomicBoolean(false)

    @Volatile private var running = false
    @Volatile private var pendingData: ByteArray? = null
    @Volatile private var currentUrl: String? = null

    private var errorListener: (() -> Unit)? = null
    private var lastBitmap: Bitmap? = null

    init {
        // Nhu iOS .scaleAspectFill
        scaleType = ScaleType.CENTER_CROP
    }

    fun setOnErrorListener(listener: () -> Unit) {
        errorListener = listener
    }

    /**
     * Bat dau stream tu [url]. Neu dang chay dung url thi bo qua.
     * Goi lai khi url doi — tu dong stop stream cu.
     */
    fun start(url: String) {
        if (url == currentUrl && running) return
        stop()
        if (decodeExecutor.isShutdown) {
            decodeExecutor = Executors.newSingleThreadExecutor()
        }
        currentUrl = url
        running = true
        streamThread = Thread({ runStream(url) }, "mjpeg-stream").apply {
            isDaemon = true
            start()
        }
    }

    /** Dung stream, giai phong ket noi. An toan khi goi nhieu lan. */
    fun stop() {
        running = false
        currentUrl = null
        pendingData = null
        streamThread?.interrupt()
        streamThread = null
    }

    override fun onDetachedFromWindow() {
        stop()
        decodeExecutor.shutdownNow()
        super.onDetachedFromWindow()
    }

    private fun runStream(url: String) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                // Frigate /api/<cam> tra ve multipart/x-mixed-replace;boundary=...
                setRequestProperty("Accept", "multipart/x-mixed-replace")
                connect()
            }
            if (conn.responseCode !in 200..299) {
                postError()
                return
            }
            parseStream(conn.inputStream)
        } catch (e: IOException) {
            if (running) postError()
        } catch (e: InterruptedException) {
            // stop() da goi — thoat em
        } finally {
            try {
                conn?.disconnect()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Doc lien tuc tu stream, trich frame JPEG hoan chinh (SOI→EOI).
     * Phan chua du frame duoc giu lai cho lan doc tiep theo.
     */
    private fun parseStream(input: InputStream) {
        var buf = ByteArray(512 * 1024)
        var len = 0
        val chunk = ByteArray(16 * 1024)
        while (running) {
            val n = try {
                input.read(chunk)
            } catch (e: IOException) {
                if (running) postError()
                return
            }
            if (n <= 0) {
                // Stream dong — bao loi de UI fallback (giong iOS: task cancel → view dung)
                if (running) postError()
                return
            }
            // Mo rong buffer neu can; reset khi vuot 10MB (nhu iOS)
            if (len + n > buf.size) {
                if (len + n > MAX_BUFFER) {
                    len = 0
                    buf = ByteArray(512 * 1024)
                } else {
                    buf = buf.copyOf(len + n + 256 * 1024)
                }
            }
            System.arraycopy(chunk, 0, buf, len, n)
            len += n

            // Trich tat ca frame hoan chinh trong buffer
            var offset = 0
            while (running) {
                val soi = indexOf(buf, JPEG_SOI, offset, len)
                if (soi < 0) break
                val eoi = indexOf(buf, JPEG_EOI, soi + 2, len)
                if (eoi < 0) {
                    offset = soi // giu frame dang do
                    break
                }
                val frameEnd = eoi + 2
                pendingData = buf.copyOfRange(soi, frameEnd)
                offset = frameEnd
                renderPending()
            }
            // Don buffer: dua phan chua xu ly ve dau
            if (offset > 0) {
                System.arraycopy(buf, offset, buf, 0, len - offset)
                len -= offset
            }
        }
    }

    /** Decode frame moi nhat (neu ranh) roi ve len man hinh. */
    private fun renderPending() {
        val data = pendingData ?: return
        // Dang decode → giu latest, se decode tiep o finally
        if (!isDecoding.compareAndSet(false, true)) return
        pendingData = null
        try {
            decodeExecutor.execute {
                try {
                    val bmp = BitmapFactory.decodeByteArray(data, 0, data.size)
                    if (bmp != null && running) {
                        post {
                            lastBitmap?.recycle()
                            lastBitmap = bmp
                            setImageBitmap(bmp)
                        }
                    } else {
                        bmp?.recycle()
                    }
                } catch (_: Exception) {
                    // Frame hong — bo qua, cho frame tiep theo
                } finally {
                    isDecoding.set(false)
                    // Co frame moi trong luc decode → decode tiep
                    if (pendingData != null && running) renderPending()
                }
            }
        } catch (_: Exception) {
            // Executor da shutdown
            isDecoding.set(false)
        }
    }

    private fun postError() {
        post { errorListener?.invoke() }
    }

    /** Tim 2-byte marker trong [buf] tu [from] den [to] (exclusive). */
    private fun indexOf(buf: ByteArray, marker: ByteArray, from: Int, to: Int): Int {
        val limit = to - 1
        var i = from
        while (i < limit) {
            if (buf[i] == marker[0] && buf[i + 1] == marker[1]) return i
            i++
        }
        return -1
    }

    companion object {
        private const val MAX_BUFFER = 10 * 1024 * 1024
        private val JPEG_SOI = byteArrayOf(0xFF.toByte(), 0xD8.toByte())
        private val JPEG_EOI = byteArrayOf(0xFF.toByte(), 0xD9.toByte())
    }
}
