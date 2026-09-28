package com.smarthome.hume.feature.energy

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smarthome.hume.core.model.EnergyDevice
import com.smarthome.hume.core.model.EnergyPowerKind
import com.smarthome.hume.core.model.EnergyUiState
import com.smarthome.hume.core.ui.components.M3ECard
import com.smarthome.hume.core.ui.components.M3EIcons
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val vn = NumberFormat.getInstance(Locale("vi", "VN"))
private fun vnd(v: Long): String = vn.format(v)

/** Chu so nghin nho, giu so nguyen giong demo (18.450 VND). */
private fun kwh1(v: Double): String = String.format(Locale.US, "%.1f", v)

@Composable
fun EnergyConsTab(
    state: EnergyUiState,
    ui: EnergyScreenUi,
    vm: EnergyViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        WeekCard(state)
        CostCard(state)
        PowerCard(state)
        DonutCard(state)
        DevicesCard(state = state, ui = ui, vm = vm)
        LowBatteryCard(state)
    }
}

// ---------- 1. Nang luong su dung ----------

@Composable
private fun WeekCard(state: EnergyUiState) {
    M3ECard(shape = RoundedCornerShape(32.dp), contentPadding = 20.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column {
                Text("Năng lượng sử dụng", fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall)
                Text("7 ngày qua · cập nhật trực tiếp",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "${kwh1(state.todayKwh)} kWh",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val maxV = (state.week.maxOfOrNull { it.kwh } ?: 1.0).coerceAtLeast(0.1)
            state.week.forEach { p ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val frac = (p.kwh / maxV).coerceIn(0.04, 1.0)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((96 * frac).dp)
                                .clip(CircleShape)
                                .background(
                                    if (p.isToday) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primaryContainer,
                                ),
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            p.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (p.isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (p.isToday) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// ---------- 2. Chi phi dien ----------

@Composable
private fun CostCard(state: EnergyUiState) {
    M3ECard(shape = RoundedCornerShape(32.dp), contentPadding = 20.dp) {
        Text("Chi phí điện", fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat2(
                label = "Điện lưới",
                value = "${vnd(state.cost.gridVnd)} VND",
                modifier = Modifier.weight(1f),
            )
            Stat2(
                label = "Điện tiêu thụ",
                value = "${vnd(state.cost.homeVnd)} VND",
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
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.titleMedium)
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
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary)
    }
}

// ---------- 3. Cong suat hoat dong ----------

@Composable
private fun PowerCard(state: EnergyUiState) {
    M3ECard(shape = RoundedCornerShape(32.dp), contentPadding = 20.dp) {
        Text("Công suất hoạt động", fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall)
        Text("Chuẩn hoá theo 7.000 W · trực tiếp",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        state.powerRows.forEach { row ->
            val w = (row.watts / 7000.0).coerceIn(0.0, 1.0)
            val label = when (row.kind) {
                EnergyPowerKind.Battery -> "Pin"
                EnergyPowerKind.Solar -> "Điện mặt trời"
                EnergyPowerKind.Grid -> "Lưới"
                EnergyPowerKind.Home -> "Tiêu thụ"
            }
            val color = when (row.kind) {
                EnergyPowerKind.Battery -> Color(0xFFD97706)
                EnergyPowerKind.Solar -> MaterialTheme.colorScheme.primary
                EnergyPowerKind.Grid -> MaterialTheme.colorScheme.tertiary
                EnergyPowerKind.Home -> MaterialTheme.colorScheme.secondary
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(label, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold)
                Text(
                    "${if (row.watts < 0) "−" else ""}${abs(row.watts).roundToInt()} W",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
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
                        .fillMaxWidth(w.toFloat())
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(color),
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

// ---------- 4. Co cau tieu thu (donut) ----------

@Composable
private fun DonutCard(state: EnergyUiState) {
    if (state.donut.isEmpty()) return
    M3ECard(shape = RoundedCornerShape(32.dp), contentPadding = 20.dp) {
        Text("Cơ cấu tiêu thụ", fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            EnergyDonut(
                total = state.donutTotalKwh,
                slices = state.donut,
                modifier = Modifier.size(132.dp),
            )
            Spacer(Modifier.width(18.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val colors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.tertiary,
                    MaterialTheme.colorScheme.secondary,
                )
                state.donut.forEachIndexed { i, s ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(colors[i % colors.size]),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(s.label, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Text("${(s.fraction * 100).roundToInt()}%",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.bodyMedium)
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
    modifier: Modifier = Modifier,
) {
    M3ECard(
        shape = RoundedCornerShape(32.dp),
        contentPadding = 20.dp,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Thiết bị tiêu thụ", fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(4.dp),
            ) {
                DeviceMode.entries.forEach { m ->
                    val isSel = ui.deviceMode == m
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (isSel) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent,
                            )
                            .clickable { vm.setDeviceMode(m) }
                            .padding(vertical = 8.dp, horizontal = 10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isSel) {
                                Icon(M3EIcons.Check, null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier
                                        .padding(end = 6.dp)
                                        .size(16.dp))
                            }
                            Text(
                                if (m == DeviceMode.Power) "Công suất" else "Năng lượng",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        val list = if (ui.deviceMode == DeviceMode.Power) state.powerDevices else state.energyDevices
        if (list.isEmpty()) {
            Text("Chưa có dữ liệu thiết bị",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp))
        }
        list.forEach { d -> DeviceRow(d, ui.deviceMode) }
    }
}

@Composable
private fun DeviceRow(d: EnergyDevice, mode: DeviceMode) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(d.name, style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1)
            Text(if (mode == DeviceMode.Power) d.sub else "Hôm nay",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            val (num, unit) = if (mode == DeviceMode.Power) {
                if (d.value >= 1000) String.format("%.1f", d.value / 1000) to "kW"
                else d.value.roundToInt().toString() to "W"
            } else {
                kwh1(d.value) to "kWh"
            }
            Text(num, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary)
            Text(unit, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (mode == DeviceMode.Energy && d.vnd != null) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(start = 12.dp),
            ) {
                Text(vnd(d.vnd), fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF16A34A))
                Text("VND", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ---------- 6. Pin thiet bi yeu ----------

@Composable
private fun LowBatteryCard(state: EnergyUiState) {
    M3ECard(shape = RoundedCornerShape(32.dp), contentPadding = 20.dp) {
        Text("Pin thiết bị yếu", fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(vertical = 12.dp))
        if (state.lowBatteries.isEmpty()) {
            Text("Tất cả thiết bị đều đủ pin",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        state.lowBatteries.forEachIndexed { i, b ->
            if (i > 0) {
                Spacer(Modifier.height(11.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(M3EIcons.Battery, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(12.dp))
                Text(b.name, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f))
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
                Text("${b.pct.roundToInt()}%",
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (b.pct <= 20) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
