package com.pronetwork.app.network

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

class RequestPollingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "RequestPolling"
        private const val WORK_NAME = "request_polling"
        private const val PREF_NAME = "polling_cache"
        private const val KEY_ADMIN_PENDING_IDS = "admin_pending_ids"
        private const val KEY_USER_STATUSES = "user_statuses"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<RequestPollingWorker>(
                repeatInterval = 15,
                repeatIntervalTimeUnit = TimeUnit.MINUTES,
                flexTimeInterval = 5,
                flexTimeIntervalUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )

            Log.i(TAG, "Polling scheduled: every 15 min (flex 5 min)")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.i(TAG, "Polling cancelled")
        }
    }

    override suspend fun doWork(): Result {
        val authManager = AuthManager(applicationContext)
        if (!authManager.isLoggedIn()) return Result.success()

        val token = authManager.getAccessToken() ?: return Result.success()
        val role = authManager.getRole()

        return try {
            if (role == "ADMIN") {
                pollForAdmin(token)
            } else {
                pollForUser(token)
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Poll failed: ${e.message}")
            Result.retry()
        }
    }

    private suspend fun pollForAdmin(token: String) {
        val bearerToken = "Bearer $token"
        val response = ApiClient.safeCall { api ->
            api.getRequests(token = bearerToken, statusFilter = "PENDING")
        }
        if (!response.isSuccessful) return

        val requests: List<ApprovalRequestResponse> = response.body() ?: emptyList()
        val currentIds: Set<Int> = requests.map { it.id }.toSet()
        val previousIds = getSavedAdminIds()

        if (previousIds.isNotEmpty()) {
            val newIds = currentIds - previousIds
            if (newIds.isNotEmpty()) {
                val latestName = requests.firstOrNull { it.id in newIds }?.target_name
                NotificationHelper.notifyAdminNewRequests(
                    applicationContext,
                    newIds.size,
                    latestName
                )
            }
        }

        saveAdminIds(currentIds)
    }

    private suspend fun pollForUser(token: String) {
        val bearerToken = "Bearer $token"
        val response = ApiClient.safeCall { api ->
            api.getMyRequests(token = bearerToken)
        }
        if (!response.isSuccessful) return

        val requests: List<ApprovalRequestResponse> = response.body() ?: emptyList()
        val currentStatuses: Map<Int, String> = requests.associate { it.id to it.status }
        val previousStatuses = getSavedUserStatuses()

        if (previousStatuses.isNotEmpty()) {
            for ((id, newStatus) in currentStatuses) {
                val oldStatus = previousStatuses[id]
                if (oldStatus == "PENDING" && newStatus != "PENDING") {
                    val request = requests.firstOrNull { it.id == id }
                    NotificationHelper.notifyUserStatusChanged(
                        applicationContext,
                        request?.target_name,
                        newStatus
                    )
                }
            }
        }

        saveUserStatuses(currentStatuses)
    }

    // --- SharedPreferences cache ---

    private fun getSavedAdminIds(): Set<Int> {
        val prefs = applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_ADMIN_PENDING_IDS, emptySet())
            ?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    }

    private fun saveAdminIds(ids: Set<Int>) {
        applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_ADMIN_PENDING_IDS, ids.map { it.toString() }.toSet())
            .apply()
    }

    private fun getSavedUserStatuses(): Map<Int, String> {
        val prefs = applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getStringSet(KEY_USER_STATUSES, emptySet()) ?: emptySet()
        return raw.mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) parts[0].toIntOrNull()?.let { id -> id to parts[1] } else null
        }.toMap()
    }

    private fun saveUserStatuses(statuses: Map<Int, String>) {
        val set = statuses.map { "${it.key}:${it.value}" }.toSet()
        applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_USER_STATUSES, set)
            .apply()
    }
}
