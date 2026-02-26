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


@Dao
interface PaymentTransactionDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: PaymentTransaction): Long

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

    @Query("SELECT * FROM payment_transactions WHERE id = :transactionId")
    suspend fun getTransactionById(transactionId: Int): PaymentTransaction?


    @Query("SELECT paymentId FROM payment_transactions WHERE id = :transactionId LIMIT 1")
    suspend fun getPaymentIdByTransactionId(transactionId: Int): Int?

    // تحصيل يومي لكل مبنى (مع الحفاظ على منطقك، فقط تعديل بسيط في count)
    @Query(
        """
    SELECT 
        c.buildingId AS buildingId,
        COALESCE(b.name, 'مبنى غير معروف') AS buildingName,
        COALESCE(SUM(pt.amount), 0) AS totalAmount,
        COUNT(DISTINCT c.id) AS clientsCount
    FROM payment_transactions AS pt
    INNER JOIN payments AS p ON p.id = pt.paymentId
    INNER JOIN clients AS c ON c.id = p.clientId
    LEFT JOIN buildings AS b ON b.id = c.buildingId
    WHERE pt.date >= :dayStartMillis AND pt.date < :dayEndMillis
    GROUP BY c.buildingId, b.name
    ORDER BY b.name COLLATE NOCASE ASC
    """
    )
    suspend fun getDailyBuildingCollectionsForDay(
        dayStartMillis: Long,
        dayEndMillis: Long
    ): List<DailyBuildingCollection>

    /** هل يوجد حركة سالبة (Refund) لهذا الـ Payment؟ */
    @Query("SELECT COUNT(*) > 0 FROM payment_transactions WHERE paymentId = :paymentId AND amount < 0")
    suspend fun hasNegativeTransaction(paymentId: Int): Boolean

    /** استعلام جماعي: أي paymentIds فيها حركات سالبة */
    @Query("""
        SELECT DISTINCT paymentId 
        FROM payment_transactions 
        WHERE paymentId IN (:paymentIds) AND amount < 0
    """)
    suspend fun getPaymentIdsWithRefunds(paymentIds: List<Int>): List<Int>

    data class PaymentTotal(
        val paymentId: Int,
        val totalPaid: Double
    )

    /**
     * استعلام لحساب ملخص التحصيل اليومي
     * يحسب: إجمالي المبلغ، عدد الدفعات المختلفة، عدد الحركات
     *
     * @param date التاريخ المطلوب (بصيغة yyyy-MM-dd)
     * @return Flow<DailySummary> يتحدث تلقائياً عند تغيير البيانات
     */
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query(
        """
        SELECT 
            COALESCE(SUM(amount), 0.0) as totalAmount,
            COUNT(DISTINCT paymentId) as totalClients,
            COUNT(*) as totalTransactions
        FROM payment_transactions 
        WHERE date(date / 1000, 'unixepoch', 'localtime') = :date
    """
    )
    fun getDailySummary(date: String): Flow<DailySummary>

    /**
     * جلب كل حركات الدفع لشهر محدد مع بيانات العميل والمبنى
     * يُستخدم لتقرير المدفوعات التفصيلي
     */
    @Query(
        """
        SELECT 
            pt.id AS transactionId,
            pt.amount AS transactionAmount,
            pt.date AS transactionDate,
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
        ORDER BY b.name, c.name, pt.date ASC
    """
    )
    suspend fun getDetailedTransactionsForMonth(month: String): List<DetailedTransaction>

    data class DetailedTransaction(
        val transactionId: Int,
        val transactionAmount: Double,
        val transactionDate: Long,
        val transactionNotes: String,
        val paymentId: Int,
        val paymentMonth: String,
        val monthlyAmount: Double,
        val isPaid: Boolean,
        val clientId: Int,
        val clientName: String,
        val subscriptionNumber: String,
        val clientPhone: String,
        val packageType: String,
        val roomNumber: String?,
        val buildingId: Int,
        val buildingName: String
    )

    // ================== تحصيل يومي تفصيلي (عميل بعميل) ==================
    @Query(
        """
            SELECT
        pt.id AS transactionId,
        pt.amount AS paidAmount,
        pt.date AS transactionDate,
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
        WHERE pt.date >= :dayStartMillis AND pt.date < :dayEndMillis
        ORDER BY b.name COLLATE NOCASE ASC, c.name COLLATE NOCASE ASC, pt.date ASC
    """
    )
    suspend fun getDetailedDailyCollections(
        dayStartMillis: Long,
        dayEndMillis: Long
    ): List<DailyDetailedTransaction>

    data class DailyDetailedTransaction(
        val transactionId: Int,
        val paidAmount: Double,
        val transactionDate: Long,
        val notes: String,
        val paymentId: Int,
        val monthlyAmount: Double,
        val clientId: Int,
        val clientName: String,
        val subscriptionNumber: String,
        val roomNumber: String?,
        val packageType: String,
        val buildingId: Int,
        val buildingName: String
    )

    // ================== Dashboard: آخر الحركات ==================
    @Query("""
        SELECT 
            pt.id AS transactionId,
            pt.amount AS transactionAmount,
            pt.date AS transactionDate,
            pt.notes AS transactionNotes,
            c.name AS clientName,
            COALESCE(b.name, 'Unknown') AS buildingName
        FROM payment_transactions AS pt
        INNER JOIN payments AS p ON p.id = pt.paymentId
        INNER JOIN clients AS c ON c.id = p.clientId
        LEFT JOIN buildings AS b ON b.id = c.buildingId
        ORDER BY pt.date DESC
        LIMIT :limit
    """)
    suspend fun getRecentTransactions(limit: Int = 10): List<DashboardRecentTransaction>

    data class DashboardRecentTransaction(
        val transactionId: Int,
        val transactionAmount: Double,
        val transactionDate: Long,
        val transactionNotes: String,
        val clientName: String,
        val buildingName: String
    )

    // ================== Dashboard: أكبر العملاء المتبقي عليهم ==================
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
        val clientId: Int,
        val clientName: String,
        val buildingName: String,
        val monthlyAmount: Double,
        val totalPaid: Double,
        val remaining: Double
    )


}
