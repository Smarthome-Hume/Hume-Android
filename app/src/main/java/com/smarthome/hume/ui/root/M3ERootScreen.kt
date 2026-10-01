package com.smarthome.hume.ui.root

import android.graphics.Bitmap
import android.view.View
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.drawToBitmap
import com.smarthome.hume.brief.BriefEdgeHost
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smarthome.hume.core.data.HumeGraph
import com.smarthome.hume.core.ha.HistoryFetcher
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.HumeConfig
import com.smarthome.hume.core.model.HumeTab
import com.smarthome.hume.core.storage.HumeSettings
import com.smarthome.hume.core.storage.SettingsStore
import com.smarthome.hume.core.ui.components.M3EMotion
import com.smarthome.hume.core.ui.components.rememberHaptic
import com.smarthome.hume.core.ui.components.rememberNeighborPress
import com.smarthome.hume.core.ui.theme.LocalHumeExtraColors
import com.smarthome.hume.core.ui.components.Ms
import com.smarthome.hume.core.ui.components.MsIcon
import com.smarthome.hume.core.ui.avatar.AvatarViewerOverlay
import com.smarthome.hume.core.ui.avatar.AvatarViewerRequest
import com.smarthome.hume.feature.home.CameraPopupOverlay
import com.smarthome.hume.feature.home.CameraPopupRequest
import com.smarthome.hume.core.ui.theme.HumeM3ETheme
import com.smarthome.hume.core.ui.theme.M3ESeed
import com.smarthome.hume.feature.home.HomeScreen
import com.smarthome.hume.feature.home.ChartHistorySeries
import com.smarthome.hume.feature.energy.EnergyScreen as M3EEnergyScreen
import com.smarthome.hume.feature.me.MeScreen
import com.smarthome.hume.feature.security.SecurityScreen as M3ESecurityScreen
import kotlinx.coroutines.launch

private data class NavItem(val tab: HumeTab, val icon: String)

private val navItems = listOf(
    NavItem(HumeTab.Home, Ms.home),
    NavItem(HumeTab.Energy, Ms.bolt),
    NavItem(HumeTab.Security, Ms.shield),
    NavItem(HumeTab.Profile, Ms.person),
)

/**
 * Root M3E: theme moi + navbar highlight full-item primaryContainer.
 * Tab Nha dung feature:home moi; cac tab con lai tam dung man hinh cu
 * (se port dan o cac cum tiep theo).
 */
@Composable
fun M3ERootScreen(
    settingsStore: SettingsStore,
    ha: HomeAssistantRepository,
    settings: HumeSettings,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val themeSettings by HumeGraph.get().themeStore.settings.collectAsState(
        initial = com.smarthome.hume.core.datastore.ThemeSettings(),
    )
    val seed = runCatching { M3ESeed.valueOf(themeSettings.seedName) }.getOrDefault(M3ESeed.Cam)
    val darkTheme = themeSettings.darkMode ?: isSystemInDarkTheme()
    // (2026-10-01, fix crash khi ap dung mau tuy chinh: dung cach tach ARGB an toan)
    val customColor = themeSettings.customColor?.let { argb ->
        runCatching {
            androidx.compose.ui.graphics.Color(
                red = ((argb shr 16) and 0xFF) / 255f,
                green = ((argb shr 8) and 0xFF) / 255f,
                blue = (argb and 0xFF) / 255f,
                alpha = ((argb shr 24) and 0xFF) / 255f,
            )
        }.getOrNull()
    }
    HumeM3ETheme(seed = seed, darkTheme = darkTheme, customSeedColor = customColor, fontFamily = themeSettings.fontFamily) {
        var selected by rememberSaveable { mutableIntStateOf(0) }
        // Deep-link "Xem pin" tu the goi y trang Nha -> tab Nang luong + cuon toi the pin.
        var energyDeepLink by remember { mutableStateOf<String?>(null) }
        // Trang Brief: mo bang vuot ngang sang phai TREN THANH NAVBAR
        // (2026-09-30, user chon thay cho vuot canh trai de tranh nham voi
        // system back gesture cua thiet bi).
        var briefOpen by rememberSaveable { mutableStateOf(false) }
        // Overlay toan man hinh (viewer avatar / popup camera): ve o tang root,
        // TREN navbar, de lop mo + blur phu ca navbar chu khong chi vung content.
        // Nhan request tu HomeScreen qua callback (HomeScreen khong tu ve overlay).
        val rootView = LocalView.current
        var bgSnapshot by remember { mutableStateOf<ImageBitmap?>(null) }
        var avatarViewer by remember { mutableStateOf<AvatarViewerRequest?>(null) }
        var camPopup by remember { mutableStateOf<CameraPopupRequest?>(null) }
        // Rect avatar header (boundsInWindow): de xoa "bong ma" avatar khoi
        // anh nen ca khi mo popup camera, khong chi avatar viewer.
        // (2026-09-30) Dung ref THUONG thay vi state: onGloballyPositioned
        // chay moi frame khi scroll -> set state o day tung recompose
        // M3ERootScreen + HomeScreen moi frame = khựng. Rect chi can luc tap.
        val headerAvatarRectRef = remember { object { var rect: Rect? = null } }
        val overlayOpen = avatarViewer != null || camPopup != null
        // Chup nen 1 lan truoc khi mo overlay roi hien anh tinh da blur san:
        // tranh blur live toan man hinh moi frame (nguyen nhan chinh gay khựng
        // khi mo/dong popup, vi content ben duoi co nhieu thu tick live nhu
        // snapshot camera 3s, thiet bi 2.8s...). Anh thu nho 1/4 cho blur re.
        // avatarRect (toa do window, px): vung avatar nho o header. Blur chi lam
        // nhoe chu khong xoa duoc phan tu nho tuong phan cao -> con "bong ma"
        // nhin xuyen qua lop dim; to de vung nay bang mau nen xung quanh
        // NGAY TREN ANH CHUP (truoc khi blur) thi bong ma bien mat han.
        fun captureSnapshot(avatarRect: Rect?): ImageBitmap? = try {
            val v = rootView
            if (!v.isLaidOut || v.width <= 0 || v.height <= 0) {
                null
            } else {
                val full = v.drawToBitmap(Bitmap.Config.ARGB_8888)
                val small = Bitmap.createScaledBitmap(
                    full,
                    (full.width / 4).coerceAtLeast(1),
                    (full.height / 4).coerceAtLeast(1),
                    true,
                )
                if (small !== full) full.recycle()
                if (avatarRect != null) eraseAvatarGhost(small, v, avatarRect)
                small.asImageBitmap()
            }
        } catch (_: Exception) {
            null
        }
        // Navbar NOI tren be mat trang: dung Box overlay thay vi Scaffold bottomBar
        // (Scaffold bottomBar van giu cho layout). Content full-bleed, navbar noi phia tren.
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            // Text() khong truyen color mac dinh lay LocalContentColor (den) vi
            // content khong nam trong Surface; cung cap onSurface (dark-aware)
            // cho ca 4 tab de chu khong bi chim trong che do toi.
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onSurface,
            ) {
            Box(Modifier.fillMaxSize()) {
                when (navItems[selected].tab) {
                    HumeTab.Home -> HomeScreen(
                        onOpenSecurity = { selected = 2 },
                        onOpenEnergy = { selected = 1 },
                        onOpenEnergyBattery = {
                            selected = 1
                            energyDeepLink = "battery"
                        },
                        onOpenAvatarViewer = { req ->
                            bgSnapshot = captureSnapshot(req.targetRect)
                            avatarViewer = req
                        },
                        onOpenCameraPopup = { req ->
                            // Xoa bong ma avatar header khoi nen (giong viewer)
                            // de avatar khong hien mo mo sau lop mo + blur.
                            bgSnapshot = captureSnapshot(headerAvatarRectRef.rect)
                            camPopup = req
                        },
                        onAvatarPositioned = { headerAvatarRectRef.rect = it },
                        loadChartHistory = { series, startMs, endMs ->
                            val entityId = when (series) {
                                ChartHistorySeries.BatterySoc -> HumeConfig.BATTERY_SOC
                                ChartHistorySeries.BatteryPower -> HumeConfig.BATTERY_POWER
                                ChartHistorySeries.SolarPower -> HumeConfig.PV_POWER
                            }
                            HistoryFetcher.fetchRange(entityId, startMs, endMs)
                                .map { it.timeMs to it.value }
                        },
                    )
                    HumeTab.Energy -> M3EEnergyScreen(
                        deepLink = energyDeepLink,
                        onDeepLinkConsumed = { energyDeepLink = null },
                    )
                    HumeTab.Security -> M3ESecurityScreen(
                        onDownloadClip = { clip ->
                            val path = clip.clipPath
                            if (path == null) {
                                android.widget.Toast.makeText(
                                    context, "Clip chưa sẵn sàng",
                                    android.widget.Toast.LENGTH_SHORT,
                                ).show()
                            } else {
                                val file = java.io.File(path)
                                if (!file.exists()) {
                                    android.widget.Toast.makeText(
                                        context, "Không tìm thấy file clip",
                                        android.widget.Toast.LENGTH_SHORT,
                                    ).show()
                                } else {
                                    android.widget.Toast.makeText(
                                        context, "Đang lưu...",
                                        android.widget.Toast.LENGTH_SHORT,
                                    ).show()
                                    coroutineScope.launch {
                                        // Ten file an toan: dateLabel "dd/MM" va timeLabel
                                        // "HH:mm" chua / va : lam hong MediaStore.
                                        val safeName =
                                            "Hume_${clip.dateLabel}_${clip.timeLabel}.mp4"
                                                .replace("/", "-")
                                                .replace(":", "-")
                                                .replace(" ", "_")
                                        val uri = com.smarthome.hume.core.frigate.saveVideoToGallery(
                                            context,
                                            file,
                                            safeName,
                                        )
                                        android.widget.Toast.makeText(
                                            context,
                                            if (uri != null) "Đã lưu vào Thư viện" else "Lưu thất bại",
                                            android.widget.Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                }
                            }
                        },
                        onShareClip = { clip ->
                            val path = clip.clipPath
                            val file = path?.let { java.io.File(it) }?.takeIf { it.exists() }
                            if (file == null) {
                                android.widget.Toast.makeText(
                                    context, "Không tìm thấy file clip",
                                    android.widget.Toast.LENGTH_SHORT,
                                ).show()
                            } else {
                                com.smarthome.hume.core.frigate.shareVideo(context, file)
                            }
                        },
                    )
                    HumeTab.Profile -> MeScreen(onViewCamera = { selected = 2 })
                }
            }
            } // CompositionLocalProvider(LocalContentColor)
            // Lop phu gradient mo dan sau status bar (2026-09-30, user yeu
            // cau theo kieu app Muse): dinh dac nhat (alpha 0.95, van nhin
            // lo mo thay noi dung ben duoi) -> trong suot dan xuong duoi.
            // Noi dung scroll chui xuong duoi va mo dan lien mach, khong co
            // duong cat cung. Khong chan touch (khong co clickable).
            // Redesign 2026-09-30 (feedback): dai mo CHI phu vung status bar,
            // nam TREN header cua app — khong tran xuong che header nhu ban cu
            // (statusBar + 80dp phu ca header search "Tim thiet bi").
            // Tren content 4 tab, duoi navbar/overlay.
            val fadeTop = WindowInsets.statusBars.asPaddingValues()
                .calculateTopPadding() + 6.dp
            val scrim = MaterialTheme.colorScheme.surface
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(fadeTop)
                    .background(
                        Brush.verticalGradient(
                            0.0f to scrim.copy(alpha = 0.95f),
                            0.6f to scrim.copy(alpha = 0.4f),
                            1.0f to scrim.copy(alpha = 0.0f),
                        ),
                    ),
            )
            M3ENavBar(
                selected = selected,
                onSelect = { selected = it },
                onSwipeOpenBrief = { briefOpen = true },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
            // Lop nen mo blur + overlay: ve SAU navbar de phu toan man hinh.
            // Dung anh chup tinh (khong blur live) nen mo/dong/keo khong khựng.
            if (overlayOpen) {
                val bmp = bgSnapshot
                if (bmp != null) {
                    Image(
                        bitmap = bmp,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(24.dp),
                    )
                } else {
                    // Chup that bai: fallback dim don gian, overlay van dung duoc
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)),
                    )
                }
            }
            avatarViewer?.let { req ->
                AvatarViewerOverlay(
                    name = req.name,
                    avatar = req.avatar,
                    haAvatarUrl = req.haAvatarUrl,
                    targetRect = req.targetRect,
                    onDismiss = { avatarViewer = null; bgSnapshot = null },
                )
            }
            camPopup?.let { req ->
                CameraPopupOverlay(
                    camKey = req.camKey,
                    camName = req.camName,
                    onDismiss = { camPopup = null; bgSnapshot = null },
                )
            }
            // Trang Brief sang: mo bang vuot ngang sang phai tren thanh navbar,
            // dong bang vuot phai->trai / nut dong / back. Lop tren cung:
            // phu ca navbar khi mo.
            BriefEdgeHost(
                open = briefOpen,
                onOpenChange = { briefOpen = it },
            )
        }
    }
}

/**
 * Xoa "bong ma" avatar header khoi anh nen: to de hinh tron tai vi tri avatar
 * bang mau trung binh cua vung xung quanh. Anh dang o ti le 1/4 nen chi vai
 * nghin pixel — re, khong anh huong toc do mo popup. Sau do blur + dim thi
 * vung nay hoa lan hoan toan vao nen, khong con nhin thay avatar cu.
 */
private fun eraseAvatarGhost(small: Bitmap, view: View, rect: Rect) {
    val loc = IntArray(2)
    view.getLocationInWindow(loc)
    val scale = small.width.toFloat() / view.width.toFloat()
    val cx = ((rect.center.x - loc[0]) * scale).toInt()
    val cy = ((rect.center.y - loc[1]) * scale).toInt()
    if (cx !in 0 until small.width || cy !in 0 until small.height) return
    val rad = (rect.width * scale / 2f + 8).toInt().coerceAtLeast(4)
    // Mau trung binh tu 24 diem tren vong tron quanh avatar
    val samples = 24
    val ringR = rad + 10
    var rSum = 0L
    var gSum = 0L
    var bSum = 0L
    for (i in 0 until samples) {
        val a = i * 2.0 * Math.PI / samples
        val x = (cx + ringR * kotlin.math.cos(a)).toInt().coerceIn(0, small.width - 1)
        val y = (cy + ringR * kotlin.math.sin(a)).toInt().coerceIn(0, small.height - 1)
        val p = small.getPixel(x, y)
        rSum += android.graphics.Color.red(p)
        gSum += android.graphics.Color.green(p)
        bSum += android.graphics.Color.blue(p)
    }
    val fill = android.graphics.Color.rgb(
        (rSum / samples).toInt(),
        (gSum / samples).toInt(),
        (bSum / samples).toInt(),
    )
    val r2 = rad * rad
    for (dy in -rad..rad) {
        for (dx in -rad..rad) {
            if (dx * dx + dy * dy > r2) continue
            val x = cx + dx
            val y = cy + dy
            if (x in 0 until small.width && y in 0 until small.height) {
                small.setPixel(x, y, fill)
            }
        }
    }
}

/**
 * Navbar M3E theo demo v4 (.nav/.navit): FLOATING — cach 2 canh 16dp,
 * cach day 20dp, bo 34dp, nen surfaceLowest 98% + shadow (gan nhu dac,
 * M3E khong co thiet ke trong suot);
 * item chon: pill 64x32 primaryContainer CHI OM ICON (no spring),
 * icon outlined (27dp khi chon), label onSurface dam khi chon;
 * neighbor-press: item dang nhan no rong (spring), 2 item ke co lai;
 * :active nen surfaceContainer. Backdrop blur bo qua (ghi nhan gioi han).
 */
@Composable
private fun M3ENavBar(
    selected: Int,
    onSelect: (Int) -> Unit,
    onSwipeOpenBrief: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val extra = LocalHumeExtraColors.current
    val pill = RoundedCornerShape(34.dp)
    val np = rememberNeighborPress(navItems.size, 1.18f, 0.93f)
    val haptic = rememberHaptic()
    // Vuot ngang sang phai tren navbar -> mo trang Brief (2026-09-30, user
    // chon thay cho vuot canh trai). Nguong 80dp de tap lech tay khong mo
    // nham. Chi nhan vuot sang phai (cung huong truot vao cua Brief).
    // Luu y: vuot bat dau tren mot item van kich hoat onPress cua item truoc
    // (haptic + press spring), nhung tryAwaitRelease tra ve false khi drag
    // vuot slop ngang -> np.release() tu huy, khong ket dinh trang thai.
    val openBriefState = rememberUpdatedState(onSwipeOpenBrief)
    val swipeThresholdPx = with(LocalDensity.current) { 80.dp.toPx() }
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
            .pointerInput(Unit) {
                var acc = 0f
                detectHorizontalDragGestures(
                    onDragStart = { acc = 0f },
                    onDragEnd = { acc = 0f },
                    onDragCancel = { acc = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        if (dragAmount > 0) {
                            acc += dragAmount
                            if (acc > swipeThresholdPx) {
                                openBriefState.value()
                                acc = 0f
                            }
                        } else acc = 0f
                    },
                )
            },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .shadow(12.dp, pill)
                .clip(pill)
                .background(extra.surfaceLowest.copy(alpha = 0.82f))
                .padding(10.dp),
        ) {
            navItems.forEachIndexed { i, item ->
                val isSel = i == selected
                val pressed = np.pressedIndex == i
                // Selected flex-grow + neighbor shrink, mo phong .navit flex-grow transition
                val weight by animateFloatAsState(
                    targetValue = np.weightFor(i),
                    animationSpec = tween(500, easing = M3EMotion.spring),
                    label = "navW$i",
                )
                val iconSize by animateDpAsState(
                    targetValue = if (isSel) 27.dp else 24.dp,
                    animationSpec = tween(300, easing = M3EMotion.spring),
                    label = "navI$i",
                )
                // Pill active chi om icon: hien/spring theo chon, an khi khong chon
                val pillOn = isSel || pressed
                val pillScale by animateFloatAsState(
                    targetValue = if (pillOn) 1f else 0.45f,
                    animationSpec = tween(450, easing = M3EMotion.spring),
                    label = "navP$i",
                )
                val pillAlpha by animateFloatAsState(
                    targetValue = if (pillOn) 1f else 0f,
                    animationSpec = tween(250),
                    label = "navPA$i",
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(weight)
                        .pointerInput(i) {
                            detectTapGestures(
                                onPress = {
                                    haptic()
                                    np.press(i)
                                    tryAwaitRelease()
                                    np.release()
                                },
                                onTap = { onSelect(i) },
                            )
                        }
                        .padding(vertical = 4.dp),
                ) {
                    // Nen active bao ca icon lan chu (dang ban dau: highlight full-item).
                    // Icon luon hien; active -> fill dac.
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // Pill nen: scale/fade spring theo chon
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = pillScale
                                    scaleY = pillScale
                                    alpha = pillAlpha
                                }
                                .matchParentSize()
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    when {
                                        isSel -> cs.primaryContainer
                                        pressed -> cs.surfaceContainer
                                        else -> Color.Transparent
                                    },
                                ),
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
                        ) {
                            MsIcon(
                                item.icon, contentDescription = item.tab.label,
                                tint = if (isSel) cs.onPrimaryContainer
                                else cs.onSurfaceVariant,
                                filled = isSel,
                                modifier = Modifier.size(iconSize),
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                item.tab.label,
                                style = MaterialTheme.typography.labelSmall,
                                lineHeight = 13.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isSel) cs.onSurface
                                else cs.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
