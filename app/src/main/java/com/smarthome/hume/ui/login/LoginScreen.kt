package com.smarthome.hume.ui.login

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowCircleRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.storage.HumeSettings
import com.smarthome.hume.core.storage.SettingsStore
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeShapes
import com.smarthome.hume.ui.theme.glassSurface
import kotlinx.coroutines.launch

/**
 * Port of Views/Profile/LoginView.swift.
 *
 * Same shape as the iOS screen: 78dp circle logo, CocopiHome title, one 28dp
 * card holding the URL segment pair, the URL field, the token field with a
 * reveal toggle, the remember-me row and a solid orange 54dp connect button.
 * The QR / photo paths are iOS-only (Vision framework) and are left out; the
 * rest behaves the same, writing straight into SettingsStore, which is what
 * triggers configure() + connect() in MainActivity.
 */
@Composable
fun LoginScreen(settingsStore: SettingsStore, settings: HumeSettings = HumeSettings()) {
    val scope = rememberCoroutineScope()

    var url by remember(settings.haUrl) {
        mutableStateOf(settings.haUrl.ifBlank { "http://192.168.102.22:8123" })
    }
    var token by remember { mutableStateOf(settings.haToken) }
    var localMode by remember { mutableStateOf(true) }
    var showToken by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }

    Box(Modifier.fillMaxSize().background(HumeColors.Background)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            Column(
                Modifier.padding(top = 50.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier.size(78.dp).clip(CircleShape).background(HumeColors.Card),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Home,
                        contentDescription = null,
                        tint = HumeColors.Orange,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Text("CocopiHome", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = HumeColors.TextPrimary)
                Text(
                    "Trang \u0111i\u1ec1u khi\u1ec3n th\u00f4ng minh",
                    fontSize = 14.sp,
                    color = HumeColors.TextSecondary,
                )
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .glassSurface(radius = HumeShapes.Sheet)
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text(
                    "K\u1ebft n\u1ed1i Home Assistant",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = HumeColors.TextPrimary,
                    modifier = Modifier.fillMaxWidth(),
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("URL Home Assistant", fontSize = 13.sp, color = HumeColors.TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Segment("N\u1ed9i b\u1ed9", Icons.Rounded.Home, localMode, Modifier.weight(1f)) {
                            localMode = true
                        }
                        Segment("Domain", Icons.Rounded.Language, !localMode, Modifier.weight(1f)) {
                            localMode = false
                        }
                    }
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        placeholder = { Text("http://\u2026") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    )
                    Text(
                        if (localMode) "IP n\u1ed9i b\u1ed9 trong nh\u00e0" else "T\u00ean mi\u1ec1n truy c\u1eadp t\u1eeb xa",
                        fontSize = 12.sp,
                        color = HumeColors.TextSecondary,
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Long-Lived Access Token", fontSize = 13.sp, color = HumeColors.TextSecondary)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedTextField(
                            value = token,
                            onValueChange = { token = it },
                            placeholder = { Text("Token") },
                            singleLine = true,
                            visualTransformation = if (showToken) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                        )
                        Box(
                            Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.55f))
                                .clickable { showToken = !showToken },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (showToken) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = "Hi\u1ec7n token",
                                tint = HumeColors.TextPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Text(
                        "L\u1ea5y t\u1eeb HA \u2192 h\u1ed3 s\u01a1 \u2192 Long-Lived Access Tokens \u2192 t\u1ea1o token",
                        fontSize = 12.sp,
                        color = HumeColors.TextSecondary,
                    )
                }

                Row(
                    Modifier.fillMaxWidth().clickable { rememberMe = !rememberMe },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        if (rememberMe) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (rememberMe) HumeColors.Orange else HumeColors.TextSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text("Ghi nh\u1edb t\u00e0i kho\u1ea3n", fontSize = 14.sp, color = HumeColors.TextPrimary)
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(HumeColors.Orange)
                        .clickable(enabled = url.isNotBlank() && token.isNotBlank()) {
                            scope.launch {
                                settingsStore.saveHomeAssistant(url.trim(), token.trim())
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Icons.Rounded.ArrowCircleRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("K\u1ebft n\u1ed1i", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

/** segment(title:icon:active:) in LoginView.swift */
@Composable
private fun Segment(
    title: String,
    icon: ImageVector,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .height(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) HumeColors.OrangeSoft else Color.White.copy(alpha = 0.55f))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (active) HumeColors.Orange else HumeColors.TextSecondary,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            title,
            fontSize = 14.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            color = if (active) HumeColors.TextPrimary else HumeColors.TextSecondary,
        )
    }
}
