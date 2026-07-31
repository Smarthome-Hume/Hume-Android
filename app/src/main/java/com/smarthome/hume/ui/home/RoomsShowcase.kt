package com.smarthome.hume.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.core.model.RoomConfig
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeIcons

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
                highlighted = true,
                onOpenRoom = onOpenRoom,
                onToggleLight = onToggleLight,
                onAdjustTarget = onAdjustTarget,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            RoomPager(
                rooms = otherRooms,
                entities = entities,
                highlighted = false,
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

@Composable
private fun SmallTileCard(tile: SmallTile, onTileClick: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .clickable(enabled = tile.entityId != null) { tile.entityId?.let(onTileClick) }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).clip(CircleShape).background(HumeColors.Background),
            contentAlignment = Alignment.Center,
        ) {
            Icon(tile.icon, contentDescription = null, tint = HumeColors.TextSecondary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(tile.value, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = HumeColors.TextPrimary, maxLines = 1)
            Text(tile.label, fontSize = 13.sp, color = HumeColors.TextSecondary, maxLines = 1)
        }
    }
}

@Composable
private fun RoomPager(
    rooms: List<RoomConfig>,
    entities: Map<String, HomeEntity>,
    highlighted: Boolean,
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
                highlighted = highlighted,
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

@Composable
private fun RoomCardLarge(
    room: RoomConfig,
    entities: Map<String, HomeEntity>,
    highlighted: Boolean,
    onOpen: () -> Unit,
    onToggleLight: () -> Unit,
    onAdjustTarget: (Double) -> Unit,
) {
    val lightOn = entities[room.lightEntity]?.isOn == true
    val useAccent = highlighted && lightOn
    val background: Brush = if (useAccent) {
        Brush.verticalGradient(listOf(HumeColors.RoomOnStart, HumeColors.RoomOnEnd))
    } else {
        Brush.verticalGradient(listOf(Color.White, Color.White))
    }
    val temp = entities.num(room.tempEntity, 0)
    val humidity = entities.num(room.humidityEntity, 0)
    val target = entities.attr(room.climateEntity ?: "", "temperature")

    Box(
        Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(background)
            .clickable(onClick = onOpen)
            .padding(18.dp)
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    room.name,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = HumeColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (useAccent) Color.White.copy(alpha = 0.35f) else HumeColors.Background)
                        .clickable(onClick = onToggleLight),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        HumeIcons.room(room.icon),
                        contentDescription = null,
                        tint = if (lightOn) HumeColors.OrangeDeep else HumeColors.TextSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                    Text(
                        temp,
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Normal,
                        color = HumeColors.TextPrimary,
                    )
                    Text("\u00b0", fontSize = 34.sp, color = HumeColors.TextPrimary)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "$humidity%",
                        fontSize = 16.sp,
                        color = HumeColors.TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                if (room.hasClimate && room.climateEntity != null) {
                    TargetStepper(target = target, accent = useAccent, onAdjustTarget = onAdjustTarget)
                }
            }
        }
    }
}

@Composable
private fun TargetStepper(target: String?, accent: Boolean, onAdjustTarget: (Double) -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (accent) Color.White.copy(alpha = 0.35f) else HumeColors.Background)
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
            fontSize = 17.sp,
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
