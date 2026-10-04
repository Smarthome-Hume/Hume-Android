package com.smarthome.hume.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import com.smarthome.hume.core.data.HumeGraph
import com.smarthome.hume.core.model.HomeNotification
import com.smarthome.hume.core.model.HomeUiState
import com.smarthome.hume.core.model.cameraKeyForSensor
import com.smarthome.hume.core.model.roomNameForSensor
import com.smarthome.hume.core.model.ConnectionState
import com.smarthome.hume.core.model.SecurityCamera
import com.smarthome.hume.core.model.SolarDay
import com.smarthome.hume.core.ui.camera.CameraFeedCard
import com.smarthome.hume.core.ui.components.blink
import com.smarthome.hume.core.ui.components.MarqueeText
import com.smarthome.hume.core.ui.components.M3ECard
import com.smarthome.hume.core.ui.components.M3EIcons
import com.smarthome.hume.core.ui.components.WeekChartD
import com.smarthome.hume.core.ui.components.pressMorph
import com.smarthome.hume.core.ui.components.rememberHaptic
import com.smarthome.hume.core.ui.components.Ms
import com.smarthome.hume.core.data.AiTip
import com.smarthome.hume.core.data.isPhoneMotionTip
import com.smarthome.hume.core.ui.components.M3EMotion
import com.smarthome.hume.core.ui.avatar.LoopingVideoAvatar
import com.smarthome.hume.core.ui.avatar.UserAvatar
import com.smarthome.hume.core.ui.components.MsIcon
import com.smarthome.hume.core.ui.theme.LocalHumeExtraColors
import java.time.LocalTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

fun greeting(): String = when (LocalTime.now().hour) {
    in 5..10 -> "Chào buổi sáng"
    in 11..13 -> "Chào buổi trưa"
    in 14..18 -> "Chào buổi chiều"
    else -> "Chào buổi tối"
}

/**
 * Header trang Nha theo demo rev12 (.hhome):
 * avatar 55px + cham xanh presence (.pdot 16px) | "Hi, ..." 22px/700/-0.2
 * truoc, loi chao 13px/500 sau | nut tron 46px, press morph scale(.88) spring.
 */
@Composable
fun HomeHeader(
    state: HomeUiState,
    avatarUrl: String? = null,
    avatarToken: String? = null,
    userAvatar: UserAvatar? = null,
    onAvatarTap: () -> Unit = {},
    onAvatarPositioned: (Rect) -> Unit = {},
    onSearch: () -> Unit,
    onNotif: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val extra = LocalHumeExtraColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp, start = 2.dp, end = 2.dp),
    ) {
        // .hava 55px surfaceHighest + .pdot 16px + vien ngoai 3px = 22px
        // Nhan vao avatar -> phong to kieu kinh lup (giong trang Thong tin).
        // KHONG clip tron o Box ngoai: cham trang thai o TopEnd tran ra ngoai
        // vien tron, bi clip cat mat mot phan. Box anh ben trong tu clip tron.
        Box(
            Modifier
                .size(55.dp)
                .onGloballyPositioned { onAvatarPositioned(it.boundsInWindow()) }
                .clickable(onClick = onAvatarTap),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(extra.surfaceHighest),
            ) {
                // Uu tien: anh/video user upload > anh HA > icon mac dinh
                when {
                    userAvatar != null && !userAvatar.isVideo -> AsyncImage(
                        model = userAvatar.file,
                        contentDescription = state.userName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                    )
                    userAvatar != null -> LoopingVideoAvatar(
                        file = userAvatar.file,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                    )
                    avatarUrl.isNullOrBlank() -> {
                        MsIcon(
                            M3EIcons.Person, null,
                            tint = cs.onSurfaceVariant,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    else -> {
                        val context = LocalContext.current
                        SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(avatarUrl)
                            .apply {
                                if (!avatarToken.isNullOrBlank()) {
                                    setHeader("Authorization", "Bearer $avatarToken")
                                }
                            }
                            .build(),
                        contentDescription = state.userName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        loading = {
                            MsIcon(
                                M3EIcons.Person, null,
                                tint = cs.onSurfaceVariant,
                                modifier = Modifier.size(28.dp),
                            )
                        },
                        error = {
                            MsIcon(
                                M3EIcons.Person, null,
                                tint = cs.onSurfaceVariant,
                                modifier = Modifier.size(28.dp),
                            )
                        },
                    )
                    } // else -> (anh HA)
                } // when
            } // Box avatar
            // Den neon nhap nhay theo trang thai ket noi HA:
            // xanh la = da ket noi (nhap nhay nhe), do = mat ket noi, da cam = dang ket noi
            val (neonColor, blinkMs) = when (state.connectionState) {
                ConnectionState.Connected -> Color(0xFF22C55E) to 1600
                ConnectionState.Connecting -> Color(0xFFF59E0B) to 800
                ConnectionState.Disconnected -> Color(0xFFEF4444) to 0
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(cs.surface),
            ) {
                // Glow ngoai (blur) + dot dac — hieu ung den neon
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(neonColor.copy(alpha = 0.45f))
                        .blur(4.dp)
                        .then(if (blinkMs > 0) Modifier.blink(blinkMs) else Modifier),
                )
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(neonColor)
                        .then(if (blinkMs > 0) Modifier.blink(blinkMs) else Modifier),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(
            Modifier.weight(1f),
        ) {
            val name = state.userName.ifBlank { "Gia đình" }
            Text(
                text = "Hi, $name",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp,
                color = cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = greeting(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = cs.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        // Nut tim kiem tron 46px, nen surfaceHighest, icon 22px
        Spacer(Modifier.width(10.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(extra.surfaceHighest)
                .pressMorph(pressedScale = 0.88f, onClick = onSearch),
        ) {
            MsIcon(
                Ms.search, null,
                tint = cs.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        // Chuong 46px + badge custom (error, 18px, 11px/700, top/right -2px)
        Box(Modifier.size(46.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(extra.surfaceHighest)
                    .pressMorph(pressedScale = 0.88f, onClick = onNotif),
            ) {
                MsIcon(
                    Ms.notifications, null,
                    tint = cs.onSurface,
                    modifier = Modifier.size(22.dp),
                )
            }
            if (state.notifications.isNotEmpty()) {
                // Badge tron co dinh 20.dp (khong gian theo so chu so nhu pill).
                // lineHeight = fontSize de glyph can giua doc chuan (khong lech do font metrics).
                val count = state.notifications.size.coerceAtMost(99)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        // User 2026-09-29: badge dung mau theme (primary),
                        // khong dung do (error).
                        .background(cs.primary),
                ) {
                    Text(
                        "$count",
                        style = MaterialTheme.typography.labelSmall,
                        lineHeight = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onPrimary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * The goi y theo demo rev12 (.suggest/.sgbtn): padding 16px 18px,
 * icon auto_awesome 30px co dinh, tieu de 14px/700, sub 12px/500,
 * nut 13px/700 padding 12px 20px bo 20px, :active scale(.9) + bo 13px spring.
 * Chi hien cac goi y urgent/he thong: pin < 30%, cua mo, camera phat hien
 * chuyen dong. Khong co goi y nao -> an the di (return, khong render).
 * Vuot ngang qua lai giua cac goi y (HorizontalPager) + dot indicator
 * ben duoi khi > 1 goi y.
 * Nhan nut: rung nhe + action that theo loai:
 *  - battery: onBatteryDetail() (mo chi tiet pin)
 *  - camera: onOpenSecurity() (chuyen sang trang An ninh)
 *  - door: chi danh dau done (giu nguyen nhu cu)
 *  - key khac: onTipAction(key)
 * Trang thai .done: opacity .6, text theo tip.doneLabel (mac dinh "Đã bật", theo HTML that).
 */
internal data class SuggestTip(
    val key: String,
    val title: String,
    val sub: String,
    val action: String,
    /** Nhan nut sau khi da xu ly xong (vd "Da tat" cho dieu hoa). */
    val doneLabel: String = "Đã bật",
)

/**
 * Camera phu hop nhat cho sensor chuyen dong: theo key tu cameraKeyForSensor;
 * phong khong co camera (phong tam, phong giat...) -> null, KHONG roi ve
 * camera dau tien (2026-09-30, user: goi y sai phong).
 */
internal fun cameraForSensor(sensorId: String, cameras: List<SecurityCamera>): SecurityCamera? {
    if (cameras.isEmpty()) return null
    val key = cameraKeyForSensor(sensorId) ?: return null
    return cameras.firstOrNull { it.key == key }
}

/**
 * Dong goi y phan ung theo state truc tiep (khong can AI):
 * - Co chuyen dong o phong nao -> goi y xem camera phong do (key rieng theo sensor).
 * - Het chuyen dong (sensor off) -> thong bao mat khoi state -> goi y tu dong bien mat.
 * Dung chung cho SuggestCard + HomeScreen.
 */
internal fun buildSuggestTips(state: HomeUiState): List<SuggestTip> {
    // Goi y chuyen dong: gom theo phong, moi phong chi giu 1 goi y co sensor
    // trigger GAN NHAT (vd 4 sensor phong khach trigger khac gio -> 1 the).
    // Mo ta chi tiet theo phan loai Frigate (vd "Có người hoạt động lúc 06:25").
    val motionTips = state.notifications
        .filter { it.title == "Phát hiện chuyển động" }
        .groupBy { roomNameForSensor(it.id) ?: it.id }
        .mapNotNull { (_, ns) ->
            val n = ns.minByOrNull { it.minutesAgo ?: Int.MAX_VALUE } ?: return@mapNotNull null
            val room = roomNameForSensor(n.id)
            val obj = state.motionObjects[n.id]
            val sub = if (obj != null) {
                val hm = n.minutesAgo?.let { mins ->
                    try {
                        java.time.LocalTime.now().minusMinutes(mins.toLong())
                            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                    } catch (e: Exception) { null }
                }
                if (hm != null) "Có $obj hoạt động lúc $hm" else "Có $obj hoạt động (${n.timeText})"
            } else n.timeText
            SuggestTip(
                key = "motion:${n.id}",
                // Title: ten phong / vi tri xuat hien; Subtitle: doi tuong + gio.
                title = room ?: n.body.ifBlank { "Phát hiện chuyển động" },
                sub = sub,
                // Phong co camera moi hien "Xem camera"; khong co -> "An ninh"
                // (2026-09-30, user: phong tam lam gi co camera).
                action = if (cameraKeyForSensor(n.id) != null) "Xem camera" else "An ninh",
            )
        }
    return buildList<SuggestTip> {
        addAll(motionTips)
    // Tip pin cu chi hien khi soc >= muc du tru (duoi muc du tru thi tip sac ep thay the).
    if (state.battery.soc in 1..29 && state.battery.soc >= state.battery.backupSoc) add(SuggestTip(
        "battery", "Pin còn ${state.battery.soc}%",
        "Hạn chế tải nặng chờ nắng lên.", "Xem pin"))
    val doors = state.notifications.filter { it.title.contains("Cửa") }
    // Cua ban cong mo -> canh bao trom (uu tien hon goi y cua chung)
    val balconyDoor = doors.firstOrNull { it.title.contains("ban công", ignoreCase = true) }
    if (balconyDoor != null) add(SuggestTip(
        key = "door_balcony",
        title = "Cửa ban công đang mở",
        sub = "Đóng lại kẻo trộm đột nhập.",
        action = "Đã đóng",
        doneLabel = "Đã đóng",
    ))
    // Cua chung (tru ban cong da xu ly rieng o tren)
    val otherDoors = if (balconyDoor != null) doors.filter { it != balconyDoor } else doors
    if (otherDoors.isNotEmpty()) add(SuggestTip(
        "door", otherDoors.first().title, otherDoors.first().body, "Đóng"))
    // (goi y chuyen dong da gom theo phong o tren: addAll(motionTips))
    // Dieu hoa chay trong khi cua mo cung phong -> ton dien
    doors.forEach { d ->
        val roomName = roomNameForSensor(d.id) ?: return@forEach
        val room = state.rooms.firstOrNull { it.name == roomName } ?: return@forEach
        val cl = room.climate
        if (cl != null && cl.isOn) add(SuggestTip(
            key = "toggle_ac:${cl.entityId}",
            title = "Đang tốn điện ở $roomName",
            sub = "Điều hòa chạy trong khi ${d.body} đang mở.",
            action = "Tắt điều hòa",
            doneLabel = "Đã tắt",
        ))
    }
    // Pin day + nang to -> goi y dung dien du
    if (state.battery.soc >= 95 && state.solarNowKw >= 2.0) add(SuggestTip(
        "sun",
        "Đang dư điện mặt trời",
        "Pin đã ${state.battery.soc}%, đang phát ${"%.1f".format(state.solarNowKw)} kW — chạy máy nặng lúc này.",
        "Xem điện",
    ))
    // Phong nong theo tung phong (khong goi y do am: nha khong co he thong thong gio)
    state.rooms.forEach { r ->
        val t = r.tempC
        val cl = r.climate
        if (t != null && t >= 31 && cl != null && !cl.isOn) add(SuggestTip(
            key = "toggle_ac:${cl.entityId}",
            title = "${r.name} đang ${"%.0f".format(t)}°C",
            sub = "Bật điều hòa làm mát phòng?",
            action = "Bật điều hòa",
        ))
    }
    // Canh bao khan: khoi / ro nuoc
    state.notifications.firstOrNull { it.title.contains("khói", ignoreCase = true) }?.let { n ->
        add(SuggestTip("alert:${n.id}", n.title, n.body, "Đã hiểu", "Đã hiểu"))
    }
    state.notifications.firstOrNull { it.title.contains("rò nước", ignoreCase = true) }?.let { n ->
        add(SuggestTip("alert:${n.id}", n.title, n.body, "Đã hiểu", "Đã hiểu"))
    }
    // Dem chua bat an ninh
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val alarm = state.alarm
    if (alarm != null && !alarm.isArmed && (hour >= 22 || hour < 6)) add(SuggestTip(
        "night",
        "Chưa bật an ninh đêm",
        "Đã ${hour}h — bật chế độ đêm cho an tâm.",
        "Xem an ninh",
    ))
    // 9-11h: nhieu den dang bat -> nhac tat (troi sang roi)
    if (hour in 9..11 && state.lightsOn.size >= 3) add(SuggestTip(
        key = "lights_day",
        title = "Đang bật ${state.lightsOn.size} đèn",
        sub = "Trời sáng rồi — tắt bớt đèn tiết kiệm điện?",
        action = "Tắt hết đèn",
        doneLabel = "Đã tắt",
    ))
    // 17-20h: toi dan, pin thap -> han che tai nang
    if (hour in 17..20 && state.battery.soc in 1..49 && state.solarNowKw < 0.5) add(SuggestTip(
        key = "evening_battery",
        title = "Tối rồi, pin còn ${state.battery.soc}%",
        sub = "Hạn chế dùng thiết bị nặng để dành pin qua đêm.",
        action = "Xem pin",
    ))
    // Goi y ve chuyen dong/hieu nang cua dien thoai: bo qua,
    // khong dua vao danh sach goi y.
    // Chong trung tieu de giua cac loai goi y: giu goi y dau tien (moi nhat).
    }.distinctBy { it.title }
        .filterNot { isPhoneMotionTip(it.title, it.sub) }
}

@Composable
fun SuggestCard(
    state: HomeUiState,
    /** Goi y tu AI (LLM, phan tich dinh ky); hien sau cac goi y phan ung truc tiep. */
    aiTips: List<AiTip> = emptyList(),
    /** Danh sach camera tu tab An ninh (de map phong -> camera khi bam "Xem camera"). */
    cameras: List<SecurityCamera> = emptyList(),
    onTipAction: (key: String) -> Unit = {},
    onBatteryDetail: () -> Unit = {},
    onOpenSecurity: () -> Unit = {},
    onOpenEnergy: () -> Unit = {},
    /** Mo popup camera ngay tren trang Nha (thay vi chuyen sang tab An ninh). */
    onOpenCamera: (camKey: String, camName: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    // Goi y phan ung truc tiep theo state (chuyen dong -> camera, cua mo, pin yeu)
    // + goi y AI phan tich sau.
    val ruleTips = buildSuggestTips(state).toMutableList()
    // Chong trung: neu rule da co goi y pin, bo goi y AI ve pin (user bao bi double).
    val hasBatteryTip = ruleTips.any { it.key == "battery" }
    val aiFiltered = aiTips.filterNot { tip ->
        hasBatteryTip && (tip.title.contains("pin", ignoreCase = true) ||
            tip.sub.contains("pin", ignoreCase = true))
    }
    val allTips = ruleTips + aiFiltered.map { SuggestTip(it.key, it.title, it.sub, it.action) }
    // Goi y da bi user xoa: luu key -> noi dung luc xoa; hien lai neu co su kien moi (noi dung doi)
    val dismissed = remember { mutableStateMapOf<String, String>() }
    // Theo doi thoi gian xuat hien de tu dong an the cu (qua 60 phut).
    val firstSeen = remember { mutableStateMapOf<String, Long>() }
    val nowMs = System.currentTimeMillis()
    // Dieu kien da het (vd het chuyen dong) -> quen trang thai xoa de su kien moi hien lai goi y
    LaunchedEffect(allTips.map { it.key }) {
        val liveKeys = allTips.map { it.key }.toSet()
        dismissed.keys.filter { it !in liveKeys }.forEach { dismissed.remove(it) }
        firstSeen.keys.filter { it !in liveKeys }.forEach { firstSeen.remove(it) }
    }
    // Ghi nhan thoi gian xuat hien lan dau cua moi tip.
    LaunchedEffect(allTips.map { it.key }) {
        allTips.forEach { tip -> firstSeen.getOrPut(tip.key) { nowMs } }
    }
    val tips = allTips.filter { tip ->
        val d = dismissed[tip.key]
        if (d != null && d == tip.title + "|" + tip.sub) return@filter false
        // Tu dong an the da hien qua 60 phut.
        val seen = firstSeen[tip.key] ?: nowMs
        nowMs - seen < 60 * 60 * 1000L
    }.take(10)
    if (tips.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { tips.size })
    val doneMap = remember { mutableStateMapOf<String, Boolean>() }
    val haptic = rememberHaptic()
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme
    val density = LocalDensity.current
    // List rut ngan lai (vd cua da dong): giu currentPage trong bien.
    LaunchedEffect(tips.size) {
        if (pagerState.currentPage >= tips.size) pagerState.scrollToPage(tips.size - 1)
    }
    // Cu chi nhan-giu + vuot len de xoa (thay nut X):
    // nhan-giu -> the chim xuong (sink + scale .97); vuot len -> the di theo tay,
    // tha qua nguong -> bay len + mo dan roi xoa; tha som -> nay spring ve cu.
    var pressing by remember { mutableStateOf(false) }
    var dragDy by remember { mutableFloatStateOf(0f) }
    val offsetYPx = remember { Animatable(0f) }
    val cardAlpha = remember { Animatable(1f) }
    val cardScale = remember { Animatable(1f) }
    val sinkPx = with(density) { 7.dp.toPx() }
    val dismissThresholdPx = with(density) { 80.dp.toPx() }
    val flyOutPx = with(density) { 180.dp.toPx() }
    val fadeRangePx = with(density) { 240.dp.toPx() }
    val pressSpring = spring<Float>(stiffness = Spring.StiffnessMediumLow)
    suspend fun dismissCurrentTip() {
        val tip = tips.getOrNull(pagerState.currentPage) ?: return
        haptic()
        offsetYPx.animateTo(-flyOutPx, tween(320, easing = M3EMotion.emphasizedAcc))
        cardAlpha.animateTo(0f, tween(300))
        dismissed[tip.key] = tip.title + "|" + tip.sub
        // Reset cho tip ke tiep hien ra
        offsetYPx.snapTo(0f)
        cardAlpha.snapTo(1f)
        cardScale.snapTo(1f)
        dragDy = 0f
    }
    suspend fun springBack() {
        dragDy = 0f
        offsetYPx.animateTo(0f, pressSpring)
        cardScale.animateTo(1f, pressSpring)
        cardAlpha.animateTo(1f, tween(200))
    }
    M3ECard(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                translationY = offsetYPx.value
                scaleX = cardScale.value
                scaleY = cardScale.value
                alpha = cardAlpha.value
            }
            .pointerInput(tips) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        pressing = true
                        haptic()
                        scope.launch {
                            launch { offsetYPx.animateTo(sinkPx, pressSpring) }
                            launch { cardScale.animateTo(0.97f, pressSpring) }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragDy += dragAmount.y
                        scope.launch {
                            offsetYPx.snapTo(sinkPx + dragDy)
                            cardAlpha.snapTo(
                                (1f - min(-dragDy / fadeRangePx, 0.5f)).coerceIn(0f, 1f),
                            )
                        }
                    },
                    onDragEnd = {
                        pressing = false
                        scope.launch {
                            if (dragDy <= -dismissThresholdPx) dismissCurrentTip()
                            else springBack()
                        }
                    },
                    onDragCancel = {
                        pressing = false
                        scope.launch { springBack() }
                    },
                )
            },
        containerColor = cs.tertiaryContainer,
        shape = RoundedCornerShape(28.dp),
        contentPadding = 16.dp,
    ) {
        Column {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                userScrollEnabled = !pressing,
            ) { page ->
                val tip = tips[page]
                SuggestTipRow(
                    tip = tip,
                    done = doneMap[tip.key] == true,
                    batterySoc = state.battery.soc,
                    onAction = {
                        haptic()
                        when {
                            tip.key == "battery" -> onBatteryDetail()
                            tip.key.startsWith("motion:") -> {
                                // Mo popup camera ngay tren trang Nha (khong chuyen tab).
                                val sensorId = tip.key.removePrefix("motion:")
                                cameraForSensor(sensorId, cameras)?.let { cam ->
                                    onOpenCamera(cam.key, cam.name)
                                } ?: onOpenSecurity()
                            }
                            tip.key == "door" -> doneMap[tip.key] = true
                            tip.key == "door_balcony" -> doneMap[tip.key] = true
                            tip.key == "sun" -> onOpenEnergy()
                            tip.key == "night" -> onOpenSecurity()
                            tip.key == "lights_day" -> {
                                onTipAction(tip.key)
                                doneMap[tip.key] = true
                            }
                            tip.key == "evening_battery" -> onBatteryDetail()
                            tip.key.startsWith("toggle_ac:") -> {
                                onTipAction(tip.key)
                                doneMap[tip.key] = true
                            }
                            tip.key.startsWith("alert:") ->
                                dismissed[tip.key] = tip.title + "|" + tip.sub
                            tip.key.startsWith("ai_") ->
                                dismissed[tip.key] = tip.title + "|" + tip.sub
                            else -> onTipAction(tip.key)
                        }
                    },
                )
            }
            if (tips.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    tips.forEachIndexed { i, _ ->
                        val selected = i == pagerState.currentPage
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (selected) 7.dp else 5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selected) cs.onTertiaryContainer
                                    else cs.onTertiaryContainer.copy(alpha = 0.35f),
                                )
                                .clickable {
                                    scope.launch { pagerState.animateScrollToPage(i) }
                                },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Goi mo popup camera o tang root (phu TOAN man hinh, ke ca navbar),
 * hieu ung mo/dim giong viewer avatar.
 */
data class CameraPopupRequest(val camKey: String, val camName: String)

/**
 * Popup camera mo ngay tren trang Nha khi bam "Xem camera" o the goi y.
 * Cham vung mo ben ngoai de dong; cham vao feed de mo khoa (giong tab An ninh).
 */
@Composable
fun CameraPopupOverlay(
    camKey: String,
    camName: String,
    onDismiss: () -> Unit,
) {
    var unlocked by remember { mutableStateOf(false) }
    val secRepo = remember { HumeGraph.get().securityRepository }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        CameraFeedCard(
            camKey = camKey,
            camName = camName,
            snapshotUrl = secRepo.snapshotUrl(camKey),
            unlocked = unlocked,
            onUnlock = { unlocked = true },
            mjpegUrl = secRepo.mjpegUrl(camKey),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                // Chan tap xuyen qua the lam dong popup
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        )
    }
}

@Composable
private fun SuggestTipRow(
    tip: SuggestTip,
    done: Boolean,
    onAction: () -> Unit,
    /** % pin de hien icon pin dung muc (cho tip pin yeu). */
    batterySoc: Int? = null,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 2.dp),
    ) {
        // Tip pin yeu: icon pin theo muc % (2026-09-30, user yeu cau).
        if (tip.key == "battery" && batterySoc != null) {
            MsIcon(
                M3EIcons.batteryLevel(batterySoc), null,
                tint = cs.onTertiaryContainer,
                modifier = Modifier.size(30.dp),
            )
        } else {
            MsIcon(
                Ms.auto_awesome, null,
                tint = cs.onTertiaryContainer,
                modifier = Modifier.size(30.dp),
            )
        }
        // Viewport marquee keo dai den sat mep icon (bo Spacer): chu chay
        // den mep icon moi an; luc nghi chu van dung yen nho startPadding.
        Column(Modifier.weight(1f)) {
            // The co dinh 1 dong tieu de + 1 dong mo ta; chu dai tu chay marquee
            MarqueeText(
                text = tip.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = cs.onTertiaryContainer,
                startPadding = 14.dp,
            )
            MarqueeText(
                text = tip.sub,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = cs.onTertiaryContainer.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 2.dp),
                startPadding = 14.dp,
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .pressMorphCard(
                    pressedScale = 0.9f,
                    corner = 14.dp,
                    pressedCorner = 10.dp,
                    onClick = if (done) null else onAction,
                )
                // .sgbtn: nen onTertiaryContainer; :disabled{opacity:.6}
                .alpha(if (done) 0.6f else 1f)
                .background(cs.onTertiaryContainer)
                .padding(horizontal = 12.dp, vertical = 7.dp),
        ) {
            Text(
                if (done) tip.doneLabel else tip.action,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = cs.tertiaryContainer,
            )
        }
    }
}

/**
 * The dien mat troi tuan (.solcard): bo 32px, padding 20px;
 * header KHONG icon: title 14px/700 + sub "San luong hom nay · truc tiep" 12px/500,
 * gia tri 26px/800 + "kWh" 13px/600;
 * bieu do scale CO DINH maxV=7 (khong scale theo data), tooltip khi cham,
 * tick 8s tang cot hom nay (+0.06, tran 6.8).
 */
@Composable
fun SolarWeekCard(state: HomeUiState, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val week: List<SolarDay> = state.solarWeek

    val vals = week.map { it.kwh }
    // Gia tri hom nay that tu HA (khong mo phong tang dan)
    val todayShown = state.solarTodayKwh?.toFloat() ?: vals.lastOrNull() ?: 0f

    M3ECard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        contentPadding = 20.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Điện mặt trời",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                "%.1f".format(todayShown),
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
        Spacer(Modifier.height(14.dp)) // .stop mb 6 + .solsvg mt 8
        // Chi ve chart khi co data that (>0); khong thi placeholder gon, tranh 150dp trang
        if (vals.isNotEmpty() && vals.any { it > 0f }) {
            WeekChartD(
                vals = vals,
                labels = week.map { it.label },
            )
        } else {
            Text(
                "Đang tải dữ liệu…",
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
            )
        }
    }
}

/**
 * The thong bao moi nhat - dat duoi "Goi y cho ban" de truy cap nhanh.
 * Neu AI duoc cau hinh, hien tom tat AI ve thong bao.
 */
@Composable
fun NotificationCard(
    notifications: List<HomeNotification>,
    aiSummary: String?,
    onViewAll: () -> Unit,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val latest = notifications.firstOrNull() ?: return

    M3ECard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        contentPadding = 18.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(cs.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Icon(
                    imageVector = M3EIcons.BellVector,
                    contentDescription = null,
                    tint = cs.onPrimaryContainer,
                    modifier = Modifier.size(28.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    latest.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${latest.body} · ${latest.timeText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
                // Tom tat AI (neu co)
                aiSummary?.let { summary ->
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.primary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                if (notifications.size > 1) {
                    TextButton(onClick = onViewAll) {
                        Text(
                            "Xem tất cả (${notifications.size})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            IconButton(onClick = { onDismiss(latest.id) }) {
                MsIcon(
                    M3EIcons.Close,
                    contentDescription = "Bỏ qua",
                    tint = cs.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
