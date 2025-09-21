package com.pronetwork.app.repository

import androidx.lifecycle.LiveData
import com.pronetwork.app.data.Client
import com.pronetwork.app.data.ClientDao

class ClientRepository(private val clientDao: ClientDao) {
    val clients: LiveData<List<Client>> = clientDao.getAllClients()
    fun searchClients(search: String) = clientDao.searchClients(search)
    fun getClientsCount() = clientDao.getClientsCount()

    suspend fun insert(client: Client) = clientDao.insert(client)
    suspend fun update(client: Client) = clientDao.update(client)
    suspend fun delete(client: Client) = clientDao.delete(client)
}