package com.pronetwork.data

/**
 * Data class لتمثيل ملخص التحصيل اليومي
 *
 * @property totalAmount إجمالي المبلغ المحصّل في اليوم
 * @property totalClients عدد العملاء الذين دفعوا في اليوم
 * @property totalTransactions عدد حركات الدفع في اليوم
 * @property paidCount عدد العملاء المدفوع بالكامل
 * @property partialCount عدد العملاء المدفوع جزئياً
 * @property settledCount عدد العملاء المُسوَّين (دفع + استرجاع)
 * @property unpaidCount عدد العملاء غير المدفوعين
 * @property settledAmount إجمالي المبالغ المُسوَّاة
 * @property refundAmount إجمالي مبالغ الاسترجاع في اليوم
 */
data class DailySummary(
    val totalAmount: Double = 0.0,
    val totalClients: Int = 0,
    val totalTransactions: Int = 0,
    val paidCount: Int = 0,
    val partialCount: Int = 0,
    val settledCount: Int = 0,
    val unpaidCount: Int = 0,
    val settledAmount: Double = 0.0,
    val refundAmount: Double = 0.0
)
