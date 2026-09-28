package com.smarthome.hume.feature.security

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.smarthome.hume.core.model.RecordingUi
import com.smarthome.hume.core.model.SensorKind
import com.smarthome.hume.core.model.SensorUi
import com.smarthome.hume.core.ui.components.EsubGroup
import com.smarthome.hume.core.ui.components.M3ECard
import com.smarthome.hume.core.ui.components.M3EIcons
import com.smarthome.hume.core.ui.theme.HumeM3ETheme
import kotlinx.coroutines.delay

/**
 * Tab An ninh — port truc tiep demo v4 rev12 (#page-security):
 * chon camera, feed Frigate (khoa/mo), toolbar, clip gan day, grid Cua/Chuyen dong/Moi truong.
 */
@Composable
fun SecurityScreen(vm: SecurityViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val selectedCam by vm.selectedCam.collectAsState()
    val clip by vm.clip.collectAsState()

    HumeM3ETheme {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 96.dp),
            ) {
                Spacer(Modifier.height(20.dp))
                // Header
                Text(
                    "An ninh",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Camera & trạng thái bảo vệ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))

                // Camera picker (esub)
                if (state.cameras.isNotEmpty()) {
                    EsubGroup(
                        items = state.cameras.map { it.name },
                        selectedIndex = selectedCam,
                        onSelect = vm::selectCamera,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                    val cam = state.cameras[selectedCam]
                    CameraCard(vm = vm, camKey = cam.key, camName = cam.name)
                    Spacer(Modifier.height(8.dp))

                    // Recent recordings
                    SecHeader(title = "Video ghi hình gần đây", action = "Tải 10 clip")
                    val recs = state.recordings[cam.key].orEmpty()
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(recs, key = { it.id }) { rec ->
                            RecCard(rec = rec, onClick = { vm.openClip(rec) })
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                // Sensor grids
                SecHeader(title = "Cửa")
                SensorGrid(sensors = state.doorSensors)
                SecHeader(title = "Chuyển động")
                SensorGrid(sensors = state.motionSensors)
                SecHeader(title = "Môi trường")
                SensorGrid(sensors = state.envSensors)
            }

            // Clip viewer overlay
            clip?.let { c ->
                ClipOverlay(
                    label = "${c.timeLabel} · ${c.dateLabel}",
                    onClose = vm::closeClip,
                )
            }
        }
    }
}

@Composable
private fun SecHeader(title: String, action: String? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 10.dp)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (action != null) {
            Text(
                action,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// ---------- camera ----------

@Composable
private fun CameraCard(vm: SecurityViewModel, camKey: String, camName: String) {
    val unlocked by vm.unlocked.collectAsState()
    val recording by vm.recording.collectAsState()
    var frame by remember(camKey) { mutableStateOf(0L) }
    var failed by remember(camKey) { mutableStateOf(false) }

    LaunchedEffect(camKey, unlocked) {
        if (!unlocked) return@LaunchedEffect
        while (true) {
            frame = System.currentTimeMillis()
            delay(3000)
        }
    }

    M3ECard(shape = RoundedCornerShape(32.dp), contentPadding = 12.dp) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.Black)
                    .clickable { vm.unlock() },
            ) {
                val context = LocalContext.current
                val url = vm.snapshotUrl(camKey)
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(if (unlocked) "$url?t=$frame" else url)
                        .memoryCachePolicy(if (unlocked) CachePolicy.DISABLED else CachePolicy.ENABLED)
                        .build(),
                    contentDescription = camName,
                    contentScale = ContentScale.Fit,
                    onSuccess = { failed = false },
                    onError = { failed = true },
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (unlocked) Modifier else Modifier.blur(16.dp)),
                )

                // LIVE badge
                Row(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE53935))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(Color.White))
                    Text("LIVE", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
                // name badge
                Text(
                    camName,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )

                if (failed) {
                    Text(
                        "Không lấy được hình từ Frigate",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else if (!unlocked) {
                    // unlock overlay
                    Column(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            M3EIcons.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(34.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Chạm để mở khoá",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                        )
                    }
                }
            }

            // toolbar
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                ToolbarBtn(
                    icon = M3EIcons.Rec,
                    label = "Ghi hình",
                    tint = if (recording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = vm::toggleRec,
                )
                ToolbarBtn(icon = M3EIcons.Mic, label = "Đàm thoại", onClick = {})
                ToolbarBtn(icon = M3EIcons.PhotoCamera, label = "Chụp ảnh", onClick = {})
                ToolbarBtn(icon = M3EIcons.Fullscreen, label = "Toàn màn hình", onClick = {})
            }
        }
    }
}

@Composable
private fun ToolbarBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit,
) {
    Icon(
        icon,
        contentDescription = label,
        tint = tint,
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(12.dp),
    )
}

// ---------- recordings ----------

@Composable
private fun RecCard(rec: RecordingUi, onClick: () -> Unit) {
    Column(
        Modifier
            .width(132.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(150f / 86f)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                M3EIcons.PlayCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(34.dp),
            )
        }
        Text(
            "${rec.timeLabel} · ${rec.dateLabel}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 7.dp),
        )
    }
}

// ---------- sensors ----------

@Composable
private fun SensorGrid(sensors: List<SensorUi>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        sensors.chunked(2).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { s ->
                    SensorCard(s, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SensorCard(s: SensorUi, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val on = s.isOn
    val bg = when {
        on && s.warn -> cs.tertiaryContainer
        on -> cs.errorContainer
        else -> cs.surfaceContainerHighest
    }
    val iconBg = when {
        on && s.warn -> cs.tertiaryContainer
        on -> Color.White.copy(alpha = 0.55f)
        else -> cs.surfaceContainer
    }
    val iconTint = when {
        on && s.warn -> cs.onTertiaryContainer
        on -> cs.onErrorContainer
        else -> cs.onSurfaceVariant
    }
    val nameColor = when {
        on && s.warn -> cs.onTertiaryContainer
        on -> cs.onErrorContainer
        else -> cs.onSurface
    }
    val chipBg = when {
        on && s.warn -> cs.tertiary
        on -> cs.error
        else -> cs.surfaceContainer
    }
    val chipText = when {
        on -> Color.White
        else -> cs.onSurfaceVariant
    }
    val chipLabel = when (s.kind) {
        SensorKind.Door -> if (on) "MỞ" else "ĐÓNG"
        SensorKind.Motion -> if (on) "PHÁT HIỆN" else "TRỐNG"
        else -> if (on) "BÁO ĐỘNG" else if (s.iconKey == "smoke") "Bình thường" else "An toàn"
    }
    val icon = when (s.iconKey) {
        "door" -> M3EIcons.Door
        "motion" -> M3EIcons.Motion
        "presence" -> M3EIcons.Presence
        "smoke" -> M3EIcons.Smoke
        else -> M3EIcons.Leak
    }

    Column(
        modifier
            .clip(RoundedCornerShape(26.dp))
            .background(bg)
            .padding(14.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
            if (on) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(cs.error))
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    s.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = nameColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    s.lastChange,
                    fontSize = 11.sp,
                    color = if (on) nameColor.copy(alpha = 0.75f) else cs.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                chipLabel,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp,
                color = chipText,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(chipBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

// ---------- clip overlay ----------

@Composable
private fun ClipOverlay(label: String, onClose: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(0.88f),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1A1A1A)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    M3EIcons.Videocam,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(72.dp),
                )
            }
            Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(
                "Đóng",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.14f))
                    .padding(horizontal = 24.dp, vertical = 10.dp),
            )
        }
    }
}
