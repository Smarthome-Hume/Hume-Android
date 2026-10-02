package com.smarthome.hume.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smarthome.hume.core.data.HumeGraph
import com.smarthome.hume.core.model.DeviceKind
import com.smarthome.hume.core.ui.components.m3ePageBottomPadding
import com.smarthome.hume.core.ui.components.M3EMotion
import com.smarthome.hume.core.ui.components.M3ESectionTitle
import com.smarthome.hume.core.ui.components.rememberHaptic
import com.smarthome.hume.core.ui.avatar.AvatarStore
import com.smarthome.hume.core.ui.avatar.AvatarViewerRequest
import com.smarthome.hume.core.ui.components.Ms
import com.smarthome.hume.core.ui.components.MsIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Trang Nha M3E theo demo hume-m3e-v4-dashboard.html rev12 (#page-home):
 * header → pills (an ninh + den) → "Goi y cho ban" → solcard →
 * solar → batcard → "Phong" → rooms grid + FAB speed-dial.
 *
 * .page{padding:4px 18px 170px}; FAB tuyet doi right 20px bottom 108px;
 * snackbar tonal bottom 104px; pull-to-refresh morphloader (#ptr).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory()),
    onOpenSecurity: () -> Unit = {},
    onOpenEnergy: () -> Unit = {},
    /** Bam "Xem pin" o the goi y -> sang tab Nang luong, cuon toi the pin. */
    onOpenEnergyBattery: () -> Unit = {},
    loadChartHistory: ChartHistoryLoader = { _, _, _ -> emptyList() },
    /**
     * Mo viewer avatar / popup camera o tang root (M3ERootScreen) de lop mo +
     * blur phu TOAN man hinh ke ca navbar. HomeScreen chi gui request.
     */
    onOpenAvatarViewer: (AvatarViewerRequest) -> Unit = {},
    onOpenCameraPopup: (CameraPopupRequest) -> Unit = {},
    /**
     * Header avatar duoc dinh vi (boundsInWindow): root dung de xoa "bong ma"
     * avatar khoi anh nen khi mo popup camera (giong avatar viewer).
     */
    onAvatarPositioned: (Rect) -> Unit = {},
) {
    val state by viewModel.state.collectAsState()
    val ui by viewModel.ui.collectAsState()
    val cs = MaterialTheme.colorScheme
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var chartDetail by remember { mutableStateOf<ChartDetailType?>(null) }
    val tips = buildSuggestTips(state)
    val aiState by viewModel.aiState.collectAsState()
    val aiTips = (aiState as? AiUiState.Loaded)?.tips.orEmpty()

    // Avatar user upload + viewer phong to kieu kinh lup (giong trang Thong tin)
    val context = LocalContext.current
    val avatarStore = remember { AvatarStore(context) }
    val avatarMap by avatarStore.avatars.collectAsState()
    val userAvatar = avatarMap[state.userKey]
    LaunchedEffect(state.userKey) {
        if (state.userKey.isNotBlank()) avatarStore.load(state.userKey)
    }
    // (2026-09-30) Ref thuong thay vi state: onGloballyPositioned chay moi
    // frame khi scroll; set state o day recompose ca HomeScreen moi frame.
    // Rect chi can luc tap avatar de mo viewer.
    val avatarRectRef = remember { object { var rect: Rect? = null } }
    val secState by remember { HumeGraph.get().securityRepository.securityState }.collectAsState()

    ui.snackbar?.let { s ->
        LaunchedEffect(s) {
            val res = snack.showSnackbar(s.msg, actionLabel = s.actionLabel)
            if (res == SnackbarResult.ActionPerformed) s.onAction?.invoke()
            viewModel.clearSnack()
        }
    }

    Scaffold(
        // Keep the scrolling viewport full-height behind the root's navbar
        // overlay. The list applies the status-bar inset to its own content.
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        snackbarHost = {
            // .snack{left:16;right:16;bottom:104} theo demo: snackbar nam TREN
            // navbar (~100dp). Navbar la overlay noi o M3ERootScreen (khong
            // phai Scaffold bottomBar) nen slot snackbar mac dinh nam o day
            // man hinh, bi navbar de len — phai nang bottom = 104.dp.
            // Vao: fade .25s + translateY(16->0) .35s; ra: nguoc lai.
            var lastData by remember { mutableStateOf<SnackbarData?>(null) }
            val current = snack.currentSnackbarData
            if (current != null) lastData = current
            val density = LocalDensity.current
            AnimatedVisibility(
                visible = current != null,
                enter = fadeIn(tween(250)) + slideInVertically(
                    tween(350, easing = M3EMotion.emphasized),
                ) { with(density) { 16.dp.roundToPx() } },
                exit = fadeOut(tween(250)) + slideOutVertically(
                    tween(350, easing = M3EMotion.emphasizedAcc),
                ) { with(density) { 16.dp.roundToPx() } },
                modifier = Modifier.padding(horizontal = 16.dp).m3ePageBottomPadding(),
            ) {
                lastData?.let { M3ESnackbar(it) }
            }
        },
        containerColor = cs.surface,
        modifier = modifier.fillMaxSize(),
    ) { padding ->
        val pullState = rememberPullToRefreshState()
        // Scroll state: tu dong thu cum an ninh SAU KHI scroll dung han
        // (2026-09-30, fix khung): khong thu ngay khi scroll bat dau nua —
        // exit animation 300ms de len scroll gay rot frame. Scroll stop ->
        // debounce 350ms (user cham lai thi huy) -> thu.
        val listState = rememberLazyListState()
        var collapseArmed by remember { mutableStateOf(false) }
        LaunchedEffect(listState.isScrollInProgress) {
            if (listState.isScrollInProgress) {
                collapseArmed = true
            } else if (collapseArmed && ui.securityExpanded) {
                delay(350)
                collapseArmed = false
                viewModel.collapseSecurity()
            }
        }
        PullToRefreshBox(
            isRefreshing = ui.isRefreshing,
            onRefresh = { viewModel.refresh() },
            state = pullState,
            // Full-bleed tran duoi status bar (trong suot): noi dung scroll
            // chui xuong duoi status bar roi bien mat lien mach, khong con
            // duong cat cung tai mep padding (2026-09-30, user yeu cau).
            // Inset status bar chuyen vao contentPadding cua LazyColumn.
            modifier = Modifier.padding(padding),
            indicator = {
                MorphLoaderIndicator(
                    isRefreshing = ui.isRefreshing,
                    state = pullState,
                    modifier = Modifier.align(Alignment.TopCenter)
                        .statusBarsPadding(),
                )
            },
        ) {
            // Chieu cao status bar de cong vao top contentPadding.
            val statusBarTop =
                WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            Box(Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    // Fling dam (0.6x van toc) -> cuon cham, do hon
                    flingBehavior = rememberDampedFlingBehavior(),
                    contentPadding = PaddingValues(
                        // Keep list content full-bleed behind the navbar overlay;
                        // no bottom system inset is reserved in the scroll area.
                        start = 18.dp, end = 18.dp,
                        top = statusBarTop + 8.dp,
                    ),
                    // Nhịp margin-collapse theo CSS (khong spacedBy):
                    // card->card 14; pills->sec 20; sec->card 12
                    // (Khong blur live o day: M3ERootScreen chup anh tinh 1 lan
                    //  roi blur san khi mo overlay -> het khựng.)
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item {
                        Column(Modifier.padding(bottom = 14.dp)) {
                            HomeHeader(
                                state = state,
                                avatarUrl = state.avatarUrl,
                                userAvatar = userAvatar,
                                onAvatarTap = {
                                    // Mo viewer o tang root: mo + blur phu ca navbar
                                    onOpenAvatarViewer(
                                        AvatarViewerRequest(
                                            name = state.userName.ifBlank { "Gia đình" },
                                            avatar = userAvatar,
                                            haAvatarUrl = state.avatarUrl,
                                            targetRect = avatarRectRef.rect,
                                        ),
                                    )
                                },
                                onAvatarPositioned = {
                                    avatarRectRef.rect = it
                                    onAvatarPositioned(it)
                                },
                                onSearch = { viewModel.openSearch(true) },
                                onNotif = { viewModel.openNotif(true) },
                            )
                        }
                    }
                    item {
                        Column(Modifier.padding(bottom = 20.dp)) {
                            PillsRow(
                                alarm = state.alarm,
                                lightsOnCount = state.lightsOn.size,
                                securityExpanded = ui.securityExpanded,
                                onToggleSecurity = { viewModel.toggleSecurity() },
                                onAutoCollapse = { viewModel.collapseSecurity() },
                                onArm = { mode, label -> viewModel.armAlarm(mode, label) },
                                onDisarm = { viewModel.disarmAlarm() },
                                onLights = { viewModel.openLights(true) },
                            )
                        }
                    }
                    if (tips.isNotEmpty() || aiTips.isNotEmpty()) {
                        item {
                            Column(Modifier.padding(bottom = 12.dp)) {
                                M3ESectionTitle("Gợi ý cho bạn")
                            }
                        }
                        item {
                            Column(Modifier.padding(bottom = 14.dp)) {
                                SuggestCard(
                                    state,
                                    aiTips = aiTips,
                                    onTipAction = { key ->
                                        when {
                                            key == "ac" -> viewModel.ac26()
                                            key.startsWith("toggle_ac:") ->
                                                viewModel.toggleClimate(key.removePrefix("toggle_ac:"))
                                            key == "lights_day" -> viewModel.turnOffAllLights()
                                        }
                                    },
                                    onBatteryDetail = {
                                        // "Xem pin" o the goi y -> link sang the pin
                                        // o trang Nang luong (user 2026-10-01).
                                        onOpenEnergyBattery()
                                    },
                                    onOpenSecurity = onOpenSecurity,
                                    onOpenEnergy = onOpenEnergy,
                                    cameras = secState.cameras,
                                    onOpenCamera = { key, name -> onOpenCameraPopup(CameraPopupRequest(key, name)) },
                                )
                            }
                        }
                    }
                    // (The thong bao da xoa theo yeu cau user - khong co tac dung)
                    item {
                        Column(Modifier.padding(bottom = 14.dp)) {
                            SolarWeekCard(state)
                        }
                    }
                    // (The cong suat PV da xoa theo yeu cau user 2026-09-30)
                    item {
                        Column(Modifier.padding(bottom = 20.dp)) {
                            BatteryCard(
                                state.battery,
                                onClick = { chartDetail = ChartDetailType.Battery },
                            )
                        }
                    }
                    item {
                        Column(Modifier.padding(bottom = 12.dp)) {
                            M3ESectionTitle("Phòng")
                        }
                    }
                    item {
                        RoomGrid(
                            rooms = state.rooms,
                            notifications = state.notifications,
                            onRoom = { viewModel.selectRoom(it) },
                            onToggleLight = { viewModel.toggle(it) },
                        )
                    }
                }

                // (FAB speed-dial tam xoa theo yeu cau 29/09)

                // Sheet phong (ModalBottomSheet cua M3: giu day du chuc nang
                // keo dong, scrim, predictive back...)
                ui.selectedRoom?.let { room ->
                    val live = state.rooms.firstOrNull { it.key == room.key } ?: room
                    RoomSheet(
                        room = live,
                        notifications = state.notifications,
                        onDismiss = { viewModel.selectRoom(null) },
                        onToggle = { viewModel.toggle(it) },
                        onClimateTemp = { id, t -> viewModel.setClimateTemp(id, t) },
                        onHvacMode = { id, m -> viewModel.setHvacMode(id, m) },
                        onToggleClimate = { viewModel.toggleClimate(it) },
                    )
                }
                if (ui.notifOpen) {
                    NotificationSheet(
                        notifications = state.notifications,
                        onDismiss = { viewModel.openNotif(false) },
                    )
                }
                // Sheet bieu do chi tiet (cham the Pin / Dien mat troi)
                chartDetail?.let { type ->
                    ChartDetailSheet(
                        type = type,
                        onDismiss = { chartDetail = null },
                        loadHistory = loadChartHistory,
                    )
                }
                if (ui.lightsOpen) {
                    LightsSheet(
                        lights = state.lightsOn,
                        onDismiss = { viewModel.openLights(false) },
                        onToggle = { viewModel.toggle(it) },
                    )
                }
                if (ui.searchOpen) {
                    // Demo: query rong hien TOAN BO thiet bi
                    val q = ui.searchQuery.trim().lowercase()
                    val results = viewModel.searchableDevices().filter {
                        q.isEmpty() ||
                            it.label.lowercase().contains(q) ||
                            it.sub.lowercase().contains(q) ||
                            it.entityId.lowercase().contains(q)
                    }.take(50)
                    DeviceSearchView(
                        query = ui.searchQuery,
                        onQuery = { viewModel.onSearchQuery(it) },
                        results = results,
                        onToggle = { id ->
                            // Dieu hoa dung toggleClimate, thiet bi thuong dung toggle
                            if (results.any { d -> d.entityId == id && d.kind == DeviceKind.Climate })
                                viewModel.toggleClimate(id)
                            else viewModel.toggle(id)
                        },
                        onBack = { viewModel.openSearch(false) },
                    )
                }
                // (Dialog tiet kiem dien di kem FAB — tam xoa theo)
                // (Viewer avatar + popup camera duoc ve o tang M3ERootScreen,
                //  tren ca navbar — khong ve o day nua.)
            }
        }
    }
}

/**
 * Pull-to-refresh indicator mo phong #ptr .morphloader cua demo:
 * blob 34px morph lien tuc, hien theo luc keo / khi dang refresh.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoxScope.MorphLoaderIndicator(
    isRefreshing: Boolean,
    state: PullToRefreshState,
    modifier: Modifier = Modifier,
) {
    // Tinh alpha/scale TRUOC khi tao infinite transition: khi an (alpha=0) thi
    // return som, khong chay animation vo han nen -> do recompose moi frame.
    val pull = state.distanceFraction
    val alpha = if (isRefreshing) 1f else (pull * 3f).coerceIn(0f, 1f)
    if (alpha <= 0f && !isRefreshing) return
    val t = rememberInfiniteTransition(label = "morph")
    val f by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "morphF",
    )
    val scale = if (isRefreshing) 1f else pull.coerceIn(0.2f, 1f)
    Box(
        modifier = modifier
            .padding(top = 16.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .size(34.dp)
            .clip(RoundedCornerShape(percent = (32 + (28 * f)).toInt()))
            .background(MaterialTheme.colorScheme.primary),
    )
}

/**
 * Snackbar tonal theo demo (.snack): surfaceContainerHigh + chu onSurface 14/500 +
 * action primary 14/700, bo 20dp, min-height 48, padding 6/6/6/18.
 */
@Composable
private fun M3ESnackbar(data: SnackbarData) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cs.surfaceContainerHigh,
        shadowElevation = 6.dp,
        modifier = Modifier.heightIn(min = 48.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        ) {
            Text(
                data.visuals.message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = cs.onSurface,
                modifier = Modifier.weight(1f),
            )
            data.visuals.actionLabel?.let { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.primary,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { data.performAction() }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                )
            }
        }
    }
}

/**
 * Dialog "Bat tiet kiem dien?" theo demo v4 (.dialog/.dscrim):
 * surfaceContainerHigh, bo 28px, padding 24px, icon bolt 24px secondary;
 * vao: fade + scale(.92) spring; dscrim dong khi bam ngoai.
 */
@Composable
private fun EcoDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    var vis by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vis = true }
    fun close(action: () -> Unit) {
        vis = false
        scope.launch {
            delay(280)
            action()
        }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = vis,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(280)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { close(onDismiss) },
            )
        }
        AnimatedVisibility(
            visible = vis,
            enter = fadeIn(tween(250)) +
                scaleIn(
                    animationSpec = tween(400, easing = M3EMotion.spring),
                    initialScale = 0.92f,
                ),
            exit = fadeOut(tween(250)) +
                scaleOut(
                    animationSpec = tween(400, easing = M3EMotion.spring),
                    targetScale = 0.92f,
                ),
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = cs.surfaceContainerHigh,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .offset(y = (-60).dp),
            ) {
                Column(Modifier.padding(24.dp)) {
                    MsIcon(
                        Ms.bolt, null,
                        tint = cs.secondary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Bật tiết kiệm điện?",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Medium,
                        color = cs.onSurface,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Sẽ tắt các thiết bị không cần thiết và giảm độ sáng đèn còn 50%.",
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp,
                        color = cs.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Spacer(Modifier.weight(1f))
                        DialogButton("Hủy") { close(onDismiss) }
                        DialogButton("Bật") { close(onConfirm) }
                    }
                }
            }
        }
    }
}

/** .dbtn: text button primary 14px/700, padding 10px 14px, bo 99px. */
@Composable
private fun DialogButton(label: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val haptic = rememberHaptic()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (pressed) cs.primaryContainer else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(
                interactionSource = interaction,
                indication = null,
            ) {
                haptic()
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = cs.primary,
        )
    }
}
