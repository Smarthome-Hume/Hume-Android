package com.smarthome.hume.feature.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.model.HomeUiState
import com.smarthome.hume.core.model.SolarDay
import com.smarthome.hume.core.ui.components.M3ECard
import com.smarthome.hume.core.ui.components.M3EIcons
import com.smarthome.hume.core.ui.components.pressMorph
import com.smarthome.hume.core.ui.components.rememberHaptic
import com.smarthome.hume.core.ui.components.Ms
import com.smarthome.hume.core.ui.components.MsIcon
import com.smarthome.hume.core.ui.theme.LocalHumeExtraColors
import java.time.LocalTime
import kotlinx.coroutines.delay

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
    avatarUrl: String = "",
    avatarToken: String = "",
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
                if (avatarUrl.isNotBlank()) {
                    // Avatar HA can Bearer token -> gui Authorization header qua Coil
                    val ctx = androidx.compose.ui.platform.LocalContext.current
                    val req = remember(avatarUrl, avatarToken, ctx) {
                        coil.request.ImageRequest.Builder(ctx)
                            .data(avatarUrl)
                            .apply {
                                if (avatarToken.isNotBlank()) {
                                    addHeader("Authorization", "Bearer $avatarToken")
                                }
                            }
                            .crossfade(true)
                            .build()
                    }
                    coil.compose.AsyncImage(
                        model = req,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    )
                } else {
                    MsIcon(
                        M3EIcons.Person, null,
                        tint = cs.onSurfaceVariant,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(cs.surface),
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E)),
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
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .height(18.dp)
                        .widthIn(min = 18.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(cs.error)
                        .padding(horizontal = 5.dp),
                ) {
                    Text(
                        "${state.notifications.size}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
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
 * Noi dung theo dieu kien thuc te (pin thap / cua mo / nang to);
 * mac dinh (khong co dieu kien) hien noi dung demo "Trời đang nóng dần".
 * Nhan nut: rung nhe + goi onTipAction(key) + chuyen trang thai .done
 * (nen surfaceContainerHigh, chu onSurfaceVariant).
 */
/**
 * The goi y (.sgcard): bo 28px, padding 16px; nen tertiaryContainer;
 * icon auto_awesome 30px; title 14px/700 + sub 12px/500;
 * nut .sgbtn: nen onTertiaryContainer, chu tertiaryContainer, 13px/700, padding 20/12.
 *
 * Noi dung theo dieu kien thuc te (pin thap / cua mo / nang to);
 * mac dinh (khong co dieu kien) hien noi dung demo "Trời đang nóng dần".
 * Nhan nut: rung nhe + goi onTipAction(key) + chuyen trang thai .done
 * (nen surfaceContainerHigh, chu onSurfaceVariant).
 *
 * AI: neu aiState = Loaded -> hien goi y tu AI (co nhan "AI");
 * Loading -> hien "Đang phân tích…"; Unavailable/Idle -> luat co san.
 */
@Composable
fun SuggestCard(
    state: HomeUiState,
    onTipAction: (key: String) -> Unit = {},
    modifier: Modifier = Modifier,
    aiState: AiUiState = AiUiState.Idle,
) {
    data class Tip(val key: String, val title: String, val sub: String, val action: String)
    val tips = buildList {
        if (state.battery.soc in 1..29) add(Tip(
            "battery", "Pin còn ${state.battery.soc}%",
            "Hạn chế tải nặng chờ nắng lên.", "Xem pin"))
        val doors = state.notifications.filter { it.title.contains("Cửa") }
        if (doors.isNotEmpty()) add(Tip(
            "door", doors.first().title, doors.first().body, "Đóng"))
        if (state.solarNowKw > 2.0) add(Tip(
            "ac", "Trời đang nắng to",
            "Bật điều hoà phòng khách 26°?", "Bật"))
    }
    val ruleTip = tips.firstOrNull() ?: Tip(
        "ac", "Trời đang nóng dần", "Bật điều hoà phòng khách 26°?", "Bật")

    // Chon nguon hien thi: AI uu tien, fallback luat
    val aiTips = (aiState as? AiUiState.Loaded)?.tips
    val isAiLoading = aiState == AiUiState.Loading
    val tip = aiTips?.firstOrNull()?.let { Tip(it.key, it.title, it.sub, it.action) }
        ?: ruleTip
    val fromAi = aiTips?.isNotEmpty() == true

    var done by remember(tip.key) { mutableStateOf(false) }
    val haptic = rememberHaptic()
    val cs = MaterialTheme.colorScheme
    M3ECard(
        modifier = modifier.fillMaxWidth(),
        containerColor = cs.tertiaryContainer,
        shape = RoundedCornerShape(28.dp),
        contentPadding = 16.dp,
    ) {
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
                if (isAiLoading) {
                    // Trang thai dang phan tich
                    Text(
                        "Đang phân tích ngôi nhà…",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = cs.onTertiaryContainer,
                    )
                    Text(
                        "AI đang đọc trạng thái thiết bị để đưa gợi ý.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = cs.onTertiaryContainer.copy(alpha = 0.75f),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                } else {
                    if (fromAi) {
                        // Nhan nho "AI" de phan biet nguon
                        Text(
                            "GỢI Ý TỪ AI",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp,
                            color = cs.onTertiaryContainer.copy(alpha = 0.7f),
                        )
                        Spacer(Modifier.height(2.dp))
                    }
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
            }
            Spacer(Modifier.width(12.dp))
            if (!isAiLoading) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .pressMorphCard(
                            pressedScale = 0.9f,
                            corner = 20.dp,
                            pressedCorner = 13.dp,
                            onClick = if (done) null else {
                                {
                                    haptic()
                                    onTipAction(tip.key)
                                    done = true
                                }
                            },
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
    var tickGrow by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(8000)
            tickGrow += 0.06f
        }
    }
    val vals = week.map { it.kwh }
    val todayBase = state.solarTodayKwh?.toFloat() ?: vals.lastOrNull() ?: 0f
    val todayShown = minOf(6.8f, todayBase + tickGrow)

    M3ECard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        contentPadding = 20.dp,
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Điện mặt trời",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface,
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "%.1f".format(todayShown),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp,
                    color = cs.onSurface,
                )
                Text(
                    "kWh",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 3.dp),
                )
            }
        }
        Spacer(Modifier.height(14.dp)) // .stop mb 6 + .solsvg mt 8
        if (vals.isNotEmpty()) {
            SolarBars(
                vals = vals,
                labels = week.map { it.label },
                tickGrow = tickGrow,
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
 * Bieu do cot mo phong SVG demo: viewBox 320x150, PT=14, PB=10, bw=30,
 * maxV=7 CO DINH. SVG width:100% nen toa do X scale theo be rong thuc te
 * (sx = w/320); Y giu nguyen vi cao co dinh 150dp.
 * Mau cot theo GIA TRI TUONG DOI: today = primary; cac ngay khac =
 * lerp(primaryContainer -> primary, v/maxTuan) de nhin ra ngay cao/thap.
 * Cham cot hien tooltip "T2: 4.2 kWh" (surfaceContainerHigh, bo 12px).
 */
@Composable
private fun SolarBars(
    vals: List<Float>,
    labels: List<String>,
    tickGrow: Float,
) {
    val cs = MaterialTheme.colorScheme
    var selected by remember { mutableStateOf<Int?>(null) }
    val shown = vals.mapIndexed { i, v ->
        if (i == vals.lastIndex) minOf(6.8f, v + tickGrow) else v
    }
    val maxV = 7f
    // mau theo gia tri tuong doi so voi max tuan (tranh chia 0)
    val maxWeek = (shown.maxOrNull() ?: 0f).coerceAtLeast(0.01f)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
    ) {
        val w = maxWidth
        val sx = w / 320.dp
        // X(i) = 20 + i*(280/6) (don vi viewBox) -> nhan sx ra dp thuc te
        fun x(i: Int): androidx.compose.ui.unit.Dp = (20f + i * (280f / 6f)).dp * sx
        fun y(v: Float): androidx.compose.ui.unit.Dp = 14.dp + 126.dp * (1f - (v / maxV).coerceIn(0f, 1f))
        val bw = 30.dp * sx
        shown.forEachIndexed { i, v ->
            val today = i == shown.lastIndex
            val top = y(v)
            val h = (140.dp - top).coerceAtLeast(4.dp)
            // today: primary dac; ngay khac: dam nhat theo gia tri tuong doi
            val barColor = if (today) cs.primary
            else lerp(cs.primaryContainer, cs.primary, (v / maxWeek).coerceIn(0f, 1f) * 0.85f)
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
                softWrap = false,
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
