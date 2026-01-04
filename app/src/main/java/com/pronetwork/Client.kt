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
    val price: Double,                      // السعر الشهري الكامل
    val firstMonthAmount: Double? = null,   // المبلغ الفعلي للشهر الأول (جزئي)
    val buildingId: Int,
    val startMonth: String,                 // ISO "yyyy-MM"
    val startDay: Int = 1,                  // يوم البداية في الشهر (1-31)
    val endMonth: String? = null,           // ISO "yyyy-MM" or null
    val isPaid: Boolean = false,            // قديم - للتوافق فقط
    val paymentDate: Long? = null,          // قديم - للتوافق فقط
    val phone: String = "",
    val address: String = "",
    val packageType: String = "5Mbps",
    val notes: String = ""
)
