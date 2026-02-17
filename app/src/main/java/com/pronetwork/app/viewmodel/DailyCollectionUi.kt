package com.pronetwork.app.viewmodel

import com.pronetwork.app.data.DailyBuildingDetailedUi

data class DailyCollectionUi(
    val dateMillis: Long,
    val totalAmount: Double,
    val buildings: List<DailyBuildingDetailedUi>,
    val totalExpected: Double = 0.0,
    val overallCollectionRate: Double = 0.0,
    val totalClientsCount: Int = 0,
    val topBuilding: String? = null,
    val lowBuilding: String? = null,
    // === حالات الدفع اليومية ===
    val paidClientsCount: Int = 0,
    val partialClientsCount: Int = 0,
    val settledClientsCount: Int = 0,
    val unpaidClientsCount: Int = 0,
    val settledAmount: Double = 0.0,
    val refundAmount: Double = 0.0
)
