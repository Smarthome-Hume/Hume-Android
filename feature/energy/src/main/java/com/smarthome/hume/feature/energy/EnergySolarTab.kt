package com.smarthome.hume.feature.energy

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.alignByBaseline
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smarthome.hume.core.model.BatteryControl
import com.smarthome.hume.core.model.BatteryControlKind
import com.smarthome.hume.core.model.EnergyUiState
import com.smarthome.hume.core.ui.components.M3ECard
import com.smarthome.hume.core.ui.components.M3EIcons
import com.smarthome.hume.core.ui.components.M3ESwitch
import com.smarthome.hume.core.ui.components.M3EMotion
import com.smarthome.hume.core.ui.components.pressMorph
import com.smarthome.hume.core.ui.components.MsIcon
import com.smarthome.hume.core.ui.theme.LocalHumeExtraColors
import kotlin.math.roundToInt

@Composable
fun EnergySolarTab(
    state: EnergyUiState,
    ui: EnergyScreenUi,
    vm: EnergyViewModel,
    risePlayed: MutableSet<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SunsynkCard(state, risePlayed)
        Expander(
            icon = M3EIcons.Battery,
            title = "Sạc pin",
            subtitle = "${state.chargeControls.size} điều khiển",
            open = ui.chargeOpen,
            onToggle = vm::toggleCharge,
            riseKey = "sol-chg",
            riseDelay = 460,
            risePlayed = risePlayed,
        ) {
            state.chargeControls.forEach { c ->
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                )
                ControlRow(c, vm)
            }
        }
        Expander(
            icon = M3EIcons.BatteryFull,
            title = "Xả pin",
            subtitle = "${state.dischargeControls.size} điều khiển",
            open = ui.dischargeOpen,
            onToggle = vm::toggleDischarge,
            riseKey = "sol-dis",
            riseDelay = 480,
            risePlayed = risePlayed,
        ) {
            state.dischargeControls.forEach { c ->
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                )
                ControlRow(c, vm)
            }
        }
        EnergyFlowCard(flow = state.flow, ui = ui, vm = vm, risePlayed = risePlayed)
    }
}

// ---------- sunsynk summary ----------

@Composable
private fun SunsynkCard(state: EnergyUiState, risePlayed: MutableSet<String>) {
    val extra = LocalHumeExtraColors.current
    M3ECard(
        shape = RoundedCornerShape(32.dp),
        contentPadding = 20.dp,
        containerColor = extra.surfaceHighest,
        modifier = Modifier.riseOnce("sol-syn", 420, risePlayed),
    ) {
        // demo .yt{align-items:baseline}
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                "Tải tiêu thụ",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 14.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.alignByBaseline(),
            )
            Row(
                modifier = Modifier.alignByBaseline(),
            ) {
                Text(
                    String.format("%.1f", state.loadTotalKw),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFeatureSettings = "tnum",
                    ),
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    " kW",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alignByBaseline(),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        state.tiers.forEach { t ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    t.name,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                Text(
                    "${String.format("%.1f", t.kw)} kW",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFeatureSettings = "tnum",
                    ),
                )
            }
            Spacer(Modifier.height(5.dp))
            // demo: width = v/tong (khong phai v/max)
            val sumTier = state.tiers.sumOf { it.kw }.coerceAtLeast(0.01)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((t.kw / sumTier).toFloat().coerceIn(0f, 1f))
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary),
                )
            }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Pin S6",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 14.sp, fontWeight = FontWeight.Bold),
            )
            Text(
                "SOC ${state.battery.soc.roundToInt()}%",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = extra.success,
                ),
            )
        }
        Spacer(Modifier.height(8.dp))
        // demo .socbar2 i{transition:width .8s}
        val socFrac by animateFloatAsState(
            targetValue = (state.battery.soc / 100).toFloat().coerceIn(0f, 1f),
            animationSpec = tween(800),
            label = "socBar",
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(socFrac)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF16A34A)),
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniBox(
                "Công suất",
                miniVal(
                    String.format("%.1f", kotlin.math.abs(state.battery.powerW) / 1000) to "kW"),
                Modifier.weight(1f),
            )
            MiniBox(
                "Dòng · Áp",
                miniVal(
                    state.battery.currentA.roundToInt().toString() to "A",
                    state.battery.voltageV.roundToInt().toString() to "V"),
                Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniBox(
                "Sạc giới hạn",
                miniVal(state.battery.chargeLimitA.roundToInt().toString() to "A"),
                Modifier.weight(1f),
            )
            MiniBox(
                "Xả giới hạn",
                miniVal(state.battery.dischargeLimitA.roundToInt().toString() to "A"),
                Modifier.weight(1f),
            )
        }
    }
}

/** Gia tri + don vi kieu demo .mini .v small (11sp/600 onSurfaceVariant). */
@Composable
private fun miniVal(vararg parts: Pair<String, String>): AnnotatedString =
    buildAnnotatedString {
        parts.forEachIndexed { i, (num, unit) ->
            if (i > 0) append(" · ")
            append(num)
            withStyle(SpanStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )) { append(" $unit") }
        }
    }

@Composable
private fun MiniBox(label: String, value: AnnotatedString, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFeatureSettings = "tnum",
            ),
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

// ---------- expander ----------

@Composable
private fun Expander(
    icon: String,
    title: String,
    subtitle: String,
    open: Boolean,
    onToggle: () -> Unit,
    riseKey: String,
    riseDelay: Int,
    risePlayed: MutableSet<String>,
    content: @Composable () -> Unit,
) {
    // Chevron: transition transform .4s spring (demo .exph .chev)
    val chev by animateFloatAsState(
        targetValue = if (open) 180f else 0f,
        animationSpec = tween(400, easing = M3EMotion.spring),
        label = "expChev",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .riseOnce(riseKey, riseDelay, risePlayed)
            .clip(RoundedCornerShape(32.dp))
            .background(LocalHumeExtraColors.current.surfaceHighest),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            ) {
                MsIcon(
                    icon, null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 14.sp, fontWeight = FontWeight.Bold),
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp, fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            MsIcon(
                M3EIcons.ChevronDown, null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(26.dp)
                    .rotate(chev),
            )
        }
        AnimatedVisibility(
            visible = open,
            enter = expandVertically(
                animationSpec = tween(500, easing = M3EMotion.emphasized)),
            exit = shrinkVertically(
                animationSpec = tween(500, easing = M3EMotion.emphasized)),
        ) {
            Column(
                modifier = Modifier.padding(
                    top = 2.dp, bottom = 16.dp, start = 20.dp, end = 20.dp),
            ) {
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
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            c.name,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
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
                Text(
                    c.state.take(5),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp, fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Stepper(value: Double, unit: String, onChange: (Double) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StepperBtn("−") { onChange((value - 1).coerceAtLeast(0.0)) }
        Text(
            "${value.roundToInt()}$unit",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFeatureSettings = "tnum",
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 56.dp),
        )
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
            .pressMorph(pressedScale = 0.85f, onClick = onClick),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 16.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
