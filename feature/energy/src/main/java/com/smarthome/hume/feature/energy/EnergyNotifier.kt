package com.smarthome.hume.feature.energy

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.smarthome.hume.core.model.EnergyUiState

/**
 * Thong bao trang thai he thong dien.
 * Theo doi: mat dien luoi, pin yeu, pin day, bat dau/kethuc san xuat dien mat troi.
 *
 * Port iOS:
 * - 3cbc087/f69835a: notification khi doi trang thai luoi (banner + am)
 * - c885ea9: bao dong tu dung sau 30s + nut "Tat bao dong" tren notification
 * - fa7805e: hien banner ca khi app dang mo (foreground) — Android: dung
 *   channel IMPORTANCE_HIGH + PRIORITY_HIGH de hien heads-up banner
 */
object EnergyNotifier {
    private const val CHANNEL_ID = "hume_energy_status"
    private const val CHANNEL_NAME = "Trạng thái hệ thống điện"
    private const val CHANNEL_ALERT_ID = "hume_grid_alert"
    private const val CHANNEL_ALERT_NAME = "Cảnh báo lưới điện"

    /** Action tat bao dong tu notification (port iOS c885ea9 STOP_ALARM). */
    const val ACTION_DISMISS_ALARM = "com.smarthome.hume.DISMISS_GRID_ALARM"
    const val NOTIF_ID_GRID = 1

    /** Bao dong tu tat sau 30s (port iOS c885ea9). */
    private const val ALARM_TIMEOUT_MS = 30_000L

    private var lastGridOn: Boolean? = null
    private var lastLowBattery: Boolean = false
    private var lastProducing: Boolean? = null
    private var lastFullBattery: Boolean = false

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
            // Channel thuong cho cac thong bao thong tin
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = "Cập nhật trạng thái điện mặt trời, pin, lưới điện" },
            )
            // Channel uu tien cao cho canh bao mat dien — hien heads-up banner
            // ca khi app dang mo (port iOS fa7805e)
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ALERT_ID,
                    CHANNEL_ALERT_NAME,
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply { description = "Báo động khi mất điện / có điện trở lại" },
            )
        }
    }

    /**
     * Goi moi khi EnergyUiState thay doi. Chi gui thong bao khi trang thai DOI.
     */
    fun onStateChanged(ctx: Context, state: EnergyUiState) {
        ensureChannel(ctx)
        val flow = state.flow
        val solarW = flow.prodKw * 1000
        val soc = state.battery.soc
        val gridOn = flow.gridOn
        val producing = solarW > 20
        val lowBattery = soc < 20
        val fullBattery = soc >= 98

        // 1. Mat / co dien luoi tro lai (port iOS 3cbc087, f69835a)
        if (lastGridOn != null && lastGridOn != gridOn) {
            if (!gridOn) notifyGridAlert(
                ctx,
                "⚠️ Mất điện lưới",
                "Hệ thống đang chạy bằng pin mặt trời. SOC ${soc.toInt()}%.",
            )
            else notifyGridAlert(
                ctx,
                "✅ Có điện lưới trở lại",
                "Lưới điện đã khôi phục.",
            )
        }
        lastGridOn = gridOn

        // 2. Pin yeu
        if (lowBattery && !lastLowBattery) {
            notify(
                ctx, 2,
                "🔋 Pin yếu (${soc.toInt()}%)",
                "Dung lượng pin dưới 20%. Hạn chế tải nặng.",
            )
        }
        lastLowBattery = lowBattery

        // 3. Pin day
        if (fullBattery && !lastFullBattery) {
            notify(
                ctx, 3,
                "🔋 Pin đã đầy (${soc.toInt()}%)",
                "Pin lưu trữ đã sạc đầy.",
            )
        }
        lastFullBattery = fullBattery

        // 4. Bat dau / ket thuc san xuat dien mat troi
        if (lastProducing != null && lastProducing != producing) {
            if (producing) notify(
                ctx, 4,
                "☀️ Bắt đầu sản xuất điện",
                "Điện mặt trời: ${formatW(solarW)}.",
            )
            else notify(
                ctx, 4,
                "🌙 Ngừng sản xuất điện",
                "Trời tối hoặc không đủ nắng.",
            )
        }
        lastProducing = producing
    }

    /**
     * Canh bao mat/co dien: uu tien cao (heads-up banner ca khi app mo),
     * nut "Tat bao dong", tu tat sau 30s (port iOS c885ea9).
     */
    private fun notifyGridAlert(ctx: Context, title: String, text: String) {
        if (!hasPermission(ctx)) return
        // Intent tat bao dong
        val dismissIntent = Intent(ctx, DismissAlarmReceiver::class.java).apply {
            action = ACTION_DISMISS_ALARM
        }
        val dismissPi = PendingIntent.getBroadcast(
            ctx, 0, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL_ALERT_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            // Tu tat sau 30s (port iOS c885ea9)
            .setTimeoutAfter(ALARM_TIMEOUT_MS)
            // Nut "Tat bao dong" (port iOS c885ea9 STOP_ALARM)
            .addAction(
                android.R.drawable.ic_delete,
                "Tắt báo động",
                dismissPi,
            )
            .build()
        NotificationManagerCompat.from(ctx).notify(NOTIF_ID_GRID, n)
    }

    private fun formatW(w: Double): String =
        if (w >= 1000) String.format("%.1f kW", w / 1000)
        else String.format("%.0f W", w)

    private fun hasPermission(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ActivityCompat.checkSelfPermission(
                ctx, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    private fun notify(ctx: Context, id: Int, title: String, text: String) {
        if (!hasPermission(ctx)) return
        val n = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(id, n)
    }

    /**
     * Xoa tat ca thong bao khi mo app (port iOS 5ce8e65: xoa badge).
     * Android khong co badge he thong (tuy launcher), xoa notification
     * la tuong duong.
     */
    fun clearAll(ctx: Context) {
        NotificationManagerCompat.from(ctx).cancelAll()
        lastGridOn = null
    }
}

/**
 * Receiver xu ly nut "Tat bao dong" tren notification (port iOS c885ea9).
 */
class DismissAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == EnergyNotifier.ACTION_DISMISS_ALARM) {
            NotificationManagerCompat.from(ctx)
                .cancel(EnergyNotifier.NOTIF_ID_GRID)
        }
    }
}
