package com.smarthome.hume.ui.scenes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smarthome.hume.core.scene.LocalSceneStore
import com.smarthome.hume.core.schedule.SceneSchedule
import com.smarthome.hume.core.schedule.SceneScheduleStore
import com.smarthome.hume.core.schedule.dayName
import com.smarthome.hume.ui.theme.HumeColors
import com.smarthome.hume.ui.theme.HumeShapes
import com.smarthome.hume.ui.theme.glassSurface

/**
 * Port of Views/Profile/SceneScheduleView.swift plus its ScheduleEditorView.
 *
 * One card per schedule: big time, scene name in orange, weekday summary and
 * the enable toggle, with edit and delete underneath. The editor picks a local
 * scene, an hour and minute, and the repeat days (1 = Monday like on iOS).
 *
 * Unselected rows, steppers and day chips use the theme fill so the editor is
 * readable in dark mode; only the selected state stays orange.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SceneScheduleSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SceneScheduleStore.get(context) }
    val sceneStore = remember { LocalSceneStore.get(context) }
    val schedules by store.schedules.collectAsStateWithLifecycle()
    val scenes by sceneStore.scenes.collectAsStateWithLifecycle()

    var editing by remember { mutableStateOf<SceneSchedule?>(null) }
    var creating by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = HumeColors.Background,
        dragHandle = null,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Schedule,
                    contentDescription = null,
                    tint = HumeColors.Orange,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "H\u1eb9n gi\u1edd ng\u1eef c\u1ea3nh",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = HumeColors.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = null, tint = HumeColors.TextSecondary)
                }
            }

            if (schedules.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().glassSurface(radius = HumeShapes.Sheet).padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = HumeColors.TextSecondary,
                        modifier = Modifier.size(30.dp),
                    )
                    Text(
                        "Ch\u01b0a c\u00f3 l\u1ecbch n\u00e0o",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HumeColors.TextPrimary,
                    )
                    Text(
                        "T\u1ea1o l\u1ecbch \u0111\u1ec3 t\u1ef1 \u0111\u1ed9ng ch\u1ea1y m\u1ed9t k\u1ecbch b\u1ea3n v\u00e0o gi\u1edd c\u1ed1 \u0111\u1ecbnh \u2014 ch\u1ea1y k\u1ec3 c\u1ea3 khi \u0111\u00f3ng app.",
                        fontSize = 12.sp,
                        color = HumeColors.TextSecondary,
                    )
                }
            }

            schedules.forEach { schedule ->
                val sceneName = scenes.firstOrNull { it.id == schedule.sceneId }?.name
                    ?: "(k\u1ecbch b\u1ea3n \u0111\u00e3 xo\u00e1)"
                Column(
                    Modifier.fillMaxWidth().glassSurface(radius = HumeShapes.Sheet).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(
                            Modifier.weight(1f).clickable { editing = schedule },
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                schedule.timeLabel,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = HumeColors.TextPrimary,
                            )
                            Text(
                                sceneName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = HumeColors.Orange,
                            )
                            Text(schedule.weekdayLabel, fontSize = 11.sp, color = HumeColors.TextSecondary)
                        }
                        Switch(
                            checked = schedule.enabled,
                            onCheckedChange = { store.toggle(schedule.id, it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = HumeColors.Orange),
                        )
                    }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(HumeColors.Divider))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            Modifier.clickable { editing = schedule },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.Edit,
                                contentDescription = null,
                                tint = HumeColors.TextPrimary,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("S\u1eeda", fontSize = 13.sp, color = HumeColors.TextPrimary)
                        }
                        Spacer(Modifier.weight(1f))
                        Row(
                            Modifier.clickable { store.delete(schedule.id) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Xo\u00e1", fontSize = 13.sp, color = Color(0xFFEF4444))
                        }
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .glassSurface(radius = 24.dp)
                    .clickable(enabled = scenes.isNotEmpty()) { creating = true }
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = null,
                    tint = HumeColors.Orange,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Th\u00eam l\u1ecbch",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HumeColors.Orange,
                )
            }
        }
    }

    val target = editing
    if (creating || target != null) {
        ScheduleEditorDialog(
            schedule = target,
            sceneOptions = scenes.map { it.id to it.name },
            onDismiss = { creating = false; editing = null },
            onSave = { saved ->
                if (target == null) store.add(saved) else store.update(saved)
                creating = false
                editing = null
            },
        )
    }
}

@Composable
private fun ScheduleEditorDialog(
    schedule: SceneSchedule?,
    sceneOptions: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onSave: (SceneSchedule) -> Unit,
) {
    var sceneId by remember { mutableStateOf(schedule?.sceneId ?: sceneOptions.firstOrNull()?.first) }
    var hour by remember { mutableStateOf(schedule?.hour ?: 7) }
    var minute by remember { mutableStateOf(schedule?.minute ?: 0) }
    var days by remember {
        mutableStateOf(
            (schedule?.weekdays?.takeIf { it.isNotEmpty() } ?: listOf(1, 2, 3, 4, 5, 6, 7)).toSet(),
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HumeColors.Card,
        title = {
            Text(
                if (schedule == null) "Th\u00eam l\u1ecbch" else "S\u1eeda l\u1ecbch",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(
                Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("K\u1ecbch b\u1ea3n", fontSize = 12.sp, color = HumeColors.TextSecondary)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    sceneOptions.forEach { option ->
                        val selected = option.first == sceneId
                        Text(
                            option.second,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) HumeColors.Orange else HumeColors.TextPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) HumeColors.OrangeSoft else HumeColors.FillTertiary,
                                )
                                .clickable { sceneId = option.first }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        )
                    }
                }

                Text("Gi\u1edd ch\u1ea1y", fontSize = 12.sp, color = HumeColors.TextSecondary)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Stepper(
                        label = "Gi\u1edd",
                        value = hour,
                        modifier = Modifier.weight(1f),
                        onDown = { hour = if (hour == 0) 23 else hour - 1 },
                        onUp = { hour = if (hour == 23) 0 else hour + 1 },
                    )
                    Stepper(
                        label = "Ph\u00fat",
                        value = minute,
                        modifier = Modifier.weight(1f),
                        onDown = { minute = if (minute == 0) 45 else minute - 15 },
                        onUp = { minute = if (minute >= 45) 0 else minute + 15 },
                    )
                }

                Text("L\u1eb7p l\u1ea1i", fontSize = 12.sp, color = HumeColors.TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..7).forEach { day ->
                        val on = days.contains(day)
                        Box(
                            Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (on) HumeColors.Orange else HumeColors.FillTertiary)
                                .clickable { days = if (on) days - day else days + day },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                dayName(day),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (on) Color.White else HumeColors.TextSecondary,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = sceneId != null,
                onClick = {
                    val id = sceneId ?: return@TextButton
                    val base = schedule ?: SceneSchedule(sceneId = id, hour = hour, minute = minute)
                    onSave(
                        base.copy(
                            sceneId = id,
                            hour = hour,
                            minute = minute,
                            weekdays = days.sorted(),
                        ),
                    )
                },
            ) { Text("L\u01b0u") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hu\u1ef7") } },
    )
}

@Composable
private fun Stepper(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    onDown: () -> Unit,
    onUp: () -> Unit,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(HumeColors.FillTertiary)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.KeyboardArrowDown,
            contentDescription = null,
            tint = HumeColors.TextSecondary,
            modifier = Modifier.size(22.dp).clickable(onClick = onDown),
        )
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 10.sp, color = HumeColors.TextSecondary)
            Text(
                if (value < 10) "0" + value else value.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = HumeColors.TextPrimary,
            )
        }
        Icon(
            Icons.Rounded.KeyboardArrowUp,
            contentDescription = null,
            tint = HumeColors.TextSecondary,
            modifier = Modifier.size(22.dp).clickable(onClick = onUp),
        )
    }
}
