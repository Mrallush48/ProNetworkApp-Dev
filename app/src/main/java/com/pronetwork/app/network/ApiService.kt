package com.pronetwork.app.network

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

// === Data Models (تطابق السيرفر) ===
data class LoginRequest(val username: String, val password: String)

data class TokenResponse(
    val access_token: String,
    val refresh_token: String,
    val token_type: String,
    val user: UserResponse
)

data class RefreshRequest(val refresh_token: String)

data class UserResponse(
    val id: Int,
    val username: String,
    val display_name: String,
    val role: String,
    val is_active: Boolean,
    val created_at: String,
    val last_login: String?
)

data class ApprovalRequestCreate(
    val request_type: String,
    val target_id: Int? = null,
    val target_name: String? = null,
    val reason: String? = null
)

data class ApprovalRequestResponse(
    val id: Int,
    val requester_id: Int,
    val request_type: String,
    val target_id: Int?,
    val target_name: String?,
    val reason: String?,
    val status: String,
    val reviewed_by: Int?,
    val created_at: String,
    val reviewed_at: String?
)

data class CreateUserRequest(
    val username: String,
    val password: String,
    val display_name: String,
    val role: String = "USER"
)

data class UpdateUserRequest(
    val display_name: String? = null,
    val password: String? = null,
    val role: String? = null
)

// === API Interface ===
interface ApiService {

    // === Admin - User Management ===
    @POST("admin/users")
    suspend fun createUser(
        @Header("Authorization") token: String,
        @Body request: CreateUserRequest
    ): Response<UserResponse>

    @GET("admin/users")
    suspend fun getUsers(
        @Header("Authorization") token: String
    ): Response<List<UserResponse>>

    @PUT("admin/users/{id}")
    suspend fun updateUser(
        @Header("Authorization") token: String,
        @Path("id") userId: Int,
        @Body request: UpdateUserRequest
    ): Response<UserResponse>

    @DELETE("admin/users/{id}")
    suspend fun deleteUser(
        @Header("Authorization") token: String,
        @Path("id") userId: Int
    ): Response<Unit>

    @PUT("admin/users/{id}/toggle")
    suspend fun toggleUser(
        @Header("Authorization") token: String,
        @Path("id") userId: Int
    ): Response<UserResponse>

    // Auth
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<TokenResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshRequest): Response<TokenResponse>

    @GET("auth/me")
    suspend fun getMe(@Header("Authorization") token: String): Response<UserResponse>

    // Approval Requests
    @POST("requests")
    suspend fun createRequest(
        @Header("Authorization") token: String,
        @Body request: ApprovalRequestCreate
    ): Response<ApprovalRequestResponse>

    @GET("requests")
    suspend fun getRequests(
        @Header("Authorization") token: String,
        @Query("status_filter") statusFilter: String? = null
    ): Response<List<ApprovalRequestResponse>>

    @GET("requests/my")
    suspend fun getMyRequests(
        @Header("Authorization") token: String
    ): Response<List<ApprovalRequestResponse>>

    @PUT("requests/{id}/approve")
    suspend fun approveRequest(
        @Header("Authorization") token: String,
        @Path("id") requestId: Int
    ): Response<ApprovalRequestResponse>

    @PUT("requests/{id}/reject")
    suspend fun rejectRequest(
        @Header("Authorization") token: String,
        @Path("id") requestId: Int
    ): Response<ApprovalRequestResponse>
}

// === Retrofit Instance ===
object ApiClient {
    // غيّر هذا للـ IP/Domain الخاص بسيرفرك
    private const val BASE_URL = "https://pronetwork-spot.duckdns.org/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
