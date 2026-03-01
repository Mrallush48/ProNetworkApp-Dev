package com.pronetwork.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pronetwork.app.data.ClientDatabase
import com.pronetwork.app.data.DailyBuildingCollection
import com.pronetwork.app.data.DailyBuildingDetailedUi


import com.pronetwork.app.data.Payment
import com.pronetwork.app.data.PaymentTransaction
import com.pronetwork.app.data.PaymentTransactionDao
import com.pronetwork.app.repository.PaymentRepository
import com.pronetwork.app.repository.PaymentTransactionRepository
import com.pronetwork.data.DailySummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val transactionRepository: PaymentTransactionRepository,
    private val db: ClientDatabase,
    private val statusResolver: PaymentStatusResolver,
    private val dailyCollectionBuilder: DailyCollectionBuilder
) : ViewModel() {

    val allPayments: LiveData<List<Payment>> = paymentRepository.allPayments

    // ─────────────────────────────────────────────────────────────────────────
    // Flow تفاعلي: بيانات عرض حالة الدفع لكل شهر لعميل واحد
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * يستمع لتغييرات جدولي [payments] و [payment_transactions]
     * ويُحدّث [totalPaid] / [remaining] / [status] تلقائياً.
     * مبني بنفس نمط [observeAllClientStatusesForMonth] لضمان التوحيد المعماري.
     */
    fun observeClientMonthPaymentsUi(clientId: Int): Flow<List<ClientMonthPaymentUi>> {
        val paymentsFlow = paymentRepository.observeClientPayments(clientId)

        return paymentsFlow.flatMapLatest { payments ->
            if (payments.isEmpty()) return@flatMapLatest flowOf(emptyList())

            val ids = payments.map { it.id }
            val totalsFlow = transactionRepository.observeTotalsForPayments(ids)
            val refundsFlow = transactionRepository.observePaymentIdsWithRefunds(ids)

            combine(totalsFlow, refundsFlow) { totalsList, refundIdsList ->
                val totalsMap = totalsList.associate { it.paymentId to it.totalPaid }
                val refundIds = refundIdsList.toSet()

                payments.map { p ->
                    val totalPaid = totalsMap[p.id] ?: 0.0
                    val remaining = (p.amount - totalPaid).coerceAtLeast(0.0)
                    val hasRefund = p.id in refundIds

                    ClientMonthPaymentUi(
                        month        = p.month,
                        monthAmount  = p.amount,
                        totalPaid    = totalPaid,
                        remaining    = remaining,
                        status       = statusResolver.resolve(totalPaid, p.amount, hasRefund)
                    )
                }.sortedBy { it.month }
            }
        }.distinctUntilChanged()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // تحصيل يومي لكل مبنى
    // ─────────────────────────────────────────────────────────────────────────

    fun getDailyBuildingCollectionsForDay(
        dayStartMillis: Long,
        dayEndMillis: Long
    ): LiveData<List<DailyBuildingCollection>> {
        val result = MutableLiveData<List<DailyBuildingCollection>>()
        viewModelScope.launch {
            result.postValue(
                transactionRepository.getDailyBuildingCollectionsForDay(dayStartMillis, dayEndMillis)
            )
        }
        return result
    }

    // ─────────────────────────────────────────────────────────────────────────
    // تحصيل يومي تفصيلي (عميل بعميل مع تفاصيل كل مبنى)
    // ─────────────────────────────────────────────────────────────────────────

    fun getDetailedDailyCollections(
        dayStartMillis: Long,
        dayEndMillis: Long,
        month: String
    ): LiveData<List<DailyBuildingDetailedUi>> {
        val result = MutableLiveData<List<DailyBuildingDetailedUi>>()

        viewModelScope.launch {
            val rawTransactions = transactionRepository.getDetailedDailyCollections(
                dayStartMillis, dayEndMillis
            )

            val allPaymentIds = rawTransactions.map { it.paymentId }.distinct()

            val refundPaymentIds = if (allPaymentIds.isNotEmpty()) {
                transactionRepository.getPaymentIdsWithRefunds(allPaymentIds).toSet()
            } else emptySet()

            val allTotalsPaidMap = if (allPaymentIds.isNotEmpty()) {
                transactionRepository.getTotalsForPayments(allPaymentIds)
            } else emptyMap()

            // ── بناء مجموعات المباني ──
            val buildingCollections = dailyCollectionBuilder.buildFromTransactions(
                rawTransactions = rawTransactions,
                allTotalsPaidMap = allTotalsPaidMap,
                refundPaymentIds = refundPaymentIds
            )

            // ── إضافة العملاء غير المدفوعين ──
            val paidClientIds = rawTransactions.map { it.clientId }.toSet()
            val finalResult = dailyCollectionBuilder.overlayUnpaidClients(
                buildingCollections = buildingCollections,
                month = month,
                paidClientIds = paidClientIds
            )

            result.postValue(finalResult)
        }

        return result
    }

    // ─────────────────────────────────────────────────────────────────────────
    // تحصيل يومي تفصيلي لمستخدم محدد
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * حركات يوم معيّن لمستخدم محدد — لـ Daily Collection الشخصي.
     * عند showAllClients = false: يعرض فقط حركات المستخدم الحالي.
     */
    fun getDetailedDailyCollectionsByUser(
        dayStartMillis: Long,
        dayEndMillis: Long,
        month: String,
        userId: String
    ): LiveData<List<DailyBuildingDetailedUi>> {
        val result = MutableLiveData<List<DailyBuildingDetailedUi>>()

        viewModelScope.launch {
            val rawTransactions = transactionRepository.getDetailedDailyCollectionsByUser(
                dayStartMillis, dayEndMillis, userId
            )

            val allPaymentIds = rawTransactions.map { it.paymentId }.distinct()

            val refundPaymentIds = if (allPaymentIds.isNotEmpty())
                transactionRepository.getPaymentIdsWithRefunds(allPaymentIds).toSet()
            else emptySet()

            val allTotalsPaidMap = if (allPaymentIds.isNotEmpty())
                transactionRepository.getTotalsForPayments(allPaymentIds)
            else emptyMap()

            val buildingCollections = dailyCollectionBuilder.buildFromTransactions(
                rawTransactions = rawTransactions,
                allTotalsPaidMap = allTotalsPaidMap,
                refundPaymentIds = refundPaymentIds
            )

            // ملاحظة: لا نضيف unpaid overlay لفلتر المستخدم
            result.postValue(buildingCollections)
        }

        return result
    }

    // ─────────────────────────────────────────────────────────────────────────
    // إحصائيات شهرية عامة
    // ─────────────────────────────────────────────────────────────────────────

    data class MonthStats(
        val month: String,
        val paidCount: Int,
        val partiallyPaidCount: Int,
        val settledCount: Int,
        val unpaidCount: Int,
        val totalPaidAmount: Double,
        val totalUnpaidAmount: Double,
        val settledAmount: Double
    )

    private val _selectedStatsMonth = MutableStateFlow("")
    val selectedStatsMonth: StateFlow<String> = _selectedStatsMonth

    /** Flow تفاعلي — يتحدث تلقائياً عند أي تغيير في payments أو transactions */
    val monthStats: StateFlow<MonthStats?> = _selectedStatsMonth
        .filter { it.isNotEmpty() }
        .flatMapLatest { month -> buildMonthStatsFlow(month) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Flow تفاعلي — إحصائيات الشهر السابق للمقارنة */
    val previousMonthStats: StateFlow<MonthStats?> = _selectedStatsMonth
        .filter { it.isNotEmpty() }
        .map { calculatePreviousMonth(it) }
        .flatMapLatest { prevMonth -> buildMonthStatsFlow(prevMonth) }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * بناء Flow تفاعلي لإحصائيات شهر معيّن.
     * يستمع لتغييرات جدولي [payments] و [payment_transactions] معاً
     * ويُعيد حساب الإحصائيات تلقائياً عند أي تغيير.
     */
    private fun buildMonthStatsFlow(month: String): Flow<MonthStats> {
        val paymentsFlow = paymentRepository.observePaymentsByMonth(month)

        return paymentsFlow.flatMapLatest { payments ->
            if (payments.isEmpty()) {
                return@flatMapLatest flowOf(
                    MonthStats(
                        month              = month,
                        paidCount          = 0,
                        partiallyPaidCount = 0,
                        settledCount       = 0,
                        unpaidCount        = 0,
                        totalPaidAmount    = 0.0,
                        totalUnpaidAmount  = 0.0,
                        settledAmount      = 0.0
                    )
                )
            }

            val ids        = payments.map { it.id }
            val totalsFlow = transactionRepository.observeTotalsForPayments(ids)
            val refundsFlow = transactionRepository.observePaymentIdsWithRefunds(ids)

            combine(totalsFlow, refundsFlow) { totalsList, refundIdsList ->
                val totalsMap = totalsList.associate { it.paymentId to it.totalPaid }
                val refundIds = refundIdsList.toSet()

                var fullPaidCount   = 0
                var partialPaidCount = 0
                var settledCount    = 0
                var unpaidCount     = 0
                var totalRemaining  = 0.0
                var totalPaidAmount = 0.0
                var settledAmount   = 0.0

                payments.forEach { p ->
                    val paidForThis      = totalsMap[p.id] ?: 0.0
                    val remainingForThis = (p.amount - paidForThis).coerceAtLeast(0.0)
                    val hasRefund        = p.id in refundIds
                    totalPaidAmount += paidForThis

                    when (statusResolver.resolve(paidForThis, p.amount, hasRefund)) {
                        PaymentStatus.UNPAID -> {
                            unpaidCount++
                            totalRemaining += p.amount
                        }
                        PaymentStatus.SETTLED -> {
                            settledCount++
                            settledAmount += paidForThis
                        }
                        PaymentStatus.PARTIAL -> {
                            partialPaidCount++
                            totalRemaining += remainingForThis
                        }
                        PaymentStatus.FULL -> fullPaidCount++
                    }
                }

                MonthStats(
                    month              = month,
                    paidCount          = fullPaidCount,
                    partiallyPaidCount = partialPaidCount,
                    settledCount       = settledCount,
                    unpaidCount        = unpaidCount,
                    totalPaidAmount    = totalPaidAmount,
                    totalUnpaidAmount  = totalRemaining,
                    settledAmount      = settledAmount
                )
            }
        }
    }

    private fun calculatePreviousMonth(month: String): String {
        return try {
            val parts = month.split("-")
            val year  = parts[0].toInt()
            val m     = parts[1].toInt()
            if (m == 1) String.format("%04d-%02d", year - 1, 12)
            else        String.format("%04d-%02d", year, m - 1)
        } catch (e: Exception) { month }
    }

    fun setStatsMonth(month: String) {
        _selectedStatsMonth.value = month
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Dashboard: آخر الحركات والعملاء الأكثر تأخراً
    // ─────────────────────────────────────────────────────────────────────────

    /** Flow تفاعلي: آخر الحركات — يتحدث تلقائياً */
    fun observeRecentTransactions(
        limit: Int = 10
    ): Flow<List<PaymentTransactionDao.DashboardRecentTransaction>> {
        return transactionRepository.observeRecentTransactions(limit)
    }

    /** Flow تفاعلي: العملاء الأكثر تأخراً — يتحدث تلقائياً */
    fun observeTopUnpaidClients(
        month: String,
        limit: Int = 5
    ): Flow<List<PaymentTransactionDao.DashboardUnpaidClient>> {
        return transactionRepository.observeTopUnpaidClients(month, limit)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Flow تفاعلي: حالة الدفع لكل العملاء (يتحدث تلقائياً)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * نسخة تفاعلية من getAllClientStatusesForMonth — تستمع لتغييرات
     * جدولي [payments] و [payment_transactions] وتُحدّث الـ UI تلقائياً.
     */
    fun observeAllClientStatusesForMonth(month: String): Flow<Map<Int, PaymentStatus>> {
        val paymentsFlow = paymentRepository.observePaymentsByMonth(month)

        return paymentsFlow.flatMapLatest { payments ->
            val ids = payments.map { it.id }
            if (ids.isEmpty()) return@flatMapLatest flowOf(emptyMap())

            val totalsFlow  = transactionRepository.observeTotalsForPayments(ids)
            val refundsFlow = transactionRepository.observePaymentIdsWithRefunds(ids)

            combine(totalsFlow, refundsFlow) { totalsList, refundIdsList ->
                val totalsMap = totalsList.associate { it.paymentId to it.totalPaid }
                val refundIds = refundIdsList.toSet()

                payments.associate { p ->
                    val totalPaid = totalsMap[p.id] ?: 0.0
                    val hasRefund = p.id in refundIds
                    p.clientId to statusResolver.resolve(totalPaid, p.amount, hasRefund)
                }
            }
        }.distinctUntilChanged()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // عدد العملاء غير المدفوعين الإجمالي لشهر معيّن (لكل المستخدمين)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * يحسب عدد العملاء حسب حالة الدفع الفعلية لشهر معيّن.
     * يُستخدم في شاشة Daily Collection لعرض الإحصائيات العالمية
     * بغض النظر عن فلتر المستخدم الحالي.
     */
    data class GlobalPaymentStatusCounts(
        val paidCount: Int = 0,
        val partialCount: Int = 0,
        val settledCount: Int = 0,
        val unpaidCount: Int = 0
    )

    fun observeGlobalPaymentStatusCounts(month: String): Flow<GlobalPaymentStatusCounts> {
        val paymentsFlow = paymentRepository.observePaymentsByMonth(month)
        return paymentsFlow.flatMapLatest { payments ->
            if (payments.isEmpty()) return@flatMapLatest flowOf(GlobalPaymentStatusCounts())
            val ids = payments.map { it.id }
            val totalsFlow = transactionRepository.observeTotalsForPayments(ids)
            val refundsFlow = transactionRepository.observePaymentIdsWithRefunds(ids)
            combine(totalsFlow, refundsFlow) { totalsList, refundIdsList ->
                val totalsMap = totalsList.associate { it.paymentId to it.totalPaid }
                val refundIds = refundIdsList.toSet()
                var paid = 0; var partial = 0; var settled = 0; var unpaid = 0
                payments.forEach { p ->
                    val totalPaid = totalsMap[p.id] ?: 0.0
                    val hasRefund = p.id in refundIds
                    when (statusResolver.resolve(totalPaid, p.amount, hasRefund)) {
                        PaymentStatus.FULL -> paid++
                        PaymentStatus.PARTIAL -> partial++
                        PaymentStatus.SETTLED -> settled++
                        PaymentStatus.UNPAID -> unpaid++
                    }
                }
                GlobalPaymentStatusCounts(paid, partial, settled, unpaid)
            }
        }.distinctUntilChanged()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // استعلامات أساسية
    // ─────────────────────────────────────────────────────────────────────────

    fun getPaymentLive(clientId: Int, month: String): LiveData<Payment?> =
        paymentRepository.getPaymentLive(clientId, month)

    fun getClientPayments(clientId: Int): LiveData<List<Payment>> =
        paymentRepository.getClientPayments(clientId)

    fun getPaymentsByMonth(month: String): LiveData<List<Payment>> =
        paymentRepository.getPaymentsByMonth(month)

    fun getPaidCountByMonth(month: String): LiveData<Int> =
        paymentRepository.getPaidCountByMonth(month)

    fun getUnpaidCountByMonth(month: String): LiveData<Int> =
        paymentRepository.getUnpaidCountByMonth(month)

    fun getTotalPaidAmountByMonth(month: String): LiveData<Double?> =
        paymentRepository.getTotalPaidAmountByMonth(month)

    fun getTotalUnpaidAmountByMonth(month: String): LiveData<Double?> =
        paymentRepository.getTotalUnpaidAmountByMonth(month)

    // ─────────────────────────────────────────────────────────────────────────
    // CRUD على Payment
    // ─────────────────────────────────────────────────────────────────────────

    fun insert(payment: Payment) =
        viewModelScope.launch { paymentRepository.insert(payment) }

    fun update(payment: Payment) =
        viewModelScope.launch { paymentRepository.update(payment) }

    fun delete(payment: Payment) =
        viewModelScope.launch { paymentRepository.delete(payment) }

    fun deleteClientPayments(clientId: Int) =
        viewModelScope.launch { paymentRepository.deleteClientPayments(clientId) }

    fun deletePayment(clientId: Int, month: String) =
        viewModelScope.launch { paymentRepository.deletePayment(clientId, month) }

    // ─────────────────────────────────────────────────────────────────────────
    // دوال مساعدة: paymentId وقائمة الحركات
    // ─────────────────────────────────────────────────────────────────────────

    @Suppress("unused")
    private suspend fun getPaymentIdForMonth(
        clientId: Int, month: String, monthAmount: Double
    ): Int = paymentRepository.getOrCreatePaymentId(clientId, month, monthAmount)

    fun getTransactionsForClientMonth(
        clientId: Int,
        month: String
    ): LiveData<List<PaymentTransaction>> {
        val result = MutableLiveData<List<PaymentTransaction>>()
        viewModelScope.launch {
            val payment = paymentRepository.getPayment(clientId, month)
            result.postValue(
                if (payment != null) transactionRepository.getTransactionsForPaymentList(payment.id)
                else emptyList()
            )
        }
        return result
    }

    // ─────────────────────────────────────────────────────────────────────────
    // حذف حركة فردية
    // ─────────────────────────────────────────────────────────────────────────

    fun deleteTransaction(transactionId: Int) = viewModelScope.launch {
        val paymentId = transactionRepository.getPaymentIdByTransactionId(transactionId)
            ?: return@launch

        transactionRepository.deleteTransactionById(transactionId)

        val newTotalPaid = transactionRepository.getTotalPaidForPayment(paymentId)
        val payment      = paymentRepository.getPaymentById(paymentId) ?: return@launch

        paymentRepository.update(
            when {
                newTotalPaid >= payment.amount -> payment.copy(isPaid = true)
                else                           -> payment.copy(isPaid = false, paymentDate = null)
            }
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // الدفع الكامل والجزئي
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * دفع كامل: ينشئ/يضمن وجود [Payment] لهذا الشهر، ويسجل فقط المبلغ المتبقي
     * للوصول إلى الدفع الكامل، ثم يضبط [isPaid] = true مع تاريخ الدفع.
     */
    fun markFullPayment(clientId: Int, month: String, amount: Double) = viewModelScope.launch {
        val paymentId   = paymentRepository.getOrCreatePaymentId(clientId, month, amount)
        val alreadyPaid = transactionRepository.getTotalPaidForPayment(paymentId)
        val remaining   = (amount - alreadyPaid).coerceAtLeast(0.0)

        if (remaining > 0.0) {
            transactionRepository.insert(
                PaymentTransaction(paymentId = paymentId, amount = remaining, notes = "full payment")
            )
        }

        paymentRepository.setPaidStatus(
            clientId    = clientId,
            month       = month,
            isPaid      = true,
            paymentDate = System.currentTimeMillis()
        )
    }

    /**
     * دفع جزئي: يسجل [PaymentTransaction] بقيمة جزئية، ثم يحسب مجموع المدفوع
     * ويقرر هل يصبح مدفوعاً بالكامل أم يبقى جزئياً.
     */
    fun addPartialPayment(
        clientId: Int, month: String, monthAmount: Double, partialAmount: Double
    ) = viewModelScope.launch {
        val paymentId = paymentRepository.getOrCreatePaymentId(clientId, month, monthAmount)

        transactionRepository.insert(
            PaymentTransaction(paymentId = paymentId, amount = partialAmount, notes = "partial payment")
        )

        val totalPaid = transactionRepository.getTotalPaidForPayment(paymentId)
        if (totalPaid >= monthAmount) {
            paymentRepository.setPaidStatus(
                clientId    = clientId,
                month       = month,
                isPaid      = true,
                paymentDate = System.currentTimeMillis()
            )
        } else {
            val payment = paymentRepository.getPayment(clientId, month)
            if (payment != null) {
                paymentRepository.update(payment.copy(amount = monthAmount, isPaid = false))
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // حركة عكسية (استرجاع / رصيد)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * تسجل [PaymentTransaction] بمبلغ سالب.
     * تُستخدم عند فصل الخدمة قبل نهاية الشهر أو عند رد جزء من المبلغ للعميل.
     */
    fun addReverseTransaction(
        clientId: Int, month: String, monthAmount: Double,
        refundAmount: Double, reason: String = "Refund"
    ) = viewModelScope.launch {
        val paymentId = paymentRepository.getOrCreatePaymentId(clientId, month, monthAmount)

        transactionRepository.insert(
            PaymentTransaction(paymentId = paymentId, amount = -refundAmount, notes = reason)
        )

        val newTotalPaid = transactionRepository.getTotalPaidForPayment(paymentId)
        val payment      = paymentRepository.getPaymentById(paymentId) ?: return@launch

        paymentRepository.update(
            when {
                newTotalPaid >= payment.amount -> payment.copy(isPaid = true)
                else                           -> payment.copy(isPaid = false, paymentDate = null)
            }
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // تعديل مبلغ الاشتراك من شهر معيّن
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * تعديل مبلغ الاشتراك الشهري اعتبارًا من شهر معيّن فما بعد.
     * لا يلمس الشهور التي عليها حركات.
     */
    fun applyNewMonthlyPriceFromMonth(
        clientId: Int, fromMonth: String, newAmount: Double
    ) = viewModelScope.launch {
        paymentRepository.updateFutureUnpaidPaymentsAmount(
            clientId  = clientId,
            fromMonth = fromMonth,
            newAmount = newAmount
        )
    }

    /**
     * تعديل مبلغ الاشتراك اعتبارًا من أول شهر "نظيف" (لا حركات عليه) فما بعد.
     * مفيد عند تغيير السعر للأشهر المستقبلية دون التأثير على الأشهر الحالية.
     */
    fun applyNewMonthlyPriceFromNextUnpaidMonth(
        clientId: Int, newAmount: Double
    ) = viewModelScope.launch {
        val fromMonth = paymentRepository.getFirstUnpaidMonthForClient(clientId) ?: return@launch
        paymentRepository.updateFutureUnpaidPaymentsAmount(
            clientId  = clientId,
            fromMonth = fromMonth,
            newAmount = newAmount
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // إنشاء دفعات للعميل
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * إنشاء دفعات شهرية للعميل.
     * الشهر الأول قد يكون بسعر مختلف (مثل 30 ريال)، والباقي بالسعر العادي.
     *
     * @param firstMonthAmount إذا كان غير null يُطبَّق على الشهر الأول فقط.
     */
    fun createPaymentsForClient(
        clientId: Int,
        startMonth: String,
        endMonth: String?,
        amount: Double,
        monthOptions: List<String>,
        firstMonthAmount: Double? = null
    ) = viewModelScope.launch {
        val months = monthOptions
            .filter { it >= startMonth && (endMonth == null || it < endMonth) }
            .sorted()

        months.forEachIndexed { index, month ->
            val monthAmount = if (index == 0 && firstMonthAmount != null) firstMonthAmount else amount
            paymentRepository.createOrUpdatePayment(
                clientId = clientId,
                month    = month,
                amount   = monthAmount,
                isPaid   = false
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Daily Summary
    // ─────────────────────────────────────────────────────────────────────────

    fun getDailySummary(dateString: String): Flow<DailySummary?> =
        transactionRepository.getDailySummary(dateString)

    // ─────────────────────────────────────────────────────────────────────────
    // دوال مساعدة (Deprecated — احتفظ بها للتوافقية)
    // ─────────────────────────────────────────────────────────────────────────

    @Deprecated("استخدم markFullPayment بدلاً من هذه الدالة", ReplaceWith("markFullPayment(clientId, month, amount)"))
    fun markAsPaid(clientId: Int, month: String, amount: Double) = viewModelScope.launch {
        markFullPayment(clientId, month, amount)
    }

    fun markAsUnpaid(clientId: Int, month: String) = viewModelScope.launch {
        val payment = paymentRepository.getPayment(clientId, month) ?: return@launch
        transactionRepository.deleteTransactionsForPayment(payment.id)
        paymentRepository.update(payment.copy(isPaid = false, paymentDate = null))
    }

    fun createOrUpdatePayment(
        clientId: Int, month: String, amount: Double,
        isPaid: Boolean = false, paymentDate: Long? = null, notes: String = ""
    ) = viewModelScope.launch {
        paymentRepository.createOrUpdatePayment(clientId, month, amount, isPaid, paymentDate, notes)
    }
}
