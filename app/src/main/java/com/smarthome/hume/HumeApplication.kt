package com.smarthome.hume

import android.app.Application
import android.net.Uri
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.smarthome.hume.core.data.HumeGraph
import com.smarthome.hume.core.frigate.FrigateStore
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.FrigateRemoteConfig
import com.smarthome.hume.core.storage.SensorDatabase
import com.smarthome.hume.core.storage.SettingsStore
import com.smarthome.hume.data.AppEnergyRepository
import com.smarthome.hume.data.AppHomeRepository
import com.smarthome.hume.data.AppSecurityRepository
import com.smarthome.hume.data.AppSyncRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

class HumeApplication : Application(), ImageLoaderFactory {
    lateinit var settingsStore: SettingsStore
        private set
    lateinit var sensorDatabase: SensorDatabase
        private set
    val haRepository = HomeAssistantRepository()
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * Cau hinh Frigate remote hien tai (de Coil interceptor gan CF-Access headers).
     * Cap nhat tu SessionStore.frigateRemote trong onCreate().
     */
    @Volatile
    var frigateCf: FrigateRemoteConfig = FrigateRemoteConfig()
        private set

    /**
     * Coil global: tu dong gan Cloudflare Access Service Token khi tai anh
     * tu hostname Frigate tren tunnel. Nho vay CameraFeedCard khong can
     * truyen headers thu cong.
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient {
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        val req = chain.request()
                        val cfg = frigateCf
                        val remoteHost = runCatching {
                            Uri.parse(cfg.remoteUrl).host
                        }.getOrNull()
                        val builder = req.newBuilder()
                        if (cfg.hasAccess && remoteHost != null && req.url.host == remoteHost) {
                            builder.header("CF-Access-Client-Id", cfg.cfClientId)
                            builder.header("CF-Access-Client-Secret", cfg.cfClientSecret)
                        }
                        chain.proceed(builder.build())
                    }
                    .build()
            }
            .build()

    override fun onCreate() {
        super.onCreate()
        // Graph moi (multi-module M3E): khoi tao 1 lan, cac feature lay qua HumeGraph.get().
        HumeGraph.init(this)
        // Nap cau hinh Frigate remote: FrigateStore + Coil interceptor dung chung.
        appScope.launch {
            HumeGraph.get().sessionStore.frigateRemote.collect { cfg ->
                frigateCf = cfg
                FrigateStore.get(this@HumeApplication).configureRemote(cfg)
            }
        }
        // Dang ky HomeRepository that (adapter tren HomeAssistantRepository cu).
        HumeGraph.get().registerHomeRepository(AppHomeRepository(haRepository, appScope, this))
        // Dang ky EnergyRepository that (adapter tren HomeAssistantRepository cu).
        HumeGraph.get().registerEnergyRepository(AppEnergyRepository(haRepository, appScope))
        settingsStore = SettingsStore(this)
        // Dang ky SecurityRepository that (camera Frigate + sensor HA that).
        HumeGraph.get().registerSecurityRepository(
            AppSecurityRepository(this, haRepository, settingsStore, appScope),
        )
        // Dang ky SyncRepository (trang thai ket noi cho tab Toi).
        HumeGraph.get().registerSyncRepository(AppSyncRepository(haRepository))
        sensorDatabase = SensorDatabase(this)
        // Watched numeric sensors are cached locally so charts still work offline.
        haRepository.sensorSink = { entityId, value, timeMs ->
            sensorDatabase.record(entityId, value, timeMs)
        }
        // Kich ban da bi go khoi giao dien: khong con re-arm alarm luc cold start,
        // nen onCreate() khong con cham vao DataStore tren main thread.
    }
}
