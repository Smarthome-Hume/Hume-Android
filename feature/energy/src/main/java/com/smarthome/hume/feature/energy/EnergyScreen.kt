package com.smarthome.hume.feature.energy

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EnergyScreen(
    vm: EnergyViewModel = viewModel(factory = EnergyViewModel.factory()),
    /** Deep-link tu the goi y ("battery"): mo sub-tab Dien mat troi + cuon toi the nang luong. */
    deepLink: String? = null,
    onDeepLinkConsumed: () -> Unit = {},
) {
    val state by vm.state.collectAsState()
    val ui by vm.ui.collectAsState()
    val haptic = rememberHaptic()
    val scope = rememberCoroutineScope()
    // Thong bao trang thai he thong dien khi state thay doi
    val ctx = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(state) {
        EnergyNotifier.onStateChanged(ctx, state)
    }
    var paneVisible by remember { mutableStateOf(true) }
    val risePlayed = remember { mutableSetOf<String>() }
    // Dinh vi the nang luong de deep-link "Xem pin" cuon toi
    // (BringIntoViewRequester tu dong cuon LazyColumn ke ca khi the nam long trong item).
    val energyBivr = remember { BringIntoViewRequester() }

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

    // Popup tai cho tu goi y nang luong / node pin flow card (muc 14)
    var insightPopup by remember { mutableStateOf<InsightPopup?>(null) }

    // Deep-link "Xem pin": chuyen sang sub-tab Dien mat troi roi cuon toi the nang luong.
    LaunchedEffect(deepLink) {
        if (deepLink != "battery") return@LaunchedEffect
        if (ui.tab != EnergySubTab.Solar) {
            paneVisible = false
            delay(180)
            vm.setTab(EnergySubTab.Solar)
            paneVisible = true
            delay(150)
        }
        runCatching { energyBivr.bringIntoView() }
        onDeepLinkConsumed()
    }

    // Full-bleed viewport: status-bar spacing and trailing navbar clearance
    // are scrollable content padding; the list still extends behind the overlay.
    val statusBarTop =
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // Action tu the goi y nang luong: popup tai cho hoac chuyen sub-tab
    // (khong dieu huong ra ngoai trang Nang luong).
    fun handleInsightAction(act: InsightAction) {
        when (val t = act.target) {
            is InsightActionTarget.DevicePopup ->
                insightPopup = InsightPopup.Device(t.entityId, t.label)
            InsightActionTarget.BatteryPopup ->
                insightPopup = InsightPopup.Battery
            is InsightActionTarget.GoTab ->
                selectTab(t.tab.ordinal)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(
            top = statusBarTop + 8.dp, start = 18.dp, end = 18.dp,
            bottom = 120.dp,
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
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp,
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
                        energyBivr = energyBivr,
                        onBatteryClick = { insightPopup = InsightPopup.Battery },
                    )
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
        // Popup tai cho (muc 14): phu len noi dung tab, khong dieu huong
        InsightPopupOverlay(
            popup = insightPopup,
            battery = state.battery,
            toggleStates = state.toggleStates,
            onToggle = vm::toggle,
            onDismiss = { insightPopup = null },
        )
    }
}
