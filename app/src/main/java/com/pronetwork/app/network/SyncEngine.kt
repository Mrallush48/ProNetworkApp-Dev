package com.pronetwork.app.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.pronetwork.app.data.ClientDatabase
import com.pronetwork.app.data.SyncQueueEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Core sync engine that handles push (upload local changes) and pull (download server changes).
 * Uses SyncQueue for offline operations and delta sync via lastSyncTimestamp.
 */
class SyncEngine(private val context: Context) {

    companion object {
        private const val TAG = "SyncEngine"
        private const val PREFS_NAME = "sync_prefs"
        private const val KEY_LAST_SYNC = "last_sync_timestamp"
        private const val MAX_RETRIES = 5
        private const val BATCH_SIZE = 50
    }

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

    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val db = ClientDatabase.getDatabase(context)
    private val syncQueueDao = db.syncQueueDao()
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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
     * Full sync cycle: Push local changes → Pull server changes.
     * Returns true if sync completed successfully.
     */
    suspend fun sync(token: String): Boolean {
        Log.i(TAG, "=== SYNC STARTED ===")

        // Step 1: Push local changes to server
        val pushSuccess = push(token)

        // Step 2: Pull server changes (even if push had partial failures)
        val pullSuccess = pull(token)

        val success = pushSuccess && pullSuccess

        _syncState.value = _syncState.value.copy(
            status = if (success) SyncStatus.SUCCESS else SyncStatus.ERROR,
            lastSyncTime = if (success) utcFormat.format(System.currentTimeMillis()) else _syncState.value.lastSyncTime
        )

        Log.i(TAG, "=== SYNC ${if (success) "SUCCESS" else "PARTIAL FAILURE"} ===")
        return success
    }

    /**
     * Push: Upload all pending local operations to the server.
     */
    private suspend fun push(token: String): Boolean {
        _syncState.value = _syncState.value.copy(status = SyncStatus.PUSHING)

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

                    // Remove successfully synced operations
                    batch.forEach { entry ->
                        syncQueueDao.remove(entry.id)
                    }
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
     */
    private suspend fun applyServerChanges(data: SyncPullResponse) {
        val gson = com.google.gson.Gson()

        // === Clients ===
        data.clients?.forEach { entity ->
            try {
                when (entity.action.uppercase()) {
                    "CREATE", "UPDATE" -> {
                        entity.data?.let { map ->
                            val json = gson.toJson(map)
                            val client = gson.fromJson(json, com.pronetwork.app.data.Client::class.java)
                            db.clientDao().insert(client) // REPLACE strategy handles upsert
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
