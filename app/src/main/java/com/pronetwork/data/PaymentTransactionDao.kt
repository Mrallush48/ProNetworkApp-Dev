package com.pronetwork.app.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface PaymentTransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: PaymentTransaction)

    @Update
    suspend fun update(transaction: PaymentTransaction)

    @Delete
    suspend fun delete(transaction: PaymentTransaction)

    @Query("SELECT * FROM payment_transactions WHERE paymentId = :paymentId ORDER BY date ASC")
    fun getTransactionsForPayment(paymentId: Int): LiveData<List<PaymentTransaction>>

    @Query("SELECT * FROM payment_transactions WHERE paymentId = :paymentId ORDER BY id ASC")
    suspend fun getTransactionsForPaymentList(paymentId: Int): List<PaymentTransaction>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM payment_transactions WHERE paymentId = :paymentId")
    suspend fun getTotalPaidForPayment(paymentId: Int): Double

    // مجموع المدفوع لكل Payment دفعة واحدة (للاستخدام في شاشات مثل ClientDetails)
    @Query(
        """
        SELECT paymentId, COALESCE(SUM(amount), 0) AS totalPaid
        FROM payment_transactions
        WHERE paymentId IN (:paymentIds)
        GROUP BY paymentId
        """
    )
    suspend fun getTotalsForPayments(paymentIds: List<Int>): List<PaymentTotal>

    @Query("DELETE FROM payment_transactions WHERE paymentId = :paymentId")
    suspend fun deleteByPaymentId(paymentId: Int)

    @Query("DELETE FROM payment_transactions WHERE id = :transactionId")
    suspend fun deleteTransactionById(transactionId: Int)

    @Query("SELECT paymentId FROM payment_transactions WHERE id = :transactionId LIMIT 1")
    suspend fun getPaymentIdByTransactionId(transactionId: Int): Int?

    data class PaymentTotal(
        val paymentId: Int,
        val totalPaid: Double
    )
}