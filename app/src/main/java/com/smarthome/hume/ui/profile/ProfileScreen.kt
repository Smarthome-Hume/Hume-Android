package com.smarthome.hume.ui.profile

import android.content.Context
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.core.scene.ManagedKind
import com.smarthome.hume.core.storage.HumeSettings
import com.smarthome.hume.core.storage.SettingsStore
import com.smarthome.hume.ui.manage.ManageListSheet
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeShapes
import com.smarthome.hume.ui.theme.glassPill
import com.smarthome.hume.ui.theme.glassSurface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive

/**
 * Profile tab, rebuilt from ProfileView.swift.
 *
 * Same three groups as the original: the orange owner card, the account rows
 * (id, name, email, phone, location) with copy and edit actions, and the
 * automation entry, followed by the connection pill and the logout button.
 * "Qu\u1ea3n l\u00fd thi\u1ebft b\u1ecb" opens the managed light list and the automation row
 * opens the managed notification list, which is what feeds the Home header.
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
    var manage by remember { mutableStateOf<ManagedKind?>(null) }

    val person: HomeEntity? = entities["person.hutchet"]
    val personName = person?.attr("friendly_name") ?: "H\u1ea3i H\u00e0"
    val userId = person?.attr("user_id") ?: "\u2014"
    val avatarUrl = person?.attr("entity_picture")?.let { settings.haUrl.trimEnd('/') + it }

    Column(
        Modifier
            .fillMaxSize()
            .background(HumeColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Th\u00f4ng tin", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = HumeColors.TextPrimary)

        // GroupGlassContainer(cornerRadius: 47, innerPadding: 8) { orangeCard }
        Box(Modifier.fillMaxWidth().glassSurface(radius = 47.dp).padding(8.dp)) {
            OwnerCard(personName, avatarUrl) { manage = ManagedKind.LIGHTS }
        }

        // Account rows
        Column(
            Modifier.fillMaxWidth().glassSurface(radius = 47.dp).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProfileRow(Icons.Rounded.Badge, "ID Ng\u01b0\u1eddi d\u00f9ng", userId, copy = true)
            ProfileRow(Icons.Rounded.Person, "T\u00ean ng\u01b0\u1eddi d\u00f9ng", personName, copy = true)
            ProfileRow(
                Icons.Rounded.MailOutline,
                "Email",
                email.ifEmpty { "Ch\u01b0a c\u1eadp nh\u1eadt" },
                copy = false,
                onClick = { editing = "email" },
            )
            ProfileRow(
                Icons.Rounded.Phone,
                "\u0110i\u1ec7n tho\u1ea1i",
                phone.ifEmpty { "Ch\u01b0a c\u1eadp nh\u1eadt" },
                copy = false,
                onClick = { editing = "phone" },
            )
            ProfileRow(
                Icons.Rounded.Place,
                "V\u1ecb tr\u00ed",
                locationName(person?.state),
                copy = true,
            )
        }

        // Automation entry -> managed notification list
        Row(
            Modifier
                .fillMaxWidth()
                .glassSurface(radius = 47.dp)
                .clickable { manage = ManagedKind.NOTIF }
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = HumeColors.Orange, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Text("T\u1ef1 \u0111\u1ed9ng & c\u1ea3nh b\u00e1o", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = HumeColors.TextPrimary, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = HumeColors.TextSecondary, modifier = Modifier.size(16.dp))
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
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "\u00b7 " + entities.size + " th\u1ef1c th\u1ec3",
                fontSize = 13.sp,
                color = HumeColors.TextSecondary,
            )
        }

        // Logout
        Box(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF44336).copy(alpha = 0.12f))
                .clickable {
                    ha.disconnect()
                    CoroutineScope(Dispatchers.IO).launch { settingsStore.logout() }
                }
                .padding(horizontal = 24.dp, vertical = 9.dp),
        ) {
            Text("Tho\u00e1t t\u00e0i kho\u1ea3n", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFEF5350))
        }
        Spacer(Modifier.height(48.dp))
    }

    manage?.let { kind ->
        ManageListSheet(kind = kind, ha = ha, onDismiss = { manage = null })
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
private fun OwnerCard(name: String, avatarUrl: String?, onManageDevices: () -> Unit) {
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
            Box(
                Modifier.size(60.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                if (avatarUrl != null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(60.dp).clip(CircleShape),
                    )
                } else {
                    Icon(Icons.Rounded.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
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
                Icon(Icons.Rounded.PhoneIphone, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Qu\u1ea3n l\u00fd thi\u1ebft b\u1ecb", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White, modifier = Modifier.weight(1f))
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
            }
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** ProfileCardRow: 50dp icon circle, label over value, copy or edit trailing button, radius 35. */
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
            Modifier.size(50.dp).clip(CircleShape).background(HumeColors.Background),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = HumeColors.TextSecondary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(15.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp, color = HumeColors.TextSecondary)
            Text(
                value.ifEmpty { "\u2014" },
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = HumeColors.TextPrimary,
                maxLines = 1,
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
                Text("\u0110\u00e3 copy", fontSize = 11.sp, color = HumeColors.TextSecondary)
            } else {
                Icon(
                    if (copy) Icons.Rounded.ContentCopy else Icons.Rounded.Edit,
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
