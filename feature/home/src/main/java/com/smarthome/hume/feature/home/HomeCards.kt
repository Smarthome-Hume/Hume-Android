package com.smarthome.hume.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.VectorConverter
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.smarthome.hume.core.data.HumeGraph
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
import com.smarthome.hume.core.ui.components.rememberHaptic
import com.smarthome.hume.core.ui.components.Ms
import com.smarthome.hume.core.data.AiTip
import com.smarthome.hume.core.data.isPhoneMotionTip
import com.smarthome.hume.core.ui.components.M3EMotion
import com.smarthome.hume.core.ui.avatar.LoopingVideoAvatar
import com.smarthome.hume.core.ui.avatar.UserAvatar
import com.smarthome.hume.core.ui.components.MsIcon
import java.time.LocalTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min
import java.util.Locale

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
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp, start = 2.dp, end = 2.dp),
    ) {
        // .hava 55px surfaceContainerHighest + .pdot 16px + vien ngoai 3px = 22px
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
                    // iOS HomeHeader.swift:96,133: Circle().fill(c.surfaceContainerHighest).
                    // Truoc day extra.surfaceHighest — voi seed Indigo/Lime, extra
                    // fallback ve Cam (M3ERawTokens thieu key) nen avatar co nen
                    // nau 35302A thay vi cham 1A203B.
                    .background(cs.surfaceContainerHighest),
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
        // (GAP-12) Da xoa nut tim kiem + chuong thong bao khoi header
        // (port iOS 487946e: HomeHeader chi con avatar 55 + ten + loi chao).
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
    // (2026-10-14, audit #10): da xoa rule nay — chuyen sang Energy Insights (theo iOS)
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
    // (2026-10-14, audit #10): da xoa rule nay — chuyen sang Energy Insights (theo iOS)
    // (2026-10-14, audit #10): da xoa rule nay — chuyen sang Energy Insights (theo iOS)
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
    /** Mo popup camera ngay tren trang Nha (thay vi chuyen sang tab An ninh);
     * truyen kem sourceRect cua nut bam de popup animate tu do (GAP-36). */
    onOpenCamera: (camKey: String, camName: String, sourceRect: Rect?) -> Unit = { _, _, _ -> },
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
    // (Da xoa hieu ung an xuong nhu iOS HomeSuggest: nhan-giu chi rung
    // haptic, khong chim 7dp + scale .97 nua); vuot len -> the di theo tay,
    // tha qua nguong -> bay len + mo dan roi xoa; tha som -> nay spring ve cu.
    var pressing by remember { mutableStateOf(false) }
    var dragDy by remember { mutableFloatStateOf(0f) }
    val offsetYPx = remember { Animatable(0f) }
    val cardAlpha = remember { Animatable(1f) }
    val cardScale = remember { Animatable(1f) }
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
                        // Chi rung haptic + keo theo tay (khong chim/scale nua).
                        pressing = true
                        haptic()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragDy += dragAmount.y
                        scope.launch {
                            offsetYPx.snapTo(dragDy)
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
        // (2026-11-17) Rut ngan them theo lenh user (vong 44d50c2 da chot
        // cat DUOI chuan iOS): padding DOC 16 -> 12dp (-8dp). iOS
        // contentPadding 16, Android moi thanh phan da <= iOS (icon 30<40,
        // title 14<16pt, sub 12<14pt, nut v7<10, pager tu nhien 33,7<60)
        // nen padding doc 32dp (gan nua the) la phan duy nhat con cat
        // duoc ma khong pha noi dung. Padding NGANG giu 16 (12+4 bu ben
        // trong) de icon/text/nut khong gian ra.
        contentPadding = 12.dp,
    ) {
        Column(Modifier.padding(horizontal = 4.dp)) {
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
                    onAction = { rect ->
                        haptic()
                        when {
                            tip.key == "battery" -> onBatteryDetail()
                            tip.key.startsWith("motion:") -> {
                                // Mo popup camera ngay tren trang Nha (khong chuyen tab).
                                val sensorId = tip.key.removePrefix("motion:")
                                cameraForSensor(sensorId, cameras)?.let { cam ->
                                    onOpenCamera(cam.key, cam.name, rect)
                                } ?: onOpenSecurity()
                            }
                            tip.key == "door" -> doneMap[tip.key] = true
                            tip.key == "door_balcony" -> doneMap[tip.key] = true
                            tip.key == "night" -> onOpenSecurity()
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
                        // (2026-11-17) Khe tren dots 10 -> 6dp (-4dp khi
                        // >1 tip, cat duoi iOS theo lenh user; 6dp la so
                        // da dung o vong 44d50c2: spacer 10->6, khe hang 8->6).
                        .padding(top = 6.dp),
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
data class CameraPopupRequest(
    val camKey: String,
    val camName: String,
    /** Vi tri nut "Xem camera" (boundsInWindow) — de animate tu do phong ra. */
    val sourceRect: Rect? = null,
)

/**
 * Popup camera mo ngay tren trang Nha khi bam "Xem camera" o the goi y.
 * Cham vung mo ben ngoai de dong; cham vao feed de mo khoa (giong tab An ninh).
 *
 * (GAP-36) Port iOS HomeSuggest.swift:1013-1100 (CameraOverlayView):
 * animate 1 animRect duy nhat tu sourceRect cua nut -> card 16:9 giua man
 * hinh (rong W-40, dich len 50dp), thu ve dung vi tri nut khi dong; khong
 * dung scaleEffect (tranh giat) va khong dung vong delay() step-based — dung
 * Animatable<Rect> + animateTo dong bo vsync (quy tac 120Hz cua repo), cung
 * cau truc nhu AvatarViewerOverlay (core/ui/avatar/AvatarViewer.kt).
 * - Mo: spring(response: 0.35, dampingFraction: 0.8) -> stiffness (2pi/0.35)^2
 *   ~ 322, dampingRatio 0.8.
 * - Dong: spring(response: 0.3, dampingFraction: 0.8) -> stiffness ~ 439.
 * - Khong co sourceRect: mo tu giua nho 40x40 (y = H/2 - 70, nhu iOS);
 *   dong thu nho ve tam 40x40.
 * Luu y: card Android co padding 12dp moi ben (CameraFeedCard) nen chieu cao
 * dich = (cardW - 24dp) * 9/16 + 24dp — dung chieu cao tu nhien nay de luc
 * dung yen card khong bi keo/gian; giua chuyen dong feed (16:9 theo chieu
 * rong) co the thua/thieu vai px chieu cao, card clip theo bo 32dp.
 */
@Composable
fun CameraPopupOverlay(
    camKey: String,
    camName: String,
    /** Vi tri nut "Xem camera" (boundsInWindow). Null = mo tu giua nho. */
    sourceRect: Rect? = null,
    onDismiss: () -> Unit,
) {
    var unlocked by remember { mutableStateOf(false) }
    val secRepo = remember { HumeGraph.get().securityRepository }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var overlayRect by remember { mutableStateOf<Rect?>(null) }
    var closing by remember { mutableStateOf(false) }

    // Dich: card rong W-40, giua man hinh, nhich len 50dp (iOS targetRect).
    fun targetRect(): Rect? {
        val o = overlayRect ?: return null
        val cardW = o.width - with(density) { 40.dp.toPx() }
        val pad = with(density) { 24.dp.toPx() }
        val cardH = (cardW - pad) * 9f / 16f + pad
        val up = with(density) { 50.dp.toPx() }
        val left = o.left + (o.width - cardW) / 2f
        val top = o.top + (o.height - cardH) / 2f - up
        return Rect(left, top, left + cardW, top + cardH)
    }

    // Ve card NGAY tu frame dau tai dung vi tri nut (nhu AvatarViewerOverlay):
    // truoc day phai doi overlayRect do xong moi ve -> card bien mat 1-2 frame.
    val rectAnim = remember(sourceRect) {
        Animatable(sourceRect ?: Rect(0f, 0f, 40f, 40f), Rect.VectorConverter)
    }
    // Nen mo dan mem thay vi pop 0.55 ngay frame dau (nhu AvatarViewerOverlay).
    var scrimOn by remember { mutableStateOf(false) }
    val scrimAlpha by animateFloatAsState(
        targetValue = if (closing) 0f else if (scrimOn) 0.55f else 0f,
        animationSpec = tween(250, easing = M3EMotion.emphasized),
        label = "camScrim",
    )

    // Mo ra: tu nut -> card giua man hinh. Chay dung 1 lan (khoa Unit):
    // khong lay sourceRect/overlayRect lam key vi khi mo popup, content bi
    // blur -> header do lai vi tri -> onGloballyPositioned ban lai -> key
    // doi -> effect relaunch giua chung -> animateTo bi huy.
    LaunchedEffect(Unit) {
        scrimOn = true
        snapshotFlow { overlayRect }.filterNotNull().first()
        // Khong co source: bat dau tu giua nho 40x40 (iOS: W/2-20, H/2-70).
        if (sourceRect == null) {
            val o = overlayRect ?: return@LaunchedEffect
            rectAnim.snapTo(
                Rect(o.center.x - 20f, o.center.y - 70f, o.center.x + 20f, o.center.y - 30f),
            )
        }
        val target = targetRect() ?: return@LaunchedEffect
        rectAnim.animateTo(
            target,
            spring(stiffness = 322f, dampingRatio = 0.8f),
        )
    }

    fun requestClose() {
        if (closing) return
        closing = true
        scope.launch {
            // Thu ve dung vi tri nut; khong co nut thi thu nho ve tam 40x40.
            val closeRect = sourceRect ?: rectAnim.value.let { r ->
                Rect(r.center.x - 20f, r.center.y - 20f, r.center.x + 20f, r.center.y + 20f)
            }
            rectAnim.animateTo(
                closeRect,
                spring(stiffness = 439f, dampingRatio = 0.8f),
            )
            onDismiss()
        }
    }

    // Ve card tai vi tri animate (doi window -> local cua overlay).
    val r = rectAnim.value
    val origin = overlayRect?.topLeft ?: Offset.Zero
    val localOffset = IntOffset(
        (r.left - origin.x).roundToInt(),
        (r.top - origin.y).roundToInt(),
    )
    // Chua do duoc overlay + khong co source: chua ve card (tranh nhay 1 frame
    // o goc 0,0); chi hien nen mo dan.
    val cardVisible = sourceRect != null || overlayRect != null
    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { overlayRect = it.boundsInWindow() }
            .background(Color.Black.copy(alpha = scrimAlpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = ::requestClose,
            ),
        contentAlignment = Alignment.TopStart,
    ) {
        if (cardVisible) {
            CameraFeedCard(
                camKey = camKey,
                camName = camName,
                snapshotUrl = secRepo.snapshotUrl(camKey),
                unlocked = unlocked,
                onUnlock = { unlocked = true },
                mjpegUrl = secRepo.mjpegUrl(camKey),
                modifier = Modifier
                    .offset { localOffset }
                    .size(
                        with(density) { r.width.toDp() },
                        with(density) { r.height.toDp() },
                    )
                    // Clip theo bo card de feed 16:9 khong tran ra ngoai
                    // giua chuyen dong (luc card chua dat ti le tu nhien).
                    .clip(RoundedCornerShape(32.dp))
                    // Chan tap xuyen qua the lam dong popup
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            )
        }
    }
}

@Composable
private fun SuggestTipRow(
    tip: SuggestTip,
    done: Boolean,
    /** Nhan rect cua nut action (boundsInWindow) de caller animate tu do (GAP-36, port iOS CameraActionButton). */
    onAction: (Rect?) -> Unit,
    /** % pin de hien icon pin dung muc (cho tip pin yeu). */
    batterySoc: Int? = null,
) {
    val cs = MaterialTheme.colorScheme
    // Vi tri nut action — do bang onGloballyPositioned nhu iOS
    // CameraActionButton (GeometryReader + onAppear/onChange), doc luc tap.
    var btnRect by remember { mutableStateOf<Rect?>(null) }
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
                    // { ... } sau else la block (goi onAction ngay luc compose),
                    // khong phai lambda — phai tra ve lambda de chi goi khi tap.
                    onClick = if (done) {
                        null
                    } else {
                        { onAction(btnRect) }
                    },
                )
                .onGloballyPositioned { btnRect = it.boundsInWindow() }
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
                // (2026-11-17) iOS cardTitle 20pt (HomeSolarCard.swift:24);
                // 0dp he qua (gia tri 39,0 van chi phoi hang).
                style = MaterialTheme.typography.headlineSmall.copy(
                    lineHeight = TextUnit.Unspecified,
                ),
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                "%.1f".format(Locale.US, todayShown),
                // (2026-11-17) lineHeight tu nhien 39,0 thay vi M3 40 (-1,0dp).
                style = MaterialTheme.typography.headlineLarge.copy(
                    lineHeight = TextUnit.Unspecified,
                ),
                fontWeight = FontWeight.SemiBold,
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
                // Nhom 11: hien luon so hom nay tren cot cuoi (khop title 14.5 kWh);
                // WeekChartD goc chi hien so khi cham cot (tooltip) nen cot hom
                // nay trong anh user khong co chi so.
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

