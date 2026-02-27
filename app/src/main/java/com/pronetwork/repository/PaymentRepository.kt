package com.pronetwork.app.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.pronetwork.app.data.ClientDao
import com.pronetwork.app.data.Payment
import com.pronetwork.app.data.PaymentDao
import com.pronetwork.app.data.Client
import com.pronetwork.app.network.SyncEngine
import com.pronetwork.app.network.SyncWorker
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

    fun getPaymentLive(clientId: Int, month: String): LiveData<Payment?> {
        return paymentDao.getPaymentLive(clientId, normalizeMonth(month))
    }

    fun getClientPayments(clientId: Int): LiveData<List<Payment>> {
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

    fun getTotalPaidAmountByMonth(month: String): LiveData<Double?> {
        return paymentDao.getTotalPaidAmountByMonth(normalizeMonth(month))
    }

    fun getTotalUnpaidAmountByMonth(month: String): LiveData<Double?> {
        return paymentDao.getTotalUnpaidAmountByMonth(normalizeMonth(month))
    }

    // ================== Flow-based reactive queries ==================

    /**
     * Flow تفاعلي: جلب كل الدفعات في شهر معيّن — يتحدث تلقائياً.
     */
    fun observePaymentsByMonth(month: String): Flow<List<Payment>> {
        return paymentDao.observePaymentsByMonth(normalizeMonth(month))
    }

    /**
     * Flow تفاعلي: جلب كل دفعات عميل معيّن — يتحدث تلقائياً.
     */
    fun observeClientPayments(clientId: Int): Flow<List<Payment>> {
        return paymentDao.observeClientPayments(clientId)
    }

    // === استعلامات الكتابة ===

    suspend fun insert(payment: Payment): Long {
        val rowId = paymentDao.insert(payment)
        enqueueSync("payment", rowId.toInt(), "CREATE", payment.copy(id = rowId.toInt()))
        return rowId
    }

    suspend fun update(payment: Payment) {
        paymentDao.update(payment)
        enqueueSync("payment", payment.id, "UPDATE", payment)
    }

    suspend fun delete(payment: Payment) {
        paymentDao.delete(payment)
        enqueueSync("payment", payment.id, "DELETE", payment)
    }

    suspend fun deleteClientPayments(clientId: Int) {
        // Get payments before deleting to enqueue sync
        val payments = paymentDao.getClientPaymentsDirect(clientId)
        paymentDao.deleteClientPayments(clientId)
        payments.forEach { payment ->
            enqueueSync("payment", payment.id, "DELETE", payment)
        }
    }

    suspend fun deletePayment(clientId: Int, month: String) {
        val normalizedMonth = normalizeMonth(month)
        val existing = paymentDao.getPayment(clientId, normalizedMonth)
        paymentDao.deletePayment(clientId, normalizedMonth)
        if (existing != null) {
            enqueueSync("payment", existing.id, "DELETE", existing)
        }
    }

    // === دوال مساعدة ===

    suspend fun markAsPaid(clientId: Int, month: String, paymentDate: Long) {
        val normalizedMonth = normalizeMonth(month)
        paymentDao.markAsPaid(clientId, normalizedMonth, paymentDate)
        // Sync the updated payment
        val updated = paymentDao.getPayment(clientId, normalizedMonth)
        if (updated != null) {
            enqueueSync("payment", updated.id, "UPDATE", updated)
        }
    }

    suspend fun markAsUnpaid(clientId: Int, month: String) {
        val normalizedMonth = normalizeMonth(month)
        paymentDao.markAsUnpaid(clientId, normalizedMonth)
        // Sync the updated payment
        val updated = paymentDao.getPayment(clientId, normalizedMonth)
        if (updated != null) {
            enqueueSync("payment", updated.id, "UPDATE", updated)
        }
    }

    suspend fun getPayment(clientId: Int, month: String): Payment? {
        return paymentDao.getPayment(clientId, normalizeMonth(month))
    }

    suspend fun createPaymentIfNotExists(payment: Payment) {
        paymentDao.createPaymentIfNotExists(payment)
    }

    /**
     * إرجاع id لسجل Payment لعميل/شهر معيّن.
     * إذا لم يكن موجودًا يتم إنشاؤه بمبلغ معيّن ويُرجع id الجديد.
     * مهم لاستخدامه مع جدول payment_transactions.
     */
    suspend fun getOrCreatePaymentId(
        clientId: Int,
        month: String,
        amount: Double
    ): Int {
        val normalizedMonth = normalizeMonth(month)
        val existing = getPayment(clientId, normalizedMonth)
        return if (existing != null) {
            existing.id
        } else {
            val newId = insert(
                Payment(
                    clientId = clientId,
                    month = normalizedMonth,
                    amount = amount,
                    isPaid = false,
                    paymentDate = null,
                    notes = ""
                )
            )
            newId.toInt()
        }
    }

    /**
     * ضبط حالة الدفع (مدفوع/غير مدفوع) لسجل معين مع تاريخ اختياري.
     * يمكن استخدامها بعد حساب المدفوع الكلي من جدول الحركات الجزئية.
     */
    suspend fun setPaidStatus(
        clientId: Int,
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
        clientId: Int,
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

    // دالة جديدة: جلب Payment حسب المعرف
    suspend fun getPaymentById(id: Int): Payment? {
        return paymentDao.getPaymentById(id)
    }

    // دالة جديدة: تحديث مبالغ الشهور المستقبلية
    suspend fun updateFuturePaymentsAmount(
        clientId: Int,
        fromMonth: String,
        newAmount: Double
    ) {
        paymentDao.updateFuturePaymentsAmount(clientId, normalizeMonth(fromMonth), newAmount)
    }

    // دالة جديدة: تحديث مبالغ الشهور المستقبلية (الجديدة)
    suspend fun updateFutureUnpaidPaymentsAmount(
        clientId: Int,
        fromMonth: String,
        newAmount: Double
    ) {
        val normalizedMonth = normalizeMonth(fromMonth)
        paymentDao.updateFutureUnpaidPaymentsAmount(
            clientId = clientId,
            fromMonth = normalizedMonth,
            newAmount = newAmount
        )
        // Sync all affected payments
        val updatedPayments = paymentDao.getFutureUnpaidPayments(clientId, normalizedMonth)
        updatedPayments.forEach { payment ->
            enqueueSync("payment", payment.id, "UPDATE", payment)
        }
    }

    // دالة جديدة: جلب أول شهر غير مسجّل عليه أي حركات لعميل معيّن
    suspend fun getFirstUnpaidMonthForClient(clientId: Int): String? {
        return paymentDao.getFirstUnpaidMonthForClient(clientId)
    }

    suspend fun getPaymentsByMonthDirect(month: String): List<Payment> {
        return paymentDao.getPaymentsByMonthDirect(normalizeMonth(month))
    }

    suspend fun getClientsByIds(ids: List<Int>): List<Client> {
        return clientDao.getClientsByIds(ids)
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
            android.util.Log.w("PaymentRepository", "Sync enqueue failed: ${e.message}")
        }
    }
}
