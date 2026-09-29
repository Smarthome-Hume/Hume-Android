package com.smarthome.hume.ui.profile

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.core.storage.HumeSettings
import com.smarthome.hume.core.storage.SettingsStore
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeShapes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import com.smarthome.hume.ui.theme.glassPill
import com.smarthome.hume.ui.theme.glassSurface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive

/** Floating nav pill plus gesture bar: nothing may scroll under it. */
private val NavBarRoom = 140.dp

/**
 * Profile tab, rebuilt from ProfileView.swift.
 *
 * Icon o day dung Phosphor regular (giong ban HTML: <i class="ph ph-...">),
 * khong dung Material Rounded vi ban Rounded la icon DAC.
 *
 * YEU CAU MOI:
 *   - KHONG con the "Tu dong & canh bao" (toan bo nhanh kich ban da bo).
 *   - KHONG con lop nen bao ngoai (GroupGlassContainer radius 47): the con tu
 *     mang nen cua no, cac the nam truc tiep tren mat trang.
 */
@Composable
fun ProfileScreen(settingsStore: SettingsStore, settings: HumeSettings, ha: HomeAssistantRepository) {
    val entities by ha.entities.collectAsState()
    val connected by ha.connected.collectAsState()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("hume_profile", Context.MODE_PRIVATE) }

    var email by remember { mutableStateOf(prefs.getString("user_email", "").orEmpty()) }
    var phone by remember { mutableStateOf(prefs.getString("user_phone", "").orEmpty()) }
    var editing by remember { mutableStateOf<String?>(null) }
    var openDeviceManager by remember { mutableStateOf(false) }

    val person: HomeEntity? = entities["person.hutchet"]
    val personName = person?.attr("friendly_name") ?: "H\u1ea3i H\u00e0"
    val userId = person?.attr("user_id") ?: "\u2014"
    val avatarUrl = person?.attr("entity_picture")?.let { settings.haUrl.trimEnd('/') + it }

    // Avatar theo user: upload anh / video ngan (< 1 phut), luu local theo userId.
    val avatarStore = remember { AvatarStore(context) }
    val userKey = userId.takeIf { it != "\u2014" } ?: personName
    val avatarMap by avatarStore.avatars.collectAsState()
    val userAvatar = avatarMap[userKey]
    LaunchedEffect(userKey) { avatarStore.load(userKey) }

    var showChooser by remember { mutableStateOf(false) }
    var viewerOpen by remember { mutableStateOf(false) }
    var avatarRect by remember { mutableStateOf<Rect?>(null) }
    val scope = rememberCoroutineScope()

    fun onPicked(uri: android.net.Uri, isVideo: Boolean) {
        scope.launch {
            val res = avatarStore.saveAvatar(userKey, uri, isVideo)
            res.onFailure { e ->
                Toast.makeText(
                    context,
                    e.message ?: "Không lưu được avatar",
                    Toast.LENGTH_SHORT,
                ).show()
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
        if (pickerAvailable) pickImage.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
        else getImage.launch("image/*")
    }
    fun launchVideoPicker() {
        if (pickerAvailable) pickVideo.launch(PickVisualMediaRequest(PickVisualMedia.VideoOnly))
        else getVideo.launch("video/*")
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .then(if (viewerOpen) Modifier.blur(24.dp) else Modifier)
                .background(HumeColors.Background)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
        Text("Th\u00f4ng tin", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = HumeColors.TextPrimary)

        // The chu nha nam truc tiep tren trang, KHONG con lop nen bao ngoai.
        OwnerCard(
            name = personName,
            avatar = userAvatar,
            haAvatarUrl = avatarUrl,
            onAvatarTap = { viewerOpen = true },
            onAvatarEdit = { showChooser = true },
            onAvatarPositioned = { avatarRect = it },
            onManageDevices = { openDeviceManager = true },
        )

        // Cac dong thong tin: moi dong tu mang nen rieng, khong co the cha.
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProfileRow(Icons.Outlined.Badge, "ID Ng\u01b0\u1eddi d\u00f9ng", userId, copy = true)
            ProfileRow(Icons.Outlined.Person, "T\u00ean ng\u01b0\u1eddi d\u00f9ng", personName, copy = true)
            ProfileRow(
                Icons.Outlined.Email,
                "Email",
                email.ifEmpty { "Ch\u01b0a c\u1eadp nh\u1eadt" },
                copy = false,
                onClick = { editing = "email" },
            )
            ProfileRow(
                Icons.Outlined.Phone,
                "\u0110i\u1ec7n tho\u1ea1i",
                phone.ifEmpty { "Ch\u01b0a c\u1eadp nh\u1eadt" },
                copy = false,
                onClick = { editing = "phone" },
            )
            ProfileRow(
                Icons.Outlined.Place,
                "V\u1ecb tr\u00ed",
                locationName(person?.state),
                copy = true,
            )
        }

        // Connection pill
        Row(
            Modifier.glassPill(20.dp).padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(8.dp).clip(CircleShape)
                    .background(if (connected) Color(0xFF22C55E) else Color(0xFFEF4444)),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                if (connected) "\u0110\u00e3 k\u1ebft n\u1ed1i" else "M\u1ea5t k\u1ebft n\u1ed1i",
                fontSize = 13.sp,
                color = if (connected) Color(0xFF22C55E) else Color(0xFFEF4444),
                maxLines = 1,
                softWrap = false,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "\u00b7 " + entities.size + " th\u1ef1c th\u1ec3",
                fontSize = 13.sp,
                color = HumeColors.TextSecondary,
                maxLines = 1,
                softWrap = false,
            )
        }

        // Logout
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFF44336).copy(alpha = 0.12f))
                .clickable {
                    ha.disconnect()
                    CoroutineScope(Dispatchers.IO).launch { settingsStore.logout() }
                }
                .padding(horizontal = 24.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Tho\u00e1t t\u00e0i kho\u1ea3n",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFEF5350),
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(NavBarRoom))
        }

        // Nhan avatar -> popup tron phong to de ngam (nen trang mo di)
        if (viewerOpen) {
            AvatarViewerOverlay(
                name = personName,
                avatar = userAvatar,
                haAvatarUrl = avatarUrl,
                targetRect = avatarRect,
                onDismiss = { viewerOpen = false },
            )
        }
    }

    if (openDeviceManager) {
        DeviceManagerSheet(ha = ha, settings = settings, onDismiss = { openDeviceManager = false })
    }

    // Chon anh / video lam avatar
    if (showChooser) {
        AlertDialog(
            onDismissRequest = { showChooser = false },
            title = { Text("Đổi avatar") },
            text = { Text("Chọn ảnh, hoặc video ngắn dưới 1 phút để làm avatar.") },
            confirmButton = {
                TextButton(onClick = { showChooser = false; launchImagePicker() }) { Text("Ảnh") }
            },
            dismissButton = {
                TextButton(onClick = { showChooser = false; launchVideoPicker() }) { Text("Video") }
            },
        )
    }

    // Nhan avatar -> mo phong to
    if (viewerOpen) {
        AvatarViewerOverlay(
            name = personName,
            avatar = userAvatar,
            haAvatarUrl = avatarUrl,
            onDismiss = { viewerOpen = false },
        )
    }

    // EditFieldView
    val field = editing
    if (field != null) {
        var draft by remember(field) { mutableStateOf(if (field == "email") email else phone) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(if (field == "email") "Email" else "\u0110i\u1ec7n tho\u1ea1i") },
            text = {
                OutlinedTextField(value = draft, onValueChange = { draft = it }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    if (field == "email") {
                        email = draft
                        prefs.edit().putString("user_email", draft).apply()
                    } else {
                        phone = draft
                        prefs.edit().putString("user_phone", draft).apply()
                    }
                    editing = null
                }) { Text("L\u01b0u") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Hu\u1ef7") } },
        )
    }
}

/** orangeCard in ProfileView.swift: gradient #f9784c to #e8653a to #fac0b6, radius 35, padding 20. */
@Composable
private fun OwnerCard(
    name: String,
    avatar: UserAvatar?,
    haAvatarUrl: String?,
    onAvatarTap: () -> Unit,
    onAvatarEdit: () -> Unit,
    onAvatarPositioned: (Rect) -> Unit,
    onManageDevices: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(35.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFFF9784C), Color(0xFFE8653A), Color(0xFFFAC0B6)),
                ),
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(
                name = name,
                avatar = avatar,
                haAvatarUrl = haAvatarUrl,
                onTap = onAvatarTap,
                onEdit = onAvatarEdit,
                onPositioned = onAvatarPositioned,
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Ch\u1ee7 nh\u00e0", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
            }
        }
        Text(
            "Y\u00eau th\u00edch c\u00f4ng ngh\u1ec7 smarthome v\u00e0 qu\u1ea3n l\u00fd thi\u1ebft b\u1ecb th\u00f4ng minh.",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.85f),
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.2f))
                    .clickable(onClick = onManageDevices)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Smartphone, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Qu\u1ea3n l\u00fd thi\u1ebft b\u1ecb", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White, maxLines = 1, softWrap = false, modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
            }
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** ProfileCardRow: 46dp icon circle, label over value, copy or edit trailing button. */
@Composable
private fun ProfileRow(
    icon: ImageVector,
    label: String,
    value: String,
    copy: Boolean,
    onClick: (() -> Unit)? = null,
) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .glassSurface(radius = HumeShapes.Popup)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(46.dp).clip(CircleShape).background(HumeColors.Background),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = HumeColors.TextSecondary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp, color = HumeColors.TextSecondary, maxLines = 1, softWrap = false)
            Text(
                value.ifEmpty { "\u2014" },
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = HumeColors.TextPrimary,
                maxLines = 1,
                softWrap = false,
            )
        }
        Box(
            Modifier
                .size(40.dp)
                .clickable {
                    if (copy) {
                        clipboard.setText(AnnotatedString(value))
                        copied = true
                    } else {
                        onClick?.invoke()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            if (copy && copied) {
                Text("\u0110\u00e3 copy", fontSize = 11.sp, color = HumeColors.TextSecondary, maxLines = 1, softWrap = false)
            } else {
                Icon(
                    if (copy) Icons.Outlined.ContentCopy else Icons.Outlined.Edit,
                    contentDescription = null,
                    tint = HumeColors.TextPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

/** locationName in ProfileView.swift */
private fun locationName(state: String?): String = when (state?.lowercase()) {
    "home" -> "Nh\u00e0 ri\u00eang"
    "not_home", "away" -> "B\u00ean ngo\u00e0i"
    "work", "office" -> "C\u01a1 quan"
    "school" -> "Tr\u01b0\u1eddng h\u1ecdc"
    "gym" -> "Ph\u00f2ng gym"
    "unavailable", "unknown", null -> "Kh\u00f4ng x\u00e1c \u0111\u1ecbnh"
    else -> state
}

private fun HomeEntity.attr(key: String): String? =
    (attributes[key] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
