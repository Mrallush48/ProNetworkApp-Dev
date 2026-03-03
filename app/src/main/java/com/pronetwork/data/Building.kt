package com.pronetwork.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pronetwork.util.generateId

@Entity(tableName = "buildings")
data class Building(
    @PrimaryKey
    val id: String = generateId(),
    val name: String,
    val location: String = "",
    val notes: String = "",
    val floors: Int = 0,
    val managerName: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Int = 1
)
