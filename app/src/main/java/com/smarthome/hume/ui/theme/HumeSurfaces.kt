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
 * Glass surfaces, ported from Theme/HumeTheme.swift.
 *
 * HumeCardModifier fills a card with .regularMaterial and strokes it with
 * white at 14% opacity; HumeElementModifier falls back to
 * Color(.tertiarySystemFill). UIKit materials resolve differently per
 * appearance, so every value here is read through a getter that asks
 * HumeColors.isDark instead of being frozen at class-load time.
 */
object HumeSurfaces {
    /** .regularMaterial: a frosted pane, brighter at the top. */
    val glassFill: Brush
        get() = if (HumeColors.isDark) {
            Brush.verticalGradient(
                listOf(Color(0xFF161616).copy(alpha = 0.92f), Color(0xFF161616).copy(alpha = 0.78f)),
            )
        } else {
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.92f), Color.White.copy(alpha = 0.74f)),
            )
        }

    /** Same pane, more opaque, for elements over coloured content. */
    val glassFillStrong: Brush
        get() = if (HumeColors.isDark) {
            Brush.verticalGradient(
                listOf(Color(0xFF1C1C1C), Color(0xFF161616)),
            )
        } else {
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.98f), Color.White.copy(alpha = 0.88f)),
            )
        }

    /** Specular edge. Swift strokes .white.opacity(0.14) on the glass. */
    val glassEdge: Color
        get() = if (HumeColors.isDark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.65f)

    /** Ambient shadow. Barely visible in dark mode, as on iOS. */
    val shadow: Color
        get() = if (HumeColors.isDark) Color(0x33000000) else Color(0x14000000)

    /** Overlay that lifts a tinted panel, the frost sitting over the tint. */
    val tintFrost: Brush
        get() = if (HumeColors.isDark) {
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.02f)),
            )
        } else {
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.28f)),
            )
        }
}

/**
 * Corner radii, taken from HumeTheme.swift rather than invented:
 * Radius.card 34, Radius.element 12, Radius.sheet 28, popups 35, scene tiles 25,
 * and the group containers 37 (GroupGlassContainer default).
 */
object HumeShapes {
    val Element: Dp = 12.dp
    val Tile: Dp = 25.dp
    val Sheet: Dp = 28.dp
    val Card: Dp = 34.dp
    val Popup: Dp = 35.dp
    val Panel: Dp = 37.dp
    val Pill: Dp = 30.dp
}

/** Spacing scale, from HumeTheme.Spacing in Swift. */
object HumeSpacing {
    val Hairline: Dp = 4.dp
    val Tight: Dp = 8.dp
    val Small: Dp = 10.dp
    val Medium: Dp = 14.dp
    val Large: Dp = 18.dp
    val Section: Dp = 24.dp
}

/**
 * Glass panel as a modifier, for rows, boxes and anything that is not a column.
 * Prefer this over hand rolling `background(Color.White)`.
 */
fun Modifier.glassSurface(
    radius: Dp = HumeShapes.Card,
    elevation: Dp = 4.dp,
    strong: Boolean = false,
): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .then(
            if (elevation > 0.dp) {
                Modifier.shadow(elevation, shape, ambientColor = HumeSurfaces.shadow, spotColor = HumeSurfaces.shadow)
            } else {
                Modifier
            },
        )
        .clip(shape)
        .background(if (strong) HumeSurfaces.glassFillStrong else HumeSurfaces.glassFill)
        .border(1.dp, HumeSurfaces.glassEdge, shape)
}

/** Accented glass, for active or alerting elements. */
fun Modifier.tintedGlass(
    tint: Color,
    radius: Dp = HumeShapes.Card,
    elevation: Dp = 4.dp,
): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .then(
            if (elevation > 0.dp) {
                Modifier.shadow(elevation, shape, ambientColor = HumeSurfaces.shadow, spotColor = HumeSurfaces.shadow)
            } else {
                Modifier
            },
        )
        .clip(shape)
        .background(tint.copy(alpha = if (HumeColors.isDark) 0.24f else 0.16f))
        .background(HumeSurfaces.tintFrost)
        .border(1.dp, tint.copy(alpha = 0.35f), shape)
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
    Column(
        modifier
            .glassSurface(radius = radius, elevation = if (elevated) 6.dp else 0.dp, strong = strong)
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
    Column(
        modifier.tintedGlass(tint = tint, radius = radius, elevation = 6.dp).padding(padding),
        content = content,
    )
}

/** Capsule surface, the humeCapsule modifier in Swift. */
fun Modifier.glassPill(radius: Dp = HumeShapes.Pill): Modifier =
    glassSurface(radius = radius, elevation = 10.dp, strong = true)
