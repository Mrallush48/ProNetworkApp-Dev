package com.pronetwork.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pronetwork.app.data.Client
import com.pronetwork.app.repository.ClientRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ClientViewModel(private val repository: ClientRepository) : ViewModel() {

    private val _buildingId = MutableStateFlow<Int?>(null)
    private val _month = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")

    // المصدر الأساسي للـ clients بحسب building + month
    private val clientsSource: Flow<List<Client>> = combine(_buildingId, _month) { b, m -> b to m }
        .flatMapLatest { (b, m) ->
            if (b != null && m != null) repository.getClientsByBuildingAndMonth(b, m)
            else flowOf(emptyList())
        }

    // تطبيق فلترة البحث على المصدر
    val clients: StateFlow<List<Client>> = combine(clientsSource, _searchQuery) { list, q ->
        if (q.isBlank()) list
        else list.filter { client ->
            client.name.contains(q, ignoreCase = true) ||
                    client.subscriptionNumber.contains(q, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // إعداد الـ building + month
    fun setBuildingAndMonth(buildingId: Int, monthIso: String) {
        _buildingId.value = buildingId
        _month.value = monthIso
    }

    fun searchClients(query: String) {
        _searchQuery.value = query
    }

    fun insertClient(client: Client) {
        viewModelScope.launch { repository.insertClient(client) }
    }

    fun updateClient(client: Client) {
        viewModelScope.launch { repository.updateClient(client) }
    }

    fun deleteClient(client: Client) {
        viewModelScope.launch { repository.deleteClient(client) }
    }

    fun softDeleteClient(client: Client, monthIso: String) {
        viewModelScope.launch { repository.softDeleteClient(client, monthIso) }
    }

    fun togglePaid(client: Client) {
        val updated = client.copy(
            isPaid = !client.isPaid,
            paymentDate = if (!client.isPaid) System.currentTimeMillis() else null
        )
        updateClient(updated)
    }
}
