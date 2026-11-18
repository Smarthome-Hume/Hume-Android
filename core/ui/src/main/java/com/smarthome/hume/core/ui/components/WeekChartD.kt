package com.smarthome.hume.core.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import kotlin.random.Random
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smarthome.hume.core.ui.theme.M3ETokens

/**
 * Bieu do cot phuong an D (HTML demo hume-m3e-solar-charts.html):
 * - Mau cot theo dai gradient dam->nhat dua tren gia tri (cao nhat = primary dam nhat)
 * - Duong TB dut net + nhan "TB x.x"
 * - Cham cot hien tooltip gia tri
 */
@Composable
fun WeekChartD(
    vals: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    /** Don vi hien trong tooltip (GAP-24): mac dinh kWh, tab Nuoc truyen "L". */
    unit: String = "kWh",
) {
    val cs = MaterialTheme.colorScheme
    var selected by remember { mutableStateOf<Int?>(null) }
    // TB an mac dinh — an vao bieu do moi hien
    var showAvg by remember { mutableStateOf(false) }
    if (vals.isEmpty()) return
    val maxValue = (vals.maxOrNull() ?: 0f).coerceAtLeast(0.01f)
    val minValue = vals.minOrNull() ?: 0f
    val range = (maxValue - minValue).coerceAtLeast(0.01f)
    val avg = vals.average().toFloat()
    // Mau kinh mo: surface trong suot 70% — khong vien, khong shadow cung
    val glassBg = cs.surface.copy(alpha = M3ETokens.Alpha.Glass)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            // Cat duoi iOS theo lenh user 22:03: 150 -> 120dp (token Height.WeekChart).
            .height(M3ETokens.Height.WeekChart)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { showAvg = !showAvg })
            },
    ) {
        val w = maxWidth
        val sx = w / 320.dp
        fun x(i: Int): androidx.compose.ui.unit.Dp = (20f + i * (280f / 6f)).dp * sx
        // Cat duoi iOS theo lenh user 22:03: range 126 -> 96dp, giu offset tren 14dp.
        // Cong thuc moi: y = 14 + 96 * (1 - v/max); day cot 110dp (= 14 + 96),
        // giu nguyen le duoi 10dp nhu cu (150 - 140 = 120 - 110 = 10dp).
        fun y(v: Float): androidx.compose.ui.unit.Dp = 14.dp + 96.dp * (1f - (v / maxValue).coerceIn(0f, 1f))
        val bw = 30.dp * sx
        val avgY = y(avg)
        // Duong TB dut net (ve sau cot)
        // Cat duoi iOS theo lenh user 22:03: 150 -> 120dp (token Height.WeekChart).
        Canvas(Modifier.fillMaxWidth().height(M3ETokens.Height.WeekChart)) {
            val yPx = avgY.toPx()
            val marginPx = 20.dp.toPx() * sx
            drawLine(
                color = cs.onSurfaceVariant.copy(alpha = M3ETokens.Alpha.ChartLabel),
                start = Offset(marginPx, yPx),
                end = Offset(size.width - marginPx, yPx),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
            )
        }
        // Hieu ung cot nhay loan khi xuat hien (port iOS c5b64c6 startChaos):
        // 8 steps, moi step 120ms, bien do +-40% giam dan ve 0, roi on dinh.
        var chaosFactors by remember { mutableStateOf(List(vals.size) { 1f }) }
        // Chi chay 1 lan khi xuat hien (nhu iOS onAppear), khong restart khi data doi
        LaunchedEffect(Unit) {
            val steps = 8
            repeat(steps) { s ->
                val dampen = 1f - s.toFloat() / steps
                chaosFactors = List(vals.size) {
                    1f + (Random.nextFloat() * 0.8f - 0.4f) * dampen
                }
                delay(120)
            }
            chaosFactors = List(vals.size) { 1f }
        }
        vals.forEachIndexed { i, v ->
            val top = y(v)
            // Cat duoi iOS theo lenh user 22:03: day cot 140 -> 110dp (= 14 + 96, theo y(v) moi).
            val baseH = (110.dp - top).coerceAtLeast(4.dp)
            val chaos = chaosFactors.getOrElse(i) { 1f }
            val h = baseH * chaos
            // Khi data doi, CHI cot bar animate chieu cao (emphasized 600ms) —
            // the chua khong rung/scale (port tu iOS c5b64c6).
            // Day cot giu yen o 110dp (cat duoi iOS theo lenh user 22:03): offsetY = top + (baseH - animH).
            val animH by animateDpAsState(
                targetValue = h,
                animationSpec = tween(120, easing = FastOutSlowInEasing),
                label = "barH$i",
            )
            // Dai mau dam->nhat theo gia tri: cao nhat = primary dam, thap nhat = primaryContainer nhat
            val t = ((v - minValue) / range).coerceIn(0f, 1f)
            val barColor = androidx.compose.ui.graphics.lerp(cs.primaryContainer, cs.primary, t)
            Box(
                modifier = Modifier
                    .offset(x = x(i) - bw / 2, y = top + (baseH - animH))
                    .width(bw)
                    .height(animH)
                    .clip(RoundedCornerShape(M3ETokens.Corner.Pill))
                    .background(barColor)
                    .pointerInput(i) {
                        detectTapGestures(onTap = {
                            selected = if (selected == i) null else i
                        })
                    },
            )
            if (selected == i) {
                // Tooltip dang pill M3E: vien thuoc tron hoan toan, nen dac
                // surfaceContainerHigh + bong do (khong dung nen mo glass).
                Box(
                    modifier = Modifier
                        .offset(x = x(i) - 80.dp, y = (top - 44.dp).coerceAtLeast(0.dp))
                        .width(160.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .shadow(M3ETokens.Shadow.Small, CircleShape)
                            .clip(CircleShape)
                            .background(cs.surfaceContainerHigh)
                            .padding(
                                horizontal = 14.dp,
                                vertical = M3ETokens.Spacing.Sm,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${labels.getOrElse(i) { "" }}: ${"%.1f".format(v)} $unit",
                            // bodySmall tran: LH M3 mac dinh 16 -> token 14,6 (audit muc 3).
                            style = MaterialTheme.typography.bodySmall.copy(
                                lineHeight = M3ETokens.Text.BodySmall.lineHeight,
                            ),
                            fontWeight = FontWeight.Bold,
                            color = cs.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        // Nhan "TB x.x" — an mac dinh, an vao bieu do moi hien; nen kinh mo khong vien
        if (showAvg) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = (avgY - 22.dp).coerceAtLeast(0.dp))
                    .padding(end = M3ETokens.Spacing.Xs)
                    // B-10/A-22: bo 8dp dung Corner.Tiny, khong muon Spacing.Sm lam bo.
                    .clip(RoundedCornerShape(M3ETokens.Corner.Tiny))
                    .background(glassBg)
                    .padding(horizontal = M3ETokens.Spacing.Sm, vertical = 3.dp),
            ) {
                Text(
                    "TB ${"%.1f".format(avg)}",
                    // labelSmall tran: LH M3 mac dinh 16 -> token 13,4 (audit muc 3).
                    style = MaterialTheme.typography.labelSmall.copy(
                        lineHeight = M3ETokens.Text.Caption.lineHeight,
                    ),
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface,
                    maxLines = 1,
                )
            }
        }
    }
    Spacer(Modifier.height(2.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        labels.forEachIndexed { i, l ->
            val today = i == labels.lastIndex
            Text(
                l,
                // lineHeight iOS: labelSmall 11pt tu nhien 11 x 1.219 = 13.4
                // (M3ETheme.swift khong set lineHeight; WeekChartD.swift:160-162
                // .font(M3EType.labelSmall)). Android labelSmall mac dinh 16
                // lam nhan ngay cao hon iOS ~2.6dp moi bieu do cot.
                style = MaterialTheme.typography.labelSmall.copy(
                    lineHeight = M3ETokens.Text.Caption.lineHeight,
                ),
                fontWeight = if (today) FontWeight.Bold else FontWeight.SemiBold,
                color = if (today) cs.primary else cs.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
