package com.smarthome.hume.feature.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.model.BatteryUi
import com.smarthome.hume.core.ui.components.HorizontalBatteryIcon
import com.smarthome.hume.core.ui.components.M3ECard
import com.smarthome.hume.core.ui.components.M3EIcons
import com.smarthome.hume.core.ui.components.M3EMotion
import com.smarthome.hume.core.ui.components.MsIcon

/**
 * The pin — LAYOUT theo anh mau user gui, MAU SAC + CHU + WAVY giu theo M3E:
 * - Hang 1: "Hieu nang pin" + tron icon pin ben phai
 * - "DANG SAC/XA" (text, khong badge)
 * - Thoi gian lon "6h 50m" + "KET THUC LUC / hh:mm" ben phai
 * - Thanh wavy M3E (flat du tru + wavy su dung + track)
 * - Legend: "Du tru 20%" | "Su dung 2%"
 */
@Composable
fun BatteryCard(battery: BatteryUi, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val cs = MaterialTheme.colorScheme
    val soc = battery.soc.coerceIn(0, 100)
    val reserve = battery.reservePct
    val usage = battery.usagePct
    // timeText = friendly_time tu entity (null khi NGHỈ)
    val bigTime = parseDurationBig(battery.timeText)

    M3ECard(
        shape = RoundedCornerShape(32.dp),
        contentPadding = 20.dp,
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        // Hang 1: title + tron icon pin (layout anh mau, mau M3E)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "Hiệu năng pin",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = cs.onSurface,
            )
            Spacer(Modifier.weight(1f))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(cs.primaryContainer),
            ) {
                HorizontalBatteryIcon(
                    soc = soc,
                    color = cs.onPrimaryContainer,
                    modifier = Modifier.size(width = 30.dp, height = 18.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        // Trang thai 3 che do theo entity: DANG SAC / DANG XA / NGHI
        Text(
            battery.statusText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.5.sp,
            color = cs.onSurfaceVariant,
        )
        // Hume goc: chi hien thoi gian lon + ket thuc luc khi KHONG nghi
        // VA chi khi parse duoc duration that (null = template tra text trang thai).
        if (!battery.isResting && bigTime != null) {
        Spacer(Modifier.height(2.dp))
        // Thoi gian lon + ket thuc luc (layout anh mau)
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                bigTime,
                fontSize = 44.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-0.5).sp,
                color = cs.onSurface,
                maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            battery.endTime?.let { et ->
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "KẾT THÚC LÚC",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                        color = cs.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Text(
                        et,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = cs.onSurface,
                        modifier = Modifier.padding(top = 2.dp),
                        maxLines = 1,
                    )
                }
            }
        }
        }
        Spacer(Modifier.height(10.dp))
        // Thanh wavy M3E (giu nguyen kieu flat/wavy hien tai)
        AnimatedWavyBar(soc = soc, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        // Legend (mau M3E, vi tri theo anh mau: trai/phai)
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
                        .background(cs.primary),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Dự trữ",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurfaceVariant,
                )
                Text(
                    "$reserve%",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = cs.onSurface,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon song demo: viewBox 28x14
                Canvas(Modifier.size(24.dp, 12.dp)) {
                    val s = size.width / 28f
                    fun X(v: Float) = v * s
                    fun Y(v: Float) = v * s
                    val path = Path().apply {
                        moveTo(X(2f), Y(7f))
                        quadraticBezierTo(X(5f), Y(-1f), X(8f), Y(7f))
                        quadraticBezierTo(X(11f), Y(15f), X(14f), Y(7f))
                        quadraticBezierTo(X(17f), Y(-1f), X(20f), Y(7f))
                        quadraticBezierTo(X(23f), Y(15f), X(26f), Y(7f))
                    }
                    drawPath(
                        path, cs.primary,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    "Sử dụng",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurfaceVariant,
                )
                Text(
                    "$usage%",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = cs.onSurface,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}

/**
 * friendly_time dang "6:50" -> "6h 50m"; neu da dang doc duoc thi giu nguyen.
 * Null (khi NGHI) -> "".
 */
/**
 * Parse duration kieu Solis/template: "6:50:00" | "6:50" | "410" (phut).
 * Tra ve tong so phut; null neu khong phai duration (template tra text
 * trang thai nhu "Dang sac/Cho") -> UI an dong gio lon, khong hien rac
 * nhu "\"Dang sac/C".
 * (2026-09-30) Truoc day chi hieu H:MM nen so phut thuan bi an oan.
 */
private fun parseDurationMinutes(timeText: String?): Int? {
    if (timeText.isNullOrBlank()) return null
    val m = Regex("""(\d+):(\d{1,2})(?::\d{1,2})?""").find(timeText)
    if (m != null) {
        val h = m.groupValues[1].toIntOrNull() ?: 0
        val min = m.groupValues[2].toIntOrNull() ?: 0
        return h * 60 + min
    }
    // Tieng Viet tu template sensor: "6 giờ 50 phút", "2 giờ", "45 phút",
    // "3 tiếng 15p". (2026-09-30) Truoc 0c61975 hien take(12) nen van thay
    // thong tin; sau 0c61975 bi an oan vi parser khong hieu.
    val lower = timeText.lowercase()
    val hM = Regex("""(\d+)\s*(giờ|gio|tiếng|tieng|h)\b""").find(lower)
    val pM = Regex("""(\d+)\s*(phút|phut|p)\b""").find(lower)
    if (hM != null || pM != null) {
        val h = hM?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val min = pM?.groupValues?.get(1)?.toIntOrNull() ?: 0
        return h * 60 + min
    }
    val mins = timeText.trim().toDoubleOrNull()?.toInt()
    if (mins != null && mins >= 0) return mins
    return null
}

private fun parseDurationBig(timeText: String?): String? {
    val mins = parseDurationMinutes(timeText) ?: return null
    val h = mins / 60
    val m = mins % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

/**
 * Thanh pin M3E — giai phau giong WavyBatteryBar (flat 10dp / khe 4 / wavy 14dp
 * / khe 4 / track 10dp / stop dot 4dp) NHUNG song chay lien tuc:
 * demo `bwaveslide 1s linear infinite` (dich -15px = dung 1 buoc song).
 * Doi rong segment theo --bpr/--bpu .8s emphasized.
 */
@Composable
private fun AnimatedWavyBar(
    soc: Int,
    modifier: Modifier = Modifier,
    reserveLimit: Int = 20,
) {
    val socC = soc.coerceIn(0, 100)
    val prTarget = minOf(socC, reserveLimit) / 100f
    val puTarget = maxOf(0, socC - reserveLimit) / 100f
    val pr by animateFloatAsState(
        prTarget, tween(800, easing = M3EMotion.emphasized), label = "bpr")
    val pu by animateFloatAsState(
        puTarget, tween(800, easing = M3EMotion.emphasized), label = "bpu")
    // Song truot: 0 -> 15px (1 buoc song) moi 1s, linear, vo han
    val waveT = rememberInfiniteTransition(label = "bwaveslide")
    val phase by waveT.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
        ),
        label = "wavePhase",
    )

    val primary = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.secondaryContainer
    val pill = RoundedCornerShape(50)

    BoxWithConstraints(
        modifier = modifier
            .height(14.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(socC.toFloat(), 0f..100f)
            },
    ) {
        val w = maxWidth
        val flatW = (w - 8.dp) * pr
        val wavyL = (w - 8.dp) * pr + 4.dp
        val wavyW = (w - 8.dp) * pu
        val trackL = (w - 8.dp) * (pr + pu) + 8.dp
        val trackW = w - 4.dp - trackL

        if (flatW > 0.dp) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .width(flatW)
                    .height(10.dp)
                    .clip(pill)
                    .background(primary),
            )
        }
        if (wavyW > 0.dp) {
            // Path song build 1 LAN theo chieu rong (remember) — moi frame chi
            // translate + drawPath, khong tessellate Path 60fps (fix khựng 2026-09-30).
            // Path phu rong hon 1 buoc song moi ben de translate khong ho vien.
            val density = LocalDensity.current
            val wavePath = remember(wavyW, density) {
                val wPx = with(density) { wavyW.toPx() }
                val wl = with(density) { 15.dp.toPx() }
                val amp = with(density) { 4.dp.toPx() }
                val cy = with(density) { 14.dp.toPx() } / 2f
                Path().apply {
                    var x = -wl
                    var up = true
                    moveTo(x, cy)
                    while (x < wPx + 2 * wl) {
                        val cx = x + wl / 4f
                        val ex = x + wl / 2f
                        quadraticBezierTo(
                            cx,
                            if (up) cy - 2f * amp else cy + 2f * amp,
                            ex,
                            cy,
                        )
                        x = ex
                        up = !up
                    }
                }
            }
            Canvas(
                Modifier
                    .offset(x = wavyL)
                    .align(Alignment.CenterStart)
                    .width(wavyW)
                    .height(14.dp)
                    .clip(pill),
            ) {
                val wavelength = 15.dp.toPx()
                translate(left = -phase * wavelength) {
                    drawPath(
                        path = wavePath,
                        color = primary,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
            }
        }
        if (trackW > 0.dp) {
            Box(
                Modifier
                    .offset(x = trackL)
                    .align(Alignment.CenterStart)
                    .width(trackW)
                    .height(10.dp)
                    .clip(pill)
                    .background(trackColor),
            )
        }
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .size(4.dp)
                .background(primary, CircleShape),
        )
    }
}
