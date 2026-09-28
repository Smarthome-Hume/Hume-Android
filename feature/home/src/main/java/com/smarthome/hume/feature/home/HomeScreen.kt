package com.smarthome.hume.feature.home

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Trang Nha M3E theo demo hume-m3e-v4-dashboard.html rev12 (#page-home):
 * header → pills (an ninh + den) → "Goi y cho ban" → solcard →
 * solar → batcard → "Phong" → rooms grid + FAB speed-dial.
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
            HomeFabMenu(
                onTurnOnLights = { viewModel.turnOnAllLights() },
                onAc26 = { viewModel.ac26() },
                onArmAway = { viewModel.armAlarm("away", "Vắng nhà") },
                onEco = { viewModel.ecoMode() },
                modifier = Modifier.padding(bottom = 88.dp, end = 4.dp),
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxSize(),
    ) { _ ->
        Box(Modifier.fillMaxSize()) {
            androidx.compose.foundation.lazy.LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = 16.dp, bottom = 120.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                item {
                    RiseIn(0) {
                        HomeHeader(
                            state = state,
                            onSearch = { viewModel.openSearch(true) },
                            onNotif = { viewModel.openNotif(true) },
                        )
                    }
                }
                item {
                    RiseIn(1) {
                        PillsRow(
                            alarm = state.alarm,
                            lightsOnCount = state.lightsOn.size,
                            onArm = { mode, label -> viewModel.armAlarm(mode, label) },
                            onDisarm = { viewModel.disarmAlarm() },
                            onLights = { viewModel.openLights(true) },
                        )
                    }
                }
                item {
                    RiseIn(2) {
                        SectionTitle("Gợi ý cho bạn")
                    }
                }
                item { RiseIn(3) { SuggestCard(state) } }
                item { RiseIn(4) { SolarWeekCard(state) } }
                item { RiseIn(5) { SolarLiveCard(state) } }
                item { RiseIn(6) { BatteryCard(state.battery) } }
                item {
                    RiseIn(7) {
                        SectionTitle("Phòng")
                    }
                }
                item {
                    RiseIn(8) {
                        RoomGrid(
                            rooms = state.rooms,
                            onRoom = { viewModel.selectRoom(it) },
                            onToggleLight = { viewModel.toggle(it) },
                        )
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }

            // Sheet phong
            ui.selectedRoom?.let { room ->
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

/** h3 section title: 16px/700 nhu demo. */
@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(top = 4.dp, bottom = 2.dp),
    )
}

/**
 * Hieu ung vao .rise cua demo: opacity 0→1 + translateY 22dp→0, 700ms,
 * stagger theo index.
 */
@Composable
private fun RiseIn(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val offsetPx = with(androidx.compose.ui.platform.LocalDensity.current) { 22.dp.roundToPx() }
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(700, delayMillis = index * 60)) +
                slideInVertically(tween(700, delayMillis = index * 60)) { offsetPx },
    ) {
        content()
    }
}
