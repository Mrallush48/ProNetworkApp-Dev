package com.pronetwork.app.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.RoomWarnings
import com.pronetwork.data.DailySummary
import kotlinx.coroutines.flow.Flow
import androidx.room.Upsert

@Dao
interface PaymentTransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: PaymentTransaction): Long

    @Upsert
    suspend fun upsert(transaction: PaymentTransaction)

    @Update
    suspend fun update(transaction: PaymentTransaction)

    @Delete
    suspend fun delete(transaction: PaymentTransaction)

    @Query("SELECT * FROM payment_transactions WHERE paymentId = :paymentId ORDER BY timestamp ASC")
    fun getTransactionsForPayment(paymentId: String): LiveData<List<PaymentTransaction>>

    @Query("SELECT * FROM payment_transactions WHERE paymentId = :paymentId ORDER BY id ASC")
    suspend fun getTransactionsForPaymentList(paymentId: String): List<PaymentTransaction>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM payment_transactions WHERE paymentId = :paymentId")
    suspend fun getTotalPaidForPayment(paymentId: String): Double

    @Query("""
        SELECT paymentId, COALESCE(SUM(amount), 0) AS totalPaid
        FROM payment_transactions
        WHERE paymentId IN (:paymentIds)
        GROUP BY paymentId
    """)
    suspend fun getTotalsForPayments(paymentIds: List<String>): List<PaymentTotal>

    @Query("DELETE FROM payment_transactions WHERE paymentId = :paymentId")
    suspend fun deleteByPaymentId(paymentId: String)

    @Query("DELETE FROM payment_transactions WHERE id = :transactionId")
    suspend fun deleteTransactionById(transactionId: String)

    @Query("SELECT * FROM payment_transactions WHERE id = :transactionId")
    suspend fun getTransactionById(transactionId: String): PaymentTransaction?

    @Query("SELECT paymentId FROM payment_transactions WHERE id = :transactionId LIMIT 1")
    suspend fun getPaymentIdByTransactionId(transactionId: String): String?

    @Query("""
        SELECT 
            c.buildingId AS buildingId,
            COALESCE(b.name, 'مبنى غير معروف') AS buildingName,
            COALESCE(SUM(pt.amount), 0) AS totalAmount,
            COUNT(DISTINCT c.id) AS clientsCount
        FROM payment_transactions AS pt
        INNER JOIN payments AS p ON p.id = pt.paymentId
        INNER JOIN clients AS c ON c.id = p.clientId
        LEFT JOIN buildings AS b ON b.id = c.buildingId
        WHERE pt.timestamp >= :dayStartMillis AND pt.timestamp < :dayEndMillis
        GROUP BY c.buildingId, b.name
        ORDER BY b.name COLLATE NOCASE ASC
    """)
    suspend fun getDailyBuildingCollectionsForDay(
        dayStartMillis: Long,
        dayEndMillis: Long
    ): List<DailyBuildingCollection>

    @Query("SELECT COUNT(*) > 0 FROM payment_transactions WHERE paymentId = :paymentId AND amount < 0")
    suspend fun hasNegativeTransaction(paymentId: String): Boolean

    @Query("""
        SELECT DISTINCT paymentId
        FROM payment_transactions
        WHERE paymentId IN (:paymentIds) AND amount < 0
    """)
    suspend fun getPaymentIdsWithRefunds(paymentIds: List<String>): List<String>

    data class PaymentTotal(
        val paymentId: String,
        val totalPaid: Double
    )

    // ================== Daily Summary ==================

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("""
        SELECT 
            COALESCE(SUM(amount), 0.0) as totalAmount,
            COUNT(DISTINCT paymentId) as totalClients,
            COUNT(*) as totalTransactions
        FROM payment_transactions
        WHERE date(timestamp / 1000, 'unixepoch', 'localtime') = :date
    """)
    fun getDailySummary(date: String): Flow<DailySummary>

    // ================== Detailed Transactions for Month ==================

    @Query("""
        SELECT 
            pt.id AS transactionId,
            pt.amount AS transactionAmount,
            pt.timestamp AS transactionDate,
            pt.notes AS transactionNotes,
            p.id AS paymentId,
            p.month AS paymentMonth,
            p.amount AS monthlyAmount,
            p.isPaid AS isPaid,
            p.clientId AS clientId,
            c.name AS clientName,
            c.subscriptionNumber AS subscriptionNumber,
            c.phone AS clientPhone,
            c.packageType AS packageType,
            c.roomNumber AS roomNumber,
            c.buildingId AS buildingId,
            COALESCE(b.name, 'Unknown') AS buildingName
        FROM payment_transactions AS pt
        INNER JOIN payments AS p ON p.id = pt.paymentId
        INNER JOIN clients AS c ON c.id = p.clientId
        LEFT JOIN buildings AS b ON b.id = c.buildingId
        WHERE p.month = :month
        ORDER BY b.name, c.name, pt.timestamp ASC
    """)
    suspend fun getDetailedTransactionsForMonth(month: String): List<DetailedTransaction>

    data class DetailedTransaction(
        val transactionId: String,
        val transactionAmount: Double,
        val transactionDate: Long,
        val transactionNotes: String,
        val paymentId: String,
        val paymentMonth: String,
        val monthlyAmount: Double,
        val isPaid: Boolean,
        val clientId: String,
        val clientName: String,
        val subscriptionNumber: String,
        val clientPhone: String,
        val packageType: String,
        val roomNumber: String?,
        val buildingId: String,
        val buildingName: String
    )

    // ================== Daily Detailed Collections ==================

    @Query("""
        SELECT 
            pt.id AS transactionId,
            pt.amount AS paidAmount,
            pt.timestamp AS transactionDate,
            pt.notes AS notes,
            p.id AS paymentId,
            p.amount AS monthlyAmount,
            p.clientId AS clientId,
            c.name AS clientName,
            c.subscriptionNumber AS subscriptionNumber,
            c.roomNumber AS roomNumber,
            c.packageType AS packageType,
            c.buildingId AS buildingId,
            COALESCE(b.name, 'Unknown') AS buildingName
        FROM payment_transactions AS pt
        INNER JOIN payments AS p ON p.id = pt.paymentId
        INNER JOIN clients AS c ON c.id = p.clientId
        LEFT JOIN buildings AS b ON b.id = c.buildingId
        WHERE pt.timestamp >= :dayStartMillis AND pt.timestamp < :dayEndMillis
        ORDER BY b.name COLLATE NOCASE ASC, c.name COLLATE NOCASE ASC, pt.timestamp ASC
    """)
    suspend fun getDetailedDailyCollections(
        dayStartMillis: Long,
        dayEndMillis: Long
    ): List<DailyDetailedTransaction>

    @Query("""
        SELECT 
            pt.id AS transactionId,
            pt.amount AS paidAmount,
            pt.timestamp AS transactionDate,
            pt.notes AS notes,
            p.id AS paymentId,
            p.amount AS monthlyAmount,
            p.clientId AS clientId,
            c.name AS clientName,
            c.subscriptionNumber AS subscriptionNumber,
            c.roomNumber AS roomNumber,
            c.packageType AS packageType,
            c.buildingId AS buildingId,
            COALESCE(b.name, 'Unknown') AS buildingName
        FROM payment_transactions AS pt
        INNER JOIN payments AS p ON p.id = pt.paymentId
        INNER JOIN clients AS c ON c.id = p.clientId
        LEFT JOIN buildings AS b ON b.id = c.buildingId
        WHERE pt.timestamp >= :dayStartMillis AND pt.timestamp < :dayEndMillis
        AND pt.createdBy = :userId
        ORDER BY b.name COLLATE NOCASE ASC, c.name COLLATE NOCASE ASC, pt.timestamp ASC
    """)
    suspend fun getDetailedDailyCollectionsByUser(
        dayStartMillis: Long,
        dayEndMillis: Long,
        userId: String
    ): List<DailyDetailedTransaction>

    data class DailyDetailedTransaction(
        val transactionId: String,
        val paidAmount: Double,
        val transactionDate: Long,
        val notes: String,
        val paymentId: String,
        val monthlyAmount: Double,
        val clientId: String,
        val clientName: String,
        val subscriptionNumber: String,
        val roomNumber: String?,
        val packageType: String,
        val buildingId: String,
        val buildingName: String
    )

    // ================== Dashboard ==================

    @Query("""
        SELECT 
            pt.id AS transactionId,
            pt.amount AS transactionAmount,
            pt.timestamp AS transactionDate,
            pt.notes AS transactionNotes,
            c.name AS clientName,
            COALESCE(b.name, 'Unknown') AS buildingName
        FROM payment_transactions AS pt
        INNER JOIN payments AS p ON p.id = pt.paymentId
        INNER JOIN clients AS c ON c.id = p.clientId
        LEFT JOIN buildings AS b ON b.id = c.buildingId
        ORDER BY pt.timestamp DESC
        LIMIT :limit
    """)
    suspend fun getRecentTransactions(limit: Int = 10): List<DashboardRecentTransaction>

    data class DashboardRecentTransaction(
        val transactionId: String,
        val transactionAmount: Double,
        val transactionDate: Long,
        val transactionNotes: String,
        val clientName: String,
        val buildingName: String
    )

    @Query("""
        SELECT 
            p.clientId AS clientId,
            c.name AS clientName,
            COALESCE(b.name, 'Unknown') AS buildingName,
            p.amount AS monthlyAmount,
            COALESCE(SUM(pt.amount), 0) AS totalPaid,
            (p.amount - COALESCE(SUM(pt.amount), 0)) AS remaining
        FROM payments AS p
        INNER JOIN clients AS c ON c.id = p.clientId
        LEFT JOIN buildings AS b ON b.id = c.buildingId
        LEFT JOIN payment_transactions AS pt ON pt.paymentId = p.id
        WHERE p.month = :month
        GROUP BY p.id
        HAVING remaining > 0
        ORDER BY remaining DESC
        LIMIT :limit
    """)
    suspend fun getTopUnpaidClientsForMonth(month: String, limit: Int = 5): List<DashboardUnpaidClient>

    data class DashboardUnpaidClient(
        val clientId: String,
        val clientName: String,
        val buildingName: String,
        val monthlyAmount: Double,
        val totalPaid: Double,
        val remaining: Double
    )

    // ================== Flow-based reactive queries ==================

    @Query("""
        SELECT paymentId, COALESCE(SUM(amount), 0) AS totalPaid
        FROM payment_transactions
        WHERE paymentId IN (:paymentIds)
        GROUP BY paymentId
    """)
    fun observeTotalsForPayments(paymentIds: List<String>): Flow<List<PaymentTotal>>

    @Query("""
        SELECT DISTINCT paymentId
        FROM payment_transactions
        WHERE paymentId IN (:paymentIds) AND amount < 0
    """)
    fun observePaymentIdsWithRefunds(paymentIds: List<String>): Flow<List<String>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM payment_transactions WHERE paymentId = :paymentId")
    fun observeTotalPaidForPayment(paymentId: String): Flow<Double>

    @Query("""
        SELECT 
            pt.id AS transactionId,
            pt.amount AS transactionAmount,
            pt.timestamp AS transactionDate,
            pt.notes AS transactionNotes,
            c.name AS clientName,
            COALESCE(b.name, 'Unknown') AS buildingName
        FROM payment_transactions AS pt
        INNER JOIN payments AS p ON p.id = pt.paymentId
        INNER JOIN clients AS c ON c.id = p.clientId
        LEFT JOIN buildings AS b ON b.id = c.buildingId
        ORDER BY pt.timestamp DESC
        LIMIT :limit
    """)
    fun observeRecentTransactions(limit: Int): Flow<List<DashboardRecentTransaction>>

    @Query("""
        SELECT 
            p.clientId AS clientId,
            c.name AS clientName,
            COALESCE(b.name, 'Unknown') AS buildingName,
            p.amount AS monthlyAmount,
            COALESCE(SUM(pt.amount), 0) AS totalPaid,
            (p.amount - COALESCE(SUM(pt.amount), 0)) AS remaining
        FROM payments AS p
        INNER JOIN clients AS c ON c.id = p.clientId
        LEFT JOIN buildings AS b ON b.id = c.buildingId
        LEFT JOIN payment_transactions AS pt ON pt.paymentId = p.id
        WHERE p.month = :month
        GROUP BY p.id
        HAVING remaining > 0
        ORDER BY remaining DESC
        LIMIT :limit
    """)
    fun observeTopUnpaidClientsForMonth(month: String, limit: Int): Flow<List<DashboardUnpaidClient>>

    /**
     * Optimistic Locking: updates only if version matches.
     * Returns 1 if updated, 0 if version mismatch.
     */
    @Query("""
        UPDATE payment_transactions SET 
            paymentId = :paymentId,
            type = :type,
            amount = :amount,
            notes = :notes,
            createdBy = :createdBy,
            timestamp = :timestamp,
            updatedAt = :updatedAt,
            version = :newVersion
        WHERE id = :id AND version = :expectedVersion
    """)
    suspend fun updateWithVersionCheck(
        id: String,
        paymentId: String,
        type: String,
        amount: Double,
        notes: String,
        createdBy: Int?,
        timestamp: Long,
        updatedAt: Long,
        newVersion: Int,
        expectedVersion: Int
    ): Int
}
