package com.pronetwork.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "buildings")
data class Building(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val location: String = "",
    val notes: String = "",
    val floors: Int = 0,
    val managerName: String = ""
)
