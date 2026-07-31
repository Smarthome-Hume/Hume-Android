package com.smarthome.hume

import android.app.Application
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.storage.SensorDatabase
import com.smarthome.hume.core.storage.SettingsStore

class HumeApplication : Application() {
    lateinit var settingsStore: SettingsStore
        private set
    lateinit var sensorDatabase: SensorDatabase
        private set
    val haRepository = HomeAssistantRepository()

    override fun onCreate() {
        super.onCreate()
        settingsStore = SettingsStore(this)
        sensorDatabase = SensorDatabase(this)
        // Watched numeric sensors are cached locally so charts still work offline.
        haRepository.sensorSink = { entityId, value, timeMs ->
            sensorDatabase.record(entityId, value, timeMs)
        }
    }
}
