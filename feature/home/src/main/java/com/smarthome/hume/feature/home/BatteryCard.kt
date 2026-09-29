package com.smarthome.hume.feature.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.model.BatteryUi
import com.smarthome.hume.core.ui.components.M3EIcons
import com.smarthome.hume.core.ui.components.M3EMotion
import com.smarthome.hume.core.ui.components.MsIcon
import kotlin.math.max
import kotlin.math.min

// Mau theo anh mau user gui (Hume goc)
private val PinCardBg = Color(0xFFD8E9CF) // xanh la nhat
private val PinDark = Color(0xFF1C2B1F) // chu xanh den
private val PinCircleBg = Color(0xFFAED6A0) // tron icon pin
private val PinFill = Color(0xFF58B368) // xanh la dam (fill)
private val PinTrack = Color(0xFFB9C6B2) // track xam xanh
private val PinBlue = Color(0xFF5B8DEF) // cham "Su dung"

/**
 * The pin theo anh mau user gui:
 * - Card xanh la nhat, bo 28dp
 * - Hang 1: "Hieu nang Pin" 20sp Bold + tron icon pin 56dp
 * - "DANG SAC" 13sp ls 2sp
 * - Thoi gian lon "6h 50m" 52sp Light + "KET THUC LUC" / "17:08" ben phai
 * - Thanh progress 30dp: fill xanh la + soc ke xanh trang o bien + track xam
 * - Legend: "Du tru 20%" (cham xanh la) | "Su dung 2%" (cham xanh duong)
 */
@Composable
fun BatteryCard(battery: BatteryUi, modifier: Modifier = Modifier) {
    val charging = battery.isCharging
    val soc = battery.soc.coerceIn(0, 100)
    val reserve = min(soc, 20)
    val usage = max(0, soc - 20)
    // timeText dang "Sac day sau 6:50" / "Con 1:29" -> doi sang "6h 50m"
    val bigTime = parseDurationBig(battery.timeText)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(PinCardBg)
            .padding(20.dp),
    ) {
        // Hang 1: title + tron icon pin
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "Hiệu năng Pin",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PinDark,
            )
            Spacer(Modifier.weight(1f))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(PinCircleBg),
            ) {
                MsIcon(
                    M3EIcons.Battery,
                    contentDescription = null,
                    tint = PinDark,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        // Trang thai
        Text(
            if (charging) "ĐANG SẠC" else "ĐANG XẢ",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp,
            color = PinDark,
        )
        Spacer(Modifier.height(4.dp))
        // Thoi gian lon + ket thuc luc
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                bigTime,
                fontSize = 52.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-1).sp,
                color = PinDark,
                maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            battery.endTime?.let { et ->
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "KẾT THÚC LÚC",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp,
                        color = PinDark.copy(alpha = 0.7f),
                        maxLines = 1,
                    )
                    Text(
                        et,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = PinDark,
                        modifier = Modifier.padding(top = 2.dp),
                        maxLines = 1,
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        // Thanh progress: fill xanh la + soc ke o bien
        StripedProgressBar(
            fraction = soc / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp),
        )
        Spacer(Modifier.height(8.dp))
        // Legend
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(PinFill),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Dự trữ $reserve%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = PinDark.copy(alpha = 0.75f),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(PinBlue),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Sử dụng $usage%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = PinDark.copy(alpha = 0.75f),
                )
            }
        }
    }
}

/**
 * "Sac day sau 6:50" -> "6h 50m"; "Con 1:29" -> "1h 29m".
 * Khong parse duoc -> "--".
 */
private fun parseDurationBig(timeText: String?): String {
    if (timeText.isNullOrBlank()) return "--"
    val m = Regex("""(\d+):(\d{1,2})""").find(timeText) ?: return "--"
    val h = m.groupValues[1].toIntOrNull() ?: 0
    val min = m.groupValues[2].toIntOrNull() ?: 0
    return if (h > 0) "${h}h ${min}m" else "${min}m"
}

/**
 * Thanh progress 30dp bo tron: track xam xanh, fill xanh la,
 * dau fill co doan soc ke xanh/trang (nhu anh mau).
 */
@Composable
private fun StripedProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    val frac by animateFloatAsState(
        fraction.coerceIn(0f, 1f),
        tween(800, easing = M3EMotion.emphasized),
        label = "pinfrac",
    )
    val pill = RoundedCornerShape(50)
    BoxWithConstraints(
        modifier = modifier
            .clip(pill)
            .background(PinTrack),
    ) {
        val w = maxWidth
        val fillW = w * frac
        val stripeW = 14.dp
        // Fill xanh la
        if (fillW > 0.dp) {
            Box(
                Modifier
                    .width(fillW)
                    .height(30.dp)
                    .clip(pill)
                    .background(PinFill),
            )
        }
        // Soc ke o bien fill (chi khi 0 < frac < 1)
        if (frac > 0.02f && frac < 0.98f) {
            Canvas(
                Modifier
                    .width(stripeW)
                    .height(30.dp)
                    .align(Alignment.CenterStart)
                    .padding(start = fillW - stripeW),
            ) {
                val sw = size.width
                val sh = size.height
                val path = Path()
                val stripePx = 5.dp.toPx()
                var x = -sh
                var dark = true
                while (x < sw + sh) {
                    path.reset()
                    path.moveTo(x, 0f)
                    path.lineTo(x + stripePx, 0f)
                    path.lineTo(x + stripePx - sh * 0.5f, sh)
                    path.lineTo(x - sh * 0.5f, sh)
                    path.close()
                    drawPath(path, if (dark) PinFill else Color.White.copy(alpha = 0.85f))
                    x += stripePx
                    dark = !dark
                }
            }
        }
    }
}
