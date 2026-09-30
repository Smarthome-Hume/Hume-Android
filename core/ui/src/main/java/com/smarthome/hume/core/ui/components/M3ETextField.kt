package com.smarthome.hume.core.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/**
 * O nhap lieu chuan M3E, dung thong nhat toan app (thay OutlinedTextField roi rac).
 *
 * - Nen tonal filled [surfaceContainer] (thap hon 1 nac so voi the
 *   surfaceContainerHighest de tao cam giac o nhap "lom" xuong), khong vien.
 * - Bo goc 20dp (expressive, nho hon the 26-28dp 1 nac).
 * - Mau tu [MaterialTheme.colorScheme] nen tu dong theo 8 seeds + dark mode.
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
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = if (label.isNotEmpty()) {
            { Text(label, maxLines = 1) }
        } else null,
        placeholder = if (placeholder.isNotEmpty()) {
            { Text(placeholder, maxLines = 1) }
        } else null,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        singleLine = singleLine,
        readOnly = readOnly,
        textStyle = textStyle ?: LocalTextStyle.current,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(20.dp),
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
            focusedLabelColor = cs.onSurfaceVariant,
            unfocusedLabelColor = cs.onSurfaceVariant,
            focusedPlaceholderColor = cs.onSurfaceVariant.copy(alpha = 0.6f),
            unfocusedPlaceholderColor = cs.onSurfaceVariant.copy(alpha = 0.6f),
            cursorColor = cs.primary,
            focusedLeadingIconColor = cs.onSurfaceVariant,
            unfocusedLeadingIconColor = cs.onSurfaceVariant,
            focusedTrailingIconColor = cs.onSurfaceVariant,
            unfocusedTrailingIconColor = cs.onSurfaceVariant,
        ),
    )
}
