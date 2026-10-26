package com.smarthome.hume.feature.energy

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitor may bom may loc nuoc — port iOS PumpAlertMonitor.swift (Batch B, GAP-04).
 *
 * - Entity: binary_sensor.r_ro_m_01 (bom cao ap Rice Home R-RO-M-01, ID that
 *   tren HA .22 — khong phai dang _pump, dang do khong ton tai nen monitor
 *   cu khong bao gio thay bom chay).
 * - Bom chay lien tuc >= 7 phut -> rung 1 lan + notification uu tien cao
 *   (timeSensitive tuong duong: IMPORTANCE_HIGH + CATEGORY_ALARM + am bao
 *   dong) + banner do trong EnergyScreen (qua [alertVisible]).
 * - Chi chay foreground nhu iOS: root (M3ERootScreen) theo doi
 *   ha.entityUpdated; MainActivity.onStop goi [onAppBackground] tam dung
 *   dem 7 phut (giu lastPumpOn + co bao, lan foreground sau sync() se dem
 *   lai neu bom con chay). Khong tao WorkManager/background job moi.
 * - Khong tu xin quyen: dung quyen POST_NOTIFICATIONS da xin 1 lan o
 *   MainActivity (Batch A, GAP-16); chua cap quyen thi chi hien banner
 *   trong app, notification tra ve im lang nhu EnergyNotifier.
 */
object PumpAlertMonitor {
    /** Entity bom may loc Rice Home R-RO-M-01 (port iOS PumpAlertMonitor.pumpEntityId). */
    const val PUMP_ENTITY_ID = "binary_sensor.r_ro_m_01"

    /** Nguong chay lien tuc truoc khi bao: 7 phut (port iOS alertAfterSeconds = 7*60). */
    const val ALERT_AFTER_MS = 7 * 60 * 1000L

    /**
     * Channel RIENG cho bom (khong dung chung channel luoi hume_grid_alert_v2):
     * ten channel hien trong Cai dat > Thong bao nen phai doc ro la bom,
     * khong de user thay canh bao bom nam duoi ten "Canh bao luoi dien".
     */
    private const val CHANNEL_ID = "hume_pump_alert"
    private const val CHANNEL_NAME = "Cảnh báo máy bơm"

    /** ID notification rieng (luoi = 1, pin yeu = 2, pin day = 3, san xuat = 4). */
    const val NOTIF_ID_PUMP = 5

    /** Rung 1 lan duy nhat khi bao (port iOS warning haptic 1 lan). */
    private val ALERT_VIBRATE_PATTERN = longArrayOf(0, 500)

    /** Banner do trong EnergyScreen dang hien hay khong (port iOS pumpAlertFired/Cleared). */
    private val _alertVisible = MutableStateFlow(false)
    val alertVisible: StateFlow<Boolean> = _alertVisible.asStateFlow()

    /** Bom dang chay lan cuoi da biet (null = chua khoi tao). */
    private var lastPumpOn: Boolean? = null

    /** Da bao cho lan chay hien tai chua (chong bao lap — port iOS alertedForCurrentRun). */
    private var alertedForCurrentRun = false

    /** Dem 7 phut bang Handler nhu debounce 3s cua EnergyNotifier (khong them job moi). */
    private val handler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null

    /** ApplicationContext giu lai de gui notification khi het 7 phut (khong giu Activity). */
    private var appCtx: Context? = null

    /** State khong hop le — bo qua (port iOS isValidState). */
    private fun isValidState(raw: String?): Boolean {
        val s = raw?.trim()?.lowercase() ?: return false
        return s.isNotEmpty() && s != "unavailable" && s != "unknown" && s != "none"
    }

    /**
     * Dong bo trang thai ma KHONG bao — dung cho lan dau root chay va moi
     * lan app len foreground (port iOS sync). Bom dang chay thi bat dau dem
     * 7 phut tu luc nay (khong bao ngay ke ca bom da chay truoc do).
     */
    fun sync(ctx: Context, stateRaw: String?) {
        appCtx = ctx.applicationContext
        if (!isValidState(stateRaw)) return
        val on = stateRaw!!.trim().lowercase() == "on"
        if (lastPumpOn == null) {
            lastPumpOn = on
            if (on) startTimerIfNeeded()
            return
        }
        // Doi trang thai trong luc sync -> xu ly nhu update nhung khong bao ngay:
        // bom tat thi an banner + reset co cho lan chay sau.
        if (lastPumpOn != on) {
            lastPumpOn = on
            if (on) {
                alertedForCurrentRun = false
                startTimerIfNeeded()
            } else {
                cancelTimer()
                alertedForCurrentRun = false
                _alertVisible.value = false
            }
        }
    }

    /** Goi khi nhan state moi tu binary_sensor bom (port iOS update). */
    fun update(ctx: Context, stateRaw: String?) {
        appCtx = ctx.applicationContext
        if (!isValidState(stateRaw)) return
        val on = stateRaw!!.trim().lowercase() == "on"
        // lastPumpOn == null van chay tiep (nhu iOS guard lastPumpOn != on):
        // lan dau thay bom chay thi bat dau dem, khong bao ngay.
        if (lastPumpOn == on) return
        lastPumpOn = on
        if (on) {
            alertedForCurrentRun = false
            startTimerIfNeeded()
        } else {
            // Bom tat -> huy dem, reset co bao cho lan chay sau, an banner.
            cancelTimer()
            alertedForCurrentRun = false
            _alertVisible.value = false
        }
    }

    /** User bam "Da biet" tren banner -> chi an banner, GIU co bao toi khi bom tat. */
    fun dismissBanner() {
        _alertVisible.value = false
    }

    /** App xuong background: tam dung dem 7 phut (foreground-only nhu iOS). */
    fun onAppBackground() {
        cancelTimer()
    }

    private fun startTimerIfNeeded() {
        if (timerRunnable != null || alertedForCurrentRun) return
        val runnable = Runnable { fireAlert() }
        timerRunnable = runnable
        handler.postDelayed(runnable, ALERT_AFTER_MS)
    }

    private fun cancelTimer() {
        timerRunnable?.let { handler.removeCallbacks(it) }
        timerRunnable = null
    }

    /** Het 7 phut ma bom van chay -> bao (port iOS fireAlert). */
    private fun fireAlert() {
        timerRunnable = null
        if (lastPumpOn != true || alertedForCurrentRun) return
        alertedForCurrentRun = true
        _alertVisible.value = true
        appCtx?.let { notifyPumpAlert(it) }
    }

    private fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = "Báo khi máy bơm máy lọc nước chạy liên tục 7 phút"
                    enableVibration(true)
                    vibrationPattern = ALERT_VIBRATE_PATTERN
                    setSound(
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build(),
                    )
                },
            )
        }
    }

    private fun hasPermission(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ActivityCompat.checkSelfPermission(
                ctx, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    /** Notification bom chay lien tuc (port iOS sendNotification). */
    private fun notifyPumpAlert(ctx: Context) {
        ensureChannel(ctx)
        if (!hasPermission(ctx)) return
        val n = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ MÁY BƠM CHẠY LIÊN TỤC!")
            .setContentText("Máy bơm máy lọc nước đã chạy liên tục 7 phút. Kiểm tra máy lọc.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(NOTIF_ID_PUMP, n)
    }
}
