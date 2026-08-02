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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.core.model.RoomConfig
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeIcons

/*
 * Layout theo ban HTML cocopi-home:
 *  - the phong: height 240, radius 30, padding 16, nen gradient #f9784c -> #fac0b6 khi den bat
 *  - tile cam bien: height 65, radius 30, vong tron 58 (gray00), icon 30
 *  - dot pager: active 20x6 mau cam, inactive 6x6
 */
private val CardHeight = 240.dp
private val CardRadius = 30.dp
private val TileHeight = 65.dp
private val TileRadius = 30.dp
private val GridGap = 12.dp
private val TileGap = 10.dp

private val ActiveGradient
    get() = Brush.linearGradient(
        colors = listOf(Color(0xFFF9784C), Color(0xFFFAC0B6)),
        start = Offset(0f, 0f),
        end = Offset(520f, 700f),
    )

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
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(GridGap),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(GridGap)) {
            SensorPager(leftTiles, onTileClick)
            RoomPager(climateRooms, entities, onOpenRoom, onToggleLight, onAdjustTarget)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(GridGap)) {
            RoomPager(otherRooms, entities, onOpenRoom, onToggleLight, onAdjustTarget)
            rightTiles.forEach { tile -> TileCard(tile, onTileClick) }
        }
    }
}

@Composable
private fun SensorPager(tiles: List<SmallTile>, onTileClick: (String) -> Unit) {
    if (tiles.isEmpty()) return
    val pages = tiles.chunked(2)
    val pagerState = rememberPagerState(pageCount = { pages.size })
    Column {
        HorizontalPager(state = pagerState, modifier = Modifier.height(TileHeight * 2 + TileGap)) { page ->
            Column(verticalArrangement = Arrangement.spacedBy(TileGap)) {
                pages[page].forEach { tile -> TileCard(tile, onTileClick) }
            }
        }
        if (pages.size > 1) {
            Spacer(Modifier.height(4.dp))
            PagerDots(pages.size, pagerState.currentPage)
        }
    }
}

/** Tile 65dp: vong tron icon 58 ben trai, gia tri o tren, ten o duoi. */
@Composable
private fun TileCard(tile: SmallTile, onTileClick: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(TileHeight)
            .clip(RoundedCornerShape(TileRadius))
            .background(HumeColors.Card)
            .clickable(enabled = tile.entityId != null) { tile.entityId?.let(onTileClick) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(4.dp))
        Box(
            Modifier.size(58.dp).clip(CircleShape).background(HumeColors.Gray00),
            contentAlignment = Alignment.Center,
        ) {
            Icon(tile.icon, contentDescription = null, tint = HumeColors.Gray1000, modifier = Modifier.size(28.dp))
        }
        Column(Modifier.weight(1f).padding(start = 10.dp, end = 12.dp)) {
            Text(
                tile.value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = HumeColors.Gray1000,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                tile.label,
                fontSize = 13.sp,
                color = HumeColors.Gray1000.copy(alpha = 0.7f),
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
            Spacer(Modifier.height(4.dp))
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
    val fg = if (lightOn) Color(0xFF000000) else HumeColors.Gray1000
    val chipBg = if (lightOn) Color.White.copy(alpha = 0.2f) else HumeColors.Gray00

    Box(
        Modifier
            .fillMaxWidth()
            .height(CardHeight)
            .clip(shape)
            .then(if (lightOn) Modifier.background(ActiveGradient) else Modifier.background(HumeColors.Card))
            .clickable(onClick = onOpen)
            .padding(16.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    room.name,
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = fg,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 6.dp),
                )
                Box(contentAlignment = Alignment.TopEnd) {
                    Box(
                        Modifier.size(55.dp).clip(CircleShape).background(chipBg).clickable(onClick = onToggleLight),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(HumeIcons.room(room.icon), contentDescription = null, tint = fg, modifier = Modifier.size(28.dp))
                    }
                    if (contactOpen) {
                        Box(
                            Modifier.offset(x = 0.dp, y = 6.dp).size(8.dp).clip(CircleShape).background(Color(0xFFFF5252))
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = if (hasStepper) Alignment.CenterVertically else Alignment.Bottom,
            ) {
                Row(
                    Modifier.weight(1f).padding(end = 6.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        "$temp\u00b0",
                        fontSize = if (hasStepper) 40.sp else 46.sp,
                        lineHeight = if (hasStepper) 42.sp else 48.sp,
                        fontWeight = FontWeight.Light,
                        color = fg,
                        maxLines = 1,
                        softWrap = false,
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        "$humidity%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = fg.copy(alpha = 0.6f),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                if (hasStepper) {
                    TargetStepper(target = target, background = chipBg, foreground = fg, onAdjustTarget = onAdjustTarget)
                }
            }
            if (hasStepper) Spacer(Modifier.height(4.dp))
        }
    }
}

/** Stepper 44dp: nut tang 26, so 34, nut giam 26 - dung nhu ban HTML. */
@Composable
private fun TargetStepper(
    target: String?,
    background: Color,
    foreground: Color,
    onAdjustTarget: (Double) -> Unit,
) {
    Column(
        Modifier.width(44.dp).clip(RoundedCornerShape(18.dp)).background(background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().height(26.dp).clickable { onAdjustTarget(1.0) }, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "T\u0103ng", tint = foreground, modifier = Modifier.size(20.dp))
        }
        Box(Modifier.fillMaxWidth().height(34.dp), contentAlignment = Alignment.Center) {
            Text(
                if (target == null) "--" else "$target\u00b0",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = foreground,
                maxLines = 1,
                softWrap = false,
            )
        }
        Box(Modifier.fillMaxWidth().height(26.dp).clickable { onAdjustTarget(-1.0) }, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Gi\u1ea3m", tint = foreground, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun PagerDots(count: Int, current: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    ) {
        Spacer(Modifier.weight(1f))
        repeat(count) { index ->
            val active = index == current
            Box(
                Modifier
                    .height(6.dp)
                    .width(if (active) 20.dp else 6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (active) HumeColors.Orange else HumeColors.Gray100)
            )
        }
        Spacer(Modifier.weight(1f))
    }
}
