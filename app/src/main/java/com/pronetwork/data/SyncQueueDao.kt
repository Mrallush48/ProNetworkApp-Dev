package com.pronetwork.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncQueueDao {

    /** Enqueue a new operation for syncing */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(entry: SyncQueueEntity): Long

    /** Get all pending operations ordered by creation time (FIFO) */
    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC")
    suspend fun getAllPending(): List<SyncQueueEntity>

    /** Get pending operations with retry count below the max limit */
    @Query("SELECT * FROM sync_queue WHERE retryCount < :maxRetries ORDER BY createdAt ASC")
    suspend fun getPendingWithRetryLimit(maxRetries: Int = 5): List<SyncQueueEntity>

    /** Get count of pending operations */
    @Query("SELECT COUNT(*) FROM sync_queue")
    suspend fun getPendingCount(): Int

    /** Increment retry count and set last error after a failed attempt */
    @Query("UPDATE sync_queue SET retryCount = retryCount + 1, lastError = :error WHERE id = :id")
    suspend fun incrementRetry(id: Long, error: String?)

    /** Remove a successfully synced operation */
    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun remove(id: Long)

    /** Remove all operations for a specific entity (e.g. after full sync) */
    @Query("DELETE FROM sync_queue WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun removeByEntity(entityType: String, entityId: Int)

    /** Clear all pending operations (used after full reset/re-sync) */
    @Query("DELETE FROM sync_queue")
    suspend fun clearAll()
}
