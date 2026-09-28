package com.smarthome.hume.data

import com.smarthome.hume.core.data.SyncRepository
import com.smarthome.hume.core.ha.HomeAssistantRepository
import kotlinx.coroutines.flow.StateFlow

/** Adapter: trang thai ket noi HA cho tab Toi. */
class AppSyncRepository(ha: HomeAssistantRepository) : SyncRepository {
    override val isConnected: StateFlow<Boolean> = ha.connected
}
