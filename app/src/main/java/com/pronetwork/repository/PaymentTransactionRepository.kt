package com.pronetwork.app.repository

import androidx.lifecycle.LiveData
import com.pronetwork.app.data.ClientDao
import com.pronetwork.app.data.PaymentTransaction
import com.pronetwork.app.data.PaymentTransactionDao
import com.pronetwork.app.data.DailyBuildingCollection
import com.pronetwork.data.DailySummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import com.pronetwork.data.MonthlyCollectionRatio
import java.util.Calendar


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

    fun getMonthlyCollectionRatio(): Flow<MonthlyCollectionRatio> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis

        val endOfMonth = Calendar.getInstance().timeInMillis

        return combine(
            clientDao.getActiveClientsCount(),
            clientDao.getPaidClientsCount(startOfMonth, endOfMonth)
        ) { totalClients, paidClients ->
            val ratio = if (totalClients > 0) {
                (paidClients.toFloat() / totalClients.toFloat()) * 100
            } else {
                0f
            }
            MonthlyCollectionRatio(
                expectedClients = totalClients,
                paidClients = paidClients,
                collectionRatio = ratio
            )
        }
    }


}
