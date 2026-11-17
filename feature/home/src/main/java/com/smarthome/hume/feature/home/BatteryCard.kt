package com.smarthome.hume.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.model.BatteryUi
import com.smarthome.hume.core.ui.components.HorizontalBatteryIcon
import com.smarthome.hume.core.ui.components.M3ECard
import com.smarthome.hume.core.ui.components.M3EIcons
import com.smarthome.hume.core.ui.components.MsIcon
import com.smarthome.hume.core.ui.components.OvershootNumber
import com.smarthome.hume.core.ui.components.WavyBatteryBar

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
    // timeText = "H:MM" tu BatteryTimeCalc v70 (null khi NGHỈ); bien "—" /
    // "Đã đầy" / "Đã cạn" hien thang nhu iOS HomeBatteryCard:88-101, khong an
    // nhu text rac cua sensor template cu (parseDurationBig tra null cho
    // text khong phai duration — chi 3 chuoi nay duoc hien thang).
    val bigTime = parseDurationBig(battery.timeText)
        ?: battery.timeText?.takeIf { it == "—" || it == "Đã đầy" || it == "Đã cạn" }

    // lineHeight: iOS KHONG dat lineHeight o dau ca (M3EType chi co
    // Font.custom(size, weight) — M3ETheme.swift; grep 0 hit "lineHeight"
    // trong M3ETheme.swift / HomeBatteryCard.swift) nen SwiftUI dung line
    // height tu nhien cua Montserrat (hhea ascent 968 + descent 251 = 1.219em,
    // do tu Hume/Fonts/Montserrat-*.ttf). Typography M3 cua Android gan
    // lineHeight co dinh (16/20/28/52) nen lech iOS: pill + legend thua
    // 1.37dp moi cho, gio lon thieu 2.86dp. Moi Text trong the nay deu dung
    // .copy(lineHeight = TextUnit.Unspecified) de Compose dung line height
    // tu nhien cua cung font Montserrat -> khop iOS theo cau truc, khong
    // tu chon so. Tong chieu cao sau fix (tinh tu code): NGHI 173.3dp,
    // SAC/XA 230.1dp = iOS (20 pad + 48 + 10 + 26.6 pill + [2 + 54.9 gio
    // lon] + 10 + 14 bar + 10 + 14.6 legend + 20 pad).
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
                // (2026-11-17) iOS cardTitle 20pt (HomeBatteryCard.swift:135);
                // 0dp he qua (vong 48dp chi phoi hang).
                style = MaterialTheme.typography.headlineSmall.copy(lineHeight = TextUnit.Unspecified),
                fontWeight = FontWeight.SemiBold,
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
        // Trang thai 3 che do theo entity: DANG SAC / DANG XA / NGHI.
        // Pill dung mau theme (khong hardcode xanh/cam) de dong bo voi seed dang chon.
        val (pillBg, pillFg) = when {
            battery.isCharging -> cs.primaryContainer to cs.onPrimaryContainer
            battery.isDischarging -> cs.secondaryContainer to cs.onSecondaryContainer
            else -> cs.surfaceContainerHigh to cs.onSurfaceVariant // nghi: trung tinh
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(pillBg)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                battery.statusText,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = TextUnit.Unspecified),
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
                color = pillFg,
            )
        }
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
                style = MaterialTheme.typography.displayMedium.copy(lineHeight = TextUnit.Unspecified),
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
                        style = MaterialTheme.typography.labelSmall.copy(lineHeight = TextUnit.Unspecified),
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp,
                        color = cs.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Text(
                        et,
                        style = MaterialTheme.typography.headlineSmall.copy(lineHeight = TextUnit.Unspecified),
                        fontWeight = FontWeight.SemiBold,
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
        // reserveLimit = limit hieu dung (backupSoc khi co dien, overdischarge_soc
        // khi mat dien) — port iOS d624be1: truyen limit, KHONG truyen min(soc,limit).
        // Thanh wavy M3E dung chung voi popup pin (core/ui WavyBatteryBar).
        WavyBatteryBar(soc = soc, reserveLimit = battery.effectiveLimit, modifier = Modifier.fillMaxWidth())
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
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = TextUnit.Unspecified),
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurfaceVariant,
                )
                // So % animate overshoot 1.2s khi data ve — port tu iOS
                // OvershootNumber(duration: 1.2) trong HomeBatteryCard.
                OvershootNumber(
                    value = reserve.toDouble(),
                    format = { "${it.toInt()}%" },
                    durationMs = 1200,
                    chaosId = "batt-reserve",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = TextUnit.Unspecified,
                    ),
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
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = TextUnit.Unspecified),
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurfaceVariant,
                )
                OvershootNumber(
                    value = usage.toDouble(),
                    format = { "${it.toInt()}%" },
                    durationMs = 1200,
                    chaosId = "batt-usage",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = TextUnit.Unspecified,
                    ),
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
