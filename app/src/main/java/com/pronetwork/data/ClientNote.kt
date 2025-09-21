package com.pronetwork.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

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
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clientId: Int,
    val note: String,
    val timestamp: Long = System.currentTimeMillis(),
    val month: String // Format: yyyy-MM, the month when the note was added
)