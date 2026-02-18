package com.pronetwork.app.repository

import androidx.lifecycle.LiveData
import com.pronetwork.app.data.ClientDao
import com.pronetwork.app.data.DailyBuildingCollection
import com.pronetwork.app.data.PaymentTransaction
import com.pronetwork.app.data.PaymentTransactionDao
import com.pronetwork.data.DailySummary
import kotlinx.coroutines.flow.Flow


class PaymentTransactionRepository(
    private val transactionDao: PaymentTransactionDao,
    private val clientDao: ClientDao
) {


    suspend fun insert(transaction: PaymentTransaction) {
        transactionDao.insert(transaction)
    }

    suspend fun update(transaction: PaymentTransaction) {
        transactionDao.update(transaction)
    }

    suspend fun delete(transaction: PaymentTransaction) {
        transactionDao.delete(transaction)
    }

    fun getTransactionsForPayment(paymentId: Int): LiveData<List<PaymentTransaction>> {
        return transactionDao.getTransactionsForPayment(paymentId)
    }

    suspend fun getTransactionsForPaymentList(paymentId: Int): List<PaymentTransaction> {
        return transactionDao.getTransactionsForPaymentList(paymentId)
    }

    suspend fun getTotalPaidForPayment(paymentId: Int): Double {
        return transactionDao.getTotalPaidForPayment(paymentId)
    }

    suspend fun getDailyBuildingCollectionsForDay(
        dayStartMillis: Long,
        dayEndMillis: Long
    ): List<DailyBuildingCollection> {
        return transactionDao.getDailyBuildingCollectionsForDay(dayStartMillis, dayEndMillis)
    }

    suspend fun getTotalsForPayments(paymentIds: List<Int>): Map<Int, Double> {
        if (paymentIds.isEmpty()) return emptyMap()
        val rows = transactionDao.getTotalsForPayments(paymentIds)
        return rows.associate { it.paymentId to it.totalPaid }
    }

    suspend fun deleteTransactionsForPayment(paymentId: Int) {
        transactionDao.deleteByPaymentId(paymentId)
    }

    suspend fun deleteTransactionById(transactionId: Int) {
        transactionDao.deleteTransactionById(transactionId)
    }

    suspend fun getPaymentIdByTransactionId(transactionId: Int): Int? {
        return transactionDao.getPaymentIdByTransactionId(transactionId)
    }

    /**
     * الحصول على ملخص التحصيل اليومي
     *
     * @param date التاريخ بصيغة yyyy-MM-dd (مثال: "2026-02-12")
     * @return Flow<DailySummary> يتحدث تلقائياً عند تغيير البيانات
     */
    fun getDailySummary(date: String): Flow<DailySummary> {
        return transactionDao.getDailySummary(date)
    }

    suspend fun getDetailedTransactionsForMonth(month: String): List<PaymentTransactionDao.DetailedTransaction> {
        return transactionDao.getDetailedTransactionsForMonth(month)
    }

    // ================== تحصيل يومي تفصيلي ==================
    suspend fun getDetailedDailyCollections(
        dayStartMillis: Long,
        dayEndMillis: Long
    ): List<PaymentTransactionDao.DailyDetailedTransaction> {
        return transactionDao.getDetailedDailyCollections(dayStartMillis, dayEndMillis)
    }

    /** هل يوجد حركة سالبة (Refund) لهذا الـ Payment؟ */
    suspend fun hasNegativeTransaction(paymentId: Int): Boolean {
        return transactionDao.hasNegativeTransaction(paymentId)
    }

    /** أي paymentIds فيها حركات سالبة */
    suspend fun getPaymentIdsWithRefunds(paymentIds: List<Int>): List<Int> {
        if (paymentIds.isEmpty()) return emptyList()
        return transactionDao.getPaymentIdsWithRefunds(paymentIds)
    }

    // ================== Dashboard ==================
    suspend fun getRecentTransactions(limit: Int = 10): List<PaymentTransactionDao.DashboardRecentTransaction> {
        return transactionDao.getRecentTransactions(limit)
    }

    suspend fun getTopUnpaidClientsForMonth(month: String, limit: Int = 5): List<PaymentTransactionDao.DashboardUnpaidClient> {
        return transactionDao.getTopUnpaidClientsForMonth(month, limit)
    }


}
