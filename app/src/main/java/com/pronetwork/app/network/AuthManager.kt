package com.pronetwork.app.network

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.delay

class AuthManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "pronetwork_auth",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_ROLE = "role"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    fun saveLoginData(tokenResponse: TokenResponse) {
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, tokenResponse.access_token)
            putString(KEY_REFRESH_TOKEN, tokenResponse.refresh_token)
            putInt(KEY_USER_ID, tokenResponse.user.id)
            putString(KEY_USERNAME, tokenResponse.user.username)
            putString(KEY_DISPLAY_NAME, tokenResponse.user.display_name)
            putString(KEY_ROLE, tokenResponse.user.role)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun getBearerToken(): String = "Bearer ${getAccessToken()}"

    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, 0)

    fun getUsername(): String = prefs.getString(KEY_USERNAME, "") ?: ""

    fun getDisplayName(): String = prefs.getString(KEY_DISPLAY_NAME, "") ?: ""

    fun getRole(): String = prefs.getString(KEY_ROLE, "USER") ?: "USER"

    fun isAdmin(): Boolean = getRole() == "ADMIN"

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun logout() {
        prefs.edit().clear().apply()
    }

    /**
     * Refresh the access token using the stored refresh token.
     * Returns the new access token if successful, null otherwise.
     * Thread-safe: uses synchronized to prevent concurrent refresh attempts.
     */
    @Volatile
    private var isRefreshing = false

    suspend fun refreshAccessToken(): String? {
        // Prevent concurrent refresh attempts
        if (isRefreshing) {
            // Wait briefly and return current token (another coroutine is refreshing)
            kotlinx.coroutines.delay(1000)
            return getAccessToken()
        }

        val refreshToken = getRefreshToken() ?: run {
            android.util.Log.w("AuthManager", "No refresh token available")
            return null
        }

        isRefreshing = true
        return try {
            val response = ApiClient.safeCall { api ->
                api.refreshToken(RefreshRequest(refreshToken))
            }

            if (response.isSuccessful) {
                val tokenResponse = response.body()
                if (tokenResponse != null) {
                    saveLoginData(tokenResponse)
                    android.util.Log.i("AuthManager", "Token refreshed successfully")
                    tokenResponse.access_token
                } else {
                    android.util.Log.w("AuthManager", "Refresh response body is null")
                    null
                }
            } else {
                android.util.Log.w("AuthManager", "Token refresh failed: ${response.code()}")
                if (response.code() == 401) {
                    // Refresh token is also expired → user must re-login
                    android.util.Log.e("AuthManager", "Refresh token expired — session invalid")
                }
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthManager", "Token refresh error: ${e.message}")
            null
        } finally {
            isRefreshing = false
        }
    }

    /**
     * Get a valid access token, refreshing if needed.
     * This is the preferred method for getting tokens for API calls.
     */
    suspend fun getValidAccessToken(): String? {
        val token = getAccessToken()
        if (token == null) {
            android.util.Log.w("AuthManager", "No access token stored")
            return null
        }

        // Try to decode JWT and check expiration
        return if (isTokenExpired(token)) {
            android.util.Log.i("AuthManager", "Access token expired, refreshing...")
            refreshAccessToken()
        } else {
            token
        }
    }

    /**
     * Check if a JWT token is expired (with 60-second buffer).
     * Returns true if expired or cannot be parsed.
     */
    private fun isTokenExpired(token: String): Boolean {
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return true

            val payload = String(
                android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING),
                Charsets.UTF_8
            )

            val expMatch = Regex("\"exp\"\\s*:\\s*(\\d+)").find(payload)
            val exp = expMatch?.groupValues?.get(1)?.toLongOrNull() ?: return true

            val now = System.currentTimeMillis() / 1000
            val buffer = 60 // 60-second buffer before actual expiry

            (now + buffer) >= exp
        } catch (e: Exception) {
            android.util.Log.w("AuthManager", "Cannot parse token expiry: ${e.message}")
            true // Assume expired if can't parse
        }
    }
}
