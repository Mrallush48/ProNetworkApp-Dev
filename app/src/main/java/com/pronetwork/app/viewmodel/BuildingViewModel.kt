package com.pronetwork.app.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.pronetwork.app.data.Building
import com.pronetwork.app.data.BuildingDatabase
import com.pronetwork.app.repository.BuildingRepository
import kotlinx.coroutines.launch

class BuildingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BuildingRepository
    private val _searchQuery = MutableLiveData("")
    val buildings: LiveData<List<Building>>

    init {
        val buildingDao = BuildingDatabase.getDatabase(application).buildingDao()
        repository = BuildingRepository(buildingDao)
        buildings = _searchQuery.switchMap { query ->
            if (query.isEmpty()) repository.buildings else repository.searchBuildings(query)
        }
    }

    fun setSearchQuery(q: String) { _searchQuery.value = q }

    fun insert(building: Building) = viewModelScope.launch { repository.insert(building) }
    fun update(building: Building) = viewModelScope.launch { repository.update(building) }
    fun delete(building: Building) = viewModelScope.launch { repository.delete(building) }
}