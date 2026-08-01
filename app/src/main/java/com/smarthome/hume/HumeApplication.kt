package com.smarthome.hume

import android.app.Application
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.schedule.SceneScheduleStore
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
        // Android drops pending alarms when the app is killed or updated, so the
        // scene schedules are re-armed on every cold start (same net effect as the
        // iOS side re-registering its notifications).
        runCatching { SceneScheduleStore.get(this).rescheduleAll() }
    }
}
