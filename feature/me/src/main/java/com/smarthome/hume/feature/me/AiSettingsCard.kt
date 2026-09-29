package com.smarthome.hume.feature.me

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import com.smarthome.hume.core.datastore.AiProvider
import com.smarthome.hume.core.ui.components.M3ESwitch
import com.smarthome.hume.core.ui.components.Ms
import com.smarthome.hume.core.ui.components.MsIcon
import com.smarthome.hume.core.ui.components.rememberHaptic

/**
 * The cau hinh AI (tab Toi): provider, base URL, model, API key (ma hoa),
 * nut kiem tra ket noi. Style theo card dong bo: surfaceContainerHighest 26dp.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsCard(vm: MeViewModel) {
    val settings by vm.aiSettings.collectAsState()
    val testing by vm.aiTesting.collectAsState()
    val testResult by vm.aiTestResult.collectAsState()
    val haptic = rememberHaptic()
    val cs = MaterialTheme.colorScheme

    // State nhap lieu cuc bo (khoi tao tu settings khi load xong)
    var enabled by remember { mutableStateOf(false) }
    var provider by remember { mutableStateOf(AiProvider.OpenAI) }
    var baseUrl by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var keyVisible by remember { mutableStateOf(false) }
    var dirty by remember { mutableStateOf(false) }

    fun doSave() {
        haptic()
        vm.saveAi(
            enabled = enabled, provider = provider,
            baseUrl = baseUrl, model = model, apiKey = apiKey,
        )
        if (apiKey.isNotBlank()) apiKey = ""
        dirty = false
    }

    LaunchedEffect(settings) {
        if (!dirty) {
            enabled = settings.enabled
            provider = settings.provider
            baseUrl = settings.customBaseUrl
            model = settings.model
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
            MsIcon(Ms.auto_awesome, null, tint = cs.primary, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Trí tuệ nhân tạo",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface,
                )
                Text(
                    if (settings.hasApiKey) "Đã lưu API key · ${settings.provider.label}"
                    else "Chưa cấu hình — gợi ý dùng luật có sẵn",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            M3ESwitch(
                checked = enabled,
                onCheckedChange = {
                    haptic(); enabled = it; dirty = true
                    vm.saveAi(enabled = it, provider = provider, baseUrl = baseUrl, model = model, apiKey = apiKey)
                    if (apiKey.isNotBlank()) apiKey = ""
                },
            )
        }

        AnimatedVisibility(
            visible = enabled,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(Modifier.padding(top = 12.dp)) {
                // Hang 1: provider + model canh nhau, moi cai weight 1f
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AiDropdown(
                        label = "Nhà cung cấp",
                        value = provider.label,
                        options = AiProvider.values().map { it.label },
                        onSelect = { label ->
                            val p = AiProvider.values().first { it.label == label }
                            haptic(); provider = p; model = ""; dirty = true
                        },
                    )
                    if (provider == AiProvider.Custom) {
                        // Custom: model tu do (endpoint OpenAI-compatible bat ky)
                        OutlinedTextField(
                            value = model,
                            onValueChange = { model = it; dirty = true },
                            label = { Text("Model") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        AiDropdown(
                            label = "Model",
                            value = if (model.isBlank()) "Mặc định (${defaultModelFor(provider)})"
                            else model,
                            options = listOf("Mặc định") + modelPresetsFor(provider),
                            onSelect = { opt ->
                                haptic()
                                model = if (opt == "Mặc định") "" else opt
                                dirty = true
                            },
                        )
                    }
                }

                // Base URL chi hien cho Custom
                if (provider == AiProvider.Custom) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = { baseUrl = it; dirty = true },
                        label = { Text("Base URL (OpenAI-compatible)") },
                        placeholder = { Text("https://example.com/v1") },
                        leadingIcon = { MsIcon(Ms.link, null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next,
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        // Hien URL mac dinh de user biet diem den
                        "Endpoint: ${providerDefaultUrl(provider)}",
                        fontSize = 11.sp,
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }

                Spacer(Modifier.height(12.dp))
                // Hang 2: API key 1 dong + nut Luu nho ben canh (width co dinh)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it; dirty = true },
                        label = { Text(if (settings.hasApiKey) "API key mới (trống = giữ key cũ)" else "API key") },
                        leadingIcon = { MsIcon(Ms.key, null) },
                        trailingIcon = {
                            IconButton(onClick = { keyVisible = !keyVisible }) {
                                MsIcon(if (keyVisible) Ms.visibility_off else Ms.visibility, null)
                            }
                        },
                        visualTransformation = if (keyVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.weight(1f),
                    )
                    Box(
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
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Lưu",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = cs.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    "Key được mã hóa bằng Android Keystore, không bao giờ gửi đi nơi khác ngoài API của nhà cung cấp.",
                    fontSize = 11.sp,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                )

                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Nut kiem tra ket noi (giu nguyen)
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
                                doSave()
                                vm.testAiConnection()
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
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = cs.onPrimaryContainer,
                        )
                    }
                }

                // Ket qua test
                testResult?.let { msg ->
                    Text(
                        msg,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (msg.startsWith("Kết nối thành công")) cs.primary else cs.error,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                    )
                }
            }
        }
    }
}

/** Dropdown gon dung chung cho provider/model: weight 1f trong Row. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RowScope.AiDropdown(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.weight(1f),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt, maxLines = 1) },
                    onClick = { expanded = false; onSelect(opt) },
                )
            }
        }
    }
}

/** Model goi y theo provider (chon "Mặc định" = de trong, dung model mac dinh). */
private fun modelPresetsFor(p: AiProvider): List<String> = when (p) {
    AiProvider.OpenAI -> listOf("gpt-4o-mini", "gpt-4o", "gpt-4.1-mini", "o4-mini")
    AiProvider.Anthropic -> listOf("claude-3-5-haiku-latest", "claude-sonnet-4-5-20250929", "claude-opus-4-1-20250805")
    AiProvider.Google -> listOf("gemini-2.0-flash", "gemini-2.5-flash", "gemini-2.5-pro")
    AiProvider.DeepSeek -> listOf("deepseek-chat", "deepseek-reasoner")
    AiProvider.Custom -> emptyList()
}

private fun providerDefaultUrl(p: AiProvider): String = when (p) {
    AiProvider.OpenAI -> "https://api.openai.com/v1"
    AiProvider.Anthropic -> "https://api.anthropic.com/v1"
    AiProvider.Google -> "https://generativelanguage.googleapis.com/v1beta/openai"
    AiProvider.DeepSeek -> "https://api.deepseek.com/v1"
    AiProvider.Custom -> "(nhập bên dưới)"
}

private fun defaultModelFor(p: AiProvider): String = when (p) {
    AiProvider.OpenAI -> "gpt-4o-mini"
    AiProvider.Anthropic -> "claude-3-5-haiku-latest"
    AiProvider.Google -> "gemini-2.0-flash"
    AiProvider.DeepSeek -> "deepseek-chat"
    AiProvider.Custom -> ""
}
