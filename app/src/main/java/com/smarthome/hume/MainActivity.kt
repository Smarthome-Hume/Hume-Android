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
import com.smarthome.hume.core.data.HumeGraph
import com.smarthome.hume.core.ha.HistoryFetcher
import com.smarthome.hume.core.model.AuthSession
import com.smarthome.hume.core.storage.HumeSettings
import com.smarthome.hume.feature.auth.LoginScreen
import com.smarthome.hume.ui.root.M3ERootScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 15 always draws edge to edge. SystemBarStyle.auto flips the
        // bar icons with the system appearance, which is what the SwiftUI app
        // gets for free from UIKit; the padding for those bars is applied in
        // HumeRootScreen.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val app = application as HumeApplication
        val graph = HumeGraph.get()

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
            // Nguon su that cho gate dang nhap: AuthRepository cua kien truc moi.
            // (Doc chung file "hume_settings"/"hume_secrets" voi SettingsStore cu.)
            val session by graph.authRepository.session.collectAsState(initial = AuthSession())
            val settings by app.settingsStore.settings.collectAsState(initial = HumeSettings())
            LaunchedEffect(session.isLoggedIn) {
                if (session.isLoggedIn) {
                    // Dong bo lai SettingsStore cu (tokenFlow cua no khong tu refresh).
                    app.settingsStore.refresh()
                    app.haRepository.configure(session.serverUrl, session.token)
                    // Duong lay lich su rieng (timeout dai) cho bieu do 7 ngay.
                    HistoryFetcher.configure(session.serverUrl, session.token)
                    app.haRepository.connect()
                }
            }
            // Chua dang nhap -> man hinh login M3E moi (feature/auth).
            if (session.isLoggedIn) {
                M3ERootScreen(
                    settingsStore = app.settingsStore,
                    ha = app.haRepository,
                    settings = settings,
                )
            } else {
                LoginScreen()
            }
        }
    }
}
