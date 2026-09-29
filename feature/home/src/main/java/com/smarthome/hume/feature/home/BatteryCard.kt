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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.model.BatteryUi
import com.smarthome.hume.core.ui.theme.LocalHumeExtraColors
import com.smarthome.hume.core.ui.components.M3ECard
import com.smarthome.hume.core.ui.components.M3EMotion
import kotlin.math.max
import kotlin.math.min

/**
 * The pin theo demo rev12 (.batcard): bo 32px, padding 20px;
 * header "Hieu nang pin" 14px/700 + badge .bstat (11px/800, ls .6):
 * sac = successContainer, xa = tertiaryContainer (demo chi co 2 trang thai);
 * soc 34px/800 + "%" 14px/600; cong suat 18px/800 (sac: success, xa: #D97706);
 * thanh wavy song CHAY (bwaveslide 1s linear infinite);
 * legend: cham tron "Du tru" + icon song "Su dung"; footer 12px/500.
 */
@Composable
fun BatteryCard(battery: BatteryUi, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val extra = LocalHumeExtraColors.current
    val charging = battery.isCharging
    // Dung SOC that tu Home Assistant - KHONG mo phong
    val soc = battery.soc.coerceIn(0, 100)
    val reserve = min(soc, 20)
    val usage = max(0, soc - 20)
    M3ECard(
        shape = RoundedCornerShape(32.dp),
        contentPadding = 20.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        // Hang tren: tieu de + badge trang thai
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
            // Badge .bstat: demo chi co 2 trang thai (Sac/Xa), 11px/800/ls.6
            val (badgeText, badgeBg, badgeFg) = if (charging) {
                Triple("ĐANG SẠC", extra.successContainer, extra.onSuccessContainer)
            } else {
                Triple("ĐANG XẢ", cs.tertiaryContainer, cs.onTertiaryContainer)
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(badgeBg)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    badgeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.6.sp,
                    color = badgeFg,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        // Dong 2: % nho o dau thanh + thanh wavy song chay
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "$soc%",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.3).sp,
                color = cs.onSurface,
                maxLines = 1,
            )
            Spacer(Modifier.width(10.dp))
            AnimatedWavyBar(soc = soc, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        // Dong 3: legend Du tru / Su dung + KET THUC LUC (neu co tu HA)
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
                Spacer(Modifier.width(14.dp))
                // Icon song demo: viewBox 28x14, scale ve 24dp
                Canvas(Modifier.size(24.dp, 12.dp)) {
                    val s = size.width / 28f
                    fun X(v: Float) = v * s
                    fun Y(v: Float) = v * s
                    val path = Path().apply {
                        // M2 7 Q5 -1 8 7 T14 7 T20 7 T26 7 (T = phan xa control)
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
            // Phai: gio ket thuc hh:mm (giu lai theo ban truoc, user da duyet)
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
            Canvas(
                Modifier
                    .offset(x = wavyL)
                    .align(Alignment.CenterStart)
                    .width(wavyW)
                    .height(14.dp)
                    .clip(pill),
            ) {
                val wavelength = 15.dp.toPx()
                val amplitude = 4.dp.toPx()
                val cy = size.height / 2f
                // Ve song rong hon segment, dich trai theo phase de tao chuyen dong
                val shift = phase * wavelength
                val path = Path().apply {
                    var x = -shift
                    var up = true
                    moveTo(x, cy)
                    while (x < size.width + wavelength) {
                        val cx = x + wavelength / 4f
                        val ex = x + wavelength / 2f
                        quadraticBezierTo(
                            cx,
                            if (up) cy - 2f * amplitude else cy + 2f * amplitude,
                            ex,
                            cy,
                        )
                        x = ex
                        up = !up
                    }
                }
                drawPath(
                    path = path,
                    color = primary,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round),
                )
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
