package com.smarthome.hume.core.storage

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Port of Core/DailySnapshotStore.swift.
 *
 * One Double per (entityId, calendar day in Asia/Ho_Chi_Minh). Past days never
 * change, so the value is cached forever (pruned after 30 days) and only the
 * current day is read live from the websocket, exactly like the iOS charts.
 */
class DailySnapshotStore private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("hume_daily_snapshots", Context.MODE_PRIVATE)

    fun get(entityId: String, dayStartMs: Long): Double? {
        val key = keyFor(entityId, dayStartMs)
        if (!prefs.contains(key)) return null
        val raw = prefs.getString(key, null) ?: return null
        return raw.toDoubleOrNull()
    }

    fun set(entityId: String, dayStartMs: Long, value: Double) {
        prefs.edit().putString(keyFor(entityId, dayStartMs), value.toString()).apply()
    }

    /** Drops entries older than [keepDays] so the file cannot grow forever. */
    fun prune(keepDays: Int = 30) {
        val cutoff = dayString(startOfDay(System.currentTimeMillis(), -keepDays))
        val editor = prefs.edit()
        var dirty = false
        prefs.all.keys.forEach { key ->
            val day = key.substringAfterLast('|', "")
            if (day.isNotEmpty() && day < cutoff) {
                editor.remove(key)
                dirty = true
            }
        }
        if (dirty) editor.apply()
    }

    private fun keyFor(entityId: String, dayStartMs: Long): String =
        entityId + "|" + dayString(dayStartMs)

    companion object {
        private val zone: TimeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")

        @Volatile
        private var instance: DailySnapshotStore? = null

        fun get(context: Context): DailySnapshotStore =
            instance ?: synchronized(this) {
                instance ?: DailySnapshotStore(context).also { instance = it }
            }

        fun calendar(): Calendar = Calendar.getInstance(zone)

        /** Midnight of today shifted by [offsetDays], in Asia/Ho_Chi_Minh. */
        fun startOfDay(fromMs: Long, offsetDays: Int = 0): Long {
            val cal = calendar()
            cal.timeInMillis = fromMs
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.add(Calendar.DAY_OF_YEAR, offsetDays)
            return cal.timeInMillis
        }

        fun dayString(timeMs: Long): String {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            fmt.timeZone = zone
            return fmt.format(Date(timeMs))
        }

        /** dayNames in the SwiftUI charts: index 0 = Sunday. */
        private val dayNames = listOf("CN", "T2", "T3", "T4", "T5", "T6", "T7")

        fun dayLabel(timeMs: Long): String {
            val cal = calendar()
            cal.timeInMillis = timeMs
            return dayNames[(cal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)]
        }
    }
}
