package com.smarthome.hume.feature.energy

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
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
import com.smarthome.hume.core.ui.components.MarqueeText
import com.smarthome.hume.core.ui.components.toSmartVndParts
import com.smarthome.hume.core.ui.components.HorizontalBatteryIcon
import com.smarthome.hume.core.ui.components.M3EMotion
import com.smarthome.hume.core.ui.components.tnum
import com.smarthome.hume.core.ui.components.toSmartPowerParts
import com.smarthome.hume.core.ui.components.toVnd
import com.smarthome.hume.core.ui.components.WeekChartD
import com.smarthome.hume.core.ui.components.pressMorph
import com.smarthome.hume.core.ui.components.Ms
import com.smarthome.hume.core.ui.components.MsIcon
import com.smarthome.hume.core.ui.theme.LocalHumeExtraColors
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.delay

/** Chu so nghin nho, giu so nguyen giong demo (18.450 VND). */
private fun kwh1(v: Double): String = String.format(Locale.US, "%.1f", v)

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

// ---------- 1. Nang luong su dung (layout giong SolarWeekCard) ----------

@Composable
private fun WeekCard(state: EnergyUiState, risePlayed: MutableSet<String>) {
    val cs = MaterialTheme.colorScheme
    val todayVal = state.week.find { it.isToday }?.kwh ?: state.todayKwh

    M3ECard(
        shape = RoundedCornerShape(32.dp),
        contentPadding = 20.dp,
        containerColor = LocalHumeExtraColors.current.surfaceHighest,
        modifier = Modifier.riseOnce("cons-week", 420, risePlayed),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Năng lượng sử dụng",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                kwh1(todayVal),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp,
                color = cs.onSurface,
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                "kWh",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .alignByBaseline(),
            )
        }
        Spacer(Modifier.height(14.dp))
        val vals = state.week.map { if (it.isToday) todayVal.toFloat() else it.kwh.toFloat() }
        val labels = state.week.map { it.label }
        if (vals.isNotEmpty()) {
            WeekChartD(vals = vals, labels = labels)
        } else {
            Text(
                "Đang tải dữ liệu…",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
            )
        }
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
            style = MaterialTheme.typography.titleSmall.copy(),
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat2(
                label = "Điện lưới",
                vnd = state.cost.gridVnd + gridJit,
                modifier = Modifier.weight(1f),
            )
            Stat2(
                label = "Điện tiêu thụ",
                vnd = state.cost.homeVnd + homeJit,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PBox("Giá mua", (state.cost.buyPrice).toVnd(), Modifier.weight(1f))
            PBox("Giá EVN", (state.cost.evnPrice).toVnd(), Modifier.weight(1f))
            PBox("Tiết kiệm", "${state.cost.savedVnd.toVnd()}đ", Modifier.weight(1f))
        }
    }
}

@Composable
private fun Stat2(label: String, vnd: Long, modifier: Modifier = Modifier) {
    val (big, unit) = vnd.toSmartVndParts()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(14.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Gia tri marquee khi tran khung, don vi giu co dinh (khong bi ep vo layout).
        // Nguyen tac: baseline don vi = baseline gia tri (alignByBaseline, khong dung Alignment.Bottom).
        Row(
            modifier = Modifier.padding(top = 4.dp),
        ) {
            MarqueeText(
                text = big,
                fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                fontWeight = FontWeight.SemiBold,
                fontFamily = MaterialTheme.typography.headlineSmall.fontFamily,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .alignByBaseline(),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                unit,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alignByBaseline(),
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
            style = MaterialTheme.typography.labelSmall.copy( fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            ).tnum(),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

// ---------- 3. Cong suat hoat dong ----------

@Composable
private fun PowerCard(state: EnergyUiState, risePlayed: MutableSet<String>) {
    M3ECard(
        shape = RoundedCornerShape(32.dp),
        contentPadding = 20.dp,
        containerColor = LocalHumeExtraColors.current.surfaceHighest,
        modifier = Modifier.riseOnce("cons-power", 460, risePlayed),
    ) {
        Text(
            "Công suất hoạt động",
            style = MaterialTheme.typography.titleSmall.copy(),
        )
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.powerRows.forEachIndexed { i, row ->
                PowerRow(row = row, index = i)
            }
        }
    }
}

@Composable
private fun PowerRow(
    row: com.smarthome.hume.core.model.EnergyPowerRow,
    index: Int,
) {
    val label = when (row.kind) {
        EnergyPowerKind.Battery -> "Pin"
        EnergyPowerKind.Solar -> "Điện mặt trời"
        EnergyPowerKind.Grid -> "Lưới"
        EnergyPowerKind.Home -> "Tiêu thụ"
    }
    // Scale co dinh theo tung hang (user yeu cau)
    val maxScale = when (row.kind) {
        EnergyPowerKind.Battery -> 4000.0
        EnergyPowerKind.Solar -> 5500.0
        EnergyPowerKind.Grid -> 9000.0
        EnergyPowerKind.Home -> 9000.0
    }
    val color = when (row.kind) {
        EnergyPowerKind.Battery -> Color(0xFFD97706) // cam
        EnergyPowerKind.Solar -> MaterialTheme.colorScheme.primary // xanh duong
        EnergyPowerKind.Grid -> Color(0xFF9AA5B1) // xam
        EnergyPowerKind.Home -> MaterialTheme.colorScheme.primary
    }
    // Gia tri that tu HA (khong jitter demo)
    val watts = row.watts
    val frac by animateFloatAsState(
        targetValue = (abs(watts) / maxScale).coerceIn(0.0, 1.0).toFloat(),
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
                style = MaterialTheme.typography.labelMedium.copy(),
            )
            Text(
                "${if (watts < 0) "−" else ""}${abs(watts).roundToInt()} W",
                style = MaterialTheme.typography.labelMedium.copy( fontWeight = FontWeight.SemiBold,
                ).tnum(),
            )
        }
        Spacer(Modifier.height(6.dp))
        // Flat bar co animation chay theo cong suat
        FlatPowerBar(frac = frac, color = color)
    }
}

/**
 * Thanh cong suat dang flat (phang), fill co animation theo gia tri.
 */
@Composable
private fun FlatPowerBar(
    frac: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(frac)
                .height(8.dp)
                .clip(CircleShape)
                .background(color),
        )
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
        Color(0xFFE8734A), // cam do (khac primary cua moi theme)
        Color(0xFF5B8DEF), // xanh duong
        Color(0xFFE8A838), // vang
        Color(0xFF9B7EDE), // tim
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
                    style = MaterialTheme.typography.bodySmall.copy(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row {
                    Text(
                        kwh1(state.donutTotalKwh),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.3).sp,
                        ).tnum(),
                        modifier = Modifier.alignByBaseline(),
                    )
                    Text(
                        " kWh",
                        style = MaterialTheme.typography.titleSmall.copy(),
                        modifier = Modifier.alignByBaseline(),
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
                        // Viewport marquee keo den sat mep cham mau (bo Spacer):
                        // padding nam TRONG marquee -> chu chay den mep cham moi an,
                        // luc nghi van cach 10.dp nhu cu.
                        Text(
                            s.name,
                            style = MaterialTheme.typography.labelMedium.copy(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1f)
                                .basicMarquee()
                                .padding(start = 10.dp),
                        )
                        // cot % rong co dinh, can phai -> cac hang dóng thẳng
                        Text(
                            "${(s.fraction * 100).roundToInt()}%",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
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
            .riseOnce("cons-dev", 440, risePlayed),
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
                    style = MaterialTheme.typography.titleMedium.copy(),
                )
                Row(
                    modifier = Modifier
                        .width(224.dp)
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
                                Text(
                                    if (m == DeviceMode.Power) "Công suất" else "Năng lượng",
                                    style = MaterialTheme.typography.labelMedium.copy(),
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
                style = MaterialTheme.typography.bodyMedium.copy(),
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
                v.toSmartPowerParts()
            } else {
                kwh1(v) to "kWh"
            }
            Text(
                num,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
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
                    (cost * factor).toLong().toVnd(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = LocalHumeExtraColors.current.success,
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
                style = MaterialTheme.typography.titleSmall.copy(),
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
                    // Icon pin nam ngang ve tay (dong nhat cac vi tri khac, 2026-09-30).
                    HorizontalBatteryIcon(
                        soc = b.pct.roundToInt(),
                        color = if (b.pct <= 20) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(width = 28.dp, height = 17.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        b.name,
                        style = MaterialTheme.typography.labelLarge.copy(),
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
                        style = MaterialTheme.typography.titleSmall.copy( fontWeight = FontWeight.SemiBold,
                        ).tnum(),
                        color = if (b.pct <= 20) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
