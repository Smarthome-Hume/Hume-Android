package com.smarthome.hume.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.smarthome.hume.core.model.AlarmUi
import com.smarthome.hume.core.model.BatteryUi
import com.smarthome.hume.core.model.HomeUiState
import com.smarthome.hume.core.model.SolarDay
import com.smarthome.hume.core.ui.components.BatteryLegend
import com.smarthome.hume.core.ui.components.M3ECard
import com.smarthome.hume.core.ui.components.M3EIcons
import com.smarthome.hume.core.ui.components.PillBarChart
import com.smarthome.hume.core.ui.components.WavyBatteryBar
import java.time.LocalTime

fun greeting(): String = when (LocalTime.now().hour) {
    in 5..10 -> "Chào buổi sáng"
    in 11..13 -> "Chào buổi trưa"
    in 14..18 -> "Chào buổi chiều"
    else -> "Chào buổi tối"
}

/** Header: loi chao + chip tim kiem + chuong thong bao. */
@Composable
fun HomeHeader(
    state: HomeUiState,
    onSearch: () -> Unit,
    onNotif: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        // Avatar 55px nhu demo .hava
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(55.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            androidx.compose.material3.Icon(
                M3EIcons.Person, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = greeting(),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val name = state.userName
            Text(
                text = if (name.isNotBlank()) name else if (state.connected) "Đã kết nối" else "Đang kết nối…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Chip tim kiem tron 46px
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .clickable(onClick = onSearch),
        ) {
            androidx.compose.material3.Icon(
                Icons.Outlined.Search, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(10.dp))
        // Chuong + badge (tam badge tren vien tron)
        BadgedBox(
            badge = {
                if (state.notifications.isNotEmpty()) {
                    Badge { Text("${state.notifications.size}") }
                }
            },
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .clickable(onClick = onNotif),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Icon(
                    Icons.Outlined.Notifications, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The goi y theo ngu canh (pin yeu / cua mo / ...). */
@Composable
fun SuggestCard(state: HomeUiState, modifier: Modifier = Modifier) {
    val tips = buildList {
        if (state.battery.soc in 1..29) add("Pin còn ${state.battery.soc}% — hạn chế tải nặng chờ nắng lên.")
        val doors = state.notifications.filter { it.title.contains("Cửa") }
        if (doors.isNotEmpty()) add("${doors.first().body} — bạn có muốn đóng lại không?")
        if (state.solarNowKw > 2.0) add("Đang nắng to (${"%.1f".format(state.solarNowKw)} kW) — tranh thủ chạy máy giặt.")
    }
    if (tips.isEmpty()) return
    M3ECard(modifier = modifier.fillMaxWidth()) {
        Text("Gợi ý", style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        Text(tips.first(), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The dien mat troi: bieu do pill 7 ngay. */
@Composable
fun SolarWeekCard(state: HomeUiState, modifier: Modifier = Modifier) {
    val week: List<SolarDay> = state.solarWeek
    M3ECard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Icon(
                M3EIcons.Solar, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("Điện mặt trời", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            state.solarTodayKwh?.let {
                Text("%.1f kWh".format(it), style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(Modifier.height(12.dp))
        if (week.isNotEmpty()) {
            PillBarChart(
                values = week.map { it.kwh },
                labels = week.map { it.label },
                valueLabel = { if (it >= 10) "${it.toInt()}" else "" },
            )
        } else {
            Text("Đang tải dữ liệu…", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** The nho: cong suat dang phat (chuyen tu subtab solar sang trang Nha). */
@Composable
fun SolarLiveCard(state: HomeUiState, modifier: Modifier = Modifier) {
    M3ECard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Đang phát", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Text("%.1f kW".format(state.solarNowKw),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** The hieu nang pin voi WavyBatteryBar. */
@Composable
fun BatteryCard(battery: BatteryUi, modifier: Modifier = Modifier) {
    M3ECard(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Icon(
                M3EIcons.Battery, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("Hiệu năng pin", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            Text("${battery.soc}%", style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.height(12.dp))
        WavyBatteryBar(soc = battery.soc, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        BatteryLegend(Modifier.fillMaxWidth().height(20.dp))
        val flow = when {
            battery.powerKw < -0.05 -> "Đang sạc %.1f kW".format(-battery.powerKw)
            battery.powerKw > 0.05 -> "Đang xả %.1f kW".format(battery.powerKw)
            else -> "Chờ"
        }
        Spacer(Modifier.height(8.dp))
        Row {
            Text(flow, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            battery.timeText?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
