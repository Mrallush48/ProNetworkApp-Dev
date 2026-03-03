package com.pronetwork.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pronetwork.util.generateId

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey
    val id: String = generateId(),
    val clientId: String,
    val month: String,
    val isPaid: Boolean = false,
    val paymentDate: Long? = null,
    val amount: Double = 0.0,
    val notes: String = "",
    val lastModifiedBy: Int? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Int = 1,
    val checksum: String = ""
) {
    /**
     * Computes HMAC-SHA256 checksum for financial data integrity.
     * Covers all fields that affect financial state.
     */
    fun computeChecksum(secretKey: ByteArray): String {
        val data = "$id|$clientId|$month|$isPaid|$amount|${paymentDate ?: "null"}"
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(javax.crypto.spec.SecretKeySpec(secretKey, "HmacSHA256"))
        return mac.doFinal(data.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
