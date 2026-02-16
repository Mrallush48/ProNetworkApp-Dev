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
    val lowBuilding: String? = null
)
