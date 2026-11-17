package com.smarthome.hume.feature.energy.powerwall

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.ui.components.M3EHeaderFade
import com.smarthome.hume.core.ui.components.M3EIcons
import com.smarthome.hume.core.ui.components.Ms
import com.smarthome.hume.core.ui.components.MsIcon
import kotlin.math.roundToInt

/**
 * Sheet chi tiet pack NATIVE (v52) — port 1:1 iOS `PowerwallPackSheet.swift`.
 * Thay modal HTML trong index.html: an the pack trong trang Powerwall ->
 * JS bao qua bridge "HumeAndroid" -> PowerwallScreen mo sheet nay. Giong
 * the phong (RoomSheet): sheet native, GrabHandle, vuot xuong de an,
 * detent 0.85, bo 60, nen surfaceContainer. Noi dung = dung modal cu
 * (SOC ring + volt/dong/cong suat, bieu do 16 cell, nhiet do cell, thong
 * so pack, firmware, loi), doc live data truc tiep tu payload (tu doc,
 * khong phu thuoc webview).
 *
 * v63: lop fade gradient kieu Zalo (M3EHeaderFade) — lop RIENG giua
 * ScrollView va dai top: dac 0.95 o tren (surfaceContainer cua than
 * sheet), gradient 72dp mo dan xuong trong suot; GrabHandle + header nam
 * TREN fade, luon ro net. Android: dragHandle = null de tu ve GrabHandle
 * trong lop overlay (giong iOS), vi dragHandle slot cua ModalBottomSheet
 * nam ngoai content nen fade khong phu duoc len no.
 * v66: so live tween (0.6s easeInOut) thay vi nhay coc — vong SOC, 16 cot
 * cell, cac o so (dien ap 2 le, dong/cong suat 1 le, nhiet 1 le, SOH 0 le,
 * cell 3 le). Khong co nut X — dong bang vuot xuong nhu the phong.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PowerwallPackSheet(
    packKey: String,
    payload: PowerwallLiveData.PowerwallPayload,
    onDismiss: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val extra = com.smarthome.hume.core.ui.theme.LocalHumeExtraColors.current
    val pack = payload.packs[packKey] ?: emptyMap()
    val packName = PowerwallLiveData.unitPrefixes.firstOrNull { it.first == packKey }?.third ?: packKey
    // Detent 0.85 (iOS .fraction(0.85)): gioi han chieu cao sheet.
    val maxSheetH = (LocalConfiguration.current.screenHeightDp * 0.85f).dp

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 60.dp, topEnd = 60.dp),
        containerColor = cs.surfaceContainer,
        dragHandle = null,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetH),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 81.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OverviewGrid(pack, cs, extra)
                CellChartCard(pack, cs, extra)
                TempsCard(pack, cs, extra)
                ParamsCard(pack, cs, extra)
            }
            M3EHeaderFade(
                color = cs.surfaceContainer,
                height = 81.dp,
                modifier = Modifier.align(Alignment.TopStart),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth(),
            ) {
                PowerwallGrabHandle()
                // v59: bo sheet 55 -> 60pt nen goc tren bo rong hon — doi
                // header (icon + ten pack) sang phai 8pt cho khoi sat mep
                // cong (iOS: .padding(.horizontal, 16) + .padding(.leading, 8)).
                PackHeader(pack, packName, cs, Modifier.padding(start = 24.dp, end = 16.dp))
            }
        }
    }
}

/** Tay cam keo sheet (.grab 44x5) — cung so do RoomSheet (feature:home). */
@Composable
internal fun PowerwallGrabHandle() {
    Box(
        modifier = Modifier
            .padding(top = 4.dp, bottom = 16.dp)
            .size(44.dp, 5.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.outline),
    )
}

// MARK: - Header: icon + ten pack + trang thai (khong co nut X).

@Composable
private fun PackHeader(
    pack: Map<String, Any?>,
    packName: String,
    cs: androidx.compose.material3.ColorScheme,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(cs.primaryContainer),
        ) {
            MsIcon(M3EIcons.Battery, null, tint = cs.primary, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            packName,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
            fontWeight = FontWeight.SemiBold,
            color = cs.onSurface,
        )
        Spacer(Modifier.weight(1f))
        ModePill(pack, cs)
    }
}

@Composable
private fun ModePill(pack: Map<String, Any?>, cs: androidx.compose.material3.ColorScheme) {
    val mode = pack.s("mode") ?: "-"
    val (bg, fg) = when (mode) {
        "Đang sạc" -> cs.primaryContainer to cs.onPrimaryContainer
        "Đang xả" -> cs.secondaryContainer to cs.onSecondaryContainer
        else -> cs.surfaceContainerHigh to cs.onSurfaceVariant
    }
    Text(
        mode.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        color = fg,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

// MARK: - SOC ring + Dien ap / Dong dien / Cong suat (grid 2x2 nhu modal).

@Composable
private fun OverviewGrid(
    pack: Map<String, Any?>,
    cs: androidx.compose.material3.ColorScheme,
    extra: com.smarthome.hume.core.ui.theme.HumeExtraColors,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SocCell(pack, cs, extra, Modifier.weight(1f))
            ValueCellNum(Ms.bolt, "Điện áp", pack.d("volt"), 2, " V", cs, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ValueCellNum(
                M3EIcons.ElectricMeter, "Dòng điện", pack.d("curr"), 1, " A", cs, Modifier.weight(1f),
            )
            ValueCellNum(
                M3EIcons.Power, "Công suất", pack.d("power"), 1, " W", cs, Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SocCell(
    pack: Map<String, Any?>,
    cs: androidx.compose.material3.ColorScheme,
    extra: com.smarthome.hume.core.ui.theme.HumeExtraColors,
    modifier: Modifier = Modifier,
) {
    val soc = pack.d("soc") ?: 0.0
    val frac by animateFloatAsState(
        targetValue = (soc / 100).coerceIn(0.0, 1.0).toFloat(),
        animationSpec = tween(600),
        label = "socRing",
    )
    val socInt by animateIntAsState(
        targetValue = soc.roundToInt(),
        animationSpec = tween(600),
        label = "socNum",
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(extra.surfaceHighest)
            .padding(vertical = 12.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
            val track = cs.surfaceContainerHigh
            val progress = extra.success
            Canvas(Modifier.size(64.dp)) {
                // Stroke nam GIUA tren duong tron: phai inset 1/2 stroke
                // neu khong nua ngoai cua vong bi cat boi bien Canvas.
                val stroke = 6.dp.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val arcTopLeft = Offset(inset, inset)
                drawArc(
                    color = track,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = stroke),
                )
                drawArc(
                    color = progress,
                    startAngle = -90f,
                    sweepAngle = 360f * frac,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
            Text(
                "$socInt%",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurface,
            )
        }
        Text(
            "SOC",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            fontWeight = FontWeight.Medium,
            color = cs.onSurfaceVariant,
        )
    }
}

/** O gia tri SO live (v66): so tween khi doi thay vi nhay coc. */
@Composable
private fun ValueCellNum(
    icon: Any,
    label: String,
    value: Double?,
    decimals: Int,
    unit: String,
    cs: androidx.compose.material3.ColorScheme,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(com.smarthome.hume.core.ui.theme.LocalHumeExtraColors.current.surfaceHighest)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MsIcon(icon, null, tint = cs.primary, modifier = Modifier.size(13.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                fontWeight = FontWeight.Medium,
                color = cs.onSurfaceVariant,
            )
        }
        TweenNumber(
            value = value,
            decimals = decimals,
            unit = unit,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
            color = cs.onSurface,
        )
    }
}

// MARK: - Dien ap tung cell (16 cot, cung cong thuc cellsSvg cu).

@Composable
private fun CellChartCard(
    pack: Map<String, Any?>,
    cs: androidx.compose.material3.ColorScheme,
    extra: com.smarthome.hume.core.ui.theme.HumeExtraColors,
) {
    val cells = pack.cells()
    val vals = cells.filterNotNull()
    InnerCard(title = "Điện áp từng cell", icon = Ms.battery_3_bar, cs = cs, extra = extra) {
        if (vals.isEmpty()) {
            Text(
                "Không rõ",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = cs.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            val mn = vals.min()
            val mx = vals.max()
            val lo = maxOf(mn - 0.02, 2.0)
            val hi = minOf(mx + 0.02, 4.0)
            val lo2 = if (hi - lo < 0.01) mn - 0.01 else lo
            val hi2 = if (hi - lo < 0.01) mn + 0.01 else hi
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp),
            ) {
                // 16 cot, goi CellBar vo dieu kien cho ca 16 o (ke ca o
                // thieu du lieu) de slot table on dinh khi du lieu doi.
                for (i in 0 until 16) {
                    val v = cells.getOrNull(i)
                    val barColor = when {
                        v == null -> Color.Transparent
                        v == mx && mx != mn -> extra.success
                        v == mn && mx != mn -> cs.error
                        else -> cs.primary
                    }
                    CellBar(v = v, lo = lo2, hi = hi2, color = barColor, modifier = Modifier.weight(1f))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // v66: 3 chip min/max/lech tween so (3 chu so thap phan).
                CapChipNum("Cao nhất", mx, cs, extra)
                CapChipNum("Thấp nhất", mn, cs, extra)
                CapChipNum("Lệch", mx - mn, cs, extra)
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Mot cot bieu do cell: chieu cao 8 + f*68 dp, tween 0.6s (v66). */
@Composable
private fun CellBar(
    v: Double?,
    lo: Double,
    hi: Double,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val f = if (v == null) 0.0 else ((v - lo) / (hi - lo)).coerceIn(0.0, 1.0)
    val h by animateFloatAsState(
        targetValue = (8 + f * 68).toFloat(),
        animationSpec = tween(600),
        label = "cellBar",
    )
    Box(
        modifier
            .height(if (v == null) 8.dp else h.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color),
    )
}

/** Chip min/max/lech voi so tween (v66): 3 chu so thap phan + " V". */
@Composable
private fun CapChipNum(
    label: String,
    value: Double,
    cs: androidx.compose.material3.ColorScheme,
    extra: com.smarthome.hume.core.ui.theme.HumeExtraColors,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(cs.surfaceContainerHigh)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            fontWeight = FontWeight.Medium,
            color = cs.onSurfaceVariant,
        )
        TweenNumber(
            value = value,
            decimals = 3,
            unit = " V",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = cs.onSurface,
        )
    }
}

// MARK: - Nhiet do cell.

@Composable
private fun TempsCard(
    pack: Map<String, Any?>,
    cs: androidx.compose.material3.ColorScheme,
    extra: com.smarthome.hume.core.ui.theme.HumeExtraColors,
) {
    InnerCard(title = "Nhiệt độ cell", icon = Ms.thermometer, cs = cs, extra = extra) {
        TwoColGrid(
            listOf(
                { KvNum("Cảm biến 1", pack.d("tc1"), 1, "°C", cs) },
                { KvNum("Cảm biến 2", pack.d("tc2"), 1, "°C", cs) },
                { KvNum("Cảm biến 3", pack.d("tc3"), 1, "°C", cs) },
                { KvNum("Trung bình", pack.d("tavg"), 1, "°C", cs) },
                { KvNum("Nhiệt độ cao nhất", pack.d("tmax"), 1, "°C", cs, suffix = idSuffix(pack, "idTMax")) },
                { KvNum("Nhiệt độ thấp nhất", pack.d("tmin"), 1, "°C", cs, suffix = idSuffix(pack, "idTMin")) },
            ),
        )
    }
}

// MARK: - Thong so pack.

@Composable
private fun ParamsCard(
    pack: Map<String, Any?>,
    cs: androidx.compose.material3.ColorScheme,
    extra: com.smarthome.hume.core.ui.theme.HumeExtraColors,
) {
    val st = pack.stat()
    val fw = (st["fw"] as? List<*>)?.mapNotNull { it as? String }?.filter { it.isNotEmpty() } ?: emptyList()
    val err = pack.i("err") ?: 0
    InnerCard(title = "Thông số pack", icon = Ms.info, cs = cs, extra = extra) {
        TwoColGrid(
            listOf(
                { KvNum("SOH", pack.d("soh"), 0, "%", cs) },
                { Kv("Chế độ", pack.s("mode") ?: "-", cs) },
                { Kv("Trạng thái", pack.s("sts") ?: "-", cs) },
                { Kv("Đầu ra", st.s("out") ?: "-", cs) },
                { Kv("Loại pack", st.s("type") ?: "-", cs) },
                { KvNum("Cell cao nhất", pack.d("cmax"), 3, " V", cs, suffix = idSuffix(pack, "idUMax")) },
                { KvNum("Cell thấp nhất", pack.d("cmin"), 3, " V", cs, suffix = idSuffix(pack, "idUMin")) },
                { KvNum("Độ lệch cell", pack.d("diff"), 3, " V", cs) },
                { Kv("Dung lượng đầy", st.s("fcc") ?: "-", cs) },
                { Kv("Dung lượng còn lại", st.s("rm") ?: "-", cs) },
                { Kv("Giới hạn dòng sạc", st.s("limC") ?: "-", cs) },
                { Kv("Giới hạn áp sạc", st.s("limCV") ?: "-", cs) },
                { Kv("Giới hạn dòng xả", st.s("limD") ?: "-", cs) },
                { Kv("Điện áp ADC ắc quy", st.s("adcB") ?: "-", cs) },
                { Kv("Điện áp ADC cầu chì", st.s("adcF") ?: "-", cs) },
                { Kv("Điện áp ADC pack", st.s("adcP") ?: "-", cs) },
                { Kv("Nhiệt độ board", st.s("tBoard") ?: "-", cs) },
                { Kv("Nhiệt độ FET", st.s("tFet") ?: "-", cs) },
                { Kv("FET sạc", st.s("fetC") ?: "-", cs) },
                { Kv("FET xả", st.s("fetD") ?: "-", cs) },
                { Kv("FET dự phòng", st.s("fetR") ?: "-", cs) },
                { Kv("Phiên bản firmware", if (fw.isEmpty()) "-" else fw.joinToString(" · "), cs) },
            ),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp),
        ) {
            MsIcon(
                if (err == 0) M3EIcons.Check else Ms.error,
                null,
                tint = if (err == 0) extra.success else cs.error,
                modifier = Modifier.size(14.dp),
            )
            Text(
                if (err == 0) "Không có lỗi" else "Mã lỗi $err",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                fontWeight = FontWeight.SemiBold,
                color = if (err == 0) extra.success else cs.error,
            )
            Spacer(Modifier.weight(1f))
        }
    }
}

// MARK: - Khung the con (nhu inner-card trong modal cu).

@Composable
private fun InnerCard(
    title: String,
    icon: Any,
    cs: androidx.compose.material3.ColorScheme,
    extra: com.smarthome.hume.core.ui.theme.HumeExtraColors,
    content: @Composable () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(extra.surfaceHighest)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(50))
                    .background(cs.primaryContainer),
            ) {
                MsIcon(icon, null, tint = cs.primary, modifier = Modifier.size(15.dp))
            }
            Text(
                title,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurface,
            )
        }
        content()
    }
}

@Composable
private fun Kv(
    label: String,
    value: String,
    cs: androidx.compose.material3.ColorScheme,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            fontWeight = FontWeight.Medium,
            color = cs.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
            fontWeight = FontWeight.SemiBold,
            color = cs.onSurface,
        )
    }
}

/** Dong label + SO live (v66): so tween khi doi thay vi nhay coc. */
@Composable
private fun KvNum(
    label: String,
    value: Double?,
    decimals: Int,
    unit: String,
    cs: androidx.compose.material3.ColorScheme,
    suffix: String = "",
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            fontWeight = FontWeight.Medium,
            color = cs.onSurfaceVariant,
        )
        Row {
            TweenNumber(
                value = value,
                decimals = decimals,
                unit = unit,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                color = cs.onSurface,
            )
            if (suffix.isNotEmpty()) {
                Text(
                    suffix,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface,
                )
            }
        }
    }
}

/** Luoi 2 cot (nhu LazyVGrid 2 flexible cua iOS, spacing 10). */
@Composable
private fun TwoColGrid(items: List<@Composable () -> Unit>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (row in items.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (item in row) {
                    Box(Modifier.weight(1f)) { item() }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/**
 * So tween (port iOS PowerwallTweenNumber.swift): chay dan 0.6s easeInOut
 * khi gia tri doi thay vi nhay coc; hien "-" khi thieu du lieu.
 */
@Composable
private fun TweenNumber(
    value: Double?,
    decimals: Int,
    unit: String,
    style: TextStyle,
    color: Color,
) {
    val target = value ?: 0.0
    val animated by animateFloatAsState(
        targetValue = target.toFloat(),
        animationSpec = tween(600),
        label = "tweenNum",
    )
    Text(
        if (value == null) "-" else PowerwallLiveData.fmt(animated.toDouble(), decimals) + unit,
        style = style,
        fontWeight = FontWeight.SemiBold,
        color = color,
    )
}

// MARK: - Doc payload (gia tri thieu -> "-").

private fun Map<String, Any?>.d(key: String): Double? = (this[key] as? Number)?.toDouble()
private fun Map<String, Any?>.s(key: String): String? = this[key] as? String
private fun Map<String, Any?>.i(key: String): Int? = (this[key] as? Number)?.toInt()
private fun Map<String, Any?>.stat(): Map<String, Any?> =
    (this["stat"] as? Map<String, Any?>) ?: emptyMap()

private fun Map<String, Any?>.cells(): List<Double?> {
    val raw = this["cells"] as? List<*> ?: return List(16) { null }
    return (0 until 16).map { idx -> (raw.getOrNull(idx) as? Number)?.toDouble() }
}

private fun idSuffix(pack: Map<String, Any?>, key: String): String {
    val v = pack.i(key) ?: return ""
    return " (ID $v)"
}
