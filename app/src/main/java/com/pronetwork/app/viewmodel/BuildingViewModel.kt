package com.pronetwork.app.viewmodel

import androidx.lifecycle.*
import com.pronetwork.app.data.Building
import com.pronetwork.app.repository.BuildingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BuildingViewModel @Inject constructor(
    private val repository: BuildingRepository
) : ViewModel() {

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> get() = _searchQuery

    val buildings: LiveData<List<Building>>

    init {
        buildings = _searchQuery.switchMap { query ->
            if (query.isEmpty()) repository.buildings else repository.searchBuildings(query)
        }
    }

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
    }

    fun insert(building: Building) = viewModelScope.launch { repository.insert(building) }
    fun update(building: Building) = viewModelScope.launch { repository.update(building) }
    fun delete(building: Building) = viewModelScope.launch { repository.delete(building) }
}
