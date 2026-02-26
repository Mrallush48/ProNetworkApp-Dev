package com.pronetwork.app.network

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SSE (Server-Sent Events) client for real-time sync notifications.
 *
 * Architecture (3-tier professional sync):
 *   Tier 1: SSE stream — instant push from server when other users make changes
 *   Tier 2: Smart polling fallback — if SSE fails, poll every 30s
 *   Tier 3: WorkManager — background sync every 15 min (handled by SyncWorker)
 *
 * SSE connects directly to the server (bypasses Cloudflare CDN buffering).
 * On receiving a sync_update event, triggers SyncEngine.sync() to pull changes.
 */
@Singleton
class SyncStreamClient @Inject constructor() {

    companion object {
        private const val TAG = "SyncSSE"

        // SSE connects directly (not through Cloudflare CDN)
        private const val PRIMARY_SSE_URL = "https://pronetwork-spot.duckdns.org/sync/stream"
        private const val FALLBACK_SSE_URL = "https://pronetwork.dpdns.org/sync/stream"

        private const val INITIAL_RETRY_MS = 2_000L
        private const val MAX_RETRY_MS = 60_000L
        private const val BACKOFF_MULTIPLIER = 2.0
        private const val MAX_RECONNECT_ATTEMPTS = 50
        private const val CONNECTION_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 120_000  // 2 min (> server heartbeat 25s)
    }

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        RECONNECTING
    }

    // Emitted when server sends sync_update — listeners should trigger a pull
    private val _syncEvents = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 10
    )
    val syncEvents: SharedFlow<Unit> = _syncEvents.asSharedFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val isRunning = AtomicBoolean(false)
    private var connectionJob: Job? = null
    private var currentRetryMs = INITIAL_RETRY_MS
    private var reconnectAttempts = 0

    /**
     * Start SSE connection. Call when app enters foreground.
     */
    fun start(tokenProvider: suspend () -> String?, scope: CoroutineScope) {
        if (isRunning.getAndSet(true)) {
            Log.d(TAG, "Already running, ignoring start()")
            return
        }

        connectionJob = scope.launch(Dispatchers.IO) {
            Log.i(TAG, "SSE client starting")
            _connectionState.value = ConnectionState.CONNECTING

            while (isActive && isRunning.get()) {
                // Get fresh token for each connection attempt
                val token = tokenProvider()
                if (token == null) {
                    Log.w(TAG, "No valid token — SSE waiting...")
                    delay(INITIAL_RETRY_MS)
                    continue
                }
                val wasConnected = tryConnect(token)

                if (!isActive || !isRunning.get()) break

                if (!wasConnected) {
                    // Connection failed — exponential backoff
                    reconnectAttempts++
                    if (reconnectAttempts > MAX_RECONNECT_ATTEMPTS) {
                        currentRetryMs = INITIAL_RETRY_MS
                        reconnectAttempts = 0
                    }

                    _connectionState.value = ConnectionState.RECONNECTING
                    Log.w(TAG, "Reconnecting in ${currentRetryMs}ms (attempt $reconnectAttempts)")
                    delay(currentRetryMs)
                    currentRetryMs = (currentRetryMs * BACKOFF_MULTIPLIER)
                        .toLong()
                        .coerceAtMost(MAX_RETRY_MS)
                } else {
                    // Was connected then lost — quick reconnect
                    currentRetryMs = INITIAL_RETRY_MS
                    reconnectAttempts = 0
                    _connectionState.value = ConnectionState.RECONNECTING
                    delay(INITIAL_RETRY_MS)
                }
            }

            _connectionState.value = ConnectionState.DISCONNECTED
            Log.i(TAG, "SSE client stopped")
        }
    }

    /**
     * Stop SSE connection. Call when app enters background.
     */
    fun stop() {
        if (!isRunning.getAndSet(false)) return
        connectionJob?.cancel()
        connectionJob = null
        currentRetryMs = INITIAL_RETRY_MS
        reconnectAttempts = 0
        _connectionState.value = ConnectionState.DISCONNECTED
        Log.i(TAG, "SSE client stop requested")
    }

    val isConnected: Boolean
        get() = _connectionState.value == ConnectionState.CONNECTED

    /**
     * Try primary SSE URL, then fallback.
     * Returns true if was successfully connected (then lost), false if never connected.
     */
    private suspend fun tryConnect(token: String): Boolean {
        val urls = listOf(PRIMARY_SSE_URL, FALLBACK_SSE_URL)
        for (url in urls) {
            try {
                val result = connectToUrl(url, token)
                if (result) return true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Failed to connect to $url: ${e.message}")
            }
        }
        return false
    }

    private suspend fun connectToUrl(urlStr: String, token: String): Boolean {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlStr)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Accept", "text/event-stream")
                setRequestProperty("Cache-Control", "no-cache")
                connectTimeout = CONNECTION_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doInput = true
            }

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                Log.w(TAG, "SSE response: $responseCode from $urlStr")
                return false
            }

            Log.i(TAG, "SSE CONNECTED to $urlStr")
            _connectionState.value = ConnectionState.CONNECTED
            currentRetryMs = INITIAL_RETRY_MS
            reconnectAttempts = 0

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            var eventType = ""
            val dataBuilder = StringBuilder()

            while (isRunning.get()) {
                val line = reader.readLine() ?: break

                when {
                    line.startsWith("event: ") -> {
                        eventType = line.removePrefix("event: ").trim()
                    }
                    line.startsWith("data: ") -> {
                        dataBuilder.append(line.removePrefix("data: "))
                    }
                    line.isEmpty() && dataBuilder.isNotEmpty() -> {
                        processEvent(eventType, dataBuilder.toString())
                        eventType = ""
                        dataBuilder.clear()
                    }
                    line.startsWith(":") -> {
                        // Heartbeat comment — connection alive
                    }
                }
            }

            reader.close()
            return true

        } catch (e: CancellationException) {
            throw e
        } catch (e: java.net.SocketTimeoutException) {
            Log.w(TAG, "SSE timeout from $urlStr — will reconnect")
            return false
        } catch (e: Exception) {
            Log.w(TAG, "SSE error from $urlStr: ${e.message}")
            return false
        } finally {
            try { connection?.disconnect() } catch (_: Exception) {}
        }
    }

    private suspend fun processEvent(eventType: String, data: String) {
        when (eventType) {
            "connected" -> {
                Log.i(TAG, "SSE handshake confirmed: $data")
            }
            "sync_update" -> {
                Log.i(TAG, ">>> SYNC UPDATE received — triggering immediate pull")
                _syncEvents.emit(Unit)
            }
            else -> {
                Log.d(TAG, "Unknown SSE event: $eventType | $data")
            }
        }
    }
}
