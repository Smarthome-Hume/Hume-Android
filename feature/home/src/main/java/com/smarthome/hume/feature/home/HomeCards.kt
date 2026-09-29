package com.smarthome.hume.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.smarthome.hume.core.ui.components.pressMorph
import com.smarthome.hume.core.ui.components.rememberHaptic
import com.smarthome.hume.core.ui.components.Ms
import com.smarthome.hume.core.ui.components.MsIcon
import com.smarthome.hume.core.ui.theme.LocalHumeExtraColors
import java.time.LocalTime
import kotlinx.coroutines.launch

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
                // Badge: rong co gian theo so chu so (1-2 chu so deu can giua)
                val count = state.notifications.size.coerceAtMost(99)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .widthIn(min = 18.dp)
                        .height(18.dp)
                        .clip(CircleShape)
                        .background(cs.error)
                        .padding(horizontal = 4.dp),
                ) {
                    Text(
                        "$count",
                        fontSize = 11.sp,
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
    val tips = buildSuggestTips(state)
    if (tips.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { tips.size })
    val doneMap = remember { mutableStateMapOf<String, Boolean>() }
    val haptic = rememberHaptic()
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme
    // List rut ngan lai (vd cua da dong): giu currentPage trong bien.
    LaunchedEffect(tips.size) {
        if (pagerState.currentPage >= tips.size) pagerState.scrollToPage(tips.size - 1)
    }
    M3ECard(
        modifier = modifier.fillMaxWidth(),
        containerColor = cs.tertiaryContainer,
        shape = RoundedCornerShape(28.dp),
        contentPadding = 16.dp,
    ) {
        Column {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
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
            SolarBars(
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
 * Bieu do cot: chieu cao ty le voi gia tri / maxValue cua tuan.
 * Neu maxValue=0 thi cot cao 0 (chi hien cham 4dp toi thieu).
 * Mau cot theo GIA TRI TUONG DOI: today = primary; cac ngay khac =
 * lerp(primaryContainer -> primary, v/maxValue) de nhin ra ngay cao/thap.
 */
@Composable
private fun SolarBars(
    vals: List<Float>,
    labels: List<String>,
) {
    val cs = MaterialTheme.colorScheme
    var selected by remember { mutableStateOf<Int?>(null) }
    val shown = vals
    // maxValue that cua tuan (tranh chia 0)
    val maxValue = (shown.maxOrNull() ?: 0f).coerceAtLeast(0.01f)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
    ) {
        val w = maxWidth
        val sx = w / 320.dp
        // X(i) = 20 + i*(280/6) (don vi viewBox) -> nhan sx ra dp thuc te
        fun x(i: Int): androidx.compose.ui.unit.Dp = (20f + i * (280f / 6f)).dp * sx
        // Chieu cao = (v / maxValue) * 126dp (vung ve tu y=14 den y=140)
        fun y(v: Float): androidx.compose.ui.unit.Dp = 14.dp + 126.dp * (1f - (v / maxValue).coerceIn(0f, 1f))
        val bw = 30.dp * sx
        shown.forEachIndexed { i, v ->
            val today = i == shown.lastIndex
            val top = y(v)
            val h = (140.dp - top).coerceAtLeast(4.dp)
            // today: primary dac; ngay khac: dam nhat theo gia tri tuong doi
            val barColor = if (today) cs.primary
            else lerp(cs.primaryContainer, cs.primary, (v / maxValue).coerceIn(0f, 1f) * 0.85f)
            Box(
                modifier = Modifier
                    .offset(x = x(i) - bw / 2, y = top)
                    .width(bw)
                    .height(h)
                    .clip(RoundedCornerShape(50))
                    .background(barColor)
                    .pointerInput(i) {
                        detectTapGestures(onTap = {
                            selected = if (selected == i) null else i
                        })
                    },
            )
            // Tooltip phia tren cot duoc cham
            if (selected == i) {
                Box(
                    modifier = Modifier
                        .offset(x = x(i) - 60.dp, y = (top - 40.dp).coerceAtLeast(0.dp))
                        .width(120.dp)
                        .heightIn(min = 24.dp)
                        .shadow(6.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(cs.surfaceContainerHigh)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${labels.getOrElse(i) { "" }}: ${"%.1f".format(v)} kWh",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(2.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        labels.forEachIndexed { i, l ->
            val today = i == labels.lastIndex
            Text(
                l,
                fontSize = 10.5.sp,
                fontWeight = if (today) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = if (today) cs.primary else cs.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
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
fun SolarLiveCard(state: HomeUiState, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    M3ECard(
        modifier = modifier.fillMaxWidth(),
        containerColor = cs.tertiaryContainer,
        shape = RoundedCornerShape(32.dp),
        contentPadding = 0.dp,
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
                color = cs.onTertiaryContainer,
                modifier = Modifier.size(90.dp, 34.dp),
            )
        }
    }
}

/** Sparkline mo phong path SVG demo (viewBox 90x34). */
@Composable
private fun Sparkline(color: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val sx = size.width / 90f
        val sy = size.height / 34f
        fun X(v: Float) = v * sx
        fun Y(v: Float) = v * sy
        val path = Path().apply {
            moveTo(X(2f), Y(28f))
            cubicTo(X(15f), Y(26f), X(20f), Y(12f), X(32f), Y(14f))
            // S 50 26, 62 18: phan xa (20,12) qua (32,14) -> (44,16)
            cubicTo(X(44f), Y(16f), X(50f), Y(26f), X(62f), Y(18f))
            // S 80 6, 88 8: phan xa (50,26) qua (62,18) -> (74,10)
            cubicTo(X(74f), Y(10f), X(80f), Y(6f), X(88f), Y(8f))
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
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
