package com.smarthome.hume.core.storage

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.smarthome.hume.core.ha.HistoryPoint

private const val TAG = "HumeHA"
private const val DB_NAME = "hume_sensors.db"
private const val DB_VERSION = 1
private const val TABLE = "readings"

/**
 * Local sensor history, ported from the SwiftUI SensorDatabase.
 *
 * Plain SQLite on purpose: Room would need an annotation processor, and the
 * AGP 9 / Kotlin 2.4 toolchain here already builds cleanly without one.
 * Used as the offline fallback for ChartDialog when /api/history is unavailable.
 */
class SensorDatabase(context: Context) : SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    /** entityId -> last write time, so a chatty sensor cannot hammer the disk. */
    private val lastWrite = HashMap<String, Long>()
    private val minWriteIntervalMs = 60_000L

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS $TABLE (" +
                "entity_id TEXT NOT NULL, " +
                "ts INTEGER NOT NULL, " +
                "value REAL NOT NULL)"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_readings_entity_ts ON $TABLE (entity_id, ts)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }

    /** Store one numeric reading. Call from a background thread. */
    fun record(entityId: String, value: Double, timeMs: Long = System.currentTimeMillis()) {
        val previous = lastWrite[entityId]
        if (previous != null && timeMs - previous < minWriteIntervalMs) return
        lastWrite[entityId] = timeMs
        try {
            val values = ContentValues().apply {
                put("entity_id", entityId)
                put("ts", timeMs)
                put("value", value)
            }
            writableDatabase.insert(TABLE, null, values)
        } catch (t: Throwable) {
            Log.e(TAG, "SensorDatabase.record failed for $entityId", t)
        }
    }

    /** Readings newer than [sinceMs], oldest first. */
    fun history(entityId: String, sinceMs: Long): List<HistoryPoint> {
        val points = mutableListOf<HistoryPoint>()
        try {
            readableDatabase.rawQuery(
                "SELECT ts, value FROM $TABLE WHERE entity_id = ? AND ts >= ? ORDER BY ts ASC",
                arrayOf(entityId, sinceMs.toString()),
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    points += HistoryPoint(cursor.getLong(0), cursor.getDouble(1))
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "SensorDatabase.history failed for $entityId", t)
        }
        return points
    }

    /** Drop readings older than [days] so the file cannot grow forever. */
    fun prune(days: Int = 30) {
        val cutoff = System.currentTimeMillis() - days * 86_400_000L
        try {
            writableDatabase.delete(TABLE, "ts < ?", arrayOf(cutoff.toString()))
        } catch (t: Throwable) {
            Log.e(TAG, "SensorDatabase.prune failed", t)
        }
    }
}
