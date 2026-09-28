package com.smarthome.hume.feature.energy

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smarthome.hume.core.ui.components.M3EIcons
import com.smarthome.hume.core.ui.theme.HumeM3ETheme

/**
 * Tab Dien: header + esub (Tieu thu / Dien mat troi) + noi dung theo demo v4 rev12.
 */
@Composable
fun EnergyScreen(
    vm: EnergyViewModel = viewModel(factory = EnergyViewModel.factory()),
) {
    HumeM3ETheme {
        val state by vm.state.collectAsState()
        val ui by vm.ui.collectAsState()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)) {
                    Text(
                        "Điện",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Dòng chảy & tiêu thụ thời gian thực",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item { EnergySubTabs(selected = ui.tab, onSelect = vm::setTab) }
            item {
                when (ui.tab) {
                    EnergySubTab.Cons -> EnergyConsTab(state = state, ui = ui, vm = vm)
                    EnergySubTab.Solar -> EnergySolarTab(state = state, ui = ui, vm = vm)
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

/**
 * esub: connected pill group 2 tab, tab chon dung primaryContainer + icon check hien.
 */
@Composable
fun EnergySubTabs(
    selected: EnergySubTab,
    onSelect: (EnergySubTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        EnergySubTab.entries.forEach { tab ->
            val isSel = tab == selected
            val label = if (tab == EnergySubTab.Cons) "Tiêu thụ" else "Điện mặt trời"
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (isSel) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0f),
                    )
                    .clickable { onSelect(tab) }
                    .padding(vertical = 10.dp, horizontal = 10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (isSel) {
                        Icon(
                            M3EIcons.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(17.dp),
                        )
                    }
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
