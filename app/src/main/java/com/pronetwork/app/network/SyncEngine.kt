package com.pronetwork.app.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.pronetwork.app.data.ClientDatabase
import com.pronetwork.app.data.SyncQueueDao
import com.pronetwork.app.data.SyncQueueEntity
import com.pronetwork.util.generateId
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
        IDLE,
        PUSHING,
        PULLING,
        SUCCESS,
        ERROR
    }

    data class SyncState(
        val status: SyncStatus = SyncStatus.IDLE,
        val pendingCount: Int = 0,
        val lastSyncTime: String? = null,
        val errorMessage: String? = null
    )

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    private val utcFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Enqueue a local operation for later sync.
     * Validates entityId is not empty — rejects invalid operations.
     * Generates idempotencyKey to prevent duplicate processing on server.
     */
    suspend fun enqueue(entityType: String, entityId: String, action: String, payload: String) {
        if (entityId.isBlank()) {
            Log.e(TAG, "REJECTED: enqueue with blank entityId for $action $entityType")
            return
        }

        val entry = SyncQueueEntity(
            entityType = entityType,
            entityId = entityId,
            operation = action,
            payload = payload,
            idempotencyKey = generateId(),
            createdAt = System.currentTimeMillis()
        )
        syncQueueDao.enqueue(entry)
        updatePendingCount()
        Log.d(TAG, "Enqueued: $action $entityType #$entityId")
    }

    /**
     * Full sync cycle with automatic token management.
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
     */
    suspend fun sync(token: String): Boolean {
        return syncWithToken(token)
    }

    /**
     * Internal sync implementation with 401 auto-retry.
     */
    private suspend fun syncWithToken(token: String): Boolean {
        Log.i(TAG, "=== SYNC STARTED ===")

        var currentToken = token
        var pushSuccess = push(currentToken)

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

        var pullSuccess = pull(currentToken)

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
            lastSyncTime = if (success) utcFormat.format(System.currentTimeMillis()) else _syncState.value.lastSyncTime,
            errorMessage = if (success) null else _syncState.value.errorMessage
        )
        Log.i(TAG, "=== SYNC ${if (success) "SUCCESS" else "PARTIAL FAILURE"} ===")
        return success
    }

    /**
     * Smart Queue Compaction — reduces redundant operations per entity.
     *
     * Rules:
     * - CREATE → DELETE = null (cancel both — entity never existed on server)
     * - CREATE → UPDATE(s) = single CREATE with latest payload
     * - Multiple UPDATEs = single UPDATE with latest payload
     * - DELETE after anything = DELETE only
     */
    private fun compactQueue(entries: List<SyncQueueEntity>): List<SyncQueueEntity> {
        val grouped = entries.groupBy { "${it.entityType}:${it.entityId}" }
        return grouped.mapNotNull { (_, ops) ->
            val sorted = ops.sortedBy { it.createdAt }
            val first = sorted.first()
            val last = sorted.last()

            when {
                // CREATE followed by DELETE → cancel both
                first.operation == "CREATE" && last.operation == "DELETE" -> null

                // CREATE followed by UPDATE(s) → single CREATE with latest payload
                first.operation == "CREATE" && last.operation == "UPDATE" ->
                    first.copy(payload = last.payload, createdAt = last.createdAt)

                // Any sequence ending with DELETE → just DELETE
                last.operation == "DELETE" -> last

                // Multiple UPDATEs or single operation → keep latest
                else -> last
            }
        }
    }

    /**
     * Push: Upload all pending local operations to the server.
     */
    private suspend fun push(token: String): Boolean {
        _syncState.value = _syncState.value.copy(status = SyncStatus.PUSHING)
        lastHttpCode = 0

        val pending = syncQueueDao.getPendingWithRetryLimit(MAX_RETRIES)
        if (pending.isEmpty()) {
            Log.d(TAG, "Push: Nothing to push")
            return true
        }

        val compacted = compactQueue(pending)
        Log.d(TAG, "Push: ${pending.size} pending, ${compacted.size} after compaction")

        // Remove cancelled operations (CREATE→DELETE = null)
        val cancelledEntityKeys = pending
            .map { "${it.entityType}:${it.entityId}" }
            .toSet()
            .minus(compacted.map { "${it.entityType}:${it.entityId}" }.toSet())

        cancelledEntityKeys.forEach { key ->
            val (type, id) = key.split(":", limit = 2)
            pending.filter { it.entityType == type && it.entityId == id }
                .forEach { syncQueueDao.remove(it.id) }
            Log.d(TAG, "Compaction: cancelled $key (CREATE→DELETE)")
        }

        if (compacted.isEmpty()) {
            Log.d(TAG, "Push: All operations cancelled by compaction")
            updatePendingCount()
            return true
        }

        var allSuccess = true

        compacted.chunked(BATCH_SIZE).forEach { batch ->
            try {
                val operations = batch.map { entry ->
                    SyncOperation(
                        entity_type = entry.entityType,
                        entity_id = entry.entityId,
                        action = entry.operation,
                        payload = entry.payload,
                        client_timestamp = utcFormat.format(entry.createdAt),
                        idempotency_key = entry.idempotencyKey
                    )
                }

                val response = ApiClient.safeCall { api ->
                    api.syncPush("Bearer $token", SyncPushRequest(operations))
                }

                if (response.isSuccessful) {
                    val result = response.body()
                    Log.d(TAG, "Push batch: ${result?.processed} processed, ${result?.failed} failed")

                    result?.acknowledgments?.forEach { ack ->
                        if (ack.status == "ok" || ack.status == "conflict_server_wins") {
                            val allOriginal = pending.filter {
                                it.entityType == ack.entity_type &&
                                        it.entityId == ack.local_id
                            }
                            allOriginal.forEach { original ->
                                syncQueueDao.remove(original.id)
                            }
                            Log.d(TAG, "Ack OK: ${ack.entity_type} id=${ack.local_id} (removed ${allOriginal.size} queue entries)")
                        }
                    }

                    if (result?.acknowledgments.isNullOrEmpty() && result?.failed == 0) {
                        batch.forEach { entry ->
                            val allOriginal = pending.filter {
                                it.entityType == entry.entityType && it.entityId == entry.entityId
                            }
                            allOriginal.forEach { original ->
                                syncQueueDao.remove(original.id)
                            }
                        }
                    }

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
                        syncQueueDao.incrementRetry(entry.id)
                    }
                    allSuccess = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Push batch error: ${e.message}")
                batch.forEach { entry ->
                    syncQueueDao.incrementRetry(entry.id)
                }
                allSuccess = false
            }
        }

        updatePendingCount()
        return allSuccess
    }

    /**
     * Pull: Download changes from server since last sync.
     * Applies changes in correct order: CREATEs/UPDATEs first (parent→child),
     * then DELETEs in reverse order (child→parent) to respect FK constraints.
     */
    private suspend fun pull(token: String): Boolean {
        _syncState.value = _syncState.value.copy(status = SyncStatus.PULLING)
        lastHttpCode = 0

        var currentSince = prefs.getString(KEY_LAST_SYNC, null)
        var totalApplied = 0
        var pageCount = 0
        val maxPages = 50

        try {
            while (pageCount < maxPages) {
                pageCount++
                Log.d(TAG, "Pull page $pageCount: since=$currentSince")

                val response = ApiClient.safeCall { api ->
                    api.syncPull("Bearer $token", currentSince)
                }

                if (response.isSuccessful) {
                    val pullData = response.body()
                    if (pullData != null) {
                        applyServerChanges(pullData)
                        val pageTotal =
                            (pullData.clients?.size ?: 0) +
                                    (pullData.buildings?.size ?: 0) +
                                    (pullData.payments?.size ?: 0) +
                                    (pullData.payment_transactions?.size ?: 0)
                        totalApplied += pageTotal

                        prefs.edit()
                            .putString(KEY_LAST_SYNC, pullData.server_timestamp)
                            .apply()
                        currentSince = pullData.server_timestamp

                        Log.d(TAG, "Pull page $pageCount: applied $pageTotal changes, has_more=${pullData.has_more}")
                        if (!pullData.has_more) break
                    } else {
                        break
                    }
                } else if (response.code() == 404) {
                    Log.w(TAG, "Pull: sync endpoint not available (404) — skipping")
                    return true
                } else if (response.code() == 401) {
                    Log.w(TAG, "Pull: authentication failed (401)")
                    lastHttpCode = 401
                    _syncState.value = _syncState.value.copy(errorMessage = "Authentication expired")
                    return false
                } else {
                    Log.w(TAG, "Pull failed: ${response.code()}")
                    _syncState.value = _syncState.value.copy(errorMessage = "Pull failed: HTTP ${response.code()}")
                    return false
                }
            }

            Log.d(TAG, "Pull complete: $totalApplied total changes in $pageCount pages")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Pull error: ${e.message}")
            _syncState.value = _syncState.value.copy(errorMessage = e.message)
            return false
        }
    }

    /**
     * Apply changes from server with correct ordering:
     * 1. CREATEs/UPDATEs: buildings → clients → payments → transactions (parent-first)
     * 2. DELETEs: transactions → payments → clients → buildings (child-first)
     * This prevents FK constraint violations.
     */
    private suspend fun applyServerChanges(data: SyncPullResponse) {
        // === Phase 1: CREATEs and UPDATEs (parent → child) ===

        // 1. Buildings
        data.buildings?.filter { it.action.uppercase() != "DELETE" }?.forEach { entity ->
            try {
                entity.data?.let { map ->
                    val json = gson.toJson(map)
                    val building = gson.fromJson(json, com.pronetwork.app.data.Building::class.java)
                    db.buildingDao().upsert(building)
                    Log.d(TAG, "Applied ${entity.action} building #${entity.id}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply building #${entity.id}: ${e.message}")
            }
        }

        // 2. Clients
        data.clients?.filter { it.action.uppercase() != "DELETE" }?.forEach { entity ->
            try {
                entity.data?.let { map ->
                    val json = gson.toJson(map)
                    val client = gson.fromJson(json, com.pronetwork.app.data.Client::class.java)
                    db.clientDao().upsert(client)
                    Log.d(TAG, "Applied ${entity.action} client #${entity.id}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply client #${entity.id}: ${e.message}")
            }
        }

        // 3. Payments
        data.payments?.filter { it.action.uppercase() != "DELETE" }?.forEach { entity ->
            try {
                entity.data?.let { map ->
                    val json = gson.toJson(map)
                    val payment = gson.fromJson(json, com.pronetwork.app.data.Payment::class.java)
                    db.paymentDao().upsert(payment)
                    Log.d(TAG, "Applied ${entity.action} payment #${entity.id}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply payment #${entity.id}: ${e.message}")
            }
        }

        // 4. Payment Transactions
        data.payment_transactions?.filter { it.action.uppercase() != "DELETE" }?.forEach { entity ->
            try {
                entity.data?.let { map ->
                    val json = gson.toJson(map)
                    val transaction = gson.fromJson(json, com.pronetwork.app.data.PaymentTransaction::class.java)
                    db.paymentTransactionDao().upsert(transaction)
                    Log.d(TAG, "Applied ${entity.action} transaction #${entity.id}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to apply transaction #${entity.id}: ${e.message}")
            }
        }

        // === Phase 2: DELETEs (child → parent) ===

        // 1. Payment Transactions (child-most)
        data.payment_transactions?.filter { it.action.uppercase() == "DELETE" }?.forEach { entity ->
            try {
                db.paymentTransactionDao().deleteTransactionById(entity.id)
                Log.d(TAG, "Applied DELETE transaction #${entity.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete transaction #${entity.id}: ${e.message}")
            }
        }

        // 2. Payments
        data.payments?.filter { it.action.uppercase() == "DELETE" }?.forEach { entity ->
            try {
                val existing = db.paymentDao().getPaymentById(entity.id)
                if (existing != null) {
                    db.paymentDao().delete(existing)
                    Log.d(TAG, "Applied DELETE payment #${entity.id}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete payment #${entity.id}: ${e.message}")
            }
        }

        // 3. Clients
        data.clients?.filter { it.action.uppercase() == "DELETE" }?.forEach { entity ->
            try {
                val existing = db.clientDao().getClientById(entity.id)
                if (existing != null) {
                    db.clientDao().delete(existing)
                    Log.d(TAG, "Applied DELETE client #${entity.id}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete client #${entity.id}: ${e.message}")
            }
        }

        // 4. Buildings (parent-most)
        data.buildings?.filter { it.action.uppercase() == "DELETE" }?.forEach { entity ->
            try {
                val existing = db.buildingDao().getBuildingById(entity.id)
                if (existing != null) {
                    db.buildingDao().delete(existing)
                    Log.d(TAG, "Applied DELETE building #${entity.id}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete building #${entity.id}: ${e.message}")
            }
        }

        val clientCount = data.clients?.size ?: 0
        val buildingCount = data.buildings?.size ?: 0
        val paymentCount = data.payments?.size ?: 0
        val transactionCount = data.payment_transactions?.size ?: 0
        Log.i(TAG, "Server changes applied: clients=$clientCount, buildings=$buildingCount, " +
                "payments=$paymentCount, transactions=$transactionCount")
    }

    private suspend fun updatePendingCount() {
        val count = syncQueueDao.getPendingCount()
        _syncState.value = _syncState.value.copy(pendingCount = count)
    }

    suspend fun reset() {
        syncQueueDao.clearAll()
        prefs.edit().remove(KEY_LAST_SYNC).apply()
        _syncState.value = SyncState()
        Log.i(TAG, "Sync state reset")
    }

    fun getLastSyncTimestamp(): String? {
        return prefs.getString(KEY_LAST_SYNC, null)
    }
}
