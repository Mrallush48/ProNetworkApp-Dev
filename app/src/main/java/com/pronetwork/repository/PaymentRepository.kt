package com.pronetwork.app.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.pronetwork.app.data.ClientDao
import com.pronetwork.app.data.Payment
import com.pronetwork.app.data.PaymentDao
import com.pronetwork.app.data.Client
import com.pronetwork.app.network.SyncEngine
import com.pronetwork.app.network.SyncWorker
import com.pronetwork.util.OptimisticLockException
import com.pronetwork.util.generateId
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

// توحيد صيغة الشهر إلى yyyy-MM
private fun normalizeMonth(yearMonth: String): String {
    return try {
        val parts = yearMonth.trim().split("-")
        val y = parts[0].toInt()
        val m = parts[1].toInt()
        String.format("%04d-%02d", y, m)
    } catch (e: Exception) {
        yearMonth.trim()
    }
}

@Singleton
class PaymentRepository @Inject constructor(
    private val paymentDao: PaymentDao,
    private val clientDao: ClientDao,
    private val syncEngine: SyncEngine,
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    // === استعلامات القراءة ===

    val allPayments: LiveData<List<Payment>> = paymentDao.getAllPayments()

    fun getPaymentLive(clientId: String, month: String): LiveData<Payment> {
        return paymentDao.getPaymentLive(clientId, normalizeMonth(month))
    }

    fun getClientPayments(clientId: String): LiveData<List<Payment>> {
        return paymentDao.getClientPayments(clientId)
    }

    fun getPaymentsByMonth(month: String): LiveData<List<Payment>> {
        return paymentDao.getPaymentsByMonth(normalizeMonth(month))
    }

    fun getPaidPaymentsByMonth(month: String): LiveData<List<Payment>> {
        return paymentDao.getPaidPaymentsByMonth(normalizeMonth(month))
    }

    fun getUnpaidPaymentsByMonth(month: String): LiveData<List<Payment>> {
        return paymentDao.getUnpaidPaymentsByMonth(normalizeMonth(month))
    }

    fun getPaidCountByMonth(month: String): LiveData<Int> {
        return paymentDao.getPaidCountByMonth(normalizeMonth(month))
    }

    fun getUnpaidCountByMonth(month: String): LiveData<Int> {
        return paymentDao.getUnpaidCountByMonth(normalizeMonth(month))
    }

    fun getTotalPaidAmountByMonth(month: String): LiveData<Double> {
        return paymentDao.getTotalPaidAmountByMonth(normalizeMonth(month))
    }

    fun getTotalUnpaidAmountByMonth(month: String): LiveData<Double> {
        return paymentDao.getTotalUnpaidAmountByMonth(normalizeMonth(month))
    }

    // ================== Flow-based reactive queries ==================

    fun observePaymentsByMonth(month: String): Flow<List<Payment>> {
        return paymentDao.observePaymentsByMonth(normalizeMonth(month))
    }

    fun observeClientPayments(clientId: String): Flow<List<Payment>> {
        return paymentDao.observeClientPayments(clientId)
    }

    // === استعلامات الكتابة ===

    suspend fun insert(payment: Payment): String {
        val newPayment = payment.copy(
            id = generateId(),
            updatedAt = System.currentTimeMillis(),
            version = 1,
            checksum = ""
        )
        paymentDao.insert(newPayment)
        enqueueSync("payment", newPayment.id, "CREATE", newPayment)
        return newPayment.id
    }

    suspend fun update(payment: Payment) {
        val updated = payment.copy(
            version = payment.version + 1,
            updatedAt = System.currentTimeMillis()
        )
        val rows = paymentDao.updateWithVersionCheck(
            id = updated.id,
            clientId = updated.clientId,
            month = updated.month,
            isPaid = updated.isPaid,
            paymentDate = updated.paymentDate,
            amount = updated.amount,
            notes = updated.notes,
            lastModifiedBy = updated.lastModifiedBy,
            updatedAt = updated.updatedAt,
            newVersion = updated.version,
            expectedVersion = payment.version,
            checksum = updated.checksum
        )
        if (rows == 0) throw OptimisticLockException("payment", payment.id)
        enqueueSync("payment", updated.id, "UPDATE", updated)
    }

    suspend fun delete(payment: Payment) {
        paymentDao.delete(payment)
        enqueueSync("payment", payment.id, "DELETE", payment)
    }

    suspend fun deleteClientPayments(clientId: String) {
        val payments = paymentDao.getClientPaymentsDirect(clientId)
        paymentDao.deleteClientPayments(clientId)
        payments.forEach { payment ->
            enqueueSync("payment", payment.id, "DELETE", payment)
        }
    }

    suspend fun deletePayment(clientId: String, month: String) {
        val normalizedMonth = normalizeMonth(month)
        val existing = paymentDao.getPayment(clientId, normalizedMonth)
        paymentDao.deletePayment(clientId, normalizedMonth)
        if (existing != null) {
            enqueueSync("payment", existing.id, "DELETE", existing)
        }
    }

    // === دوال مساعدة ===

    suspend fun markAsPaid(clientId: String, month: String, paymentDate: Long) {
        val normalizedMonth = normalizeMonth(month)
        val existing = paymentDao.getPayment(clientId, normalizedMonth) ?: return
        update(existing.copy(isPaid = true, paymentDate = paymentDate))
    }

    suspend fun markAsUnpaid(clientId: String, month: String) {
        val normalizedMonth = normalizeMonth(month)
        val existing = paymentDao.getPayment(clientId, normalizedMonth) ?: return
        update(existing.copy(isPaid = false, paymentDate = null))
    }

    suspend fun getPayment(clientId: String, month: String): Payment? {
        return paymentDao.getPayment(clientId, normalizeMonth(month))
    }

    suspend fun createPaymentIfNotExists(payment: Payment) {
        paymentDao.createPaymentIfNotExists(payment)
    }

    /**
     * إرجاع id لسجل Payment لعميل/شهر معيّن.
     * إذا لم يكن موجودًا يتم إنشاؤه بمبلغ معيّن ويُرجع id الجديد.
     */
    suspend fun getOrCreatePaymentId(
        clientId: String,
        month: String,
        amount: Double
    ): String {
        val normalizedMonth = normalizeMonth(month)
        val existing = getPayment(clientId, normalizedMonth)
        return if (existing != null) {
            existing.id
        } else {
            insert(
                Payment(
                    clientId = clientId,
                    month = normalizedMonth,
                    amount = amount,
                    isPaid = false,
                    paymentDate = null,
                    notes = ""
                )
            )
        }
    }

    /**
     * ضبط حالة الدفع (مدفوع/غير مدفوع) لسجل معين مع تاريخ اختياري.
     */
    suspend fun setPaidStatus(
        clientId: String,
        month: String,
        isPaid: Boolean,
        paymentDate: Long? = null
    ) {
        val normalizedMonth = normalizeMonth(month)
        val existing = getPayment(clientId, normalizedMonth)
        if (existing != null) {
            update(
                existing.copy(
                    isPaid = isPaid,
                    paymentDate = paymentDate
                )
            )
        }
    }

    // === دالة ذكية: إنشاء أو تحديث دفعة ===

    suspend fun createOrUpdatePayment(
        clientId: String,
        month: String,
        amount: Double,
        isPaid: Boolean = false,
        paymentDate: Long? = null,
        notes: String = ""
    ) {
        val normalizedMonth = normalizeMonth(month)
        val existing = getPayment(clientId, normalizedMonth)
        if (existing == null) {
            insert(
                Payment(
                    clientId = clientId,
                    month = normalizedMonth,
                    amount = amount,
                    isPaid = isPaid,
                    paymentDate = paymentDate,
                    notes = notes
                )
            )
        } else {
            update(
                existing.copy(
                    amount = amount,
                    isPaid = isPaid,
                    paymentDate = paymentDate,
                    notes = notes
                )
            )
        }
    }

    suspend fun getPaymentById(id: String): Payment? {
        return paymentDao.getPaymentById(id)
    }

    suspend fun updateFuturePaymentsAmount(clientId: String, fromMonth: String, newAmount: Double) {
        val normalizedMonth = normalizeMonth(fromMonth)
        val payments = paymentDao.getClientPaymentsDirect(clientId)
            .filter { it.month >= normalizedMonth }
        payments.forEach { payment ->
            try {
                update(payment.copy(amount = newAmount))
            } catch (e: OptimisticLockException) {
                android.util.Log.w("PaymentRepository",
                    "Version conflict updating payment ${payment.id} — will resolve on next sync")
            }
        }
    }

    suspend fun updateFutureUnpaidPaymentsAmount(clientId: String, fromMonth: String, newAmount: Double) {
        val normalizedMonth = normalizeMonth(fromMonth)
        val payments = paymentDao.getFutureUnpaidPayments(clientId, normalizedMonth)
        payments.forEach { payment ->
            try {
                update(payment.copy(amount = newAmount))
            } catch (e: OptimisticLockException) {
                android.util.Log.w("PaymentRepository",
                    "Version conflict updating payment ${payment.id} — will resolve on next sync")
            }
        }
    }

    suspend fun getFirstUnpaidMonthForClient(clientId: String): String? {
        return paymentDao.getFirstUnpaidMonthForClient(clientId)
    }

    suspend fun getPaymentsByMonthDirect(month: String): List<Payment> {
        return paymentDao.getPaymentsByMonthDirect(normalizeMonth(month))
    }

    suspend fun getClientsByIds(ids: List<String>): List<Client> {
        return clientDao.getClientsByIds(ids)
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
            android.util.Log.w("PaymentRepository", "Sync enqueue failed: ${e.message}")
        }
    }
}
