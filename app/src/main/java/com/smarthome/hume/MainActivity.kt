package com.smarthome.hume

import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.smarthome.hume.core.data.HumeGraph
import com.smarthome.hume.core.datastore.ThemeSettings
import com.smarthome.hume.core.ha.HistoryFetcher
import com.smarthome.hume.core.model.AuthSession
import com.smarthome.hume.core.storage.HumeSettings
import com.smarthome.hume.core.ui.theme.HumeM3ETheme
import com.smarthome.hume.core.ui.theme.M3ESeed
import com.smarthome.hume.feature.auth.LoginScreen
import com.smarthome.hume.ui.root.M3ERootScreen

class MainActivity : ComponentActivity() {
    override fun onResume() {
        super.onResume()
        // Port iOS 5ce8e65: xoa notification moi lan app len foreground.
        NotificationManagerCompat.from(this).cancelAll()
    }

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
        // Port iOS 5ce8e65: xoa badge/notification khi mo app.
        // Android khong co badge he thong (tuy launcher) — xoa notification la tuong duong.
        NotificationManagerCompat.from(this).cancelAll()
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

        // Doi mang (bat/tat WireGuard, WiFi <-> 4G) -> tu chuyen local <-> remote.
        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        connectivityManager?.registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    app.haRepository.noteNetworkChanged()
                }

                override fun onLost(network: Network) {
                    app.haRepository.noteNetworkChanged()
                }
            },
            Handler(Looper.getMainLooper()),
        )

        setContent {
            // Nguon su that cho gate dang nhap: AuthRepository cua kien truc moi.
            // (Doc chung file "hume_settings"/"hume_secrets" voi SettingsStore cu.)
            val session by graph.authRepository.session.collectAsState(initial = AuthSession())
            val settings by app.settingsStore.settings.collectAsState(initial = HumeSettings())
            LaunchedEffect(session.isLoggedIn) {
                if (session.isLoggedIn) {
                    // Dong bo lai SettingsStore cu (tokenFlow cua no khong tu refresh).
                    app.settingsStore.refresh()
                    app.haRepository.configure(session.localUrl, session.remoteUrl, session.token)
                    // Duong lay lich su rieng (timeout dai) cho bieu do 7 ngay.
                    HistoryFetcher.configure(app.haRepository.endpoint, session.token)
                    app.haRepository.connect()
                } else {
                    // Vua dang xuat (hoac session het han): ngat socket cu ngay.
                    app.haRepository.disconnect()
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
                // Theme mot lan o root (thay cho wrapper long trong LoginScreen).
                val themeSettings by graph.themeStore.settings.collectAsState(
                    initial = ThemeSettings(),
                )
                val seed = runCatching { M3ESeed.valueOf(themeSettings.seedName) }
                    .getOrDefault(M3ESeed.Cam)
                // (2026-10-01, fix crash khi ap dung mau tuy chinh: dung cach tach ARGB an toan
                //  thay vi Color(Long.toULong()) de tranh van de bit pattern)
                val customColor = themeSettings.customColor?.let { argb ->
                    runCatching {
                        androidx.compose.ui.graphics.Color(
                            red = ((argb shr 16) and 0xFF) / 255f,
                            green = ((argb shr 8) and 0xFF) / 255f,
                            blue = (argb and 0xFF) / 255f,
                            alpha = ((argb shr 24) and 0xFF) / 255f,
                        )
                    }.getOrNull()
                }
                HumeM3ETheme(
                    seed = seed,
                    darkTheme = themeSettings.darkMode ?: isSystemInDarkTheme(),
                    customSeedColor = customColor,
                    fontFamily = themeSettings.fontFamily,
                ) {
                    LoginScreen()
                }
            }
        }
    }
}
