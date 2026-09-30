package com.smarthome.hume.core.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Tieu de muc dung chung toan app (thay the SecTitle/SectionTitle rieng le
 * o Brief/Home/Me). Kieu titleMedium 16sp Bold, letterSpacing -0.1sp.
 */
@Composable
fun M3ESectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.1).sp,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(start = 4.dp, end = 4.dp),
    )
}

/**
 * Nhan muc nho dang VIET HOA dung chung (vd "NHAN DINH", "TONG HOP").
 * Kieu labelSmall 11sp Bold + letterSpacing 1.5sp.
 *
 * @param light Neu true dung mau tren nen primaryContainer (vd card AI).
 */
@Composable
fun M3ESectionLabel(
    text: String,
    light: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.5.sp,
        color = if (light) cs.onPrimaryContainer else cs.onSurfaceVariant,
        modifier = modifier,
    )
}
