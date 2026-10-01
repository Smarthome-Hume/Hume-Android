package com.smarthome.hume.feature.energy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smarthome.hume.core.ui.components.EsubGroup
import com.smarthome.hume.core.ui.components.rememberHaptic
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Tab Dien: header + esub (Tieu thu / Dien mat troi) + noi dung theo demo v4 rev12.
 * (Khong boc HumeM3ETheme o day — root M3ERootScreen da boc.)
 */
@Composable
fun EnergyScreen(
    vm: EnergyViewModel = viewModel(factory = EnergyViewModel.factory()),
) {
    val state by vm.state.collectAsState()
    val ui by vm.ui.collectAsState()
    val haptic = rememberHaptic()
    val scope = rememberCoroutineScope()
    var paneVisible by remember { mutableStateOf(true) }
    val risePlayed = remember { mutableSetOf<String>() }

    // Doi subtab: fade/slide 180ms + vibrate 6ms (demo .etab/.pre)
    fun selectTab(i: Int) {
        val t = EnergySubTab.entries[i]
        if (t == ui.tab) return
        haptic()
        scope.launch {
            paneVisible = false
            delay(180)
            vm.setTab(t)
            paneVisible = true
        }
    }

    // Full-bleed tran duoi status bar trong suot (2026-09-30): inset status
    // bar nam trong contentPadding de scroll lien mach (nhu tab Nha).
    val statusBarTop =
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(
            // The cuoi trang cach navbar 20dp (navbar floating ~86dp +
            // margin 20dp + system inset).
            top = statusBarTop + 8.dp, start = 18.dp, end = 18.dp,
            bottom = 140.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            // Title boc boi nen card
            // Header: chi title duoc boc nen (subtitle de ngoai, khong nen)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .riseOnce("hdr", 0, risePlayed),
                ) {
                    Text(
                        "Năng lượng",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.3).sp,
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
        item {
            EsubGroup(
                items = listOf("Tiêu thụ", "Điện mặt trời"),
                selectedIndex = ui.tab.ordinal,
                onSelect = ::selectTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .riseOnce("esub", 400, risePlayed),
            )
        }
        item {
            Box(Modifier.paneFade(paneVisible)) {
                when (ui.tab) {
                    EnergySubTab.Cons -> EnergyConsTab(
                        state = state, ui = ui, vm = vm, risePlayed = risePlayed,
                    )
                    EnergySubTab.Solar -> EnergySolarTab(
                        state = state, ui = ui, vm = vm, risePlayed = risePlayed,
                    )
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}
