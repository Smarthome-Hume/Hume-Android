package com.smarthome.hume.brief

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.smarthome.hume.core.data.HumeGraph
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/**
 * Chay 6:00 hang ngay: gom so lieu -> AI soan brief -> luu cache.
 * Ngay mung 1 thi tao them brief thang cho thang truoc.
 */
class BriefWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            val graph = HumeGraph.get()
            val session = graph.authRepository.session.first()
            if (!session.isLoggedIn || session.token.isBlank()) {
                Log.w(TAG, "chua dang nhap, bo qua")
                return Result.success()
            }
            val repo = BriefRepository(
                applicationContext,
                session.localUrl,
                session.remoteUrl,
                session.token,
                graph.aiRepository,
            )
            repo.refreshAll()
            Log.i(TAG, "brief refreshed")
            Result.success()
        } catch (t: Throwable) {
            Log.e(TAG, "brief failed: ${t.message}")
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "HumeBriefWorker"
        private const val UNIQUE = "brief-6am"

        /** Hen 6:00 hang ngay (lan dau = 6:00 ke tiep), chi chay khi co mang. */
        fun schedule(context: Context) {
            val now = LocalDateTime.now()
            var next = now.toLocalDate().atTime(6, 0)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val delayMin = Duration.between(now, next).toMinutes().coerceAtLeast(1)
            val req = PeriodicWorkRequestBuilder<BriefWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delayMin, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .addTag("brief")
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE, ExistingPeriodicWorkPolicy.KEEP, req)
            Log.i(TAG, "scheduled, first run in ${delayMin}min")
        }
    }
}
