package com.pronetwork.app.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.pronetwork.app.data.Building
import com.pronetwork.app.data.BuildingDao
import com.pronetwork.app.data.ClientDatabase
import com.pronetwork.app.network.SyncEngine
import com.pronetwork.app.network.SyncWorker
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuildingRepository @Inject constructor(
    private val buildingDao: BuildingDao,
    private val clientDatabase: ClientDatabase,
    private val syncEngine: SyncEngine,
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    val buildings: LiveData<List<Building>> = buildingDao.getAllBuildings()
    fun searchBuildings(search: String) = buildingDao.searchBuildings(search)

    suspend fun insert(building: Building): Long {
        val insertedRowId = buildingDao.insert(building)

        // تزامن تلقائي إلى client_database
        try {
            val syncedBuilding = building.copy(id = insertedRowId.toInt())
            clientDatabase.buildingDao().insert(syncedBuilding)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        enqueueSync("building", insertedRowId.toInt(), "CREATE", building.copy(id = insertedRowId.toInt()))
        return insertedRowId
    }

    suspend fun update(building: Building) {
        buildingDao.update(building)
        try {
            clientDatabase.buildingDao().update(building)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        enqueueSync("building", building.id, "UPDATE", building)
    }

    suspend fun delete(building: Building) {
        buildingDao.delete(building)
        try {
            clientDatabase.buildingDao().delete(building)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        enqueueSync("building", building.id, "DELETE", building)
    }

    /**
     * إضافة العملية لقائمة المزامنة + تشغيل sync فوري
     * يعمل بصمت — أي خطأ في الـ enqueue لا يؤثر على العملية الأساسية
     */
    private suspend fun enqueueSync(entityType: String, entityId: Int, action: String, entity: Any) {
        try {
            syncEngine.enqueue(
                entityType = entityType,
                entityId = entityId,
                action = action,
                payload = gson.toJson(entity)
            )
            // تشغيل مزامنة فورية في الخلفية
            SyncWorker.syncNow(context)
        } catch (e: Exception) {
            android.util.Log.w("BuildingRepository", "Sync enqueue failed: ${e.message}")
        }
    }
}
