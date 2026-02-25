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

// === Sync Models ===
data class SyncPushRequest(
    val operations: List<SyncOperation>
)

data class SyncOperation(
    val entity_type: String,
    val entity_id: Int,
    val action: String,
    val payload: String,
    val client_timestamp: String
)

data class SyncPushResponse(
    val processed: Int,
    val failed: Int,
    val errors: List<String>? = null,
    val acknowledgments: List<SyncPushAck>? = null
)

data class SyncPushAck(
    val entity_type: String,
    val local_id: Int,
    val server_id: Int,
    val action: String,
    val status: String = "ok"
)

data class SyncPullResponse(
    val clients: List<SyncEntity>? = null,
    val buildings: List<SyncEntity>? = null,
    val payments: List<SyncEntity>? = null,
    val payment_transactions: List<SyncEntity>? = null,
    val server_timestamp: String
)

data class SyncEntity(
    val id: Int,
    val action: String,
    val data: Map<String, Any?>? = null
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


    @DELETE("requests/{id}")
    suspend fun cancelRequest(
        @Header("Authorization") token: String,
        @Path("id") requestId: Int
    ): Response<Unit>

    // === Sync ===
    @POST("sync/push")
    suspend fun syncPush(
        @Header("Authorization") token: String,
        @Body request: SyncPushRequest
    ): Response<SyncPushResponse>

    @GET("sync/pull")
    suspend fun syncPull(
        @Header("Authorization") token: String,
        @Query("since") since: String? = null
    ): Response<SyncPullResponse>

    @GET("sync/status")
    suspend fun syncStatus(
        @Header("Authorization") token: String
    ): Response<Map<String, Any>>

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

// === Connection Logger - logs which server is being used ===
class ConnectionLoggerInterceptor : okhttp3.Interceptor {
    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        val request = chain.request()
        val host = request.url.host
        val path = request.url.encodedPath
        android.util.Log.i("ProNetwork-API", ">>> REQUEST: $host$path")

        val startTime = System.currentTimeMillis()
        val response = chain.proceed(request)
        val duration = System.currentTimeMillis() - startTime

        android.util.Log.i("ProNetwork-API", "<<< RESPONSE: $host | ${response.code} | ${duration}ms")
        return response
    }
}

// === Retrofit Instance with Failover ===
object ApiClient {
    private const val PRIMARY_URL = "https://pronetwork.dpdns.org/"
    private const val FALLBACK_URL = "https://pronetwork-spot.duckdns.org/"

    @Volatile
    private var currentBaseUrl: String = PRIMARY_URL

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(RetryInterceptor(maxRetries = 2))
        .addInterceptor(ConnectionLoggerInterceptor())
        .addInterceptor(loggingInterceptor)
        .dns(CachingDnsSelector())
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .connectionPool(ConnectionPool(5, 10, TimeUnit.MINUTES))
        .protocols(listOf(okhttp3.Protocol.HTTP_2, okhttp3.Protocol.HTTP_1_1))
        .build()

    private fun buildApi(baseUrl: String): ApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private var primaryApi: ApiService = buildApi(PRIMARY_URL)
    private var fallbackApi: ApiService = buildApi(FALLBACK_URL)

    val api: ApiService
        get() = if (currentBaseUrl == PRIMARY_URL) primaryApi else fallbackApi

    suspend fun <T> safeCall(block: suspend (ApiService) -> Response<T>): Response<T> {
        // Try PRIMARY first
        try {
            val response = block(primaryApi)
            if (response.isSuccessful || response.code() in 400..499) {
                // Real response (success or client error) — PRIMARY works
                if (currentBaseUrl != PRIMARY_URL) {
                    currentBaseUrl = PRIMARY_URL
                    android.util.Log.i("ProNetwork-API", "SWITCHED BACK to PRIMARY: $PRIMARY_URL")
                }
                return response
            }
            // Server error (5xx) — try fallback
            android.util.Log.w("ProNetwork-API", "PRIMARY returned ${response.code()}, trying FALLBACK")
        } catch (e: Exception) {
            android.util.Log.w("ProNetwork-API", "PRIMARY failed: ${e.message}, trying FALLBACK")
        }

        // Try FALLBACK
        try {
            val response = block(fallbackApi)
            currentBaseUrl = FALLBACK_URL
            android.util.Log.i("ProNetwork-API", "Using FALLBACK: $FALLBACK_URL")
            return response
        } catch (fallbackError: Exception) {
            android.util.Log.e("ProNetwork-API", "Both PRIMARY and FALLBACK failed")
            throw fallbackError
        }
    }
}
