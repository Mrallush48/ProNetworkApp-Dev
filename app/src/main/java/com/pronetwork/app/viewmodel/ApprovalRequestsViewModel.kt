package com.pronetwork.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pronetwork.app.network.ApiClient
import com.pronetwork.app.network.ApprovalRequestResponse
import com.pronetwork.app.network.AuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class ApprovalRequestsUiState(
    val requests: List<ApprovalRequestResponse> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val selectedFilter: String = "ALL",
    val isAdmin: Boolean = false,
    val showRejectDialog: Boolean = false,
    val selectedRequestId: Int? = null
)

@HiltViewModel
class ApprovalRequestsViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(ApprovalRequestsUiState())
    val uiState: StateFlow<ApprovalRequestsUiState> = _uiState.asStateFlow()

    init {
        checkRole()
        loadRequests()
    }

    private fun checkRole() {
        val role = authManager.getRole()
        _uiState.value = _uiState.value.copy(isAdmin = role == "ADMIN")
    }

    fun loadRequests() {
        // Re-check role every time (in case user switched accounts)
        checkRole()

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val filter = _uiState.value.selectedFilter
                val statusFilter = if (filter == "ALL") null else filter

                val response = if (_uiState.value.isAdmin) {
                    ApiClient.safeCall { api ->
                        api.getRequests(
                            token = authManager.getBearerToken(),
                            statusFilter = statusFilter
                        )
                    }
                } else {
                    ApiClient.safeCall { api ->
                        api.getMyRequests(
                            token = authManager.getBearerToken()
                        )
                    }
                }

                if (response.isSuccessful && response.body() != null) {
                    var requests = response.body()!!
                    // Apply local filter for non-admin (getMyRequests doesn't support server filter)
                    if (!_uiState.value.isAdmin && statusFilter != null) {
                        requests = requests.filter { it.status == statusFilter }
                    }
                    _uiState.value = _uiState.value.copy(
                        requests = requests.sortedByDescending { it.created_at },
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = when (response.code()) {
                            401 -> "SESSION_EXPIRED"
                            403 -> "NOT_ADMIN"
                            else -> "LOAD_FAILED"
                        }
                    )
                }
            } catch (e: java.net.ConnectException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "NO_CONNECTION"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "NETWORK_ERROR"
                )
            }
        }
    }

    fun approveRequest(requestId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = ApiClient.safeCall { api ->
                    api.approveRequest(
                        token = authManager.getBearerToken(),
                        requestId = requestId
                    )
                }
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "REQUEST_APPROVED"
                    )
                    loadRequests()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = when (response.code()) {
                            404 -> "REQUEST_NOT_FOUND"
                            400 -> "REQUEST_ALREADY_REVIEWED"
                            else -> "APPROVE_FAILED"
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "NETWORK_ERROR"
                )
            }
        }
    }

    fun rejectRequest(requestId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = ApiClient.safeCall { api ->
                    api.rejectRequest(
                        token = authManager.getBearerToken(),
                        requestId = requestId
                    )
                }
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "REQUEST_REJECTED",
                        showRejectDialog = false,
                        selectedRequestId = null
                    )
                    loadRequests()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = when (response.code()) {
                            404 -> "REQUEST_NOT_FOUND"
                            400 -> "REQUEST_ALREADY_REVIEWED"
                            else -> "REJECT_FAILED"
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "NETWORK_ERROR"
                )
            }
        }
    }

    fun setFilter(filter: String) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
        loadRequests()
    }

    fun showRejectDialog(requestId: Int) {
        _uiState.value = _uiState.value.copy(
            showRejectDialog = true,
            selectedRequestId = requestId
        )
    }

    fun dismissRejectDialog() {
        _uiState.value = _uiState.value.copy(
            showRejectDialog = false,
            selectedRequestId = null
        )
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
    fun cancelRequest(requestId: Int) {
        viewModelScope.launch {
            try {
                val response = ApiClient.safeCall { api ->
                    api.cancelRequest(token = authManager.getBearerToken(), requestId = requestId)
                }
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        successMessage = "Request cancelled"
                    )
                    loadRequests()
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Cannot cancel: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Error: ${e.message}"
                )
            }
        }
    }
}
