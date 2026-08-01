package com.smarthome.hume.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * One UI 8.5 surface language, in one place.
 *
 * Samsung's 8.5 look is translucent multi layered glass: a light frosted panel,
 * a bright hairline on the edge, soft depth, and heavy corner rounding. True
 * backdrop blur needs a blur library, so the panels here approximate it with a
 * translucent gradient fill plus the highlight edge. Swapping in real backdrop
 * blur later only means changing this file.
 */
object HumeSurfaces {
    /** Frosted panel fill, brighter at the top like a lit pane of glass. */
    val glassFill = Brush.verticalGradient(
        listOf(Color.White.copy(alpha = 0.92f), Color.White.copy(alpha = 0.74f)),
    )

    /** Same panel for elements sitting on top of coloured content. */
    val glassFillStrong = Brush.verticalGradient(
        listOf(Color.White.copy(alpha = 0.98f), Color.White.copy(alpha = 0.88f)),
    )

    /** The bright hairline One UI draws along the edge of every glass panel. */
    val glassEdge = Color.White.copy(alpha = 0.65f)

    /** Very soft ambient shadow. One UI keeps depth subtle. */
    val shadow = Color(0x14000000)
}

/** Corner radii used across the app, matching the SwiftUI card sizes. */
object HumeShapes {
    val Tile: Dp = 24.dp
    val Card: Dp = 30.dp
    val Panel: Dp = 37.dp
    val Sheet: Dp = 44.dp
    val Pill: Dp = 30.dp
}

/** Spacing scale. Keep every gap on this scale. */
object HumeSpacing {
    val Hairline: Dp = 4.dp
    val Tight: Dp = 8.dp
    val Small: Dp = 10.dp
    val Medium: Dp = 14.dp
    val Large: Dp = 18.dp
    val Section: Dp = 24.dp
}

/**
 * The standard glass panel. Use this instead of hand rolling
 * `background(Color.White)` so the whole app changes together.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    radius: Dp = HumeShapes.Card,
    padding: PaddingValues = PaddingValues(16.dp),
    strong: Boolean = false,
    elevated: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Column(
        modifier
            .then(if (elevated) Modifier.shadow(6.dp, shape, ambientColor = HumeSurfaces.shadow, spotColor = HumeSurfaces.shadow) else Modifier)
            .clip(shape)
            .background(if (strong) HumeSurfaces.glassFillStrong else HumeSurfaces.glassFill)
            .border(1.dp, HumeSurfaces.glassEdge, shape)
            .padding(padding),
        content = content,
    )
}

/**
 * Tinted glass, for cards that carry an accent such as an active room or an
 * alert. The tint sits under the frost so it reads as coloured glass.
 */
@Composable
fun TintedGlassCard(
    tint: Color,
    modifier: Modifier = Modifier,
    radius: Dp = HumeShapes.Card,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Column(
        modifier
            .shadow(6.dp, shape, ambientColor = HumeSurfaces.shadow, spotColor = HumeSurfaces.shadow)
            .clip(shape)
            .background(tint.copy(alpha = 0.16f))
            .background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.28f)),
                ),
            )
            .border(1.dp, tint.copy(alpha = 0.35f), shape)
            .padding(padding),
        content = content,
    )
}

/**
 * Floating pill used by the bottom navigation and by in app toolbars. One UI
 * 8.5 lifts these off the bottom edge instead of docking them.
 */
fun Modifier.glassPill(radius: Dp = HumeShapes.Pill): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .shadow(10.dp, shape, ambientColor = HumeSurfaces.shadow, spotColor = HumeSurfaces.shadow)
        .clip(shape)
        .background(HumeSurfaces.glassFillStrong)
        .border(1.dp, HumeSurfaces.glassEdge, shape)
}
