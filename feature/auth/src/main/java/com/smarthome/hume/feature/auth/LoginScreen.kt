package com.smarthome.hume.feature.auth

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.smarthome.hume.core.ui.components.M3ECard
import com.smarthome.hume.core.ui.components.M3EConnectedButtonGroup
import com.smarthome.hume.core.ui.components.M3EIcons
import com.smarthome.hume.core.ui.components.M3ESwitch
import com.smarthome.hume.core.ui.components.Ms
import com.smarthome.hume.core.ui.components.MsIcon

private val ServerMode.label: String
    get() = if (this == ServerMode.Local) "Nội bộ" else "Domain"

private val ServerMode.iconGlyph: String
    get() = if (this == ServerMode.Local) Ms.home else Ms.language

private val ServerMode.urlHint: String
    get() = if (this == ServerMode.Local) "http://192.168.1.10:8123" else "https://nha-cua-ban.duckdns.org"

private val ServerMode.helper: String
    get() = if (this == ServerMode.Local) "IP nội bộ trong nhà" else "Tên miền truy cập từ xa"

private val TokenEntryMode.label: String
    get() = when (this) {
        TokenEntryMode.Manual -> "Nhập tay"
        TokenEntryMode.Qr -> "Mã QR"
        TokenEntryMode.Scan -> "Quét"
    }

/** Ve ma QR tu chuoi payload (nau den tren nen trang). */
private fun qrImageBitmap(payload: String, sizePx: Int = 512) =
    runCatching {
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, sizePx, sizePx)
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bmp.setPixel(
                    x, y,
                    if (matrix.get(x, y)) android.graphics.Color.BLACK
                    else android.graphics.Color.WHITE,
                )
            }
        }
        bmp.asImageBitmap()
    }.getOrNull()

/**
 * Man hinh dang nhap M3E: chon Noi bo/Domain, nhap URL + token theo 3 cach
 * (nhap tay / hien ma QR / quet camera), tick ghi nho dang nhap.
 * Token chi duoc luu SAU KHI ket noi thu thanh cong.
 */
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel(factory = LoginViewModel.factory()),
) {
    val state by viewModel.uiState.collectAsState()
    val focus = LocalFocusManager.current
    val clipboard = LocalClipboardManager.current

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Logo
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(76.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                ) {
                    MsIcon(
                        glyph = Ms.home,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(38.dp),
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Hume",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Ngôi nhà thông minh của bạn",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(28.dp))

                // Form card 28dp M3E
                M3ECard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Kết nối Home Assistant",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(20.dp))

                    // Segment Noi bo / Domain
                    Text(
                        text = "Địa chỉ máy chủ",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    M3EConnectedButtonGroup(
                        options = ServerMode.entries,
                        selected = state.serverMode,
                        onSelect = viewModel::onServerModeChange,
                        label = { it.label },
                        icon = { it.iconGlyph },
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.serverUrl,
                        onValueChange = viewModel::onUrlChange,
                        placeholder = { Text(state.serverMode.urlHint) },
                        leadingIcon = { MsIcon(Ms.link, null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focus.moveFocus(FocusDirection.Down) },
                        ),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = state.serverMode.helper,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(20.dp))

                    // 3 cach nhap token
                    Text(
                        text = "Long-Lived Access Token",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    M3EConnectedButtonGroup(
                        options = TokenEntryMode.entries,
                        selected = state.entryMode,
                        onSelect = viewModel::onEntryModeChange,
                        label = { it.label },
                    )
                    Spacer(Modifier.height(12.dp))
                    when (state.entryMode) {
                        TokenEntryMode.Manual -> ManualTokenField(state, viewModel, focus)
                        TokenEntryMode.Qr -> QrShowSection(
                            payload = state.qrPayload,
                            onCopy = {
                                state.qrPayload?.let {
                                    clipboard.setText(AnnotatedString(it))
                                    viewModel.onCopiedQr()
                                }
                            },
                        )
                        TokenEntryMode.Scan -> {
                            QrScanner(
                                onScanned = viewModel::onScanResult,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            QrScanHint()
                        }
                    }

                    // Ghi nho dang nhap
                    Spacer(Modifier.height(20.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Ghi nhớ đăng nhập",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        M3ESwitch(
                            checked = state.rememberMe,
                            onCheckedChange = viewModel::onRememberMeChange,
                        )
                    }
                    Text(
                        text = if (state.rememberMe) {
                            "Lưu địa chỉ + token mã hóa trên máy này."
                        } else {
                            "Không lưu — mở app lại sẽ phải đăng nhập lại."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                // Thong bao thanh cong tam thoi (quet/copy)
                AnimatedVisibility(
                    visible = state.notice != null,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Card(
                        shape = MaterialTheme.shapes.small,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp),
                        ) {
                            MsIcon(
                                M3EIcons.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = state.notice.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = viewModel::onDismissNotice) {
                                Text("Đóng")
                            }
                        }
                    }
                }

                // Loi
                AnimatedVisibility(
                    visible = state.error != null,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Card(
                        shape = MaterialTheme.shapes.small,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp),
                        ) {
                            MsIcon(
                                Ms.error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = state.error.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = viewModel::onLogin,
                    enabled = state.canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp),
                        )
                    } else {
                        Text("Kết nối", style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Token chỉ lưu trên máy này, mã hóa bằng Keystore.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ManualTokenField(
    state: LoginUiState,
    viewModel: LoginViewModel,
    focus: androidx.compose.ui.focus.FocusManager,
) {
    OutlinedTextField(
        value = state.token,
        onValueChange = viewModel::onTokenChange,
        label = { Text("Dán token vào đây") },
        leadingIcon = { MsIcon(Ms.key, null) },
        trailingIcon = {
            IconButton(onClick = viewModel::onToggleTokenVisibility) {
                MsIcon(
                    if (state.tokenVisible) Ms.visibility_off else Ms.visibility,
                    contentDescription = if (state.tokenVisible) "Ẩn token" else "Hiện token",
                )
            }
        },
        singleLine = true,
        visualTransformation = if (state.tokenVisible) VisualTransformation.None
        else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            focus.clearFocus()
            viewModel.onLogin()
        }),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = "Tạo token trong Home Assistant: Hồ sơ → Bảo mật → Token truy cập dài hạn.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun QrShowSection(
    payload: String?,
    onCopy: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (payload != null) {
            val bitmap = remember(payload) { qrImageBitmap(payload) }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Mã QR chứa địa chỉ và token",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(androidx.compose.ui.graphics.Color.White)
                        .padding(12.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onCopy) {
                Text("Sao chép nội dung mã QR")
            }
            Text(
                text = "Quét mã này từ thiết bị khác để đăng nhập mà không cần gõ tay.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        } else {
            Text(
                text = "Nhập địa chỉ máy chủ và token ở tab “Nhập tay” trước, " +
                    "mã QR sẽ hiện ở đây.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
    }
}
