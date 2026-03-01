package com.pronetwork.app.viewmodel

// ─────────────────────────────────────────────────────────────────────────────
// Payment UI Models — نماذج البيانات المشتركة بين ViewModel والشاشات
// ─────────────────────────────────────────────────────────────────────────────

/**
 * بيانات عرض حالة الدفع لشهر واحد لعميل معيّن.
 * تُستخدم في شاشة تفاصيل العميل [ClientDetailsScreen].
 */
data class ClientMonthPaymentUi(
    val month: String,
    val monthAmount: Double,
    val totalPaid: Double,
    val remaining: Double,
    val status: PaymentStatus
)

/**
 * إحصائيات شهرية عامة — تُستخدم في شاشة الإحصائيات.
 */
data class MonthStats(
    val month: String,
    val paidCount: Int,
    val partiallyPaidCount: Int,
    val settledCount: Int,
    val unpaidCount: Int,
    val totalPaidAmount: Double,
    val totalUnpaidAmount: Double,
    val settledAmount: Double
)

/**
 * عدد العملاء حسب حالة الدفع — لشاشة Daily Collection.
 */
data class GlobalPaymentStatusCounts(
    val paidCount: Int = 0,
    val partialCount: Int = 0,
    val settledCount: Int = 0,
    val unpaidCount: Int = 0
)

// ─────────────────────────────────────────────────────────────────────────────
// Performance Evaluation — تقييم الأداء اليومي
// ─────────────────────────────────────────────────────────────────────────────

enum class DailyPerformanceLevel {
    EXCELLENT, GOOD, POOR
}

fun getDailyPerformance(totalAmount: Double): DailyPerformanceLevel = when {
    totalAmount >= 2000.0 -> DailyPerformanceLevel.EXCELLENT
    totalAmount >= 1000.0 -> DailyPerformanceLevel.GOOD
    else -> DailyPerformanceLevel.POOR
}
