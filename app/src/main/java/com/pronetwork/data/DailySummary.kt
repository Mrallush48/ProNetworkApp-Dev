package com.pronetwork.data

/**
 * Data class لتمثيل ملخص التحصيل اليومي
 *
 * @property totalAmount إجمالي المبلغ المحصّل في اليوم
 * @property totalClients عدد العملاء الذين دفعوا في اليوم
 * @property totalTransactions عدد حركات الدفع في اليوم
 */
data class DailySummary(
    val totalAmount: Double = 0.0,
    val totalClients: Int = 0,
    val totalTransactions: Int = 0
)
data class MonthlyCollectionRatio(
    val expectedClients: Int,
    val paidClients: Int,
    val collectionRatio: Float
)
