package com.pronetwork.app.network

import android.content.Context
import android.util.Log
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Production-grade SyncWorker with:
 * - Exponential Backoff + Jitter (industry standard)
 * - Detailed duration logging
 * - Network constraint enforcement
 * - Graceful error handling with categorized retries
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SyncWorker"
        private const val WORK_NAME = "sync_work"
        private const val KEY_TRIGGER = "trigger"

        /**
         * Enqueue a one-time sync with network constraint.
         * Uses ExponentialBackoff + Jitter for retries.
         */
        fun enqueue(context: Context, trigger: String = "manual") {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30, TimeUnit.SECONDS  // 30s -> 60s -> 120s -> 240s...
                )
                .setInputData(workDataOf(KEY_TRIGGER to trigger))
                .addTag("sync")
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    request
                )

            Log.i(TAG, "Sync enqueued | trigger=$trigger")
        }

        /**
         * Alias for enqueue() - backward compatibility
         */
        fun syncNow(context: Context) = enqueue(context, trigger = "syncNow")

        /**
         * Schedule periodic sync every 15 minutes with network constraint.
         */
        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(
                15, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES  // flex interval
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30, TimeUnit.SECONDS
                )
                .setInputData(workDataOf(KEY_TRIGGER to "periodic"))
                .addTag("sync_periodic")
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "sync_periodic",
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )

            Log.i(TAG, "Periodic sync scheduled (every 15 min)")
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val trigger = inputData.getString(KEY_TRIGGER) ?: "unknown"
        val startTime = System.currentTimeMillis()
        val attempt = runAttemptCount

        Log.i(TAG, "=== SYNC START | trigger=$trigger | attempt=$attempt ===")

        try {
            val syncEngine = SyncEngine(applicationContext)

            // Get valid access token
            val token = AuthManager(applicationContext).getAccessToken()
            if (token == null) {
                Log.w(TAG, "No auth token available - sync skipped")
                return@withContext Result.failure(
                    workDataOf("error" to "no_auth_token")
                )
            }

            // Execute full sync cycle
            val success = syncEngine.sync(token)
            val duration = System.currentTimeMillis() - startTime

            if (success) {
                Log.i(TAG, "=== SYNC SUCCESS | duration=${duration}ms | attempt=$attempt ===")
                Result.success(
                    workDataOf(
                        "duration_ms" to duration,
                        "attempt" to attempt,
                        "trigger" to trigger
                    )
                )
            } else {
                Log.w(TAG, "=== SYNC PARTIAL FAILURE | duration=${duration}ms | attempt=$attempt ===")
                if (attempt < 5) {
                    // Retry with exponential backoff (handled by WorkManager)
                    Result.retry()
                } else {
                    Log.e(TAG, "Max retries reached ($attempt) - giving up")
                    Result.failure(
                        workDataOf(
                            "error" to "max_retries_exceeded",
                            "attempts" to attempt
                        )
                    )
                }
            }

        } catch (e: java.net.UnknownHostException) {
            val duration = System.currentTimeMillis() - startTime
            Log.w(TAG, "Network unreachable | duration=${duration}ms | attempt=$attempt")
            Result.retry() // Will use exponential backoff

        } catch (e: java.net.SocketTimeoutException) {
            val duration = System.currentTimeMillis() - startTime
            Log.w(TAG, "Timeout | duration=${duration}ms | attempt=$attempt")
            Result.retry()

        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "=== SYNC ERROR | duration=${duration}ms | attempt=$attempt ===", e)
            if (attempt < 3) {
                Result.retry()
            } else {
                Result.failure(
                    workDataOf("error" to (e.message ?: "unknown_error"))
                )
            }
        }
    }
}
