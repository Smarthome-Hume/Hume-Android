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
import com.smarthome.hume.ui.theme.neonGlowCircle
import com.smarthome.hume.ui.theme.rememberNeonBeat

/*
 * Header theo ban HTML: avatar 55 + cham trang thai 16, ten 22 bold, loi chao 16, chuong 50.
 *
 * NEON nut chuong (chi khi co thong bao): nen rgba(255,82,82,0.15) va vien
 * 1px rgba(255,82,82,0.4) la TINH; chi den do hat RA NGOAI vien tron dap theo
 * nhip chung. Ben trong vien khong nhap nhay.
 */
private val PresenceGreen = Color(0xFF22C55E)
private val PresenceRed = Color(0xFFEF4444)
private val BellRed = Color(0xFFFF5252)

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
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                Modifier.size(55.dp).clip(CircleShape).background(HumeColors.Gray00),
                contentAlignment = Alignment.Center,
            ) {
                if (avatarUrl.isNullOrBlank()) {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = null,
                        tint = HumeColors.Gray1000,
                        modifier = Modifier.size(26.dp),
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
                                tint = HumeColors.Gray1000,
                                modifier = Modifier.size(26.dp),
                            )
                        },
                        error = {
                            Icon(
                                Icons.Rounded.Person,
                                contentDescription = null,
                                tint = HumeColors.Gray1000,
                                modifier = Modifier.size(26.dp),
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
                    .background(if (connected) PresenceGreen else PresenceRed)
                    .border(2.dp, HumeColors.Background, CircleShape)
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                "Hi, " + userName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = HumeColors.Gray1000,
                maxLines = 1,
            )
            Text(
                greeting,
                fontSize = 16.sp,
                color = HumeColors.Gray600,
                maxLines = 1,
            )
        }

        Spacer(Modifier.width(12.dp))

        val hasAlerts = alertCount > 0
        val beat = if (hasAlerts) rememberNeonBeat() else 0f
        Box(
            Modifier
                .size(50.dp)
                // Den do hat ra NGOAI vien tron.
                .then(
                    if (hasAlerts) Modifier.neonGlowCircle(
                        color = BellRed,
                        spread = 10.dp + 10.dp * beat,
                        intensity = 0.45f + 0.55f * beat,
                        maxAlpha = 0.55f,
                    ) else Modifier
                )
                .clip(CircleShape)
                // Nen va vien TINH.
                .background(if (hasAlerts) BellRed.copy(alpha = 0.15f) else HumeColors.Gray00)
                .border(
                    1.dp,
                    if (hasAlerts) BellRed.copy(alpha = 0.40f) else Color.Transparent,
                    CircleShape,
                )
                .combinedClickable(
                    onClick = onOpenNotifications,
                    onLongClick = onManageNotifications,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                HumeIcons.Bell,
                contentDescription = "Th\u00f4ng b\u00e1o",
                tint = if (hasAlerts) BellRed else HumeColors.Gray1000,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
