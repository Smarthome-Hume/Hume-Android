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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.smarthome.hume.core.data.HumeGraph
import com.smarthome.hume.core.ha.TempAlertWatcher
import com.smarthome.hume.core.ha.ZoneArrivalManager
import com.smarthome.hume.core.datastore.ThemeSettings
import com.smarthome.hume.core.ha.HistoryFetcher
import com.smarthome.hume.core.model.AuthSession
import com.smarthome.hume.core.storage.HumeSettings
import com.smarthome.hume.core.ui.theme.HumeM3ETheme
import com.smarthome.hume.core.ui.theme.M3ESeed
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
                // (GAP-42) Port iOS HumeApp.onAppActive: ZoneArrival time
                // triggers (60s ticker) + bat geofence nha khi
                // arrival_auto_enabled; TempAlert watch sensor nhiet do.
                // Geofence chi dang ky duoc khi da cap FINE location
                // (xem ZoneArrivalManager.startHomeMonitoring).
                ZoneArrivalManager.onAppActive(applicationContext)
                if (ZoneArrivalManager.isArrivalAutoEnabled(applicationContext)) {
                    ZoneArrivalManager.startHomeMonitoring(applicationContext)
                }
                TempAlertWatcher.start(applicationContext, app.haRepository)
            }

            override fun onStop(owner: LifecycleOwner) {
                app.haRepository.onAppBackground()
                // (GAP-42) Dung ticker gio ZoneArrival + watch nhiet do khi
                // xuong background (socket cung ngat o tren).
                ZoneArrivalManager.onAppBackground()
                TempAlertWatcher.stop()
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
            // (fontScale 1f, user 21:06) Ghim fontScale = 1f toan app: chu sp
            // khong scale theo font he thong, khop iOS dung pt cung -> cac the
            // co ngan ve dung kich thuoc thiet ke. Neu khong on thi revert
            // commit nay (ve ban cu). Chi doi fontScale, giu nguyen density
            // (dp -> px khong doi) nen chi anh huong chu sp, khong anh huong
            // layout dp hay dp -> px (vd swipe threshold).
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = density.density, fontScale = 1f),
            ) {
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
                // Mau tuy chinh: tach ARGB qua ham chung (audit C-02).
                val customColor = themeSettings.customColor?.let(::colorFromArgb)
                HumeM3ETheme(
                    seed = seed,
                    darkTheme = themeSettings.darkMode ?: isSystemInDarkTheme(),
                    customSeedColor = customColor,
                    fontFamily = themeSettings.fontFamily,
                ) {
                    LoginScreen()
                }
            }
            } // CompositionLocalProvider(LocalDensity fontScale=1f)
        }
    }
}
