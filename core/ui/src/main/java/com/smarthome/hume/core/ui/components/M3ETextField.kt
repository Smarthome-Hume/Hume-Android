package com.smarthome.hume.core.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val cs = MaterialTheme.colorScheme
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(20.dp)

    Column(modifier = modifier) {
        if (label.isNotEmpty()) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = cs.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )
        }
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
                .border(
                    width = if (focused) 2.dp else 1.dp,
                    color = if (focused) cs.primary else cs.outlineVariant.copy(alpha = 0.45f),
                    shape = shape,
                ),
            placeholder = if (placeholder.isNotEmpty()) {
                { Text(placeholder, maxLines = 1) }
            } else null,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            singleLine = singleLine,
            readOnly = readOnly,
            textStyle = textStyle ?: LocalTextStyle.current.copy(
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            ),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
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
                disabledTextColor = cs.onSurface.copy(alpha = 0.6f),
                focusedPlaceholderColor = cs.onSurfaceVariant.copy(alpha = 0.55f),
                unfocusedPlaceholderColor = cs.onSurfaceVariant.copy(alpha = 0.55f),
                cursorColor = cs.primary,
                focusedLeadingIconColor = cs.onSurfaceVariant,
                unfocusedLeadingIconColor = cs.onSurfaceVariant,
                focusedTrailingIconColor = cs.onSurfaceVariant,
                unfocusedTrailingIconColor = cs.onSurfaceVariant,
            ),
        )
    }
}
