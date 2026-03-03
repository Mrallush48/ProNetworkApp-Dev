package com.pronetwork.app.data

/**
 * حركة واحدة لعميل في يوم معيّن
 */
data class DailyTransactionItem(
    val amount: Double,
    val time: String,
    val type: String,
    val notes: String
)

/**
 * تفاصيل تحصيل عميل واحد في يوم معيّن
 */
data class DailyClientCollection(
    val clientId: String,
    val clientName: String,
    val subscriptionNumber: String,
    val roomNumber: String?,
    val packageType: String,
    val monthlyAmount: Double,
    val paidAmount: Double,
    val todayPaid: Double = 0.0,
    val totalPaid: Double = 0.0,
    val transactionTime: String,
    val notes: String,
    val transactions: List<DailyTransactionItem> = emptyList(),
    val paymentStatus: String = ""
)

/**
 * نتيجة استعلام Room البسيط — تحصيل مبنى يومي
 */
data class DailyBuildingCollection(
    val buildingId: String,
    val buildingName: String,
    val totalAmount: Double,
    val clientsCount: Int
)

/**
 * موديل عرض مبنى تفصيلي — يُبنى في ViewModel
 */
data class DailyBuildingDetailedUi(
    val buildingId: String,
    val buildingName: String,
    val totalAmount: Double,
    val clientsCount: Int,
    val expectedAmount: Double = 0.0,
    val collectionRate: Double = 0.0,
    val clients: List<DailyClientCollection> = emptyList()
)
