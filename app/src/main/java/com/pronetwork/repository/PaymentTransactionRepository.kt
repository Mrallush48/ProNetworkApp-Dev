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
import com.pronetwork.util.OptimisticLockException
import com.pronetwork.util.generateId
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
     * user ID الحالي من SharedPreferences.
     * يُستخدم لتعبئة createdBy تلقائياً عند إدخال حركة جديدة.
     */
    private fun currentUserId(): Int? {
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
            prefs.getInt("user_id", -1).takeIf { it != -1 }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun insert(transaction: PaymentTransaction): String {
        val newTransaction = transaction.copy(
            id = generateId(),
            createdBy = transaction.createdBy ?: currentUserId(),
            updatedAt = System.currentTimeMillis(),
            version = 1
        )
        transactionDao.insert(newTransaction)
        enqueueSync("payment_transaction", newTransaction.id, "CREATE", newTransaction)
        return newTransaction.id
    }

    suspend fun update(transaction: PaymentTransaction) {
        val updated = transaction.copy(
            version = transaction.version + 1,
            updatedAt = System.currentTimeMillis()
        )
        val rows = transactionDao.updateWithVersionCheck(
            id = updated.id,
            paymentId = updated.paymentId,
            type = updated.type,
            amount = updated.amount,
            notes = updated.notes,
            createdBy = updated.createdBy,
            timestamp = updated.timestamp,
            updatedAt = updated.updatedAt,
            newVersion = updated.version,
            expectedVersion = transaction.version
        )
        if (rows == 0) throw OptimisticLockException("payment_transaction", transaction.id)
        enqueueSync("payment_transaction", updated.id, "UPDATE", updated)
    }

    suspend fun delete(transaction: PaymentTransaction) {
        transactionDao.delete(transaction)
        enqueueSync("payment_transaction", transaction.id, "DELETE", transaction)
    }

    fun getTransactionsForPayment(paymentId: String): LiveData<List<PaymentTransaction>> {
        return transactionDao.getTransactionsForPayment(paymentId)
    }

    suspend fun getTransactionsForPaymentList(paymentId: String): List<PaymentTransaction> {
        return transactionDao.getTransactionsForPaymentList(paymentId)
    }

    suspend fun getTotalPaidForPayment(paymentId: String): Double {
        return transactionDao.getTotalPaidForPayment(paymentId)
    }

    suspend fun getDailyBuildingCollectionsForDay(
        dayStartMillis: Long,
        dayEndMillis: Long
    ): List<DailyBuildingCollection> {
        return transactionDao.getDailyBuildingCollectionsForDay(dayStartMillis, dayEndMillis)
    }

    suspend fun getTotalsForPayments(paymentIds: List<String>): Map<String, Double> {
        if (paymentIds.isEmpty()) return emptyMap()
        val rows = transactionDao.getTotalsForPayments(paymentIds)
        return rows.associate { it.paymentId to it.totalPaid }
    }

    suspend fun deleteTransactionsForPayment(paymentId: String) {
        val transactions = transactionDao.getTransactionsForPaymentList(paymentId)
        transactionDao.deleteByPaymentId(paymentId)
        transactions.forEach { tx ->
            enqueueSync("payment_transaction", tx.id, "DELETE", tx)
        }
    }

    suspend fun deleteTransactionById(transactionId: String) {
        val tx = transactionDao.getTransactionById(transactionId)
        transactionDao.deleteTransactionById(transactionId)
        if (tx != null) {
            enqueueSync("payment_transaction", tx.id, "DELETE", tx)
        }
    }

    suspend fun getPaymentIdByTransactionId(transactionId: String): String? {
        return transactionDao.getPaymentIdByTransactionId(transactionId)
    }

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

    suspend fun getDetailedDailyCollectionsByUser(
        dayStartMillis: Long,
        dayEndMillis: Long,
        userId: String
    ): List<PaymentTransactionDao.DailyDetailedTransaction> {
        return transactionDao.getDetailedDailyCollectionsByUser(dayStartMillis, dayEndMillis, userId)
    }

    suspend fun hasNegativeTransaction(paymentId: String): Boolean {
        return transactionDao.hasNegativeTransaction(paymentId)
    }

    suspend fun getPaymentIdsWithRefunds(paymentIds: List<String>): List<String> {
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

    fun observeTotalsForPayments(paymentIds: List<String>): Flow<List<PaymentTransactionDao.PaymentTotal>> {
        if (paymentIds.isEmpty()) return kotlinx.coroutines.flow.flowOf(emptyList())
        return transactionDao.observeTotalsForPayments(paymentIds)
    }

    fun observePaymentIdsWithRefunds(paymentIds: List<String>): Flow<List<String>> {
        if (paymentIds.isEmpty()) return kotlinx.coroutines.flow.flowOf(emptyList())
        return transactionDao.observePaymentIdsWithRefunds(paymentIds)
    }

    fun observeTotalPaidForPayment(paymentId: String): Flow<Double> {
        return transactionDao.observeTotalPaidForPayment(paymentId)
    }

    fun observeRecentTransactions(limit: Int): Flow<List<PaymentTransactionDao.DashboardRecentTransaction>> {
        return transactionDao.observeRecentTransactions(limit)
    }

    fun observeTopUnpaidClients(month: String, limit: Int): Flow<List<PaymentTransactionDao.DashboardUnpaidClient>> {
        return transactionDao.observeTopUnpaidClientsForMonth(month, limit)
    }

    // === مزامنة العمليات ===

    private suspend fun enqueueSync(entityType: String, entityId: String, action: String, entity: Any) {
        try {
            syncEngine.enqueue(
                entityType = entityType,
                entityId = entityId,
                action = action,
                payload = gson.toJson(entity)
            )
            SyncWorker.syncNow(context)
        } catch (e: Exception) {
            android.util.Log.w("PaymentTransactionRepo", "Sync enqueue failed: ${e.message}")
        }
    }
}
