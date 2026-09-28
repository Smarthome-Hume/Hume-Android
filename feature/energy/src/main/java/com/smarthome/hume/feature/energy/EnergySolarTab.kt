package com.smarthome.hume.feature.energy

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smarthome.hume.core.model.BatteryControl
import com.smarthome.hume.core.model.BatteryControlKind
import com.smarthome.hume.core.model.EnergyUiState
import com.smarthome.hume.core.ui.components.M3ECard
import com.smarthome.hume.core.ui.components.M3ESwitch
import com.smarthome.hume.core.ui.components.M3EIcons
import kotlin.math.roundToInt

@Composable
fun EnergySolarTab(
    state: EnergyUiState,
    ui: EnergyScreenUi,
    vm: EnergyViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        EnergyFlowCard(state.flow)
        SunsynkCard(state)
        Expander(
            icon = M3EIcons.Battery,
            title = "Sạc pin",
            subtitle = "${state.chargeControls.size} điều khiển",
            open = ui.chargeOpen,
            onToggle = vm::toggleCharge,
        ) {
            state.chargeControls.forEach { c -> ControlRow(c, vm) }
        }
        Expander(
            icon = M3EIcons.BatteryFull,
            title = "Xả pin",
            subtitle = "${state.dischargeControls.size} điều khiển",
            open = ui.dischargeOpen,
            onToggle = vm::toggleDischarge,
        ) {
            state.dischargeControls.forEach { c -> ControlRow(c, vm) }
        }
    }
}

// ---------- sunsynk summary ----------

@Composable
private fun SunsynkCard(state: EnergyUiState) {
    M3ECard(shape = RoundedCornerShape(32.dp), contentPadding = 20.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text("Tải tiêu thụ", fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(String.format("%.1f", state.loadTotalKw),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold)
                Text(" kW", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(12.dp))
        val maxTier = (state.tiers.maxOfOrNull { it.kw } ?: 1.0).coerceAtLeast(0.01)
        state.tiers.forEach { t ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(t.name, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold)
                Text("${String.format("%.1f", t.kw)} kW",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(5.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((t.kw / maxTier).toFloat().coerceIn(0f, 1f))
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary),
                )
            }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Pin S6", fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall)
            Text("SOC ${state.battery.soc.roundToInt()}%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF16A34A))
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((state.battery.soc / 100).toFloat().coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF16A34A)),
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniBox("Công suất",
                "${String.format("%.1f", kotlin.math.abs(state.battery.powerW) / 1000)} kW",
                Modifier.weight(1f))
            MiniBox("Dòng · Áp",
                "${state.battery.currentA.roundToInt()} A · ${state.battery.voltageV.roundToInt()} V",
                Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniBox("Sạc giới hạn",
                "${state.battery.chargeLimitA.roundToInt()} A", Modifier.weight(1f))
            MiniBox("Xả giới hạn",
                "${state.battery.dischargeLimitA.roundToInt()} A", Modifier.weight(1f))
        }
    }
}

@Composable
private fun MiniBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(14.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold)
        Text(value, fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 3.dp))
    }
}

// ---------- expander ----------

@Composable
private fun Expander(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    open: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable { onToggle() }
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = if (open) 8.dp else 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            ) {
                Icon(icon, null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(M3EIcons.ChevronDown, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(26.dp)
                    .rotate(if (open) 180f else 0f))
        }
        AnimatedVisibility(
            visible = open,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)) {
                content()
            }
        }
    }
}

// ---------- control rows ----------

@Composable
private fun ControlRow(c: BatteryControl, vm: EnergyViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(c.name, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f))
        when (c.kind) {
            BatteryControlKind.Switch -> M3ESwitch(
                checked = c.isOn,
                onCheckedChange = { vm.toggle(c.entityId) },
            )
            BatteryControlKind.Number -> Stepper(
                value = c.state.toDoubleOrNull() ?: 0.0,
                unit = c.unit.ifEmpty { "%" },
                onChange = { vm.setNumber(c.entityId, it) },
            )
            BatteryControlKind.Time -> {
                Text(c.state.take(5),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Stepper(value: Double, unit: String, onChange: (Double) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepperBtn("−") { onChange((value - 1).coerceAtLeast(0.0)) }
        Text("${value.roundToInt()}$unit",
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 10.dp))
        StepperBtn("+") { onChange(value + 1) }
    }
}

@Composable
private fun StepperBtn(text: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable { onClick() },
    ) {
        Text(text, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium)
    }
}
