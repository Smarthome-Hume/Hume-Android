package com.smarthome.hume.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeIcons
import com.smarthome.hume.ui.theme.glassSurface

/** #22c55e presence dot and #ff5252 bell, straight from GreetingHeaderView.swift. */
private val PresenceGreen = Color(0xFF22C55E)
private val BellRed = Color(0xFFFF5252)

/**
 * Header row: glass avatar with the person.hutchet picture, greeting block and
 * the glass bell. Tap the bell for notifications, long press for the managed
 * notification list, exactly like the original onBell / onBellLongPress pair.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeHeader(
    userName: String,
    greeting: String,
    location: String,
    connected: Boolean,
    alertCount: Int,
    onOpenNotifications: () -> Unit,
    avatarUrl: String? = null,
    onManageNotifications: () -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        // ══ AVATAR + DOT ══
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                Modifier.size(55.dp).glassSurface(radius = 28.dp).clip(CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (avatarUrl.isNullOrBlank()) {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = null,
                        tint = HumeColors.TextPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                } else {
                    SubcomposeAsyncImage(
                        model = avatarUrl,
                        contentDescription = userName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        loading = {
                            Icon(
                                Icons.Rounded.Person,
                                contentDescription = null,
                                tint = HumeColors.TextPrimary,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                        error = {
                            Icon(
                                Icons.Rounded.Person,
                                contentDescription = null,
                                tint = HumeColors.TextPrimary,
                                modifier = Modifier.size(24.dp),
                            )
                        },
                    )
                }
            }
            Box(
                Modifier
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (connected) PresenceGreen else HumeColors.Amber)
                    .border(2.dp, Color.White, CircleShape)
            )
        }

        Spacer(Modifier.width(12.dp))

        // ══ TÊN + CHÀO ══
        Column(Modifier.weight(1f)) {
            Text(
                "Hi, " + userName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = HumeColors.TextPrimary,
                maxLines = 1,
            )
            Text(
                greeting,
                fontSize = 16.sp,
                color = HumeColors.TextPrimary.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 2.dp),
            )
            if (location.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 3.dp),
                ) {
                    Icon(
                        Icons.Rounded.LocationOn,
                        contentDescription = null,
                        tint = HumeColors.TextPrimary.copy(alpha = 0.55f),
                        modifier = Modifier.size(10.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        location,
                        fontSize = 12.sp,
                        color = HumeColors.TextPrimary.copy(alpha = 0.55f),
                        maxLines = 1,
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        // ══ CHUÔNG ══
        val hasAlerts = alertCount > 0
        Box(
            Modifier
                .size(50.dp)
                .glassSurface(radius = 25.dp)
                .then(
                    if (hasAlerts) Modifier.background(BellRed.copy(alpha = 0.15f), CircleShape)
                    else Modifier
                )
                .border(
                    1.dp,
                    if (hasAlerts) BellRed.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f),
                    CircleShape,
                )
                .clip(CircleShape)
                .combinedClickable(
                    onClick = onOpenNotifications,
                    onLongClick = onManageNotifications,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                HumeIcons.Bell,
                contentDescription = "Th\u00f4ng b\u00e1o",
                tint = if (hasAlerts) BellRed else HumeColors.TextPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
