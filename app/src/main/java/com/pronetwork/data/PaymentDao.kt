package com.pronetwork.app.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface PaymentDao {

    // === استعلامات القراءة ===

    // جلب كل الدفعات
    @Query("SELECT * FROM payments ORDER BY month DESC")
    fun getAllPayments(): LiveData<List<Payment>>

    // جلب دفعة معيّنة لعميل في شهر محدد
    @Query("SELECT * FROM payments WHERE clientId = :clientId AND month = :month LIMIT 1")
    suspend fun getPayment(clientId: Int, month: String): Payment?

    // جلب دفعة معيّنة لعميل في شهر محدد (LiveData)
    @Query("SELECT * FROM payments WHERE clientId = :clientId AND month = :month LIMIT 1")
    fun getPaymentLive(clientId: Int, month: String): LiveData<Payment?>

    // جلب كل دفعات عميل معيّن
    @Query("SELECT * FROM payments WHERE clientId = :clientId ORDER BY month DESC")
    fun getClientPayments(clientId: Int): LiveData<List<Payment>>

    // جلب كل الدفعات في شهر معيّن
    @Query("SELECT * FROM payments WHERE month = :month")
    fun getPaymentsByMonth(month: String): LiveData<List<Payment>>

    // جلب الدفعات المدفوعة في شهر معيّن
    @Query("SELECT * FROM payments WHERE month = :month AND isPaid = 1")
    fun getPaidPaymentsByMonth(month: String): LiveData<List<Payment>>

    // جلب الدفعات غير المدفوعة في شهر معيّن
    @Query("SELECT * FROM payments WHERE month = :month AND isPaid = 0")
    fun getUnpaidPaymentsByMonth(month: String): LiveData<List<Payment>>

    // عدد الدفعات المدفوعة في شهر معيّن
    @Query("SELECT COUNT(*) FROM payments WHERE month = :month AND isPaid = 1")
    fun getPaidCountByMonth(month: String): LiveData<Int>

    // عدد الدفعات غير المدفوعة في شهر معيّن
    @Query("SELECT COUNT(*) FROM payments WHERE month = :month AND isPaid = 0")
    fun getUnpaidCountByMonth(month: String): LiveData<Int>

    // مجموع المبالغ المدفوعة في شهر معيّن
    @Query("SELECT SUM(amount) FROM payments WHERE month = :month AND isPaid = 1")
    fun getTotalPaidAmountByMonth(month: String): LiveData<Double?>

    // مجموع المبالغ غير المدفوعة في شهر معيّن
    @Query("SELECT SUM(amount) FROM payments WHERE month = :month AND isPaid = 0")
    fun getTotalUnpaidAmountByMonth(month: String): LiveData<Double?>

    // === استعلامات الكتابة ===

    // إضافة دفعة جديدة
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(payment: Payment): Long

    // تحديث دفعة موجودة
    @Update
    suspend fun update(payment: Payment)

    // حذف دفعة
    @Delete
    suspend fun delete(payment: Payment)

    // حذف كل دفعات عميل معيّن
    @Query("DELETE FROM payments WHERE clientId = :clientId")
    suspend fun deleteClientPayments(clientId: Int)

    // حذف دفعة معيّنة لعميل في شهر محدد
    @Query("DELETE FROM payments WHERE clientId = :clientId AND month = :month")
    suspend fun deletePayment(clientId: Int, month: String)

    // === دوال مساعدة ===

    // تأكيد الدفع
    @Query("UPDATE payments SET isPaid = 1, paymentDate = :paymentDate WHERE clientId = :clientId AND month = :month")
    suspend fun markAsPaid(clientId: Int, month: String, paymentDate: Long)

    // التراجع عن الدفع
    @Query("UPDATE payments SET isPaid = 0, paymentDate = NULL WHERE clientId = :clientId AND month = :month")
    suspend fun markAsUnpaid(clientId: Int, month: String)

    // إنشاء دفعة تلقائية لعميل في شهر معيّن (إذا لم تكن موجودة)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun createPaymentIfNotExists(payment: Payment)
}
