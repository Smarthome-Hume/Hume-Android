package com.smarthome.hume.core.ha

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.smarthome.hume.core.storage.DailySnapshotStore
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

private const val TAG_ZONE = "ZoneArrival"

/**
 * Port 1:1 iOS ZoneArrivalManager.swift — tu chay kich ban theo VI TRI
 * va THOI GIAN:
 * - Vao khu vuc nha  -> "Về nhà"      (SceneRole.arrive)
 * - Roi khu vuc nha  -> "Ra khỏi nhà" (SceneRole.leave)
 * - Buoi sang (gio)   -> "Chào buổi sáng" (SceneRole.morning)
 * - Buoi toi (gio)    -> "Chúc ngủ ngon"  (SceneRole.night)
 *
 * Anh xa iOS -> Android:
 * - CLCircularRegion (radius 180m, id "hume_home_zone", notifyOnEntry/
 *   notifyOnExit) -> GeofencingClient + Geofence cung id/ban kinh
 *   (play-services-location 21.3.0 da co san, khong them dependency).
 * - Timer 60s tren RunLoop (onAppActive) -> coroutine ticker 60s,
 *   start o MainActivity.onStart, cancel o onStop.
 * - UserDefaults keys giu nguyen ten ("arrival_auto_enabled",
 *   "scene_morning_*", "scene_night_*", "scene_fired_*",
 *   "home_lat"/"home_lon") trong file prefs "hume_automation".
 *
 * NHUNG GI KHONG PORT DUOC 1:1 (ghi ro, khong gia vo co):
 * - iOS fire() kich hoat scene qua LocalSceneStore (tim scene theo ten
 *   roi toggle activate) + AppleHomeManager neu homekit_sync bat.
 *   Android KHONG CO LocalSceneStore (grep 0 hit) va khong co HomeKit.
 *   Nen fire()/stopActive() chay qua [SceneRunner] — mot plug (null
 *   mac dinh: chi log). Khi nao Android co store scene thi gan runner
 *   vao, khong sua manager nay.
 * - iOS xin "always" bang requestAlwaysAuthorization(). Android khong
 *   co call tuong duong 1 dong: can ACCESS_FINE_LOCATION (geofence bat
 *   buoc — thieu la SecurityException) + ACCESS_BACKGROUND_LOCATION
 *   (Android 10+, de geofence kich hoat khi app background). Ca 2
 *   CHUA co trong AndroidManifest.xml — can user duyet truoc khi them
 *   (xem ghi chu o [startHomeMonitoring]). Code nay chi kiem tra quyen
 *   hien co, khong tu xin, khong tu them permission.
 * - iOS LocationManager con reverse-geocode ten dia diem (locationName).
 *   Phan do Android da co san o BriefRepository (thoi tiet) nen khong
 *   port lai o day.
 */
enum class SceneRole {
    ARRIVE, LEAVE, MORNING, NIGHT;

    /** Ten scene trong LocalSceneStore iOS — giu nguyen tieng Viet. */
    val sceneName: String
        get() = when (this) {
            ARRIVE -> "Về nhà"
            LEAVE -> "Ra khỏi nhà"
            MORNING -> "Chào buổi sáng"
            NIGHT -> "Chúc ngủ ngon"
        }

    val headline: String
        get() = when (this) {
            ARRIVE -> "Sắp về nhà"
            LEAVE -> "Rời khỏi nhà"
            MORNING -> "Chào buổi sáng"
            NIGHT -> "Chuẩn bị đi ngủ"
        }

    /** Ten SF Symbol goc iOS — giu de tham chieu, Android khong dung SF Symbol. */
    val iconSfName: String
        get() = when (this) {
            ARRIVE -> "house.fill"
            LEAVE -> "figure.walk.departure"
            MORNING -> "sun.horizon.fill"
            NIGHT -> "moon.stars.fill"
        }

    val tintHex: String
        get() = when (this) {
            ARRIVE -> "#66d19e"
            LEAVE -> "#73b9f2"
            MORNING -> "#f2d26f"
            NIGHT -> "#ad99e6"
        }
}

/**
 * Diem chay scene that (LocalSceneStore tuong lai). Ngu nghia nhu iOS
 * fire(): runScene = kich hoat scene neu chua active (activate() cua
 * iOS la toggle — chi goi khi chua active); stopScene = tat scene dang
 * chay (iOS goi activate() lan nua de toggle tat).
 */
interface SceneRunner {
    fun runScene(named: String)
    fun stopScene(named: String)
}

object ZoneArrivalManager {

    /** Id vung nha — giong iOS LocationManager.homeRegionId. */
    const val HOME_REGION_ID = "hume_home_zone"

    /** Ban kinh geofence nha — giong iOS CLCircularRegion radius 180m. */
    const val HOME_RADIUS_METERS = 180f

    /** Runner that — null = chua co store scene tren Android, chi log. */
    var sceneRunner: SceneRunner? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null
    private var pendingRole: SceneRole? = null
    private var activeRole: SceneRole? = null

    private fun prefs(context: Context) =
        context.getSharedPreferences(TempAlertConfig.PREFS_NAME, Context.MODE_PRIVATE)

    private fun boolPref(context: Context, key: String, default: Boolean): Boolean =
        prefs(context).getBoolean(key, default)

    private fun intPref(context: Context, key: String, default: Int): Int =
        prefs(context).getInt(key, default)

    /** Port ZoneArrivalManager.enabled — mac dinh TAT (iOS: false). */
    fun isArrivalAutoEnabled(context: Context): Boolean =
        boolPref(context, "arrival_auto_enabled", false)

    private fun isMorningOn(context: Context): Boolean =
        boolPref(context, "scene_morning_enabled", false)

    private fun isNightOn(context: Context): Boolean =
        boolPref(context, "scene_night_enabled", false)

    /** iOS homekit_sync_enabled — doc de du cau hinh, Android khong co HomeKit nen khong chay. */
    @Suppress("unused")
    fun isHomekitSync(context: Context): Boolean =
        boolPref(context, "homekit_sync_enabled", false)

    private fun morningHour(context: Context): Int = intPref(context, "scene_morning_hour", 7)
    private fun morningMinute(context: Context): Int = intPref(context, "scene_morning_minute", 0)
    private fun nightHour(context: Context): Int = intPref(context, "scene_night_hour", 22)
    private fun nightMinute(context: Context): Int = intPref(context, "scene_night_minute", 30)

    /** Port LocationManager.hasHome: da luu vi tri nha hay chua. */
    fun hasHome(context: Context): Boolean =
        prefs(context).contains("home_lat") && prefs(context).contains("home_lon")

    /** Vi tri nha da luu, null neu chua co. */
    fun homeLatLon(context: Context): Pair<Double, Double>? {
        val p = prefs(context)
        if (!p.contains("home_lat") || !p.contains("home_lon")) return null
        val lat = Double.fromBits(p.getLong("home_lat", 0L))
        val lon = Double.fromBits(p.getLong("home_lon", 0L))
        return lat to lon
    }

    // MARK: - Vong doi (goi tu MainActivity khi app active — port HumeApp.onAppActive)

    /**
     * Port ZoneArrivalManager.onAppActive(): ticker 60s danh gia trigger
     * theo gio + danh gia ngay. Goi tu MainActivity.onStart (cung observer
     * voi haRepository.onAppForeground). iOS khong huy Timer khi xuong
     * background (he thong tu suspend app); Android huy o [onAppBackground]
     * de khong dot pin — lech co chu y, hanh vi ngoai giong nhau.
     */
    fun onAppActive(context: Context) {
        val appContext = context.applicationContext
        tickJob?.cancel()
        evaluateTimeTriggers(appContext)
        tickJob = scope.launch {
            while (isActive) {
                delay(60_000)
                evaluateTimeTriggers(appContext)
            }
        }
    }

    fun onAppBackground() {
        tickJob?.cancel()
        tickJob = null
    }

    // MARK: - Trigger theo VI TRI (goi tu ZoneArrivalReceiver / LocationManager)

    /** Port ZoneArrivalManager.handleArrival() — gate theo arrival_auto_enabled. */
    fun handleArrival(context: Context) {
        if (!isArrivalAutoEnabled(context)) return
        trigger(SceneRole.ARRIVE)
    }

    /** Port ZoneArrivalManager.handleDeparture(). */
    fun handleDeparture(context: Context) {
        if (!isArrivalAutoEnabled(context)) return
        trigger(SceneRole.LEAVE)
    }

    // MARK: - Trigger theo GIO

    /** Port ZoneArrivalManager.evaluateTimeTriggers(): chi kich 1 lan/ngay/role. */
    fun evaluateTimeTriggers(context: Context) {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val dayKey = DailySnapshotStore.dayString(System.currentTimeMillis())
        if (isMorningOn(context) && hour == morningHour(context) &&
            minute == morningMinute(context) && lastFired(context, SceneRole.MORNING) != dayKey
        ) {
            setFired(context, SceneRole.MORNING, dayKey)
            trigger(SceneRole.MORNING)
        }
        if (isNightOn(context) && hour == nightHour(context) &&
            minute == nightMinute(context) && lastFired(context, SceneRole.NIGHT) != dayKey
        ) {
            setFired(context, SceneRole.NIGHT, dayKey)
            trigger(SceneRole.NIGHT)
        }
    }

    private fun lastFired(context: Context, role: SceneRole): String? =
        prefs(context).getString("scene_fired_${role.name.lowercase()}", null)

    private fun setFired(context: Context, role: SceneRole, value: String) {
        prefs(context).edit().putString("scene_fired_${role.name.lowercase()}", value).apply()
    }

    // MARK: - Dem nguoc -> chay (iOS: Live Activity da go — chay thang)

    /** Port ZoneArrivalManager.trigger(): chi 1 role cho chay tai 1 thoi diem. */
    fun trigger(role: SceneRole) {
        if (pendingRole != null) return
        pendingRole = role
        fire()
    }

    fun runNow() {
        fire()
    }

    fun cancel() {
        pendingRole = null
    }

    private fun fire() {
        val role = pendingRole ?: return
        pendingRole = null
        val runner = sceneRunner
        if (runner == null) {
            // Android chua co LocalSceneStore: ghi log thay vi gia vo chay scene.
            Log.i(TAG_ZONE, "Scene '${role.sceneName}' triggered but no SceneRunner (no LocalSceneStore on Android yet)")
        } else {
            runner.runScene(role.sceneName)
        }
        activeRole = role
    }

    /** Port ZoneArrivalManager.stopActive(): tat scene dang chay (toggle). */
    fun stopActive() {
        val role = activeRole ?: return
        sceneRunner?.stopScene(role.sceneName)
            ?: Log.i(TAG_ZONE, "stopActive '${role.sceneName}' ignored: no SceneRunner")
        activeRole = null
    }

    /**
     * Port ZoneArrivalManager.showExternalScene(named:): scene duoc bat TU
     * NGOAI (HA dashboard / bang Alarmo) — chi ghi nhan role dang chay,
     * vi scene da duoc kich hoat san.
     */
    fun showExternalScene(named: String) {
        activeRole = SceneRole.values().firstOrNull { it.sceneName == named }
    }

    // MARK: - Geofence khu vuc nha (port LocationManager.startHomeMonitoring)

    /**
     * Port LocationManager.setHomeHere(): luu vi tri hien tai lam nha roi
     * bat monitoring. Dung getCurrentLocation HIGH_ACCURACY 1 lan (mau
     * BriefRepository.currentLatLon, nhung HIGH thay vi BALANCED vi
     * geofence 180m can fix chinh xac hon thoi tiet cap thanh pho).
     * [onDone] tra false khi chua cap quyen vi tri hoac khong lay duoc fix.
     */
    fun setHomeHere(context: Context, onDone: (Boolean) -> Unit) {
        val appContext = context.applicationContext
        if (!hasAnyLocationPermission(appContext)) {
            Log.w(TAG_ZONE, "setHomeHere: no location permission")
            onDone(false)
            return
        }
        scope.launch {
            val loc = currentLatLon(appContext)
            if (loc == null) {
                onDone(false)
                return@launch
            }
            prefs(appContext).edit()
                .putLong("home_lat", loc.first.toBits())
                .putLong("home_lon", loc.second.toBits())
                .apply()
            Log.i(TAG_ZONE, "Home set at ${loc.first}, ${loc.second}")
            startHomeMonitoring(appContext)
            onDone(true)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun currentLatLon(context: Context): Pair<Double, Double>? =
        withTimeoutOrNull(8_000) {
            suspendCancellableCoroutine { cont ->
                val fused = LocationServices.getFusedLocationProviderClient(context)
                val cts = CancellationTokenSource()
                cont.invokeOnCancellation { cts.cancel() }
                try {
                    fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                        .addOnSuccessListener { l ->
                            if (!cont.isCompleted) cont.resume(l?.let { it.latitude to it.longitude })
                        }
                        .addOnFailureListener { if (!cont.isCompleted) cont.resume(null) }
                        .addOnCanceledListener { if (!cont.isCompleted) cont.resume(null) }
                } catch (t: Throwable) {
                    if (!cont.isCompleted) cont.resume(null)
                }
            }
        }

    private fun hasAnyLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun hasFineLocation(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun hasBackgroundLocation(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Port LocationManager.startHomeMonitoring(): dang ky geofence tron
     * 180m quanh nha, bao ca vao (ENTER) va ra (EXIT). Giong iOS: dung
     * lai monitoring cu cung id truoc khi dang ky moi (GeofencingClient
     * tu ghi de khi trung requestId, nhung van remove truoc cho chac).
     *
     * QUYEN CAN (CHUA THEM VAO MANIFEST — can user duyet):
     * - ACCESS_FINE_LOCATION: BAT BUOC, GeofencingClient nem
     *   SecurityException neu thieu. Ham nay return im neu chua cap
     *   (log warning), khong tu xin.
     * - ACCESS_BACKGROUND_LOCATION (Android 10+): de transition kich
     *   hoat khi app background; thieu thi geofence chi chac chan chay
     *   khi app foreground — van dang ky nhung log canh bao.
     * Manifest hien chi co ACCESS_COARSE_LOCATION (thoi tiet Brief) —
     * COARSE khong du cho geofence 180m.
     */
    @SuppressLint("MissingPermission")
    fun startHomeMonitoring(context: Context) {
        val appContext = context.applicationContext
        val home = homeLatLon(appContext) ?: run {
            Log.w(TAG_ZONE, "startHomeMonitoring: no home saved (setHomeHere first)")
            return
        }
        if (!hasFineLocation(appContext)) {
            Log.w(TAG_ZONE, "startHomeMonitoring: ACCESS_FINE_LOCATION not granted — geofence not registered (needs manifest + runtime grant)")
            return
        }
        if (!hasBackgroundLocation(appContext)) {
            Log.w(TAG_ZONE, "startHomeMonitoring: ACCESS_BACKGROUND_LOCATION missing — transitions may not fire in background")
        }
        val geofence = Geofence.Builder()
            .setRequestId(HOME_REGION_ID)
            .setCircularRegion(home.first, home.second, HOME_RADIUS_METERS)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()
        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()
        val client: GeofencingClient = LocationServices.getGeofencingClient(appContext)
        client.removeGeofences(geofencePendingIntent(appContext))
            .addOnCompleteListener {
                client.addGeofences(request, geofencePendingIntent(appContext))
                    .addOnSuccessListener {
                        Log.i(TAG_ZONE, "Home geofence registered (${home.first}, ${home.second}, r=180m)")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG_ZONE, "addGeofences failed: ${e.message}", e)
                    }
            }
    }

    /** Port LocationManager.stopHomeMonitoring(). */
    fun stopHomeMonitoring(context: Context) {
        LocationServices.getGeofencingClient(context.applicationContext)
            .removeGeofences(geofencePendingIntent(context.applicationContext))
            .addOnSuccessListener { Log.i(TAG_ZONE, "Home geofence removed") }
    }

    private fun geofencePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ZoneArrivalReceiver::class.java)
        // FLAG_MUTABLE chi co tu API 31 (minSdk 26): guard de lint khong
        // bao RequiresApi; tren API cu bit nay bi framework bo qua.
        val mutabilityFlag =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                0
            }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or mutabilityFlag,
        )
    }
}

/**
 * BroadcastReceiver nhan geofence transition — tuong duong iOS
 * LocationManagerDelegate.didEnterRegion/didExitRegion. Dang ky trong
 * AndroidManifest.xml (exported=false). Chi xu ly dung id
 * "hume_home_zone" (giong guard region.identifier cua iOS).
 */
class ZoneArrivalReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.from(intent) ?: return
        if (event.hasError()) {
            Log.e(TAG_ZONE, "GeofencingEvent error ${event.errorCode}")
            return
        }
        val entered = event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER
        val exited = event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT
        val isHome = event.triggeringGeofences?.any { it.requestId == ZoneArrivalManager.HOME_REGION_ID } == true
        if (!isHome || (!entered && !exited)) return
        if (entered) {
            Log.i(TAG_ZONE, "Entered home zone")
            ZoneArrivalManager.handleArrival(context.applicationContext)
        } else {
            Log.i(TAG_ZONE, "Exited home zone")
            ZoneArrivalManager.handleDeparture(context.applicationContext)
        }
    }
}
