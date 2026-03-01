package com.pronetwork.app.viewmodel

import com.pronetwork.app.data.ClientDatabase
import com.pronetwork.app.data.DailyBuildingDetailedUi
import com.pronetwork.app.data.DailyClientCollection
import com.pronetwork.app.data.DailyTransactionItem
import com.pronetwork.app.data.Payment
import com.pronetwork.app.data.PaymentTransactionDao
import com.pronetwork.app.repository.PaymentRepository
import com.pronetwork.app.repository.PaymentTransactionRepository
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────────────────────────────────────
// DailyCollectionBuilder — بناء بيانات التحصيل اليومي التفصيلي
// ─────────────────────────────────────────────────────────────────────────────

/**
 * مسؤول عن تحويل البيانات الخام (raw transactions) إلى UI models
 * جاهزة للعرض في شاشة التحصيل اليومي.
 *
 * يستخرج المنطق المشترك بين:
 * - [PaymentViewModel.getDetailedDailyCollections] (كل الحركات)
 * - [PaymentViewModel.getDetailedDailyCollectionsByUser] (حركات مستخدم محدد)
 *
 * يعتمد على [PaymentStatusResolver] كمصدر وحيد لحساب حالة الدفع.
 */
@Singleton
class DailyCollectionBuilder @Inject constructor(
    private val statusResolver: PaymentStatusResolver,
    private val paymentRepository: PaymentRepository,
    private val transactionRepository: PaymentTransactionRepository,
    private val db: ClientDatabase
) {

    // ─────────────────────────────────────────────────────────────────────
    // بناء قائمة المباني من الحركات اليومية
    // ─────────────────────────────────────────────────────────────────────

    /**
     * تحويل الحركات اليومية الخام إلى قائمة مباني تفصيلية.
     *
     * @param rawTransactions الحركات اليومية من قاعدة البيانات
     * @param allTotalsPaidMap مجموع المدفوع الكلي لكل paymentId
     * @param refundPaymentIds مجموعة الـ paymentIds التي فيها حركات سالبة
     * @return قائمة المباني مع تفاصيل العملاء، مرتبة حسب المبلغ تنازلياً
     */
    fun buildFromTransactions(
        rawTransactions: List<PaymentTransactionDao.DailyDetailedTransaction>,
        allTotalsPaidMap: Map<Int, Double>,
        refundPaymentIds: Set<Int>
    ): List<DailyBuildingDetailedUi> {
        val timeFormat = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())

        return rawTransactions
            .groupBy { it.buildingId }
            .map { (buildingId, txList) ->
                val buildingName = txList.first().buildingName

                val clients = txList
                    .groupBy { it.clientId }
                    .map { (clientId, clientTxs) ->
                        val firstTx = clientTxs.first()
                        val totalPaid = clientTxs.sumOf { it.paidAmount }
                        val lastTxTime = clientTxs.maxOf { it.transactionDate }

                        val allNotes = clientTxs
                            .map { it.notes }
                            .filter { it.isNotBlank() }
                            .distinct()
                            .joinToString(" | ")

                        val txItems = clientTxs.map { tx ->
                            DailyTransactionItem(
                                amount = tx.paidAmount,
                                time = timeFormat.format(java.util.Date(tx.transactionDate)),
                                type = if (tx.paidAmount < 0) "Refund" else "Payment",
                                notes = tx.notes
                            )
                        }

                        val clientPaymentId = firstTx.paymentId
                        val overallTotalPaid = allTotalsPaidMap[clientPaymentId] ?: totalPaid
                        val hasRefund = clientPaymentId in refundPaymentIds

                        val status = statusResolver.resolveAsDisplayString(
                            overallTotalPaid, firstTx.monthlyAmount, hasRefund
                        )

                        DailyClientCollection(
                            clientId = clientId,
                            clientName = firstTx.clientName,
                            subscriptionNumber = firstTx.subscriptionNumber,
                            roomNumber = firstTx.roomNumber,
                            packageType = firstTx.packageType,
                            monthlyAmount = firstTx.monthlyAmount,
                            paidAmount = totalPaid,
                            todayPaid = totalPaid,
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
    }

    // ─────────────────────────────────────────────────────────────────────
    // إضافة العملاء غير المدفوعين (Unpaid Overlay)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * يضيف العملاء غير المدفوعين اليوم إلى قائمة المباني الموجودة.
     * يُستخدم فقط في العرض الشامل (كل المستخدمين) وليس في فلتر المستخدم.
     *
     * @param buildingCollections قائمة المباني من الحركات اليومية
     * @param month الشهر الحالي (yyyy-MM)
     * @param paidClientIds مجموعة العملاء الذين لديهم حركات اليوم
     * @return القائمة المحدّثة مع إضافة العملاء غير المدفوعين
     */
    suspend fun overlayUnpaidClients(
        buildingCollections: List<DailyBuildingDetailedUi>,
        month: String,
        paidClientIds: Set<Int>
    ): List<DailyBuildingDetailedUi> {
        // ── جلب كل الدفعات للشهر وتحديد غير المدفوعين ──────────────────
        val allPaymentsForMonth = paymentRepository.getPaymentsByMonthDirect(month)
        val unpaidPayments = allPaymentsForMonth.filter { it.clientId !in paidClientIds }

        if (unpaidPayments.isEmpty()) return buildingCollections

        // ── حساب الحالة الحقيقية لكل payment غير مدفوع اليوم ────────────
        val nonTodayPaymentIds = unpaidPayments.map { it.id }
        val nonTodayTotalsMap = if (nonTodayPaymentIds.isNotEmpty()) {
            transactionRepository.getTotalsForPayments(nonTodayPaymentIds)
        } else emptyMap()

        val nonTodayRefundIds = if (nonTodayPaymentIds.isNotEmpty()) {
            transactionRepository.getPaymentIdsWithRefunds(nonTodayPaymentIds).toSet()
        } else emptySet()

        // status/totalPaid لكل payment غير مدفوع اليوم
        data class NonTodayInfo(val status: String, val totalPaid: Double, val amount: Double)

        val nonTodayInfoMap = unpaidPayments.associate { p ->
            val totalPaidAll = nonTodayTotalsMap[p.id] ?: 0.0
            val hasRefund = p.id in nonTodayRefundIds
            val status = statusResolver.resolveAsDisplayString(totalPaidAll, p.amount, hasRefund)
            p.id to NonTodayInfo(status, totalPaidAll, p.amount)
        }

        // فلتر: استبعاد المدفوعين بالكامل
        val filteredNonTodayPayments = unpaidPayments.filter { p ->
            nonTodayInfoMap[p.id]?.status != "PAID"
        }

        if (filteredNonTodayPayments.isEmpty()) return buildingCollections

        // ── جلب بيانات العملاء والمباني ─────────────────────────────────
        val unpaidClientIds = filteredNonTodayPayments.map { it.clientId }.distinct()
        val unpaidClientsMap = if (unpaidClientIds.isNotEmpty()) {
            paymentRepository.getClientsByIds(unpaidClientIds).associateBy { it.id }
        } else emptyMap()

        val allBuildingsMap = db.buildingDao().getAllBuildingsDirect().associate { it.id to it.name }

        val unpaidByBuilding = filteredNonTodayPayments.groupBy { payment ->
            unpaidClientsMap[payment.clientId]?.buildingId ?: -1
        }

        // ── دالة مساعدة: تحويل Payment → DailyClientCollection ─────────
        fun buildUnpaidClientCollection(payment: Payment): DailyClientCollection? {
            val client = unpaidClientsMap[payment.clientId] ?: return null
            val info = nonTodayInfoMap[payment.id] ?: return null
            return DailyClientCollection(
                clientId = client.id,
                clientName = client.name,
                subscriptionNumber = client.subscriptionNumber,
                roomNumber = client.roomNumber,
                packageType = client.packageType,
                monthlyAmount = payment.amount,
                paidAmount = info.totalPaid,
                todayPaid = 0.0,
                totalPaid = info.totalPaid,
                transactionTime = "",
                notes = "",
                transactions = emptyList(),
                paymentStatus = info.status
            )
        }

        // ── تحديث المباني الموجودة + إضافة مباني جديدة ──────────────────
        val existingBuildingIds = buildingCollections.map { it.buildingId }.toSet()

        val updatedBuildings = buildingCollections.map { building ->
            val unpaidForBuilding = unpaidByBuilding[building.buildingId] ?: emptyList()
            if (unpaidForBuilding.isEmpty()) return@map building

            val unpaidClients = unpaidForBuilding
                .mapNotNull { buildUnpaidClientCollection(it) }
                .sortedBy { it.clientName }

            val allClients = building.clients + unpaidClients
            val newExpected = building.expectedAmount + unpaidClients.sumOf { it.monthlyAmount }
            val newRate = if (newExpected > 0) (building.totalAmount / newExpected) * 100 else 0.0

            building.copy(
                clients = allClients,
                clientsCount = allClients.size,
                expectedAmount = newExpected,
                collectionRate = newRate
            )
        }.toMutableList()

        // مباني جديدة تحتوي عملاء غير مدفوعين فقط
        unpaidByBuilding
            .filterKeys { it !in existingBuildingIds && it != -1 }
            .forEach { (buildingId, payments) ->
                val unpaidClients = payments
                    .mapNotNull { buildUnpaidClientCollection(it) }
                    .sortedBy { it.clientName }

                if (unpaidClients.isNotEmpty()) {
                    updatedBuildings.add(
                        DailyBuildingDetailedUi(
                            buildingId = buildingId,
                            buildingName = allBuildingsMap[buildingId] ?: "Unknown Building",
                            totalAmount = 0.0,
                            clientsCount = unpaidClients.size,
                            expectedAmount = unpaidClients.sumOf { it.monthlyAmount },
                            collectionRate = 0.0,
                            clients = unpaidClients
                        )
                    )
                }
            }

        return updatedBuildings.sortedByDescending { it.totalAmount }
    }
}
