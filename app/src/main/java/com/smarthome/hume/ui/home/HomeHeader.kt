package com.smarthome.hume.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeIcons

/**
 * Header from the HTML prototype: avatar with an online dot, a large greeting,
 * and a round bell button with a soft glow ring.
 */
@Composable
fun HomeHeader(
    userName: String,
    greeting: String,
    connected: Boolean,
    alertCount: Int,
    onOpenNotifications: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(HumeColors.OrangeSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        userName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = HumeColors.OrangeDeep,
                    )
                }
                Box(
                    Modifier
                        .size(14.dp)
                        .offset(x = 40.dp, y = 0.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (connected) HumeColors.Green else HumeColors.Amber)
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    "Hi, $userName",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    greeting,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        BellButton(alertCount = alertCount, onClick = onOpenNotifications)
    }
}

@Composable
private fun BellButton(alertCount: Int, onClick: () -> Unit) {
    Box(contentAlignment = Alignment.TopEnd) {
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(HumeColors.OrangeSofter)
                .border(2.dp, HumeColors.Orange.copy(alpha = 0.35f), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                HumeIcons.Bell,
                contentDescription = "Th\u00f4ng b\u00e1o",
                tint = HumeColors.Orange,
                modifier = Modifier.size(24.dp),
            )
        }
        if (alertCount > 0) {
            Box(
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(HumeColors.Red),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (alertCount > 99) "99+" else alertCount.toString(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
