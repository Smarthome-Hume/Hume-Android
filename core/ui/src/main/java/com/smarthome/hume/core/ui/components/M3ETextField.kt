package com.smarthome.hume.core.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import com.smarthome.hume.core.ui.theme.M3ETokens

/**
 * O nhap lieu chuan M3E, dung thong nhat toan app.
 *
 * Khac biet so voi Material3 mac dinh (de hoa hop voi he M3E expressive):
 * - Nhan IN DAM nam NGOAI phia tren o nhap (khong phai floating label ti hon
 *   ben trong) — de doc, cung ngon ngu voi cac tieu de trong he thong.
 * - Vien subtle [outlineVariant] 1dp; khi focus: vien [primary] 2dp.
 * - Nen tonal filled [surfaceContainer] (thap hon 1 nac so voi the
 *   surfaceContainerHighest de tao cam giac o nhap "lom" xuong).
 * - Bo goc 20dp (expressive, nho hon the 26-28dp 1 nac).
 * - Mau tu [MaterialTheme.colorScheme] nen tu dong theo 8 seeds + dark mode.
 *
 * Modifier truyen vao ap len Column ngoai (nhan + o nhap) nen van dung duoc
 * voi ExposedDropdownMenuBox: menu van neo duoi o nhap nhu cu.
 */
@Composable
fun M3ETextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    placeholder: String = "",
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    singleLine: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    // (2026-11-19, BOM 2026.11.01 / M3 1.5.0): VisualTransformation deprecated,
    // thay bang OutputTransformation. Pass null = hien text that (nhu
    // VisualTransformation.None truoc day); pass PasswordOutputTransformation
    // (duoi file) = an mat khau.
    outputTransformation: OutputTransformation? = null,
) {
    val cs = MaterialTheme.colorScheme
    var focused by remember { mutableStateOf(false) }
    // B-02: bo 20dp -> Corner.TextField (cung gia tri).
    val shape = RoundedCornerShape(M3ETokens.Corner.TextField)

    // (2026-11-19, BOM 2026.11.01 / M3 1.5.0): TextField value-based chi co
    // visualTransformation (deprecated), KHONG co outputTransformation —
    // chi TextField state-based moi co. Cau noi sang state-based ben trong,
    // giu nguyen API value/onValueChange cho 17 call site:
    // - state la nguon that ben trong o nhap; value ben ngoai dong bo 2 chieu.
    // - lastExternal chan echo: gia tri do ben ngoai day vao (vd xoa trang
    //   sau khi Luu) KHONG duoc day nguoc lai onValueChange — neu khong
    //   AiSettingsCard/FrigateRemoteCard se bat lai dirty=true ngay sau Luu.
    val state = rememberTextFieldState(value)
    var lastExternal by remember { mutableStateOf(value) }
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    LaunchedEffect(value) {
        if (value != lastExternal) {
            lastExternal = value
            if (state.text.toString() != value) {
                state.setTextAndPlaceCursorAtEnd(value)
            }
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.text.toString() }.collect { text ->
            if (text != lastExternal) {
                lastExternal = text
                currentOnValueChange(text)
            }
        }
    }

    Column(modifier = modifier) {
        if (label.isNotEmpty()) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = cs.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.padding(start = M3ETokens.Spacing.Xs, bottom = M3ETokens.Spacing.Sm),
            )
        }
        TextField(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
                .border(
                    width = if (focused) M3ETokens.Border.Focused else M3ETokens.Border.Thin,
                    color = if (focused) cs.primary else cs.outlineVariant.copy(alpha = M3ETokens.Alpha.Border),
                    shape = shape,
                ),
            placeholder = if (placeholder.isNotEmpty()) {
                { Text(placeholder, maxLines = 1) }
            } else null,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            readOnly = readOnly,
            textStyle = textStyle ?: LocalTextStyle.current
                .merge(MaterialTheme.typography.bodyMedium)
                .copy(fontWeight = FontWeight.Medium),
            outputTransformation = outputTransformation,
            keyboardOptions = keyboardOptions,
            onKeyboardAction = keyboardActions.toHandler(keyboardOptions.imeAction),
            lineLimits = if (singleLine) TextFieldLineLimits.SingleLine
            else TextFieldLineLimits.MultiLine(),
            shape = shape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = cs.surfaceContainer,
                unfocusedContainerColor = cs.surfaceContainer,
                disabledContainerColor = cs.surfaceContainer,
                errorContainerColor = cs.surfaceContainer,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                focusedTextColor = cs.onSurface,
                unfocusedTextColor = cs.onSurface,
                disabledTextColor = cs.onSurface.copy(alpha = M3ETokens.Alpha.DisabledText),
                focusedPlaceholderColor = cs.onSurfaceVariant.copy(alpha = M3ETokens.Alpha.Placeholder),
                unfocusedPlaceholderColor = cs.onSurfaceVariant.copy(alpha = M3ETokens.Alpha.Placeholder),
                cursorColor = cs.primary,
                focusedLeadingIconColor = cs.onSurfaceVariant,
                unfocusedLeadingIconColor = cs.onSurfaceVariant,
                focusedTrailingIconColor = cs.onSurfaceVariant,
                unfocusedTrailingIconColor = cs.onSurfaceVariant,
            ),
        )
    }
}

/**
 * An mat khau, thay cho PasswordVisualTransformation (deprecated tu Compose 1.13).
 * Dung lam `outputTransformation` cua M3ETextField khi truong dang o che do an.
 *
 * QUAN TRONG: replace RIENG TUNG KY TU (khong replace ca chuoi 1 lan).
 * OutputTransformation coi moi lan replace la 1 khoi nguyen tu: replace ca
 * chuoi "abc" -> "***" 1 lan se bien ca field thanh 1 khoi khong tach duoc,
 * backspace se xoa sach toan bo 1 lan. Replace tung ky tu thi moi ky tu la
 * 1 don vi rieng — backspace xoa dung 1 ky tu nhu PasswordVisualTransformation cu.
 * Ky tu che la '•' — giong mask mac dinh cua PasswordVisualTransformation.
 */
val PasswordOutputTransformation: OutputTransformation = OutputTransformation {
    for (i in 0 until length) {
        replace(i, i + 1, "•")
    }
}

/**
 * Chuyen KeyboardActions cu (value-based TextField) sang KeyboardActionHandler
 * cua TextField state-based, chon dung handler theo imeAction hien tai.
 * Tra null khi khong co handler cho imeAction do -> TextField chay hanh vi
 * mac dinh (nhu truoc: Done = an ban phim, Next/Previous = chuyen focus).
 * Handler cu nhan KeyboardActionScope: scope nay chuyen defaultKeyboardAction
 * ve performDefaultAction cua API moi.
 */
private fun KeyboardActions.toHandler(imeAction: ImeAction): KeyboardActionHandler? {
    val action: (KeyboardActionScope.() -> Unit)? = when (imeAction) {
        ImeAction.Done -> onDone
        ImeAction.Go -> onGo
        ImeAction.Next -> onNext
        ImeAction.Previous -> onPrevious
        ImeAction.Search -> onSearch
        ImeAction.Send -> onSend
        else -> null
    } ?: return null
    return KeyboardActionHandler { performDefaultAction ->
        val scope = object : KeyboardActionScope {
            override fun defaultKeyboardAction(imeAction: ImeAction) {
                performDefaultAction()
            }
        }
        scope.action()
    }
}
