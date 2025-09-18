package com.pronetwork.app.repository

import com.pronetwork.app.data.Client
import com.pronetwork.app.data.ClientDao
import kotlinx.coroutines.flow.Flow

class ClientRepository(private val dao: ClientDao) {

    fun getClientsByBuildingAndMonth(buildingId: Int, month: String): Flow<List<Client>> {
        return dao.getClientsByBuildingAndMonth(buildingId, month)
    }

    fun searchClients(buildingId: Int, month: String, query: String): Flow<List<Client>> {
        return dao.searchClients(buildingId, month, query)
    }

    suspend fun insertClient(client: Client) {
        dao.insert(client)
    }

    suspend fun updateClient(client: Client) {
        dao.update(client)
    }

    suspend fun deleteClient(client: Client) {
        dao.delete(client)
    }

    /**
     * حذف "ناعم" — يضع endMonth = month (اختياري للاستعمال لاحقاً)
     */
    suspend fun softDeleteClient(client: Client, month: String) {
        val updated = client.copy(endMonth = month)
        dao.update(updated)
    }
}
