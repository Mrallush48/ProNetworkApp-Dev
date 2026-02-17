package com.pronetwork.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
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

class PaymentViewModel(application: Application) : AndroidViewModel(application) {

    private val paymentRepository: PaymentRepository
    private val transactionRepository: PaymentTransactionRepository

    val allPayments: LiveData<List<Payment>>

    init {
        val db = ClientDatabase.getDatabase(application)
        val paymentDao = db.paymentDao()
        val transactionDao = db.paymentTransactionDao()
        val clientDao = db.clientDao()

        paymentRepository = PaymentRepository(paymentDao, clientDao)
        transactionRepository = PaymentTransactionRepository(transactionDao, clientDao)

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

                // جلب قائمة الـ paymentIds التي فيها حركات سالبة (Refund)
                val refundIds = transactionRepository.getPaymentIdsWithRefunds(ids).toSet()

                val uiList = payments.map { p ->
                    val totalPaid = totalsMap[p.id] ?: 0.0
                    val remaining = (p.amount - totalPaid).coerceAtLeast(0.0)
                    val hasRefund = refundIds.contains(p.id)
                    val status = when {
                        totalPaid <= 0.0 -> PaymentStatus.UNPAID
                        totalPaid < p.amount && hasRefund -> PaymentStatus.SETTLED
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

    // ================== تحصيل يومي لكل مبنى ==================

    fun getDailyBuildingCollectionsForDay(
        dayStartMillis: Long,
        dayEndMillis: Long
    ): LiveData<List<DailyBuildingCollection>> {
        val result = MutableLiveData<List<DailyBuildingCollection>>()

        viewModelScope.launch {
            val list = transactionRepository.getDailyBuildingCollectionsForDay(
                dayStartMillis,
                dayEndMillis
            )
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
            val rawTransactions = transactionRepository.getDetailedDailyCollections(
                dayStartMillis, dayEndMillis
            )

            // جلب paymentIds اللي فيها حركات سالبة (Refund) لتحديد حالة SETTLED
            val allPaymentIds = rawTransactions.map { it.paymentId }.distinct()
            val refundPaymentIds = if (allPaymentIds.isNotEmpty()) {
                transactionRepository.getPaymentIdsWithRefunds(allPaymentIds).toSet()
            } else emptySet()

            // جلب إجمالي المدفوع الحقيقي لكل paymentId من كل الحركات (وليس فقط اليوم)
            val allTotalsPaidMap = if (allPaymentIds.isNotEmpty()) {
                transactionRepository.getTotalsForPayments(allPaymentIds)
            } else emptyMap()

            val timeFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())

            // تجميع الحركات حسب المبنى ثم حسب العميل
            val buildingGroups = rawTransactions.groupBy { it.buildingId }

            val buildingCollections = buildingGroups.map { (buildingId, txList) ->
                val buildingName = txList.first().buildingName

                // تجميع حسب العميل داخل المبنى
                val clientGroups = txList.groupBy { it.clientId }

                val clients = clientGroups.map { (clientId, clientTxs) ->
                    val firstTx = clientTxs.first()
                    val totalPaid = clientTxs.sumOf { it.paidAmount }
                    val lastTxTime = clientTxs.maxOf { it.transactionDate }
                    val allNotes = clientTxs
                        .map { it.notes }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .joinToString(" | ")

                    // بناء قائمة الحركات الفردية
                    val txItems = clientTxs.map { tx ->
                        val txType = if (tx.paidAmount < 0) "Refund" else "Payment"
                        DailyTransactionItem(
                            amount = tx.paidAmount,
                            time = timeFormat.format(java.util.Date(tx.transactionDate)),
                            type = txType,
                            notes = tx.notes
                        )
                    }

                    // حساب المبالغ بشكل محاسبي صحيح
                    val clientPaymentId = firstTx.paymentId
                    val todayPaidAmount = totalPaid  // مجموع حركات اليوم فقط
                    val overallTotalPaid = allTotalsPaidMap[clientPaymentId] ?: todayPaidAmount

                    // تحديد حالة الدفع بناءً على الإجمالي الحقيقي (كل الحركات)
                    val hasRefund = refundPaymentIds.contains(clientPaymentId)
                    val status = when {
                        overallTotalPaid <= 0.0 -> "UNPAID"
                        overallTotalPaid < firstTx.monthlyAmount && hasRefund -> "SETTLED"
                        overallTotalPaid < firstTx.monthlyAmount -> "PARTIAL"
                        else -> "PAID"
                    }

                    DailyClientCollection(
                        clientId = clientId,
                        clientName = firstTx.clientName,
                        subscriptionNumber = firstTx.subscriptionNumber,
                        roomNumber = firstTx.roomNumber,
                        packageType = firstTx.packageType,
                        monthlyAmount = firstTx.monthlyAmount,
                        paidAmount = todayPaidAmount,
                        todayPaid = todayPaidAmount,
                        totalPaid = overallTotalPaid,
                        transactionTime = timeFormat.format(java.util.Date(lastTxTime)),
                        notes = allNotes,
                        transactions = txItems,
                        paymentStatus = status
                    )
                }.sortedBy { it.clientName }


                val totalAmount = clients.sumOf { it.paidAmount }
                val expectedAmount = clients.sumOf { it.monthlyAmount }
                val rate = if (expectedAmount > 0) (totalAmount / expectedAmount) * 100 else 0.0

                DailyBuildingDetailedUi(
                    buildingId = buildingId,
                    buildingName = buildingName,
                    totalAmount = totalAmount,
                    clientsCount = clients.size,
                    expectedAmount = expectedAmount,
                    collectionRate = rate,
                    clients = clients
                )

            }.sortedByDescending { it.totalAmount }

            // === إضافة العملاء غير المدفوعين لكل مبنى ===
            val allPaymentsForMonth = paymentRepository.getPaymentsByMonthDirect(month)
            val paidClientIds = rawTransactions.map { it.clientId }.toSet()
            val unpaidPayments = allPaymentsForMonth.filter { it.clientId !in paidClientIds }

            val unpaidClientIds = unpaidPayments.map { it.clientId }.distinct()
            val unpaidClientsMap = if (unpaidClientIds.isNotEmpty()) {
                paymentRepository.getClientsByIds(unpaidClientIds).associateBy { it.id }
            } else emptyMap()

            val db = ClientDatabase.getDatabase(getApplication())
            val allBuildings = db.buildingDao().getAllBuildingsDirect()
            val allBuildingsMap = allBuildings.associate { it.id to it.name }

            // تجميع العملاء غير المدفوعين حسب المبنى
            val unpaidByBuilding = unpaidPayments.groupBy { payment ->
                val client = unpaidClientsMap[payment.clientId]
                client?.buildingId ?: -1
            }

            // دمج العملاء غير المدفوعين مع المباني الموجودة + إنشاء مباني جديدة
            val existingBuildingIds = buildingCollections.map { it.buildingId }.toSet()
            val updatedBuildings = buildingCollections.map { building ->
                val unpaidForBuilding = unpaidByBuilding[building.buildingId] ?: emptyList()
                if (unpaidForBuilding.isEmpty()) return@map building

                val unpaidClients = unpaidForBuilding.mapNotNull { payment ->
                    val client = unpaidClientsMap[payment.clientId] ?: return@mapNotNull null
                    DailyClientCollection(
                        clientId = client.id,
                        clientName = client.name,
                        subscriptionNumber = client.subscriptionNumber,
                        roomNumber = client.roomNumber,
                        packageType = client.packageType,
                        monthlyAmount = payment.amount,
                        paidAmount = 0.0,
                        todayPaid = 0.0,
                        totalPaid = 0.0,
                        transactionTime = "",
                        notes = "",
                        transactions = emptyList(),
                        paymentStatus = "UNPAID"
                    )
                }

                val allClients = building.clients + unpaidClients.sortedBy { it.clientName }
                val newExpected = allClients.sumOf { it.monthlyAmount }
                val newRate = if (newExpected > 0) (building.totalAmount / newExpected) * 100 else 0.0

                building.copy(
                    clients = allClients,
                    clientsCount = allClients.size,
                    expectedAmount = newExpected,
                    collectionRate = newRate
                )
            }.toMutableList()

            // مباني جديدة فيها عملاء غير مدفوعين فقط
            unpaidByBuilding.filter { it.key !in existingBuildingIds && it.key != -1 }.forEach { (buildingId, payments) ->
                val unpaidClients = payments.mapNotNull { payment ->
                    val client = unpaidClientsMap[payment.clientId] ?: return@mapNotNull null
                    DailyClientCollection(
                        clientId = client.id,
                        clientName = client.name,
                        subscriptionNumber = client.subscriptionNumber,
                        roomNumber = client.roomNumber,
                        packageType = client.packageType,
                        monthlyAmount = payment.amount,
                        paidAmount = 0.0,
                        todayPaid = 0.0,
                        totalPaid = 0.0,
                        transactionTime = "",
                        notes = "",
                        transactions = emptyList(),
                        paymentStatus = "UNPAID"
                    )
                }.sortedBy { it.clientName }

                if (unpaidClients.isNotEmpty()) {
                    val buildingName = allBuildingsMap[buildingId] ?: "Unknown Building"

                    updatedBuildings.add(
                        DailyBuildingDetailedUi(
                            buildingId = buildingId,
                            buildingName = buildingName,
                            totalAmount = 0.0,
                            clientsCount = unpaidClients.size,
                            expectedAmount = unpaidClients.sumOf { it.monthlyAmount },
                            collectionRate = 0.0,
                            clients = unpaidClients
                        )
                    )
                }
            }

            result.postValue(updatedBuildings.sortedByDescending { it.totalAmount })
        }
        return result
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
        val settledCount: Int,
        val unpaidCount: Int,
        val totalPaidAmount: Double,
        val totalUnpaidAmount: Double,
        val settledAmount: Double
    )

    // LiveData للإحصائيات الشهرية الجاهزة
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

                    var fullPaidCount = 0
                    var partialPaidCount = 0
                    var settledCount = 0
                    var unpaidCount = 0
                    var totalRemaining = 0.0
                    var totalPaidAmount = 0.0
                    var settledAmount = 0.0

                    ps.forEach { p ->
                        val paidForThis = totalsMap[p.id] ?: 0.0
                        val remainingForThis = (p.amount - paidForThis).coerceAtLeast(0.0)
                        totalPaidAmount += paidForThis

                        val hasRefund = refundIds.contains(p.id)

                        when {
                            paidForThis <= 0.0 -> {
                                unpaidCount += 1
                                totalRemaining += p.amount
                            }
                            paidForThis < p.amount && hasRefund -> {
                                // مُسوَّى: دفع كامل ثم استرجاع
                                settledCount += 1
                                settledAmount += paidForThis
                                // لا يُضاف للمتبقي لأنه مُسوَّى
                            }
                            paidForThis < p.amount -> {
                                // دفع جزئي عادي
                                partialPaidCount += 1
                                totalRemaining += remainingForThis
                            }
                            else -> {
                                fullPaidCount += 1
                            }
                        }
                    }

                    postValue(
                        MonthStats(
                            month = month,
                            paidCount = fullPaidCount,
                            partiallyPaidCount = partialPaidCount,
                            settledCount = settledCount,
                            unpaidCount = unpaidCount,
                            totalPaidAmount = totalPaidAmount,
                            totalUnpaidAmount = totalRemaining,
                            settledAmount = settledAmount
                        )
                    )
                }
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

    /**
     * إرجاع حالة دفع شهر واحد لعميل واحد (UNPAID / PARTIAL / FULL)
     * مبنية على مجموع الحركات (transactions) وقيمة amount في جدول payments.
     */
    fun getClientMonthStatus(clientId: Int, month: String): LiveData<PaymentStatus> {
        val result = MutableLiveData<PaymentStatus>()

        viewModelScope.launch {
            // 1) احصل على الـ Payment لهذا الشهر (قد يكون null إذا لم يُنشأ بعد)
            val payment = paymentRepository.getPayment(clientId, month)

            if (payment == null) {
                // لا يوجد سجل دفع → غير مدفوع
                result.postValue(PaymentStatus.UNPAID)
                return@launch
            }

            // 2) احسب مجموع ما دُفع لهذا الـ Payment من جدول الحركات
            val totalPaid = transactionRepository.getTotalPaidForPayment(payment.id)

            // 3) طبّق نفس قاعدة الحالة التي تستخدمها في الإحصائيات
            val hasRefund = transactionRepository.hasNegativeTransaction(payment.id)
            val status = when {
                totalPaid <= 0.0 -> PaymentStatus.UNPAID
                totalPaid < payment.amount && hasRefund -> PaymentStatus.SETTLED
                totalPaid < payment.amount -> PaymentStatus.PARTIAL
                else -> PaymentStatus.FULL
            }

            result.postValue(status)
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
                    notes = "full payment"
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
    fun addPartialPayment(
        clientId: Int,
        month: String,
        monthAmount: Double,
        partialAmount: Double
    ) =
        viewModelScope.launch {
            val paymentId = paymentRepository.getOrCreatePaymentId(clientId, month, monthAmount)

            // أضف حركة جديدة بمبلغ جزئي
            transactionRepository.insert(
                PaymentTransaction(
                    paymentId = paymentId,
                    amount = partialAmount,
                    notes = "partial payment"
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
        reason: String = "Refund"
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
        firstMonthAmount: Double? = null
    ) = viewModelScope.launch {
        val months = if (endMonth != null) {
            monthOptions.filter { month ->
                month >= startMonth && month < endMonth
            }
        } else {
            monthOptions.filter { it >= startMonth }
        }
            .sorted()   // ✅ مهم: تأكيد أن الترتيب تصاعدي مثل 2025-01, 2025-02, ..., 2026-01

        months.forEachIndexed { index, month ->
            val monthAmount = if (index == 0 && firstMonthAmount != null) {
                firstMonthAmount           // الآن تُطبَّق على أقدم شهر = startMonth الصحيح
            } else {
                amount
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

// Performance evaluation logic
enum class DailyPerformanceLevel { EXCELLENT, GOOD, POOR }

fun getDailyPerformance(totalAmount: Double): DailyPerformanceLevel {
    return when {
        totalAmount >= 4000.0 -> DailyPerformanceLevel.EXCELLENT
        totalAmount >= 3000.0 -> DailyPerformanceLevel.GOOD
        else -> DailyPerformanceLevel.POOR
    }
}
