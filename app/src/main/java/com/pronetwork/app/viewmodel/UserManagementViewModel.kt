package com.pronetwork.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pronetwork.app.network.ApiClient
import com.pronetwork.app.network.AuthManager
import com.pronetwork.app.network.CreateUserRequest
import com.pronetwork.app.network.UpdateUserRequest
import com.pronetwork.app.network.UserResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserManagementUiState(
    val users: List<UserResponse> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showCreateDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val selectedUser: UserResponse? = null,
    // حقول إنشاء/تعديل مستخدم
    val formUsername: String = "",
    val formPassword: String = "",
    val formDisplayName: String = "",
    val formRole: String = "USER"
)

class UserManagementViewModel(application: Application) : AndroidViewModel(application) {

    private val authManager = AuthManager(application)
    private val _uiState = MutableStateFlow(UserManagementUiState())
    val uiState: StateFlow<UserManagementUiState> = _uiState.asStateFlow()

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = ApiClient.api.getUsers(authManager.getBearerToken())
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = _uiState.value.copy(
                        users = response.body()!!,
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

    fun createUser() {
        val state = _uiState.value
        if (state.formUsername.isBlank() || state.formPassword.isBlank() || state.formDisplayName.isBlank()) {
            _uiState.value = state.copy(errorMessage = "FIELDS_REQUIRED")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = ApiClient.api.createUser(
                    token = authManager.getBearerToken(),
                    request = CreateUserRequest(
                        username = state.formUsername.trim(),
                        password = state.formPassword,
                        display_name = state.formDisplayName.trim(),
                        role = state.formRole
                    )
                )
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showCreateDialog = false,
                        successMessage = "USER_CREATED",
                        formUsername = "",
                        formPassword = "",
                        formDisplayName = "",
                        formRole = "USER"
                    )
                    loadUsers()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = when (response.code()) {
                            409 -> "USERNAME_EXISTS"
                            else -> "CREATE_FAILED"
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

    fun updateUser() {
        val state = _uiState.value
        val user = state.selectedUser ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = ApiClient.api.updateUser(
                    token = authManager.getBearerToken(),
                    userId = user.id,
                    request = UpdateUserRequest(
                        display_name = state.formDisplayName.trim().ifBlank { null },
                        password = state.formPassword.ifBlank { null },
                        role = state.formRole
                    )
                )
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showEditDialog = false,
                        selectedUser = null,
                        successMessage = "USER_UPDATED",
                        formUsername = "",
                        formPassword = "",
                        formDisplayName = "",
                        formRole = "USER"
                    )
                    loadUsers()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "UPDATE_FAILED"
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

    fun toggleUser(userId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = ApiClient.api.toggleUser(
                    token = authManager.getBearerToken(),
                    userId = userId
                )
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "USER_TOGGLED"
                    )
                    loadUsers()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "TOGGLE_FAILED"
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

    fun deleteUser(userId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val response = ApiClient.api.deleteUser(
                    token = authManager.getBearerToken(),
                    userId = userId
                )
                if (response.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "USER_DELETED"
                    )
                    loadUsers()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = when (response.code()) {
                            400 -> "CANNOT_DELETE_SELF"
                            else -> "DELETE_FAILED"
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

    // === Dialog Controls ===
    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(
            showCreateDialog = true,
            formUsername = "",
            formPassword = "",
            formDisplayName = "",
            formRole = "USER",
            errorMessage = null
        )
    }

    fun showEditDialog(user: UserResponse) {
        _uiState.value = _uiState.value.copy(
            showEditDialog = true,
            selectedUser = user,
            formUsername = user.username,
            formDisplayName = user.display_name,
            formPassword = "",
            formRole = user.role,
            errorMessage = null
        )
    }

    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(
            showCreateDialog = false,
            showEditDialog = false,
            selectedUser = null,
            errorMessage = null
        )
    }

    // === Form Updates ===
    fun onFormUsernameChange(value: String) {
        _uiState.value = _uiState.value.copy(formUsername = value, errorMessage = null)
    }

    fun onFormPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(formPassword = value, errorMessage = null)
    }

    fun onFormDisplayNameChange(value: String) {
        _uiState.value = _uiState.value.copy(formDisplayName = value, errorMessage = null)
    }

    fun onFormRoleChange(value: String) {
        _uiState.value = _uiState.value.copy(formRole = value)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}
