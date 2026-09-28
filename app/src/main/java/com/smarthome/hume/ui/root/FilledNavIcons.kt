package com.smarthome.hume.ui.root

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill

/**
 * Icon navbar fill dac (Material filled paths, 24x24 grid).
 * Font subset hien tai chi co outlined (FILL=0), khong tai duoc font filled
 * trong sandbox -> ve truc tiep bang Canvas Path.
 */
@Composable
fun FilledNavIcon(
    tab: com.smarthome.hume.core.model.HumeTab,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val s = size.minDimension / 24f
        val p = Path()
        when (tab) {
            com.smarthome.hume.core.model.HumeTab.Home -> {
                // material home filled
                p.moveTo(12f * s, 3f * s)
                p.lineTo(21f * s, 11f * s)
                p.lineTo(18f * s, 11f * s)
                p.lineTo(18f * s, 20f * s)
                p.lineTo(14f * s, 20f * s)
                p.lineTo(14f * s, 14f * s)
                p.lineTo(10f * s, 14f * s)
                p.lineTo(10f * s, 20f * s)
                p.lineTo(6f * s, 20f * s)
                p.lineTo(6f * s, 11f * s)
                p.lineTo(3f * s, 11f * s)
                p.close()
            }
            com.smarthome.hume.core.model.HumeTab.Energy -> {
                // material bolt filled
                p.moveTo(13f * s, 2f * s)
                p.lineTo(4f * s, 14f * s)
                p.lineTo(11f * s, 14f * s)
                p.lineTo(11f * s, 22f * s)
                p.lineTo(20f * s, 10f * s)
                p.lineTo(13f * s, 10f * s)
                p.close()
            }
            com.smarthome.hume.core.model.HumeTab.Security -> {
                // material shield filled
                p.moveTo(12f * s, 2f * s)
                p.lineTo(20f * s, 5f * s)
                p.lineTo(20f * s, 11f * s)
                p.cubicTo(
                    20f * s, 16f * s, 16.6f * s, 20.4f * s, 12f * s, 22f * s,
                )
                p.cubicTo(7.4f * s, 20.4f * s, 4f * s, 16f * s, 4f * s, 11f * s)
                p.lineTo(4f * s, 5f * s)
                p.close()
            }
            com.smarthome.hume.core.model.HumeTab.Profile -> {
                // material person filled: head circle + body
                p.addOval(
                    androidx.compose.ui.geometry.Rect(
                        8f * s, 4f * s, 16f * s, 12f * s,
                    ),
                )
                p.moveTo(12f * s, 14f * s)
                p.cubicTo(7.6f * s, 14f * s, 4f * s, 15.3f * s, 4f * s, 18f * s)
                p.lineTo(4f * s, 20f * s)
                p.lineTo(20f * s, 20f * s)
                p.lineTo(20f * s, 18f * s)
                p.cubicTo(20f * s, 15.3f * s, 16.4f * s, 14f * s, 12f * s, 14f * s)
                p.close()
            }
        }
        drawPath(p, color = tint, style = Fill)
    }
}
