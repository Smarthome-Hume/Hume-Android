package com.smarthome.hume.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.core.model.RoomConfig
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeIcons
import com.smarthome.hume.ui.theme.glassSurface

/** Card geometry from HumeTheme.swift: Radius.card = 34, room card height 240. */
private val CardRadius = 34.dp
private val CardHeight = 240.dp

/** Small square tile used in the staggered room area. */
data class SmallTile(
    val icon: ImageVector,
    val value: String,
    val label: String,
    val entityId: String? = null,
)

/**
 * The staggered two column area from the prototype: swipeable room cards with
 * page dots, mixed with small sensor tiles.
 */
@Composable
fun RoomsShowcase(
    climateRooms: List<RoomConfig>,
    otherRooms: List<RoomConfig>,
    entities: Map<String, HomeEntity>,
    leftTiles: List<SmallTile>,
    rightTiles: List<SmallTile>,
    onOpenRoom: (RoomConfig) -> Unit,
    onToggleLight: (RoomConfig) -> Unit,
    onTileClick: (String) -> Unit,
    onAdjustTarget: (RoomConfig, Double) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            TilePager(leftTiles, onTileClick)
            RoomPager(
                rooms = climateRooms,
                entities = entities,
                onOpenRoom = onOpenRoom,
                onToggleLight = onToggleLight,
                onAdjustTarget = onAdjustTarget,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            RoomPager(
                rooms = otherRooms,
                entities = entities,
                onOpenRoom = onOpenRoom,
                onToggleLight = onToggleLight,
                onAdjustTarget = onAdjustTarget,
            )
            TilePager(rightTiles, onTileClick)
        }
    }
}

@Composable
private fun TilePager(tiles: List<SmallTile>, onTileClick: (String) -> Unit) {
    if (tiles.isEmpty()) return
    val pages = tiles.chunked(2)
    val pagerState = rememberPagerState(pageCount = { pages.size })
    Column {
        HorizontalPager(state = pagerState) { page ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                pages[page].forEach { tile -> SmallTileCard(tile, onTileClick) }
            }
        }
        if (pages.size > 1) {
            Spacer(Modifier.height(8.dp))
            PagerDots(pages.size, pagerState.currentPage)
        }
    }
}

/**
 * SmallSensorCardView.swift. Card height 64 with radius 32, and a 60 point
 * glass circle inside it, so the card corner peeks 2 points around the circle.
 * Value 16 medium, name 14 at 70 percent. The icon container stays 64 wide so
 * the text column always starts at the same x.
 */
@Composable
private fun SmallTileCard(tile: SmallTile, onTileClick: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .glassSurface(radius = 32.dp)
            .clickable(enabled = tile.entityId != null) { tile.entityId?.let(onTileClick) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(64.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(60.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(tile.icon, contentDescription = null, tint = HumeColors.TextPrimary, modifier = Modifier.size(26.dp))
            }
        }
        Column(Modifier.padding(start = 12.dp, end = 8.dp)) {
            Text(tile.value, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = HumeColors.TextPrimary, maxLines = 1)
            Text(tile.label, fontSize = 14.sp, color = HumeColors.TextSecondary, maxLines = 1)
        }
    }
}

@Composable
private fun RoomPager(
    rooms: List<RoomConfig>,
    entities: Map<String, HomeEntity>,
    onOpenRoom: (RoomConfig) -> Unit,
    onToggleLight: (RoomConfig) -> Unit,
    onAdjustTarget: (RoomConfig, Double) -> Unit,
) {
    if (rooms.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { rooms.size })
    Column {
        HorizontalPager(state = pagerState) { page ->
            RoomCardLarge(
                room = rooms[page],
                entities = entities,
                onOpen = { onOpenRoom(rooms[page]) },
                onToggleLight = { onToggleLight(rooms[page]) },
                onAdjustTarget = { delta -> onAdjustTarget(rooms[page], delta) },
            )
        }
        if (rooms.size > 1) {
            Spacer(Modifier.height(8.dp))
            PagerDots(rooms.size, pagerState.currentPage)
        }
    }
}

/**
 * RoomCardView.swift / ClimateRoomCardView.swift.
 *
 * The card surface is always the neutral glass panel. Orange is not decoration:
 * it is the light on indicator, and it is applied exactly the way the SwiftUI
 * card applies it. A translucent orange wash at 10 percent, an orange border at
 * 40 percent, an orange glow, and an orange icon tint. Lights off means no
 * orange anywhere on the card.
 */
@Composable
private fun RoomCardLarge(
    room: RoomConfig,
    entities: Map<String, HomeEntity>,
    onOpen: () -> Unit,
    onToggleLight: () -> Unit,
    onAdjustTarget: (Double) -> Unit,
) {
    val lightOn = entities[room.lightEntity]?.isOn == true
    val temp = entities.num(room.tempEntity, 0)
    val humidity = entities.num(room.humidityEntity, 0)
    val target = entities.attr(room.climateEntity ?: "", "temperature")
    val shape = RoundedCornerShape(CardRadius)

    Box(
        Modifier
            .fillMaxWidth()
            .height(CardHeight)
            // Orange glow only while the light is on, radius 10 like the original.
            .shadow(
                elevation = if (lightOn) 10.dp else 4.dp,
                shape = shape,
                ambientColor = if (lightOn) HumeColors.Orange.copy(alpha = 0.27f) else Color(0x14000000),
                spotColor = if (lightOn) HumeColors.Orange.copy(alpha = 0.27f) else Color(0x14000000),
            )
            .glassSurface(radius = CardRadius, elevation = 0.dp)
            // The 10 percent orange wash sits on top of the glass, not instead of it.
            .then(if (lightOn) Modifier.background(HumeColors.Orange.copy(alpha = 0.10f), shape) else Modifier)
            .then(if (lightOn) Modifier.border(1.dp, HumeColors.Orange.copy(alpha = 0.40f), shape) else Modifier)
            .clickable(onClick = onOpen)
            .padding(16.dp),
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    room.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HumeColors.TextPrimary,
                    maxLines = 2,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier
                        .size(55.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.35f))
                        .clickable(onClick = onToggleLight),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        HumeIcons.room(room.icon),
                        contentDescription = null,
                        tint = if (lightOn) HumeColors.Orange else HumeColors.TextSecondary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                    Text(
                        temp,
                        fontSize = 46.sp,
                        fontWeight = FontWeight.ExtraLight,
                        color = HumeColors.TextPrimary,
                        maxLines = 1,
                    )
                    Text("\u00b0", fontSize = 34.sp, fontWeight = FontWeight.ExtraLight, color = HumeColors.TextPrimary)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "$humidity%",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Light,
                        color = HumeColors.TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                if (room.hasClimate && room.climateEntity != null) {
                    TargetStepper(target = target, onAdjustTarget = onAdjustTarget)
                }
            }
        }
    }
}

/** tempStepper() in ClimateRoomCardView.swift, element radius 18. */
@Composable
private fun TargetStepper(target: String?, onAdjustTarget: (Double) -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.45f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Rounded.KeyboardArrowUp,
            contentDescription = "T\u0103ng",
            tint = HumeColors.TextPrimary,
            modifier = Modifier.size(22.dp).clickable { onAdjustTarget(1.0) },
        )
        Text(
            if (target == null) "--" else "$target\u00b0",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = HumeColors.TextPrimary,
        )
        Icon(
            Icons.Rounded.KeyboardArrowDown,
            contentDescription = "Gi\u1ea3m",
            tint = HumeColors.TextPrimary,
            modifier = Modifier.size(22.dp).clickable { onAdjustTarget(-1.0) },
        )
    }
}

@Composable
fun PagerDots(count: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(start = 4.dp)) {
        repeat(count) { index ->
            val active = index == current
            Box(
                Modifier
                    .height(6.dp)
                    .width(if (active) 18.dp else 6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (active) HumeColors.Orange else HumeColors.Divider)
            )
        }
    }
}
