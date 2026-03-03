package com.pronetwork.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import com.pronetwork.util.generateId

@Entity(
    tableName = "client_notes",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("clientId"),
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ClientNote(
    @PrimaryKey
    val id: String = generateId(),
    val clientId: String,
    val note: String,
    val timestamp: Long = System.currentTimeMillis(),
    val month: String
)
