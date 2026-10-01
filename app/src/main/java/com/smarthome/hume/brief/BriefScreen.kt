package com.smarthome.hume.brief

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import com.smarthome.hume.core.ui.components.M3ECard
import com.smarthome.hume.core.ui.components.M3EMotion
import com.smarthome.hume.core.ui.components.M3EPageBottomSpacing
import com.smarthome.hume.core.ui.components.M3ESectionLabel
import com.smarthome.hume.core.ui.components.pressMorph
import com.smarthome.hume.core.ui.components.MarqueeText
import com.smarthome.hume.core.ui.components.Ms
import com.smarthome.hume.core.ui.components.MsIcon
import com.smarthome.hume.core.ui.components.toSmartVndParts
import com.smarthome.hume.core.ui.components.toVnd

/** Trang Brief sang: mo bang vuot phai tren navbar, dong bang vuot phai->trai / nut back he thong. */
@Composable
fun BriefScreen(
    cache: BriefCache?,
    refreshing: Boolean,
    onRequestLocation: () -> Unit,
    onRefresh: (forceMonthly: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val daily = cache?.daily
    val monthly = cache?.monthly
    val monthlyLive = cache?.monthlyLive
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
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Brief", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, color = cs.onSurface)
                    Spacer(Modifier.width(8.dp))
                    AiPill()
                }
                Text(
                    when (tab) {
                        0 -> daily?.let { "${it.weekdayVi} · ${it.dateLabel}" } ?: "Chưa có dữ liệu"
                        else -> monthly?.let { "Tháng ${it.monthLabel}" }
                            ?: monthlyLive?.let { "Tháng ${it.monthLabel}" }
                            ?: "Chưa có dữ liệu"
                    },
                    style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant,
                )
            }
        }
        // ---- tabs + content ----
        // Box chung: tab co dinh ve SAU (tren) lop fade — fade chui xuong
        // duoi tab, khong che tab; noi dung scroll mo dan khi chui xuong
        // duoi. Giong scrim vung header trang Nha.
        val density = LocalDensity.current
        // Uoc luong chieu cao tab (~62dp) de frame dau khong nhay hinh;
        // onSizeChanged se hieu chinh ve gia tri thuc.
        var tabBarH by remember { mutableStateOf(62.dp) }
        // Fade cao hon tab 14dp = dung khoang cach chuan giua cac the:
        // the dau cach tab 14dp, nam ngay mep trong suot cua fade.
        // Gradient 1 lop DUY NHAT theo duong cong cosine muot.
        val fadeH = tabBarH + 14.dp
        val fadeStops = remember(cs.surface) {
            List(10) { i ->
                val t = i / 9f
                t to cs.surface.copy(alpha = cos(t * PI / 2).toFloat())
            }
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            if (refreshing && daily == null) {
                Box(Modifier.fillMaxSize().padding(top = tabBarH), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = cs.primary)
                }
            } else {
                // Chuyen tab: fade + truot ngang nhe theo chieu tab (M3E emphasized).
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        (fadeIn(tween(300, easing = M3EMotion.emphasized)) +
                            slideInHorizontally(tween(300, easing = M3EMotion.emphasized)) { dir * it / 6 }) togetherWith
                            (fadeOut(tween(220, easing = M3EMotion.emphasizedAcc)) +
                                slideOutHorizontally(tween(220, easing = M3EMotion.emphasizedAcc)) { -dir * it / 6 })
                    },
                    label = "briefTabContent",
                ) { t ->
                    // topPad = chieu cao tab + 14dp = khoang cach chuan giua
                    // cac the: tab -> the dau bang the -> the.
                    val topPad = tabBarH + 14.dp
                    if (t == 0) {
                        if (daily == null) BriefEmpty({ onRefresh(false) }, refreshing, Modifier.padding(top = tabBarH))
                        else DailyContent(daily, onRequestLocation, refreshing, tabKey = "day", topPad = topPad)
                    } else {
                        if (monthly == null && monthlyLive == null) BriefEmpty({ onRefresh(true) }, refreshing, Modifier.padding(top = tabBarH))
                        else MonthlyTab(monthly, monthlyLive, tabKey = "month", onCreateLive = { onRefresh(true) }, refreshing = refreshing, topPad = topPad)
                    }
                }
            }
            // Lop fade: 1 lop gradient duy nhat theo duong cong cosine —
            // dinh dac nam SAU tab (bi tab che), mo dan xuong duoi.
            // Tu trong suot -> mo o top lien mach nhu 1 lop.
            // Khong chan touch (khong clickable).
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(fadeH)
                    .background(Brush.verticalGradient(*fadeStops.toTypedArray())),
            )
            // Tab (connected button group M3E) ve SAU lop fade: fade nam duoi
            // tab, khong che tab.
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .onSizeChanged { tabBarH = with(density) { it.height.toDp() } }
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
        Text("AI", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = cs.onPrimaryContainer)
    }
}

@Composable
private fun BriefTab(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    // Giong he EsubGroup (tab Tieu thu/Dien mat troi): tab chon =
    // primaryContainer + onPrimaryContainer, doi mau tuc thi (khong animate
    // gay nhap nhay), khong shadow/vien mo; press morph khi nhan.
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(99.dp))
            .background(if (selected) cs.primaryContainer else Color.Transparent)
            .pressMorph(0.94f, onClick)
            .padding(vertical = 9.dp),
    ) {
        Text(
            text, style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) cs.onPrimaryContainer else cs.onSurfaceVariant,
        )
    }
}

@Composable
private fun BriefEmpty(onRefresh: () -> Unit, refreshing: Boolean, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        MsIcon(Ms.wb_sunny, contentDescription = null, tint = cs.onSurfaceVariant, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text("Chưa có bản brief", style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
        Spacer(Modifier.height(6.dp))
        Text(
            "Brief được tạo tự động lúc 6:00 sáng. Bạn cũng có thể tạo ngay bây giờ.",
            style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant,
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
            else Text("Tạo ngay", style = MaterialTheme.typography.titleSmall, color = cs.onPrimaryContainer)
        }
    }
}

// ---------------- daily ----------------

@Composable
private fun DailyContent(d: BriefDaily, onRequestLocation: () -> Unit, refreshing: Boolean, tabKey: Any, topPad: Dp) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            // Top = chieu cao tab (tab de len tren noi dung) + 14dp chuan;
            // day = M3EPageBottomSpacing de the cuoi cach navbar ~14dp.
            .padding(horizontal = 16.dp).padding(top = topPad, bottom = M3EPageBottomSpacing),
    ) {
        // AI nhan dinh
        StaggerCard(0, tabKey) {
            M3ECard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = cs.primaryContainer,
                contentPadding = 18.dp,
            ) {
                M3ESectionLabel("NHẬN ĐỊNH", light = true)
                Spacer(Modifier.height(8.dp))
                Text(d.aiInsight, style = MaterialTheme.typography.bodyMedium, lineHeight = 21.sp, color = cs.onPrimaryContainer)
            }
        }
        // Thoi tiet
        StaggerCard(1, tabKey) {
            BriefCard {
                M3ESectionLabel("THỜI TIẾT HÔM NAY")
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
                        Text(d.weather.conditionVi, style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                        Text(
                            "${d.weather.tempMin}° – ${d.weather.tempMax}°C · Độ ẩm ${d.weather.humidity}%",
                            style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant,
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("PV dự kiến", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                        Text(
                            "≈ ${countUpText(d.weather.pvEstimateKwh, 1, tabKey)} kWh",
                            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = cs.primary,
                        )
                    }
                }
                if (d.weather.hasLocation) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MsIcon(Ms.wb_sunny, contentDescription = null, tint = cs.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Theo vị trí của bạn · Met.no", style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
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
                            style = MaterialTheme.typography.labelSmall, color = cs.primary,
                        )
                    }
                }
            }
        }
        // Nang luong
        StaggerCard(2, tabKey) {
            BriefCard {
                M3ESectionLabel("NĂNG LƯỢNG HÔM QUA")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBox(
                        icon = Ms.bolt, label = "MUA EVN",
                        big = "${countUpText(d.gridImportKwh, 1, tabKey)}",
                        unit = "kWh",
                        sub = "≈ ${(d.gridCostVnd).toVnd()} gồm VAT",
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
                        "Lũy kế kỳ này: ${(d.billingCostVnd).toVnd()} · ${countUpText(d.billingKwh, 1, tabKey)} kWh",
                        style = MaterialTheme.typography.labelMedium, color = cs.onPrimaryContainer,
                        lineHeight = 18.sp,
                    )
                }
            }
        }
        // Top thiet bi
        StaggerCard(3, tabKey) {
            BriefCard {
                M3ESectionLabel("TOP TIÊU THỤ HÔM QUA")
                Spacer(Modifier.height(10.dp))
                val max = d.topDevices.maxOfOrNull { it.kwh } ?: 1.0
                d.topDevices.forEachIndexed { i, dev ->
                    DeviceRow(dev, max, i, tabKey)
                    if (i < d.topDevices.lastIndex) Spacer(Modifier.height(10.dp))
                }
                // Phan tieu thu chua co sensor do: hien ro thay vi gia vo top 3 la day du.
                if (d.unmeasuredKwh > 0.5) {
                    Spacer(Modifier.height(10.dp))
                    UnmeasuredRow(d.unmeasuredKwh, max, tabKey)
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
                        d.aiTip, style = MaterialTheme.typography.bodySmall, lineHeight = 20.sp,
                        color = cs.onSurface,
                    )
                }
            }
        }
    }
}

// ---------------- monthly ----------------

@Composable
private fun MonthlyTab(
    monthly: BriefMonthly?,
    monthlyLive: BriefMonthly?,
    tabKey: Any,
    onCreateLive: () -> Unit,
    refreshing: Boolean,
    topPad: Dp,
) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            // Top = chieu cao tab (tab de len tren noi dung) + 36dp: the dau
            // tien khong bi tab che; 28dp trong do la vung fade.
            .padding(top = topPad, bottom = M3EPageBottomSpacing),
    ) {
        monthly?.let { m ->
            MonthlySectionHeader("Tháng ${m.monthLabel}", "Đã chốt")
            MonthlyContent(m, tabKey = "${tabKey}_closed")
        }
        monthlyLive?.let { m ->
            MonthlySectionHeader("Tháng ${m.monthLabel}", "Đang chạy")
            MonthlyContent(m, tabKey = "${tabKey}_live")
        }
        // Nut tao/cap nhat: cach khoi the 14dp chuan.
        Spacer(Modifier.height(14.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(cs.primaryContainer)
                .clickable(onClick = onCreateLive)
                .padding(vertical = 12.dp),
        ) {
            if (refreshing) CircularProgressIndicator(color = cs.onPrimaryContainer, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            else Text(
                if (monthlyLive == null) "Tạo tổng hợp kỳ đang chạy" else "Cập nhật kỳ đang chạy",
                style = MaterialTheme.typography.titleSmall, color = cs.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun MonthlySectionHeader(month: String, status: String) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp).padding(top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("TỔNG HỢP $month".uppercase(), style = MaterialTheme.typography.titleSmall, color = cs.onSurface)
        Box(
            Modifier
                .clip(RoundedCornerShape(99.dp))
                .background(cs.secondaryContainer)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(status, style = MaterialTheme.typography.labelMedium, color = cs.onSecondaryContainer)
        }
    }
}

@Composable
private fun MonthlyContent(m: BriefMonthly, tabKey: Any) {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        StaggerCard(0, tabKey) {
            M3ECard(
                modifier = Modifier.fillMaxWidth(),
                containerColor = cs.primaryContainer,
                contentPadding = 18.dp,
            ) {
                Text(m.aiSummary, style = MaterialTheme.typography.bodyMedium, lineHeight = 21.sp, color = cs.onPrimaryContainer)
            }
        }
        StaggerCard(1, tabKey) {
            BriefCard {
                M3ESectionLabel("TỔNG QUAN THÁNG ${m.monthLabel}")
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.height(IntrinsicSize.Min),
                ) {
                    val (costBig, costUnit) = countUpDouble(m.costVnd.toDouble(), tabKey).toSmartVndParts()
                    StatBox(
                        icon = Ms.electric_meter, label = "TIỀN ĐIỆN DỰ KIẾN",
                        big = costBig,
                        unit = costUnit,
                        sub = "${countUpText(m.gridKwh, 1, tabKey)} kWh",
                        modifier = Modifier.weight(1f),
                    )
                    StatBox(
                        icon = Ms.solar_power, label = "PV CẢ THÁNG",
                        big = countUpText(m.pvKwh, 0, tabKey),
                        unit = "kWh",
                        sub = "≈ ${(m.savedVnd).toVnd()} tiết kiệm",
                        modifier = Modifier.weight(1f),
                    )
                }
                // Tien thuc te ca nha tieu thu trong ky (sensor.home_cost).
                if (m.homeCostVnd > 0) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(cs.primaryContainer)
                            .padding(12.dp),
                    ) {
                        MsIcon(Ms.home, contentDescription = null, tint = cs.onPrimaryContainer, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Thực tế cả nhà tiêu thụ: ≈ ${(m.homeCostVnd).toVnd()} · Tiết kiệm ${m.savingsPct}% nhờ PV",
                            style = MaterialTheme.typography.labelMedium, color = cs.onPrimaryContainer,
                            lineHeight = 18.sp,
                        )
                    }
                }
            }
        }
        StaggerCard(2, tabKey) {
            BriefCard {
                M3ESectionLabel("TẦNG TIÊU THỤ NHIỀU NHẤT")
                Spacer(Modifier.height(10.dp))
                val max = m.floors.maxOfOrNull { it.kwh } ?: 1.0
                m.floors.forEachIndexed { i, f ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(f.name, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = cs.onSurface, modifier = Modifier.width(64.dp))
                        AnimBar(
                            fraction = (f.kwh / max).toFloat(), tabKey = tabKey, height = 10.dp,
                            color = if (i == 0) Color(0xFFEF6C00) else cs.primary,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("${countUpText(f.kwh, 1, tabKey)} kWh", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = cs.onSurfaceVariant, modifier = Modifier.width(76.dp))
                    }
                    if (i < m.floors.lastIndex) Spacer(Modifier.height(10.dp))
                }
            }
        }
        StaggerCard(3, tabKey) {
            BriefCard {
                M3ESectionLabel("TOP THIẾT BỊ THÁNG")
                Spacer(Modifier.height(10.dp))
                val max = m.topDevices.maxOfOrNull { it.kwh } ?: 1.0
                m.topDevices.forEachIndexed { i, dev ->
                    DeviceRow(dev, max, i, tabKey)
                    if (i < m.topDevices.lastIndex) Spacer(Modifier.height(10.dp))
                }
                if (m.unmeasuredKwh > 1.0) {
                    Spacer(Modifier.height(10.dp))
                    UnmeasuredRow(m.unmeasuredKwh, max, tabKey)
                }
            }
        }
    }
}

// ---------------- pieces ----------------

@Composable
private fun BriefCard(content: @Composable ColumnScope.() -> Unit) {
    M3ECard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentPadding = 18.dp,
        elevation = 8.dp,
        content = content,
    )
}

@Composable
private fun StatBox(icon: String, label: String, big: String, unit: String, sub: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .background(cs.surfaceContainer)
            .padding(14.dp),
    ) {
        // Vung label co dinh 2 dong: the nao nhan cung cao bang nhau.
        Box(Modifier.height(30.dp), contentAlignment = Alignment.TopStart) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MsIcon(icon, contentDescription = null, tint = cs.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    label, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp,
                    color = cs.onSurfaceVariant, maxLines = 2, lineHeight = 14.sp,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        // Gia tri marquee khi tran khung, don vi giu co dinh (khong bi ep vo layout).
        // Nguyen tac: baseline don vi = baseline gia tri (alignByBaseline).
        Row {
            MarqueeText(
                text = big,
                fontSize = MaterialTheme.typography.titleLarge.fontSize,
                fontWeight = FontWeight.SemiBold,
                fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                color = cs.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .alignByBaseline(),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                unit, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant,
                modifier = Modifier.alignByBaseline(),
            )
        }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(4.dp))
        // Vung phu co dinh 2 dong de day 2 the can nhau.
        Box(Modifier.height(32.dp), contentAlignment = Alignment.TopStart) {
            Text(sub, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, maxLines = 2, lineHeight = 15.sp)
        }
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
            Text("${index + 1}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = cs.onSurfaceVariant)
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(dev.name, style = MaterialTheme.typography.titleSmall, color = cs.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text("${countUpText(dev.kwh, 1, tabKey)} kWh", style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant, maxLines = 1, softWrap = false)
            }
            Spacer(Modifier.height(6.dp))
            AnimBar(fraction = (dev.kwh / max).toFloat(), tabKey = tabKey, color = cs.primary)
        }
    }
}

/**
 * Dong "thiet bi khac (chua do)": hien phan tieu thu khong co sensor do duoc,
 * de user biet danh sach top chua day du (vd dieu hoa mat du lieu se roi vao day).
 */
@Composable
private fun UnmeasuredRow(kwh: Double, max: Double, tabKey: Any) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(cs.surfaceContainerHigh),
        ) {
            MsIcon(Ms.info, contentDescription = null, tint = cs.onSurfaceVariant, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(10.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(cs.surfaceContainer),
        ) {
            MsIcon(Ms.power, contentDescription = null, tint = cs.onSurfaceVariant, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Thiết bị khác", style = MaterialTheme.typography.titleSmall, color = cs.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Text("${countUpText(kwh, 1, tabKey)} kWh", style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant, maxLines = 1, softWrap = false)
            }
            Spacer(Modifier.height(6.dp))
            AnimBar(fraction = (kwh / max).toFloat(), tabKey = tabKey, color = cs.onSurfaceVariant)
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
private fun countUpDouble(target: Double, tabKey: Any): Double {
    var started by remember(tabKey, target) { mutableStateOf(false) }
    val p by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(900, easing = M3EMotion.emphasized), label = "countUp",
    )
    LaunchedEffect(tabKey, target) { started = true }
    return target * p
}

@Composable
private fun countUpText(target: Double, decimals: Int, tabKey: Any): String {
    val v = countUpDouble(target, tabKey)
    val pattern = "%.${decimals}f"
    return String.format(Locale("vi"), pattern, v)
}

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
