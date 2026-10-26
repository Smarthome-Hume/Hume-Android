package com.smarthome.hume.feature.energy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.model.EnergyUiState
import com.smarthome.hume.core.model.WaterUiState
import com.smarthome.hume.core.ui.components.M3ECard
import com.smarthome.hume.core.ui.components.Ms
import com.smarthome.hume.core.ui.components.MsIcon
import com.smarthome.hume.core.ui.components.WeekChartD
import com.smarthome.hume.core.ui.theme.LocalHumeExtraColors
import java.util.Locale

/**
 * Sub-tab Nuoc RO (Batch C, GAP-01 — port iOS EnergyWaterTab.swift).
 * Ba nhom the, dung thu tu iOS: Su dung -> Do sach -> luoi sensor cards.
 * Du lieu lay tu state.water / state.waterWeek (AppEnergyRepository build
 * tu entity HA .22, xem comment o WaterUiState) — tab nay khong tu goi HA.
 * Khong copy uiThrottle 2s cua iOS; khong hien loi 4-5.
 *
 * ICON (Batch E GAP-11, font regen 150 glyph tu Material Symbols Rounded
 * goc v2.978): iOS dung SF Symbol `filter` va `pump`; nay font da co
 * glyph that nen khong dung icon tam nua:
 *   Tuoi tho loi -> Ms.filter     (U+E3D3, glyph loc)
 *   May bom     -> Ms.water_pump (U+F5D8, glyph bom nuoc)
 * Nuoc cap -> Ms.water_drop va Dang su dung -> Ms.person la parity dung
 * iOS (ca 2 deu co trong font).
 */

/** "29,9" voi dau cham (khop Locale.US ben iOS, temp1). */
private fun temp1(v: Double): String = String.format(Locale.US, "%.1f", v)

/** Dong thoi gian duoi ten sensor (port iOS waterAgoString). */
private fun waterAgo(minutes: Int?): String {
    if (minutes == null) return "Giám sát"
    if (minutes < 1) return "Vừa xong"
    if (minutes < 60) return "$minutes phút trước"
    val h = minutes / 60
    if (h < 24) return "$h giờ trước"
    return "${h / 24} ngày trước"
}

@Composable
fun EnergyWaterTab(
    state: EnergyUiState,
    risePlayed: MutableSet<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        WaterWeekCard(state, risePlayed)
        WaterTdsCard(state.water, risePlayed)
        WaterSensorsGrid(state.water, risePlayed)
    }
}

// ---------- 1. Su dung (so lit lon + nhiet do mo + bieu do tuan) ----------

@Composable
private fun WaterWeekCard(state: EnergyUiState, risePlayed: MutableSet<String>) {
    val cs = MaterialTheme.colorScheme
    val w = state.water
    M3ECard(
        shape = RoundedCornerShape(32.dp),
        contentPadding = 20.dp,
        containerColor = LocalHumeExtraColors.current.surfaceHighest,
        modifier = Modifier.riseOnce("water-week", 420, risePlayed),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "Sử dụng",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            // Cum gia tri (port iOS 26/10: lit headlineLarge 32 semibold;
            // nhiet do chi tham khao nen xuong bodyMedium mo, cung " • " va
            // "°C"). Moi Text maxLines=1 + softWrap=false de khong vo dong
            // kieu bug iOS 22:14 25/10 ("10." / "6"); don vi can baseline
            // voi gia tri (alignByBaseline, khong dung Alignment.Bottom).
            Row {
                Text(
                    temp1(w.todayLiters),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp,
                    ),
                    color = cs.onSurface,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    "L",
                    style = MaterialTheme.typography.labelLarge,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .alignByBaseline(),
                )
                Text(
                    " • ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    temp1(w.tempC),
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    "°C",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .alignByBaseline(),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        val vals = state.waterWeek.map { it.kwh.toFloat() }
        val labels = state.waterWeek.map { it.label }
        if (vals.isNotEmpty()) {
            WeekChartD(vals = vals, labels = labels, unit = "L")
        } else {
            Text(
                "Đang tải dữ liệu…",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
            )
        }
    }
}

// ---------- 2. Do sach nuoc (TDS + badge + thang 5 muc) ----------

@Composable
private fun WaterTdsCard(w: WaterUiState, risePlayed: MutableSet<String>) {
    val cs = MaterialTheme.colorScheme
    val extra = LocalHumeExtraColors.current
    val tds = w.tds
    // Badge theo thang ngu nghia iOS: <50 rat sach (success), <100 sach
    // (primary), <200 trung binh (tertiary), con lai kem (error).
    val (badge, badgeBg, badgeFg) = when {
        tds < 50 -> Triple("Rất sạch", extra.successContainer, extra.onSuccessContainer)
        tds < 100 -> Triple("Sạch", cs.primaryContainer, cs.onPrimaryContainer)
        tds < 200 -> Triple("Trung bình", cs.tertiaryContainer, cs.onTertiaryContainer)
        else -> Triple("Kém", cs.errorContainer, cs.onErrorContainer)
    }
    // Thang 5 muc iOS: 0-50-100-200-300+.
    val segOn = when {
        tds < 50 -> 1
        tds < 100 -> 2
        tds < 200 -> 3
        tds < 300 -> 4
        else -> 5
    }
    M3ECard(
        shape = RoundedCornerShape(32.dp),
        contentPadding = 20.dp,
        containerColor = extra.surfaceHighest,
        modifier = Modifier.riseOnce("water-tds", 440, risePlayed),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Độ sạch nước",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(badgeBg)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(
                    badge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = badgeFg,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row {
            Text(
                "${tds.toInt()}",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp,
                ),
                color = cs.onSurface,
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                "ppm TDS",
                style = MaterialTheme.typography.labelLarge,
                color = cs.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .alignByBaseline(),
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(5) { i ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (i < segOn) extra.success else cs.surfaceContainer,
                        ),
                )
            }
        }
        Row(modifier = Modifier.padding(top = 6.dp)) {
            Text("0", style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text("50", style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text("100", style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text("200", style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text("300+", style = MaterialTheme.typography.labelSmall,
                color = cs.onSurfaceVariant)
        }
    }
}

// ---------- 3. Luoi sensor cards (mau M3ESensorCard cua iOS) ----------

@Composable
private fun WaterSensorsGrid(w: WaterUiState, risePlayed: MutableSet<String>) {
    val filterActive = w.filterPct > 0
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.riseOnce("water-sensors", 460, risePlayed),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            WaterSensorCard(
                glyph = Ms.filter,
                name = "Tuổi thọ lõi",
                ago = waterAgo(w.filterMinutesAgo),
                active = filterActive,
                pill = if (filterActive) {
                    "${w.filterPct.toInt()}% · ${w.filterLiters.toInt()} L"
                } else "—",
                modifier = Modifier.weight(1f),
            )
            WaterSensorCard(
                glyph = Ms.water_pump,
                name = "Máy bơm",
                ago = waterAgo(w.pumpMinutesAgo),
                active = w.pumpOn,
                pill = if (w.pumpOn) "ĐANG CHẠY" else "TẮT",
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            WaterSensorCard(
                glyph = Ms.water_drop,
                name = "Nước cấp",
                ago = waterAgo(w.waterInMinutesAgo),
                active = w.waterInOn,
                pill = if (w.waterInOn) "ĐANG CẤP" else "NGẮT",
                modifier = Modifier.weight(1f),
            )
            WaterSensorCard(
                glyph = Ms.person,
                name = "Đang sử dụng",
                ago = waterAgo(w.waterUseMinutesAgo),
                active = w.waterUseOn,
                pill = if (w.waterUseOn) "ĐANG DÙNG" else "KHÔNG",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * The sensor so/binary theo dung mau M3ESensorCard iOS: icon tron 44 +
 * ten + dong thoi gian, pill 120 o giua. Nen active = primaryContainer,
 * khong active = surfaceContainerHighest; pill active = primary.
 */
@Composable
private fun WaterSensorCard(
    glyph: String,
    name: String,
    ago: String,
    active: Boolean,
    pill: String,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val cardBg = if (active) cs.primaryContainer else cs.surfaceContainerHighest
    val containerOn = cs.onPrimaryContainer
    val pillBg = if (active) cs.primary else cs.surfaceContainer
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(26.dp))
            .background(cardBg)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (active) Color.White.copy(alpha = 0.55f)
                        else cs.surfaceContainer,
                    ),
            ) {
                MsIcon(
                    glyph,
                    contentDescription = null,
                    tint = if (active) containerOn else cs.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)) {
                Text(
                    name,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) containerOn else cs.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    ago,
                    style = MaterialTheme.typography.labelSmall,
                    color = (if (active) containerOn else cs.onSurfaceVariant)
                        .copy(alpha = if (active) 0.75f else 1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(120.dp)
                    .clip(CircleShape)
                    .background(pillBg)
                    .padding(vertical = 6.dp),
            ) {
                Text(
                    pill,
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 0.8.sp,
                    ),
                    color = if (active) cs.onPrimary else cs.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                )
            }
            Spacer(Modifier.weight(1f))
        }
    }
}
