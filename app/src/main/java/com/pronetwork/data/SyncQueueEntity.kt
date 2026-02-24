package com.pronetwork.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Holds pending operations that need to be synced to the server.
 * Each row = one CREATE / UPDATE / DELETE that happened offline.
 */
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Entity type: "client", "building", "payment", "payment_transaction" */
    val entityType: String,

    /** Local Room ID of the affected entity */
    val entityId: Int,

    /** Action: "CREATE", "UPDATE", "DELETE" */
    val action: String,

    /** Full JSON payload of the entity at the time of the operation */
    val payload: String,

    /** ISO-8601 UTC timestamp when the operation was queued */
    val createdAt: String,

    /** Number of times we tried and failed to sync this operation */
    val retryCount: Int = 0,

    /** Last error message if sync failed */
    val lastError: String? = null
)
