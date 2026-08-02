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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.core.model.RoomConfig
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeIcons
import com.smarthome.hume.ui.theme.glassSurface

private val CardRadius = 34.dp
private val CardHeight = 236.dp
private val GridGap = 12.dp
private val SensorPageHeight = 140.dp
private val StepperWidth = 46.dp

data class SmallTile(
    val icon: ImageVector,
    val value: String,
    val label: String,
    val entityId: String? = null,
)

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
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(GridGap), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(GridGap)) {
            SensorPager(leftTiles, onTileClick)
            RoomPager(climateRooms, entities, onOpenRoom, onToggleLight, onAdjustTarget)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(GridGap)) {
            RoomPager(otherRooms, entities, onOpenRoom, onToggleLight, onAdjustTarget)
            rightTiles.forEach { tile -> WideTileCard(tile, onTileClick) }
        }
    }
}

@Composable
private fun SensorPager(tiles: List<SmallTile>, onTileClick: (String) -> Unit) {
    if (tiles.isEmpty()) return
    val pages = tiles.chunked(2)
    val pagerState = rememberPagerState(pageCount = { pages.size })
    Column {
        HorizontalPager(state = pagerState, modifier = Modifier.height(SensorPageHeight)) { page ->
            Column(verticalArrangement = Arrangement.spacedBy(GridGap)) {
                pages[page].forEach { tile -> SensorTileCard(tile, onTileClick) }
            }
        }
        if (pages.size > 1) {
            Spacer(Modifier.height(6.dp))
            PagerDots(pages.size, pagerState.currentPage)
        }
    }
}

@Composable
private fun SensorTileCard(tile: SmallTile, onTileClick: (String) -> Unit) {
    TileCard(tile = tile, cardHeight = 64.dp, circleSize = 56.dp, iconSize = 24.dp, onTileClick = onTileClick)
}

@Composable
private fun WideTileCard(tile: SmallTile, onTileClick: (String) -> Unit) {
    TileCard(tile = tile, cardHeight = 66.dp, circleSize = 58.dp, iconSize = 24.dp, trailingPadding = 12.dp, onTileClick = onTileClick)
}

@Composable
private fun TileCard(
    tile: SmallTile,
    cardHeight: Dp,
    circleSize: Dp,
    iconSize: Dp,
    trailingPadding: Dp = 10.dp,
    onTileClick: (String) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .glassSurface(radius = cardHeight / 2)
            .clickable(enabled = tile.entityId != null) { tile.entityId?.let(onTileClick) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(cardHeight), contentAlignment = Alignment.Center) {
            Box(Modifier.size(circleSize).clip(CircleShape).background(HumeColors.FillTertiary), contentAlignment = Alignment.Center) {
                Icon(tile.icon, contentDescription = null, tint = HumeColors.TextPrimary, modifier = Modifier.size(iconSize))
            }
        }
        Column(Modifier.weight(1f).padding(start = 8.dp, end = trailingPadding)) {
            Text(
                tile.value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = HumeColors.TextPrimary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                tile.label,
                fontSize = 13.sp,
                color = HumeColors.TextPrimary.copy(alpha = 0.7f),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
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
        HorizontalPager(state = pagerState, modifier = Modifier.height(CardHeight)) { page ->
            RoomCardLarge(
                room = rooms[page],
                entities = entities,
                onOpen = { onOpenRoom(rooms[page]) },
                onToggleLight = { onToggleLight(rooms[page]) },
                onAdjustTarget = { delta -> onAdjustTarget(rooms[page], delta) },
            )
        }
        if (rooms.size > 1) {
            Spacer(Modifier.height(6.dp))
            PagerDots(rooms.size, pagerState.currentPage)
        }
    }
}

@Composable
private fun RoomCardLarge(
    room: RoomConfig,
    entities: Map<String, HomeEntity>,
    onOpen: () -> Unit,
    onToggleLight: () -> Unit,
    onAdjustTarget: (Double) -> Unit,
) {
    val lightOn = entities[room.lightEntity]?.isOn == true
    val contactOpen = room.contactEntity?.let { entities[it]?.isOn == true } == true
    val temp = entities.num(room.tempEntity, 0)
    val humidity = entities.num(room.humidityEntity, 0)
    val target = entities.attr(room.climateEntity ?: "", "temperature")
    val hasStepper = room.hasClimate && room.climateEntity != null
    val shape = RoundedCornerShape(CardRadius)

    Box(
        Modifier
            .fillMaxWidth()
            .height(CardHeight)
            .shadow(
                elevation = if (lightOn) 10.dp else 4.dp,
                shape = shape,
                ambientColor = if (lightOn) HumeColors.Orange.copy(alpha = 0.27f) else Color(0x14000000),
                spotColor = if (lightOn) HumeColors.Orange.copy(alpha = 0.27f) else Color(0x14000000),
            )
            .glassSurface(radius = CardRadius, elevation = 0.dp)
            .then(if (lightOn) Modifier.background(HumeColors.Orange.copy(alpha = 0.10f), shape) else Modifier)
            .border(1.dp, if (lightOn) HumeColors.Orange.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.08f), shape)
            .clickable(onClick = onOpen)
            .padding(14.dp),
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    room.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HumeColors.TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 6.dp),
                )
                Box(contentAlignment = Alignment.TopEnd) {
                    Box(
                        Modifier.size(52.dp).clip(CircleShape).background(HumeColors.FillTertiary).clickable(onClick = onToggleLight),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(HumeIcons.room(room.icon), contentDescription = null, tint = if (lightOn) HumeColors.Orange else HumeColors.TextPrimary, modifier = Modifier.size(26.dp))
                    }
                    if (contactOpen) {
                        Box(Modifier.offset(x = 2.dp, y = 4.dp).size(8.dp).clip(CircleShape).background(HumeColors.Orange))
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Row(
                    Modifier.weight(1f).padding(end = 4.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        "$temp\u00b0",
                        fontSize = if (hasStepper) 38.sp else 44.sp,
                        lineHeight = if (hasStepper) 42.sp else 48.sp,
                        fontWeight = FontWeight.ExtraLight,
                        color = HumeColors.TextPrimary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                    )
                    Text(
                        "$humidity%",
                        fontSize = if (hasStepper) 13.sp else 15.sp,
                        fontWeight = FontWeight.Light,
                        color = HumeColors.TextPrimary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.padding(bottom = 7.dp),
                    )
                }
                if (hasStepper) {
                    TargetStepper(target = target, onAdjustTarget = onAdjustTarget)
                }
            }
        }
    }
}

@Composable
private fun TargetStepper(target: String?, onAdjustTarget: (Double) -> Unit) {
    Column(
        Modifier.width(StepperWidth).clip(RoundedCornerShape(18.dp)).background(HumeColors.FillTertiary),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().height(28.dp).clickable { onAdjustTarget(1.0) }, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "T\u0103ng", tint = HumeColors.TextPrimary, modifier = Modifier.size(18.dp))
        }
        Box(Modifier.fillMaxWidth().height(36.dp), contentAlignment = Alignment.Center) {
            Text(
                if (target == null) "--" else "$target\u00b0",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = HumeColors.TextPrimary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
            )
        }
        Box(Modifier.fillMaxWidth().height(28.dp).clickable { onAdjustTarget(-1.0) }, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Gi\u1ea3m", tint = HumeColors.TextPrimary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun PagerDots(count: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.weight(1f))
        repeat(count) { index ->
            val active = index == current
            Box(
                Modifier.height(6.dp).width(if (active) 16.dp else 6.dp).clip(RoundedCornerShape(3.dp)).background(if (active) HumeColors.TextPrimary else HumeColors.TextSecondary.copy(alpha = 0.4f))
            )
        }
        Spacer(Modifier.weight(1f))
    }
}
