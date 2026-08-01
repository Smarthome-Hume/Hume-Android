@file:OptIn(ExperimentalMaterial3Api::class)

package com.smarthome.hume.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smarthome.hume.HumeApplication
import com.smarthome.hume.core.ha.HomeAssistantRepository
import com.smarthome.hume.core.model.HomeEntity
import com.smarthome.hume.core.model.RoomConfig
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeIcons
import java.util.Calendar
import java.util.Locale

internal const val ALARM_ENTITY = "alarm_control_panel.alarm_security"
private const val USER_NAME = "H\u1ea3i H\u00e0"
private const val LOCATION = "Li\u00ean Ph\u01b0\u1eddng, P. Ki\u1ebfn An"

/**
 * Home dashboard matching the prototype recording: header, status pills,
 * solar chart, battery card, staggered room carousels and the scene grid.
 */
@Composable
fun HomeScreen(ha: HomeAssistantRepository) {
    val app = LocalContext.current.applicationContext as HumeApplication
    val vm: HomeViewModel = viewModel(factory = HomeViewModelFactory(ha, app.sensorDatabase))
    val state by vm.uiState.collectAsStateWithLifecycle()
    val entities = state.entities
    val alarmState = entities[ALARM_ENTITY]?.state

    var roomSheet by remember { mutableStateOf<RoomConfig?>(null) }
    var lightsSheet by remember { mutableStateOf(false) }
    var notificationSheet by remember { mutableStateOf(false) }
    var chartEntityId by remember { mutableStateOf<String?>(null) }
    var weekly by remember { mutableStateOf<List<DayValue>>(emptyList()) }

    val dailyEnergy = remember(entities.size) { EnergyDetect.dailyEnergy(entities) }
    val solarPower = remember(entities.size) { EnergyDetect.solarPower(entities) }
    val battery = remember(entities.size) { EnergyDetect.battery(entities) }
    val runtime = remember(entities.size) { EnergyDetect.batteryRuntime(entities) }
    val extraSensors = remember(entities.size) { EnergyDetect.highlightSensors(entities, 4) }

    LaunchedEffect(entities.size) { vm.watchEnergySensors(entities) }
    LaunchedEffect(dailyEnergy?.entityId) {
        val id = dailyEnergy?.entityId
        weekly = if (id == null) emptyList() else vm.weekly(id)
    }

    val leftTiles = buildList {
        solarPower?.let {
            add(SmallTile(HumeIcons.Solar, it.formatted() + " " + it.unit(), "\u0110i\u1ec7n m\u1eb7t tr\u1eddi", it.entityId))
        }
        dailyEnergy?.let {
            add(SmallTile(HumeIcons.Solar, it.formatted() + " " + it.unit(), "S\u1ea3n l\u01b0\u1ee3ng", it.entityId))
        }
        extraSensors.take(2).forEach {
            add(SmallTile(HumeIcons.Desk, it.formatted() + " " + it.unit(), it.friendly(), it.entityId))
        }
    }
    val rightTiles = buildList {
        extraSensors.drop(2).forEach {
            add(SmallTile(HumeIcons.Desk, it.formatted() + " " + it.unit(), it.friendly(), it.entityId))
        }
        vm.rooms.mapNotNull { it.contactEntity }.take(2).forEach { contact ->
            val entity = entities[contact]
            add(
                SmallTile(
                    icon = if (entity?.isOn == true) HumeIcons.Door else HumeIcons.DoorClosed,
                    value = if (entity?.isOn == true) "\u0110ang m\u1edf" else "\u0110\u00e3 \u0111\u00f3ng",
                    label = entity?.friendly() ?: contact.substringAfter('.'),
                    entityId = contact,
                )
            )
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            HomeHeader(
                userName = USER_NAME,
                greeting = if (state.connected) greeting() else "\u0110ang k\u1ebft n\u1ed1i Home Assistant...",
                location = LOCATION,
                connected = state.connected,
                alertCount = state.alertCount,
                onOpenNotifications = { notificationSheet = true },
            )
        }
        state.error?.let { message ->
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(HumeColors.Red.copy(alpha = 0.12f))
                        .padding(14.dp)
                ) {
                    Text(message, style = MaterialTheme.typography.bodySmall, color = HumeColors.Red)
                }
            }
        }
        item {
            StatusChipRow(
                alarmState = alarmState,
                lightsOn = state.lightsOn,
                onOpenAlarm = { notificationSheet = true },
                onOpenLights = { lightsSheet = true },
            )
        }
        item {
            SolarChartCard(
                title = "\u0110i\u1ec7n m\u1eb7t tr\u1eddi",
                totalText = dailyEnergy?.formatted() ?: "--",
                unitText = dailyEnergy?.unit() ?: "kWh",
                days = weekly,
                emptyHint = if (dailyEnergy == null)
                    "Ch\u01b0a d\u00f2 \u0111\u01b0\u1ee3c sensor n\u0103ng l\u01b0\u1ee3ng"
                else
                    "Ch\u01b0a c\u00f3 l\u1ecbch s\u1eed 7 ng\u00e0y",
            )
        }
        item {
            BatteryCard(
                percent = battery?.numericState,
                charging = EnergyDetect.charging(entities),
                headline = runtime?.let { it.formatted() + " " + it.unit() } ?: (battery?.formatted()?.plus("%") ?: "--"),
                trailingLabel = if (EnergyDetect.charging(entities)) "\u0110\u1ea6Y L\u00daC" else "K\u1ebeT TH\u00daC L\u00daC",
                trailingValue = endTimeLabel(runtime?.numericState),
            )
        }
        item {
            RoomsShowcase(
                climateRooms = vm.climateRooms,
                otherRooms = vm.basicRooms,
                entities = entities,
                leftTiles = leftTiles,
                rightTiles = rightTiles,
                onOpenRoom = { roomSheet = it },
                onToggleLight = { vm.toggleRoomLight(it) },
                onTileClick = { chartEntityId = it },
                onAdjustTarget = { room, delta -> vm.adjustTarget(room, delta) },
            )
        }
        item {
            SceneGridSection(
                scenes = sceneItems(entities),
                alarmState = alarmState,
                onRun = { vm.activateScene(it) },
            )
        }
    }

    roomSheet?.let { room ->
        RoomBottomSheet(room = room, ha = ha, entities = entities, onDismiss = { roomSheet = null })
    }
    if (lightsSheet) {
        LightsBottomSheet(rooms = vm.rooms, ha = ha, entities = entities, onDismiss = { lightsSheet = false })
    }
    if (notificationSheet) {
        NotificationBottomSheet(entities = entities, onDismiss = { notificationSheet = false })
    }
    chartEntityId?.let { id ->
        ChartDialog(
            entityId = id,
            entities = entities,
            loadHistory = { vm.history(it) },
            onDismiss = { chartEntityId = null },
        )
    }
}

private fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 11 -> "Ch\u00e0o bu\u1ed5i s\u00e1ng"
        hour < 14 -> "Ch\u00e0o bu\u1ed5i tr\u01b0a"
        hour < 18 -> "Ch\u00e0o bu\u1ed5i chi\u1ec1u"
        else -> "Ch\u00e0o bu\u1ed5i t\u1ed1i"
    }
}

/** Clock time when the remaining runtime in hours runs out. */
private fun endTimeLabel(hoursRemaining: Double?): String {
    if (hoursRemaining == null || hoursRemaining <= 0.0) return "--:--"
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.MINUTE, (hoursRemaining * 60).toInt())
    return String.format(
        Locale.US,
        "%02d:%02d",
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
    )
}

// ---- shared helpers used by the sheets and the view model ----

internal fun toggleLight(ha: HomeAssistantRepository, room: RoomConfig, entities: Map<String, HomeEntity>) {
    val on = entities[room.lightEntity]?.isOn == true
    setLight(ha, room.lightEntity, !on)
}

internal fun setLight(ha: HomeAssistantRepository, entityId: String, on: Boolean) {
    if (on) ha.turnOn(entityId) else ha.turnOff(entityId)
}

internal fun setAllLights(ha: HomeAssistantRepository, rooms: List<RoomConfig>, on: Boolean) {
    rooms.forEach { setLight(ha, it.lightEntity, on) }
}

internal fun Map<String, HomeEntity>.num(entityId: String, digits: Int): String {
    val value = this[entityId]?.numericState ?: return "--"
    return String.format(Locale.US, "%.${digits}f", value)
}

internal fun Map<String, HomeEntity>.attr(entityId: String, key: String): String? =
    this[entityId]?.attrString(key)

internal fun alarmLabel(state: String?): String = when (state) {
    "disarmed" -> "\u0110\u00e3 t\u1eaft"
    "armed_home" -> "\u1ede nh\u00e0"
    "armed_away" -> "V\u1eafng nh\u00e0"
    "armed_night" -> "Ban \u0111\u00eam"
    "armed_vacation" -> "K\u1ef3 ngh\u1ec9"
    "arming", "pending" -> "\u0110ang k\u00edch ho\u1ea1t"
    "triggered" -> "\u0110ang b\u00e1o \u0111\u1ed9ng"
    else -> "Kh\u00f4ng r\u00f5"
}
