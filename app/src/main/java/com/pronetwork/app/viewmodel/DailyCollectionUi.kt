package com.pronetwork.app.viewmodel

import com.pronetwork.app.data.DailyBuildingCollection

data class DailyCollectionUi(
    val dateMillis: Long,
    val totalAmount: Double,
    val buildings: List<DailyBuildingCollection>
)
