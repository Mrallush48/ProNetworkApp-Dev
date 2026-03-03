package com.pronetwork.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pronetwork.util.generateId

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey
    val id: String = generateId(),
    val name: String,
    val subscriptionNumber: String,
    val roomNumber: String? = null,
    val mobile: String? = null,
    val price: Double,
    val firstMonthAmount: Double? = null,
    val buildingId: String,
    val startMonth: String,
    val startDay: Int = 1,
    val endMonth: String? = null,
    val isPaid: Boolean = false,
    val paymentDate: Long? = null,
    val phone: String = "",
    val address: String = "",
    val packageType: String = "5Mbps",
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Int = 1,
    val checksum: String = ""
) {
    /**
     * Computes HMAC-SHA256 checksum for financial data integrity.
     * Includes only fields that affect financial calculations.
     */
    fun computeChecksum(secretKey: ByteArray): String {
        val data = "$id|$buildingId|$name|$price|${firstMonthAmount ?: "null"}"
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(javax.crypto.spec.SecretKeySpec(secretKey, "HmacSHA256"))
        return mac.doFinal(data.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
