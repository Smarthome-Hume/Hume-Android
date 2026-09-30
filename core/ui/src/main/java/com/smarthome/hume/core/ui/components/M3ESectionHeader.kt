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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Header muc co action ben phai (vd "Xem tat ca").
 * Tach tu SecHeader (SecurityScreen.kt) de dung chung.
 *
 * @param title Tieu de muc (titleMedium Bold).
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
            .padding(top = 20.dp, bottom = 12.dp)
            .padding(horizontal = 4.dp)
            .riseIn(delayMs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.1).sp,
        )
        if (actionLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
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
