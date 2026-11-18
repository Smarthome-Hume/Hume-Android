package com.smarthome.hume.ui.root

import android.graphics.Bitmap
import android.os.Build
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
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
import com.smarthome.hume.core.ui.components.M3EIcons
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
import com.smarthome.hume.feature.energy.powerwall.PowerwallScreen
import com.smarthome.hume.feature.me.MeScreen
import com.smarthome.hume.feature.security.SecurityScreen as M3ESecurityScreen
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.json.contentOrNull
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

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
    // RoomStore cho Quan ly phong (port iOS RoomManagementView)
    val roomStoreAdapter = remember(context, coroutineScope) {
        com.smarthome.hume.data.RoomStoreAdapter(
            real = (context.applicationContext as com.smarthome.hume.HumeApplication).roomStore,
            scope = coroutineScope,
        )
    }
    // (GAP-37) Vuot mep trai mo Brief nam o Box content ben duoi
    // (briefEdgeSwipe), khong phai o day.
    // Entities cho EntityPicker (port iOS SensorPickerView)
    val pickerEntities = remember(ha) {
        ha.entities.map { map ->
            map.mapValues { (id, e) ->
                val friendly = e.attributes["friendly_name"]
                    ?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull }
                    ?: id
                com.smarthome.hume.feature.home.PickerEntity(id, friendly)
            }
        }.stateIn(
            coroutineScope,
            kotlinx.coroutines.flow.SharingStarted.Eagerly,
            emptyMap(),
        )
    }
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
        // (GAP-37 + N-05) Trang mo phong Powerwall 3D dang mo — chan vuot mep mo Brief
        // (port iOS M3ERootView.swift:47-48,52-66: if powerwallShown { return }).
        // (N-05) Root Android KHONG co NavHost (tab chuyen bang `when (selected)`
        // ben duoi, khong phai navigation-compose) nen trang Powerwall duoc mo
        // bang co boolean nay — tuong duong iOS `.navigationDestination(
        // isPresented: showPowerwall)` (M3EHomeView.swift:147,
        // M3EEnergyView.swift:227): the pin / node pin goi onOpenPowerwall
        // -> set true; pill title "Powerwall" hoac back he thong -> false.
        var powerwallShown by remember { mutableStateOf(false) }
        // (v81 muc 2, watched theo tab) Noi tab dang mo + trang Powerwall
        // vao repo: AppHomeRepository collect ha.activeTab /
        // ha.powerwallOpen de rebuild watched set (alwaysOn ∪ tab mo,
        // + 205 entity BMS chi khi Powerwall mo). Chi them wiring tab —
        // KHONG dung hinh hoc/mau navbar (worker khac so huu vung do).
        // LaunchedEffect chay ngay lan dau (selected=Home, powerwall=false
        // -> setActiveTab/setPowerwallOpen deu no-op vi da dung gia tri).
        androidx.compose.runtime.LaunchedEffect(selected) {
            ha.setActiveTab(navItems[selected].tab)
        }
        androidx.compose.runtime.LaunchedEffect(powerwallShown) {
            ha.setPowerwallOpen(powerwallShown)
        }
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
            Box(
                Modifier
                    .fillMaxSize()
                    // (GAP-37) Vuot mep trai mo Brief (port iOS M3ERootView
                    // briefOpenGesture); GIU NGUYEN vuot tren navbar
                    // (M3ENavBar onSwipeOpenBrief).
                    .briefEdgeSwipe(
                        enabled = !powerwallShown,
                        onOpenBrief = { briefOpen = true },
                    ),
            ) {
                when (navItems[selected].tab) {
                    HumeTab.Home -> HomeScreen(
                        onOpenSecurity = { selected = 2 },
                        onOpenEnergy = { selected = 1 },
                        onOpenEnergyBattery = {
                            selected = 1
                            energyDeepLink = "battery"
                        },
                        // (N-05) An the pin -> mo trang Powerwall (iOS M3EHomeView.swift:62).
                        onOpenPowerwall = { powerwallShown = true },
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
                        // (N-05) An node Pin tren flow card -> mo trang Powerwall
                        // (iOS M3EEnergyView.swift:172).
                        onOpenPowerwall = { powerwallShown = true },
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
                    HumeTab.Profile -> MeScreen(
                        roomStore = roomStoreAdapter,
                        pickerEntities = pickerEntities,
                    )
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
            // (2026-10-01) Bo fadeBottom theo user "ko them fade".
            // (N-05) Trang Powerwall mo thi an navbar (port iOS
            // M3ERootView.swift:86-87: opacity 0 + allowsHitTesting false).
            if (!powerwallShown) {
                M3ENavBar(
                    selected = selected,
                    onSelect = { selected = it },
                    onSwipeOpenBrief = { briefOpen = true },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
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
                    sourceRect = req.sourceRect,
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
            // (N-05) Trang Powerwall 3D — mo bang co powerwallShown (tuong duong
            // iOS .navigationDestination(isPresented: showPowerwall)); lop tren
            // cung, phu ca navbar (navbar da an o tren). Ve bang pill title
            // "Powerwall" (onBack) hoac back he thong (BackHandler — tuong duong
            // back cua NavHost, vi root khong co NavHost).
            BackHandler(enabled = powerwallShown) { powerwallShown = false }
            if (powerwallShown) {
                PowerwallScreen(onBack = { powerwallShown = false })
            }
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
 * (GAP-37) Vuot mep trai mo Brief — port 1:1 iOS M3ERootView.swift:52-66
 * (briefOpenGesture + simultaneousGesture tren tabContent):
 * - DragGesture(minimumDistance: 20): chi xet khi bat dau tu mep trai
 *   (startLocation.x < 24) va vuot phai dx > 80, |dx| > |dy| * 1.5.
 * - simultaneousGesture: ScrollView con khong nuot gesture — tren Compose
 *   lam bang cach CHI QUAN SAT, KHONG consume: doc vi tri tho qua
 *   awaitPointerEvent, khong goi change.consume(), nen LazyColumn con van
 *   cuon binh thuong trong khi vuot. awaitFirstDown(requireUnconsumed =
 *   false) de van thay down ngay ca khi bat dau tren mot item clickable
 *   (con no consume down cho hieu ung nhan).
 * - Trang Powerwall mo thi chan (iOS: if powerwallShown { return }) —
 *   truyen qua [enabled] = !powerwallShown.
 *
 * System back gesture: tu Android 10, vuot mep trai mac dinh la Back he
 * thong va he thong lay gesture truoc khi app thay — de vuot mep toi duoc
 * Compose, phai khai vung mep la "app xu ly" bang
 * View.setSystemGestureExclusionRects (dai 24dp sat mep trai, cao toan vung
 * content). Rect tinh theo toa do cua View goc (rootView = LocalView), cap
 * nhat moi lan layout doi (xoay man hinh...) va ap lai khi [enabled] doi
 * (Powerwall mo thi tra lai mep trai cho Back he thong — vuot trai o trang
 * Powerwall la back/xoay 3D theo iOS). Back he thong van dung duoc tu mep
 * phai / thanh dieu huong.
 */
@Composable
private fun Modifier.briefEdgeSwipe(
    enabled: Boolean,
    onOpenBrief: () -> Unit,
): Modifier {
    val density = LocalDensity.current
    val view = LocalView.current
    val edgePx = with(density) { 24.dp.toPx() }
    val thresholdPx = with(density) { 80.dp.toPx() }
    val openBriefRef = rememberUpdatedState(onOpenBrief)
    val enabledRef = rememberUpdatedState(enabled)
    // bounds + rect da ap dung (chi set lai khi doi).
    val exclusionState = remember {
        object {
            var bounds: Rect? = null
            var applied: android.graphics.Rect? = null
        }
    }
    fun applyExclusion() {
        val b = exclusionState.bounds ?: return
        val loc = IntArray(2).also { view.getLocationInWindow(it) }
        val strip = if (enabledRef.value) {
            android.graphics.Rect(
                (b.left - loc[0]).roundToInt(),
                (b.top - loc[1]).roundToInt(),
                (b.left - loc[0] + edgePx).roundToInt(),
                (b.bottom - loc[1]).roundToInt(),
            )
        } else {
            null
        }
        if (exclusionState.applied != strip) {
            exclusionState.applied = strip
            // setSystemGestureExclusionRects chi co tu API 29 (Android 10) —
            // may cu hon khong co system back gesture mep nen khong can loai tru.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                view.setSystemGestureExclusionRects(
                    if (strip == null) emptyList() else listOf(strip),
                )
            }
        }
    }
    androidx.compose.runtime.LaunchedEffect(enabled) { applyExclusion() }
    return this
        .onGloballyPositioned { coords ->
            exclusionState.bounds = coords.boundsInWindow()
            applyExclusion()
        }
        .pointerInput(enabled) {
            while (true) {
                awaitPointerEventScope {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    // Chi nhan khi bat dau tu mep trai < 24dp (iOS
                    // startLocation.x < 24); khong phai thi bo qua
                    // gesture nay (cho lan cham tiep theo).
                    if (!enabledRef.value || down.position.x >= edgePx) return@awaitPointerEventScope
                    var dx = 0f
                    var dy = 0f
                    var done = false
                    while (!done) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                            ?: continue
                        if (change.changedToUpIgnoreConsumed()) {
                            done = true
                            continue
                        }
                        val d = change.position - change.previousPosition
                        dx += d.x
                        dy += d.y
                        // iOS: dx > 80 && |dx| > |dy| * 1.5 (minimumDistance 20
                        // da thoa vi 80 > 20). KHONG consume -> ScrollView con
                        // van nhan du su kien (tuong duong simultaneousGesture).
                        if (dx > thresholdPx && abs(dx) > abs(dy) * 1.5f) {
                            openBriefRef.value()
                            done = true
                        }
                    }
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
    // (navbar 21:24) Bo nen ngoai 30dp = bo nen active 20dp + khe 10dp
    // (nguyen tac dong tam: R_ngoai - R_active = khe giua 2 nen).
    // Android cu 34dp (= 20 + 14, khop khe tren/duoi cu 14dp nhung
    // lech khe canh 10dp). Sau khi giam chieu cao, khe tren/duoi =
    // khe canh = 10dp nen R_ngoai = 30dp. Luu y: user noi nen active
    // bo 16dp nhung code hien tai la RoundedCornerShape(20.dp) —
    // lay so tu code (20dp); neu muon doi active ve 16dp thi
    // R_ngoai phai tinh lai = 26dp.
    val pill = RoundedCornerShape(30.dp)
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
            // (2026-10-01, user: bo 20dp gap duoi navbar — "2 padding"
            // o sat bottom): navbar sat xuong, chi giu inset he thong.
            .padding(start = 16.dp, end = 16.dp)
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
                // Bo shadow (2026-10-01): user "bo het shadow cho dong nhat".
                .clip(pill)
                // Navbar dac 0.98 (user 2026-10-02).
                // Navbar = surfaceContainer (user 18/11 09:47: nen nam GIUA
                // nen the va nen trang — KHOP quyet iOS "dam hon 1 nac"):
                // midpoint 50/50 surfaceContainerHighest<->surface tren
                // seed Indigo (tinh tu generator colorSchemeForCustom,
                // float32 nhu Compose): light #FAFBFD vs the #FFFFFF =
                // 1,04:1 — gan nhu khong thay (duoi nguong 1,05:1, giong
                // iOS); dark #111527 vs the #1A203B = 1,13:1 (thay duoc
                // nhung phai 1 quy tac chung 2 mode). surfaceContainer
                // (light #F2F3F9 / dark #0F1222, token san co) vs the:
                // 1,11:1 light / 1,16:1 dark — thay ro ca 2 mode.
                // CHI DOI TOKEN, khong hardcode hex (M3EColorSchemes.kt
                // la file generate, cam sua), khong dung hinh hoc navbar.
                // LUU Y: nen pressed cua pill active cung la
                // surfaceContainer (xem duoi) nen sau doi nay pill nhan
                // se trung nen navbar — chi con scale/fade lam feedback;
                // chua doi o vong nay, cho user phan.
                .background(cs.surfaceContainer.copy(alpha = 0.98f))
                // (navbar 21:24) Padding doc 10 -> 6dp: khe tren/duoi
                // (nen active -> mep nen ngoai) cu = 10 (Row) + 4 (item)
                // = 14dp, lon hon khe canh = 10dp (Row ngang). Giam de
                // khe tren/duoi = 6 + 4 = 10dp = khe canh. Chieu cao
                // navbar 86 -> 78dp. Padding ngang giu 10dp.
                .padding(horizontal = 10.dp, vertical = 6.dp),
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
                                item.icon, item.tab.label,
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
