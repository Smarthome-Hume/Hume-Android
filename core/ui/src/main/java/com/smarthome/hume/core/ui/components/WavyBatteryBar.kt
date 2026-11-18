package com.smarthome.hume.core.ui.components

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.EaseOutQuad
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.unit.dp
import com.smarthome.hume.core.ui.theme.M3ETokens

/**
 * Thanh pin M3E — port 1:1 tu iOS WavyBatteryBar.swift (ban chot, KHONG gap/marker):
 * - Limit la RANH GIOI: segment 0→limit (nen xam secondaryContainer) vs
 *   segment limit→100% (track xam nhat, alpha 0.45).
 * - Flat dac (primary): 0 → min(soc, limit).
 * - Xam "du tru chua sac": soc → limit (khi soc < limit), cung mau nen segment.
 * - Wavy (primary): limit → soc, chi khi soc > limit; song truot lien tuc
 *   -15dp (= dung 1 buoc song) moi 1s, linear, vo han.
 * - Overshoot mo dau: flat 0→1 (khong vuot), wavy 0→full→ve dung.
 * - KHONG gap 6dp tai vi tri limit, KHONG vach marker dung (iOS dynamic moi da bo).
 * - Vi tri soc/limit animate mem bang animateFloatAsState khi doi (port iOS
 *   .animation(emphasized, value: animKey "soc-limit")) nen sau lan dau
 *   thanh van truot mem thay vi set tuc thi nhu co che barPlayed cu.
 *
 * Component DUNG CHUNG: the pin Home (BatteryCard) + popup pin tab Dien
 * (InsightPopup). Truoc day popup dung ban cu (pr = min(soc,limit), khe 4px,
 * song tinh); gio cung 1 model segment + song truot voi the pin.
 * (B5 KHONG dong vao: popup van doc battery_power_flow nhu cu, chua kiem
 * chung lai doi voi battery_power_2 cua iOS.)
 */
@Composable
fun WavyBatteryBar(
    soc: Int,
    modifier: Modifier = Modifier,
    reserveLimit: Int = 20,
) {
    val socC = soc.coerceIn(0, 100)
    val limitC = reserveLimit.coerceIn(0, 100)
    val socPos = socC / 100f
    val limitPos = limitC / 100f

    // Vi tri animate mem khi soc/limit doi (thay cho set tuc thi sau lan dau).
    val animSocPos by animateFloatAsState(
        targetValue = socPos,
        animationSpec = tween(700, easing = M3EMotion.emphasized),
        label = "socPos",
    )
    val animLimitPos by animateFloatAsState(
        targetValue = limitPos,
        animationSpec = tween(700, easing = M3EMotion.emphasized),
        label = "limitPos",
    )
    val flatEnd = minOf(animSocPos, animLimitPos) // 0 → min(soc, limit): flat dac
    val wavyLen = maxOf(0f, animSocPos - animLimitPos) // limit → soc: wavy (khi soc > limit)

    // Animation scale mo dau: flat 0→1 (ease-out bac 2, 630ms), wavy 0→full→ve
    // dung (ease-out bac 2 len full 630ms roi ease-out bac 3 ve 1, tong 1400ms)
    // — dung iOS WavyBatteryBar.startBarAnimation (18+22 buoc x 35ms).
    // He so de wavy dat full 100%: 1/wavyLen — nhu iOS fullScale.
    val full = if (wavyLen > 0f) 1f / wavyLen else 1f
    val flatScale by animateFloatAsState(
        targetValue = if (socC > 0) 1f else 0f,
        animationSpec = tween(durationMillis = 630, easing = EaseOutQuad),
        label = "flatScale",
    )
    val barScale by animateFloatAsState(
        targetValue = if (socC > 0) 1f else 0f,
        animationSpec = keyframes {
            durationMillis = 1400
            0f at 0 using EaseOutQuad
            full at 630 using EaseOutCubic
            1f at 1400
        },
        label = "barScale",
    )
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
    val segmentBg = MaterialTheme.colorScheme.secondaryContainer // nen segment 0→limit
    val trackColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = M3ETokens.Alpha.Track) // track nhat
    val pill = RoundedCornerShape(M3ETokens.Corner.Pill)

    BoxWithConstraints(
        modifier = modifier
            .height(14.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(socC.toFloat(), 0f..100f)
            },
    ) {
        val w = maxWidth
        val barW = w - 8.dp // chieu rong vung ve (tru 8dp nhu iOS)
        val limitX = barW * animLimitPos // vi tri limit

        // Segment 0→limit: nen xam (lien mach, khong tru gap)
        val segW = limitX.coerceAtLeast(0.dp)
        // Track limit→100%: xam nhat
        val trackL = limitX + 8.dp
        val trackW = (w - 4.dp - trackL).coerceAtLeast(0.dp)
        // Flat: 0 → min(soc, limit)
        val flatW = (barW * flatEnd * flatScale).coerceAtLeast(0.dp)
        // Wavy: limit → soc (khi soc > limit)
        val wavyL = limitX + 4.dp
        val wavyW = (barW * wavyLen * barScale).coerceAtLeast(0.dp)

        // Track: vung su dung (limit → 100%), mau nhat
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
        // Nen segment du tru (0 → limit): xam dac
        if (segW > 0.dp) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .width(segW)
                    .height(10.dp)
                    .clip(pill)
                    .background(segmentBg),
            )
        }
        // Flat dac: phan da sac (0 → min(soc, limit)), nam tren nen segment
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
        // (Khong vach marker dung tai limit — iOS dynamic moi da bo.)
        if (wavyW > 0.dp) {
            // Path song build 1 LAN theo chieu rong (remember) — moi frame chi
            // translate + drawPath, khong tessellate Path 60fps (fix khựng 2026-09-30).
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
        // Stop dot cuoi thanh (iOS co: Circle 4pt trailing).
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .size(4.dp)
                .background(primary, CircleShape),
        )
    }
}

/** Legend cham tron dac / icon song mini cho the pin. */
@Composable
fun BatteryLegend(modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(primaryColor, CircleShape))
            Text(
                text = "Dự trữ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(22.dp, 10.dp)) {
                val cy = size.height / 2f
                val path = Path().apply {
                    moveTo(0f, cy)
                    quadraticBezierTo(size.width * 0.25f, 0f, size.width * 0.5f, cy)
                    quadraticBezierTo(size.width * 0.75f, size.height, size.width, cy)
                }
                drawPath(path, primaryColor, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            }
            Text(
                text = "Sử dụng",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}
