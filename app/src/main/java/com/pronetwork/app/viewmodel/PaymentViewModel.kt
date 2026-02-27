package com.pronetwork.app.viewmodel

import androidx.lifecycle.*
import com.pronetwork.app.data.ClientDatabase
import com.pronetwork.app.data.DailyBuildingCollection
import com.pronetwork.app.data.DailyBuildingDetailedUi
import com.pronetwork.app.data.DailyClientCollection
import com.pronetwork.app.data.Payment
import com.pronetwork.app.data.PaymentTransaction
import com.pronetwork.app.repository.PaymentRepository
import com.pronetwork.app.repository.PaymentTransactionRepository
import kotlinx.coroutines.launch
import com.pronetwork.app.data.DailyTransactionItem
import com.pronetwork.app.data.PaymentTransactionDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import com.pronetwork.data.DailySummary
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest


// enum جديد لحالة الدفع
enum class PaymentStatus {
    UNPAID,
    PARTIAL,
    SETTLED,
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

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val transactionRepository: PaymentTransactionRepository,
    private val db: ClientDatabase
) : ViewModel() {

    val allPayments: LiveData<List<Payment>> = paymentRepository.allPayments

    // ================== منطق مشترك لحساب حالة الدفع ==================

    /**
     * حساب حالة الدفع من المبلغ المدفوع والمبلغ المطلوب ووجود استرجاع.
     * تُستخدم كمصدر وحيد للحقيقة في كل مكان يحتاج تحديد PaymentStatus.
     */
    private fun resolvePaymentStatus(
        totalPaid: Double,
        monthAmount: Double,
        hasRefund: Boolean
    ): PaymentStatus = when {
        totalPaid <= 0.0 -> PaymentStatus.UNPAID
        totalPaid < monthAmount && hasRefund -> PaymentStatus.SETTLED
        totalPaid < monthAmount -> PaymentStatus.PARTIAL
        else -> PaymentStatus.FULL
    }

    // ================== Flow تفاعلي: بيانات عرض حالة الدفع لكل شهر لعميل واحد ==================

    /**
     * نسخة تفاعلية — تستمع لتغييرات جدولي payments و payment_transactions
     * وتحدّث totalPaid / remaining / status تلقائياً.
     * تُبنى بنفس نمط observeAllClientStatusesForMonth لضمان التوحيد المعماري.
     */
    fun observeClientMonthPaymentsUi(clientId: Int): Flow<List<ClientMonthPaymentUi>> {
        val paymentsFlow = paymentRepository.observeClientPayments(clientId)

        return paymentsFlow.flatMapLatest { payments ->
            if (payments.isEmpty()) {
                return@flatMapLatest kotlinx.coroutines.flow.flowOf(emptyList<ClientMonthPaymentUi>())
            }

            val ids = payments.map { it.id }
            val totalsFlow = transactionRepository.observeTotalsForPayments(ids)
            val refundsFlow = transactionRepository.observePaymentIdsWithRefunds(ids)

            combine(totalsFlow, refundsFlow) { totalsList, refundIdsList ->
                val totalsMap = totalsList.associate { it.paymentId to it.totalPaid }
                val refundIds = refundIdsList.toSet()

                payments.map { p ->
                    val totalPaid = totalsMap[p.id] ?: 0.0
                    val remaining = (p.amount - totalPaid).coerceAtLeast(0.0)
                    val hasRefund = refundIds.contains(p.id)

                    ClientMonthPaymentUi(
                        month = p.month,
                        monthAmount = p.amount,
                        totalPaid = totalPaid,
                        remaining = remaining,
                        status = resolvePaymentStatus(totalPaid, p.amount, hasRefund)
                    )
                }.sortedBy { it.month }
            }
        }.distinctUntilChanged()
    }

    // ================== تحصيل يومي لكل مبنى ==================

    fun getDailyBuildingCollectionsForDay(
        dayStartMillis: Long,
        dayEndMillis: Long
    ): LiveData<List<DailyBuildingCollection>> {
        val result = MutableLiveData<List<DailyBuildingCollection>>()
        viewModelScope.launch {
            val list = transactionRepository.getDailyBuildingCollectionsForDay(dayStartMillis, dayEndMillis)
            result.postValue(list)
        }
        return result
    }

    // ================== تحصيل يومي تفصيلي (عميل بعميل مع تفاصيل كل مبنى) ==================

    fun getDetailedDailyCollections(
        dayStartMillis: Long,
        dayEndMillis: Long,
        month: String
    ): LiveData<List<DailyBuildingDetailedUi>> {
        val result = MutableLiveData<List<DailyBuildingDetailedUi>>()
        viewModelScope.launch {
            val rawTransactions = transactionRepository.getDetailedDailyCollections(dayStartMillis, dayEndMillis)

            val allPaymentIds = rawTransactions.map { it.paymentId }.distinct()
            val refundPaymentIds = if (allPaymentIds.isNotEmpty()) {
                transactionRepository.getPaymentIdsWithRefunds(allPaymentIds).toSet()
            } else emptySet()

            val allTotalsPaidMap = if (allPaymentIds.isNotEmpty()) {
                transactionRepository.getTotalsForPayments(allPaymentIds)
            } else emptyMap()

            val timeFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())

            val buildingGroups = rawTransactions.groupBy { it.buildingId }
            val buildingCollections = buildingGroups.map { (buildingId, txList) ->
                val buildingName = txList.first().buildingName
                val clientGroups = txList.groupBy { it.clientId }
                val clients = clientGroups.map { (clientId, clientTxs) ->
                    val firstTx = clientTxs.first()
                    val totalPaid = clientTxs.sumOf { it.paidAmount }
                    val lastTxTime = clientTxs.maxOf { it.transactionDate }
                    val allNotes = clientTxs.map { it.notes }.filter { it.isNotBlank() }.distinct().joinToString(" | ")

                    val txItems = clientTxs.map { tx ->
                        val txType = if (tx.paidAmount < 0) "Refund" else "Payment"
                        DailyTransactionItem(
                            amount = tx.paidAmount,
                            time = timeFormat.format(java.util.Date(tx.transactionDate)),
                            type = txType,
                            notes = tx.notes
                        )
                    }

                    val clientPaymentId = firstTx.paymentId
                    val todayPaidAmount = totalPaid
                    val overallTotalPaid = allTotalsPaidMap[clientPaymentId] ?: todayPaidAmount

                    val hasRefund = refundPaymentIds.contains(clientPaymentId)
                    val status = when {
                        overallTotalPaid <= 0.0 -> "UNPAID"
                        overallTotalPaid < firstTx.monthlyAmount && hasRefund -> "SETTLED"
                        overallTotalPaid < firstTx.monthlyAmount -> "PARTIAL"
                        else -> "PAID"
                    }

                    DailyClientCollection(
                        clientId = clientId, clientName = firstTx.clientName,
                        subscriptionNumber = firstTx.subscriptionNumber,
                        roomNumber = firstTx.roomNumber, packageType = firstTx.packageType,
                        monthlyAmount = firstTx.monthlyAmount,
                        paidAmount = todayPaidAmount, todayPaid = todayPaidAmount,
                        totalPaid = overallTotalPaid,
                        transactionTime = timeFormat.format(java.util.Date(lastTxTime)),
                        notes = allNotes, transactions = txItems, paymentStatus = status
                    )
                }.sortedBy { it.clientName }

                val totalAmount = clients.sumOf { it.paidAmount }
                val expectedAmount = clients.sumOf { it.monthlyAmount }
                val rate = if (expectedAmount > 0) (totalAmount / expectedAmount) * 100 else 0.0

                DailyBuildingDetailedUi(
                    buildingId = buildingId, buildingName = buildingName,
                    totalAmount = totalAmount, clientsCount = clients.size,
                    expectedAmount = expectedAmount, collectionRate = rate, clients = clients
                )
            }.sortedByDescending { it.totalAmount }

            // === إضافة العملاء غير المدفوعين لكل مبنى ===
            val allPaymentsForMonth = paymentRepository.getPaymentsByMonthDirect(month)
            val paidClientIds = rawTransactions.map { it.clientId }.toSet()
            val unpaidPayments = allPaymentsForMonth.filter { it.clientId !in paidClientIds }

            val nonTodayPaymentIds = unpaidPayments.map { it.id }
            val nonTodayTotalsMap = if (nonTodayPaymentIds.isNotEmpty()) {
                transactionRepository.getTotalsForPayments(nonTodayPaymentIds)
            } else emptyMap()
            val nonTodayRefundIds = if (nonTodayPaymentIds.isNotEmpty()) {
                transactionRepository.getPaymentIdsWithRefunds(nonTodayPaymentIds).toSet()
            } else emptySet()

            val nonTodayStatusMap = unpaidPayments.associate { p ->
                val totalPaidAll = nonTodayTotalsMap[p.id] ?: 0.0
                val hasRefund = nonTodayRefundIds.contains(p.id)
                val actualStatus = when {
                    totalPaidAll >= p.amount -> "PAID"
                    totalPaidAll > 0.0 && hasRefund -> "SETTLED"
                    totalPaidAll > 0.0 -> "PARTIAL"
                    else -> "UNPAID"
                }
                p.id to Triple(actualStatus, totalPaidAll, p.amount)
            }

            val filteredNonTodayPayments = unpaidPayments.filter {
                val st = nonTodayStatusMap[it.id]?.first ?: "UNPAID"
                st != "PAID"
            }

            val unpaidClientIds = filteredNonTodayPayments.map { it.clientId }.distinct()
            val unpaidClientsMap = if (unpaidClientIds.isNotEmpty()) {
                paymentRepository.getClientsByIds(unpaidClientIds).associateBy { it.id }
            } else emptyMap()

            val allBuildings = db.buildingDao().getAllBuildingsDirect()
            val allBuildingsMap = allBuildings.associate { it.id to it.name }

            val unpaidByBuilding = filteredNonTodayPayments.groupBy { payment ->
                val client = unpaidClientsMap[payment.clientId]
                client?.buildingId ?: -1
            }

            val existingBuildingIds = buildingCollections.map { it.buildingId }.toSet()
            val updatedBuildings = buildingCollections.map { building ->
                val unpaidForBuilding = unpaidByBuilding[building.buildingId] ?: emptyList()
                if (unpaidForBuilding.isEmpty()) return@map building

                val unpaidClients = unpaidForBuilding.mapNotNull { payment ->
                    val client = unpaidClientsMap[payment.clientId] ?: return@mapNotNull null
                    DailyClientCollection(
                        clientId = client.id, clientName = client.name,
                        subscriptionNumber = client.subscriptionNumber,
                        roomNumber = client.roomNumber, packageType = client.packageType,
                        monthlyAmount = payment.amount,
                        paidAmount = nonTodayStatusMap[payment.id]?.second ?: 0.0,
                        todayPaid = 0.0,
                        totalPaid = nonTodayStatusMap[payment.id]?.second ?: 0.0,
                        transactionTime = "", notes = "", transactions = emptyList(),
                        paymentStatus = nonTodayStatusMap[payment.id]?.first ?: "UNPAID"
                    )
                }

                val allClients = building.clients + unpaidClients.sortedBy { it.clientName }
                val newExpected = building.expectedAmount + unpaidClients.sumOf { it.monthlyAmount }
                val newRate = if (newExpected > 0) (building.totalAmount / newExpected) * 100 else 0.0

                building.copy(
                    clients = allClients, clientsCount = allClients.size,
                    expectedAmount = newExpected, collectionRate = newRate
                )
            }.toMutableList()

            // مباني جديدة فيها عملاء غير مدفوعين فقط
            unpaidByBuilding.filter { it.key !in existingBuildingIds && it.key != -1 }.forEach { (buildingId, payments) ->
                val unpaidClients = payments.mapNotNull { payment ->
                    val client = unpaidClientsMap[payment.clientId] ?: return@mapNotNull null
                    DailyClientCollection(
                        clientId = client.id, clientName = client.name,
                        subscriptionNumber = client.subscriptionNumber,
                        roomNumber = client.roomNumber, packageType = client.packageType,
                        monthlyAmount = payment.amount,
                        paidAmount = nonTodayStatusMap[payment.id]?.second ?: 0.0,
                        todayPaid = 0.0,
                        totalPaid = nonTodayStatusMap[payment.id]?.second ?: 0.0,
                        transactionTime = "", notes = "", transactions = emptyList(),
                        paymentStatus = nonTodayStatusMap[payment.id]?.first ?: "UNPAID"
                    )
                }.sortedBy { it.clientName }

                if (unpaidClients.isNotEmpty()) {
                    val buildingName = allBuildingsMap[buildingId] ?: "Unknown Building"
                    updatedBuildings.add(
                        DailyBuildingDetailedUi(
                            buildingId = buildingId, buildingName = buildingName,
                            totalAmount = 0.0, clientsCount = unpaidClients.size,
                            expectedAmount = unpaidClients.sumOf { it.monthlyAmount },
                            collectionRate = 0.0, clients = unpaidClients
                        )
                    )
                }
            }

            result.postValue(updatedBuildings.sortedByDescending { it.totalAmount })
        }
        return result
    }

    // ================== إحصائيات شهرية عامة ==================

    private val _selectedStatsMonth = MutableLiveData<String>()
    val selectedStatsMonth: LiveData<String> = _selectedStatsMonth

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

    val monthStats: LiveData<MonthStats> = _selectedStatsMonth.switchMap { month ->
        val paymentsLive = paymentRepository.getPaymentsByMonth(month)
        MediatorLiveData<MonthStats>().apply {
            var payments: List<Payment>? = null
            fun update() {
                val ps = payments ?: return
                viewModelScope.launch {
                    val ids = ps.map { it.id }
                    val totalsMap = transactionRepository.getTotalsForPayments(ids)
                    val refundIds = transactionRepository.getPaymentIdsWithRefunds(ids).toSet()

                    var fullPaidCount = 0; var partialPaidCount = 0
                    var settledCount = 0; var unpaidCount = 0
                    var totalRemaining = 0.0; var totalPaidAmount = 0.0; var settledAmount = 0.0

                    ps.forEach { p ->
                        val paidForThis = totalsMap[p.id] ?: 0.0
                        val remainingForThis = (p.amount - paidForThis).coerceAtLeast(0.0)
                        totalPaidAmount += paidForThis
                        val hasRefund = refundIds.contains(p.id)
                        when {
                            paidForThis <= 0.0 -> { unpaidCount += 1; totalRemaining += p.amount }
                            paidForThis < p.amount && hasRefund -> { settledCount += 1; settledAmount += paidForThis }
                            paidForThis < p.amount -> { partialPaidCount += 1; totalRemaining += remainingForThis }
                            else -> { fullPaidCount += 1 }
                        }
                    }
                    postValue(MonthStats(
                        month = month, paidCount = fullPaidCount,
                        partiallyPaidCount = partialPaidCount, settledCount = settledCount,
                        unpaidCount = unpaidCount, totalPaidAmount = totalPaidAmount,
                        totalUnpaidAmount = totalRemaining, settledAmount = settledAmount
                    ))
                }
            }
            addSource(paymentsLive) { payments = it ?: emptyList(); update() }
        }
    }

    // ================== إحصائيات الشهر السابق (للمقارنة في Dashboard) ==================

    val previousMonthStats: LiveData<MonthStats> = _selectedStatsMonth.switchMap { month ->
        val previousMonth = calculatePreviousMonth(month)
        val paymentsLive = paymentRepository.getPaymentsByMonth(previousMonth)
        MediatorLiveData<MonthStats>().apply {
            var payments: List<Payment>? = null
            fun update() {
                val ps = payments ?: return
                viewModelScope.launch {
                    val ids = ps.map { it.id }
                    val totalsMap = transactionRepository.getTotalsForPayments(ids)
                    val refundIds = transactionRepository.getPaymentIdsWithRefunds(ids).toSet()

                    var fullPaidCount = 0; var partialPaidCount = 0
                    var settledCount = 0; var unpaidCount = 0
                    var totalRemaining = 0.0; var totalPaidAmount = 0.0; var settledAmount = 0.0

                    ps.forEach { p ->
                        val paidForThis = totalsMap[p.id] ?: 0.0
                        val remainingForThis = (p.amount - paidForThis).coerceAtLeast(0.0)
                        totalPaidAmount += paidForThis
                        val hasRefund = refundIds.contains(p.id)
                        when {
                            paidForThis <= 0.0 -> { unpaidCount += 1; totalRemaining += p.amount }
                            paidForThis < p.amount && hasRefund -> { settledCount += 1; settledAmount += paidForThis }
                            paidForThis < p.amount -> { partialPaidCount += 1; totalRemaining += remainingForThis }
                            else -> { fullPaidCount += 1 }
                        }
                    }
                    postValue(MonthStats(
                        month = previousMonth, paidCount = fullPaidCount,
                        partiallyPaidCount = partialPaidCount, settledCount = settledCount,
                        unpaidCount = unpaidCount, totalPaidAmount = totalPaidAmount,
                        totalUnpaidAmount = totalRemaining, settledAmount = settledAmount
                    ))
                }
            }
            addSource(paymentsLive) { payments = it ?: emptyList(); update() }
        }
    }

    private fun calculatePreviousMonth(month: String): String {
        return try {
            val parts = month.split("-")
            val year = parts[0].toInt()
            val m = parts[1].toInt()
            if (m == 1) String.format("%04d-%02d", year - 1, 12)
            else String.format("%04d-%02d", year, m - 1)
        } catch (e: Exception) { month }
    }

    fun setStatsMonth(month: String) { _selectedStatsMonth.value = month }

    // ================== Dashboard: آخر الحركات ==================

    fun getRecentTransactions(limit: Int = 10): LiveData<List<PaymentTransactionDao.DashboardRecentTransaction>> {
        val result = MutableLiveData<List<PaymentTransactionDao.DashboardRecentTransaction>>()
        viewModelScope.launch {
            val list = transactionRepository.getRecentTransactions(limit)
            result.postValue(list)
        }
        return result
    }

    // ================== Dashboard: العملاء الأكثر تأخراً ==================

    fun getTopUnpaidClients(month: String, limit: Int = 5): LiveData<List<PaymentTransactionDao.DashboardUnpaidClient>> {
        val result = MutableLiveData<List<PaymentTransactionDao.DashboardUnpaidClient>>()
        viewModelScope.launch {
            val list = transactionRepository.getTopUnpaidClientsForMonth(month, limit)
            result.postValue(list)
        }
        return result
    }

    // ================== Flow تفاعلي: حالة الدفع لكل العملاء (يتحدث تلقائياً) ==================

    /**
     * نسخة تفاعلية من getAllClientStatusesForMonth — تستمع لتغييرات
     * جدولي payments و payment_transactions وتحدّث الـ UI تلقائياً.
     */
    fun observeAllClientStatusesForMonth(month: String): Flow<Map<Int, PaymentStatus>> {
        val paymentsFlow = paymentRepository.observePaymentsByMonth(month)

        return paymentsFlow.flatMapLatest { payments ->
            val ids = payments.map { it.id }
            if (ids.isEmpty()) return@flatMapLatest kotlinx.coroutines.flow.flowOf(emptyMap())

            val totalsFlow = transactionRepository.observeTotalsForPayments(ids)
            val refundsFlow = transactionRepository.observePaymentIdsWithRefunds(ids)

            combine(totalsFlow, refundsFlow) { totalsList, refundIdsList ->
                val totalsMap = totalsList.associate { it.paymentId to it.totalPaid }
                val refundIds = refundIdsList.toSet()

                payments.associate { p ->
                    val totalPaid = totalsMap[p.id] ?: 0.0
                    val hasRefund = refundIds.contains(p.id)
                    p.clientId to resolvePaymentStatus(totalPaid, p.amount, hasRefund)
                }
            }
        }.distinctUntilChanged()
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

    fun insert(payment: Payment) = viewModelScope.launch { paymentRepository.insert(payment) }
    fun update(payment: Payment) = viewModelScope.launch { paymentRepository.update(payment) }
    fun delete(payment: Payment) = viewModelScope.launch { paymentRepository.delete(payment) }
    fun deleteClientPayments(clientId: Int) = viewModelScope.launch { paymentRepository.deleteClientPayments(clientId) }
    fun deletePayment(clientId: Int, month: String) = viewModelScope.launch { paymentRepository.deletePayment(clientId, month) }

    // ================== دوال مساعدة للحصول على paymentId وقائمة الحركات ==================

    private suspend fun getPaymentIdForMonth(
        clientId: Int, month: String, monthAmount: Double
    ): Int {
        return paymentRepository.getOrCreatePaymentId(clientId, month, monthAmount)
    }

    fun getTransactionsForClientMonth(
        clientId: Int, month: String
    ): LiveData<List<PaymentTransaction>> {
        val result = MutableLiveData<List<PaymentTransaction>>()
        viewModelScope.launch {
            val payment = paymentRepository.getPayment(clientId, month)
            if (payment != null) {
                val list = transactionRepository.getTransactionsForPaymentList(payment.id)
                result.postValue(list)
            } else {
                result.postValue(emptyList())
            }
        }
        return result
    }

    // ================== حذف حركة فردية ==================

    fun deleteTransaction(transactionId: Int) = viewModelScope.launch {
        val paymentId = transactionRepository.getPaymentIdByTransactionId(transactionId)
        if (paymentId == null) return@launch

        transactionRepository.deleteTransactionById(transactionId)

        val newTotalPaid = transactionRepository.getTotalPaidForPayment(paymentId)
        val payment = paymentRepository.getPaymentById(paymentId)
        if (payment != null) {
            when {
                newTotalPaid <= 0.0 -> {
                    paymentRepository.update(payment.copy(isPaid = false, paymentDate = null))
                }
                newTotalPaid < payment.amount -> {
                    paymentRepository.update(payment.copy(isPaid = false, paymentDate = null))
                }
                else -> {
                    paymentRepository.update(payment.copy(isPaid = true))
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
        val alreadyPaid = transactionRepository.getTotalPaidForPayment(paymentId)
        val remaining = (amount - alreadyPaid).coerceAtLeast(0.0)

        if (remaining > 0.0) {
            transactionRepository.insert(
                PaymentTransaction(paymentId = paymentId, amount = remaining, notes = "full payment")
            )
        }

        val paymentDate = System.currentTimeMillis()
        paymentRepository.setPaidStatus(clientId = clientId, month = month, isPaid = true, paymentDate = paymentDate)
    }

    /**
     * دفع جزئي: يسجل Transaction بقيمة جزئية، ثم يحسب مجموع المدفوع
     * ويقرر هل يصبح مدفوع بالكامل أم يبقى جزئياً (isPaid = false لكن amount > 0).
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
            val paymentDate = System.currentTimeMillis()
            paymentRepository.setPaidStatus(clientId = clientId, month = month, isPaid = true, paymentDate = paymentDate)
        } else {
            val payment = paymentRepository.getPayment(clientId, month)
            if (payment != null) {
                paymentRepository.update(payment.copy(amount = monthAmount, isPaid = false))
            }
        }
    }

    // ================== حركة عكسية (استرجاع / رصيد) ==================

    /**
     * حركة عكسية (استرجاع / رصيد): تسجل Transaction بمبلغ سالب.
     * تستخدم عند فصل الخدمة قبل نهاية الشهر أو عند رد جزء من المبلغ للعميل.
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
        val payment = paymentRepository.getPaymentById(paymentId)
        if (payment != null) {
            when {
                newTotalPaid <= 0.0 -> {
                    paymentRepository.update(payment.copy(isPaid = false, paymentDate = null))
                }
                newTotalPaid < payment.amount -> {
                    paymentRepository.update(payment.copy(isPaid = false, paymentDate = null))
                }
                else -> {
                    paymentRepository.update(payment.copy(isPaid = true))
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
        clientId: Int, fromMonth: String, newAmount: Double
    ) = viewModelScope.launch {
        paymentRepository.updateFutureUnpaidPaymentsAmount(
            clientId = clientId, fromMonth = fromMonth, newAmount = newAmount
        )
    }

    /**
     * تعديل مبلغ الاشتراك الشهري للعميل اعتبارًا من أول شهر "نظيف" (لا حركات عليه) فما بعد.
     * مفيد عند تغيير السعر فقط لأشهر المستقبلية دون التأثير على الشهور الحالية.
     */
    fun applyNewMonthlyPriceFromNextUnpaidMonth(
        clientId: Int, newAmount: Double
    ) = viewModelScope.launch {
        val fromMonth = paymentRepository.getFirstUnpaidMonthForClient(clientId)
        if (fromMonth != null) {
            paymentRepository.updateFutureUnpaidPaymentsAmount(
                clientId = clientId, fromMonth = fromMonth, newAmount = newAmount
            )
        }
    }

    // ================== دالة مساعدة جديدة: إنشاء دفعات للعميل ==================

    /**
     * إنشاء دفعات شهرية للعميل.
     * الشهر الأول قد يكون بسعر مختلف (مثل 30 ريال)، والباقي بالسعر العادي (مثل 150 ريال).
     */
    fun createPaymentsForClient(
        clientId: Int, startMonth: String, endMonth: String?,
        amount: Double, monthOptions: List<String>,
        firstMonthAmount: Double? = null
    ) = viewModelScope.launch {
        val months = if (endMonth != null) {
            monthOptions.filter { month -> month >= startMonth && month < endMonth }
        } else {
            monthOptions.filter { it >= startMonth }
        }.sorted()

        months.forEachIndexed { index, month ->
            val monthAmount = if (index == 0 && firstMonthAmount != null) {
                firstMonthAmount
            } else {
                amount
            }
            paymentRepository.createOrUpdatePayment(
                clientId = clientId, month = month, amount = monthAmount, isPaid = false
            )
        }
    }

    // ================== Daily Summary ==================
    fun getDailySummary(dateString: String): Flow<DailySummary?> {
        return transactionRepository.getDailySummary(dateString)
    }


    // ================== دوال مساعدة قديمة (تبقى موجودة) ==================

    fun markAsPaid(clientId: Int, month: String, amount: Double) = viewModelScope.launch {
        markFullPayment(clientId, month, amount)
    }

    fun markAsUnpaid(clientId: Int, month: String) = viewModelScope.launch {
        val payment = paymentRepository.getPayment(clientId, month)
        if (payment != null) {
            transactionRepository.deleteTransactionsForPayment(payment.id)
            paymentRepository.update(payment.copy(isPaid = false, paymentDate = null))
        }
    }

    fun createOrUpdatePayment(
        clientId: Int, month: String, amount: Double,
        isPaid: Boolean = false, paymentDate: Long? = null, notes: String = ""
    ) = viewModelScope.launch {
        paymentRepository.createOrUpdatePayment(clientId, month, amount, isPaid, paymentDate, notes)
    }
}

// Performance evaluation logic
enum class DailyPerformanceLevel {
    EXCELLENT, GOOD, POOR
}

fun getDailyPerformance(totalAmount: Double): DailyPerformanceLevel {
    return when {
        totalAmount >= 2000.0 -> DailyPerformanceLevel.EXCELLENT
        totalAmount >= 1000.0 -> DailyPerformanceLevel.GOOD
        else -> DailyPerformanceLevel.POOR
    }
}
