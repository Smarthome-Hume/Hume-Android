package com.smarthome.hume.feature.me

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smarthome.hume.core.ui.R as UiR
import com.smarthome.hume.core.ui.avatar.AvatarStore
import com.smarthome.hume.core.ui.avatar.ProfileAvatar
import com.smarthome.hume.core.ui.avatar.UserAvatar
import com.smarthome.hume.core.ui.components.M3EConnectedButtonGroup
import com.smarthome.hume.core.ui.components.M3EIcons
import com.smarthome.hume.core.ui.components.M3EMotion
import com.smarthome.hume.core.ui.components.M3ETextField
import com.smarthome.hume.core.ui.components.NeighborPressState
import com.smarthome.hume.core.ui.components.rememberHaptic
import com.smarthome.hume.core.ui.components.rememberNeighborPress
import com.smarthome.hume.core.ui.components.MsIcon
import com.smarthome.hume.core.ui.components.Ms
import com.smarthome.hume.core.ui.components.pressMorph
import com.smarthome.hume.core.ui.theme.LocalHumeExtraColors
import com.smarthome.hume.core.ui.theme.M3ESeed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Tab Toi — port truc tiep demo v4 rev12 (#page-me):
 * the Dong bo (wavy loading), the Thong bao, Giao dien (dark mode + 10 seeds).
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
    val seed by vm.seed.collectAsState()
    val darkMode by vm.darkMode.collectAsState()
    val haptic = rememberHaptic()

    // Avatar: dung chung key voi header trang Nha (AppHomeRepository publish vao HumeGraph).
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userKey by vm.userKey.collectAsState()
    val avatarStore = remember { AvatarStore(context) }
    val avatarMap by avatarStore.avatars.collectAsState()
    val userAvatar = avatarMap[userKey]
    LaunchedEffect(userKey) {
        if (userKey.isNotBlank()) avatarStore.load(userKey)
    }
    var showChooser by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showFontSheet by remember { mutableStateOf(false) }

    fun onPicked(uri: Uri, isVideo: Boolean) {
        val key = userKey
        if (key.isBlank()) return
        scope.launch {
            val res = avatarStore.saveAvatar(key, uri, isVideo)
            res.onFailure { e ->
                Toast.makeText(context, e.message ?: "Không lưu được avatar", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { onPicked(it, false) }
    }
    val pickVideo = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { onPicked(it, true) }
    }
    val getImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onPicked(it, false) }
    }
    val getVideo = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onPicked(it, true) }
    }
    val pickerAvailable = remember { PickVisualMedia.isPhotoPickerAvailable(context) }
    fun launchImagePicker() {
        if (pickerAvailable) pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        else getImage.launch("image/*")
    }
    fun launchVideoPicker() {
        if (pickerAvailable) pickVideo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
        else getVideo.launch("video/*")
    }

    // Full-bleed tran duoi status bar trong suot (2026-09-30): khong
    // statusBarsPadding o modifier; inset status bar la Spacer dau tien
    // de scroll lien mach (nhu tab Nha).
    val statusBarTop =
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            // Muc cuoi (mau tuy chinh) cach navbar 20dp: navbar floating cao
            // ~86dp + margin 20dp + system inset -> day content 140dp.
            .padding(bottom = 140.dp),
    ) {
        Spacer(Modifier.height(statusBarTop + 8.dp))
        // Title: chi title duoc boc nen
        Column(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .riseEntrance(0)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
            ) {
                // demo .phdr h2: 26px/700 ls -.3px
                Text(
                    "Thông tin",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Box(Modifier.riseEntrance(400)) { SecTitle("Ảnh đại diện") }
        Box(Modifier.riseEntrance(420)) {
            MeAvatarCard(
                userKey = userKey,
                avatar = userAvatar,
                onChange = { showChooser = true },
                onRemove = if (userAvatar != null && userKey.isNotBlank()) {
                    { scope.launch { avatarStore.clearAvatar(userKey) } }
                } else null,
                onLogout = { showLogoutConfirm = true },
            )
        }

        Box(Modifier.riseEntrance(620)) { SecTitle("Trí tuệ nhân tạo") }
        Box(Modifier.riseEntrance(640)) { AiSettingsCard(vm = vm) }

        Box(Modifier.riseEntrance(660)) { SecTitle("Camera") }
        Box(Modifier.riseEntrance(680)) { FrigateRemoteCard(vm = vm) }

        // (Muc "FAB menu" tam xoa theo yeu cau 29/09)

        // (Title "Giao dien" da xoa theo yeu cau 2026-09-30)
        ThemeModeCard(
            mode = darkMode,
            onSelect = { haptic(); vm.setDarkMode(it) },
        )
        // Card 1: mau chu dao (10 seed) co background rieng
        val cs = MaterialTheme.colorScheme
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(cs.surfaceContainerHighest)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                "MÀU CHỦ ĐẠO",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = cs.onSurfaceVariant,
                letterSpacing = 0.4.sp,
            )
            SeedRow(selected = seed, onSelect = { haptic(); vm.setSeed(it) })
        }
        // Card 2: mau tuy chinh co background rieng (muc cuoi trang)
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(cs.surfaceContainerHighest)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                "MÀU TÙY CHỈNH",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = cs.onSurfaceVariant,
                letterSpacing = 0.4.sp,
            )
            Spacer(Modifier.height(12.dp))
            CustomSeedRow(onApplyCustom = { haptic(); vm.setCustomColor(it) })
        }
        // Card 3: font chu (Google Fonts) — chon de tai ve va ap dung (2026-09-30)
        Box(Modifier.riseEntrance(0).padding(top = 12.dp)) {
            FontCard(
                current = vm.fontFamily.collectAsState().value,
                onOpen = { showFontSheet = true },
            )
        }
    }

    // Chon anh / video lam avatar
    if (showChooser) {
        AlertDialog(
            onDismissRequest = { showChooser = false },
            title = { Text("Đổi avatar") },
            text = {
                Column {
                    Text("Chọn ảnh, video ngắn dưới 1 phút, hoặc dùng video Jolly có sẵn.")
                    Spacer(Modifier.height(4.dp))
                    @Composable
                    fun Opt(label: String, onPick: () -> Unit) {
                        TextButton(
                            onClick = { showChooser = false; onPick() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                label,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start,
                            )
                        }
                    }
                    fun useJolly(@androidx.annotation.RawRes resId: Int) {
                        val key = userKey
                        if (key.isBlank()) return
                        scope.launch {
                            val res = avatarStore.saveRawVideo(key, resId)
                            res.onFailure { e ->
                                Toast.makeText(
                                    context,
                                    e.message ?: "Không lưu được avatar",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    }
                    Opt("Ảnh từ thư viện") { launchImagePicker() }
                    Opt("Video từ thư viện") { launchVideoPicker() }
                    Opt("Video Jolly có sẵn") { useJolly(UiR.raw.jolly_avatar) }
                    Opt("Video Jolly làm việc") { useJolly(UiR.raw.jolly_working) }
                }
            },
            confirmButton = {},
        )
    }

    // Xac nhan dang xuat
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Đăng xuất?") },
            text = { Text("App sẽ ngắt kết nối Home Assistant và quay về màn hình đăng nhập.") },
            confirmButton = {
                TextButton(
                    onClick = { showLogoutConfirm = false; vm.logout() },
                ) {
                    Text(
                        "Đăng xuất",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Hủy")
                }
            },
        )
    }

    // Sheet chon font chu
    if (showFontSheet) {
        FontPickerSheet(vm = vm, onDismiss = { showFontSheet = false })
    }
}

// ---------- font chu (Google Fonts) ----------

/** The font chu: hien font hien tai, bam mo sheet chon font de tai ve. */
@Composable
private fun FontCard(current: String, onOpen: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(cs.surfaceContainerHighest)
            .pressMorph(pressedScale = 0.97f, onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(cs.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            // text_fields khong co trong font subset -> dung language (2026-09-30).
            MsIcon(Ms.language, contentDescription = null, tint = cs.onPrimaryContainer)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "FONT CHỮ",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = cs.onSurfaceVariant,
                letterSpacing = 0.4.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                current,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurface,
            )
        }
        MsIcon(
            Ms.chevron_right,
            contentDescription = null,
            tint = cs.onSurfaceVariant,
            // Size explicit: tranh MsIcon do size tu constraints (2026-09-30).
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Sheet chon font: danh sach Google Fonts, uu tien ho tro tieng Viet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FontPickerSheet(
    vm: MeViewModel,
    onDismiss: () -> Unit,
) {
    val fonts by vm.fonts.collectAsState()
    val loading by vm.fontsLoading.collectAsState()
    val current by vm.fontFamily.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { vm.loadFonts() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                "Chọn font chữ",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Font từ Google Fonts — chọn để tải về và áp dụng. Font có dấu ✓ hỗ trợ tiếng Việt.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            M3ETextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Tìm font…",
            )
            Spacer(Modifier.height(8.dp))
            when {
                loading -> {
                    Box(
                        Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                }
                fonts.isEmpty() -> {
                    Box(
                        Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Không tải được danh sách font.\nKiểm tra mạng rồi thử lại.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
                    val shown = remember(query, fonts) {
                        if (query.isBlank()) fonts
                        else fonts.filter { it.family.contains(query, ignoreCase = true) }
                    }
                    LazyColumn(
                        Modifier.fillMaxWidth().heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(shown, key = { it.id }) { font ->
                            FontRow(
                                font = font,
                                selected = font.family == current,
                                onSelect = {
                                    vm.setFontFamily(font.family)
                                    onDismiss()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FontRow(font: GoogleFontInfo, selected: Boolean, onSelect: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) cs.primaryContainer else cs.surfaceContainerLow)
            .pressMorph(pressedScale = 0.97f, onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    font.family,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) cs.onPrimaryContainer else cs.onSurface,
                )
                if (font.supportsVietnamese) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "✓",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = cs.primary,
                    )
                }
            }
            Text(
                "AaBbCcDd 0123456789",
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) cs.onPrimaryContainer else cs.onSurfaceVariant,
            )
        }
        if (selected) {
            MsIcon(
                Ms.check,
                contentDescription = null,
                tint = cs.onPrimaryContainer,
                // Size explicit: tranh MsIcon do size tu constraints (2026-09-30).
                modifier = Modifier.size(20.dp),
            )
        }
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

// ---------- avatar ----------

/**
 * The doi avatar o tab Toi: preview tron + nut Doi / Go
 * (Go chi hien khi da co avatar upload). Khong co chu mo ta (2026-09-30).
 */
@Composable
private fun MeAvatarCard(
    userKey: String,
    avatar: UserAvatar?,
    onChange: () -> Unit,
    onRemove: (() -> Unit)?,
    onLogout: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileAvatar(
            name = userKey,
            avatar = avatar,
            haAvatarUrl = null,
            onTap = {},
            size = 52.dp,
        )
        Spacer(Modifier.width(14.dp))
        // (2026-09-30, user: bo chu "Anh dai dien" — chi giu avatar + nut Doi/Go)
        Spacer(Modifier.weight(1f))
        // Nut Doi/Go: nen pill + pressMorph giong cum dieu hoa (2026-09-30).
        Box(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .pressMorph(pressedScale = 0.88f, onClick = onChange)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Đổi",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        if (onRemove != null) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .pressMorph(pressedScale = 0.88f, onClick = onRemove)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Gỡ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
        // Nut Dang xuat: canh cum Doi/Go (2026-09-30).
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .pressMorph(pressedScale = 0.88f, onClick = onLogout)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Đăng xuất",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
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
            Text("Phát hiện chuyển động", style = MaterialTheme.typography.titleSmall)
            Text(
                "Camera sân trước · 2 phút trước",
                style = MaterialTheme.typography.bodySmall,
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
        Text(text, style = MaterialTheme.typography.titleSmall, color = fg)
    }
}

// ---------- giao dien ----------

/**
 * Card chon che do hien thi: He thong / Sang / Toi (segmented M3E).
 * Luu vao ThemeStore.darkMode (null = theo he thong).
 */
@Composable
private fun ThemeModeCard(mode: Boolean?, onSelect: (Boolean?) -> Unit) {
    // demo .ac-modes: nen surfaceHighest bo 24dp padding 14dp;
    // 3 che do moi cai boc nen rieng (surfaceContainer, selected = primaryContainer)
    // + animation press scale + flex-grow spring giong cum dieu hoa.
    val cs = MaterialTheme.colorScheme
    val options = listOf(null, false, true)
    val labels = listOf("Hệ thống", "Sáng", "Tối")
    val icons = listOf("settings_suggest", "light_mode", "dark_mode")
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 4.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(cs.surfaceContainerHighest)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text("Chế độ hiển thị", style = MaterialTheme.typography.titleSmall)
        // Cum 3 che do: nen surfaceHighest bo 24, moi option nen rieng
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(LocalHumeExtraColors.current.surfaceHighest)
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEachIndexed { i, opt ->
                val selected = mode == opt
                var pressed by remember { mutableStateOf(false) }
                val bg by animateColorAsState(
                    if (selected) cs.primaryContainer else cs.surfaceContainer,
                    tween(300), label = "tmBg",
                )
                val fg by animateColorAsState(
                    if (selected) cs.onPrimaryContainer else cs.onSurfaceVariant,
                    tween(300), label = "tmFg",
                )
                // flex-grow spring: selected/pressed no ra nhu .rmm.press-main
                val grow by animateFloatAsState(
                    if (pressed) 1.45f else 1f,
                    spring(dampingRatio = 0.6f, stiffness = 400f), label = "tmGrow",
                )
                val scale by animateFloatAsState(
                    if (pressed) 0.9f else 1f,
                    spring(dampingRatio = 0.6f, stiffness = 400f), label = "tmScale",
                )
                Box(
                    Modifier
                        .weight(grow)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .clip(RoundedCornerShape(18.dp))
                        .background(bg)
                        .pointerInput(opt) {
                            detectTapGestures(
                                onPress = {
                                    pressed = true
                                    tryAwaitRelease()
                                    pressed = false
                                },
                                onTap = { onSelect(opt) },
                            )
                        }
                        .padding(vertical = 14.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        labels[i],
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        color = fg,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/**
 * Nhap ma hex + nut Ap dung: chon seed GAN NHAT trong 10 seed co san
 * (theme engine chi co 8 scheme hand-built, khong sinh scheme tu mau tuy y).
 * Dai mau chon nhanh = SeedRow 8 circle ben tren.
 */
@Composable
private fun CustomSeedRow(onApplyCustom: (Long) -> Unit) {
    val cs = MaterialTheme.colorScheme
    var hue by remember { androidx.compose.runtime.mutableFloatStateOf(0f) } // 0..360
    var sat by remember { androidx.compose.runtime.mutableFloatStateOf(1f) } // 0..1
    var value by remember { androidx.compose.runtime.mutableFloatStateOf(1f) } // 0..1
    var feedback by remember { mutableStateOf<Pair<String, Boolean>?>(null) }

    // Mau hien tai tu HSV
    val currentColor = remember(hue, sat, value) {
        androidx.compose.ui.graphics.Color.hsv(hue, sat, value)
    }
    val hexString = remember(currentColor) {
        val c = currentColor
        "#%02X%02X%02X".format(
            (c.red * 255).toInt(),
            (c.green * 255).toInt(),
            (c.blue * 255).toInt(),
        )
    }
    val rgbString = remember(currentColor) {
        "${(currentColor.red * 255).toInt()}, " +
            "${(currentColor.green * 255).toInt()}, " +
            "${(currentColor.blue * 255).toInt()}"
    }
    val hsvString = remember(hue, sat, value) {
        "${hue.toInt()}°, ${(sat * 100).toInt()}%, ${(value * 100).toInt()}%"
    }

    Column(
        Modifier.fillMaxWidth(),
    ) {
        // Dai mau spectrum: hue slider (giong Google color picker) - keo duoc.
        // Gon nhe theo design system (2026-09-30, user: the mau tuy chinh qua to).
        Box(Modifier.fillMaxWidth().height(28.dp)) {
            // Nen gradient cau vong
            Box(
                Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = List(7) { i ->
                                androidx.compose.ui.graphics.Color.hsv(i * 60f, 1f, 1f)
                            } + listOf(androidx.compose.ui.graphics.Color.hsv(0f, 1f, 1f)),
                        ),
                    ),
            )
            // Slider trong suot de keo
            androidx.compose.material3.Slider(
                value = hue,
                onValueChange = { hue = it },
                valueRange = 0f..360f,
                modifier = Modifier.matchParentSize(),
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = androidx.compose.ui.graphics.Color.White,
                    activeTrackColor = androidx.compose.ui.graphics.Color.Transparent,
                    inactiveTrackColor = androidx.compose.ui.graphics.Color.Transparent,
                ),
            )
        }
        // O mau 2D: saturation (ngang) x value/brightness (doc) — cham/keo de chon.
        // Thu gon 160 -> 112dp cho the khong qua cao (2026-09-30).
        Spacer(Modifier.height(6.dp))
        androidx.compose.foundation.layout.BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(112.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            androidx.compose.ui.graphics.Color.Transparent,
                            androidx.compose.ui.graphics.Color.Black,
                        ),
                    ),
                )
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        sat = (offset.x / size.width).coerceIn(0f, 1f)
                        value = (1f - offset.y / size.height).coerceIn(0f, 1f)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        sat = (change.position.x / size.width).coerceIn(0f, 1f)
                        value = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                        change.consume()
                    }
                },
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = listOf(
                                androidx.compose.ui.graphics.Color.White,
                                androidx.compose.ui.graphics.Color.hsv(hue, 1f, 1f),
                            ),
                        ),
                    ),
            )
            // Thumb tron hien thi vi tri sat/value hien tai
            Box(
                Modifier
                    .offset(
                        x = maxWidth * sat - 9.dp,
                        y = maxHeight * (1f - value) - 9.dp,
                    )
                    .size(18.dp)
                    .border(
                        2.dp,
                        androidx.compose.ui.graphics.Color.White,
                        androidx.compose.foundation.shape.CircleShape,
                    ),
            )
        }
        // Slider cho saturation va brightness
        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("S", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, modifier = Modifier.width(16.dp))
            androidx.compose.material3.Slider(
                value = sat,
                onValueChange = { sat = it },
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("V", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, modifier = Modifier.width(16.dp))
            androidx.compose.material3.Slider(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.weight(1f),
            )
        }
        // Hien thi HEX / RGB / HSV + preview
        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(currentColor)
                    .border(1.dp, cs.outlineVariant, RoundedCornerShape(10.dp)),
            )
            Column(Modifier.weight(1f)) {
                Text("HEX $hexString", style = MaterialTheme.typography.bodySmall)
                Text("RGB $rgbString", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                Text("HSV $hsvString", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
            }
            Box(
                Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(cs.primaryContainer)
                    .pressMorph(pressedScale = 0.92f) {
                        val c = currentColor
                        val argb = (0xFF000000L or
                            ((c.red * 255).toInt().toLong() shl 16) or
                            ((c.green * 255).toInt().toLong() shl 8) or
                            (c.blue * 255).toInt().toLong())
                        onApplyCustom(argb)
                        feedback = "Đã áp dụng màu tùy chỉnh" to false
                    }
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Áp dụng",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = cs.onPrimaryContainer,
                )
            }
        }
        feedback?.let { (msg, isError) ->
            Text(
                msg,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = if (isError) cs.error else cs.primary,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp),
            )
        }
    }
}

/** Khoang cach RGB binh phuong — de tim seed gan nhat. */
private fun colorDistance(a: Color, b: Color): Float {
    val dr = a.red - b.red
    val dg = a.green - b.green
    val db = a.blue - b.blue
    return dr * dr + dg * dg + db * db
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
    M3ESeed.Lime to Color(0xFF6B9E2F),
    M3ESeed.Indigo to Color(0xFF4A5AA8),
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
    M3ESeed.Lime to "Xanh nõn",
    M3ESeed.Indigo to "Chàm",
)

/**
 * SeedRow demo (.seeds): flex-wrap, gap 14px; seed 58px circle, press scale .86;
 * check hien opacity/scale spring .3s; ring ::after inset -7px border 2px mau seed.
 */
@OptIn(ExperimentalLayoutApi::class)
/** Hang mau chu dao: 2 hang x 5 mau (2026-09-30, user yeu cau) —
 *  moi hang la Row SpaceBetween de luon du 5 dot/hang, khong wrap lech. */
@Composable
private fun SeedRow(selected: M3ESeed, onSelect: (M3ESeed) -> Unit) {
    Column(
        // Nam trong card nen chi can khoang cach voi label phia tren
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        M3ESeed.entries.chunked(5).forEach { rowSeeds ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                rowSeeds.forEach { s ->
                    SeedDot(
                        seed = s,
                        selected = s == selected,
                        onSelect = onSelect,
                    )
                }
            }
        }
    }
}

@Composable
private fun SeedDot(
    seed: M3ESeed,
    selected: Boolean,
    onSelect: (M3ESeed) -> Unit,
) {
    val color = seedColors[seed] ?: Color.Gray
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.86f else 1f,
        tween(300, easing = M3EMotion.spring),
        label = "seedScale",
    )
    val checkAlpha by animateFloatAsState(
        if (selected) 1f else 0f,
        tween(300, easing = M3EMotion.spring),
        label = "seedCheckAlpha",
    )
    val checkScale by animateFloatAsState(
        if (selected) 1f else 0.5f,
        tween(300, easing = M3EMotion.spring),
        label = "seedCheckScale",
    )
    // ring ::after: inset -7px => khung ngoai 72dp, border 2px mau seed
    Box(
        Modifier
            .size(56.dp)
            .then(
                if (selected) Modifier.border(2.dp, color, CircleShape)
                else Modifier
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(color)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = { onSelect(seed) },
                ),
            contentAlignment = Alignment.Center,
        ) {
            MsIcon(
                M3EIcons.Check,
                contentDescription = seedNames[seed],
                tint = Color.White,
                modifier = Modifier
                    .size(26.dp)
                    .scale(checkScale)
                    .alpha(checkAlpha),
            )
        }
    }
}
