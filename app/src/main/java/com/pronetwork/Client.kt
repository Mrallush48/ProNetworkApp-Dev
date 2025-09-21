package com.pronetwork.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val subscriptionNumber: String,
    val roomNumber: String? = null,
    val mobile: String? = null,
    val price: Double,
    val buildingId: Int,
    val startMonth: String,           // ISO "yyyy-MM"
    val endMonth: String? = null,     // ISO "yyyy-MM" or null
    val isPaid: Boolean = false,
    val paymentDate: Long? = null,
    val phone: String = "",
    val address: String = "",
    val packageType: String = "5Mbps", // new field
    val notes: String = ""             // new field
)
