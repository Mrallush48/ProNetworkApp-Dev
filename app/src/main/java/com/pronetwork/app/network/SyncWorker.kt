package com.pronetwork.app.network

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Background worker that runs sync periodically and on-demand.
 * Uses WorkManager for battery-efficient background execution.
 */
class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "SyncWorker"
        private const val PERIODIC_WORK_NAME = "sync_periodic"
        private const val ONE_TIME_WORK_NAME = "sync_immediate"

        /**
         * Schedule periodic sync every 15 minutes (minimum allowed by WorkManager).
         * Only runs when network is available.
         */
        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    1, TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )

            Log.i(TAG, "Periodic sync scheduled (every 15 min)")
        }

        /**
         * Trigger an immediate one-time sync.
         * Used when connectivity is restored or after local changes.
         */
        fun syncNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30, TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )

            Log.i(TAG, "Immediate sync triggered")
        }

        /**
         * Cancel all sync work (used on logout).
         */
        fun cancelAll(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
            WorkManager.getInstance(context).cancelUniqueWork(ONE_TIME_WORK_NAME)
            Log.i(TAG, "All sync work cancelled")
        }
    }

    override suspend fun doWork(): Result {
        Log.i(TAG, "SyncWorker started (attempt: $runAttemptCount)")

        val authManager = AuthManager(applicationContext)
        val token = authManager.getAccessToken()

        if (token.isNullOrBlank()) {
            Log.w(TAG, "No auth token — skipping sync")
            return Result.success()
        }

        return try {
            val syncEngine = SyncEngine(applicationContext)
            val success = syncEngine.sync(token)

            if (success) {
                Log.i(TAG, "SyncWorker completed successfully")
                Result.success()
            } else {
                Log.w(TAG, "SyncWorker completed with errors — will retry")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "SyncWorker failed: ${e.message}")
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
