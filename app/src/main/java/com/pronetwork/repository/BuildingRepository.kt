package com.pronetwork.app.repository

import androidx.lifecycle.LiveData
import com.pronetwork.app.data.Building
import com.pronetwork.app.data.BuildingDao
import com.pronetwork.app.data.ClientDatabase

class BuildingRepository(
    private val buildingDao: BuildingDao,
    private val clientDatabase: ClientDatabase? = null
) {
    val buildings: LiveData<List<Building>> = buildingDao.getAllBuildings()
    fun searchBuildings(search: String) = buildingDao.searchBuildings(search)

    suspend fun insert(building: Building): Long {
        // 1. أدخل في قاعدة بيانات المباني الرئيسية (الآن تُرجع Long)
        val insertedRowId = buildingDao.insert(building)

        // 2. تزامن تلقائي إلى client_database
        try {
            // نسخ المبنى مع تحويل الـ Long إلى Int (لأن Building.id هو Int)
            val syncedBuilding = building.copy(id = insertedRowId.toInt())
            clientDatabase?.buildingDao()?.insert(syncedBuilding)
        } catch (e: Exception) {
            // تجاهل أي خطأ في التزامن (لا يؤثر على العملية الأساسية)
            e.printStackTrace()
        }

        return insertedRowId
    }

    suspend fun update(building: Building) {
        buildingDao.update(building)
        try {
            clientDatabase?.buildingDao()?.update(building)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun delete(building: Building) {
        buildingDao.delete(building)
        try {
            clientDatabase?.buildingDao()?.delete(building)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}