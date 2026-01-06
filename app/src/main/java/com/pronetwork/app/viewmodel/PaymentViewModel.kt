package com.pronetwork.app.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.pronetwork.app.data.ClientDatabase
import com.pronetwork.app.data.Payment
import com.pronetwork.app.data.PaymentTransaction
import com.pronetwork.app.repository.PaymentRepository
import com.pronetwork.app.repository.PaymentTransactionRepository
import kotlinx.coroutines.launch

// enum جديد لحالة الدفع
enum class PaymentStatus {
    UNPAID,
    PARTIAL,
    FULL
}

// موديل عرض حالة دفع العميل لكل شهر
data class ClientMonthPaymentUi(
    val month: String,
    val monthAmount: Double,
    val totalPaid: Double,
    val remaining: Double,
    val status: PaymentStatus
)

class PaymentViewModel(application: Application) : AndroidViewModel(application) {

    private val paymentRepository: PaymentRepository
    private val transactionRepository: PaymentTransactionRepository

    val allPayments: LiveData<List<Payment>>

    init {
        val db = ClientDatabase.getDatabase(application)
        val paymentDao = db.paymentDao()
        val transactionDao = db.paymentTransactionDao()

        paymentRepository = PaymentRepository(paymentDao)
        transactionRepository = PaymentTransactionRepository(transactionDao)

        allPayments = paymentRepository.allPayments
    }

    // ================== دالة جديدة: تحضير بيانات عرض حالة الدفع لكل شهر ==================

    fun getClientMonthPaymentsUi(clientId: Int): LiveData<List<ClientMonthPaymentUi>> {
        val result = MutableLiveData<List<ClientMonthPaymentUi>>()

        // نراقب قائمة الـ Payments لهذا العميل
        val source = paymentRepository.getClientPayments(clientId)

        // نستخدم MediatorLiveData حتى نربط قراءة Room مع حساباتنا
        val mediator = MediatorLiveData<List<ClientMonthPaymentUi>>()
        mediator.addSource(source) { paymentList ->
            viewModelScope.launch {
                val payments = paymentList ?: emptyList()
                val ids = payments.map { it.id }
                val totalsMap = transactionRepository.getTotalsForPayments(ids)

                val uiList = payments.map { p ->
                    val totalPaid = totalsMap[p.id] ?: 0.0
                    val remaining = (p.amount - totalPaid).coerceAtLeast(0.0)

                    val status = when {
                        totalPaid <= 0.0 -> PaymentStatus.UNPAID
                        totalPaid < p.amount -> PaymentStatus.PARTIAL
                        else -> PaymentStatus.FULL
                    }

                    ClientMonthPaymentUi(
                        month = p.month,
                        monthAmount = p.amount,
                        totalPaid = totalPaid,
                        remaining = remaining,
                        status = status
                    )
                }.sortedBy { it.month }

                mediator.postValue(uiList)
            }
        }

        // نعيد الـ Mediator كـ LiveData
        return mediator
    }

    // ================== إحصائيات شهرية عامة ==================

    // الشهر المختار للإحصائيات (مثل "2026-01")
    private val _selectedStatsMonth = MutableLiveData<String>()
    val selectedStatsMonth: LiveData<String> = _selectedStatsMonth

    // موديل الإحصائيات الشهرية
    data class MonthStats(
        val month: String,
        val paidCount: Int,
        val partiallyPaidCount: Int,
        val unpaidCount: Int,
        val totalPaidAmount: Double,
        val totalUnpaidAmount: Double
    )

    // LiveData للإحصائيات الشهرية الجاهزة
    val monthStats: LiveData<MonthStats> = _selectedStatsMonth.switchMap { month ->
        val paidCountLive = paymentRepository.getPaidCountByMonth(month)
        val unpaidCountLive = paymentRepository.getUnpaidCountByMonth(month)
        val totalPaidLive = paymentRepository.getTotalPaidAmountByMonth(month)
        val totalUnpaidLive = paymentRepository.getTotalUnpaidAmountByMonth(month)
        val paymentsLive = paymentRepository.getPaymentsByMonth(month)

        MediatorLiveData<MonthStats>().apply {
            var paidCount: Int? = null
            var unpaidCount: Int? = null
            var totalPaid: Double? = null
            var totalUnpaid: Double? = null
            var payments: List<Payment>? = null

            fun update() {
                val pc = paidCount
                val uc = unpaidCount
                val tp = totalPaid
                val tu = totalUnpaid
                val ps = payments
                if (pc != null && uc != null && tp != null && tu != null && ps != null) {
                    // العملاء المدفوع لهم كلياً حسب isPaid
                    val fullPaid = pc
                    // العملاء غير المدفوعين كلياً
                    val notPaid = uc
                    // العملاء ذوو الدفع الجزئي = الموجودون في payments لكن isPaid = false
                    // (لاحقاً يمكن تحسينها بالاعتماد على مجموع الترانزكشنات)
                    val partialPaid = ps.count { !it.isPaid && it.amount > 0.0 }

                    value = MonthStats(
                        month = month,
                        paidCount = fullPaid,
                        partiallyPaidCount = partialPaid,
                        unpaidCount = notPaid,
                        totalPaidAmount = tp,
                        totalUnpaidAmount = tu
                    )
                }
            }

            addSource(paidCountLive) {
                paidCount = it ?: 0
                update()
            }
            addSource(unpaidCountLive) {
                unpaidCount = it ?: 0
                update()
            }
            addSource(totalPaidLive) {
                totalPaid = it ?: 0.0
                update()
            }
            addSource(totalUnpaidLive) {
                totalUnpaid = it ?: 0.0
                update()
            }
            addSource(paymentsLive) {
                payments = it ?: emptyList()
                update()
            }
        }
    }

    fun setStatsMonth(month: String) {
        _selectedStatsMonth.value = month
    }

    // ================== استعلامات أساسية ==================

    fun getPaymentLive(clientId: Int, month: String): LiveData<Payment?> {
        return paymentRepository.getPaymentLive(clientId, month)
    }

    fun getClientPayments(clientId: Int): LiveData<List<Payment>> {
        return paymentRepository.getClientPayments(clientId)
    }

    fun getPaymentsByMonth(month: String): LiveData<List<Payment>> {
        return paymentRepository.getPaymentsByMonth(month)
    }

    fun getPaidCountByMonth(month: String): LiveData<Int> {
        return paymentRepository.getPaidCountByMonth(month)
    }

    fun getUnpaidCountByMonth(month: String): LiveData<Int> {
        return paymentRepository.getUnpaidCountByMonth(month)
    }

    fun getTotalPaidAmountByMonth(month: String): LiveData<Double?> {
        return paymentRepository.getTotalPaidAmountByMonth(month)
    }

    fun getTotalUnpaidAmountByMonth(month: String): LiveData<Double?> {
        return paymentRepository.getTotalUnpaidAmountByMonth(month)
    }

    // ================== CRUD على Payment ==================

    fun insert(payment: Payment) = viewModelScope.launch {
        paymentRepository.insert(payment)
    }

    fun update(payment: Payment) = viewModelScope.launch {
        paymentRepository.update(payment)
    }

    fun delete(payment: Payment) = viewModelScope.launch {
        paymentRepository.delete(payment)
    }

    fun deleteClientPayments(clientId: Int) = viewModelScope.launch {
        paymentRepository.deleteClientPayments(clientId)
    }

    fun deletePayment(clientId: Int, month: String) = viewModelScope.launch {
        paymentRepository.deletePayment(clientId, month)
    }

    // ================== دوال مساعدة للحصول على paymentId وقائمة الحركات ==================

    private suspend fun getPaymentIdForMonth(
        clientId: Int,
        month: String,
        monthAmount: Double
    ): Int {
        return paymentRepository.getOrCreatePaymentId(clientId, month, monthAmount)
    }

    fun getTransactionsForClientMonth(
        clientId: Int,
        month: String
    ): LiveData<List<PaymentTransaction>> {
        val result = MutableLiveData<List<PaymentTransaction>>()

        viewModelScope.launch {
            // احصل على الـ Payment الموجود لهذا الشهر
            val payment = paymentRepository.getPayment(clientId, month)
            if (payment != null) {
                // نستخدم الدالة الجديدة من الريبو التي ترجع List مباشرة
                val list = transactionRepository.getTransactionsForPaymentList(payment.id)
                result.postValue(list)
            } else {
                // إذا لا يوجد Payment أصلاً، لا توجد حركات
                result.postValue(emptyList())
            }
        }

        return result
    }

    // ================== حذف حركة فردية ==================

    fun deleteTransaction(transactionId: Int) = viewModelScope.launch {
        // 1) احصل على paymentId من transactionId
        val paymentId = transactionRepository.getPaymentIdByTransactionId(transactionId)
        if (paymentId == null) {
            // لا يوجد شيء نحذفه أو الحركة غير موجودة
            return@launch
        }

        // 2) نفّذ الحذف
        transactionRepository.deleteTransactionById(transactionId)

        // 3) أعد حساب مجموع المدفوع الجديد لهذا الـ Payment
        val newTotalPaid = transactionRepository.getTotalPaidForPayment(paymentId)

        // 4) احصل على سجل Payment لتعديل الحالة
        val payment = paymentRepository.getPaymentById(paymentId)
        if (payment != null) {
            // 5) حدّث حالة الدفع في جدول payments بناءً على المجموع الجديد
            when {
                newTotalPaid <= 0.0 -> {
                    // لا يوجد مدفوع بعد الحذف → الشهر غير مدفوع
                    paymentRepository.update(
                        payment.copy(
                            isPaid = false,
                            paymentDate = null
                        )
                    )
                }
                newTotalPaid < payment.amount -> {
                    // لا يزال مدفوع جزئيًا → isPaid = false مع إبقاء المبلغ
                    paymentRepository.update(
                        payment.copy(
                            isPaid = false,
                            paymentDate = null
                        )
                    )
                }
                else -> {
                    // ما زال مدفوع بالكامل → نحافظ على isPaid = true
                    paymentRepository.update(
                        payment.copy(
                            isPaid = true
                        )
                    )
                }
            }
        }
    }

    // ================== الدفع الكامل والجزئي ==================

    /**
     * دفع كامل: ينشئ/يضمن وجود Payment لهذا الشهر، ويسجل فقط المبلغ المتبقي للوصول إلى الدفع الكامل،
     * ثم يضبط isPaid = true مع تاريخ الدفع.
     */
    fun markFullPayment(clientId: Int, month: String, amount: Double) = viewModelScope.launch {
        val paymentId = paymentRepository.getOrCreatePaymentId(clientId, month, amount)

        // اجمع ما تم دفعه مسبقاً لهذا الشهر
        val alreadyPaid = transactionRepository.getTotalPaidForPayment(paymentId)
        val remaining = (amount - alreadyPaid).coerceAtLeast(0.0)

        // إذا بقي مبلغ للوصول إلى الدفع الكامل، نسجّل فقط الجزء المتبقي
        if (remaining > 0.0) {
            transactionRepository.insert(
                PaymentTransaction(
                    paymentId = paymentId,
                    amount = remaining,
                    notes = "استكمال دفع كامل"
                )
            )
        }

        // اضبط حالة الدفع كمدفوع بالكامل مع تاريخ الدفع
        val paymentDate = System.currentTimeMillis()
        paymentRepository.setPaidStatus(
            clientId = clientId,
            month = month,
            isPaid = true,
            paymentDate = paymentDate
        )
    }

    /**
     * دفع جزئي: يسجل Transaction بقيمة جزئية، ثم يحسب مجموع المدفوع
     * ويقرر هل يصبح مدفوع بالكامل أم يبقى جزئياً (isPaid = false لكن amount > 0).
     */
    fun addPartialPayment(clientId: Int, month: String, monthAmount: Double, partialAmount: Double) =
        viewModelScope.launch {
            val paymentId = paymentRepository.getOrCreatePaymentId(clientId, month, monthAmount)

            // أضف حركة جديدة بمبلغ جزئي
            transactionRepository.insert(
                PaymentTransaction(
                    paymentId = paymentId,
                    amount = partialAmount,
                    notes = "دفع جزئي"
                )
            )

            // احسب المجموع الكلي المدفوع لهذا الشهر
            val totalPaid = transactionRepository.getTotalPaidForPayment(paymentId)

            if (totalPaid >= monthAmount) {
                // أصبح مدفوع بالكامل
                val paymentDate = System.currentTimeMillis()
                paymentRepository.setPaidStatus(
                    clientId = clientId,
                    month = month,
                    isPaid = true,
                    paymentDate = paymentDate
                )
            } else {
                // مدفوع جزئياً: نضمن أن isPaid = false لكن نخزن المبلغ المطلوب (amount)
                val payment = paymentRepository.getPayment(clientId, month)
                if (payment != null) {
                    paymentRepository.update(
                        payment.copy(
                            amount = monthAmount,
                            isPaid = false
                        )
                    )
                }
            }
        }

    // ================== حركة عكسية (استرجاع / رصيد) ==================

    /**
     * حركة عكسية (استرجاع / رصيد): تسجل Transaction بمبلغ سالب.
     * تستخدم عند فصل الخدمة قبل نهاية الشهر أو عند رد جزء من المبلغ للعميل.
     */
    fun addReverseTransaction(
        clientId: Int,
        month: String,
        monthAmount: Double,
        refundAmount: Double,
        reason: String = "حركة عكسية / استرجاع"
    ) = viewModelScope.launch {
        // نضمن وجود Payment لهذا الشهر
        val paymentId = paymentRepository.getOrCreatePaymentId(clientId, month, monthAmount)

        // نسجل حركة جديدة بمبلغ سالب
        transactionRepository.insert(
            PaymentTransaction(
                paymentId = paymentId,
                amount = -refundAmount,        // المبلغ بالسالب
                notes = reason
            )
        )

        // بعد الحركة العكسية، نعيد حساب المجموع ونحدث حالة الدفع
        val newTotalPaid = transactionRepository.getTotalPaidForPayment(paymentId)
        val payment = paymentRepository.getPaymentById(paymentId)

        if (payment != null) {
            when {
                newTotalPaid <= 0.0 -> {
                    // لا يوجد مدفوع فعليًا بعد الاسترجاع
                    paymentRepository.update(
                        payment.copy(
                            isPaid = false,
                            paymentDate = null
                        )
                    )
                }
                newTotalPaid < payment.amount -> {
                    // أصبح مدفوع جزئيًا بعد الاسترجاع
                    paymentRepository.update(
                        payment.copy(
                            isPaid = false,
                            paymentDate = null
                        )
                    )
                }
                else -> {
                    // ما زال مدفوع بالكامل (نادر لكن ممكن إذا كان الاسترجاع صغير)
                    paymentRepository.update(
                        payment.copy(
                            isPaid = true
                        )
                    )
                }
            }
        }
    }

    // ================== تعديل مبلغ الاشتراك من شهر معيّن ==================

    /**
     * تعديل مبلغ الاشتراك الشهري للعميل اعتبارًا من شهر معين فما بعد.
     * لا يلمس الشهور التي عليها حركات.
     */
    fun applyNewMonthlyPriceFromMonth(
        clientId: Int,
        fromMonth: String,
        newAmount: Double
    ) = viewModelScope.launch {
        paymentRepository.updateFutureUnpaidPaymentsAmount(
            clientId = clientId,
            fromMonth = fromMonth,
            newAmount = newAmount
        )
    }

    /**
     * تعديل مبلغ الاشتراك الشهري للعميل اعتبارًا من أول شهر "نظيف" (لا حركات عليه) فما بعد.
     * مفيد عند تغيير السعر فقط لأشهر المستقبلية دون التأثير على الشهور الحالية.
     */
    fun applyNewMonthlyPriceFromNextUnpaidMonth(
        clientId: Int,
        newAmount: Double
    ) = viewModelScope.launch {
        val fromMonth = paymentRepository.getFirstUnpaidMonthForClient(clientId)
        if (fromMonth != null) {
            paymentRepository.updateFutureUnpaidPaymentsAmount(
                clientId = clientId,
                fromMonth = fromMonth,
                newAmount = newAmount
            )
        }
    }

    // ================== دالة مساعدة جديدة: إنشاء دفعات للعميل ==================

    /**
     * إنشاء دفعات شهرية للعميل.
     * الشهر الأول قد يكون بسعر مختلف (مثل 30 ريال)، والباقي بالسعر العادي (مثل 150 ريال).
     */
    fun createPaymentsForClient(
        clientId: Int,
        startMonth: String,
        endMonth: String?,
        amount: Double,
        monthOptions: List<String>,
        firstMonthAmount: Double? = null      // ✅ جديد: مبلغ الشهر الأول
    ) = viewModelScope.launch {
        val months = if (endMonth != null) {
            monthOptions.filter { month ->
                month >= startMonth && month < endMonth
            }
        } else {
            monthOptions.filter { it >= startMonth }
        }

        months.forEachIndexed { index, month ->
            val monthAmount = if (index == 0 && firstMonthAmount != null) {
                firstMonthAmount           // ✅ للشهر الأول استخدم القيمة الخاصة (مثل 30)
            } else {
                amount                     // ✅ باقي الشهور بالمبلغ الكامل (150)
            }

            paymentRepository.createOrUpdatePayment(
                clientId = clientId,
                month = month,
                amount = monthAmount,
                isPaid = false
            )
        }
    }

    // ================== دوال مساعدة قديمة (تبقى موجودة) ==================

    fun markAsPaid(clientId: Int, month: String, amount: Double) = viewModelScope.launch {
        // لأسباب التوافق، نستدعي الدفع الكامل هنا
        markFullPayment(clientId, month, amount)
    }

    fun markAsUnpaid(clientId: Int, month: String) = viewModelScope.launch {
        // احصل على سجل الـ Payment لهذا الشهر
        val payment = paymentRepository.getPayment(clientId, month)
        if (payment != null) {
            // احذف كل حركات الدفع المرتبطة بهذا الشهر
            transactionRepository.deleteTransactionsForPayment(payment.id)

            // أعد ضبط حالة الدفع في جدول payments
            paymentRepository.update(
                payment.copy(
                    isPaid = false,
                    paymentDate = null
                )
            )
        }
    }

    fun createOrUpdatePayment(
        clientId: Int,
        month: String,
        amount: Double,
        isPaid: Boolean = false,
        paymentDate: Long? = null,
        notes: String = ""
    ) = viewModelScope.launch {
        paymentRepository.createOrUpdatePayment(clientId, month, amount, isPaid, paymentDate, notes)
    }
}