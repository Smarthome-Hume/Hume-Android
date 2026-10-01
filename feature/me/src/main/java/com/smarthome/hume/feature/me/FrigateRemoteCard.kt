package com.smarthome.hume.feature.me

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.smarthome.hume.core.ui.components.M3ETextField
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
    val testing by vm.frigateTesting.collectAsState()
    val testResult by vm.frigateTestResult.collectAsState()
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
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface,
                )
                Text(
                    if (settings.hasAccess) "Đã cấu hình Cloudflare Access"
                    else "Chưa cấu hình — tắt VPN sẽ không xem được camera",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        Column(Modifier.padding(top = 12.dp)) {
            M3ETextField(
                value = url,
                onValueChange = { url = it; dirty = true },
                label = "Frigate qua Cloudflare",
                placeholder = "https://frigate.haiha93.xyz",
                leadingIcon = { MsIcon(Ms.link, null, modifier = Modifier.size(20.dp)) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            M3ETextField(
                value = clientId,
                onValueChange = { clientId = it; dirty = true },
                label = "CF-Access Client ID",
                placeholder = "Service Token ID",
                leadingIcon = { MsIcon(Ms.key, null, modifier = Modifier.size(20.dp)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            // Secret + nut Luu: can day de nut thang hang voi o nhap.
            Row(verticalAlignment = Alignment.Bottom) {
                M3ETextField(
                    value = clientSecret,
                    onValueChange = { clientSecret = it; dirty = true },
                    label = "CF-Access Client Secret",
                    placeholder = if (settings.hasAccess) "Trống = giữ secret cũ" else "",
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
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Row(
                    Modifier
                        .width(88.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(cs.primary)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = ::doSave,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "Lưu",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onPrimary,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Kiem tra bang gia tri dang nhap (secret trong = dung secret da luu)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(cs.primaryContainer)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = !testing,
                        ) {
                            haptic()
                            // Luu truoc roi moi test (save giu secret cu khi o nhap
                            // trong) -> ket qua test phan anh dung cau hinh that.
                            val urlNow = url
                            val idNow = clientId
                            val secretNow = clientSecret.ifBlank { settings.cfClientSecret }
                            doSave()
                            vm.testFrigate(
                                url = urlNow,
                                cfClientId = idNow,
                                cfClientSecret = secretNow,
                            )
                        }
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                ) {
                    if (testing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = cs.onPrimaryContainer,
                        )
                    } else {
                        MsIcon(Ms.check, null, tint = cs.onPrimaryContainer, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (testing) "Đang kiểm tra…" else "Kiểm tra kết nối",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = cs.onPrimaryContainer,
                    )
                }
            }
            testResult?.let { msg ->
                Text(
                    msg,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = if (msg.startsWith("Kết nối OK")) cs.primary else cs.error,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                )
            }
        }
    }
}
