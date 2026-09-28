package com.smarthome.hume.feature.energy

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.model.EnergyDevice
import com.smarthome.hume.core.model.EnergyPowerKind
import com.smarthome.hume.core.model.EnergyUiState
import com.smarthome.hume.core.model.EnergyWeekPoint
import com.smarthome.hume.core.ui.components.M3ECard
import com.smarthome.hume.core.ui.components.M3EIcons
import com.smarthome.hume.core.ui.components.M3EMotion
import com.smarthome.hume.core.ui.components.pressMorph
import com.smarthome.hume.core.ui.components.Ms
import com.smarthome.hume.core.ui.components.MsIcon
import com.smarthome.hume.core.ui.theme.LocalHumeExtraColors
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.delay

private val vn = NumberFormat.getInstance(Locale("vi", "VN"))
private fun vnd(v: Long): String = vn.format(v)

/** Chu so nghin nho, giu so nguyen giong demo (18.450 VND). */
private fun kwh1(v: Double): String = String.format(Locale.US, "%.1f", v)

private fun TextStyle.tnum(): TextStyle = copy(fontFeatureSettings = "tnum")

@Composable
fun EnergyConsTab(
    state: EnergyUiState,
    ui: EnergyScreenUi,
    vm: EnergyViewModel,
    risePlayed: MutableSet<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        WeekCard(state, risePlayed)
        CostCard(state, risePlayed)
        PowerCard(state, risePlayed)
        DonutCard(state, risePlayed)
        DevicesCard(state = state, ui = ui, vm = vm, risePlayed = risePlayed)
        LowBatteryCard(state, risePlayed)
    }
}

// ---------- 1. Nang luong su dung ----------

@Composable
private fun WeekCard(state: EnergyUiState, risePlayed: MutableSet<String>) {
    // Tick 8s: cot hom nay +0.05, tran 9.9 (demo drawW)
    var boost by remember { mutableDoubleStateOf(0.0) }
    val latest by rememberUpdatedState(state)
    LaunchedEffect(Unit) {
        while (true) {
            delay(8000)
            val s = latest
            val base = s.week.find { it.isToday }?.kwh ?: s.todayKwh
            boost = (minOf(9.9, base + boost + 0.05) - base).coerceAtLeast(0.0)
        }
    }
    val todayVal = (state.week.find { it.isToday }?.kwh ?: state.todayKwh) + boost

    M3ECard(
        shape = RoundedCornerShape(32.dp),
        contentPadding = 20.dp,
        containerColor = LocalHumeExtraColors.current.surfaceHighest,
        modifier = Modifier.riseOnce("cons-week", 420, risePlayed),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Năng lượng sử dụng",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 14.sp, fontWeight = FontWeight.Bold),
                )
                Text(
                    "7 ngày qua · cập nhật trực tiếp",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp, fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    kwh1(todayVal),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp,
                    ).tnum(),
                )
                Text(
                    " kWh",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.week.forEach { p ->
                WeekBar(
                    p = p,
                    kwh = if (p.isToday) todayVal else p.kwh,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun WeekBar(p: EnergyWeekPoint, kwh: Double, modifier: Modifier = Modifier) {
    // Thang CO DINH 10 kWh (demo: height = v/10*100%)
    val frac = (kwh / 10.0).coerceIn(0.0, 1.0)
    val h by animateFloatAsState(
        targetValue = frac.toFloat(),
        animationSpec = tween(800, easing = M3EMotion.emphasized),
        label = "wbarH",
    )
    val label = if (p.isToday) "HN" else p.label
    val primary = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .fillMaxHeight()
            .semantics { contentDescription = "$label: ${kwh1(kwh)} kWh" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 34.dp)
                .height((120 * h).dp)
                .shadow(
                    if (p.isToday) 4.dp else 0.dp,
                    CircleShape,
                    spotColor = primary.copy(alpha = 0.4f),
                )
                .clip(CircleShape)
                .background(
                    if (p.isToday) primary
                    else MaterialTheme.colorScheme.primaryContainer,
                ),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = if (p.isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
            ),
            color = if (p.isToday) primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------- 2. Chi phi dien ----------

@Composable
private fun CostCard(state: EnergyUiState, risePlayed: MutableSet<String>) {
    // Tick 9s nhu demo (cGrid=18450+rand(120), cHome=26880+rand(200)):
    // chi nhich hien thi, khong doi du lieu HA goc.
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(9000)
            tick++
        }
    }
    val gridJit = Random(tick * 71 + 3).nextInt(120)
    val homeJit = Random(tick * 131 + 7).nextInt(200)
    M3ECard(
        shape = RoundedCornerShape(32.dp),
        contentPadding = 20.dp,
        containerColor = LocalHumeExtraColors.current.surfaceHighest,
        modifier = Modifier.riseOnce("cons-cost", 440, risePlayed),
    ) {
        Text(
            "Chi phí điện",
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 14.sp, fontWeight = FontWeight.Bold),
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat2(
                label = "Điện lưới",
                value = vnd(state.cost.gridVnd + gridJit),
                modifier = Modifier.weight(1f),
            )
            Stat2(
                label = "Điện tiêu thụ",
                value = vnd(state.cost.homeVnd + homeJit),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PBox("Giá mua", "${vnd(state.cost.buyPrice)}đ", Modifier.weight(1f))
            PBox("Giá EVN", "${vnd(state.cost.evnPrice)}đ", Modifier.weight(1f))
            PBox("Tiết kiệm", "${vnd(state.cost.savedVnd)}đ", Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat2(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(14.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 19.sp, fontWeight = FontWeight.ExtraBold,
                ).tnum(),
            )
            Text(
                " VND",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
            ).tnum(),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

// ---------- 3. Cong suat hoat dong ----------

@Composable
private fun PowerCard(state: EnergyUiState, risePlayed: MutableSet<String>) {
    // Live tick 5s (demo: randomize gia tri, bar transition width .8s emphasized)
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000)
            tick++
        }
    }
    M3ECard(
        shape = RoundedCornerShape(32.dp),
        contentPadding = 20.dp,
        containerColor = LocalHumeExtraColors.current.surfaceHighest,
        modifier = Modifier.riseOnce("cons-power", 460, risePlayed),
    ) {
        Text(
            "Công suất hoạt động",
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 14.sp, fontWeight = FontWeight.Bold),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Chuẩn hoá theo 7.000 W · trực tiếp",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp, fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.powerRows.forEachIndexed { i, row ->
                PowerRow(row = row, index = i, tick = tick)
            }
        }
    }
}

@Composable
private fun PowerRow(
    row: com.smarthome.hume.core.model.EnergyPowerRow,
    index: Int,
    tick: Int,
) {
    val extra = LocalHumeExtraColors.current
    val label = when (row.kind) {
        EnergyPowerKind.Battery -> "Pin"
        EnergyPowerKind.Solar -> "Điện mặt trời"
        EnergyPowerKind.Grid -> "Lưới"
        EnergyPowerKind.Home -> "Tiêu thụ"
    }
    val color = when (row.kind) {
        EnergyPowerKind.Battery -> Color(0xFFD97706)
        EnergyPowerKind.Solar -> MaterialTheme.colorScheme.primary
        EnergyPowerKind.Grid -> extra.info
        EnergyPowerKind.Home -> MaterialTheme.colorScheme.tertiary
    }
    // Jitter nhe moi tick cho giong demo (khong doi du lieu goc)
    val watts = row.watts * (0.94 + Random(tick * 97 + index * 13).nextDouble() * 0.12)
    val frac by animateFloatAsState(
        targetValue = (abs(watts) / 7000.0).coerceIn(0.0, 1.0).toFloat(),
        animationSpec = tween(800, easing = M3EMotion.emphasized),
        label = "pwFill",
    )
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold),
            )
            Text(
                "${if (watts < 0) "−" else ""}${abs(watts).roundToInt()} W",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold,
                ).tnum(),
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(frac)
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

// ---------- 4. Co cau tieu thu (donut) ----------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DonutCard(state: EnergyUiState, risePlayed: MutableSet<String>) {
    if (state.donut.isEmpty()) return
    val extra = LocalHumeExtraColors.current
    // 4 mau slice RIENG BIET: primary (theme) + 3 mau co dinh (xanh duong, ho phach, tim)
    // de tranh trung nhau nhu vu primary==tertiary o theme xanh la
    val sliceColors = listOf(
        MaterialTheme.colorScheme.primary,
        Color(0xFF5B8DEF),
        Color(0xFFE8A838),
        Color(0xFF9B7EDE),
    )
    // Legend entrance: tu phai sang, stagger (demo .dli)
    var legendShown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { legendShown = true }
    M3ECard(
        shape = RoundedCornerShape(40.dp),
        contentPadding = 20.dp,
        containerColor = LocalHumeExtraColors.current.surfaceHighest,
        modifier = Modifier.riseOnce("cons-donut", 480, risePlayed),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "Cơ cấu tiêu thụ",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 12.sp, fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        kwh1(state.donutTotalKwh),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.3).sp,
                        ).tnum(),
                    )
                    Text(
                        " kWh",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 13.sp, fontWeight = FontWeight.Bold),
                    )
                }
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
            ) {
                MsIcon(
                    Ms.donut_large, null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            EnergyDonut(
                total = state.donutTotalKwh,
                slices = state.donut,
                sliceColors = sliceColors,
                modifier = Modifier.size(132.dp),
            )
            Spacer(Modifier.width(18.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                state.donut.forEachIndexed { i, s ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.staggerEnter(
                            index = i,
                            visible = legendShown,
                            baseDelayMs = 0,
                            staggerMs = 200,
                            fromX = 12f,
                            fromY = 0f,
                        ),
                    ) {
                        Box(
                            Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(sliceColors[i % sliceColors.size]),
                        )
                        Spacer(Modifier.width(10.dp))
                        // chu thich dai -> chay chu (marquee), 1 dong
                        Text(
                            s.name,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1f)
                                .basicMarquee(),
                        )
                        // cot % rong co dinh, can phai -> cac hang dóng thẳng
                        Text(
                            "${(s.fraction * 100).roundToInt()}%",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            ).tnum(),
                            maxLines = 1,
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(44.dp),
                        )
                    }
                }
            }
        }
    }
}

// ---------- 5. Thiet bi tieu thu ----------

@Composable
private fun DevicesCard(
    state: EnergyUiState,
    ui: EnergyScreenUi,
    vm: EnergyViewModel,
    risePlayed: MutableSet<String>,
    modifier: Modifier = Modifier,
) {
    // Live tick 2.8s (demo: jitter ±8% gia tri)
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2800)
            tick++
        }
    }
    // Doi che do: rows fade/slide ra (180ms) roi vao lai voi stagger 45ms
    var rowsVisible by remember { mutableStateOf(true) }
    var firstMode by remember { mutableStateOf(true) }
    LaunchedEffect(ui.deviceMode) {
        if (firstMode) {
            firstMode = false
            return@LaunchedEffect
        }
        rowsVisible = false
        delay(180)
        rowsVisible = true
    }
    // Top 5 theo che do (demo: sort desc + slice 0,5)
    val devs = remember(tick, ui.deviceMode, state.powerDevices, state.energyDevices) {
        val base = if (ui.deviceMode == DeviceMode.Power) state.powerDevices else state.energyDevices
        base.sortedByDescending { it.value }.take(5).mapIndexed { i, d ->
            d to (1.0 + (Random(tick * 131 + i * 17).nextDouble() - 0.5) * 0.16)
        }
    }
    M3ECard(
        shape = RoundedCornerShape(32.dp),
        contentPadding = 0.dp,
        containerColor = LocalHumeExtraColors.current.surfaceHighest,
        modifier = modifier
            .riseOnce("cons-dev", 440, risePlayed)
            .shadow(12.dp, RoundedCornerShape(32.dp)),
    ) {
        Column(
            modifier = Modifier.padding(
                top = 20.dp, bottom = 12.dp, start = 16.dp, end = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Thiết bị",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 17.sp, fontWeight = FontWeight.Bold),
                )
                Row(
                    modifier = Modifier
                        .width(190.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    DeviceMode.entries.forEach { m ->
                        val isSel = ui.deviceMode == m
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    if (isSel) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent,
                                )
                                .pressMorph(pressedScale = 0.94f) { vm.setDeviceMode(m) }
                                .padding(vertical = 8.dp, horizontal = 10.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AnimatedVisibility(
                                    visible = isSel,
                                    enter = expandHorizontally() + fadeIn(),
                                    exit = shrinkHorizontally() + fadeOut(),
                                ) {
                                    MsIcon(M3EIcons.Check, null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier
                                            .padding(end = 6.dp)
                                            .size(16.dp))
                                }
                                Text(
                                    if (m == DeviceMode.Power) "Công suất" else "Năng lượng",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    // demo .dvsegi{white-space:nowrap}: khong de vo 2 dong
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            if (devs.isEmpty()) {
                Text(
                    "Chưa có dữ liệu thiết bị",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            devs.forEachIndexed { i, (d, f) ->
                DeviceRow(d = d, mode = ui.deviceMode, factor = f,
                    visible = rowsVisible, index = i)
            }
        }
    }
}

@Composable
private fun DeviceRow(
    d: EnergyDevice,
    mode: DeviceMode,
    factor: Double,
    visible: Boolean,
    index: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .staggerEnter(index = index, visible = visible, staggerMs = 45),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                d.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 14.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (mode == DeviceMode.Power) "Vừa xong" else "Hôm nay",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        val v = d.value * factor
        Column(horizontalAlignment = Alignment.End) {
            val (num, unit) = if (mode == DeviceMode.Power) {
                if (v >= 1000) String.format(Locale.US, "%.1f", v / 1000) to "kW"
                else v.roundToInt().toString() to "W"
            } else {
                kwh1(v) to "kWh"
            }
            Text(
                num,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                ).tnum(),
                textAlign = TextAlign.End,
            )
            Text(
                unit,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        val cost = d.costVnd
        if (mode == DeviceMode.Energy && cost != null) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .widthIn(min = 64.dp),
            ) {
                Text(
                    vnd((cost * factor).toLong()),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A),
                    ).tnum(),
                    textAlign = TextAlign.End,
                )
                Text(
                    "VND",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

// ---------- 6. Pin thiet bi yeu ----------

private fun lowBattIcon(name: String): String {
    val n = name.lowercase()
    return when {
        "remote" in n -> Ms.remote_gen
        "khoá" in n || "khóa" in n || "khoa" in n -> Ms.lock
        "cảm biến" in n || "cam bien" in n || "pir" in n -> Ms.sensors
        "cửa" in n || "cua" in n -> Ms.door_front
        else -> M3EIcons.Battery
    }
}

@Composable
private fun LowBatteryCard(state: EnergyUiState, risePlayed: MutableSet<String>) {
    M3ECard(
        shape = RoundedCornerShape(32.dp),
        contentPadding = 0.dp,
        containerColor = LocalHumeExtraColors.current.surfaceHighest,
        modifier = Modifier.riseOnce("cons-blw", 500, risePlayed),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                "Pin thiết bị yếu",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 14.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            )
            if (state.lowBatteries.isEmpty()) {
                Text(
                    "Tất cả thiết bị đều đủ pin",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.lowBatteries.forEachIndexed { i, b ->
                if (i > 0) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    )
                }
                Row(
                    modifier = Modifier.padding(vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MsIcon(
                        lowBattIcon(b.name), null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        b.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((b.pct / 100).toFloat().coerceIn(0f, 1f))
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (b.pct <= 20) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary,
                                ),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "${b.pct.roundToInt()}%",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                        ).tnum(),
                        color = if (b.pct <= 20) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
