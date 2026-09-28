package com.smarthome.hume.feature.me

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smarthome.hume.core.ui.components.M3EIcons
import com.smarthome.hume.core.ui.components.M3ESwitch
import com.smarthome.hume.core.ui.theme.HumeM3ETheme
import com.smarthome.hume.core.ui.theme.M3ESeed

/**
 * Tab Toi — port truc tiep demo v4 rev12 (#page-me):
 * the Dong bo (wavy loading), the Thong bao, Giao dien (dark mode + 8 seeds).
 */
@Composable
fun MeScreen(
    vm: MeViewModel = viewModel(),
    onViewCamera: () -> Unit = {},
) {
    val connected by vm.isConnected.collectAsState()
    val seed by vm.seed.collectAsState()
    val darkMode by vm.darkMode.collectAsState()

    HumeM3ETheme {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 96.dp),
        ) {
            Spacer(Modifier.height(20.dp))
            Text("Tôi", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Đồng bộ, thông báo & hệ thống",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SecTitle("Đồng bộ")
            SyncCard(connected = connected)

            SecTitle("Thông báo")
            NotifCard(onViewCamera = onViewCamera)

            SecTitle("Giao diện")
            DarkModeRow(darkMode = darkMode, onToggle = vm::setDarkMode)
            Text(
                "MÀU CHỦ ĐẠO",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.4.sp,
                modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 12.dp),
            )
            SeedRow(selected = seed, onSelect = vm::setSeed)
            Text(
                "Mỗi seed sinh ra cả dải tonal light/dark — primary family và neutrals (nền, viền, chữ) đều đổi theo đúng quy tắc M3.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                lineHeight = 20.sp,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp),
            )
        }
    }
}

@Composable
private fun SecTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 10.dp),
    )
}

// ---------- dong bo ----------

@Composable
private fun SyncCard(connected: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // morphloader: hinh vuong primary xoay/morph (don gian hoa)
            val t = rememberInfiniteTransition(label = "sync")
            val rot by t.animateFloat(0f, 360f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "rot")
            Canvas(Modifier.size(38.dp)) {
                rotate(rot) {
                    drawRoundRect(
                        color = primary,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                if (connected) "Đã đồng bộ Home Assistant" else "Đang đồng bộ Home Assistant…",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        WavyProgress(active = !connected)
    }
}

/**
 * Wavy loading bar (demo v4: .wtrack/.wfill):
 * song sin buoc 40px truot -40px/vong (tron 1 buoc song, lien mach),
 * chi hien song mau primary, khong nen track.
 */
@Composable
private fun WavyProgress(active: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    val t = rememberInfiniteTransition(label = "wave")
    val slide by t.animateFloat(
        0f, -40f,
        infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "slide",
    )
    val progress by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(2800, easing = LinearEasing)),
        label = "prog",
    )
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(10.dp),
    ) {
        val waveLen = 40.dp.toPx()
        val amp = 5.dp.toPx()
        val y0 = size.height / 2
        val path = Path().apply {
            moveTo(slide, y0)
            var x = slide
            var up = false
            while (x < size.width + waveLen) {
                // Q20 0 / T40 — gan dung song sin cua demo
                quadraticTo(x + waveLen / 4, y0 + if (up) -amp else amp, x + waveLen / 2, y0)
                x += waveLen / 2
                up = !up
            }
        }
        clipRect(right = size.width * if (active) progress else 1f) {
            drawPath(
                path = path,
                color = primary,
                style = Stroke(
                    width = 4.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                ),
            )
        }
    }
}

// ---------- thong bao ----------

@Composable
private fun NotifCard(onViewCamera: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                M3EIcons.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text("Phát hiện chuyển động", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                "Camera sân trước · 2 phút trước",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 3.dp, bottom = 12.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NButton(
                    icon = M3EIcons.Videocam,
                    text = "Xem camera",
                    primary = true,
                    onClick = onViewCamera,
                    modifier = Modifier.weight(1f),
                )
                NButton(
                    icon = M3EIcons.Close,
                    text = "Bỏ qua",
                    primary = false,
                    onClick = {},
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NButton(
    icon: ImageVector,
    text: String,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (primary) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceContainer,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ---------- giao dien ----------

@Composable
private fun DarkModeRow(darkMode: Boolean?, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("Chế độ tối", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                "Đảo tông có hệ thống",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        // darkMode null = theo he thong → hien thi theo he thong, bat toggle = dat thu cong
        M3ESwitch(
            checked = darkMode ?: androidx.compose.foundation.isSystemInDarkTheme(),
            onCheckedChange = onToggle,
        )
    }
}

private val seedColors = mapOf(
    M3ESeed.Cam to Color(0xFFF9784C),
    M3ESeed.Green to Color(0xFF478A4D),
    M3ESeed.Blue to Color(0xFF2F6EA3),
    M3ESeed.Violet to Color(0xFF6A54A6),
    M3ESeed.Red to Color(0xFFBE4130),
    M3ESeed.Pink to Color(0xFF9E3D6E),
    M3ESeed.Teal to Color(0xFF2A7F76),
    M3ESeed.Amber to Color(0xFF776000),
)

private val seedNames = mapOf(
    M3ESeed.Cam to "Cam Hume",
    M3ESeed.Green to "Xanh lá",
    M3ESeed.Blue to "Xanh dương",
    M3ESeed.Violet to "Tím",
    M3ESeed.Red to "Đỏ",
    M3ESeed.Pink to "Hồng",
    M3ESeed.Teal to "Ngọc",
    M3ESeed.Amber to "Vàng",
)

@Composable
private fun SeedRow(selected: M3ESeed, onSelect: (M3ESeed) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        M3ESeed.entries.forEach { s ->
            val color = seedColors[s] ?: Color.Gray
            val on = s == selected
            Box(
                Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(color)
                    .clickable { onSelect(s) }
                    .padding(4.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (on) {
                    Icon(
                        M3EIcons.Check,
                        contentDescription = seedNames[s],
                        tint = Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }
    }
}
