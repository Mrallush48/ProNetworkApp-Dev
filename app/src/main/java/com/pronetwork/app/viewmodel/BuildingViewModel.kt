package com.pronetwork.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pronetwork.app.data.Building
import com.pronetwork.app.repository.BuildingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

class BuildingViewModel(private val repository: BuildingRepository) : ViewModel() {

    // exposed as StateFlow
    val buildings: StateFlow<List<Building>> =
        repository.getAllBuildings()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addBuilding(building: Building) {
        viewModelScope.launch { repository.addBuilding(building) }
    }

    fun updateBuilding(building: Building) {
        viewModelScope.launch { repository.updateBuilding(building) }
    }

    fun deleteBuilding(building: Building) {
        viewModelScope.launch { repository.deleteBuilding(building) }
    }
}
