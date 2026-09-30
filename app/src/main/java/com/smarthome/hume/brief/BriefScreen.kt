package com.smarthome.hume.brief

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.ui.components.M3EMotion
import com.smarthome.hume.core.ui.components.Ms
import com.smarthome.hume.core.ui.components.MsIcon
import java.util.Locale

/** Trang Brief sang: mo bang vuot canh trai, dong bang vuot phai->trai / nut back. */
@Composable
fun BriefScreen(
    cache: BriefCache?,
    refreshing: Boolean,
    onClose: () -> Unit,
    onRequestLocation: () -> Unit,
    onRefresh: (forceMonthly: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val daily = cache?.daily
    val monthly = cache?.monthly
    var tab by remember { mutableStateOf(0) }

    Column(
        modifier
            .fillMaxSize()
            .background(cs.surface),
    ) {
        // ---- header ----
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
                    start = 16.dp, end = 16.dp, bottom = 4.dp,
                ),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(21.dp))
                    .background(cs.surfaceContainer)
                    .clickable(onClick = onClose),
            ) {
                MsIcon(Ms.arrow_back, contentDescription = "Đóng", modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Brief sáng", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = cs.onSurface)
                    Spacer(Modifier.width(8.dp))
                    AiPill()
                }
                Text(
                    when (tab) {
                        0 -> daily?.let { "${it.weekdayVi} · ${it.dateLabel} · Cập nhật 6:00" } ?: "Chưa có dữ liệu"
                        else -> monthly?.let { "Tháng ${it.monthLabel} · Cập nhật 6:00 ngày 1" } ?: "Chưa có dữ liệu"
                    },
                    fontSize = 11.sp, fontWeight = FontWeight.Medium, color = cs.onSurfaceVariant,
                )
            }
        }
        // ---- tabs (connected button group M3E) ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(cs.surfaceContainer)
                .padding(4.dp),
        ) {
            BriefTab("Hôm qua", selected = tab == 0, onClick = { tab = 0 }, modifier = Modifier.weight(1f))
            BriefTab(
                "Tháng ${monthly?.monthLabel?.substringBefore('/') ?: ""}".trim(),
                selected = tab == 1, onClick = { tab = 1 }, modifier = Modifier.weight(1f),
            )
        }
        // ---- content ----
        if (refreshing && daily == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = cs.primary)
            }
        } else if (tab == 0) {
            if (daily == null) BriefEmpty({ onRefresh(false) }, refreshing)
            else DailyContent(daily, onRequestLocation, refreshing, tabKey = "day")
        } else {
            if (monthly == null) BriefEmpty({ onRefresh(true) }, refreshing)
            else MonthlyContent(monthly, tabKey = "month")
        }
    }
}

@Composable
private fun AiPill() {
    val cs = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(cs.primaryContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        MsIcon(Ms.auto_awesome, contentDescription = null, tint = cs.onPrimaryContainer, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text("AI", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = cs.onPrimaryContainer)
    }
}

@Composable
private fun BriefTab(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(99.dp))
            .then(if (selected) Modifier.shadow(8.dp, RoundedCornerShape(99.dp)) else Modifier)
            .background(if (selected) cs.surfaceContainerHigh else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
    ) {
        Text(
            text, fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) cs.onSurface else cs.onSurfaceVariant,
        )
    }
}

@Composable
private fun BriefEmpty(onRefresh: () -> Unit, refreshing: Boolean) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        MsIcon(Ms.wb_sunny, contentDescription = null, tint = cs.onSurfaceVariant, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text("Chưa có bản brief", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = cs.onSurface)
        Spacer(Modifier.height(6.dp))
        Text(
            "Brief được tạo tự động lúc 6:00 sáng. Bạn cũng có thể tạo ngay bây giờ.",
            fontSize = 12.sp, color = cs.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(99.dp))
                .background(cs.primaryContainer)
                .clickable(onClick = onRefresh)
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            if (refreshing) CircularProgressIndicator(color = cs.onPrimaryContainer, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            else Text("Tạo ngay", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = cs.onPrimaryContainer)
        }
    }
}

// ---------------- daily ----------------

@Composable
private fun DailyContent(d: BriefDaily, onRequestLocation: () -> Unit, refreshing: Boolean, tabKey: Any) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        // AI nhan dinh
        StaggerCard(0, tabKey) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(cs.primaryContainer)
                    .padding(18.dp),
            ) {
                SecTitle("AI NHẬN ĐỊNH", light = true)
                Spacer(Modifier.height(8.dp))
                Text(d.aiInsight, fontSize = 13.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium, color = cs.onPrimaryContainer)
            }
        }
        // Thoi tiet
        StaggerCard(1, tabKey) {
            BriefCard {
                SecTitle("THỜI TIẾT HÔM NAY")
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (icon, tint) = when (d.weather.kind) {
                        "sunny" -> Ms.wb_sunny to Color(0xFFF9A825)
                        "rain" -> Ms.water_drop to Color(0xFF1565C0)
                        else -> Ms.wb_sunny to cs.onSurfaceVariant
                    }
                    MsIcon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(d.weather.conditionVi, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = cs.onSurface)
                        Text(
                            "${d.weather.tempMin}° – ${d.weather.tempMax}°C · Độ ẩm ${d.weather.humidity}%",
                            fontSize = 11.sp, fontWeight = FontWeight.Medium, color = cs.onSurfaceVariant,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("PV dự kiến", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = cs.onSurfaceVariant)
                        Text(
                            "≈ ${countUpText(d.weather.pvEstimateKwh, 1, tabKey)} kWh",
                            fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = cs.primary,
                        )
                    }
                }
                if (d.weather.hasLocation) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MsIcon(Ms.wb_sunny, contentDescription = null, tint = cs.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Theo vị trí của bạn · Met.no", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = cs.onSurfaceVariant)
                    }
                } else {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(cs.surfaceContainer)
                            .clickable(enabled = !refreshing, onClick = onRequestLocation)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            if (refreshing) "Đang lấy thời tiết…" else "Cho phép vị trí để lấy thời tiết chính xác",
                            fontSize = 11.sp, fontWeight = FontWeight.Bold, color = cs.primary,
                        )
                    }
                }
            }
        }
        // Nang luong
        StaggerCard(2, tabKey) {
            BriefCard {
                SecTitle("NĂNG LƯỢNG HÔM QUA")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBox(
                        icon = Ms.bolt, label = "MUA EVN",
                        big = "${countUpText(d.gridImportKwh, 1, tabKey)}",
                        unit = "kWh",
                        sub = "≈ ${fmtVnd(d.gridCostVnd)} gồm VAT",
                        modifier = Modifier.weight(1f),
                    )
                    StatBox(
                        icon = Ms.solar_power, label = "PV SẢN XUẤT",
                        big = "${countUpText(d.pvKwh, 1, tabKey)}",
                        unit = "kWh",
                        sub = "Tự chủ ${d.selfSufficiencyPct}%",
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(cs.primaryContainer)
                        .padding(12.dp),
                ) {
                    MsIcon(Ms.electric_meter, contentDescription = null, tint = cs.onPrimaryContainer, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Lũy kế kỳ này: ${fmtVnd(d.billingCostVnd)} · ${countUpText(d.billingKwh, 1, tabKey)} kWh",
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = cs.onPrimaryContainer,
                        lineHeight = 18.sp,
                    )
                }
            }
        }
        // Top thiet bi
        StaggerCard(3, tabKey) {
            BriefCard {
                SecTitle("TOP TIÊU THỤ HÔM QUA")
                Spacer(Modifier.height(10.dp))
                val max = d.topDevices.maxOfOrNull { it.kwh } ?: 1.0
                d.topDevices.forEachIndexed { i, dev ->
                    DeviceRow(dev, max, i, tabKey)
                    if (i < d.topDevices.lastIndex) Spacer(Modifier.height(10.dp))
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(cs.surfaceContainer)
                        .padding(12.dp),
                ) {
                    MsIcon(Ms.lightbulb, contentDescription = null, tint = cs.primary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        d.aiTip, fontSize = 12.sp, lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium, color = cs.onSurface,
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

// ---------------- monthly ----------------

@Composable
private fun MonthlyContent(m: BriefMonthly, tabKey: Any) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        StaggerCard(0, tabKey) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(cs.primaryContainer)
                    .padding(18.dp),
            ) {
                SecTitle("AI TỔNG KẾT THÁNG", light = true)
                Spacer(Modifier.height(8.dp))
                Text(m.aiSummary, fontSize = 13.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium, color = cs.onPrimaryContainer)
            }
        }
        StaggerCard(1, tabKey) {
            BriefCard {
                SecTitle("TỔNG QUAN THÁNG ${m.monthLabel}")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBox(
                        icon = Ms.electric_meter, label = "TIỀN ĐIỆN DỰ KIẾN",
                        big = countUpText(m.costVnd / 1_000_000.0, 2, tabKey),
                        unit = "tr đ",
                        sub = "${countUpText(m.gridKwh, 1, tabKey)} kWh mua EVN",
                        modifier = Modifier.weight(1f),
                    )
                    StatBox(
                        icon = Ms.solar_power, label = "PV CẢ THÁNG",
                        big = countUpText(m.pvKwh, 0, tabKey),
                        unit = "kWh",
                        sub = "≈ ${fmtVnd(m.savedVnd)} tiết kiệm",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        StaggerCard(2, tabKey) {
            BriefCard {
                SecTitle("TẦNG TIÊU THỤ NHIỀU NHẤT")
                Spacer(Modifier.height(10.dp))
                val max = m.floors.maxOfOrNull { it.kwh } ?: 1.0
                m.floors.forEachIndexed { i, f ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(f.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = cs.onSurface, modifier = Modifier.width(64.dp))
                        AnimBar(
                            fraction = (f.kwh / max).toFloat(), tabKey = tabKey, height = 10.dp,
                            color = if (i == 0) Color(0xFFEF6C00) else cs.primary,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("${countUpText(f.kwh, 1, tabKey)} kWh", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = cs.onSurfaceVariant, modifier = Modifier.width(76.dp))
                    }
                    if (i < m.floors.lastIndex) Spacer(Modifier.height(10.dp))
                }
            }
        }
        StaggerCard(3, tabKey) {
            BriefCard {
                SecTitle("TOP THIẾT BỊ THÁNG")
                Spacer(Modifier.height(10.dp))
                val max = m.topDevices.maxOfOrNull { it.kwh } ?: 1.0
                m.topDevices.forEachIndexed { i, dev ->
                    DeviceRow(dev, max, i, tabKey)
                    if (i < m.topDevices.lastIndex) Spacer(Modifier.height(10.dp))
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

// ---------------- pieces ----------------

@Composable
private fun BriefCard(content: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(cs.surfaceContainerHigh)
            .padding(18.dp),
    ) { content() }
}

@Composable
private fun SecTitle(text: String, light: Boolean = false) {
    val cs = MaterialTheme.colorScheme
    Text(
        text, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.5.sp,
        color = if (light) cs.onPrimaryContainer else cs.onSurfaceVariant,
    )
}

@Composable
private fun StatBox(icon: String, label: String, big: String, unit: String, sub: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(cs.surfaceContainer)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MsIcon(icon, contentDescription = null, tint = cs.onSurfaceVariant, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = cs.onSurfaceVariant)
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(big, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = cs.onSurface)
            Spacer(Modifier.width(4.dp))
            Text(unit, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = cs.onSurfaceVariant, modifier = Modifier.padding(bottom = 2.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(sub, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = cs.onSurfaceVariant)
    }
}

@Composable
private fun DeviceRow(dev: BriefDeviceStat, max: Double, index: Int, tabKey: Any) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(cs.surfaceContainerHigh),
        ) {
            Text("${index + 1}", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = cs.onSurfaceVariant)
        }
        Spacer(Modifier.width(10.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(cs.surfaceContainer),
        ) {
            MsIcon(briefIcon(dev.iconKey), contentDescription = null, tint = cs.onSurfaceVariant, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(dev.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = cs.onSurface)
                Text("${countUpText(dev.kwh, 1, tabKey)} kWh", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = cs.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            AnimBar(fraction = (dev.kwh / max).toFloat(), tabKey = tabKey, color = cs.primary)
        }
    }
}

@Composable
private fun AnimBar(fraction: Float, tabKey: Any, color: Color, modifier: Modifier = Modifier, height: Dp = 6.dp) {
    val cs = MaterialTheme.colorScheme
    var started by remember(tabKey) { mutableStateOf(false) }
    val w by animateFloatAsState(
        targetValue = if (started) fraction.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(1000, easing = M3EMotion.emphasized), label = "animBar",
    )
    LaunchedEffect(tabKey) { started = true }
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(99.dp))
            .background(cs.surfaceContainer),
    ) {
        Box(
            Modifier
                .fillMaxWidth(w)
                .fillMaxHeight()
                .clip(RoundedCornerShape(99.dp))
                .background(color),
        )
    }
}

@Composable
private fun StaggerCard(index: Int, tabKey: Any, content: @Composable () -> Unit) {
    var visible by remember(tabKey) { mutableStateOf(false) }
    LaunchedEffect(tabKey) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            tween(380, delayMillis = index * 70, easing = M3EMotion.emphasized),
        ) +
            slideInVertically(
                tween(480, delayMillis = index * 70, easing = M3EMotion.emphasized),
            ) { it / 3 },
    ) {
        Column {
            content()
            Spacer(Modifier.height(14.dp))
        }
    }
}

/** Dem so chay len kieu M3E khi mo tab. */
@Composable
private fun countUpText(target: Double, decimals: Int, tabKey: Any): String {
    var started by remember(tabKey, target) { mutableStateOf(false) }
    val p by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(900, easing = M3EMotion.emphasized), label = "countUp",
    )
    LaunchedEffect(tabKey, target) { started = true }
    val pattern = "%.${decimals}f"
    return String.format(Locale("vi"), pattern, target * p)
}

private fun fmtVnd(v: Long): String =
    "%,d".format(Locale.US, v).replace(',', '.') + "đ"

private fun briefIcon(key: String): String = when (key) {
    "ac_unit" -> Ms.ac_unit
    "cooking" -> Ms.cooking
    "whatshot" -> Ms.whatshot
    "soup_kitchen" -> Ms.soup_kitchen
    "dishwasher" -> Ms.dishwasher
    "local_laundry_service" -> Ms.local_laundry_service
    "desk" -> Ms.desk
    "power" -> Ms.power
    else -> Ms.power
}
