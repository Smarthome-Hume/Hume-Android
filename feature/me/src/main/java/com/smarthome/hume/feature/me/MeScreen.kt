package com.smarthome.hume.feature.me

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smarthome.hume.core.ui.components.M3EIcons
import com.smarthome.hume.core.ui.components.M3EMotion
import com.smarthome.hume.core.ui.components.M3ESwitch
import com.smarthome.hume.core.ui.components.NeighborPressState
import com.smarthome.hume.core.ui.components.rememberHaptic
import com.smarthome.hume.core.ui.components.rememberNeighborPress
import com.smarthome.hume.core.ui.components.MsIcon
import com.smarthome.hume.core.ui.theme.LocalHumeExtraColors
import com.smarthome.hume.core.ui.theme.M3ESeed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Tab Toi — port truc tiep demo v4 rev12 (#page-me):
 * the Dong bo (wavy loading), the Thong bao, Giao dien (dark mode + 8 seeds).
 */

/** Rise entrance theo demo: opacity 0->1 + translateY 22px, .7s emphasized, stagger theo delay. */
@Composable
private fun Modifier.riseEntrance(delayMs: Int): Modifier = composed {
    val density = LocalDensity.current
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(with(density) { 22.dp.toPx() }) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        launch { alpha.animateTo(1f, tween(700, easing = M3EMotion.emphasized)) }
        offsetY.animateTo(0f, tween(700, easing = M3EMotion.emphasized))
    }
    this
        .alpha(alpha.value)
        .offset { IntOffset(0, offsetY.value.roundToInt()) }
}

@Composable
fun MeScreen(
    vm: MeViewModel = viewModel(),
    onViewCamera: () -> Unit = {},
) {
    val syncing by vm.syncing.collectAsState()
    val seed by vm.seed.collectAsState()
    val darkMode by vm.darkMode.collectAsState()
    val haptic = rememberHaptic()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(bottom = 100.dp),
    ) {
        // demo .phdr: padding 12px 2px 6px, cong page padding-top 4px => top 16dp
        Column(
            Modifier
                .riseEntrance(0)
                .padding(start = 2.dp, end = 2.dp, top = 16.dp, bottom = 6.dp),
        ) {
            // demo .phdr h2: 26px/700 ls -.3px ; p: 13px
            Text("Tôi", fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp)
            Text(
                "Đồng bộ, thông báo & hệ thống",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp),
            )
        }

        Box(Modifier.riseEntrance(500)) { SecTitle("Đồng bộ") }
        Box(Modifier.riseEntrance(520)) { SyncCard(syncing = syncing, onSync = vm::doSync) }

        Box(Modifier.riseEntrance(620)) { SecTitle("Trí tuệ nhân tạo") }
        Box(Modifier.riseEntrance(640)) { AiSettingsCard(vm = vm) }

        // demo: .sec.rise cua "Giao dien" khong co animation-delay
        Box(Modifier.riseEntrance(0)) { SecTitle("Giao diện") }
        DarkModeRow(
            darkMode = darkMode,
            onToggle = { haptic(); vm.setDarkMode(it) },
        )
        Text(
            "MÀU CHỦ ĐẠO",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.4.sp,
            modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 12.dp),
        )
        SeedRow(selected = seed, onSelect = { haptic(); vm.setSeed(it) })
        Text(
            // demo .tnote: "primary family <b>và</b> neutrals"
            buildAnnotatedString {
                append("Mỗi seed sinh ra cả dải tonal light/dark — primary family ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("và") }
                append(" neutrals (nền, viền, chữ) đều đổi theo đúng quy tắc M3.")
            },
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            lineHeight = 20.sp,
            modifier = Modifier.padding(start = 4.dp, top = 6.dp),
        )
    }
}

@Composable
private fun SecTitle(title: String) {
    // demo .sec h3: 16px/700 ls -.1px == titleMedium
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.1).sp,
        modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 12.dp),
    )
}

// ---------- dong bo ----------

@Composable
private fun SyncCard(syncing: Boolean, onSync: () -> Unit) {
    // demo .syncrow: margin-top 4px; khong co press scale (chi cursor:pointer)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSync,
            )
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // morphloader: xoay 4.4s + blob morph 4.55s, an khi dong bo xong
            if (syncing) {
                MorphLoader()
                Spacer(Modifier.width(12.dp))
            }
            Text(
                if (syncing) "Đang đồng bộ Home Assistant…"
                else "Đã đồng bộ · vừa xong · 24 thiết bị",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        WavyProgress(syncing = syncing)
    }
}

/** morphloader demo: mspin 4.4s linear infinite + mmorph 4.55s blob morph. */
@Composable
private fun MorphLoader() {
    val primary = MaterialTheme.colorScheme.primary
    val spinT = rememberInfiniteTransition(label = "mspin")
    val rot by spinT.animateFloat(0f, 360f, infiniteRepeatable(tween(4400, easing = LinearEasing)), label = "rot")
    val blobT = rememberInfiniteTransition(label = "mmorph")
    val phase by blobT.animateFloat(0f, 1f, infiniteRepeatable(tween(4550, easing = LinearEasing)), label = "phase")
    Canvas(Modifier.size(38.dp)) {
        rotate(rot) {
            val s = size.minDimension
            // blob: 4 goc bo dao dong lech pha quanh 50% (circle), bien do ~28%
            val p = phase * 2f * PI.toFloat()
            fun r(off: Float): Float =
                (s * 0.5f * (1f + 0.28f * sin(p + off))).coerceIn(0f, s * 0.5f)
            val path = Path().apply {
                addRoundRect(
                    RoundRect(
                        0f, 0f, s, s,
                        CornerRadius(r(0f), r(0f)),
                        CornerRadius(r(2.1f), r(2.1f)),
                        CornerRadius(r(4.2f), r(4.2f)),
                        CornerRadius(r(1.05f), r(1.05f)),
                    )
                )
            }
            drawPath(path, primary)
        }
    }
}

/**
 * Wavy loading bar (demo v4: .wtrack/.wfill):
 * fill 0->100% trong 4s linear 1 LAN khi syncing roi dung o 100%;
 * song sin buoc 40px truot -40px/vong (1.2s) lien tuc.
 */
@Composable
private fun WavyProgress(syncing: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    val fillFrac = remember { Animatable(0f) }
    LaunchedEffect(syncing) {
        if (syncing) {
            fillFrac.snapTo(0f)
            fillFrac.animateTo(1f, tween(4000, easing = LinearEasing))
        }
    }
    val waveT = rememberInfiniteTransition(label = "wave")
    val slide by waveT.animateFloat(
        0f, -40f,
        infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "slide",
    )
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(10.dp),
    ) {
        val waveLen = 40.dp.toPx()
        val amp = 5.dp.toPx()
        val y0 = size.height / 2
        val slidePx = slide.dp.toPx()
        val path = Path().apply {
            moveTo(slidePx, y0)
            var x = slidePx
            var up = false
            while (x < size.width + waveLen) {
                // Q20 0 / T40 — gan dung song sin cua demo
                quadraticTo(x + waveLen / 4, y0 + if (up) -amp else amp, x + waveLen / 2, y0)
                x += waveLen / 2
                up = !up
            }
        }
        clipRect(right = size.width * fillFrac.value) {
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
    val haptic = rememberHaptic()
    // demo .nbtn: press-main flex-grow 1.18 / press-nei flex-grow .93 + scaleX(.95)
    val np = rememberNeighborPress(2, 1.18f, 0.93f)
    val surfaceLow = LocalHumeExtraColors.current.surfaceLow
    Row(
        Modifier
            .fillMaxWidth()
            // demo --shadow light: 0 12px 32px rgba(25,20,18,.10)
            .shadow(
                12.dp,
                RoundedCornerShape(30.dp),
                spotColor = Color(0x1A191412),
                ambientColor = Color(0x1A191412),
            )
            .clip(RoundedCornerShape(30.dp))
            .background(surfaceLow)
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
            MsIcon(
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
                    index = 0,
                    np = np,
                    icon = M3EIcons.Videocam,
                    text = "Xem camera",
                    primary = true,
                    onClick = { haptic(); onViewCamera() },
                )
                NButton(
                    index = 1,
                    np = np,
                    icon = M3EIcons.Close,
                    text = "Bỏ qua",
                    primary = false,
                    // demo: "Bo qua" khong co handler (chi animation press)
                    onClick = { haptic() },
                )
            }
        }
    }
}

/**
 * NButton demo (.nbtn): bo 20px (press 13px), padding 13 all, gap 8px;
 * neighbor-press: nut dang nhan flex-grow 1.18, nut ke co;
 * active: secondary -> surfaceHigh, primary -> primaryContainer/onPrimaryContainer.
 */
@Composable
private fun RowScope.NButton(
    index: Int,
    np: NeighborPressState,
    icon: String,
    text: String,
    primary: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    LaunchedEffect(pressed) { if (pressed) np.press(index) else np.release() }
    val weight by animateFloatAsState(
        np.weightFor(index),
        tween(500, easing = M3EMotion.spring),
        label = "nbtnWeight",
    )
    // demo .nbtn.press-nei: scaleX(.95) — cung nhip voi flex-grow
    val neiScaleX by animateFloatAsState(
        if (np.isNeighbor(index)) 0.95f else 1f,
        tween(500, easing = M3EMotion.spring),
        label = "nbtnNeiScaleX",
    )
    val corner by animateDpAsState(
        if (pressed) 13.dp else 20.dp,
        tween(400, easing = M3EMotion.spring),
        label = "nbtnCorner",
    )
    val bg = when {
        primary && pressed -> MaterialTheme.colorScheme.primaryContainer
        primary -> MaterialTheme.colorScheme.primary
        pressed -> LocalHumeExtraColors.current.surfaceHigh
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val fg = when {
        primary && pressed -> MaterialTheme.colorScheme.onPrimaryContainer
        primary -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        Modifier
            .weight(weight)
            .graphicsLayer { scaleX = neiScaleX }
            .clip(RoundedCornerShape(corner))
            .background(bg)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(13.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MsIcon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = fg)
    }
}

// ---------- giao dien ----------

@Composable
private fun DarkModeRow(darkMode: Boolean?, onToggle: (Boolean) -> Unit) {
    // demo .trow: margin 18px 0 4px, bo 28px, padding 14/16
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 4.dp)
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
        // demo: .tgl luon bat dau OFF (light), khong follow system
        M3ESwitch(
            checked = darkMode ?: false,
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

/**
 * SeedRow demo (.seeds): flex-wrap, gap 14px; seed 58px circle, press scale .86;
 * check hien opacity/scale spring .3s; ring ::after inset -7px border 2px mau seed.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeedRow(selected: M3ESeed, onSelect: (M3ESeed) -> Unit) {
    FlowRow(
        // demo .seeds: padding 2px 4px 8px
        Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        M3ESeed.entries.forEach { s ->
            val color = seedColors[s] ?: Color.Gray
            val on = s == selected
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val scale by animateFloatAsState(
                if (pressed) 0.86f else 1f,
                tween(300, easing = M3EMotion.spring),
                label = "seedScale",
            )
            val checkAlpha by animateFloatAsState(
                if (on) 1f else 0f,
                tween(300, easing = M3EMotion.spring),
                label = "seedCheckAlpha",
            )
            val checkScale by animateFloatAsState(
                if (on) 1f else 0.5f,
                tween(300, easing = M3EMotion.spring),
                label = "seedCheckScale",
            )
            // ring ::after: inset -7px => khung ngoai 72dp, border 2px mau seed
            Box(
                Modifier
                    .size(72.dp)
                    .then(
                        if (on) Modifier.border(2.dp, color, CircleShape)
                        else Modifier
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(58.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(color)
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            onClick = { onSelect(s) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    MsIcon(
                        M3EIcons.Check,
                        contentDescription = seedNames[s],
                        tint = Color.White,
                        modifier = Modifier
                            .size(26.dp)
                            .scale(checkScale)
                            .alpha(checkAlpha),
                    )
                }
            }
        }
    }
}
