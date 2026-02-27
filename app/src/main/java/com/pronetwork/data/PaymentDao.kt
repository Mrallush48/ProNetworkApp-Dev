package com.pronetwork.app.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {

    @Query("SELECT * FROM payments WHERE clientId = :clientId")
    suspend fun getClientPaymentsDirect(clientId: Int): List<Payment>

    @Query("SELECT * FROM payments WHERE clientId = :clientId AND month >= :fromMonth AND isPaid = 0")
    suspend fun getFutureUnpaidPayments(clientId: Int, fromMonth: String): List<Payment>


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

    // جلب كل الدفعات في شهر معيّن (suspend - للتقارير)
    @Query("SELECT * FROM payments WHERE month = :month")
    suspend fun getPaymentsByMonthDirect(month: String): List<Payment>

    // جلب كل الدفعات (suspend - للتقارير)
    @Query("SELECT * FROM payments ORDER BY month DESC")
    suspend fun getAllPaymentsDirect(): List<Payment>


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

    // دالة جديدة: جلب Payment حسب المعرف
    @Query("SELECT * FROM payments WHERE id = :id LIMIT 1")
    suspend fun getPaymentById(id: Int): Payment?

    // دالة جديدة: تحديث مبالغ الشهور المستقبلية (القديمة)
    @Query(
        """
        UPDATE payments
        SET amount = :newAmount
        WHERE clientId = :clientId
          AND month >= :fromMonth
    """
    )
    suspend fun updateFuturePaymentsAmount(
        clientId: Int,
        fromMonth: String,
        newAmount: Double
    )

    // دالة جديدة: تحديث مبالغ الشهور المستقبلية (الجديدة)
    @Query(
        """
        UPDATE payments
        SET amount = :newAmount
        WHERE clientId = :clientId
          AND month >= :fromMonth
          AND id NOT IN (
              SELECT DISTINCT paymentId
              FROM payment_transactions
              WHERE clientId = :clientId
          )
    """
    )
    suspend fun updateFutureUnpaidPaymentsAmount(
        clientId: Int,
        fromMonth: String,
        newAmount: Double
    )

    // دالة جديدة: جلب أول شهر غير مسجّل عليه أي حركات لعميل معيّن
    @Query(
        """
        SELECT month
        FROM payments
        WHERE clientId = :clientId
          AND id NOT IN (
              SELECT DISTINCT paymentId
              FROM payment_transactions
              WHERE clientId = :clientId
          )
        ORDER BY month
        LIMIT 1
    """
    )
    suspend fun getFirstUnpaidMonthForClient(clientId: Int): String?

    // ================== Flow-based reactive queries (real-time UI updates) ==================

    /**
     * Flow تفاعلي: جلب كل الدفعات في شهر معيّن — يتحدث تلقائياً عند أي تغيير في جدول payments.
     * يُستخدم مع observeTotalsForPayments لحساب حالات الدفع بشكل تفاعلي.
     */
    @Query("SELECT * FROM payments WHERE month = :month")
    fun observePaymentsByMonth(month: String): Flow<List<Payment>>

    /**
     * Flow تفاعلي: جلب كل دفعات عميل معيّن — يتحدث تلقائياً.
     * يُستخدم في شاشة تفاصيل العميل لتحديث فوري.
     */
    @Query("SELECT * FROM payments WHERE clientId = :clientId ORDER BY month DESC")
    fun observeClientPayments(clientId: Int): Flow<List<Payment>>
}