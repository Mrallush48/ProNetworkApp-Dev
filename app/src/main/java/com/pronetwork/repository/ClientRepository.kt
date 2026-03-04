package com.pronetwork.app.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.pronetwork.app.data.Client
import com.pronetwork.app.data.ClientDao
import com.pronetwork.app.network.SyncEngine
import com.pronetwork.app.network.SyncWorker
import com.pronetwork.util.OptimisticLockException
import com.pronetwork.util.generateId
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import com.pronetwork.util.ChecksumKeyManager

@Singleton
class ClientRepository @Inject constructor(
    private val clientDao: ClientDao,
    private val syncEngine: SyncEngine,
    private val checksumKeyManager: ChecksumKeyManager,
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    val clients: LiveData<List<Client>> = clientDao.getAllClients()

    fun searchClients(search: String) = clientDao.searchClients(search)

    fun getClientsCount() = clientDao.getClientsCount()

    suspend fun getClientById(id: String): Client? = clientDao.getClientById(id)

    suspend fun insert(client: Client): String {
        val withFields = client.copy(
            id = generateId(),
            updatedAt = System.currentTimeMillis(),
            version = 1
        )
        val newClient = withFields.copy(
            checksum = withFields.computeChecksum(checksumKeyManager.getSecretKey())
        )
        clientDao.insert(newClient)
        enqueueSync("client", newClient.id, "CREATE", newClient)
        return newClient.id
    }

    suspend fun update(client: Client) {
        val withFields = client.copy(
            version = client.version + 1,
            updatedAt = System.currentTimeMillis()
        )
        val updated = withFields.copy(
            checksum = withFields.computeChecksum(checksumKeyManager.getSecretKey())
        )
        val rows = clientDao.updateWithVersionCheck(
            id = updated.id,
            name = updated.name,
            subscriptionNumber = updated.subscriptionNumber,
            roomNumber = updated.roomNumber,
            mobile = updated.mobile,
            price = updated.price,
            firstMonthAmount = updated.firstMonthAmount,
            buildingId = updated.buildingId,
            startMonth = updated.startMonth,
            startDay = updated.startDay,
            endMonth = updated.endMonth,
            isPaid = updated.isPaid,
            paymentDate = updated.paymentDate,
            phone = updated.phone,
            address = updated.address,
            packageType = updated.packageType,
            notes = updated.notes,
            updatedAt = updated.updatedAt,
            newVersion = updated.version,
            expectedVersion = client.version,
            checksum = updated.checksum
        )
        if (rows == 0) throw OptimisticLockException("client", client.id)
        enqueueSync("client", updated.id, "UPDATE", updated)
    }

    suspend fun delete(client: Client) {
        clientDao.delete(client)
        enqueueSync("client", client.id, "DELETE", client)
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
            android.util.Log.w("ClientRepository", "Sync enqueue failed: ${e.message}")
        }
    }
}
