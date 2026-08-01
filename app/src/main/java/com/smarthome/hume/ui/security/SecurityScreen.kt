package com.smarthome.hume.ui.security

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.DoorFront
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeShapes
import com.smarthome.hume.ui.theme.glassPill
import com.smarthome.hume.ui.theme.glassSurface
import kotlinx.coroutines.delay

/** FRIGATE in SecurityView.swift */
private const val FRIGATE = "http://192.168.102.64:5000"

private data class SecurityCamera(val key: String, val name: String)

/** CAMERAS in SecurityView.swift */
private val cameras = listOf(
    SecurityCamera("living", "Ph\u00f2ng kh\u00e1ch"),
    SecurityCamera("kitchen", "Ph\u00f2ng \u0103n"),
    SecurityCamera("outdoor", "Ngo\u00e0i tr\u1eddi"),
    SecurityCamera("server", "Ph\u00f2ng th\u1edd"),
)

/** SensorDef in SecurityView.swift. `kind` drives the status wording. */
private data class SensorDef(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val kind: SensorKind,
    val alert: Color? = null,
)

private enum class SensorKind { Door, Motion, Smoke, Leak }

/** DOOR_SENSORS */
private val doorSensors = listOf(
    SensorDef("binary_sensor.cam_bien_cua_kinh_contact", "C\u1eeda k\u00ednh", Icons.Rounded.DoorFront, SensorKind.Door),
    SensorDef("binary_sensor.cam_bien_cua_phong_ngu_chinh_contact", "C\u1eeda ph\u00f2ng ng\u1ee7 ch\u00ednh", Icons.Rounded.DoorFront, SensorKind.Door),
    SensorDef("binary_sensor.cam_bien_cua_phong_ngu_be_contact", "C\u1eeda ph\u00f2ng ng\u1ee7 b\u00e9", Icons.Rounded.DoorFront, SensorKind.Door),
    SensorDef("binary_sensor.cam_bien_cua_phong_tam_contact", "C\u1eeda ph\u00f2ng t\u1eafm", Icons.Rounded.DoorFront, SensorKind.Door),
    SensorDef("binary_sensor.cam_bien_cua_ban_cong_tt2_contact", "C\u1eeda ban c\u00f4ng T2", Icons.Rounded.DoorFront, SensorKind.Door),
    SensorDef("binary_sensor.cam_bien_ban_cong_t3_contact", "C\u1eeda ban c\u00f4ng T3", Icons.Rounded.DoorFront, SensorKind.Door),
)

/** MOTION_SENSORS */
private val motionSensors = listOf(
    SensorDef("binary_sensor.cam_bien_pir_t1_occupancy", "PIR T\u1ea7ng 1", Icons.Rounded.DirectionsWalk, SensorKind.Motion),
    SensorDef("binary_sensor.cam_bien_pir_t2_occupancy", "PIR T\u1ea7ng 2", Icons.Rounded.DirectionsWalk, SensorKind.Motion),
    SensorDef("binary_sensor.cam_bien_pir_t3_occupancy", "PIR T\u1ea7ng 3", Icons.Rounded.DirectionsWalk, SensorKind.Motion),
    SensorDef("binary_sensor.cam_bien_hien_dien_presence", "C\u1ea3m bi\u1ebfn hi\u1ec7n di\u1ec7n", Icons.Rounded.DirectionsWalk, SensorKind.Motion),
    SensorDef("binary_sensor.cam_bien_pir_phong_tho_occupancy", "PIR ph\u00f2ng th\u1edd", Icons.Rounded.DirectionsWalk, SensorKind.Motion),
    SensorDef("binary_sensor.cam_bien_tuong_t2_occupancy", "C\u1ea3m bi\u1ebfn t\u01b0\u1eddng T2", Icons.Rounded.DirectionsWalk, SensorKind.Motion),
)

/** ENV_SENSORS */
private val envSensors = listOf(
    SensorDef("binary_sensor.cam_bien_khoi_smoke", "Kh\u00f3i", Icons.Rounded.Whatshot, SensorKind.Smoke, Color(0xFFFF6D00)),
    SensorDef("binary_sensor.cam_bien_nuoc_water_leak", "R\u00f2 r\u1ec9 n\u01b0\u1edbc", Icons.Rounded.WaterDrop, SensorKind.Leak, Color(0xFF2196F3)),
)

/**
 * Security tab, laid out exactly like SecurityView.swift: a camera picker, one
 * glass group holding the camera, and one glass group holding the three sensor
 * sections. The alarm mode buttons are not on this screen in the original; they
 * live in the home header.
 */
@Composable
fun SecurityScreen(ha: HomeAssistantRepository) {
    val entities by ha.entities.collectAsState()
    var selected by remember { mutableStateOf(cameras.first().key) }
    val camera = cameras.firstOrNull { it.key == selected } ?: cameras.first()

    Column(
        Modifier
            .fillMaxSize()
            .background(HumeColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Text("An ninh", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = HumeColors.TextPrimary)
        Spacer(Modifier.height(12.dp))

        // .pickerStyle(.segmented) on a glass background
        Row(Modifier.fillMaxWidth().glassPill(22.dp).padding(4.dp)) {
            cameras.forEach { cam ->
                val active = cam.key == selected
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (active) HumeColors.Orange else Color.Transparent)
                        .clickable { selected = cam.key }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        cam.name,
                        fontSize = 12.sp,
                        maxLines = 1,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (active) Color.White else HumeColors.TextSecondary,
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        // GroupGlassContainer(cornerRadius: 37, innerPadding: 10)
        Box(Modifier.fillMaxWidth().glassSurface(radius = HumeShapes.Panel).padding(10.dp)) {
            CameraCard(camera)
        }
        Spacer(Modifier.height(14.dp))

        Column(
            Modifier.fillMaxWidth().glassSurface(radius = HumeShapes.Panel).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SensorSection("C\u1eeda", doorSensors, entities)
            SensorSection("Chuy\u1ec3n \u0111\u1ed9ng", motionSensors, entities)
            SensorSection("M\u00f4i tr\u01b0\u1eddng", envSensors, entities)
        }
        Spacer(Modifier.height(48.dp))
    }
}

/* ------------------------- camera ------------------------- */

/**
 * CameraView in SecurityView.swift. Locked state shows a single blurred
 * snapshot so nothing in frame is readable; dragging the pill up past 80dp
 * unlocks the live view, which re-fetches latest.jpg every 3 seconds. Switching
 * camera locks it again.
 */
@Composable
private fun CameraCard(camera: SecurityCamera) {
    var unlocked by remember(camera.key) { mutableStateOf(false) }
    var frame by remember(camera.key) { mutableStateOf(0L) }

    // Live polling, the Android side of SnapshotLoader.
    LaunchedEffect(camera.key, unlocked) {
        if (!unlocked) return@LaunchedEffect
        while (true) {
            frame = System.currentTimeMillis()
            delay(3000)
        }
    }

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(HumeShapes.Tile))) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black),
        ) {
            val context = LocalContext.current
            val url = FRIGATE + "/api/" + camera.key + "/latest.jpg"
            Crossfade(targetState = unlocked, label = "camera") { live ->
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(if (live) url + "?t=" + frame else url)
                        .memoryCachePolicy(if (live) CachePolicy.DISABLED else CachePolicy.ENABLED)
                        .build(),
                    contentDescription = camera.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (live) Modifier else Modifier.blur(22.dp)),
                )
            }

            if (!unlocked) {
                Icon(
                    Icons.Rounded.Videocam,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.align(Alignment.Center).size(26.dp),
                )
                // VerticalSlideToUnlockBar
                var dragged by remember(camera.key) { mutableStateOf(0f) }
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp)
                        .width(48.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .pointerInput(camera.key) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (dragged < -80f) unlocked = true
                                    dragged = 0f
                                },
                                onVerticalDrag = { _, amount -> dragged += amount },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.KeyboardArrowUp,
                        contentDescription = "Xem tr\u1ef1c ti\u1ebfp",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // Camera name pill, top leading
            Text(
                camera.name,
                fontSize = 12.sp,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )

            if (unlocked) {
                Row(
                    Modifier.align(Alignment.BottomStart).padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(HumeColors.Green))
                    Spacer(Modifier.width(6.dp))
                    Text("LIVE", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }
    }
}

/* ------------------------- sensors ------------------------- */

@Composable
private fun SensorSection(title: String, sensors: List<SensorDef>, entities: Map<String, HomeEntity>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = HumeColors.TextSecondary,
            modifier = Modifier
                .clip(RoundedCornerShape(HumeShapes.Element))
                .background(Color.White.copy(alpha = 0.55f))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
        sensors.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { sensor ->
                    Box(Modifier.weight(1f)) { BinarySensorCard(sensor, entities[sensor.id]) }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/**
 * BinarySensorCardView: 25dp glass card, 36dp icon circle, name over an ago
 * line, and a status chip whose wording depends on the sensor kind.
 */
@Composable
private fun BinarySensorCard(sensor: SensorDef, entity: HomeEntity?) {
    val isOn = entity?.isOn == true
    val accent = sensor.alert ?: HumeColors.Orange
    val minutes = entity?.minutesAgo()
    val status = when (sensor.kind) {
        SensorKind.Leak -> if (isOn) "R\u00d2 R\u1ec8" else "AN TO\u00c0N"
        SensorKind.Smoke -> if (isOn) "C\u00d3 KH\u00d3I" else "B\u00ccNH TH\u01af\u1edcNG"
        SensorKind.Motion -> if (isOn) "PH\u00c1T HI\u1ec6N" else "TR\u1ed0NG"
        SensorKind.Door -> if (isOn) "M\u1ede" else "\u0110\u00d3NG"
    }

    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .glassSurface(radius = HumeShapes.Tile)
            .background(if (isOn) accent.copy(alpha = 0.10f) else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isOn) accent.copy(alpha = 0.22f) else HumeColors.Background),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    sensor.icon,
                    contentDescription = null,
                    tint = if (isOn) accent else HumeColors.TextPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    sensor.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isOn) accent else HumeColors.TextPrimary,
                    maxLines = 1,
                )
                if (minutes != null) {
                    Text(agoLabel(minutes), fontSize = 9.sp, color = HumeColors.TextSecondary)
                }
            }
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                status,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isOn) accent else HumeColors.TextSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isOn) accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.55f))
                    .padding(horizontal = 10.dp, vertical = 2.dp),
            )
        }
    }
}

/** agoText() in SecurityView.swift */
private fun agoLabel(minutes: Int): String = when {
    minutes < 1 -> "V\u1eeba xong"
    minutes < 60 -> minutes.toString() + " ph\u00fat tr\u01b0\u1edbc"
    else -> (minutes / 60).toString() + " gi\u1edd tr\u01b0\u1edbc"
}
