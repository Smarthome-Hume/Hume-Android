package com.smarthome.hume.core.data

import com.smarthome.hume.core.model.SecurityUiState
import kotlinx.coroutines.flow.StateFlow

/** Nguon du lieu cho tab An ninh (demo v4) — UI chi doc state nay. */
interface SecurityRepository {
    val securityState: StateFlow<SecurityUiState>

    /** Entity ID cac sensor an ninh (cua/chuyen dong/khoi/nuoc) de dang ky realtime. */
    val sensorEntityIds: Set<String>

    /** Tai lai danh sach clip Frigate cho camera. */
    fun refreshRecordings(cameraKey: String)

    /** URL snapshot Frigate cho camera (de UI tu load anh). */
    fun snapshotUrl(cameraKey: String): String

    /**
     * URL RTSP truc tiep tu Frigate/go2rtc cho camera, de stream lien tuc
     * bang ExoPlayer. Tra ve null khi dang di duong remote (Cloudflare
     * Tunnel khong cho RTSP) — UI tu fallback ve snapshot polling.
     */
    fun rtspUrl(cameraKey: String): String?

    /**
     * URL MJPEG live stream cho camera (multipart/x-mixed-replace) —
     * port tu iOS FrigateStore.liveStreamURL. Live that, nhe hon RTSP,
     * chay duoc ca local lan remote (HTTP qua Cloudflare Tunnel).
     */
    fun mjpegUrl(cameraKey: String): String
}
