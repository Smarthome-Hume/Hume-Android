package com.smarthome.hume.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.foundation.layout.PaddingValues
import com.smarthome.hume.core.ui.theme.M3ETokens

/**
 * Nhom subtab kieu demo v4 (.esub/.dvsegi):
 * nen surfaceContainer pill, padding 4dp, gap 2dp;
 * tab chon = primaryContainer + chu onPrimaryContainer (khong icon check
 * truoc title, 2026-10-01 user yeu cau), tab thuong = chu onSurfaceVariant.
 */
@Composable
fun EsubGroup(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    /** Cho phep spec khac nhau giua cac tab (Dien: 12sp/700/pad 10-10; An ninh: 12sp/600/pad 8-10). */
    // B-07 (Q4(b) lam tron): 12.5sp -> Text.BodySmall 12sp (DOI GIA TRI 12.5 -> 12sp).
    fontSize: TextUnit = M3ETokens.Text.BodySmall.fontSize,
    fontWeight: FontWeight = FontWeight.Bold,
    itemPadding: PaddingValues = PaddingValues(vertical = M3ETokens.Spacing.CardGap, horizontal = M3ETokens.Spacing.CardGap),
    /** Neu co: hien icon thay vi chu (vd camera picker khong du cho). */
    icons: List<String>? = null,
) {
    val pill = RoundedCornerShape(M3ETokens.Corner.Pill)
    Row(
        modifier = modifier
            .clip(pill)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            // Tong chieu cao chuan 46dp (muc 23): min 46dp — padding/text
            // tieu chuan van du 46dp, noi dung lon hon thi tu gian (khong clip).
            .heightIn(min = M3ETokens.Height.EsubGroup)
            .padding(M3ETokens.Spacing.Xs),
        horizontalArrangement = Arrangement.spacedBy(M3ETokens.Spacing.Xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { i, label ->
            val isSel = i == selectedIndex
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(pill)
                    .background(
                        if (isSel) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0f),
                    )
                    .pressMorph(0.94f) { onSelect(i) }
                    .padding(itemPadding),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    val iconKey = icons?.getOrNull(i)
                    if (iconKey != null) {
                        // Che do icon: hien icon thay chu.
                        // B-07: icon 22dp GIU NGUYEN literal — khong co token
                        // icon 22dp chung; Icon.EnvTile (22dp) sai ngu nghia.
                        MsIcon(
                            iconKey,
                            contentDescription = label,
                            modifier = Modifier.size(22.dp),
                            tint = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            label,
                            fontSize = fontSize,
                            fontWeight = fontWeight,
                            color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            // demo .esubi{white-space:nowrap}: nhan khong xuong dong
                            // (camera "Phòng khách", "Phòng ngủ chính")
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
