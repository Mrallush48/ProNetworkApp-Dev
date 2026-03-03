package com.pronetwork.app.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.pronetwork.app.data.Building
import com.pronetwork.app.data.BuildingDao
import com.pronetwork.app.network.SyncEngine
import com.pronetwork.app.network.SyncWorker
import com.pronetwork.util.OptimisticLockException
import com.pronetwork.util.generateId
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuildingRepository @Inject constructor(
    private val buildingDao: BuildingDao,
    private val syncEngine: SyncEngine,
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    val buildings: LiveData<List<Building>> = buildingDao.getAllBuildings()

    fun searchBuildings(search: String) = buildingDao.searchBuildings(search)

    suspend fun insert(building: Building): String {
        val newBuilding = building.copy(
            id = generateId(),
            updatedAt = System.currentTimeMillis(),
            version = 1
        )
        buildingDao.insert(newBuilding)
        enqueueSync("building", newBuilding.id, "CREATE", newBuilding)
        return newBuilding.id
    }

    suspend fun update(building: Building) {
        val updated = building.copy(
            version = building.version + 1,
            updatedAt = System.currentTimeMillis()
        )
        val rows = buildingDao.updateWithVersionCheck(
            id = updated.id,
            name = updated.name,
            location = updated.location,
            notes = updated.notes,
            floors = updated.floors,
            managerName = updated.managerName,
            updatedAt = updated.updatedAt,
            newVersion = updated.version,
            expectedVersion = building.version
        )
        if (rows == 0) throw OptimisticLockException("building", building.id)
        enqueueSync("building", updated.id, "UPDATE", updated)
    }

    suspend fun delete(building: Building) {
        buildingDao.delete(building)
        enqueueSync("building", building.id, "DELETE", building)
    }

    /**
     * إضافة العملية لقائمة المزامنة + تشغيل sync فوري
     * يعمل بصمت — أي خطأ في الـ enqueue لا يؤثر على العملية الأساسية
     */
    private suspend fun enqueueSync(entityType: String, entityId: String, action: String, entity: Any) {
        try {
            syncEngine.enqueue(
                entityType = entityType,
                entityId = entityId,
                action = action,
                payload = gson.toJson(entity)
            )
            SyncWorker.syncNow(context)
        } catch (e: Exception) {
            android.util.Log.w("BuildingRepository", "Sync enqueue failed: ${e.message}")
        }
    }
}
