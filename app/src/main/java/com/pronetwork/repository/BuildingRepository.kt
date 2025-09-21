package com.pronetwork.app.repository

import androidx.lifecycle.LiveData
import com.pronetwork.app.data.Building
import com.pronetwork.app.data.BuildingDao

class BuildingRepository(private val buildingDao: BuildingDao) {
    val buildings: LiveData<List<Building>> = buildingDao.getAllBuildings()
    fun searchBuildings(search: String) = buildingDao.searchBuildings(search)

    suspend fun insert(building: Building) = buildingDao.insert(building)
    suspend fun update(building: Building) = buildingDao.update(building)
    suspend fun delete(building: Building) = buildingDao.delete(building)
}