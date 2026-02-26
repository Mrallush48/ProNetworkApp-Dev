package com.pronetwork.app.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.pronetwork.app.data.ClientDatabase
import com.pronetwork.app.data.SyncQueueDao
import com.pronetwork.app.data.SyncQueueEntity
import com.google.gson.GsonBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Core sync engine that handles push (upload local changes) and pull (download server changes).
 * Uses SyncQueue for offline operations and delta sync via lastSyncTimestamp.
 */
@Singleton
class SyncEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: ClientDatabase,
    private val syncQueueDao: SyncQueueDao,
    private val authManager: AuthManager
) {

    companion object {
        private const val TAG = "SyncEngine"
        private const val PREFS_NAME = "sync_prefs"
        private const val KEY_LAST_SYNC = "last_sync_timestamp"
        private const val MAX_RETRIES = 5
        private const val BATCH_SIZE = 50
    }

    /** Track last HTTP error code for 401 auto-retry logic */
    @Volatile
    private var lastHttpCode: Int = 0

    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    enum class SyncStatus {
        IDLE,       // No sync in progress
        PUSHING,    // Uploading local changes
        PULLING,    // Downloading server changes
        SUCCESS,    // Last sync completed successfully
        ERROR       // Last sync failed
    }

    data class SyncState(
        val status: SyncStatus = SyncStatus.IDLE,
        val pendingCount: Int = 0,
        val lastSyncTime: String? = null,
        val errorMessage: String? = null
    )
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Gson that ignores unknown fields (server_id, _checksum, etc.)
    private val gson = GsonBuilder()
        .setLenient()
        .create()

    private val utcFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Enqueue a local operation for later sync.
     * Called whenever the app creates/updates/deletes an entity.
     */
    suspend fun enqueue(entityType: String, entityId: Int, action: String, payload: String) {
        val entry = SyncQueueEntity(
            entityType = entityType,
            entityId = entityId,
            action = action,
            payload = payload,
            createdAt = utcFormat.format(System.currentTimeMillis())
        )
        syncQueueDao.enqueue(entry)
        updatePendingCount()
        Log.d(TAG, "Enqueued: $action $entityType #$entityId")
    }

    /**
     * Full sync cycle with automatic token management.
     * Preferred entry point — gets valid token automatically.
     */
    suspend fun sync(): Boolean {
        val token = authManager.getValidAccessToken()
        if (token == null) {
            Log.w(TAG, "No valid token available — sync skipped")
            _syncState.value = _syncState.value.copy(
                status = SyncStatus.ERROR,
                errorMessage = "Not authenticated"
            )
            return false
        }
        return syncWithToken(token)
    }

    /**
     * Full sync cycle with explicit token (used by SyncWorker/ProNetworkApp).
     * Includes auto-retry with token refresh on 401.
     */
    suspend fun sync(token: String): Boolean {
        return syncWithToken(token)
    }

    /**
     * Internal sync implementation with 401 auto-retry.
     * On first 401 → refreshes token → retries once.
     */
    private suspend fun syncWithToken(token: String): Boolean {
        Log.i(TAG, "=== SYNC STARTED ===")

        // Step 1: Push local changes to server
        var currentToken = token
        var pushSuccess = push(currentToken)

        // If push got 401 → refresh token and retry
        if (!pushSuccess && lastHttpCode == 401) {
            Log.i(TAG, "Push got 401 — refreshing token and retrying")
            val newToken = authManager.refreshAccessToken()
            if (newToken != null) {
                currentToken = newToken
                pushSuccess = push(currentToken)
            } else {
                Log.e(TAG, "Token refresh failed — cannot retry push")
            }
        }

        // Step 2: Pull server changes (even if push had partial failures)
        var pullSuccess = pull(currentToken)

        // If pull got 401 → refresh token and retry
        if (!pullSuccess && lastHttpCode == 401) {
            Log.i(TAG, "Pull got 401 — refreshing token and retrying")
            val newToken = authManager.refreshAccessToken()
            if (newToken != null) {
                currentToken = newToken
                pullSuccess = pull(currentToken)
            } else {
                Log.e(TAG, "Token refresh failed — cannot retry pull")
            }
        }

        val success = pushSuccess && pullSuccess
        _syncState.value = _syncState.value.copy(
            status = if (success) SyncStatus.SUCCESS else SyncStatus.ERROR,
            lastSyncTime = if (success) utcFormat.format(System.currentTimeMillis())
            else _syncState.value.lastSyncTime,
            errorMessage = if (success) null else _syncState.value.errorMessage
        )

        Log.i(TAG, "=== SYNC ${if (success) "SUCCESS" else "PARTIAL FAILURE"} ===")
        return success
    }


    /**
     * Push: Upload all pending local operations to the server.
     * Processes acknowledgments to remove successfully synced items.
     */
    private suspend fun push(token: String): Boolean {
        _syncState.value = _syncState.value.copy(status = SyncStatus.PUSHING)
        lastHttpCode = 0
        val pending = syncQueueDao.getPendingWithRetryLimit(MAX_RETRIES)
        if (pending.isEmpty()) {
            Log.d(TAG, "Push: Nothing to push")
            return true
        }

        Log.d(TAG, "Push: ${pending.size} operations pending")
        var allSuccess = true

        // Process in batches
        pending.chunked(BATCH_SIZE).forEach { batch ->
            try {
                val operations = batch.map { entry ->
                    SyncOperation(
                        entity_type = entry.entityType,
                        entity_id = entry.entityId,
                        action = entry.action,
                        payload = entry.payload,
                        client_timestamp = entry.createdAt
                    )
                }

                val response = ApiClient.safeCall { api ->
                    api.syncPush("Bearer $token", SyncPushRequest(operations))
                }

                if (response.isSuccessful) {
                    val result = response.body()
                    Log.d(TAG, "Push batch: ${result?.processed} processed, ${result?.failed} failed")

                    // Process acknowledgments
                    result?.acknowledgments?.forEach { ack ->
                        if (ack.status == "ok") {
                            // Find and remove the matching queue entry
                            val matchingEntry = batch.find {
                                it.entityType == ack.entity_type && it.entityId == ack.local_id
                            }
                            matchingEntry?.let { entry ->
                                syncQueueDao.remove(entry.id)
                                Log.d(TAG, "Ack OK: ${ack.entity_type} local=${ack.local_id} -> server=${ack.server_id}")
                            }
                        }
                    }

                    // If no acknowledgments returned, remove all (backward compat)
                    if (result?.acknowledgments.isNullOrEmpty() && result?.failed == 0) {
                        batch.forEach { entry ->
                            syncQueueDao.remove(entry.id)
                        }
                    }

                    // Log errors if any
                    result?.errors?.forEach { error ->
                        Log.w(TAG, "Push error: $error")
                    }

                    if ((result?.failed ?: 0) > 0) {
                        allSuccess = false
                    }

                } else if (response.code() == 401) {
                    Log.w(TAG, "Push: authentication failed (401)")
                    lastHttpCode = 401
                    allSuccess = false
                    return@forEach
                } else if (response.code() == 404) {
                    Log.w(TAG, "Push: sync endpoint not available (404) — skipping")
                } else {
                    Log.w(TAG, "Push batch failed: ${response.code()}")
                    batch.forEach { entry ->
                        syncQueueDao.incrementRetry(entry.id, "HTTP ${response.code()}")
                    }
                    allSuccess = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Push batch error: ${e.message}")
                batch.forEach { entry ->
                    syncQueueDao.incrementRetry(entry.id, e.message)
                }
                allSuccess = false
            }
        }

        updatePendingCount()
        return allSuccess
    }

    /**
     * Pull: Download changes from server since last sync.
     * Uses delta sync — only fetches what changed.
     */
    private suspend fun pull(token: String): Boolean {
        _syncState.value = _syncState.value.copy(status = SyncStatus.PULLING)
        lastHttpCode = 0
        val lastSync = prefs.getString(KEY_LAST_SYNC, null)
        Log.d(TAG, "Pull: since=$lastSync")

        return try {
            val response = ApiClient.safeCall { api ->
                api.syncPull("Bearer $token", lastSync)
            }

            if (response.isSuccessful) {
                val pullData = response.body()
                if (pullData != null) {
                    applyServerChanges(pullData)

                    // Save server timestamp for next delta sync
                    prefs.edit()
                        .putString(KEY_LAST_SYNC, pullData.server_timestamp)
                        .apply()

                    Log.d(TAG, "Pull: applied changes, next sync from ${pullData.server_timestamp}")
                }
                true
            } else if (response.code() == 404) {
                Log.w(TAG, "Pull: sync endpoint not available (404) — skipping")
                true
            } else if (response.code() == 401) {
                Log.w(TAG, "Pull: authentication failed (401)")
                lastHttpCode = 401
                _syncState.value = _syncState.value.copy(
                    errorMessage = "Authentication expired"
                )
                false
            } else {
                Log.w(TAG, "Pull failed: ${response.code()}")
                _syncState.value = _syncState.value.copy(
                    errorMessage = "Pull failed: HTTP ${response.code()}"
                )
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Pull error: ${e.message}")
            _syncState.value = _syncState.value.copy(errorMessage = e.message)
            false
        }
    }

    /**
     * Apply changes received from the server to the local Room database.
     * Server wins in case of conflicts (Last-Write-Wins strategy).
     * Gson ignores unknown fields (server_id, _checksum) automatically.
     */
    private suspend fun applyServerChanges(data: SyncPullResponse) {

        // === Clients ===
        data.clients?.forEach { entity ->
            try {
                when (entity.action.uppercase()) {
                    "CREATE", "UPDATE" -> {
                        entity.data?.let { map ->
                            val json = gson.toJson(map)
                            val client = gson.fromJson(json, com.pronetwork.app.data.Client::class.java)
                            db.clientDao().insert(client)
                            Log.d(TAG, "Applied ${entity.action} client #${entity.id}")
                        }
                    }
                    "DELETE" -> {
                        val existing = db.clientDao().getClientById(entity.id)
                        if (existing != null) {
                            db.clientDao().delete(existing)
                            Log.d(TAG, "Applied DELETE client #${entity.id}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply client #${entity.id}: ${e.message}")
            }
        }

        // === Buildings ===
        data.buildings?.forEach { entity ->
            try {
                when (entity.action.uppercase()) {
                    "CREATE", "UPDATE" -> {
                        entity.data?.let { map ->
                            val json = gson.toJson(map)
                            val building = gson.fromJson(json, com.pronetwork.app.data.Building::class.java)
                            db.buildingDao().insert(building)
                            Log.d(TAG, "Applied ${entity.action} building #${entity.id}")
                        }
                    }
                    "DELETE" -> {
                        val existing = db.buildingDao().getBuildingById(entity.id)
                        if (existing != null) {
                            db.buildingDao().delete(existing)
                            Log.d(TAG, "Applied DELETE building #${entity.id}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply building #${entity.id}: ${e.message}")
            }
        }

        // === Payments ===
        data.payments?.forEach { entity ->
            try {
                when (entity.action.uppercase()) {
                    "CREATE", "UPDATE" -> {
                        entity.data?.let { map ->
                            val json = gson.toJson(map)
                            val payment = gson.fromJson(json, com.pronetwork.app.data.Payment::class.java)
                            db.paymentDao().insert(payment)
                            Log.d(TAG, "Applied ${entity.action} payment #${entity.id}")
                        }
                    }
                    "DELETE" -> {
                        val existing = db.paymentDao().getPaymentById(entity.id)
                        if (existing != null) {
                            db.paymentDao().delete(existing)
                            Log.d(TAG, "Applied DELETE payment #${entity.id}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply payment #${entity.id}: ${e.message}")
            }
        }

        // === Payment Transactions ===
        data.payment_transactions?.forEach { entity ->
            try {
                when (entity.action.uppercase()) {
                    "CREATE", "UPDATE" -> {
                        entity.data?.let { map ->
                            val json = gson.toJson(map)
                            val transaction = gson.fromJson(json, com.pronetwork.app.data.PaymentTransaction::class.java)
                            db.paymentTransactionDao().insert(transaction)
                            Log.d(TAG, "Applied ${entity.action} transaction #${entity.id}")
                        }
                    }
                    "DELETE" -> {
                        db.paymentTransactionDao().deleteTransactionById(entity.id)
                        Log.d(TAG, "Applied DELETE transaction #${entity.id}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply transaction #${entity.id}: ${e.message}")
            }
        }

        val clientCount = data.clients?.size ?: 0
        val buildingCount = data.buildings?.size ?: 0
        val paymentCount = data.payments?.size ?: 0
        val transactionCount = data.payment_transactions?.size ?: 0
        Log.i(TAG, "Server changes applied: clients=$clientCount, buildings=$buildingCount, " +
                "payments=$paymentCount, transactions=$transactionCount")
    }

    /**
     * Update the pending count in the sync state.
     */
    private suspend fun updatePendingCount() {
        val count = syncQueueDao.getPendingCount()
        _syncState.value = _syncState.value.copy(pendingCount = count)
    }

    /**
     * Reset sync state (used after logout or full re-sync).
     */
    suspend fun reset() {
        syncQueueDao.clearAll()
        prefs.edit().remove(KEY_LAST_SYNC).apply()
        _syncState.value = SyncState()
        Log.i(TAG, "Sync state reset")
    }

    /**
     * Get last sync timestamp.
     */
    fun getLastSyncTimestamp(): String? {
        return prefs.getString(KEY_LAST_SYNC, null)
    }
}