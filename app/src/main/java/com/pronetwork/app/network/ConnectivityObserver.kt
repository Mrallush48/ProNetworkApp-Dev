package com.pronetwork.app.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Observes network connectivity changes and emits status as a Flow.
 * Used by SyncEngine to trigger sync when connection is restored.
 */
class ConnectivityObserver(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    enum class Status {
        AVAILABLE,      // 🟢 Online
        LOSING,         // 🟡 Losing connection
        LOST,           // 🔴 Offline
        UNAVAILABLE     // 🔴 No network
    }

    /**
     * Returns current connectivity status (one-shot check).
     */
    fun isOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Emits connectivity status changes as a Flow.
     * Automatically triggers when network state changes.
     */
    fun observe(): Flow<Status> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(Status.AVAILABLE)
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                trySend(Status.LOSING)
            }

            override fun onLost(network: Network) {
                trySend(Status.LOST)
            }

            override fun onUnavailable() {
                trySend(Status.UNAVAILABLE)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Emit initial state
        trySend(if (isOnline()) Status.AVAILABLE else Status.UNAVAILABLE)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
}
