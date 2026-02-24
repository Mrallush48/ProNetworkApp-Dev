package com.pronetwork.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pronetwork.app.network.ApiClient
import com.pronetwork.app.network.AuthManager
import com.pronetwork.app.network.LoginRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val errorMessage: String? = null,
    val username: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val loginSuccess: Boolean = false,
    val displayName: String = "",
    val role: String = ""
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    val authManager = AuthManager(application)

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        // تحقق إذا المستخدم مسجّل دخول مسبقاً
        if (authManager.isLoggedIn()) {
            _uiState.value = _uiState.value.copy(
                isLoggedIn = true,
                loginSuccess = true,
                displayName = authManager.getDisplayName(),
                role = authManager.getRole()
            )
        }
    }

    fun onUsernameChange(value: String) {
        _uiState.value = _uiState.value.copy(
            username = value,
            errorMessage = null
        )
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(
            password = value,
            errorMessage = null
        )
    }

    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            isPasswordVisible = !_uiState.value.isPasswordVisible
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun login() {
        val state = _uiState.value

        if (state.username.isBlank()) {
            _uiState.value = state.copy(errorMessage = "USERNAME_REQUIRED")
            return
        }
        if (state.password.isBlank()) {
            _uiState.value = state.copy(errorMessage = "PASSWORD_REQUIRED")
            return
        }

        _uiState.value = state.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val response = ApiClient.safeCall { api ->
                    api.login(
                        LoginRequest(
                            username = state.username.trim(),
                            password = state.password
                        )
                    )
                }

                if (response.isSuccessful && response.body() != null) {
                    val tokenResponse = response.body()!!
                    authManager.saveLoginData(tokenResponse)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        loginSuccess = true,
                        displayName = tokenResponse.user.display_name,
                        role = tokenResponse.user.role,
                        errorMessage = null
                    )
                } else {
                    val errorMsg = when (response.code()) {
                        401 -> "INVALID_CREDENTIALS"
                        403 -> "ACCOUNT_DISABLED"
                        404 -> "USER_NOT_FOUND"
                        500 -> "SERVER_ERROR"
                        else -> "UNKNOWN_ERROR"
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = errorMsg
                    )
                }
            } catch (e: java.net.ConnectException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "NO_CONNECTION"
                )
            } catch (e: java.net.SocketTimeoutException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "TIMEOUT"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "NETWORK_ERROR"
                )
            }
        }
    }

    fun logout() {
        authManager.logout()
        _uiState.value = LoginUiState()
    }
}
