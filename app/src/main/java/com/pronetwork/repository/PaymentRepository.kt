package com.pronetwork.app.repository

import androidx.lifecycle.LiveData
import com.pronetwork.app.data.Payment
import com.pronetwork.app.data.PaymentDao

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

class PaymentRepository(private val paymentDao: PaymentDao) {

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
}
