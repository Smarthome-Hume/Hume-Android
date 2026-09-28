package com.smarthome.hume.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Bieu do cot pill/capsule phong cach M3E (demo rev4):
 * ngay cu = primaryContainer, hom nay = primary dac.
 */
@Composable
fun PillBarChart(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 120.dp,
    barWidth: Dp = 30.dp,
    todayIndex: Int = values.size - 1,
    valueLabel: (Float) -> String = { "" },
) {
    val max = (values.maxOrNull() ?: 1f).coerceAtLeast(0.001f)
    val pill = RoundedCornerShape(50)
    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            values.forEachIndexed { i, v ->
                val h = maxHeight * (v / max).coerceAtLeast(0.04f)
                val color = if (i == todayIndex) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.primaryContainer
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.height(maxHeight + 20.dp),
                ) {
                    val lbl = valueLabel(v)
                    if (lbl.isNotEmpty()) {
                        Text(lbl, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .width(barWidth)
                            .height(h)
                            .clip(pill)
                            .background(color),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            labels.forEach { l ->
                Text(
                    l,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(barWidth),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}
