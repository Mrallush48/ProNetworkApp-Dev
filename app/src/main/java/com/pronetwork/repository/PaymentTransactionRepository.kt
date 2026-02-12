package com.pronetwork.app.repository

import androidx.lifecycle.LiveData
import com.pronetwork.app.data.PaymentTransaction
import com.pronetwork.app.data.PaymentTransactionDao
import com.pronetwork.app.data.DailyBuildingCollection

class PaymentTransactionRepository(
    private val transactionDao: PaymentTransactionDao
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
}
