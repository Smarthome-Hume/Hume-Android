package com.smarthome.hume.core.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar
import java.util.UUID

private const val TAG = "HumeHA"
private const val PREFS = "hume_scene_schedules"
private const val KEY = "schedules_v1"

/**
 * Port of SceneSchedule / SceneScheduleStore in the SwiftUI app.
 *
 * weekdays uses the iOS numbering: 1 = Monday \u2026 7 = Sunday. An empty list
 * means every day. Firing is done with AlarmManager instead of
 * UNUserNotificationCenter, so a schedule still runs with the app closed.
 */
@Serializable
data class SceneSchedule(
    val id: String = UUID.randomUUID().toString(),
    val sceneId: String,
    val hour: Int,
    val minute: Int,
    val weekdays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
    val enabled: Boolean = true,
) {
    val timeLabel: String
        get() = pad(hour) + ":" + pad(minute)

    /** weekdayLabel in SceneSchedule.swift */
    val weekdayLabel: String
        get() {
            val days = weekdays.sorted()
            if (days.isEmpty() || days.size == 7) return "H\u1eb1ng ng\u00e0y"
            if (days == listOf(1, 2, 3, 4, 5)) return "Th\u1ee9 2 \u2013 Th\u1ee9 6"
            if (days == listOf(6, 7)) return "Cu\u1ed1i tu\u1ea7n"
            return days.joinToString(" \u00b7 ") { dayName(it) }
        }

    private fun pad(value: Int): String = if (value < 10) "0" + value else value.toString()
}

/** 1 = Monday, 7 = Sunday, same as the iOS editor. */
fun dayName(day: Int): String = when (day) {
    1 -> "T2"
    2 -> "T3"
    3 -> "T4"
    4 -> "T5"
    5 -> "T6"
    6 -> "T7"
    else -> "CN"
}

class SceneScheduleStore private constructor(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _schedules = MutableStateFlow(load())
    val schedules: StateFlow<List<SceneSchedule>> = _schedules.asStateFlow()

    private fun load(): List<SceneSchedule> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<SceneSchedule>>(raw) }.getOrElse {
            Log.e(TAG, "SceneScheduleStore load failed", it)
            emptyList()
        }
    }

    private fun commit(list: List<SceneSchedule>) {
        val sorted = list.sortedWith(compareBy({ it.hour }, { it.minute }))
        _schedules.value = sorted
        prefs.edit().putString(KEY, json.encodeToString(sorted)).apply()
        rescheduleAll()
    }

    fun add(schedule: SceneSchedule) = commit(_schedules.value + schedule)

    fun update(schedule: SceneSchedule) =
        commit(_schedules.value.map { if (it.id == schedule.id) schedule else it })

    fun delete(id: String) {
        cancel(id)
        commit(_schedules.value.filterNot { it.id == id })
    }

    fun toggle(id: String, enabled: Boolean) =
        commit(_schedules.value.map { if (it.id == id) it.copy(enabled = enabled) else it })

    /** Called on every change and from HumeApplication at startup. */
    fun rescheduleAll() {
        _schedules.value.forEach { schedule ->
            if (schedule.enabled) schedule(schedule) else cancel(schedule.id)
        }
    }

    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun intentFor(id: String): PendingIntent {
        val intent = Intent(context, SceneScheduleReceiver::class.java).apply {
            action = SceneScheduleReceiver.ACTION_RUN
            putExtra(SceneScheduleReceiver.EXTRA_SCHEDULE_ID, id)
        }
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getBroadcast(context, id.hashCode(), intent, flags)
    }

    private fun schedule(schedule: SceneSchedule) {
        val triggerAt = nextTrigger(schedule) ?: return
        val pending = intentFor(schedule.id)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
            Log.i(TAG, "Schedule " + schedule.id + " armed for " + triggerAt)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to arm schedule " + schedule.id, t)
        }
    }

    private fun cancel(id: String) {
        runCatching { alarmManager.cancel(intentFor(id)) }
    }

    /** Next matching weekday at hour:minute, searching up to eight days ahead. */
    fun nextTrigger(schedule: SceneSchedule, from: Long = System.currentTimeMillis()): Long? {
        val days = if (schedule.weekdays.isEmpty()) listOf(1, 2, 3, 4, 5, 6, 7) else schedule.weekdays
        val calendar = Calendar.getInstance().apply { timeInMillis = from }
        for (offset in 0..8) {
            val candidate = Calendar.getInstance().apply {
                timeInMillis = from
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, schedule.hour)
                set(Calendar.MINUTE, schedule.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (candidate.timeInMillis <= calendar.timeInMillis) continue
            // Calendar.SUNDAY == 1, so shift into the iOS numbering where Monday is 1.
            val isoDay = ((candidate.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1
            if (days.contains(isoDay)) return candidate.timeInMillis
        }
        return null
    }

    companion object {
        @Volatile
        private var instance: SceneScheduleStore? = null

        fun get(context: Context): SceneScheduleStore =
            instance ?: synchronized(this) {
                instance ?: SceneScheduleStore(context.applicationContext).also { instance = it }
            }
    }
}
