package com.pronetwork.app

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.pronetwork.app.network.AuthManager
import com.pronetwork.app.network.ConnectivityObserver
import com.pronetwork.app.network.SyncEngine
import com.pronetwork.app.network.SyncStreamClient
import com.pronetwork.app.network.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Application class — Professional 3-tier sync architecture:
 *
 *   Tier 1: SSE (Server-Sent Events)
 *     → Real-time push from server when OTHER users make changes
 *     → Triggers immediate pull on receiving sync_update event
 *     → Active only when app is in foreground
 *
 *   Tier 2: Smart Polling Fallback
 *     → If SSE is disconnected, polls every 30s while in foreground
 *     → Automatically stops when SSE reconnects
 *
 *   Tier 3: WorkManager Background Sync
 *     → Periodic sync every 15 min when app is in background
 *     → Handles offline queue flush when connectivity returns
 *
 *   + Connectivity-aware: triggers immediate sync when network returns
 *
 * @HiltAndroidApp triggers Hilt's code generation for DI.
 * Configuration.Provider allows Hilt to inject workers.
 */
@HiltAndroidApp
class ProNetworkApp : Application(), Configuration.Provider {

    companion object {
        private const val TAG = "ProNetworkApp"
        private const val POLLING_INTERVAL_MS = 30_000L
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var connectivityObserver: ConnectivityObserver
        private set

    private val sseClient = SyncStreamClient()
    private var pollingJob: Job? = null

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Initialize connectivity observer
        connectivityObserver = ConnectivityObserver(this)

        // Tier 3: Schedule periodic background sync (every 15 min)
        SyncWorker.schedulePeriodic(this)

        // Connectivity-aware: trigger immediate sync when back online
        connectivityObserver.observe()
            .onEach { status ->
                Log.d(TAG, "Network status: $status")
                if (status == ConnectivityObserver.Status.AVAILABLE) {
                    SyncWorker.syncNow(this@ProNetworkApp)
                }
            }
            .launchIn(appScope)

        // Tier 1: Listen for SSE sync_update events → trigger immediate pull
        sseClient.syncEvents
            .onEach {
                Log.i(TAG, "SSE sync_update → triggering immediate sync")
                performImmediateSync()
            }
            .launchIn(appScope)

        // Tier 2: Monitor SSE state → start/stop polling fallback
        sseClient.connectionState
            .onEach { state ->
                Log.d(TAG, "SSE state: $state")
                when (state) {
                    SyncStreamClient.ConnectionState.CONNECTED -> {
                        stopPollingFallback()
                    }
                    SyncStreamClient.ConnectionState.RECONNECTING,
                    SyncStreamClient.ConnectionState.DISCONNECTED -> {
                        startPollingFallback()
                    }
                    SyncStreamClient.ConnectionState.CONNECTING -> { }
                }
            }
            .launchIn(appScope)

        // Lifecycle-aware: SSE on in foreground, off in background
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                Log.i(TAG, "App FOREGROUND — starting real-time sync")
                startRealtimeSync()
            }

            override fun onStop(owner: LifecycleOwner) {
                Log.i(TAG, "App BACKGROUND — stopping real-time sync")
                stopRealtimeSync()
            }
        })

        Log.i(TAG, "ProNetworkApp initialized — 3-tier sync architecture ready")
    }

    private fun startRealtimeSync() {
        val authManager = AuthManager(this)
        val token = authManager.getAccessToken() ?: return

        sseClient.start(token, appScope)

        appScope.launch(Dispatchers.IO) {
            performImmediateSync()
        }
    }

    private fun stopRealtimeSync() {
        sseClient.stop()
        stopPollingFallback()
    }

    private fun startPollingFallback() {
        if (pollingJob?.isActive == true) return

        pollingJob = appScope.launch(Dispatchers.IO) {
            Log.i(TAG, "Polling fallback STARTED (every ${POLLING_INTERVAL_MS / 1000}s)")
            while (isActive) {
                delay(POLLING_INTERVAL_MS)
                if (sseClient.isConnected) {
                    Log.i(TAG, "SSE reconnected — stopping polling fallback")
                    break
                }
                performImmediateSync()
            }
        }
    }

    private fun stopPollingFallback() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun performImmediateSync() {
        try {
            val authManager = AuthManager(this@ProNetworkApp)
            val token = authManager.getAccessToken() ?: return

            val syncEngine = SyncEngine(this@ProNetworkApp)
            val success = syncEngine.sync(token)

            if (success) {
                Log.d(TAG, "Immediate sync completed successfully")
            } else {
                Log.w(TAG, "Immediate sync completed with errors")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Immediate sync error: ${e.message}")
        }
    }
}
