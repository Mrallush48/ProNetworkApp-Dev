package com.pronetwork.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payment_transactions",
    foreignKeys = [
        ForeignKey(

            entity = Payment::class,
            parentColumns = ["id"],
            childColumns = ["paymentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["paymentId"])
    ]
)
data class PaymentTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val paymentId: Int,        // يرتبط بسجل Payment (عميل + شهر)
    val amount: Double,        // قيمة الدفعة (قد تكون جزئية أو كاملة)
    val date: Long = System.currentTimeMillis(), // تاريخ الدفعة
    val notes: String = ""     // ملاحظات اختيارية (مثلاً "دفع جزئي")
)
