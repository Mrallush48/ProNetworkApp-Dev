package com.pronetwork.app.network

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import okhttp3.ConnectionPool

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
// === DNS Cache to eliminate DNS lookup delays ===
class CachingDnsSelector : okhttp3.Dns {
    private val cache = java.util.concurrent.ConcurrentHashMap<String, CachedResult>()

    private data class CachedResult(
        val addresses: List<java.net.InetAddress>,
        val timestamp: Long
    )

    override fun lookup(hostname: String): List<java.net.InetAddress> {
        val now = System.currentTimeMillis()
        val cached = cache[hostname]

        // Use cache for 10 minutes
        if (cached != null && (now - cached.timestamp) < 600_000) {
            return cached.addresses
        }

        // Resolve and cache
        val addresses = okhttp3.Dns.SYSTEM.lookup(hostname)
        cache[hostname] = CachedResult(addresses, now)
        return addresses
    }
}

// === Retry Interceptor for automatic retry ===
class RetryInterceptor(private val maxRetries: Int = 2) : okhttp3.Interceptor {
    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        var lastException: Exception? = null
        for (attempt in 0..maxRetries) {
            try {
                return chain.proceed(chain.request())
            } catch (e: java.net.SocketTimeoutException) {
                lastException = e
                if (attempt < maxRetries) Thread.sleep(1000L * (attempt + 1))
            } catch (e: java.net.ConnectException) {
                lastException = e
                if (attempt < maxRetries) Thread.sleep(1000L * (attempt + 1))
            }
        }
        throw lastException ?: java.io.IOException("Request failed after $maxRetries retries")
    }
}

// === Retrofit Instance ===
object ApiClient {
    private const val BASE_URL = "https://pronetwork-spot.duckdns.org/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(RetryInterceptor(maxRetries = 2))
        .addInterceptor(loggingInterceptor)
        .dns(CachingDnsSelector())
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .connectionPool(ConnectionPool(5, 10, TimeUnit.MINUTES))
        .protocols(listOf(okhttp3.Protocol.HTTP_2, okhttp3.Protocol.HTTP_1_1))
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