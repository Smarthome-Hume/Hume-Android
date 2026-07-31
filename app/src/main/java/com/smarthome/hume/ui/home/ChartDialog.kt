package com.smarthome.hume.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.smarthome.hume.core.ha.HistoryPoint
import com.smarthome.hume.core.model.HomeEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Chart popup from HomeView.swift, drawn with Compose Canvas.
 * History loading is injected by HomeViewModel, so this stays UI-only.
 */
@Composable
fun ChartDialog(
    entityId: String,
    entities: Map<String, HomeEntity>,
    loadHistory: suspend (String) -> List<HistoryPoint>,
    onDismiss: () -> Unit,
) {
    var points by remember(entityId) { mutableStateOf<List<HistoryPoint>?>(null) }
    LaunchedEffect(entityId) { points = loadHistory(entityId) }

    val entity = entities[entityId]
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Đóng") } },
        title = {
            Column {
                Text(entity?.friendly() ?: entityId, style = MaterialTheme.typography.titleMedium)
                Text("24 giờ qua", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            val data = points
            when {
                data == null -> Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                data.size < 2 -> Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    Text("Không có dữ liệu lịch sử cho entity này.", style = MaterialTheme.typography.bodySmall)
                }
                else -> {
                    val min = data.minOf { it.value }
                    val max = data.maxOf { it.value }
                    val unit = entity?.unit().orEmpty()
                    Column {
                        Canvas(Modifier.fillMaxWidth().height(170.dp)) {
                            val span = (max - min).takeIf { it > 0.0001 } ?: 1.0
                            val stepX = size.width / (data.size - 1).toFloat()
                            val path = Path()
                            data.forEachIndexed { index, point ->
                                val x = stepX * index
                                val y = size.height - ((point.value - min) / span).toFloat() * size.height
                                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawLine(gridColor, Offset(0f, size.height), Offset(size.width, size.height), 2f)
                            drawLine(gridColor, Offset(0f, 0f), Offset(size.width, 0f), 2f)
                            drawPath(path, lineColor, style = Stroke(width = 5f))
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Thấp " + format(min) + " " + unit, style = MaterialTheme.typography.bodySmall)
                            Text("Cao " + format(max) + " " + unit, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(timeLabel(data.first().timeMs), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(timeLabel(data.last().timeMs), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
    )
}

private fun format(value: Double): String = String.format(Locale.US, "%.1f", value)

private fun timeLabel(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.US).format(Date(millis))
