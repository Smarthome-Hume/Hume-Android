package com.smarthome.hume.core.data

import kotlinx.coroutines.flow.StateFlow

/** Trang thai ket noi HA cho the Dong bo (tab Toi). */
interface SyncRepository {
    val isConnected: StateFlow<Boolean>
}
