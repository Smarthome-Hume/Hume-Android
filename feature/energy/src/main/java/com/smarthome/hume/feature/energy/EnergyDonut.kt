package com.smarthome.hume.feature.energy

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smarthome.hume.core.model.EnergyDonutSlice
import com.smarthome.hume.core.ui.components.M3EMotion
import com.smarthome.hume.core.ui.components.M3EPopupAnimation
import com.smarthome.hume.core.ui.components.OvershootNumber
import com.smarthome.hume.core.ui.theme.LocalHumeExtraColors
import java.util.Locale
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.launch

/**
 * Donut kieu Arc ring — port 1:1 tu iOS `EnergyDonut`
 * (Hume/M3E/Tabs/EnergyConsumeTab+Cards.swift:336-740, v72/v75).
 *
 * - Khe tung bien (v72, iOS :382-383, :595-597): cung ve tu
 *   (bien + pullback trai) toi (bien sau - pullback phai),
 *   pullback = capOverhang cua chinh cung do + minGapDeg/2, dau bo tron
 *   (StrokeCap.Round) cua 2 cung ke luon cach nhau dung minGapDeg (4 do).
 * - Arc chon no (iOS :330-331, :644-648): day +40%, dai them 9 do moi phia,
 *   ban kinh +3dp, arc ke mong -10%; lo xo bounce response 1.1s /
 *   damping 0.30 (iOS :166-171, :330) — qua
 *   [M3EPopupAnimation.springFromResponse] (stiffness = (2*pi/1.1)^2).
 * - Arc khong chon desaturate thanh xam (iOS :627-636): phu lop
 *   outlineVariant theo tien do desat (bang expand — iOS animate ca 2 ve
 *   cung target bang cung lo xo nen chung luon bang nhau).
 * - Doi lat A→B noi suy qua selT (v75, iOS :151-161, :603-625): hinh hoc
 *   (start, sweep, lineW, radius) + do xam noi suy tuyen tinh theo selT,
 *   easeInOut 1.0s, KHONG overshoot de khong pha khe 4 do.
 * - Cham nho (slot <= 20 do) duoc mien bi bop (iOS :408-410, :424-437);
 *   cham duoc chon no thanh vien nang quet 30 do, day +55% (iOS :477-497,
 *   :520-527); cham thuong co duong kinh vua slot tru khe (iOS :528-535).
 * - Giua donut: mac dinh hien TONG kWh; khi user an vao slice nao thi hien
 *   gia tri + ten cua slice do (an lai de ve tong).
 * - Entrance / data update: ve tung slice stagger (draw 1.46s emphasized,
 *   delay 180ms/slice) kem wobble (vot qua roi nay ve) — port tu iOS c5b64c6.
 *
 * Don vi pt→dp 1:1 theo dung so iOS (4 do, +40%, 9 do, +3, 1.1s/0.30,
 * <=20 do, 30 do, +55%, -10%, le trong 17dp).
 */

/** Khe ho nhin thay toi thieu giua dau bo tron cua 2 cung ke — iOS minGapDeg. */
private const val MIN_GAP_DEG = 4.0
/** Cung chon dai them moi phia — iOS pushDeg. */
private const val PUSH_DEG = 9.0
/** Slot <= 20 do la "cham", duoc mien bi bop — iOS adjustedFractions. */
private const val DOT_SLOT_DEG = 20.0
/** Vien nang cua cham duoc chon quet 30 do — iOS sliceGeometry. */
private const val CAPSULE_SWEEP_DEG = 30.0

/** Hinh hoc ve cua 1 slice — iOS sliceGeometry tra (start, sweep, lineW, radius). */
private data class SliceGeom(
    val startDeg: Double,
    val sweepDeg: Double,
    val lineWidthPx: Float,
    val radiusPx: Float,
)

/**
 * Goc nua dau bo tron cua net rong [lineWidthPx] ve tren vong ban kinh
 * [radiusPx] — iOS capOverhangDeg (:399-402): asin((w/2)/rad).
 */
private fun capOverhangDeg(lineWidthPx: Float, radiusPx: Float): Double {
    if (radiusPx <= 0f) return 0.0
    val v = min(1.0, (lineWidthPx.toDouble() / 2.0) / radiusPx.toDouble())
    return asin(v) * 180.0 / PI
}

/**
 * Fractions sau khi noi slot cua arc duoc chon (+pushDeg moi phia) va bop
 * 2 slot ke (-pushDeg moi phia) — iOS adjustedFractions (:417-441).
 * Slice cham (slot <= 20 do) duoc MIEN bi bop; phan bop don sang arc ke
 * con lai (bop 2xpushDeg). Neu ca 2 ke deu la cham thi bop deu nhu cu.
 * Tong van 360 do nen khe giu deu. Dung chung cho ca ve lan vung cham.
 */
private fun adjustedFractions(norm: List<Double>, sel: Int?, expand: Float): List<Double> {
    val sum = norm.sum()
    val safeSel = sel?.takeIf { it in norm.indices }
    if (sum <= 0 || safeSel == null || norm.size <= 1) return norm
    val adj = norm.toMutableList()
    val push = PUSH_DEG * expand.toDouble() / 360.0
    val n = adj.size
    val prev = (safeSel + n - 1) % n
    val next = (safeSel + 1) % n
    // iOS so voi fraction GOC (chua chuan hoa); o day norm da chuan hoa
    // tong = 1 nen tuong duong.
    val prevIsDot = norm[prev] * 360.0 <= DOT_SLOT_DEG
    val nextIsDot = norm[next] * 360.0 <= DOT_SLOT_DEG
    adj[safeSel] += 2 * push
    if (prevIsDot && !nextIsDot) {
        adj[next] = max(0.002, adj[next] - 2 * push)
    } else if (nextIsDot && !prevIsDot) {
        adj[prev] = max(0.002, adj[prev] - 2 * push)
    } else {
        adj[prev] = max(0.002, adj[prev] - push)
        adj[next] = max(0.002, adj[next] - push)
    }
    val total = adj.sum()
    return adj.map { it / total }
}

/**
 * Hinh hoc ve cua tung slice voi 1 lua chon [sel] cho truoc — DA nhan
 * entrance [locals] (stagger tung slice) — iOS sliceGeometry (:444-556).
 * [locals] giu nguyen cong thuc wobble port tu iOS c5b64c6.
 */
private fun sliceGeometry(
    norm: List<Double>,
    sel: Int?,
    expand: Float,
    rPx: Float,
    lineBasePx: Float,
    swellPx: Float,
    locals: List<Float>,
): List<SliceGeom> {
    val normAdj = adjustedFractions(norm, sel, expand)
    val n = normAdj.size
    if (n == 0) return emptyList()
    val safeSel = sel?.takeIf { it in normAdj.indices }
    // Bien slot: slot i = [slotStart[i], slotStart[i] + slotSweep[i]]
    val slotStart = DoubleArray(n)
    val slotSweep = DoubleArray(n)
    var curDeg = -90.0
    for (i in 0 until n) {
        slotStart[i] = curDeg
        slotSweep[i] = normAdj[i] * 360.0
        curDeg += normAdj[i] * 360.0
    }
    // Kieu ve cua slice i khi la cung thuong: do day x
    // (chon +40% / ke -10%) va ban kinh (+3dp neu chon) — iOS arcStyle.
    fun arcStyle(i: Int): Pair<Double, Float> {
        val isSel = safeSel == i
        val isAdj = safeSel != null && n > 1 &&
            (i == (safeSel + 1) % n || i == (safeSel + n - 1) % n)
        val wMul = 1.0 + (if (isSel) 0.40 else if (isAdj) -0.10 else 0.0) * expand.toDouble()
        val rad = rPx + (if (isSel) swellPx * expand else 0f)
        return wMul to rad
    }
    // Pullback co ban cua slice i tren 1 bien — iOS pullback.
    fun pullback(i: Int): Double {
        val (wMul, rad) = arcStyle(i)
        return capOverhangDeg(lineBasePx * wMul.toFloat(), rad) + MIN_GAP_DEG / 2
    }
    // Cham duoc chon no thanh vien nang 30 do: vien nang lan qua bien slot
    // cua no (spill) nen 2 arc ke phai lui them (spill + minGapDeg/2).
    var capsuleIdx: Int? = null
    var capsuleSpill = 0.0
    if (safeSel != null && n > 1) {
        val sw = slotSweep[safeSel] - 2 * pullback(safeSel)
        if (sw <= 0.05) {
            capsuleIdx = safeSel
            val ohC = capOverhangDeg(lineBasePx * (1 + 0.55f * expand), rPx)
            capsuleSpill = max(0.0, 15.0 + ohC - slotSweep[safeSel] / 2)
        }
    }
    fun leftPullback(i: Int): Double {
        var p = pullback(i)
        val cap = capsuleIdx
        if (cap != null && i == (cap + 1) % n) p += capsuleSpill + MIN_GAP_DEG / 2
        return p
    }
    fun rightPullback(i: Int): Double {
        var p = pullback(i)
        val cap = capsuleIdx
        if (cap != null && i == (cap + n - 1) % n) p += capsuleSpill + MIN_GAP_DEG / 2
        return p
    }
    val out = ArrayList<SliceGeom>(n)
    for (i in 0 until n) {
        val local = locals.getOrElse(i) { 1f }.toDouble()
        val isSel = safeSel == i
        if (n == 1) {
            // 1 slice duy nhat = vong tron kin, khong can khe — nhung van
            // day len/no ban kinh khi duoc chon (arcStyle), nhu cung thuong.
            val (wMul, rad) = arcStyle(0)
            out += SliceGeom(-90.0, 360.0 * local, lineBasePx * wMul.toFloat(), rad)
        } else {
            val dStart = slotStart[i] + leftPullback(i)
            val dSweep = slotSweep[i] - leftPullback(i) - rightPullback(i)
            val isDot = dSweep <= 0.05
            when {
                isSel && isDot -> {
                    // Cham duoc chon no thanh vien nang: quet 30 do quanh
                    // tam slot, day +55%.
                    out += SliceGeom(
                        startDeg = slotStart[i] + slotSweep[i] / 2 - 15.0,
                        sweepDeg = CAPSULE_SWEEP_DEG * local,
                        lineWidthPx = lineBasePx * (1 + 0.55f * expand),
                        radiusPx = rPx,
                    )
                }
                isDot -> {
                    // Cham: duong kinh vua slot (tru khe 2 ben) — khong bao
                    // gio rong hon slot nen khong de len cung ke.
                    val dotHalf = max(0.0, slotSweep[i] / 2 - MIN_GAP_DEG / 2)
                    val dotDia = min(lineBasePx, 2 * rPx * sin(dotHalf * PI / 180).toFloat())
                    out += SliceGeom(
                        startDeg = slotStart[i] + slotSweep[i] / 2 - 0.05,
                        sweepDeg = 0.1,
                        lineWidthPx = dotDia,
                        radiusPx = rPx,
                    )
                }
                else -> {
                    val (wMul, rad) = arcStyle(i)
                    out += SliceGeom(dStart, dSweep * local, lineBasePx * wMul.toFloat(), rad)
                }
            }
        }
    }
    return out
}

@Composable
fun EnergyDonut(
    total: Double,
    slices: List<EnergyDonutSlice>,
    sliceColors: List<Color>,
    modifier: Modifier = Modifier,
) {
    val track = LocalHumeExtraColors.current.surfaceHigh
    val grey = MaterialTheme.colorScheme.outlineVariant
    // Khi data doi: ve lai tu dau kem wobble thay vi nhay cung —
    // port tu iOS c5b64c6. Animatable de snapTo(0) roi replay.
    val tAnim = remember { Animatable(0f) }
    LaunchedEffect(slices) {
        tAnim.snapTo(0f)
        tAnim.animateTo(1f, animationSpec = tween(1460, easing = M3EMotion.emphasized))
    }
    val t = tAnim.value
    // v72: expand/desat — iOS animate ca 2 ve cung target bang cung lo xo
    // bounce (DonutCard.animateExpand) nen chung luon bang nhau; 1
    // Animatable dai dien cho ca 2, Canvas ve lai moi frame nhu iOS
    // Animatable (animatableData).
    val expandAnim = remember { Animatable(0f) }
    val expand = expandAnim.value
    // v75: selT — tien do noi suy hinh hoc khi DOI lat khac trong luc van
    // dang chon (A→B). DonutCard iOS dat fromSel = lat cu, selT = 0 roi
    // animate selT → 1; EnergyDonut noi suy tung frame trong Canvas.
    val selTAnim = remember { Animatable(1f) }
    val selT = selTAnim.value
    // Slice dang duoc chon (null = hien tong)
    var selected by remember { mutableStateOf<Int?>(null) }
    var fromSel by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()
    // Chuan hoa fractions de tong = 1 (tranh gap thua do lam tron) — nhu iOS;
    // dung chung cho ve + tap de khop nhau.
    val normFractions = remember(slices) {
        val sum = slices.sumOf { it.fraction }
        if (sum > 0) slices.map { it.fraction / sum } else slices.map { it.fraction }
    }
    // Doi lua chon — port DonutCard.onChange (:280-292) + animateExpand
    // (:166-171). A→B: expand/desat DA = 1 nen KHONG animate duoc — noi
    // suy hinh hoc bang selT thay the. nil→A / A→nil: selT ve 1 ngay,
    // expand/desat chay bang lo xo bounce.
    fun applySelection(newSel: Int?) {
        val old = selected
        if (old != null && newSel != null && old != newSel) {
            fromSel = old
            scope.launch {
                selTAnim.snapTo(0f)
                // iOS .easeInOut(duration: 1.0) — doi xung, KHONG overshoot
                // de khong pha khe 4 do; cubic-bezier (0.42, 0, 0.58, 1).
                selTAnim.animateTo(
                    1f,
                    tween(1000, CubicBezierEasing(0.42f, 0f, 0.58f, 1f)),
                )
            }
        } else {
            fromSel = null
            scope.launch { selTAnim.snapTo(1f) }
            val target = if (newSel == null) 0f else 1f
            scope.launch {
                expandAnim.animateTo(
                    target,
                    // iOS M3EMotion.bounce(response: 1.1, damping: 0.30).
                    M3EPopupAnimation.springFromResponse(
                        responseSec = 1.1,
                        dampingRatio = 0.30f,
                    ),
                )
            }
        }
        selected = newSel
    }
    Box(
        modifier = modifier.pointerInput(slices) {
            detectTapGestures { offset ->
                val w = size.width.toFloat()
                val h = size.height.toFloat()
                if (w <= 0 || h <= 0) return@detectTapGestures
                val cx = w / 2f
                val cy = h / 2f
                val dx = offset.x - cx
                val dy = offset.y - cy
                // iOS :700-711: chi tap trong dai vong (r-14 .. r+17) moi
                // chon slice — cung chon no toi r+3+12.4 = r+15.4;
                // tap lo giua / ngoai vong = bo chon.
                val rPx = min(w, h) / 2f - with(density) { 17.dp.toPx() }
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < rPx - with(density) { 14.dp.toPx() } ||
                    dist > rPx + with(density) { 17.dp.toPx() }
                ) {
                    if (selected != null) applySelection(null)
                    return@detectTapGestures
                }
                // Goc tap theo he drawArc: 0 = 3h, duong = cung chieu kim dong ho
                val ang = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                // Chuyen ve he bat dau tu -90
                var rel = ang - (-90f)
                while (rel < 0) rel += 360f
                while (rel >= 360) rel -= 360f
                // Vung cham theo fractions da noi/bop nhu luc ve —
                // iOS adjustedFractions(sel: selected). Doc expandAnim.value
                // truc tiep (khong qua `expand` da chup) de khong dung gia
                // tri cu khi pointerInput chua tao lai.
                val hitFracs = adjustedFractions(normFractions, selected, expandAnim.value)
                var acc = 0.0
                var idx: Int? = null
                for ((i, f) in hitFracs.withIndex()) {
                    val sweep = f * 360.0
                    if (rel >= acc && rel < acc + sweep) {
                        idx = i
                        break
                    }
                    acc += sweep
                }
                idx?.let { applySelection(if (selected == it) null else it) }
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // v72: le trong 17dp — iOS baseMargin: cung chon o trang thai
            // no nhat (ban kinh 53+3x1.37≈57.1 o dinh overshoot cua lo xo
            // damping 0.30, nua net 12.4) cham 69.5dp < 70dp nua khung 140dp.
            val side = min(size.width, size.height)
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = side / 2f - 17.dp.toPx()
            val lineBase = 16.dp.toPx()
            val swell = 3.dp.toPx()
            drawArc(
                color = track, startAngle = 0f, sweepAngle = 360f,
                useCenter = false, style = Stroke(lineBase, cap = StrokeCap.Round),
            )
            // Entrance stagger + wobble (port tu iOS c5b64c6, GIU NGUYEN):
            // slice vot qua roi nay ve thay vi nhay cung khi data update.
            val locals = normFractions.mapIndexed { i, _ ->
                val rawLocal = ((t * 1460f - i * 180f) / 1100f).coerceIn(0f, 1f)
                val wobble = if (rawLocal < 1f) {
                    (sin(rawLocal * 12f + i) * 0.15 * (1f - rawLocal)).toFloat()
                } else 0f
                // Clamp [0,1] nhu iOS — khong de overshoot lam slice vuot full sweep
                (rawLocal + wobble).coerceIn(0f, 1f)
            }
            // v75 — NOI SUY DOI LUA CHON (selT): hinh hoc tung slice duoc
            // tinh cho ca lat cu (fromSel) lan lat moi (selected) roi noi
            // suy tuyen tinh theo selT. Moi khe noi suy giua 2 khe >= 4 do
            // nen van >= 4 do. Khi selT = 1 (hoac khong doi lat) thi dung
            // layout cua `selected` nhu v72 — khong doi hanh vi cu.
            val geomTo = sliceGeometry(normFractions, selected, expand, r, lineBase, swell, locals)
            val n = geomTo.size
            val geom: List<SliceGeom> =
                if (fromSel != null && selected != null && selT < 1f && fromSel != selected) {
                    val geomFrom = sliceGeometry(
                        normFractions, fromSel, expand, r, lineBase, swell, locals,
                    )
                    List(n) { i ->
                        val a = geomFrom[i]
                        val b = geomTo[i]
                        SliceGeom(
                            startDeg = a.startDeg + (b.startDeg - a.startDeg) * selT.toDouble(),
                            sweepDeg = a.sweepDeg + (b.sweepDeg - a.sweepDeg) * selT.toDouble(),
                            lineWidthPx = a.lineWidthPx + (b.lineWidthPx - a.lineWidthPx) * selT,
                            radiusPx = a.radiusPx + (b.radiusPx - a.radiusPx) * selT,
                        )
                    }
                } else {
                    geomTo
                }
            // Do mo cua lop phu xam tren slice i: ngoai luc morph thi nhu
            // v72 (khong chon → desat, chon → 0); trong luc morph, xam
            // chuyen dan tu lat cu sang lat moi cung voi hinh hoc (lat moi
            // het xam dan, lat cu xam dan) — iOS overlayOpacity (:627-636).
            fun overlayOpacity(i: Int): Float {
                val s = selected ?: return 0f
                val from = fromSel
                if (from != null && selT < 1f && from != s) {
                    if (i == s) return expand * (1f - selT)
                    if (i == from) return expand * selT
                }
                return if (s != i) expand else 0f
            }
            for (i in 0 until n) {
                val g = geom[i]
                if (g.sweepDeg > 0.05) {
                    val topLeft = Offset(cx - g.radiusPx, cy - g.radiusPx)
                    val arcSize = Size(g.radiusPx * 2f, g.radiusPx * 2f)
                    val style = Stroke(g.lineWidthPx, cap = StrokeCap.Round)
                    drawArc(
                        color = sliceColors[i % sliceColors.size],
                        startAngle = g.startDeg.toFloat(),
                        sweepAngle = g.sweepDeg.toFloat(),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = style,
                    )
                    // Arc khong duoc chon: phu xam theo tien do desat.
                    val op = overlayOpacity(i)
                    if (op > 0f) {
                        drawArc(
                            color = grey.copy(alpha = op),
                            startAngle = g.startDeg.toFloat(),
                            sweepAngle = g.sweepDeg.toFloat(),
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = style,
                        )
                    }
                }
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp),
        ) {
            // Giua donut: slice duoc chon (gia tri + ten), mac dinh la TONG.
            // Overshoot khi data ve (port iOS 84dfe2c, donut-center).
            val selSlice = selected?.let { slices.getOrNull(it) }
            OvershootNumber(
                value = if (selSlice != null) total * selSlice.fraction else total,
                format = { String.format(Locale.US, "%.1f", it) },
                durationMs = 1000,
                chaosId = "donut-center",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum",
                ),
            )
            Text(
                selSlice?.name ?: "kWh",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                // iOS :~665: ten xuong toi da 2 dong + co chu, khong cat
                // "..." — lo donut chi ~90pt. minimumScaleFactor 0.7 cua
                // iOS khong co doi tac Compose nen Text khong tu co chu.
                maxLines = 2,
                modifier = Modifier.widthIn(max = 90.dp),
            )
        }
    }
}
