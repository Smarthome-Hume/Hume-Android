package com.smarthome.hume

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.smarthome.hume.core.storage.HumeSettings
import com.smarthome.hume.ui.login.LoginScreen
import com.smarthome.hume.ui.root.HumeRootScreen
import com.smarthome.hume.ui.theme.HumeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 15 always draws edge to edge. Declaring both bars as "light"
        // with a transparent scrim keeps the clock and gesture bar icons dark on
        // the app's cream background, even when the phone is in dark mode; the
        // padding for those bars is applied in HumeRootScreen.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
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
                // Same gate as HumeApp.swift: no token means the login screen.
                if (settings.hasToken) {
                    HumeRootScreen(
                        settingsStore = app.settingsStore,
                        ha = app.haRepository,
                        settings = settings,
                    )
                } else {
                    LoginScreen(settingsStore = app.settingsStore, settings = settings)
                }
            }
        }
    }
}
