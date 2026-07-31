package com.smarthome.hume

import android.app.Application
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.storage.SettingsStore

class HumeApplication : Application() {
    lateinit var settingsStore: SettingsStore
        private set
    val haRepository = HomeAssistantRepository()

    override fun onCreate() {
        super.onCreate()
        settingsStore = SettingsStore(this)
    }
}
