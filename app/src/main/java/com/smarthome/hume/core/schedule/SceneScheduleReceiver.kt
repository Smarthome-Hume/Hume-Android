package com.smarthome.hume.core.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.smarthome.hume.HumeApplication
import com.smarthome.hume.core.scene.LocalSceneStore

private const val TAG = "HumeHA"

/**
 * Fires a scheduled local scene, then arms the next occurrence.
 *
 * The SwiftUI build does this from a background task; on Android the alarm is
 * one-shot, so every run has to schedule the following one. BOOT_COMPLETED is
 * handled too because Android drops all alarms on reboot.
 */
class SceneScheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val store = SceneScheduleStore.get(context)

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.i(TAG, "Boot completed, re-arming scene schedules")
            store.rescheduleAll()
            return
        }
        if (intent.action != ACTION_RUN) return

        val scheduleId = intent.getStringExtra(EXTRA_SCHEDULE_ID) ?: return
        val schedule = store.schedules.value.firstOrNull { it.id == scheduleId }
        if (schedule == null || !schedule.enabled) {
            Log.w(TAG, "Schedule $scheduleId fired but is gone or disabled")
            return
        }

        val app = context.applicationContext as? HumeApplication
        if (app == null) {
            Log.e(TAG, "Schedule $scheduleId fired without HumeApplication context")
            return
        }

        val sceneStore = LocalSceneStore.get(context)
        val scene = sceneStore.scenes.value.firstOrNull { it.id == schedule.sceneId }
        if (scene == null) {
            Log.w(TAG, "Schedule $scheduleId points at a deleted scene")
        } else if (scene.isActive) {
            Log.i(TAG, "Scene ${scene.name} already active, schedule $scheduleId skipped")
        } else {
            Log.i(TAG, "Running scene ${scene.name} from schedule $scheduleId")
            sceneStore.activate(scene.id, app.haRepository)
        }

        // One-shot alarm: arm the next occurrence.
        store.rescheduleAll()
    }

    companion object {
        const val ACTION_RUN = "com.smarthome.hume.action.RUN_SCENE_SCHEDULE"
        const val EXTRA_SCHEDULE_ID = "schedule_id"
    }
}
