package com.pronetwork.app.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.pronetwork.app.data.Payment
import com.pronetwork.app.data.ClientDatabase
import com.pronetwork.app.repository.PaymentRepository
import kotlinx.coroutines.launch

class PaymentViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PaymentRepository
    val allPayments: LiveData<List<Payment>>

    init {
        val paymentDao = ClientDatabase.getDatabase(application).paymentDao()
        repository = PaymentRepository(paymentDao)
        allPayments = repository.allPayments
    }

    // === دوال للحصول على حالة دفعة معيّنة ===

    fun getPaymentLive(clientId: Int, month: String): LiveData<Payment?> {
        return repository.getPaymentLive(clientId, month)
    }

    fun getClientPayments(clientId: Int): LiveData<List<Payment>> {
        return repository.getClientPayments(clientId)
    }

    fun getPaymentsByMonth(month: String): LiveData<List<Payment>> {
        return repository.getPaymentsByMonth(month)
    }

    fun getPaidCountByMonth(month: String): LiveData<Int> {
        return repository.getPaidCountByMonth(month)
    }

    fun getUnpaidCountByMonth(month: String): LiveData<Int> {
        return repository.getUnpaidCountByMonth(month)
    }

    fun getTotalPaidAmountByMonth(month: String): LiveData<Double?> {
        return repository.getTotalPaidAmountByMonth(month)
    }

    fun getTotalUnpaidAmountByMonth(month: String): LiveData<Double?> {
        return repository.getTotalUnpaidAmountByMonth(month)
    }

    // === دوال الكتابة ===

    fun insert(payment: Payment) = viewModelScope.launch {
        repository.insert(payment)
    }

    fun update(payment: Payment) = viewModelScope.launch {
        repository.update(payment)
    }

    fun delete(payment: Payment) = viewModelScope.launch {
        repository.delete(payment)
    }

    fun deleteClientPayments(clientId: Int) = viewModelScope.launch {
        repository.deleteClientPayments(clientId)
    }

    fun deletePayment(clientId: Int, month: String) = viewModelScope.launch {
        repository.deletePayment(clientId, month)
    }

    // === دوال تأكيد/تراجع الدفع (مصلحة) ===

    fun markAsPaid(clientId: Int, month: String, amount: Double) = viewModelScope.launch {
        val paymentDate = System.currentTimeMillis()
        repository.markAsPaid(clientId, month, paymentDate)
    }

    fun markAsUnpaid(clientId: Int, month: String) = viewModelScope.launch {
        repository.markAsUnpaid(clientId, month)
    }

    // === دالة ذكية: إنشاء أو تحديث دفعة ===
    fun createOrUpdatePayment(
        clientId: Int,
        month: String,
        amount: Double,
        isPaid: Boolean = false,
        paymentDate: Long? = null,
        notes: String = ""
    ) = viewModelScope.launch {
        repository.createOrUpdatePayment(clientId, month, amount, isPaid, paymentDate, notes)
    }

    // === دالة مساعدة: إنشاء دفعات تلقائية لعميل جديد ===
    fun createPaymentsForClient(
        clientId: Int,
        startMonth: String,
        endMonth: String?,
        amount: Double,
        monthOptions: List<String>
    ) = viewModelScope.launch {
        // تحديد نطاق الشهور
        val months = if (endMonth != null) {
            monthOptions.filter { month ->
                month >= startMonth && month < endMonth
            }
        } else {
            monthOptions.filter { it >= startMonth }
        }

        // إنشاء دفعة لكل شهر
        months.forEach { month ->
            repository.createOrUpdatePayment(
                clientId = clientId,
                month = month,
                amount = amount,
                isPaid = false
            )
        }
    }
}
