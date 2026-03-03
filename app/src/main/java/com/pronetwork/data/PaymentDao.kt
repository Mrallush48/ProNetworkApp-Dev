package com.pronetwork.app.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import androidx.room.Upsert

@Dao
interface PaymentDao {

    @Query("SELECT * FROM payments WHERE clientId = :clientId")
    suspend fun getClientPaymentsDirect(clientId: String): List<Payment>

    @Query("SELECT * FROM payments WHERE clientId = :clientId AND month >= :fromMonth AND isPaid = 0")
    suspend fun getFutureUnpaidPayments(clientId: String, fromMonth: String): List<Payment>

    // === استعلامات القراءة ===

    @Query("SELECT * FROM payments ORDER BY month DESC")
    fun getAllPayments(): LiveData<List<Payment>>

    @Query("SELECT * FROM payments WHERE clientId = :clientId AND month = :month LIMIT 1")
    suspend fun getPayment(clientId: String, month: String): Payment?

    @Query("SELECT * FROM payments WHERE clientId = :clientId AND month = :month LIMIT 1")
    fun getPaymentLive(clientId: String, month: String): LiveData<Payment>

    @Query("SELECT * FROM payments WHERE clientId = :clientId ORDER BY month DESC")
    fun getClientPayments(clientId: String): LiveData<List<Payment>>

    @Query("SELECT * FROM payments WHERE month = :month")
    fun getPaymentsByMonth(month: String): LiveData<List<Payment>>

    @Query("SELECT * FROM payments WHERE month = :month")
    suspend fun getPaymentsByMonthDirect(month: String): List<Payment>

    @Query("SELECT * FROM payments ORDER BY month DESC")
    suspend fun getAllPaymentsDirect(): List<Payment>

    @Query("SELECT * FROM payments WHERE month = :month AND isPaid = 1")
    fun getPaidPaymentsByMonth(month: String): LiveData<List<Payment>>

    @Query("SELECT * FROM payments WHERE month = :month AND isPaid = 0")
    fun getUnpaidPaymentsByMonth(month: String): LiveData<List<Payment>>

    @Query("SELECT COUNT(*) FROM payments WHERE month = :month AND isPaid = 1")
    fun getPaidCountByMonth(month: String): LiveData<Int>

    @Query("SELECT COUNT(*) FROM payments WHERE month = :month AND isPaid = 0")
    fun getUnpaidCountByMonth(month: String): LiveData<Int>

    @Query("SELECT SUM(amount) FROM payments WHERE month = :month AND isPaid = 1")
    fun getTotalPaidAmountByMonth(month: String): LiveData<Double>

    @Query("SELECT SUM(amount) FROM payments WHERE month = :month AND isPaid = 0")
    fun getTotalUnpaidAmountByMonth(month: String): LiveData<Double>

    // === استعلامات الكتابة ===

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(payment: Payment): Long

    @Upsert
    suspend fun upsert(payment: Payment)

    @Update
    suspend fun update(payment: Payment)

    @Delete
    suspend fun delete(payment: Payment)

    @Query("DELETE FROM payments WHERE clientId = :clientId")
    suspend fun deleteClientPayments(clientId: String)

    @Query("DELETE FROM payments WHERE clientId = :clientId AND month = :month")
    suspend fun deletePayment(clientId: String, month: String)

    // === دوال مساعدة ===

    @Query("UPDATE payments SET isPaid = 1, paymentDate = :paymentDate WHERE clientId = :clientId AND month = :month")
    suspend fun markAsPaid(clientId: String, month: String, paymentDate: Long)

    @Query("UPDATE payments SET isPaid = 0, paymentDate = NULL WHERE clientId = :clientId AND month = :month")
    suspend fun markAsUnpaid(clientId: String, month: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun createPaymentIfNotExists(payment: Payment)

    @Query("SELECT * FROM payments WHERE id = :id LIMIT 1")
    suspend fun getPaymentById(id: String): Payment?

    @Query("""
        UPDATE payments SET amount = :newAmount
        WHERE clientId = :clientId AND month >= :fromMonth
    """)
    suspend fun updateFuturePaymentsAmount(
        clientId: String,
        fromMonth: String,
        newAmount: Double
    )

    @Query("""
        UPDATE payments SET amount = :newAmount
        WHERE clientId = :clientId AND month >= :fromMonth
        AND id NOT IN (
            SELECT DISTINCT paymentId FROM payment_transactions
            WHERE clientId = :clientId
        )
    """)
    suspend fun updateFutureUnpaidPaymentsAmount(
        clientId: String,
        fromMonth: String,
        newAmount: Double
    )

    @Query("""
        SELECT month FROM payments
        WHERE clientId = :clientId
        AND id NOT IN (
            SELECT DISTINCT paymentId FROM payment_transactions
            WHERE clientId = :clientId
        )
        ORDER BY month LIMIT 1
    """)
    suspend fun getFirstUnpaidMonthForClient(clientId: String): String?

    // ================== Flow-based reactive queries ==================

    @Query("SELECT * FROM payments WHERE month = :month")
    fun observePaymentsByMonth(month: String): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE clientId = :clientId ORDER BY month DESC")
    fun observeClientPayments(clientId: String): Flow<List<Payment>>

    /**
     * Optimistic Locking: updates only if version matches.
     * Returns 1 if updated, 0 if version mismatch.
     */
    @Query("""
        UPDATE payments SET 
            clientId = :clientId,
            month = :month,
            isPaid = :isPaid,
            paymentDate = :paymentDate,
            amount = :amount,
            notes = :notes,
            lastModifiedBy = :lastModifiedBy,
            updatedAt = :updatedAt,
            version = :newVersion,
            checksum = :checksum
        WHERE id = :id AND version = :expectedVersion
    """)
    suspend fun updateWithVersionCheck(
        id: String,
        clientId: String,
        month: String,
        isPaid: Boolean,
        paymentDate: Long?,
        amount: Double,
        notes: String,
        lastModifiedBy: Int?,
        updatedAt: Long,
        newVersion: Int,
        expectedVersion: Int,
        checksum: String
    ): Int
}
