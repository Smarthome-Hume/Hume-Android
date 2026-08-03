package com.smarthome.hume.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.core.model.RoomConfig
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeIcons
import com.smarthome.hume.ui.theme.humeMarquee
import kotlin.math.absoluteValue

/*
 * Luoi 2 cot doi xung. Moi cot deu gom DUNG hai khoi + hai hang dots.
 *
 * DA BO HAN HIEU UNG NEON NHAP NHAY tren the phong: khong con vien nhap nhay,
 * khong con quang sang do bong, khong con vong sang quanh nut icon. The phong
 * dang bat den chi doi sang nen gradient cam nhu ban goc.
 * Cua/cua so dang mo van co mot cham do TINH o goc nut icon de canh bao.
 *
 * CHAY CHU: gia tri/nhan cua tile va ten phong dung humeMarquee() - port tu
 * `animation: marquee 8s linear infinite` cua ban HTML, chi chay khi chu tran.
 */
private val GridGap = 12.dp
private val TileGap = 10.dp
private val DotsRow = 14.dp
private val CardRadius = 30.dp
private val TileRadius = 30.dp
private const val CardAspect = 1.30f
private const val TileBlockRatio = 0.583f
private val NeonRed = Color(0xFFFF5252)

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
private fun Modifier.pressScale(interaction: MutableInteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, tween(140), label = "press")
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}

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
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columnWidth = (maxWidth - GridGap) / 2
        val cardHeight = columnWidth * CardAspect
        val tileBlock = cardHeight * TileBlockRatio
        val tileHeight = (tileBlock - TileGap) / 2

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(GridGap),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f)) {
                SensorPager(leftTiles, tileBlock, tileHeight, onTileClick)
                RoomPager(climateRooms, entities, columnWidth, cardHeight, onOpenRoom, onToggleLight, onAdjustTarget)
            }
            Column(Modifier.weight(1f)) {
                RoomPager(otherRooms, entities, columnWidth, cardHeight, onOpenRoom, onToggleLight, onAdjustTarget)
                SensorPager(rightTiles, tileBlock, tileHeight, onTileClick)
            }
        }
    }
}

@Composable
private fun SensorPager(
    tiles: List<SmallTile>,
    blockHeight: Dp,
    tileHeight: Dp,
    onTileClick: (String) -> Unit,
) {
    val pages = tiles.chunked(2)
    val pagerState = rememberPagerState(pageCount = { maxOf(pages.size, 1) })
    Column {
        Box(Modifier.height(blockHeight)) {
            if (pages.isNotEmpty()) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    Column(
                        Modifier.pageTransition(pagerState.currentPage, pagerState.currentPageOffsetFraction, page),
                        verticalArrangement = Arrangement.spacedBy(TileGap),
                    ) {
                        pages[page].forEach { tile -> TileCard(tile, tileHeight, onTileClick) }
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(DotsRow), contentAlignment = Alignment.Center) {
            if (pages.size > 1) PagerDots(pages.size, pagerState.currentPage)
        }
    }
}

/** Chuyen trang: scale + mo dan giong pager ban goc. */
private fun Modifier.pageTransition(currentPage: Int, offset: Float, page: Int): Modifier =
    this.graphicsLayer {
        val distance = ((currentPage - page) + offset).absoluteValue.coerceAtMost(1f)
        val s = lerp(0.94f, 1f, 1f - distance)
        scaleX = s
        scaleY = s
        alpha = lerp(0.55f, 1f, 1f - distance)
    }

@Composable
private fun TileCard(tile: SmallTile, tileHeight: Dp, onTileClick: (String) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val circle = tileHeight - 10.dp
    Row(
        Modifier
            .fillMaxWidth()
            .height(tileHeight)
            .pressScale(interaction)
            .clip(RoundedCornerShape(TileRadius))
            .background(HumeColors.Card)
            .clickable(interactionSource = interaction, indication = null, enabled = tile.entityId != null) {
                tile.entityId?.let(onTileClick)
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(5.dp))
        Box(
            Modifier.size(circle).clip(CircleShape).background(HumeColors.Gray00),
            contentAlignment = Alignment.Center,
        ) {
            Icon(tile.icon, contentDescription = null, tint = HumeColors.Gray1000, modifier = Modifier.size(circle * 0.44f))
        }
        Column(Modifier.weight(1f).padding(start = 7.dp, end = 8.dp)) {
            Text(
                tile.value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = HumeColors.Gray1000,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = Modifier.humeMarquee(),
            )
            Text(
                tile.label,
                fontSize = 11.sp,
                color = HumeColors.Gray1000.copy(alpha = 0.7f),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = Modifier.humeMarquee(),
            )
        }
    }
}

@Composable
private fun RoomPager(
    rooms: List<RoomConfig>,
    entities: Map<String, HomeEntity>,
    columnWidth: Dp,
    cardHeight: Dp,
    onOpenRoom: (RoomConfig) -> Unit,
    onToggleLight: (RoomConfig) -> Unit,
    onAdjustTarget: (RoomConfig, Double) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { maxOf(rooms.size, 1) })
    Column {
        Box(Modifier.height(cardHeight)) {
            if (rooms.isNotEmpty()) {
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    Box(Modifier.pageTransition(pagerState.currentPage, pagerState.currentPageOffsetFraction, page)) {
                        RoomCardLarge(
                            room = rooms[page],
                            entities = entities,
                            columnWidth = columnWidth,
                            cardHeight = cardHeight,
                            onOpen = { onOpenRoom(rooms[page]) },
                            onToggleLight = { onToggleLight(rooms[page]) },
                            onAdjustTarget = { delta -> onAdjustTarget(rooms[page], delta) },
                        )
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(DotsRow), contentAlignment = Alignment.Center) {
            if (rooms.size > 1) PagerDots(rooms.size, pagerState.currentPage)
        }
    }
}

@Composable
private fun RoomCardLarge(
    room: RoomConfig,
    entities: Map<String, HomeEntity>,
    columnWidth: Dp,
    cardHeight: Dp,
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
    val chipBg = if (lightOn) Color.White.copy(alpha = 0.22f) else HumeColors.Gray00
    val interaction = remember { MutableInteractionSource() }
    val iconCircle = (columnWidth * 0.27f).coerceIn(44.dp, 54.dp)
    val tempSize = (columnWidth.value * (if (hasStepper) 0.20f else 0.24f)).sp

    Box(
        Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .pressScale(interaction)
            .clip(shape)
            .then(if (lightOn) Modifier.background(ActiveGradient) else Modifier.background(HumeColors.Card))
            .clickable(interactionSource = interaction, indication = null, onClick = onOpen)
            .padding(14.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    room.name,
                    fontSize = 17.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = fg,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.weight(1f).padding(end = 6.dp).humeMarquee(),
                )
                Box(contentAlignment = Alignment.TopEnd) {
                    Box(
                        Modifier
                            .size(iconCircle)
                            .clip(CircleShape)
                            .background(chipBg)
                            .clickable(onClick = onToggleLight),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(HumeIcons.room(room.icon), contentDescription = null, tint = fg, modifier = Modifier.size(iconCircle * 0.48f))
                    }
                    if (contactOpen) AlertDot(Modifier.offset(x = 4.dp, y = (-4).dp))
                }
            }

            Spacer(Modifier.weight(1f))

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Row(
                    Modifier.weight(1f).padding(end = 4.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        "$temp\u00b0",
                        fontSize = tempSize,
                        lineHeight = tempSize * 1.02f,
                        fontWeight = FontWeight.Light,
                        color = fg,
                        maxLines = 1,
                        softWrap = false,
                    )
                    Text(
                        "$humidity%",
                        fontSize = 13.sp,
                        color = fg.copy(alpha = 0.6f),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.padding(start = 5.dp, bottom = 5.dp),
                    )
                }
                if (hasStepper) {
                    TargetStepper(target = target, background = chipBg, foreground = fg, onAdjustTarget = onAdjustTarget)
                }
            }
        }
    }
}

/** Cham do TINH bao cua/cua so dang mo - khong nhap nhay. */
@Composable
private fun AlertDot(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(NeonRed)
            .border(2.dp, HumeColors.Card, CircleShape)
    )
}

@Composable
private fun TargetStepper(
    target: String?,
    background: Color,
    foreground: Color,
    onAdjustTarget: (Double) -> Unit,
) {
    Column(
        Modifier.width(40.dp).clip(RoundedCornerShape(18.dp)).background(background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().height(22.dp).clickable { onAdjustTarget(1.0) }, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "T\u0103ng", tint = foreground, modifier = Modifier.size(17.dp))
        }
        Box(Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.Center) {
            Text(
                if (target == null) "--" else "$target\u00b0",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = foreground,
                maxLines = 1,
                softWrap = false,
            )
        }
        Box(Modifier.fillMaxWidth().height(22.dp).clickable { onAdjustTarget(-1.0) }, contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Gi\u1ea3m", tint = foreground, modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
fun PagerDots(count: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(count) { index ->
            val active = index == current
            val width by animateFloatAsState(if (active) 18f else 6f, tween(260), label = "dot")
            Box(
                Modifier
                    .height(6.dp)
                    .width(width.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (active) HumeColors.Orange else HumeColors.Gray100)
            )
        }
    }
}
