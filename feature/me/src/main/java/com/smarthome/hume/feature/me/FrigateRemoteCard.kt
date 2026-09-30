package com.smarthome.hume.feature.me

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.ui.components.Ms
import com.smarthome.hume.core.ui.components.MsIcon
import com.smarthome.hume.core.ui.components.rememberHaptic

/**
 * The cau hinh xem camera Frigate tu xa (tab Toi): hostname Frigate tren
 * Cloudflare Tunnel + Cloudflare Access Service Token. Khi app di duong
 * remote (khong VPN), snapshot/events/clip Frigate di qua day; khi di
 * local thi van dung IP LAN nhu cu. Style theo card AI: surfaceContainerHighest 26dp.
 */
@Composable
fun FrigateRemoteCard(vm: MeViewModel) {
    val settings by vm.frigateRemote.collectAsState()
    val haptic = rememberHaptic()
    val cs = MaterialTheme.colorScheme

    var url by remember { mutableStateOf("") }
    var clientId by remember { mutableStateOf("") }
    var clientSecret by remember { mutableStateOf("") }
    var secretVisible by remember { mutableStateOf(false) }
    var dirty by remember { mutableStateOf(false) }

    fun doSave() {
        haptic()
        vm.saveFrigateRemote(url = url, cfClientId = clientId, cfClientSecret = clientSecret)
        if (clientSecret.isNotBlank()) clientSecret = ""
        dirty = false
    }

    LaunchedEffect(settings) {
        if (!dirty) {
            url = settings.remoteUrl
            clientId = settings.cfClientId
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(cs.surfaceContainerHighest)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MsIcon(Ms.videocam, null, tint = cs.primary, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Camera từ xa",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface,
                )
                Text(
                    if (settings.hasAccess) "Đã cấu hình Cloudflare Access"
                    else "Chưa cấu hình — tắt VPN sẽ không xem được camera",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        Column(Modifier.padding(top = 12.dp)) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it; dirty = true },
                label = { Text("Frigate qua Cloudflare") },
                placeholder = { Text("https://frigate.haiha93.xyz") },
                leadingIcon = { MsIcon(Ms.link, null, modifier = Modifier.size(20.dp)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next,
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = clientId,
                onValueChange = { clientId = it; dirty = true },
                label = { Text("CF-Access Client ID") },
                placeholder = { Text("Service Token ID") },
                leadingIcon = { MsIcon(Ms.key, null, modifier = Modifier.size(20.dp)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = clientSecret,
                    onValueChange = { clientSecret = it; dirty = true },
                    label = { Text("CF-Access Client Secret", maxLines = 1) },
                    placeholder = { Text(if (settings.hasAccess) "Trống = giữ secret cũ" else "") },
                    leadingIcon = { MsIcon(Ms.lock, null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        IconButton(onClick = { secretVisible = !secretVisible }) {
                            MsIcon(
                                if (secretVisible) Ms.visibility_off else Ms.visibility, null,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    },
                    visualTransformation = if (secretVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Row(
                    Modifier
                        .width(88.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(cs.surfaceContainerHigh)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = ::doSave,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Lưu",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }
            Text(
                "Tạo Service Token ở Cloudflare Zero Trust → Access → Service Tokens, " +
                    "rồi thêm policy Service Auth cho hostname Frigate.",
                fontSize = 11.sp,
                color = cs.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
            )
        }
    }
}
