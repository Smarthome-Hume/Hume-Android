package com.smarthome.hume.core.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.delay
import kotlin.math.pow
import kotlin.random.Random

/**
 * Port tu iOS: CountUpNumber.swift, ChaosCoordinator.swift (OvershootNumber),
 * ChaoticLoading.swift (ChaoticNumber).
 *
 * Cac composable so dong — dung cho con so chinh: kWh, VND, %, W...
 * Tat ca dung [FontFamily.Monospace] (tuong duong iOS `.monospacedDigit()`),
 * khong hardcode mau/size — lay qua [style]/[color].
 */

/**
 * Singleton theo doi card nao da chay hieu ung trong session hien tai.
 * Port 1:1 tu iOS `ChaosCoordinator`.
 *
 * Vi du:
 * ```
 * if (ChaosCoordinator.shouldPlay("energy-card")) {
 *     // chay hieu ung lan dau
 * }
 * // Pull-to-refresh:
 * ChaosCoordinator.reset()
 * ```
 */
object ChaosCoordinator {
    private val playedIds = mutableSetOf<String>()

    /** Tra ve true neu la lan dau (nen chay hieu ung), dong thoi danh dau da chay. */
    @Synchronized
    fun shouldPlay(id: String): Boolean = playedIds.add(id)

    /** Reset khi can (vd: user pull-to-refresh). */
    @Synchronized
    fun reset() {
        playedIds.clear()
    }

    /** Reset 1 card cu the. */
    @Synchronized
    fun reset(id: String) {
        playedIds.remove(id)
    }
}

/**
 * Text so tu dong dem tu 0 len [value] khi xuat hien (hieu ung loading trang),
 * easing emphasized decelerate `1-(1-t)^3`.
 *
 * - [delayMs]: tre truoc khi bat dau (de stagger nhieu so).
 * - Khi [value] doi (data moi) → dem lai tu so dang hien.
 *
 * Port tu iOS `CountUpNumber` (30 steps, mac dinh 0.8s).
 *
 * Vi du:
 * ```
 * CountUpText(
 *     value = solarKwh,
 *     format = { "%.1f".format(it) },
 *     delayMs = 120, // stagger
 * )
 * ```
 */
@Composable
fun CountUpText(
    value: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    format: (Double) -> String = { "%.0f".format(it) },
    durationMs: Long = 800,
    delayMs: Long = 0,
) {
    var displayed by remember { mutableDoubleStateOf(0.0) }
    var started by remember { mutableStateOf(false) }

    // LaunchedEffect tu cancel khi [value] doi → tuong duong iOS task?.cancel().
    LaunchedEffect(value) {
        val from: Double
        if (!started) {
            // onAppear: tre de stagger roi dem 0 → value
            started = true
            if (delayMs > 0) delay(delayMs)
            from = 0.0
        } else {
            // onChange: dem lai tu so dang hien
            from = displayed
        }
        animateCountUp(from = from, to = value, durationMs = durationMs) { displayed = it }
    }

    Text(
        text = format(displayed),
        modifier = modifier,
        style = style,
        color = color,
        fontFamily = FontFamily.Monospace,
    )
}

/**
 * So chay tu 0 → vot peak → lui ve dung gia tri.
 *
 * Trigger khi DATA ve lan dau ([value] 0 → >0), khong phai khi view tao —
 * dam bao user thay hieu ung du data load cham.
 * Cac lan update sau → animate muot thuong (khong overshoot).
 * Neu co [chaosId], moi card chi chay hieu ung 1 lan/session
 * (qua [ChaosCoordinator]); nil/tu chon → luon chay khi data ve.
 *
 * Port tu iOS `OvershootNumber` (duration mac dinh 1.0s,
 * peak = max(100, target * 1.25), phase 1: 45% / phase 2: 55%).
 *
 * Vi du:
 * ```
 * OvershootNumber(
 *     value = batterySoc,
 *     chaosId = "battery-card",
 *     format = { "${it.toInt()}%" },
 * )
 * ```
 */
@Composable
fun OvershootNumber(
    value: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    format: (Double) -> String = { "%.0f".format(it) },
    durationMs: Long = 1000,
    chaosId: String? = null,
) {
    var displayed by remember { mutableDoubleStateOf(0.0) }
    var hasPlayed by remember { mutableStateOf(false) }
    var prevValue by remember { mutableStateOf(0.0) }

    LaunchedEffect(value) {
        val old = prevValue
        prevValue = value

        if (old == 0.0 && value > 0.0) {
            // Data vua ve lan dau → chay overshoot (neu duoc phep)
            val shouldPlay = if (chaosId != null) {
                ChaosCoordinator.shouldPlay(chaosId)
            } else {
                !hasPlayed
            }
            hasPlayed = true
            if (shouldPlay) {
                runOvershoot(target = value, durationMs = durationMs) { displayed = it }
            } else {
                displayed = value
            }
        } else if (hasPlayed) {
            // Data update sau do → muot thuong
            animateSmooth(from = displayed, to = value) { displayed = it }
        } else {
            displayed = value
        }
    }

    Text(
        text = format(displayed),
        modifier = modifier,
        style = style,
        color = color,
        fontFamily = FontFamily.Monospace,
    )
}

/**
 * Text so "nhay loan" truoc khi on dinh — hieu ung "dang tai".
 * Phase 1 (40%): nhay qua cac gia tri ngau nhien quanh gia tri that (±30%).
 * Phase 2 (60%): dem ve dung so voi easing emphasized decelerate.
 *
 * Port tu iOS `ChaoticNumber` (duration mac dinh 1.0s).
 *
 * Vi du:
 * ```
 * if (isLoading) ChaoticNumber(value = powerW) else Text("${powerW.toInt()} W")
 * ```
 */
@Composable
fun ChaoticNumber(
    value: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    format: (Double) -> String = { "%.0f".format(it) },
    durationMs: Long = 1000,
) {
    var displayed by remember { mutableDoubleStateOf(0.0) }

    LaunchedEffect(value) {
        // Phase 1: nhay loan (40% thoi gian)
        val chaosSteps = 6
        repeat(chaosSteps) {
            val randomOffset = Random.nextDouble(-0.3, 0.3) * value
            displayed = maxOf(0.0, value + randomOffset)
            delay((durationMs * 0.4 / chaosSteps).toLong())
        }
        // Phase 2: dem ve dung so (60% thoi gian)
        val from = displayed
        val steps = 18
        for (i in 1..steps) {
            val t = i.toDouble() / steps
            val eased = 1.0 - (1.0 - t).pow(3.0)
            displayed = from + (value - from) * eased
            delay((durationMs * 0.6 / steps).toLong())
        }
        displayed = value
    }

    Text(
        text = format(displayed),
        modifier = modifier,
        style = style,
        color = color,
        fontFamily = FontFamily.Monospace,
    )
}

// ---------------------------------------------------------------------------
// Helpers (private) — buoc chay tay nhu iOS, LaunchedEffect lo cancel
// ---------------------------------------------------------------------------

/** Dem [from] → [to] voi easing emphasized decelerate `1-(1-t)^3`. */
private suspend fun animateCountUp(
    from: Double,
    to: Double,
    durationMs: Long,
    steps: Int = 30,
    onFrame: (Double) -> Unit,
) {
    if (steps <= 0 || durationMs <= 0) {
        onFrame(to)
        return
    }
    val stepMs = durationMs / steps
    for (i in 1..steps) {
        val t = i.toDouble() / steps
        val eased = 1.0 - (1.0 - t).pow(3.0)
        onFrame(from + (to - from) * eased)
        delay(stepMs)
    }
    onFrame(to)
}

/** Overshoot: 0 → peak (45%, ease-out bac 2) → target (55%, ease-out bac 3). */
private suspend fun runOvershoot(
    target: Double,
    durationMs: Long,
    onFrame: (Double) -> Unit,
) {
    val peak = maxOf(100.0, target * 1.25)
    val p1 = 15
    for (i in 1..p1) {
        val t = i.toDouble() / p1
        onFrame(peak * (1.0 - (1.0 - t).pow(2.0)))
        delay((durationMs * 0.45 / p1).toLong())
    }
    val p2 = 20
    for (i in 1..p2) {
        val t = i.toDouble() / p2
        onFrame(peak + (target - peak) * (1.0 - (1.0 - t).pow(3.0)))
        delay((durationMs * 0.55 / p2).toLong())
    }
    onFrame(target)
}

/** Animate muot tuyen tinh cho cac update sau overshoot (0.25s, 12 steps). */
private suspend fun animateSmooth(
    from: Double,
    to: Double,
    onFrame: (Double) -> Unit,
    steps: Int = 12,
    totalMs: Long = 250,
) {
    for (i in 1..steps) {
        onFrame(from + (to - from) * i / steps)
        delay(totalMs / steps)
    }
    onFrame(to)
}
