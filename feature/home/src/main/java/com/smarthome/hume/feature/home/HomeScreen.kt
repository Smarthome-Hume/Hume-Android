package com.smarthome.hume.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Trang Nha M3E: du lieu that tu HomeRepository.
 * (Giao dien theo demo hume-m3e-v4-dashboard.html rev12.)
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory()),
) {
    val state by viewModel.state.collectAsState()
    val ui by viewModel.ui.collectAsState()
    val snack = remember { SnackbarHostState() }

    ui.snackbar?.let { msg ->
        LaunchedEffect(msg) {
            snack.showSnackbar(msg)
            viewModel.clearSnack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        floatingActionButton = {
            if (state.lightsOn.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.turnOffAllLights() },
                    icon = { Icon(Icons.Outlined.Lightbulb, null) },
                    text = { Text("Tắt hết đèn") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxSize(),
    ) { _ ->
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    HomeHeader(
                        state = state,
                        onSearch = { viewModel.openSearch(true) },
                        onNotif = { viewModel.openNotif(true) },
                    )
                }
                item { SuggestCard(state) }
                item { SolarWeekCard(state) }
                item { SolarLiveCard(state) }
                item { BatteryCard(state.battery) }
                item {
                    SecurityRow(
                        alarm = state.alarm,
                        lightsOnCount = state.lightsOn.size,
                        onArm = { mode, label -> viewModel.armAlarm(mode, label) },
                        onDisarm = { viewModel.disarmAlarm() },
                        onLights = { viewModel.openLights(true) },
                    )
                }
                item {
                    RoomGrid(
                        rooms = state.rooms,
                        onRoom = { viewModel.selectRoom(it) },
                        onToggleLight = { viewModel.toggle(it) },
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            // Sheet phong
            ui.selectedRoom?.let { room ->
                // room la snapshot — lay ban moi nhat tu state de toggle cap nhat live
                val live = state.rooms.firstOrNull { it.key == room.key } ?: room
                RoomSheet(
                    room = live,
                    onDismiss = { viewModel.selectRoom(null) },
                    onToggle = { viewModel.toggle(it) },
                    onClimateTemp = { id, t -> viewModel.setClimateTemp(id, t) },
                    onHvacMode = { id, m -> viewModel.setHvacMode(id, m) },
                    onToggleClimate = { viewModel.toggleClimate(it) },
                )
            }
            if (ui.notifOpen) {
                NotificationSheet(
                    notifications = state.notifications,
                    onDismiss = { viewModel.openNotif(false) },
                )
            }
            if (ui.lightsOpen) {
                LightsSheet(
                    lights = state.lightsOn,
                    onDismiss = { viewModel.openLights(false) },
                    onToggle = { viewModel.toggle(it) },
                )
            }
            // Tim kiem phu toan man hinh
            if (ui.searchOpen) {
                val q = ui.searchQuery.trim().lowercase()
                val results = if (q.isEmpty()) emptyList()
                else viewModel.searchableDevices().filter {
                    it.label.lowercase().contains(q) || it.entityId.lowercase().contains(q)
                }.take(30)
                DeviceSearchView(
                    query = ui.searchQuery,
                    onQuery = { viewModel.onSearchQuery(it) },
                    results = results,
                    onToggle = { viewModel.toggle(it) },
                    onBack = { viewModel.openSearch(false) },
                )
            }
        }
    }
}
