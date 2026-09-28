package com.smarthome.hume.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.ui.components.M3EIcons

/**
 * FAB speed-dial trang Nha theo demo v4 (.fabwrap/.fab/.fmi):
 * trigger 56dp bo 16dp primaryContainer, icon add; mo ra thanh tron
 * primary; 4 mon: Bat den / Dieu hoa 26° / Bat an ninh / Tiet kiem dien.
 */
@Composable
fun HomeFabMenu(
    onTurnOnLights: () -> Unit,
    onAc26: () -> Unit,
    onArmAway: () -> Unit,
    onEco: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val items = listOf(
        Triple("Bật đèn", M3EIcons.Light, onTurnOnLights),
        Triple("Điều hoà 26°", M3EIcons.Climate, onAc26),
        Triple("Bật an ninh", M3EIcons.Shield, onArmAway),
        Triple("Tiết kiệm điện", M3EIcons.Power, onEco),
    )

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        AnimatedVisibility(
            visible = open,
            enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                items.forEach { (label, icon, action) ->
                    FabMenuItem(label = label, icon = icon, onClick = {
                        open = false
                        action()
                    })
                }
            }
        }
        // trigger
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .shadow(8.dp, RoundedCornerShape(if (open) 28.dp else 16.dp))
                .clip(RoundedCornerShape(if (open) 28.dp else 16.dp))
                .background(
                    if (open) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.primaryContainer,
                )
                .clickable { open = !open },
        ) {
            Icon(
                if (open) M3EIcons.Close else M3EIcons.Add,
                contentDescription = null,
                tint = if (open) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(if (open) 20.dp else 24.dp),
            )
        }
    }
}

@Composable
private fun FabMenuItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .height(56.dp)
            .shadow(8.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
    ) {
        Icon(
            icon, contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(24.dp),
        )
        Text(
            label,
            fontSize = 14.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
