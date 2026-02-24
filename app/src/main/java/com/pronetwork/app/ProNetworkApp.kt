package com.pronetwork.app

import android.app.Application
import android.util.Log
import com.pronetwork.app.network.ConnectivityObserver
import com.pronetwork.app.network.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Application class — initializes sync scheduling and connectivity monitoring.
 */
class ProNetworkApp : Application() {

    companion object {
        private const val TAG = "ProNetworkApp"
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    lateinit var connectivityObserver: ConnectivityObserver
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize connectivity observer
        connectivityObserver = ConnectivityObserver(this)

        // Schedule periodic background sync
        SyncWorker.schedulePeriodic(this)

        // Watch connectivity — trigger immediate sync when back online
        connectivityObserver.observe()
            .onEach { status ->
                Log.d(TAG, "Network status: $status")
                if (status == ConnectivityObserver.Status.AVAILABLE) {
                    SyncWorker.syncNow(this@ProNetworkApp)
                }
            }
            .launchIn(appScope)

        Log.i(TAG, "ProNetworkApp initialized — sync engine ready")
    }
}
