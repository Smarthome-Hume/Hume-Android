package com.smarthome.hume.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import com.smarthome.hume.core.model.HomeNotification
import com.smarthome.hume.core.model.HomeUiState
import com.smarthome.hume.core.model.ConnectionState
import com.smarthome.hume.core.model.SolarDay
import com.smarthome.hume.core.ui.components.blink
import com.smarthome.hume.core.ui.components.M3ECard
import com.smarthome.hume.core.ui.components.M3EIcons
import com.smarthome.hume.core.ui.components.WeekChartD
import com.smarthome.hume.core.ui.components.pressMorph
import com.smarthome.hume.core.ui.components.rememberHaptic
import com.smarthome.hume.core.ui.components.Ms
import com.smarthome.hume.core.ui.components.M3EMotion
import com.smarthome.hume.core.ui.components.MsIcon
import com.smarthome.hume.core.ui.theme.LocalHumeExtraColors
import java.time.LocalTime
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
        Box(Modifier.size(55.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(extra.surfaceHighest),
            ) {
                if (avatarUrl.isNullOrBlank()) {
                    MsIcon(
                        M3EIcons.Person, null,
                        tint = cs.onSurfaceVariant,
                        modifier = Modifier.size(28.dp),
                    )
                } else {
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
                }
            }
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
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.2).sp,
                color = cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = greeting(),
                fontSize = 13.sp,
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
                        .background(cs.error),
                ) {
                    Text(
                        "$count",
                        fontSize = 10.sp,
                        lineHeight = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
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
 * Trang thai .done: opacity .6, text "Đã bật" (theo HTML that).
 */
internal data class SuggestTip(
    val key: String,
    val title: String,
    val sub: String,
    val action: String,
)

/** Dung list goi y urgent tu state; dung chung cho SuggestCard + HomeScreen. */
internal fun buildSuggestTips(state: HomeUiState): List<SuggestTip> = buildList {
    if (state.battery.soc in 1..29) add(SuggestTip(
        "battery", "Pin còn ${state.battery.soc}%",
        "Hạn chế tải nặng chờ nắng lên.", "Xem pin"))
    val doors = state.notifications.filter { it.title.contains("Cửa") }
    if (doors.isNotEmpty()) add(SuggestTip(
        "door", doors.first().title, doors.first().body, "Đóng"))
    val motions = state.notifications.filter { it.title == "Phát hiện chuyển động" }
    if (motions.isNotEmpty()) add(SuggestTip(
        "camera", "Phát hiện chuyển động", motions.first().body, "Xem camera"))
}

@Composable
fun SuggestCard(
    state: HomeUiState,
    onTipAction: (key: String) -> Unit = {},
    onBatteryDetail: () -> Unit = {},
    onOpenSecurity: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val allTips = buildSuggestTips(state)
    // Goi y da bi user xoa: luu key -> noi dung luc xoa; hien lai neu co su kien moi (noi dung doi)
    val dismissed = remember { mutableStateMapOf<String, String>() }
    val tips = allTips.filter { tip ->
        val d = dismissed[tip.key]
        d == null || d != tip.title + "|" + tip.sub
    }
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
                    onAction = {
                        haptic()
                        when (tip.key) {
                            "battery" -> onBatteryDetail()
                            "camera" -> onOpenSecurity()
                            "door" -> doneMap[tip.key] = true
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

@Composable
private fun SuggestTipRow(
    tip: SuggestTip,
    done: Boolean,
    onAction: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 2.dp),
    ) {
        MsIcon(
            Ms.auto_awesome, null,
            tint = cs.onTertiaryContainer,
            modifier = Modifier.size(30.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                tip.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = cs.onTertiaryContainer,
            )
            Text(
                tip.sub,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = cs.onTertiaryContainer.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .pressMorphCard(
                    pressedScale = 0.9f,
                    corner = 20.dp,
                    pressedCorner = 13.dp,
                    onClick = if (done) null else onAction,
                )
                // .sgbtn: nen onTertiaryContainer; :disabled{opacity:.6}
                .alpha(if (done) 0.6f else 1f)
                .background(cs.onTertiaryContainer)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Text(
                if (done) "Đã bật" else tip.action,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
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
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                "%.1f".format(todayShown),
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.3).sp,
                color = cs.onSurface,
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                "kWh",
                fontSize = 13.sp,
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
                fontSize = 12.sp,
                color = cs.onSurfaceVariant,
            )
        }
    }
}

/**
 * The nho cong suat dang phat (.solar): tertiaryContainer, bo 32px,
 * padding 16px 18px; icon nen trang 35% (.sicon 48px, icon 26px);
 * sub 12px/500; sparkline SVG 90x34 ben phai.
 */
@Composable
fun SolarLiveCard(state: HomeUiState, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val cs = MaterialTheme.colorScheme
    // Buffer lich su cong suat PV (tong PV1+PV2 tu sensor.solis_s6_eh1p_total_pv_power_2),
    // lay mau moi khi solarNowKw thay doi, giu 30 diem gan nhat de ve line realtime.
    var history by remember { mutableStateOf(listOf<Double>()) }
    LaunchedEffect(state.solarNowKw) {
        val v = state.solarNowKw.coerceAtLeast(0.0)
        history = (history + v).takeLast(30)
    }
    M3ECard(
        modifier = modifier.fillMaxWidth(),
        containerColor = cs.tertiaryContainer,
        shape = RoundedCornerShape(32.dp),
        contentPadding = 0.dp,
        onClick = onClick,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.35f)),
            ) {
                MsIcon(
                    M3EIcons.Solar, null,
                    tint = cs.onTertiaryContainer,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Điện mặt trời",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = cs.onTertiaryContainer,
                )
                Text(
                    "Đang phát · ${"%.1f".format(state.solarNowKw)} kW",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = cs.onTertiaryContainer.copy(alpha = 0.75f),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Sparkline(
                values = history,
                color = cs.onTertiaryContainer,
                modifier = Modifier.size(90.dp, 34.dp),
            )
        }
    }
}

/** Sparkline ve line that tu lich su cong suat PV (realtime, 30 diem gan nhat). */
@Composable
private fun Sparkline(values: List<Double>, color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val maxV = (values.maxOrNull() ?: 1.0).coerceAtLeast(0.01)
        val n = values.size
        val stepX = size.width / (n - 1).coerceAtLeast(1)
        val path = Path().apply {
            values.forEachIndexed { i, v ->
                val x = i * stepX
                // y dao nguoc: gia tri cao -> len tren; padding 2px tren/duoi
                val y = size.height - 2.dp.toPx() -
                    (v / maxV).toFloat() * (size.height - 4.dp.toPx())
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
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
                MsIcon(
                    M3EIcons.Bell,
                    contentDescription = null,
                    tint = cs.onPrimaryContainer,
                    modifier = Modifier.size(28.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    latest.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${latest.body} · ${latest.timeText}",
                    fontSize = 12.sp,
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
                        fontSize = 12.sp,
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
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
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
