package com.pronetwork.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pronetwork.util.generateId

@Entity(tableName = "payment_transactions")
data class PaymentTransaction(
    @PrimaryKey
    val id: String = generateId(),
    val paymentId: String,
    val type: String,
    val amount: Double,
    val notes: String = "",
    val createdBy: Int? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Int = 1
)
