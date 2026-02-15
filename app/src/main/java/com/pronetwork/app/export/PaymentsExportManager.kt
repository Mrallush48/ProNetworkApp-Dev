package com.pronetwork.app.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.pronetwork.app.data.ClientDatabase
import com.pronetwork.app.repository.PaymentTransactionRepository
import com.pronetwork.app.ui.components.PaymentReportFilter
import com.pronetwork.app.ui.components.PaymentReportPeriod
import com.pronetwork.app.ui.components.PaymentReportType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PaymentsExportManager(private val context: Context) {

    private val db = ClientDatabase.getDatabase(context)
    private val transactionRepo =
        PaymentTransactionRepository(db.paymentTransactionDao(), db.clientDao())
    private val paymentDao = db.paymentDao()

    // ===================== DATA MODELS =====================

    data class ClientPaymentSummary(
        val clientId: Int,
        val clientName: String,
        val subscriptionNumber: String,
        val phone: String,
        val packageType: String,
        val roomNumber: String?,
        val buildingId: Int,
        val buildingName: String,
        val monthlyAmount: Double,
        val totalPaid: Double,
        val remaining: Double,
        val status: String,
        val statusIcon: String,
        val lastPaymentDate: String?,
        val transactions: List<TransactionDetail>,
        val clientNote: String?
    )

    data class TransactionDetail(
        val amount: Double,
        val date: String,
        val type: String,
        val notes: String
    )

    data class BuildingSummary(
        val buildingName: String,
        val clientCount: Int,
        val totalExpected: Double,
        val totalCollected: Double,
        val totalRemaining: Double,
        val collectionRate: Double
    )

    data class MonthData(
        val month: String,
        val totalClients: Int,
        val paidCount: Int,
        val partialCount: Int,
        val unpaidCount: Int,
        val totalExpected: Double,
        val totalCollected: Double,
        val totalRemaining: Double,
        val collectionRate: Double,
        val clients: List<ClientPaymentSummary>,
        val buildings: List<BuildingSummary>
    )

    data class PaymentReportData(
        val reportTitle: String,
        val reportType: PaymentReportType,
        val period: PaymentReportPeriod,
        val generatedDate: String,
        val months: List<MonthData>,
        val grandTotalClients: Int,
        val grandTotalExpected: Double,
        val grandTotalCollected: Double,
        val grandTotalRemaining: Double,
        val grandCollectionRate: Double,
        val bestMonth: String?,
        val worstMonth: String?,
        val avgMonthlyCollection: Double,
        val trend: String
    )

    data class SmartInsight(
        val icon: String,
        val level: String,  // "CRITICAL", "WARNING", "INFO", "SUCCESS"
        val title: String,
        val detail: String
    )

    // ===================== SMART INSIGHTS (خارج gatherFullReport) =====================

    private fun generateSmartInsights(data: PaymentReportData): List<SmartInsight> {
        val insights = mutableListOf<SmartInsight>()

        // 1. Collection Rate Alert
        if (data.grandCollectionRate < 50) {
            insights.add(
                SmartInsight(
                    "🔴",
                    "CRITICAL",
                    "Low Collection Rate",
                    "Overall collection is only ${"%.1f".format(data.grandCollectionRate)}% — immediate action required"
                )
            )
        } else if (data.grandCollectionRate < 75) {
            insights.add(
                SmartInsight(
                    "🟡",
                    "WARNING",
                    "Below Target",
                    "Collection rate ${"%.1f".format(data.grandCollectionRate)}% is below the 75% target"
                )
            )
        } else {
            insights.add(
                SmartInsight(
                    "🟢",
                    "SUCCESS",
                    "Good Collection",
                    "Collection rate ${"%.1f".format(data.grandCollectionRate)}% is on track"
                )
            )
        }

        // 2. Unpaid clients count
        val totalUnpaid = data.months.maxOfOrNull { it.unpaidCount } ?: 0
        if (totalUnpaid > 0) {
            insights.add(
                SmartInsight(
                    "🔴",
                    "CRITICAL",
                    "$totalUnpaid Unpaid Clients",
                    "There are $totalUnpaid clients with zero payments — follow up immediately"
                )
            )
        }

        // 3. Partial payment clients
        val totalPartial = data.months.maxOfOrNull { it.partialCount } ?: 0
        if (totalPartial > 0) {
            insights.add(
                SmartInsight(
                    "🟡",
                    "WARNING",
                    "$totalPartial Partial Payments",
                    "$totalPartial clients have partial payments — contact for remaining balance"
                )
            )
        }

        // 4. Building performance alerts
        val allBuildings = data.months.flatMap { it.buildings }
            .groupBy { it.buildingName }
            .map { (name, list) ->
                val totalExp = list.sumOf { it.totalExpected }
                val totalCol = list.sumOf { it.totalCollected }
                val rate = if (totalExp > 0) (totalCol / totalExp) * 100 else 0.0
                name to rate
            }

        allBuildings.filter { it.second < 50 }.forEach { (name, rate) ->
            insights.add(
                SmartInsight(
                    "🏢",
                    "CRITICAL",
                    "$name — ${"%.0f".format(rate)}%",
                    "Building '$name' collection is critically low at ${"%.1f".format(rate)}%"
                )
            )
        }

        val bestBuilding = allBuildings.maxByOrNull { it.second }
        val worstBuilding = allBuildings.minByOrNull { it.second }
        if (bestBuilding != null && worstBuilding != null && allBuildings.size > 1) {
            insights.add(
                SmartInsight(
                    "🏆",
                    "INFO",
                    "Top: ${bestBuilding.first}",
                    "Best performing building at ${"%.1f".format(bestBuilding.second)}%"
                )
            )
            if (worstBuilding.second < bestBuilding.second - 20) {
                insights.add(
                    SmartInsight(
                        "⚠️",
                        "WARNING",
                        "Gap Alert",
                        "'${worstBuilding.first}' is ${"%.0f".format(bestBuilding.second - worstBuilding.second)}% behind '${bestBuilding.first}'"
                    )
                )
            }
        }

        // 5. Trend alert (multi-month)
        if (data.months.size >= 2) {
            val firstRate = data.months.first().collectionRate
            val lastRate = data.months.last().collectionRate
            if (lastRate > firstRate + 10) {
                insights.add(
                    SmartInsight(
                        "📈",
                        "SUCCESS",
                        "Strong Growth",
                        "Collection improved by ${"%.1f".format(lastRate - firstRate)}% over the period"
                    )
                )
            } else if (lastRate < firstRate - 10) {
                insights.add(
                    SmartInsight(
                        "📉",
                        "CRITICAL",
                        "Declining Trend",
                        "Collection dropped by ${"%.1f".format(firstRate - lastRate)}% — investigate cause"
                    )
                )
            }
        }

        // 6. Outstanding amount
        if (data.grandTotalRemaining > 0) {
            insights.add(
                SmartInsight(
                    "💰",
                    "INFO",
                    "Outstanding: ${"%.2f".format(data.grandTotalRemaining)} SAR",
                    "Total uncollected amount across all clients"
                )
            )
        }

        // 7. Full collection achievement
        val fullPaidMonths = data.months.filter { it.collectionRate >= 100 }
        if (fullPaidMonths.isNotEmpty()) {
            insights.add(
                SmartInsight(
                    "🎯",
                    "SUCCESS",
                    "${fullPaidMonths.size} Perfect Month(s)",
                    "Full collection achieved in: ${fullPaidMonths.joinToString { it.month }}"
                )
            )
        }

        return insights.sortedBy {
            when (it.level) {
                "CRITICAL" -> 0
                "WARNING" -> 1
                "INFO" -> 2
                "SUCCESS" -> 3
                else -> 4
            }
        }
    }

    // ===================== MONTH CALCULATION =====================

    private fun getMonthsForPeriod(
        period: PaymentReportPeriod,
        startMonth: String,
        endMonth: String?,
        allMonths: List<String>
    ): List<String> {
        return when (period) {
            PaymentReportPeriod.MONTHLY -> listOf(startMonth)
            PaymentReportPeriod.QUARTERLY -> {
                val idx = allMonths.indexOf(startMonth)
                if (idx >= 0) allMonths.drop(idx).take(3) else listOf(startMonth)
            }
            PaymentReportPeriod.YEARLY -> {
                val idx = allMonths.indexOf(startMonth)
                if (idx >= 0) allMonths.drop(idx).take(12) else listOf(startMonth)
            }
            PaymentReportPeriod.CUSTOM -> {
                if (endMonth == null) return listOf(startMonth)
                val startIdx = allMonths.indexOf(startMonth)
                val endIdx = allMonths.indexOf(endMonth)
                if (startIdx >= 0 && endIdx >= 0) {
                    val from = minOf(startIdx, endIdx)
                    val to = maxOf(startIdx, endIdx)
                    allMonths.subList(from, to + 1)
                } else listOf(startMonth)
            }
        }
    }

    // ===================== DATA GATHERING =====================

    private suspend fun gatherMonthData(
        month: String,
        buildingFilter: Int?,
        packageFilter: String?,
        statusFilter: PaymentReportFilter
    ): MonthData {
        val transactions = transactionRepo.getDetailedTransactionsForMonth(month)
        val allPaymentsForMonth = paymentDao.getPaymentsByMonthDirect(month)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val txByClient = transactions.groupBy { it.clientId }

        val clientSummaries = allPaymentsForMonth.map { payment ->
            val clientTxs = txByClient[payment.clientId] ?: emptyList()
            val totalPaid = clientTxs.sumOf { it.transactionAmount }
            val remaining = (payment.amount - totalPaid).coerceAtLeast(0.0)

            val status = when {
                totalPaid <= 0.0 -> "UNPAID"
                totalPaid < payment.amount -> "PARTIAL"
                else -> "PAID"
            }
            val statusIcon = when (status) {
                "PAID" -> "✅"
                "PARTIAL" -> "⚠️"
                else -> "❌"
            }

            val firstTx = clientTxs.firstOrNull()
            val finalClientName: String
            val finalSubNumber: String
            val finalPhone: String
            val finalPackageType: String
            val finalRoomNumber: String?
            val finalBuildingId: Int
            val finalBuildingName: String

            if (firstTx != null) {
                finalClientName = firstTx.clientName
                finalSubNumber = firstTx.subscriptionNumber
                finalPhone = firstTx.clientPhone
                finalPackageType = firstTx.packageType
                finalRoomNumber = firstTx.roomNumber
                finalBuildingId = firstTx.buildingId
                finalBuildingName = firstTx.buildingName
            } else {
                val client = db.clientDao().getClientById(payment.clientId)
                val building = client?.buildingId?.let { db.buildingDao().getBuildingById(it) }
                finalClientName = client?.name ?: "Unknown"
                finalSubNumber = client?.subscriptionNumber ?: ""
                finalPhone = client?.phone ?: ""
                finalPackageType = client?.packageType ?: ""
                finalRoomNumber = client?.roomNumber
                finalBuildingId = client?.buildingId ?: 0
                finalBuildingName = building?.name ?: "Unknown"
            }

            val txDetails: List<TransactionDetail> = clientTxs.map { tx ->
                val type = when {
                    tx.transactionAmount < 0 -> "Refund"
                    tx.transactionNotes.contains("partial", true) -> "Partial Payment"
                    else -> "Full Payment"
                }

                TransactionDetail(
                    amount = tx.transactionAmount,
                    date = dateFormat.format(Date(tx.transactionDate)),
                    type = type,
                    notes = tx.transactionNotes
                )
            }

            // تحديد ملاحظة العميل من الـ DB
            val client = db.clientDao().getClientById(payment.clientId)
            val clientNoteValue = client?.notes ?: ""

            val lastDate = clientTxs
                .filter { it.transactionAmount > 0 }
                .maxByOrNull { it.transactionDate }
                ?.let { dateFormat.format(Date(it.transactionDate)) }

            ClientPaymentSummary(
                clientId = payment.clientId,
                clientName = finalClientName,
                subscriptionNumber = finalSubNumber,
                phone = finalPhone,
                packageType = finalPackageType,
                roomNumber = finalRoomNumber,
                buildingId = finalBuildingId,
                buildingName = finalBuildingName,
                monthlyAmount = payment.amount,
                totalPaid = totalPaid,
                remaining = remaining,
                status = status,
                statusIcon = statusIcon,
                lastPaymentDate = lastDate,
                transactions = txDetails,
                clientNote = clientNoteValue
            )
        }

        // Apply filters
        val filtered = clientSummaries
            .filter { c -> buildingFilter == null || c.buildingId == buildingFilter }
            .filter { c -> packageFilter == null || c.packageType == packageFilter }
            .filter { c ->
                when (statusFilter) {
                    PaymentReportFilter.ALL -> true
                    PaymentReportFilter.PAID_ONLY -> c.status == "PAID"
                    PaymentReportFilter.UNPAID_ONLY -> c.status == "UNPAID"
                    PaymentReportFilter.PARTIAL_ONLY -> c.status == "PARTIAL"
                }
            }

        val buildingSummaries = filtered.groupBy { it.buildingName }.map { (name, cls) ->
            val expected = cls.sumOf { it.monthlyAmount }
            val collected = cls.sumOf { it.totalPaid }
            val rem = (expected - collected).coerceAtLeast(0.0)
            val rate = if (expected > 0) (collected / expected) * 100 else 0.0
            BuildingSummary(name, cls.size, expected, collected, rem, rate)
        }.sortedByDescending { it.totalCollected }

        val totalExpected = filtered.sumOf { it.monthlyAmount }
        val totalCollected = filtered.sumOf { it.totalPaid }
        val totalRemaining = (totalExpected - totalCollected).coerceAtLeast(0.0)
        val rate = if (totalExpected > 0) (totalCollected / totalExpected) * 100 else 0.0

        return MonthData(
            month,
            filtered.size,
            filtered.count { it.status == "PAID" },
            filtered.count { it.status == "PARTIAL" },
            filtered.count { it.status == "UNPAID" },
            totalExpected,
            totalCollected,
            totalRemaining,
            rate,
            filtered.sortedBy { it.buildingName },
            buildingSummaries
        )
    }

    private suspend fun gatherFullReport(
        reportType: PaymentReportType,
        period: PaymentReportPeriod,
        startMonth: String,
        endMonth: String?,
        buildingFilter: Int?,
        packageFilter: String?,
        statusFilter: PaymentReportFilter
    ): PaymentReportData {
        val allMonths = paymentDao.getPaymentsByMonthDirect(startMonth)
            .map { it.month }.distinct().sorted()
            .let { if (it.isEmpty()) listOf(startMonth) else it }

        // Get all available months from DB
        val allAvailableMonths = withContext(Dispatchers.IO) {
            val allPayments = db.paymentDao().getAllPaymentsDirect()
            allPayments.map { it.month }.distinct().sorted()
        }

        val months = getMonthsForPeriod(period, startMonth, endMonth, allAvailableMonths)
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())

        val monthsData = months.map { m ->
            gatherMonthData(m, buildingFilter, packageFilter, statusFilter)
        }

        val grandExpected = monthsData.sumOf { it.totalExpected }
        val grandCollected = monthsData.sumOf { it.totalCollected }
        val grandRemaining = (grandExpected - grandCollected).coerceAtLeast(0.0)
        val grandRate = if (grandExpected > 0) (grandCollected / grandExpected) * 100 else 0.0

        val bestMonth = monthsData.maxByOrNull { it.collectionRate }?.month
        val worstMonth = monthsData.minByOrNull { it.collectionRate }?.month
        val avgCollection = if (monthsData.isNotEmpty()) grandCollected / monthsData.size else 0.0

        val trend = if (monthsData.size >= 2) {
            val first = monthsData.first().collectionRate
            val last = monthsData.last().collectionRate
            when {
                last > first + 5 -> "📈 Improving (+${"%.1f".format(last - first)}%)"
                last < first - 5 -> "📉 Declining (${"%.1f".format(last - first)}%)"
                else -> "➡️ Stable"
            }
        } else "—"

        val title = when (period) {
            PaymentReportPeriod.MONTHLY -> "Monthly Report: $startMonth"
            PaymentReportPeriod.QUARTERLY -> "Quarterly Report: ${months.first()} - ${months.last()}"
            PaymentReportPeriod.YEARLY -> "Annual Report: ${months.first()} - ${months.last()}"
            PaymentReportPeriod.CUSTOM -> "Custom Report: ${months.first()} - ${months.last()}"
        }

        return PaymentReportData(
            reportTitle = title,
            reportType = reportType,
            period = period,
            generatedDate = now,
            months = monthsData,
            grandTotalClients = monthsData.maxOfOrNull { it.totalClients } ?: 0,
            grandTotalExpected = grandExpected,
            grandTotalCollected = grandCollected,
            grandTotalRemaining = grandRemaining,
            grandCollectionRate = grandRate,
            bestMonth = bestMonth,
            worstMonth = worstMonth,
            avgMonthlyCollection = avgCollection,
            trend = trend
        )
    }

    // ===================== EXCEL BUILDER =====================

    private fun buildExcelXml(data: PaymentReportData): String {
        return buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<?mso-application progid="Excel.Sheet"?>""")
            appendLine("""<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet" xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet">""")

            // STYLES
            appendLine("""<Styles>""")
            appendLine("""<Style ss:ID="Default"><Font ss:Size="10"/></Style>""")
            appendLine("""<Style ss:ID="Title"><Font ss:Size="16" ss:Bold="1" ss:Color="#FFFFFF"/><Interior ss:Color="#673AB7" ss:Pattern="Solid"/><Alignment ss:Horizontal="Center"/></Style>""")
            appendLine("""<Style ss:ID="SubTitle"><Font ss:Size="11" ss:Bold="1" ss:Color="#311B92"/><Interior ss:Color="#EDE7F6" ss:Pattern="Solid"/></Style>""")
            appendLine("""<Style ss:ID="Header"><Font ss:Size="9" ss:Bold="1" ss:Color="#FFFFFF"/><Interior ss:Color="#7E57C2" ss:Pattern="Solid"/><Alignment ss:Horizontal="Center"/><Borders><Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1"/></Borders></Style>""")
            appendLine("""<Style ss:ID="Cell"><Font ss:Size="9"/><Borders><Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E0E0E0"/></Borders></Style>""")
            appendLine("""<Style ss:ID="CellAlt"><Font ss:Size="9"/><Interior ss:Color="#F5F5F5" ss:Pattern="Solid"/><Borders><Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E0E0E0"/></Borders></Style>""")
            appendLine("""<Style ss:ID="Paid"><Font ss:Size="9" ss:Bold="1" ss:Color="#2E7D32"/></Style>""")
            appendLine("""<Style ss:ID="Partial"><Font ss:Size="9" ss:Bold="1" ss:Color="#F57F17"/></Style>""")
            appendLine("""<Style ss:ID="Unpaid"><Font ss:Size="9" ss:Bold="1" ss:Color="#C62828"/></Style>""")
            appendLine("""<Style ss:ID="Currency"><NumberFormat ss:Format="#,##0.00"/><Font ss:Size="9"/><Borders><Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E0E0E0"/></Borders></Style>""")
            appendLine("""<Style ss:ID="CurrencyAlt"><NumberFormat ss:Format="#,##0.00"/><Font ss:Size="9"/><Interior ss:Color="#F5F5F5" ss:Pattern="Solid"/><Borders><Border ss:Position="Bottom" ss:LineStyle="Continuous" ss:Weight="1" ss:Color="#E0E0E0"/></Borders></Style>""")
            appendLine("""<Style ss:ID="TotalRow"><Font ss:Size="10" ss:Bold="1" ss:Color="#FFFFFF"/><Interior ss:Color="#9575CD" ss:Pattern="Solid"/><NumberFormat ss:Format="#,##0.00"/></Style>""")
            appendLine("""<Style ss:ID="SectionHeader"><Font ss:Size="12" ss:Bold="1" ss:Color="#4527A0"/><Interior ss:Color="#D1C4E9" ss:Pattern="Solid"/><Alignment ss:Horizontal="Center"/></Style>""")
            appendLine("""<Style ss:ID="KpiGood"><Font ss:Size="11" ss:Bold="1" ss:Color="#2E7D32"/><Interior ss:Color="#E8F5E9" ss:Pattern="Solid"/><Alignment ss:Horizontal="Center"/></Style>""")
            appendLine("""<Style ss:ID="KpiWarn"><Font ss:Size="11" ss:Bold="1" ss:Color="#F57F17"/><Interior ss:Color="#FFF8E1" ss:Pattern="Solid"/><Alignment ss:Horizontal="Center"/></Style>""")
            appendLine("""<Style ss:ID="KpiBad"><Font ss:Size="11" ss:Bold="1" ss:Color="#C62828"/><Interior ss:Color="#FFEBEE" ss:Pattern="Solid"/><Alignment ss:Horizontal="Center"/></Style>""")
            appendLine("""<Style ss:ID="TrendUp"><Font ss:Size="10" ss:Bold="1" ss:Color="#2E7D32"/><Interior ss:Color="#E8F5E9" ss:Pattern="Solid"/></Style>""")
            appendLine("""<Style ss:ID="TrendDown"><Font ss:Size="10" ss:Bold="1" ss:Color="#C62828"/><Interior ss:Color="#FFEBEE" ss:Pattern="Solid"/></Style>""")
            appendLine("""<Style ss:ID="TrendStable"><Font ss:Size="10" ss:Bold="1" ss:Color="#1565C0"/><Interior ss:Color="#E3F2FD" ss:Pattern="Solid"/></Style>""")
            appendLine("""</Styles>""")

            // SHEET 1: Dashboard
            appendLine("""<Worksheet ss:Name="Dashboard">""")
            appendLine("""<Table>""")
            appendLine("""<Column ss:Width="200"/><Column ss:Width="150"/><Column ss:Width="150"/><Column ss:Width="150"/>""")

            // --- Title ---
            appendLine("""<Row><Cell ss:StyleID="Title" ss:MergeAcross="3"><Data ss:Type="String">Pro Network - ${data.reportTitle}</Data></Cell></Row>""")
            appendLine("""<Row><Cell ss:StyleID="SubTitle" ss:MergeAcross="3"><Data ss:Type="String">Generated: ${data.generatedDate} | Type: ${data.reportType.name}</Data></Cell></Row>""")
            appendLine("""<Row></Row>""")

            // --- KPIs ---
            val rateStyle = when {
                data.grandCollectionRate >= 80 -> "KpiGood"
                data.grandCollectionRate >= 50 -> "KpiWarn"
                else -> "KpiBad"
            }

            val paidCount = data.months.sumOf { it.paidCount }
            val partialCount = data.months.sumOf { it.partialCount }
            val unpaidCount = data.months.sumOf { it.unpaidCount }

            appendLine("""<Row><Cell ss:StyleID="SectionHeader" ss:MergeAcross="3"><Data ss:Type="String">Key Performance Indicators</Data></Cell></Row>""")
            appendLine("""<Row><Cell ss:StyleID="Cell"><Data ss:Type="String">Total Clients</Data></Cell><Cell ss:StyleID="Cell"><Data ss:Type="Number">${data.grandTotalClients}</Data></Cell><Cell ss:StyleID="Cell"><Data ss:Type="String">Collection Rate</Data></Cell><Cell ss:StyleID="$rateStyle"><Data ss:Type="String">${"%.1f".format(data.grandCollectionRate)}%</Data></Cell></Row>""")
            appendLine("""<Row><Cell ss:StyleID="Cell"><Data ss:Type="String">✅ Fully Paid</Data></Cell><Cell ss:StyleID="Paid"><Data ss:Type="Number">$paidCount</Data></Cell><Cell ss:StyleID="Cell"><Data ss:Type="String">⚠️ Partial</Data></Cell><Cell ss:StyleID="Partial"><Data ss:Type="Number">$partialCount</Data></Cell></Row>""")
            appendLine("""<Row><Cell ss:StyleID="Cell"><Data ss:Type="String">❌ Unpaid</Data></Cell><Cell ss:StyleID="Unpaid"><Data ss:Type="Number">$unpaidCount</Data></Cell><Cell ss:StyleID="Cell"><Data ss:Type="String">Avg/Month</Data></Cell><Cell ss:StyleID="Currency"><Data ss:Type="Number">${data.avgMonthlyCollection}</Data></Cell></Row>""")
            appendLine("""<Row></Row>""")

            // --- Financial Summary ---
            appendLine("""<Row><Cell ss:StyleID="SectionHeader" ss:MergeAcross="3"><Data ss:Type="String">Financial Summary</Data></Cell></Row>""")
            appendLine("""<Row><Cell ss:StyleID="Header"><Data ss:Type="String">Description</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Amount (SAR)</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Percentage</Data></Cell><Cell/></Row>""")
            appendLine("""<Row><Cell ss:StyleID="Cell"><Data ss:Type="String">Total Expected</Data></Cell><Cell ss:StyleID="Currency"><Data ss:Type="Number">${data.grandTotalExpected}</Data></Cell><Cell ss:StyleID="Cell"><Data ss:Type="String">100%</Data></Cell><Cell/></Row>""")
            appendLine("""<Row><Cell ss:StyleID="Cell"><Data ss:Type="String">Total Collected</Data></Cell><Cell ss:StyleID="Currency"><Data ss:Type="Number">${data.grandTotalCollected}</Data></Cell><Cell ss:StyleID="KpiGood"><Data ss:Type="String">${"%.1f".format(data.grandCollectionRate)}%</Data></Cell><Cell/></Row>""")

            val remainingPct = if (data.grandTotalExpected > 0) ((data.grandTotalRemaining / data.grandTotalExpected) * 100) else 0.0
            appendLine("""<Row><Cell ss:StyleID="Cell"><Data ss:Type="String">Total Remaining</Data></Cell><Cell ss:StyleID="Currency"><Data ss:Type="Number">${data.grandTotalRemaining}</Data></Cell><Cell ss:StyleID="KpiBad"><Data ss:Type="String">${"%.1f".format(remainingPct)}%</Data></Cell><Cell/></Row>""")
            appendLine("""<Row></Row>""")

            // --- Trend (multi-month only) ---
            if (data.months.size > 1) {
                val trendStyle = when {
                    data.trend.contains("Improving") -> "TrendUp"
                    data.trend.contains("Declining") -> "TrendDown"
                    else -> "TrendStable"
                }
                appendLine("""<Row><Cell ss:StyleID="SectionHeader" ss:MergeAcross="3"><Data ss:Type="String">Trend Analysis</Data></Cell></Row>""")
                appendLine("""<Row><Cell ss:StyleID="Cell"><Data ss:Type="String">Trend</Data></Cell><Cell ss:StyleID="$trendStyle" ss:MergeAcross="2"><Data ss:Type="String">${data.trend}</Data></Cell></Row>""")
                appendLine("""<Row><Cell ss:StyleID="Cell"><Data ss:Type="String">Best Month</Data></Cell><Cell ss:StyleID="KpiGood"><Data ss:Type="String">${data.bestMonth ?: "-"}</Data></Cell><Cell ss:StyleID="Cell"><Data ss:Type="String">Worst Month</Data></Cell><Cell ss:StyleID="KpiBad"><Data ss:Type="String">${data.worstMonth ?: "-"}</Data></Cell></Row>""")
                appendLine("""<Row></Row>""")

                // Monthly Comparison
                appendLine("""<Row><Cell ss:StyleID="SectionHeader" ss:MergeAcross="3"><Data ss:Type="String">Monthly Comparison</Data></Cell></Row>""")
                appendLine("""<Row><Cell ss:StyleID="Header"><Data ss:Type="String">Month</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Expected</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Collected</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Rate</Data></Cell></Row>""")
                data.months.forEach { m ->
                    val mStyle = when {
                        m.collectionRate >= 80 -> "Paid"
                        m.collectionRate >= 50 -> "Partial"
                        else -> "Unpaid"
                    }
                    appendLine("""<Row><Cell ss:StyleID="Cell"><Data ss:Type="String">${m.month}</Data></Cell><Cell ss:StyleID="Currency"><Data ss:Type="Number">${m.totalExpected}</Data></Cell><Cell ss:StyleID="Currency"><Data ss:Type="Number">${m.totalCollected}</Data></Cell><Cell ss:StyleID="$mStyle"><Data ss:Type="String">${"%.1f".format(m.collectionRate)}%</Data></Cell></Row>""")
                }
                appendLine("""<Row><Cell ss:StyleID="TotalRow"><Data ss:Type="String">TOTAL</Data></Cell><Cell ss:StyleID="TotalRow"><Data ss:Type="Number">${data.grandTotalExpected}</Data></Cell><Cell ss:StyleID="TotalRow"><Data ss:Type="Number">${data.grandTotalCollected}</Data></Cell><Cell ss:StyleID="TotalRow"><Data ss:Type="String">${"%.1f".format(data.grandCollectionRate)}%</Data></Cell></Row>""")
                appendLine("""<Row></Row>""")
            }

            // --- Collection by Building ---
            val allBuildings = data.months
                .flatMap { it.buildings }
                .groupBy { it.buildingName }
                .map { (name, list) ->
                    BuildingSummary(
                        name,
                        list.maxOf { it.clientCount },
                        list.sumOf { it.totalExpected },
                        list.sumOf { it.totalCollected },
                        list.sumOf { it.totalRemaining },
                        if (list.sumOf { it.totalExpected } > 0)
                            (list.sumOf { it.totalCollected } / list.sumOf { it.totalExpected }) * 100
                        else 0.0
                    )
                }
                .sortedByDescending { it.totalCollected }

            appendLine("""<Row><Cell ss:StyleID="SectionHeader" ss:MergeAcross="3"><Data ss:Type="String">Collection by Building</Data></Cell></Row>""")
            appendLine("""<Row><Cell ss:StyleID="Header"><Data ss:Type="String">Building</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Clients</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Collected</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Rate</Data></Cell></Row>""")
            allBuildings.forEach { b ->
                val bStyle = when {
                    b.collectionRate >= 80 -> "Paid"
                    b.collectionRate >= 50 -> "Partial"
                    else -> "Unpaid"
                }
                appendLine("""<Row><Cell ss:StyleID="Cell"><Data ss:Type="String">${b.buildingName}</Data></Cell><Cell ss:StyleID="Cell"><Data ss:Type="Number">${b.clientCount}</Data></Cell><Cell ss:StyleID="Currency"><Data ss:Type="Number">${b.totalCollected}</Data></Cell><Cell ss:StyleID="$bStyle"><Data ss:Type="String">${"%.1f".format(b.collectionRate)}%</Data></Cell></Row>""")
            }

            // --- Smart Insights ---
            val insights = generateSmartInsights(data)
            if (insights.isNotEmpty()) {
                appendLine("""<Row></Row>""")
                appendLine("""<Row><Cell ss:StyleID="SectionHeader" ss:MergeAcross="3"><Data ss:Type="String">Smart Insights &amp; Alerts</Data></Cell></Row>""")
                insights.forEach { insight ->
                    val style = when (insight.level) {
                        "CRITICAL" -> "KpiBad"
                        "WARNING" -> "KpiWarn"
                        "SUCCESS" -> "KpiGood"
                        else -> "Cell"
                    }
                    appendLine("""<Row><Cell ss:StyleID="$style"><Data ss:Type="String">${insight.icon} ${insight.title}</Data></Cell><Cell ss:StyleID="Cell" ss:MergeAcross="2"><Data ss:Type="String">${insight.detail}</Data></Cell></Row>""")
                }
            }

            appendLine("""</Table></Worksheet>""")

            // DETAILED only: Client Details + Transactions
            if (data.reportType == PaymentReportType.DETAILED) {

                // SHEET 2: Client Details
                data.months.forEach { monthData ->
                    appendLine("""<Worksheet ss:Name="Clients ${monthData.month}">""")
                    appendLine("""<Table>""")
                    appendLine("""<Column ss:Width="120"/><Column ss:Width="80"/><Column ss:Width="90"/><Column ss:Width="70"/><Column ss:Width="50"/><Column ss:Width="100"/><Column ss:Width="80"/><Column ss:Width="80"/><Column ss:Width="80"/><Column ss:Width="70"/><Column ss:Width="110"/><Column ss:Width="180"/>""")

                    appendLine("""<Row><Cell ss:StyleID="Title" ss:MergeAcross="11"><Data ss:Type="String">Client Details — ${monthData.month}</Data></Cell></Row>""")
                    appendLine("""<Row><Cell ss:StyleID="Header"><Data ss:Type="String">Client</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Sub#</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Phone</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Package</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Room</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Building</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Amount</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Paid</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Remaining</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Status</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Last Payment</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Notes</Data></Cell></Row>""")

                    monthData.clients.forEachIndexed { index, c ->
                        val rowStyle = if (index % 2 == 0) "Cell" else "CellAlt"
                        val curStyle = if (index % 2 == 0) "Currency" else "CurrencyAlt"
                        val statusStyle = when (c.status) {
                            "PAID" -> "Paid"
                            "PARTIAL" -> "Partial"
                            else -> "Unpaid"
                        }

                        val clientNote = c.clientNote.orEmpty()
                        val txnNote = c.transactions.lastOrNull()?.notes.orEmpty()
                        val notes = when {
                            clientNote.isNotEmpty() && txnNote.isNotEmpty() -> "$clientNote | $txnNote"
                            clientNote.isNotEmpty() -> clientNote
                            else -> txnNote
                        }

                        appendLine(
                            """<Row><Cell ss:StyleID="$rowStyle"><Data ss:Type="String">${c.clientName}</Data></Cell>""" +
                                    """<Cell ss:StyleID="$rowStyle"><Data ss:Type="String">${c.subscriptionNumber}</Data></Cell>""" +
                                    """<Cell ss:StyleID="$rowStyle"><Data ss:Type="String">${c.phone}</Data></Cell>""" +
                                    """<Cell ss:StyleID="$rowStyle"><Data ss:Type="String">${c.packageType}</Data></Cell>""" +
                                    """<Cell ss:StyleID="$rowStyle"><Data ss:Type="String">${c.roomNumber ?: "-"}</Data></Cell>""" +
                                    """<Cell ss:StyleID="$rowStyle"><Data ss:Type="String">${c.buildingName}</Data></Cell>""" +
                                    """<Cell ss:StyleID="$curStyle"><Data ss:Type="Number">${c.monthlyAmount}</Data></Cell>""" +
                                    """<Cell ss:StyleID="$curStyle"><Data ss:Type="Number">${c.totalPaid}</Data></Cell>""" +
                                    """<Cell ss:StyleID="$curStyle"><Data ss:Type="Number">${c.remaining}</Data></Cell>""" +
                                    """<Cell ss:StyleID="$statusStyle"><Data ss:Type="String">${c.statusIcon} ${c.status}</Data></Cell>""" +
                                    """<Cell ss:StyleID="$rowStyle"><Data ss:Type="String">${c.lastPaymentDate ?: "-"}</Data></Cell>""" +
                                    """<Cell ss:StyleID="$rowStyle"><Data ss:Type="String">$notes</Data></Cell></Row>"""
                        )
                    }

                    // TOTAL row
                    appendLine(
                        """<Row><Cell ss:StyleID="TotalRow" ss:MergeAcross="5"><Data ss:Type="String">TOTAL</Data></Cell>""" +
                                """<Cell ss:StyleID="TotalRow"><Data ss:Type="Number">${monthData.totalExpected}</Data></Cell>""" +
                                """<Cell ss:StyleID="TotalRow"><Data ss:Type="Number">${monthData.totalCollected}</Data></Cell>""" +
                                """<Cell ss:StyleID="TotalRow"><Data ss:Type="Number">${monthData.totalRemaining}</Data></Cell>""" +
                                """<Cell ss:StyleID="TotalRow"><Data ss:Type="String">${"%.1f".format(monthData.collectionRate)}%</Data></Cell>""" +
                                """<Cell ss:StyleID="TotalRow"/><Cell ss:StyleID="TotalRow"/></Row>"""
                    )

                    appendLine("""</Table></Worksheet>""")
                }

                // SHEET 3: Transactions
                appendLine("""<Worksheet ss:Name="Transactions">""")
                appendLine("""<Table>""")
                appendLine("""<Column ss:Width="80"/><Column ss:Width="120"/><Column ss:Width="100"/><Column ss:Width="130"/><Column ss:Width="80"/><Column ss:Width="110"/><Column ss:Width="180"/>""")

                appendLine("""<Row><Cell ss:StyleID="Title" ss:MergeAcross="6"><Data ss:Type="String">Transaction Audit Trail</Data></Cell></Row>""")
                appendLine("""<Row><Cell ss:StyleID="Header"><Data ss:Type="String">Month</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Client</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Building</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Date</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Amount</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Type</Data></Cell><Cell ss:StyleID="Header"><Data ss:Type="String">Notes</Data></Cell></Row>""")

                var txRowIdx = 0
                data.months.forEach { monthData ->
                    monthData.clients.forEach { c ->
                        c.transactions.forEach { tx ->
                            val rowStyle = if (txRowIdx % 2 == 0) "Cell" else "CellAlt"
                            val curStyle = if (txRowIdx % 2 == 0) "Currency" else "CurrencyAlt"
                            appendLine(
                                """<Row><Cell ss:StyleID="$rowStyle"><Data ss:Type="String">${monthData.month}</Data></Cell>""" +
                                        """<Cell ss:StyleID="$rowStyle"><Data ss:Type="String">${c.clientName}</Data></Cell>""" +
                                        """<Cell ss:StyleID="$rowStyle"><Data ss:Type="String">${c.buildingName}</Data></Cell>""" +
                                        """<Cell ss:StyleID="$rowStyle"><Data ss:Type="String">${tx.date}</Data></Cell>""" +
                                        """<Cell ss:StyleID="$curStyle"><Data ss:Type="Number">${tx.amount}</Data></Cell>""" +
                                        """<Cell ss:StyleID="$rowStyle"><Data ss:Type="String">${tx.type}</Data></Cell>""" +
                                        """<Cell ss:StyleID="$rowStyle"><Data ss:Type="String">${tx.notes}</Data></Cell></Row>"""
                            )
                            txRowIdx++
                        }
                    }
                }

                appendLine("""</Table></Worksheet>""")
            }

            appendLine("""</Workbook>""")
        }
    }

// ===================== PDF BUILDER =====================

    private fun buildPdf(data: PaymentReportData): ByteArray {
        val pdfDocument = android.graphics.pdf.PdfDocument()

        // Paint definitions
        val titlePaint = android.graphics.Paint().apply {
            textSize = 18f
            color = android.graphics.Color.parseColor("#673AB7")
            isFakeBoldText = true
        }

        val headerPaint = android.graphics.Paint().apply {
            textSize = 8f
            color = android.graphics.Color.WHITE
            isFakeBoldText = true
        }

        val cellPaint = android.graphics.Paint().apply {
            textSize = 7f
            color = android.graphics.Color.BLACK
        }

        val sectionPaint = android.graphics.Paint().apply {
            textSize = 12f
            color = android.graphics.Color.parseColor("#4527A0")
            isFakeBoldText = true
        }

        val kpiPaint = android.graphics.Paint().apply {
            textSize = 9f
            isFakeBoldText = true
        }

        val fillPaint = android.graphics.Paint()

        val pageWidth = 842
        val pageHeight = 595
        val margin = 20f
        var yPos = 45f
        var pageNum = 1

        var pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        fun checkNewPage() {
            if (yPos > pageHeight - 40) {
                pdfDocument.finishPage(page)
                pageNum++
                pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                yPos = 30f
            }
        }

        // ===================== DASHBOARD (All Report Types) =====================
        // Title
        canvas.drawText("Pro Network - ${data.reportTitle}", margin, yPos, titlePaint)
        yPos += 18f
        cellPaint.textSize = 8f
        canvas.drawText("Generated: ${data.generatedDate} | Type: ${data.reportType.name} | Trend: ${data.trend}", margin, yPos, cellPaint)
        yPos += 20f

        // KPIs
        canvas.drawText("Key Performance Indicators", margin, yPos, sectionPaint)
        yPos += 14f

        val paidCount = data.months.sumOf { it.paidCount }
        val partialCount = data.months.sumOf { it.partialCount }
        val unpaidCount = data.months.sumOf { it.unpaidCount }

        cellPaint.textSize = 7f
        canvas.drawText("Total Clients: ${data.grandTotalClients} | ✅ Fully Paid: $paidCount | ⚠  Partial: $partialCount | ✘ Unpaid: $unpaidCount", margin, yPos, cellPaint)
        yPos += 11f
        canvas.drawText("Expected: ${"%.2f".format(data.grandTotalExpected)} SAR | Collected: ${"%.2f".format(data.grandTotalCollected)} SAR | Remaining: ${"%.2f".format(data.grandTotalRemaining)} SAR | Rate: ${"%.1f".format(data.grandCollectionRate)}%", margin, yPos, cellPaint)
        yPos += 11f

        if (data.months.size > 1) {
            canvas.drawText("Best: ${data.bestMonth ?: "-"} | Worst: ${data.worstMonth ?: "-"} | Avg/Month: ${"%.2f".format(data.avgMonthlyCollection)} SAR", margin, yPos, cellPaint)
            yPos += 18f

            // Monthly Comparison
            checkNewPage()
            canvas.drawText("Monthly Comparison", margin, yPos, sectionPaint)
            yPos += 14f

            fillPaint.color = android.graphics.Color.parseColor("#7E57C2")
            canvas.drawRect(margin, yPos - 10f, pageWidth - margin, yPos + 4f, fillPaint)

            val compHeaders = listOf("Month", "Expected", "Collected", "Rate")
            val compW = listOf(120f, 120f, 120f, 80f)
            var xP = margin
            compHeaders.forEachIndexed { i, h ->
                canvas.drawText(h, xP + 2f, yPos, headerPaint)
                xP += compW[i]
            }
            yPos += 14f

            data.months.forEach { m ->
                checkNewPage()
                xP = margin
                listOf(
                    m.month,
                    "%.2f".format(m.totalExpected),
                    "%.2f".format(m.totalCollected),
                    "%.1f".format(m.collectionRate) + "%"
                ).forEachIndexed { i, d ->
                    canvas.drawText(d, xP + 2f, yPos, cellPaint)
                    xP += compW[i]
                }
                yPos += 12f
            }
            yPos += 8f
        } else {
            yPos += 8f
        }

        // Collection by Building
        val allBuildings = data.months
            .flatMap { it.buildings }
            .groupBy { it.buildingName }
            .map { (name, list) ->
                BuildingSummary(
                    name,
                    list.maxOf { it.clientCount },
                    list.sumOf { it.totalExpected },
                    list.sumOf { it.totalCollected },
                    list.sumOf { it.totalRemaining },
                    if (list.sumOf { it.totalExpected } > 0)
                        (list.sumOf { it.totalCollected } / list.sumOf { it.totalExpected }) * 100
                    else 0.0
                )
            }
            .sortedByDescending { it.totalCollected }

        checkNewPage()
        canvas.drawText("Collection by Building", margin, yPos, sectionPaint)
        yPos += 14f

        fillPaint.color = android.graphics.Color.parseColor("#7E57C2")
        canvas.drawRect(margin, yPos - 10f, pageWidth - margin, yPos + 4f, fillPaint)

        val bHeaders = listOf("Building", "Clients", "Collected", "Rate")
        val bW = listOf(160f, 80f, 120f, 100f)
        var bx = margin
        bHeaders.forEachIndexed { i, h ->
            canvas.drawText(h, bx + 2f, yPos, headerPaint)
            bx += bW[i]
        }
        yPos += 14f

        allBuildings.forEach { b ->
            checkNewPage()
            bx = margin
            listOf(
                b.buildingName,
                b.clientCount.toString(),
                "%.2f".format(b.totalCollected),
                "%.1f".format(b.collectionRate) + "%"
            ).forEachIndexed { i, d ->
                canvas.drawText(d, bx + 2f, yPos, cellPaint)
                bx += bW[i]
            }
            yPos += 12f
        }
        yPos += 8f

        // Smart Insights
        val insights = generateSmartInsights(data)
        if (insights.isNotEmpty()) {
            checkNewPage()
            canvas.drawText("Smart Insights & Alerts", margin, yPos, sectionPaint)
            yPos += 14f

            insights.forEach { insight ->
                checkNewPage()
                val insightColor = when (insight.level) {
                    "CRITICAL" -> android.graphics.Color.parseColor("#C62828")
                    "WARNING" -> android.graphics.Color.parseColor("#F57F17")
                    "SUCCESS" -> android.graphics.Color.parseColor("#2E7D32")
                    else -> android.graphics.Color.parseColor("#1565C0")
                }
                kpiPaint.color = insightColor
                canvas.drawText("${insight.icon} ${insight.title}: ${insight.detail}", margin, yPos, kpiPaint)
                yPos += 12f
            }
            yPos += 8f
        }

        // ===================== DETAILED ONLY: Client Details =====================

        if (data.reportType == PaymentReportType.DETAILED) {
            data.months.forEach { monthData ->
                checkNewPage()
                canvas.drawText("Client Details - ${monthData.month}", margin, yPos, sectionPaint)
                yPos += 14f

                fillPaint.color = android.graphics.Color.parseColor("#7E57C2")
                canvas.drawRect(margin, yPos - 10f, pageWidth - margin, yPos + 4f, fillPaint)

                val cols = listOf("Name", "Sub#", "Package", "Building", "Room", "Amount", "Paid", "Rem.", "Status", "Notes")
                val colW = listOf(85f, 65f, 65f, 85f, 45f, 65f, 65f, 65f, 65f, 150f)

                var xPos = margin
                cols.forEachIndexed { i, h ->
                    canvas.drawText(h, xPos + 2f, yPos, headerPaint)
                    xPos += colW[i]
                }
                yPos += 14f

                monthData.clients.forEachIndexed { index, c ->
                    checkNewPage()

                    // Zebra striping
                    if (index % 2 == 1) {
                        fillPaint.color = android.graphics.Color.parseColor("#F5F5F5")
                        canvas.drawRect(margin, yPos - 9f, pageWidth - margin, yPos + 3f, fillPaint)
                    }

                    val clientNote = c.transactions.lastOrNull()?.notes.orEmpty()
                    val notes = clientNote

                    val rowValues = listOf(
                        c.clientName,
                        c.subscriptionNumber,
                        c.packageType,
                        c.buildingName,
                        c.roomNumber ?: "",
                        "%.2f".format(c.monthlyAmount),
                        "%.2f".format(c.totalPaid),
                        "%.2f".format(c.remaining),
                        "${c.statusIcon} ${c.status}",
                        notes
                    )

                    xPos = margin
                    rowValues.forEachIndexed { i, d ->
                        val maxLen = if (i == cols.lastIndex) 22 else 14
                        val text = if (d.length > maxLen) d.take(maxLen - 2) + ".." else d
                        canvas.drawText(text, xPos + 2f, yPos, cellPaint)
                        xPos += colW[i]
                    }
                    yPos += 12f
                }

                // TOTAL Row
                checkNewPage()
                fillPaint.color = android.graphics.Color.parseColor("#9575CD")
                canvas.drawRect(margin, yPos - 9f, pageWidth - margin, yPos + 3f, fillPaint)

                val totalPaint = android.graphics.Paint().apply {
                    textSize = 8f
                    color = android.graphics.Color.WHITE
                    isFakeBoldText = true
                }

                xPos = margin
                canvas.drawText("TOTAL", xPos + 2f, yPos, totalPaint)
                xPos += colW[0] + colW[1] + colW[2] + colW[3] + colW[4] // Skip first 5 cols
                canvas.drawText("%.2f".format(monthData.totalExpected), xPos + 2f, yPos, totalPaint)
                xPos += colW[5]
                canvas.drawText("%.2f".format(monthData.totalCollected), xPos + 2f, yPos, totalPaint)
                xPos += colW[6]
                canvas.drawText("%.2f".format(monthData.totalRemaining), xPos + 2f, yPos, totalPaint)
                xPos += colW[7]
                canvas.drawText("%.1f".format(monthData.collectionRate) + "%", xPos + 2f, yPos, totalPaint)

                yPos += 18f
            }

            // ===================== Transaction Audit Trail ==========================

            checkNewPage()
            canvas.drawText("Transaction Audit Trail", margin, yPos, sectionPaint)
            yPos += 14f

            fillPaint.color = android.graphics.Color.parseColor("#7E57C2")
            canvas.drawRect(margin, yPos - 10f, pageWidth - margin, yPos + 4f, fillPaint)

            val tHeaders = listOf("Month", "Client", "Building", "Date", "Amount", "Type", "Notes")
            val tW = listOf(65f, 95f, 85f, 100f, 65f, 75f, 160f)

            var txX = margin
            tHeaders.forEachIndexed { i, h ->
                canvas.drawText(h, txX + 2f, yPos, headerPaint)
                txX += tW[i]
            }
            yPos += 14f

            var txRow = 0
            data.months.forEach { monthData ->
                monthData.clients.forEach { c ->
                    c.transactions.forEach { tx ->
                        checkNewPage()

                        if (txRow % 2 == 1) {
                            fillPaint.color = android.graphics.Color.parseColor("#F5F5F5")
                            canvas.drawRect(margin, yPos - 9f, pageWidth - margin, yPos + 3f, fillPaint)
                        }

                        txX = margin
                        val rowVals = listOf(
                            monthData.month,
                            c.clientName,
                            c.buildingName,
                            tx.date,
                            "%.2f".format(tx.amount),
                            tx.type,
                            tx.notes
                        )

                        rowVals.forEachIndexed { i, d ->
                            val maxLen = if (i == tHeaders.lastIndex) 24 else 16
                            val text = if (d.length > maxLen) d.take(maxLen - 2) + ".." else d
                            canvas.drawText(text, txX + 2f, yPos, cellPaint)
                            txX += tW[i]
                        }
                        yPos += 12f
                        txRow++
                    }
                }
            }
        }

        // Finish last page and return PDF
        pdfDocument.finishPage(page)
        val outputStream = java.io.ByteArrayOutputStream()
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
        return outputStream.toByteArray()
    }

    // ===================== EXPORT FUNCTIONS =====================

    suspend fun exportExcelToDownloads(
        reportType: PaymentReportType,
        period: PaymentReportPeriod,
        startMonth: String,
        endMonth: String?,
        buildingFilter: Int?,
        packageFilter: String?,
        statusFilter: PaymentReportFilter
    ) {
        withContext(Dispatchers.IO) {
            val data =
                gatherFullReport(
                    reportType,
                    period,
                    startMonth,
                    endMonth,
                    buildingFilter,
                    packageFilter,
                    statusFilter
                )
            val xml = buildExcelXml(data)
            val fileName = "payment_report_${startMonth}_${period.name.lowercase()}.xls"

            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/vnd.ms-excel")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri =
                context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { os ->
                    os.write(xml.toByteArray(Charsets.UTF_8))
                }
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Saved to Downloads: $fileName", Toast.LENGTH_LONG).show()
            }
        }
    }

    suspend fun shareExcel(
        reportType: PaymentReportType,
        period: PaymentReportPeriod,
        startMonth: String,
        endMonth: String?,
        buildingFilter: Int?,
        packageFilter: String?,
        statusFilter: PaymentReportFilter
    ) {
        withContext(Dispatchers.IO) {
            val data =
                gatherFullReport(
                    reportType,
                    period,
                    startMonth,
                    endMonth,
                    buildingFilter,
                    packageFilter,
                    statusFilter
                )
            val xml = buildExcelXml(data)
            val fileName = "payment_report_${startMonth}_${period.name.lowercase()}.xls"

            val file = java.io.File(context.cacheDir, fileName)
            file.writeText(xml, Charsets.UTF_8)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            withContext(Dispatchers.Main) {
                try {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/vnd.ms-excel"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(
                        Intent.createChooser(intent, "Share Payment Report").apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                } catch (e: Exception) {
                    Toast.makeText(context, "Share failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    suspend fun exportPdfToDownloads(
        reportType: PaymentReportType,
        period: PaymentReportPeriod,
        startMonth: String,
        endMonth: String?,
        buildingFilter: Int?,
        packageFilter: String?,
        statusFilter: PaymentReportFilter
    ) {
        withContext(Dispatchers.IO) {
            val data =
                gatherFullReport(
                    reportType,
                    period,
                    startMonth,
                    endMonth,
                    buildingFilter,
                    packageFilter,
                    statusFilter
                )
            val pdfBytes = buildPdf(data)
            val fileName = "payment_report_${startMonth}_${period.name.lowercase()}.pdf"

            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri =
                context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { os ->
                    os.write(pdfBytes)
                }
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Saved to Downloads: $fileName", Toast.LENGTH_LONG).show()
            }
        }
    }
}
