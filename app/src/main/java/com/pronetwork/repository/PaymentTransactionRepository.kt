package com.pronetwork.app.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.pronetwork.app.data.ClientDao
import com.pronetwork.app.data.DailyBuildingCollection
import com.pronetwork.app.data.PaymentTransaction
import com.pronetwork.app.data.PaymentTransactionDao
import com.pronetwork.app.network.SyncEngine
import com.pronetwork.app.network.SyncWorker
import com.pronetwork.data.DailySummary
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentTransactionRepository @Inject constructor(
    private val transactionDao: PaymentTransactionDao,
    private val clientDao: ClientDao,
    private val syncEngine: SyncEngine,
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    /**
     * اسم المستخدم الحالي من SharedPreferences.
     * يُستخدم لتعبئة createdBy تلقائياً عند إدخال حركة جديدة.
     */
    private fun currentUsername(): String {
        return try {
            val masterKey = androidx.security.crypto.MasterKey.Builder(context)
                .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
                .build()
            val prefs = androidx.security.crypto.EncryptedSharedPreferences.create(
                context,
                "pronetwork_auth",
                masterKey,
                androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            prefs.getString("username", "") ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun insert(transaction: PaymentTransaction) {
        val actualTransaction = if (transaction.createdBy.isEmpty()) {
            transaction.copy(createdBy = currentUsername())
        } else {
            transaction
        }
        val rowId = transactionDao.insert(actualTransaction)
        val savedTransaction = actualTransaction.copy(id = rowId.toInt())
        enqueueSync("payment_transaction", savedTransaction.id, "CREATE", savedTransaction)
    }

    suspend fun update(transaction: PaymentTransaction) {
        transactionDao.update(transaction)
        enqueueSync("payment_transaction", transaction.id, "UPDATE", transaction)
    }

    suspend fun delete(transaction: PaymentTransaction) {
        transactionDao.delete(transaction)
        enqueueSync("payment_transaction", transaction.id, "DELETE", transaction)
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
        // Get transactions before deleting to enqueue sync
        val transactions = transactionDao.getTransactionsForPaymentList(paymentId)
        transactionDao.deleteByPaymentId(paymentId)
        transactions.forEach { tx ->
            enqueueSync("payment_transaction", tx.id, "DELETE", tx)
        }
    }

    suspend fun deleteTransactionById(transactionId: Int) {
        // Get transaction before deleting to enqueue sync
        val tx = transactionDao.getTransactionById(transactionId)
        transactionDao.deleteTransactionById(transactionId)
        if (tx != null) {
            enqueueSync("payment_transaction", tx.id, "DELETE", tx)
        }
    }

    suspend fun getPaymentIdByTransactionId(transactionId: Int): Int? {
        return transactionDao.getPaymentIdByTransactionId(transactionId)
    }

    /**
     * الحصول على ملخص التحصيل اليومي
     *
     * @param date التاريخ بصيغة yyyy-MM-dd (مثال: "2026-02-12")
     * @return Flow يتحدث تلقائياً عند تغيير البيانات
     */
    fun getDailySummary(date: String): Flow<DailySummary?> {
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

    /**
     * حركات يوم معيّن لمستخدم محدد — لـ Daily Collection الشخصي.
     */
    suspend fun getDetailedDailyCollectionsByUser(
        dayStartMillis: Long,
        dayEndMillis: Long,
        userId: String
    ): List<PaymentTransactionDao.DailyDetailedTransaction> {
        return transactionDao.getDetailedDailyCollectionsByUser(dayStartMillis, dayEndMillis, userId)
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

    // ================== Flow-based reactive queries ==================

    /**
     * Flow تفاعلي: مجموع المدفوع لكل paymentId — يتحدث تلقائياً.
     * يُمرّر مباشرة من الـ Dao بدون تحويل.
     */
    fun observeTotalsForPayments(paymentIds: List<Int>): Flow<List<PaymentTransactionDao.PaymentTotal>> {
        if (paymentIds.isEmpty()) return kotlinx.coroutines.flow.flowOf(emptyList())
        return transactionDao.observeTotalsForPayments(paymentIds)
    }

    /**
     * Flow تفاعلي: أي paymentIds فيها حركات سالبة — يتحدث تلقائياً.
     */
    fun observePaymentIdsWithRefunds(paymentIds: List<Int>): Flow<List<Int>> {
        if (paymentIds.isEmpty()) return kotlinx.coroutines.flow.flowOf(emptyList())
        return transactionDao.observePaymentIdsWithRefunds(paymentIds)
    }

    /**
     * Flow تفاعلي: مجموع المدفوع لـ payment واحد — يتحدث تلقائياً.
     */
    fun observeTotalPaidForPayment(paymentId: Int): Flow<Double> {
        return transactionDao.observeTotalPaidForPayment(paymentId)
    }

    /**
     * Flow تفاعلي: آخر الحركات — يتحدث تلقائياً.
     */
    fun observeRecentTransactions(limit: Int): Flow<List<PaymentTransactionDao.DashboardRecentTransaction>> {
        return transactionDao.observeRecentTransactions(limit)
    }

    /**
     * Flow تفاعلي: العملاء الأكثر تأخراً — يتحدث تلقائياً.
     */
    fun observeTopUnpaidClients(month: String, limit: Int): Flow<List<PaymentTransactionDao.DashboardUnpaidClient>> {
        return transactionDao.observeTopUnpaidClientsForMonth(month, limit)
    }

    // === مزامنة العمليات ===

    /**
     * إضافة العملية لقائمة المزامنة + تشغيل sync فوري
     * يعمل بصمت — أي خطأ في الـ enqueue لا يؤثر على العملية الأساسية
     */
    private suspend fun enqueueSync(entityType: String, entityId: Int, action: String, entity: Any) {
        try {
            syncEngine.enqueue(
                entityType = entityType,
                entityId = entityId,
                action = action,
                payload = gson.toJson(entity)
            )
            // تشغيل مزامنة فورية في الخلفية
            SyncWorker.syncNow(context)
        } catch (e: Exception) {
            // لا نوقف العملية المحلية أبداً بسبب فشل الـ enqueue
            android.util.Log.w("PaymentTransactionRepo", "Sync enqueue failed: ${e.message}")
        }
    }
}
