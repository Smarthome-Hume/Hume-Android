package com.smarthome.hume.core.data

import com.smarthome.hume.core.model.SecurityUiState
import kotlinx.coroutines.flow.StateFlow

/** Nguon du lieu cho tab An ninh (demo v4) — UI chi doc state nay. */
interface SecurityRepository {
    val securityState: StateFlow<SecurityUiState>

    /** Tai lai danh sach clip Frigate cho camera. */
    fun refreshRecordings(cameraKey: String)

    /** URL snapshot Frigate cho camera (de UI tu load anh). */
    fun snapshotUrl(cameraKey: String): String
}
