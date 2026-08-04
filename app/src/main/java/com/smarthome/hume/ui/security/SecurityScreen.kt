package com.smarthome.hume.ui.security

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.smarthome.hume.core.frigate.FrigateRecording
import com.smarthome.hume.core.frigate.FrigateStore
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.core.storage.HumeSettings
import com.smarthome.hume.core.storage.SettingsStore
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeShapes
import com.smarthome.hume.ui.theme.Ph
import com.smarthome.hume.ui.theme.glassSurface
import com.smarthome.hume.ui.theme.humeMarquee
import com.smarthome.hume.ui.theme.neonGlow
import com.smarthome.hume.ui.theme.neonGlowCircle
import com.smarthome.hume.ui.theme.rememberNeonBeat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** FRIGATE in SecurityView.swift */
private const val FRIGATE = "http://192.168.102.64:5000"

/** Floating nav pill plus gesture bar: nothing may scroll under it. */
private val NavBarRoom = 140.dp

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
    SensorDef("binary_sensor.cam_bien_cua_kinh_contact", "C\u1eeda k\u00ednh", Ph.Door, SensorKind.Door),
    SensorDef("binary_sensor.cam_bien_cua_phong_ngu_chinh_contact", "C\u1eeda ph\u00f2ng ng\u1ee7 ch\u00ednh", Ph.Door, SensorKind.Door),
    SensorDef("binary_sensor.cam_bien_cua_phong_ngu_be_contact", "C\u1eeda ph\u00f2ng ng\u1ee7 b\u00e9", Ph.Door, SensorKind.Door),
    SensorDef("binary_sensor.cam_bien_cua_phong_tam_contact", "C\u1eeda ph\u00f2ng t\u1eafm", Ph.Door, SensorKind.Door),
    SensorDef("binary_sensor.cam_bien_cua_ban_cong_tt2_contact", "C\u1eeda ban c\u00f4ng T2", Ph.Door, SensorKind.Door),
    SensorDef("binary_sensor.cam_bien_ban_cong_t3_contact", "C\u1eeda ban c\u00f4ng T3", Ph.Door, SensorKind.Door),
)

/** MOTION_SENSORS */
private val motionSensors = listOf(
    SensorDef("binary_sensor.cam_bien_pir_t1_occupancy", "PIR T\u1ea7ng 1", Ph.Walk, SensorKind.Motion),
    SensorDef("binary_sensor.cam_bien_pir_t2_occupancy", "PIR T\u1ea7ng 2", Ph.Walk, SensorKind.Motion),
    SensorDef("binary_sensor.cam_bien_pir_t3_occupancy", "PIR T\u1ea7ng 3", Ph.Walk, SensorKind.Motion),
    SensorDef("binary_sensor.cam_bien_hien_dien_presence", "C\u1ea3m bi\u1ebfn hi\u1ec7n di\u1ec7n", Ph.Walk, SensorKind.Motion),
    SensorDef("binary_sensor.cam_bien_pir_phong_tho_occupancy", "PIR ph\u00f2ng th\u1edd", Ph.Walk, SensorKind.Motion),
    SensorDef("binary_sensor.cam_bien_tuong_t2_occupancy", "C\u1ea3m bi\u1ebfn t\u01b0\u1eddng T2", Ph.Walk, SensorKind.Motion),
)

/** ENV_SENSORS */
private val envSensors = listOf(
    SensorDef("binary_sensor.cam_bien_khoi_smoke", "Kh\u00f3i", Ph.Fire, SensorKind.Smoke, Color(0xFFFF6D00)),
    SensorDef("binary_sensor.cam_bien_nuoc_water_leak", "R\u00f2 r\u1ec9 n\u01b0\u1edbc", Ph.Drop, SensorKind.Leak, Color(0xFF2196F3)),
)

/**
 * Security tab, laid out exactly like SecurityView.swift: a camera picker, one
 * glass group holding the camera plus its recorded clips, and one glass group
 * holding the three sensor sections.
 *
 * Icon dung Phosphor regular giong ban HTML, khong dung Material Rounded (dac).
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
            .padding(start = 16.dp, end = 16.dp, top = 12.dp),
    ) {
        Text("An ninh", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = HumeColors.TextPrimary)
        Spacer(Modifier.height(12.dp))

        /*
         * Picker camera dung DUNG mau cua subtab trong ban HTML:
         *   khung background var(--gray000) radius 25 padding 4 gap 4,
         *   muc dang chon background var(--gray00), chu LUON var(--gray1000).
         * Khong to cam, khong doi chu sang trang.
         */
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(25.dp))
                .background(HumeColors.Card)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            cameras.forEach { cam ->
                val active = cam.key == selected
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(21.dp))
                        .background(if (active) HumeColors.Gray00 else Color.Transparent)
                        .clickable { selected = cam.key }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        cam.name,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        color = HumeColors.Gray1000,
                        modifier = Modifier.fillMaxWidth().humeMarquee(),
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        // GroupGlassContainer(cornerRadius: 37, innerPadding: 10)
        Column(
            Modifier.fillMaxWidth().glassSurface(radius = HumeShapes.Panel).padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CameraCard(camera)
            FrigateEventsSection(camera)
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
        Spacer(Modifier.height(NavBarRoom))
    }
}

/* ------------------------- camera ------------------------- */

/**
 * CameraView in SecurityView.swift. Locked state shows a single blurred
 * snapshot; dragging the pill up past 80dp unlocks the live view, which
 * re-fetches latest.jpg every 3 seconds.
 *
 * Neu snapshot khong tai duoc thi hien han loi thay vi de khung den tron, de
 * lan sau con biet la loi mang chu khong phai loi giao dien.
 */
@Composable
private fun CameraCard(camera: SecurityCamera) {
    var unlocked by remember(camera.key) { mutableStateOf(false) }
    var frame by remember(camera.key) { mutableStateOf(0L) }
    var failed by remember(camera.key) { mutableStateOf(false) }

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
                    onSuccess = { failed = false },
                    onError = { failed = true },
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (live) Modifier else Modifier.blur(22.dp)),
                )
            }

            if (failed) {
                Column(
                    Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Ph.Warning,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        "Kh\u00f4ng l\u1ea5y \u0111\u01b0\u1ee3c h\u00ecnh t\u1eeb Frigate",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        FRIGATE + "/api/" + camera.key + "/latest.jpg",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.45f),
                        textAlign = TextAlign.Center,
                    )
                }
            } else if (!unlocked) {
                Icon(
                    Ph.VideoCamera,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.align(Alignment.Center).size(26.dp),
                )
            }

            if (!unlocked) {
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
                        Ph.CaretUp,
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
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )

            if (unlocked && !failed) {
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

/* ------------------------- frigate clips ------------------------- */

/**
 * FrigateEventsSection in SecurityView.swift. Clips are downloaded to app
 * storage first and played from file, exactly like the iOS build.
 */
@Composable
private fun FrigateEventsSection(camera: SecurityCamera) {
    val context = LocalContext.current
    val store = remember { FrigateStore.get(context) }
    val settingsStore = remember { SettingsStore(context) }
    val settings by settingsStore.settings.collectAsState(initial = HumeSettings())
    val byCamera by store.byCamera.collectAsState()
    val downloading by store.downloading.collectAsState()
    val errors by store.lastError.collectAsState()
    val scope = rememberCoroutineScope()

    val recordings = byCamera[camera.key].orEmpty()
    val busy = downloading.contains(camera.key)
    var playing by remember { mutableStateOf<FrigateRecording?>(null) }

    // .task(id: camera) — pull once per camera when nothing is cached yet.
    LaunchedEffect(camera.key, settings.haToken) {
        if (recordings.isEmpty() && settings.haToken.isNotBlank()) {
            store.refresh(camera.key, settings.haUrl, settings.haToken)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Ph.VideoCamera,
                contentDescription = null,
                tint = HumeColors.Orange,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Video ghi h\u00ecnh g\u1ea7n \u0111\u00e2y",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = HumeColors.TextPrimary,
                maxLines = 1,
                softWrap = false,
            )
            Spacer(Modifier.weight(1f))
            if (busy) {
                CircularProgressIndicator(
                    color = HumeColors.TextSecondary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp),
                )
            } else {
                Icon(
                    Ph.Download,
                    contentDescription = "T\u1ea3i video m\u1edbi nh\u1ea5t",
                    tint = HumeColors.Orange,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable {
                            scope.launch {
                                store.refresh(camera.key, settings.haUrl, settings.haToken)
                            }
                        },
                )
            }
        }

        when {
            busy && recordings.isEmpty() -> Row(
                Modifier.fillMaxWidth().padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    color = HumeColors.TextSecondary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "\u0110ang t\u1ea3i 10 video m\u1edbi nh\u1ea5t\u2026",
                    fontSize = 12.sp,
                    color = HumeColors.TextSecondary,
                )
            }

            recordings.isEmpty() -> Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Text(
                    "Ch\u01b0a c\u00f3 video offline",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = HumeColors.TextPrimary,
                )
                Text(
                    errors[camera.key]
                        ?: "Nh\u1ea5n n\u00fat t\u1ea3i \u0111\u1ec3 l\u1ea5y 10 video m\u1edbi nh\u1ea5t t\u1eeb Frigate v\u1ec1 m\u00e1y.",
                    fontSize = 11.sp,
                    color = HumeColors.TextSecondary,
                )
            }

            else -> {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    recordings.forEach { rec ->
                        RecordingThumb(rec, store) { playing = rec }
                    }
                }
                errors[camera.key]?.let {
                    Text(it, fontSize = 10.sp, color = HumeColors.Orange)
                }
            }
        }
    }

    playing?.let { rec ->
        ClipPlayerDialog(store.clipFile(rec).absolutePath) { playing = null }
    }
}

/** RecordingThumb: 150dp wide, 86dp image, 9sp label row, 10dp corners. */
@Composable
private fun RecordingThumb(
    rec: FrigateRecording,
    store: FrigateStore,
    onTap: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier
            .width(150.dp)
            .clip(shape)
            .background(HumeColors.FillTertiary)
            .clickable(onClick = onTap),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(86.dp)
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = store.thumbFile(rec),
                contentDescription = rec.label,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier.size(24.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Ph.Play,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (rec.label.isNotEmpty()) {
                Text(rec.label, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = HumeColors.Orange, maxLines = 1, softWrap = false)
            }
            Text(clipTime(rec), fontSize = 9.sp, color = HumeColors.TextSecondary, maxLines = 1, softWrap = false)
        }
    }
}

/** DateFormatter "HH:mm dd/MM" */
private fun clipTime(rec: FrigateRecording): String {
    if (rec.startTime <= 0.0) return rec.label
    val format = SimpleDateFormat("HH:mm dd/MM", Locale("vi"))
    return format.format(Date((rec.startTime * 1000).toLong()))
}

/** VideoPlayer(player:) sheet — plays the downloaded file full screen. */
@Composable
private fun ClipPlayerDialog(path: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    android.widget.VideoView(context).apply {
                        setVideoPath(path)
                        setOnPreparedListener { player ->
                            player.isLooping = true
                            start()
                        }
                    }
                },
            )
            Text(
                "\u0110\u00f3ng",
                fontSize = 13.sp,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(18.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.18f))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

/* ------------------------- sensors ------------------------- */

/**
 * SK trong ban HTML:
 *   tieu de {fontSize:14, fontWeight:600, color:var(--gray1000), marginBottom:8,
 *   paddingLeft:4} — KHONG co nen chip; luoi 2 cot gap 6.
 */
@Composable
private fun SensorSection(title: String, sensors: List<SensorDef>, entities: Map<String, HomeEntity>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = HumeColors.Gray1000,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(start = 4.dp),
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
 * xK trong ban HTML — sao y tung so do, khong tu sang tac:
 *   borderRadius 25, padding '8px 12px', minHeight 44, flex column gap 2,
 *   background  = on ? accent 12 (7%)  : rgba(255,255,255,0.08),
 *   border 1px  = on ? accent 55 (33%) : rgba(255,255,255,0.1),
 *   boxShadow   = on ? '0 0 16px accent44' (vet sang NGOAI vien, tinh),
 *   vong icon 36 (icon 18), ten 13/500, dong phu 9 gray500 opacity .7,
 *   chip trang thai alignSelf CENTER, 10/600, padding '2px 10px', radius 8.
 *
 * Den neon = chấm tron 14 o goc phai tren (top -4, right -4) nhay theo
 * keyframes neon-blink 1.5s (opacity 1 -> .3 -> 1), co quang lan ra ngoai.
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

    val dark = HumeColors.isDark
    val shape = RoundedCornerShape(25.dp)
    val bg = if (isOn) accent.copy(alpha = 0.07f) else if (dark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f)
    val edge = if (isOn) accent.copy(alpha = 0.33f) else if (dark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f)
    val beat = rememberNeonBeat(1500)

    Box(
        Modifier
            .fillMaxWidth()
            .then(if (isOn) Modifier.neonGlow(accent, cornerRadius = 25.dp, spread = 16.dp, intensity = 1f, maxAlpha = 0.30f) else Modifier),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .clip(shape)
                .background(bg)
                .border(1.dp, edge, shape)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isOn) accent.copy(alpha = 0.13f) else HumeColors.Gray00),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        sensor.icon,
                        contentDescription = null,
                        tint = if (isOn) accent else HumeColors.Gray1000,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        sensor.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isOn) accent else HumeColors.Gray1000,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.fillMaxWidth().humeMarquee(),
                    )
                    if (minutes != null) {
                        Text(
                            agoLabel(minutes),
                            fontSize = 9.sp,
                            color = HumeColors.Gray500.copy(alpha = 0.7f),
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(top = 1.dp),
                        )
                    }
                }
            }
            Text(
                status,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isOn) accent else HumeColors.Gray500,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isOn) accent.copy(alpha = 0.13f) else HumeColors.Gray00)
                    .padding(horizontal = 10.dp, vertical = 2.dp),
            )
        }

        if (isOn) {
            // Cham neon o goc, bi cat boi overflow:hidden cua the — giong HTML.
            Box(Modifier.matchParentSize().clip(shape)) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(14.dp)
                        .neonGlowCircle(accent, spread = 10.dp, intensity = beat, maxAlpha = 0.6f)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.3f + 0.7f * beat)),
                )
            }
        }
    }
}

/** agoText() in SecurityView.swift */
private fun agoLabel(minutes: Int): String = when {
    minutes < 1 -> "V\u1eeba xong"
    minutes < 60 -> minutes.toString() + " ph\u00fat tr\u01b0\u1edbc"
    else -> (minutes / 60).toString() + " gi\u1edd tr\u01b0\u1edbc"
}
