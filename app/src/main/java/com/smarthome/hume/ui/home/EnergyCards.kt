package com.smarthome.hume.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ElectricMeter
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.ui.theme.HumeColors
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.util.Locale

/* ---------- entity attribute helpers, shared inside ui.home ---------- */

internal fun HomeEntity.attrString(key: String): String? =
    (attributes[key] as? JsonPrimitive)?.contentOrNull

internal fun HomeEntity.deviceClass(): String? = attrString("device_class")

internal fun HomeEntity.unit(): String = attrString("unit_of_measurement").orEmpty()

internal fun HomeEntity.friendly(): String = attrString("friendly_name") ?: id

internal fun HomeEntity.formatted(): String {
    val value = numericState ?: return state
    val text = if (kotlin.math.abs(value) >= 100) {
        String.format(Locale.US, "%.0f", value)
    } else {
        String.format(Locale.US, "%.1f", value)
    }
    val suffix = unit()
    return if (suffix.isBlank()) text else "$text $suffix"
}

internal data class EnergyMetric(
    val entityId: String,
    val label: String,
    val value: String,
    val icon: ImageVector,
)

/**
 * The SwiftUI app hardcoded solar entity IDs. Here they are auto-detected from
 * device_class + id keywords, so the card works before the IDs are pinned down.
 */
internal fun energyMetrics(entities: Map<String, HomeEntity>): List<EnergyMetric> {
    fun pick(deviceClass: String, vararg keywords: String): HomeEntity? =
        entities.values.firstOrNull { entity ->
            entity.deviceClass() == deviceClass &&
                entity.numericState != null &&
                keywords.any { entity.id.contains(it) }
        }

    val metrics = mutableListOf<EnergyMetric>()
    pick("power", "solar", "pv_power", "pv1", "inverter")?.let {
        metrics += EnergyMetric(it.id, "Dien mat troi", it.formatted(), Icons.Rounded.WbSunny)
    }
    pick("power", "grid")?.let {
        metrics += EnergyMetric(it.id, "Luoi dien", it.formatted(), Icons.Rounded.ElectricMeter)
    }
    pick("power", "load", "house", "consumption")?.let {
        metrics += EnergyMetric(it.id, "Tieu thu", it.formatted(), Icons.Rounded.Bolt)
    }
    pick("battery", "powerwall", "battery_soc", "pin")?.let {
        metrics += EnergyMetric(it.id, "Pin luu tru", it.formatted(), Icons.Rounded.BatteryChargingFull)
    }
    entities.values.firstOrNull {
        it.deviceClass() == "energy" && it.numericState != null &&
            (it.id.contains("today") || it.id.contains("daily") || it.id.contains("hom_nay"))
    }?.let {
        metrics += EnergyMetric(it.id, "Hom nay", it.formatted(), Icons.Rounded.Insights)
    }
    return metrics
}

/** Solar / energy summary card from HomeView.swift. Tap a metric to open the chart. */
@Composable
fun SolarEnergyCard(entities: Map<String, HomeEntity>, onChart: (String) -> Unit) {
    val metrics = remember(entities) { energyMetrics(entities) }
    ElevatedCard(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
        Column(Modifier.padding(18.dp)) {
            Text("Năng lượng", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            if (metrics.isEmpty()) {
                Text(
                    "Chưa dò được sensor năng lượng nào. Cần gán entity ID thật ở bước sau.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                metrics.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { metric ->
                            EnergyTile(metric = metric, modifier = Modifier.weight(1f), onClick = { onChart(metric.entityId) })
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun EnergyTile(metric: EnergyMetric, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(modifier.clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        Icon(metric.icon, contentDescription = null, tint = HumeColors.Amber, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(metric.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                metric.value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
