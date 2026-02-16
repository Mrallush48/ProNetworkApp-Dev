package com.pronetwork.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.pronetwork.app.data.Client
import com.pronetwork.app.data.ClientDatabase
import com.pronetwork.app.repository.ClientRepository
import kotlinx.coroutines.launch

class ClientViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ClientRepository
    private val _searchQuery = MutableLiveData("")
    val clients: LiveData<List<Client>>
    val clientsCount: LiveData<Int>
    val paidClientsCount: LiveData<Int>
    val unpaidClientsCount: LiveData<Int>

    init {
        val clientDao = ClientDatabase.getDatabase(application).clientDao()
        repository = ClientRepository(clientDao)
        clients = _searchQuery.switchMap { query ->
            if (query.isEmpty()) repository.clients else repository.searchClients(query)
        }
        clientsCount = repository.getClientsCount()
        paidClientsCount = repository.clients.map { list -> list.count { it.isPaid } }
        unpaidClientsCount = repository.clients.map { list -> list.count { !it.isPaid } }
    }

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun insert(client: Client) = viewModelScope.launch { repository.insert(client) }
    // دالة جديدة ترجع الـ ID مباشرة (تستخدم في الاستيراد)
    suspend fun insertAndGetId(client: Client): Long {
        return repository.insert(client)
    }
    fun update(client: Client) = viewModelScope.launch { repository.update(client) }
    fun delete(client: Client) = viewModelScope.launch { repository.delete(client) }
}
