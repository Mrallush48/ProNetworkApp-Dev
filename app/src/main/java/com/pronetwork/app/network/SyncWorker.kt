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
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

/**
 * Background worker that runs sync periodically and on-demand.
 * Uses WorkManager for battery-efficient background execution.
 *
 * Best Practices Applied:
 * - Exponential backoff for retries
 * - Network constraint (only runs with connectivity)
 * - Battery-aware (periodic sync only when battery not low)
 * - Notification of sync results via SyncEngine state
 * - Safe token handling
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
         * Only runs when network is available and battery is not low.
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
        val startTime = System.currentTimeMillis()
        Log.i(TAG, "SyncWorker started (attempt: $runAttemptCount)")

        // 1. Check auth token
        val authManager = AuthManager(applicationContext)
        val token = authManager.getAccessToken()

        if (token.isNullOrBlank()) {
            Log.w(TAG, "No valid auth token — skipping sync")
            return Result.success()
        }

        // 2. Check if there's anything to sync
        val syncEngine = SyncEngine(applicationContext)
        val pendingCount = syncEngine.syncState.value.pendingCount

        return try {
            // 3. Execute sync
            val success = syncEngine.sync(token)
            val duration = System.currentTimeMillis() - startTime

            if (success) {
                Log.i(TAG, "SyncWorker completed successfully in ${duration}ms")
                Result.success()
            } else {
                Log.w(TAG, "SyncWorker completed with errors in ${duration}ms — will retry")
                if (runAttemptCount < 5) {
                    Result.retry()
                } else {
                    Log.e(TAG, "SyncWorker max retries reached — failing")
                    Result.failure(
                        workDataOf("error" to "Max retries reached after $runAttemptCount attempts")
                    )
                }
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "SyncWorker failed in ${duration}ms: ${e.message}")

            if (runAttemptCount < 5) {
                Result.retry()
            } else {
                Result.failure(
                    workDataOf("error" to (e.message ?: "Unknown error"))
                )
            }
        }
    }
}
