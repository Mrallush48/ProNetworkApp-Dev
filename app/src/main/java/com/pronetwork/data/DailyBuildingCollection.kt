package com.pronetwork.app.data



/**
 * حركة واحدة لعميل في يوم معيّن
 */
data class DailyTransactionItem(
    val amount: Double,
    val time: String,
    val type: String,       // "Payment" or "Refund"
    val notes: String
)

/**
 * تفاصيل تحصيل عميل واحد في يوم معيّن
 */
data class DailyClientCollection(
    val clientId: Int,
    val clientName: String,
    val subscriptionNumber: String,
    val roomNumber: String?,
    val packageType: String,
    val monthlyAmount: Double,
    val paidAmount: Double,
    val transactionTime: String,
    val notes: String,
    val transactions: List<DailyTransactionItem> = emptyList(),
    val paymentStatus: String = ""
)


/**
 * نتيجة استعلام Room البسيط — تحصيل مبنى يومي
 * يستخدم فقط مع getDailyBuildingCollectionsForDay
 */
data class DailyBuildingCollection(
    val buildingId: Int,
    val buildingName: String,
    val totalAmount: Double,
    val clientsCount: Int
)

/**
 * موديل عرض مبنى تفصيلي — يُبنى في ViewModel
 * يحتوي على كل بيانات المبنى + قائمة العملاء
 */
data class DailyBuildingDetailedUi(
    val buildingId: Int,
    val buildingName: String,
    val totalAmount: Double,
    val clientsCount: Int,
    val expectedAmount: Double = 0.0,
    val collectionRate: Double = 0.0,
    val clients: List<DailyClientCollection> = emptyList()
)
