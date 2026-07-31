package com.smarthome.hume

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.smarthome.hume.core.storage.HumeSettings
import com.smarthome.hume.ui.root.HumeRootScreen
import com.smarthome.hume.ui.theme.HumeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as HumeApplication

        // Lifecycle-aware connection: socket only while the UI is visible.
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                app.haRepository.onAppForeground()
            }

            override fun onStop(owner: LifecycleOwner) {
                app.haRepository.onAppBackground()
            }
        })

        setContent {
            HumeTheme {
                val settings by app.settingsStore.settings.collectAsState(initial = HumeSettings())
                LaunchedEffect(settings.haUrl, settings.haToken) {
                    if (settings.hasToken) {
                        app.haRepository.configure(settings.haUrl, settings.haToken)
                        app.haRepository.connect()
                    }
                }
                HumeRootScreen(settingsStore = app.settingsStore, ha = app.haRepository, settings = settings)
            }
        }
    }
}
