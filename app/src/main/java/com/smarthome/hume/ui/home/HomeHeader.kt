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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Icon
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

/** Avatar, greeting, location line and the glowing bell button. */
@Composable
fun HomeHeader(
    userName: String,
    greeting: String,
    location: String,
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
                    Modifier.size(46.dp).clip(CircleShape).background(HumeColors.OrangeSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        userName.take(1).uppercase(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = HumeColors.OrangeDeep,
                    )
                }
                Box(
                    Modifier.size(13.dp).offset(x = 35.dp).clip(CircleShape).background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(9.dp).clip(CircleShape)
                            .background(if (connected) HumeColors.Green else HumeColors.Amber)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "Hi, $userName",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = HumeColors.TextPrimary,
                )
                Text(greeting, fontSize = 14.sp, color = HumeColors.TextSecondary)
                if (location.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = HumeColors.TextSecondary,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(location, fontSize = 11.sp, color = HumeColors.TextSecondary, maxLines = 1)
                    }
                }
            }
        }
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(HumeColors.ChipPink)
                    .border(2.dp, HumeColors.Orange.copy(alpha = 0.30f), CircleShape)
                    .clickable(onClick = onOpenNotifications),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    HumeIcons.Bell,
                    contentDescription = "Th\u00f4ng b\u00e1o",
                    tint = HumeColors.Orange,
                    modifier = Modifier.size(21.dp),
                )
            }
            if (alertCount > 0) {
                Box(
                    Modifier.size(18.dp).clip(CircleShape).background(HumeColors.Red),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (alertCount > 99) "99+" else alertCount.toString(),
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
