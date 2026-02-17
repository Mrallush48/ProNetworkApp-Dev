package com.pronetwork.app.repository

import androidx.lifecycle.LiveData
import com.pronetwork.app.data.ClientDao
import com.pronetwork.app.data.Payment
import com.pronetwork.app.data.PaymentDao
import com.pronetwork.app.data.Client


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

class PaymentRepository(
    private val paymentDao: PaymentDao,
    private val clientDao: ClientDao
) {


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

    // === استعلامات الكتابة ===

    suspend fun insert(payment: Payment): Long {
        return paymentDao.insert(payment)
    }

    suspend fun update(payment: Payment) {
        paymentDao.update(payment)
    }

    suspend fun delete(payment: Payment) {
        paymentDao.delete(payment)
    }

    suspend fun deleteClientPayments(clientId: Int) {
        paymentDao.deleteClientPayments(clientId)
    }

    suspend fun deletePayment(clientId: Int, month: String) {
        paymentDao.deletePayment(clientId, normalizeMonth(month))
    }

    // === دوال مساعدة ===

    suspend fun markAsPaid(clientId: Int, month: String, paymentDate: Long) {
        paymentDao.markAsPaid(clientId, normalizeMonth(month), paymentDate)
    }

    suspend fun markAsUnpaid(clientId: Int, month: String) {
        paymentDao.markAsUnpaid(clientId, normalizeMonth(month))
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
        paymentDao.updateFutureUnpaidPaymentsAmount(
            clientId = clientId,
            fromMonth = normalizeMonth(fromMonth),
            newAmount = newAmount
        )
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


}