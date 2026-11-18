package com.smarthome.hume.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.ui.theme.M3ETokens

/**
 * Header muc co action ben phai (vd "Xem tat ca").
 * Tach tu SecHeader (SecurityScreen.kt) de dung chung.
 *
 * @param title Tieu de muc (SectionTitle 16/19,5 Bold).
 * @param action Text action ben phai (labelMedium Bold, mau primary), null = an.
 * @param onAction Callback khi bam action.
 * @param actionLoading Hien loading thay cho action.
 * @param delayMs Delay cho hieu ung riseIn.
 */
@Composable
fun M3ESectionHeader(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    actionLoading: Boolean = false,
    delayMs: Int = 0,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = M3ETokens.Spacing.Xl, bottom = M3ETokens.Spacing.Md)
            .padding(horizontal = M3ETokens.Spacing.Xs)
            .riseIn(delayMs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = M3ETokens.Text.SectionTitle.fontSize,
                lineHeight = M3ETokens.Text.SectionTitle.lineHeight,
            ),
            fontWeight = FontWeight.Bold,
            letterSpacing = M3ETokens.LetterSpacing.Tight,
        )
        if (actionLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(M3ETokens.Icon.XSmall),
                strokeWidth = M3ETokens.Border.Focused,
                color = MaterialTheme.colorScheme.primary,
            )
        } else if (action != null) {
            Text(
                action,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(
                    enabled = onAction != null,
                    onClick = { onAction?.invoke() },
                ),
            )
        }
    }
}
