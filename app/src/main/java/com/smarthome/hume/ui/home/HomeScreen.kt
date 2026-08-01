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
import androidx.compose.runtime.DisposableEffect
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
import com.smarthome.hume.core.model.HumeConfig
import com.smarthome.hume.core.model.RoomConfig
import com.smarthome.hume.core.scene.ManagedListsStore
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeIcons
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

private const val USER_NAME = "H\u1ea3i H\u00e0"
private const val LOCATION = "Li\u00ean Ph\u01b0\u1eddng, P. Ki\u1ebfn An"

/** Alarmo when present, alarm_security otherwise (AlarmLights.swift). */
internal fun alarmEntityId(entities: Map<String, HomeEntity>): String =
    if (entities.containsKey(HumeConfig.ALARM_PRIMARY)) HumeConfig.ALARM_PRIMARY else HumeConfig.ALARM_FALLBACK

internal const val ALARM_ENTITY = HumeConfig.ALARM_FALLBACK

/**
 * Home dashboard ported from HomeView.swift: header, alarm/lights pills,
 * solar chart, Powerwall card, staggered carousels and the scene grid.
 */
@Composable
fun HomeScreen(ha: HomeAssistantRepository) {
    val context = LocalContext.current
    val app = context.applicationContext as HumeApplication
    val lists = remember { ManagedListsStore.get(app) }
    val vm: HomeViewModel = viewModel(factory = HomeViewModelFactory(ha, app.sensorDatabase, lists))
    val state by vm.uiState.collectAsStateWithLifecycle()
    val entities = state.entities
    val alarmEntity = alarmEntityId(entities)
    val alarmState = entities[alarmEntity]?.state

    var roomSheet by remember { mutableStateOf<RoomConfig?>(null) }
    var lightsSheet by remember { mutableStateOf(false) }
    var notificationSheet by remember { mutableStateOf(false) }
    var chartEntityId by remember { mutableStateOf<String?>(null) }
    var weekly by remember { mutableStateOf<List<DayValue>>(emptyList()) }

    LaunchedEffect(Unit) { vm.watchEntities(EnergyDetect.watchedIds()) }
    // Keyed on "do we have a snapshot yet", never on the entity count: the count
    // changes whenever Home Assistant adds or drops an entity, which would restart
    // the subscription for no reason.
    LaunchedEffect(entities.isNotEmpty()) { vm.watchEnergySensors(entities) }
    LaunchedEffect(Unit) { weekly = vm.weekly(HumeConfig.PV_TODAY) }

    // HomeView.onChange(of: activeSheet?.id): while a room sheet is open only
    // that room keeps repainting, every other room is frozen.
    DisposableEffect(roomSheet?.rawKey) {
        ha.setActiveRoom(roomSheet?.rawKey)
        onDispose { }
    }

    // Six small sensor tiles, exactly the list the iOS app shows.
    val leftTiles = HumeConfig.sensorTiles.map { tile ->
        SmallTile(
            icon = HumeIcons.sensor(tile.icon),
            value = sensorValue(entities[tile.entityId], tile.unit),
            label = tile.label,
            entityId = tile.entityId,
        )
    }

    // Right column: the two selector-driven cards (device power + door).
    val deviceKey = entities[HumeConfig.ACTIVE_CARD]?.state
    val device = HumeConfig.deviceCards[deviceKey] ?: HumeConfig.deviceCards.getValue("Table")
    val doorKey = entities[HumeConfig.ACTIVE_CARD_2]?.state
    val door = HumeConfig.doorCards[doorKey] ?: HumeConfig.doorCards.getValue("Master")
    val rightTiles = listOf(
        SmallTile(
            icon = HumeIcons.sensor(device.icon),
            value = (entities[device.entityId]?.numericState?.toInt() ?: 0).toString() + " W",
            label = device.label,
            entityId = device.entityId,
        ),
        SmallTile(
            // DoorCardView.swift always draws the same "door" glyph; only the
            // timestamp underneath changes when the contact opens or closes.
            icon = HumeIcons.Door,
            value = agoText(entities[door.entityId]?.lastChanged),
            label = door.label,
            entityId = door.entityId,
        ),
    )

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 130.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            HomeHeader(
                userName = entities["person.hutchet"]?.friendly() ?: USER_NAME,
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
            // Tapping the alarm card swaps the row for the five mode buttons,
            // exactly like AlarmLightsView does on iOS.
            StatusChipRow(
                alarmState = alarmState,
                lightsOn = state.lightsOn,
                ha = ha,
                alarmEntity = alarmEntity,
                onOpenLights = { lightsSheet = true },
            )
        }
        item {
            val today = entities[HumeConfig.PV_TODAY]?.numericState ?: 0.0
            SolarChartCard(
                title = "\u0110i\u1ec7n m\u1eb7t tr\u1eddi",
                totalText = String.format(Locale.US, "%.1f", today),
                unitText = "kWh",
                days = weekly,
                emptyHint = "Ch\u01b0a c\u00f3 l\u1ecbch s\u1eed 7 ng\u00e0y",
            )
        }
        item {
            BatteryCard(
                soc = EnergyDetect.soc(entities),
                power = EnergyDetect.power(entities),
                backupSoc = EnergyDetect.backupSoc(entities),
                timeText = EnergyDetect.runtimeText(entities),
                finishTime = endTimeLabel(EnergyDetect.runtimeHours(entities)),
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
            // Scenes live in LocalSceneStore, not in scene.* entities.
            SceneGridSection(ha = ha, alarmState = alarmState)
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

/** LiveSmallSensor formatting: one decimal below 10, none above. */
private fun sensorValue(entity: HomeEntity?, unit: String): String {
    val value = entity?.numericState ?: return (entity?.state ?: "--") + " " + unit
    val text = if (abs(value) < 10.0) String.format(Locale.US, "%.1f", value)
    else String.format(Locale.US, "%.0f", value)
    return "$text $unit"
}

/** DoorCardView.agoText */
internal fun agoText(lastChanged: String?): String {
    val millis = parseTimestamp(lastChanged) ?: return "\u2014"
    val minutes = ((System.currentTimeMillis() - millis) / 60_000L).toInt()
    return when {
        minutes < 1 -> "V\u1eeba xong"
        minutes < 60 -> "$minutes ph\u00fat tr\u01b0\u1edbc"
        else -> (minutes / 60).toString() + " gi\u1edd tr\u01b0\u1edbc"
    }
}

internal fun parseTimestamp(value: String?): Long? {
    if (value.isNullOrBlank()) return null
    return runCatching { Instant.parse(value).toEpochMilli() }
        .recoverCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }
        .getOrNull()
}

private fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour >= 18 -> "Ch\u00e0o bu\u1ed5i t\u1ed1i"
        hour >= 12 -> "Ch\u00e0o bu\u1ed5i chi\u1ec1u"
        else -> "Ch\u00e0o bu\u1ed5i s\u00e1ng"
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

internal fun alarmLabel(state: String?): String = HumeConfig.alarmLabel(state)
