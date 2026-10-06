package com.smarthome.hume.feature.energy

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.smarthome.hume.core.model.EnergyUiState

/**
 * Thong bao trang thai he thong dien.
 * Theo doi: mat dien luoi, pin yeu, pin day, bat dau/kethuc san xuat dien mat troi.
 */
object EnergyNotifier {
    private const val CHANNEL_ID = "hume_energy_status"
    private const val CHANNEL_NAME = "Trạng thái hệ thống điện"

    private var lastGridOn: Boolean? = null
    private var lastLowBattery: Boolean = false
    private var lastProducing: Boolean? = null
    private var lastFullBattery: Boolean = false

    fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Cập nhật trạng thái điện mặt trời, pin, lưới điện" }
            ctx.getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(ch)
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

        // 1. Mat / co dien luoi tro lai
        if (lastGridOn != null && lastGridOn != gridOn) {
            if (!gridOn) notify(
                ctx, 1,
                "⚠️ Mất điện lưới",
                "Hệ thống đang chạy bằng pin mặt trời. SOC ${soc.toInt()}%.",
            )
            else notify(
                ctx, 1,
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

    private fun formatW(w: Double): String =
        if (w >= 1000) String.format("%.1f kW", w / 1000)
        else String.format("%.0f W", w)

    private fun notify(ctx: Context, id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ActivityCompat.checkSelfPermission(
                ctx, Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) return
        val n = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(id, n)
    }
}
