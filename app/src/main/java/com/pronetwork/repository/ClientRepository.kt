package com.pronetwork.app.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.pronetwork.app.data.Client
import com.pronetwork.app.data.ClientDao
import com.pronetwork.app.network.SyncEngine
import com.pronetwork.app.network.SyncWorker
import com.google.gson.Gson

class ClientRepository(
    private val clientDao: ClientDao,
    private val syncEngine: SyncEngine? = null,
    private val context: Context? = null
) {
    private val gson = Gson()

    val clients: LiveData<List<Client>> = clientDao.getAllClients()
    fun searchClients(search: String) = clientDao.searchClients(search)
    fun getClientsCount() = clientDao.getClientsCount()

    suspend fun insert(client: Client): Long {
        val rowId = clientDao.insert(client)
        enqueueSync("client", rowId.toInt(), "CREATE", client.copy(id = rowId.toInt()))
        return rowId
    }

    suspend fun update(client: Client) {
        clientDao.update(client)
        enqueueSync("client", client.id, "UPDATE", client)
    }

    suspend fun delete(client: Client) {
        clientDao.delete(client)
        enqueueSync("client", client.id, "DELETE", client)
    }

    /**
     * إضافة العملية لقائمة المزامنة + تشغيل sync فوري
     * يعمل بصمت — أي خطأ في الـ enqueue لا يؤثر على العملية الأساسية
     */
    private suspend fun enqueueSync(entityType: String, entityId: Int, action: String, entity: Any) {
        try {
            syncEngine?.enqueue(
                entityType = entityType,
                entityId = entityId,
                action = action,
                payload = gson.toJson(entity)
            )
            // تشغيل مزامنة فورية في الخلفية
            context?.let { SyncWorker.syncNow(it) }
        } catch (e: Exception) {
            // لا نوقف العملية المحلية أبداً بسبب فشل الـ enqueue
            android.util.Log.w("ClientRepository", "Sync enqueue failed: ${e.message}")
        }
    }
}
