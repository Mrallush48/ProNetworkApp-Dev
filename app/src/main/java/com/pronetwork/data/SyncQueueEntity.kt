package com.pronetwork.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pronetwork.util.generateId

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val payload: String,
    val idempotencyKey: String = generateId(),
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val status: String = "pending"
)
