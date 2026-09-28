package com.smarthome.hume.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Nhom subtab kieu demo v4 (.esub/.dvsegi):
 * nen surfaceContainer pill, padding 4dp, gap 2dp;
 * tab chon = primaryContainer + chu onPrimaryContainer + icon check hien
 * (animate width/opacity/scale nhu demo), tab thuong = chu onSurfaceVariant.
 */
@Composable
fun EsubGroup(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pill = RoundedCornerShape(999.dp)
    Row(
        modifier = modifier
            .clip(pill)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
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
                    .clickable { onSelect(i) }
                    .padding(vertical = 10.dp, horizontal = 10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    AnimatedVisibility(
                        visible = isSel,
                        enter = expandHorizontally() + fadeIn() + scaleIn(),
                        exit = shrinkHorizontally() + fadeOut() + scaleOut(),
                    ) {
                        Icon(
                            M3EIcons.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(17.dp),
                        )
                    }
                    Text(
                        label,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
