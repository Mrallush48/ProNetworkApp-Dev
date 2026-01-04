package com.pronetwork.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE // عند حذف العميل، تُحذف كل دفعاته
        )
    ],
    indices = [
        Index(value = ["clientId", "month"], unique = true) // كل عميل له دفعة واحدة فقط لكل شهر
    ]
)
data class Payment(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val clientId: Int,              // معرّف العميل
    val month: String,               // الشهر بصيغة yyyy-MM (مثل: 2026-01)
    val isPaid: Boolean = false,     // حالة الدفع
    val paymentDate: Long? = null,   // تاريخ الدفع بالـ milliseconds (null إذا غير مدفوع)
    val amount: Double = 0.0,        // المبلغ المدفوع (من سعر العميل)
    val notes: String = "",          // ملاحظات اختيارية (مثل: "دفع متأخر", "خصم 10%")
    val createdAt: Long = System.currentTimeMillis() // تاريخ إنشاء السجل
)
