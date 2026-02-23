package com.pronetwork.app.network

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

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
}
