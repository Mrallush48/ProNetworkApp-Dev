package com.pronetwork.app.repository

import com.pronetwork.app.data.Building
import com.pronetwork.app.data.BuildingDao
import kotlinx.coroutines.flow.Flow

class BuildingRepository(private val dao: BuildingDao) {
    fun getAllBuildings(): Flow<List<Building>> = dao.getAllBuildings()
    suspend fun addBuilding(building: Building) = dao.insert(building)
    suspend fun updateBuilding(building: Building) = dao.update(building)
    suspend fun deleteBuilding(building: Building) = dao.delete(building)
}
