package com.example.policemobiledirectory.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class to check internet connectivity and monitor network state
 * Provides both one-time checks and continuous monitoring via Flow
 */
@Singleton
class ConnectivityManager @Inject constructor(
    private val context: Context
) {
    private val connectivityManager: ConnectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    /**
     * Check if device has active internet connection
     * @return true if connected to internet, false otherwise
     */
    fun isConnected(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkConnectivityV23()
        } else {
            checkConnectivityLegacy()
        }
    }

    /**
     * Check if device has active internet connection (Android 6.0+)
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkConnectivityV23(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Check if device has active internet connection (Legacy Android)
     */
    @Suppress("DEPRECATION")
    private fun checkConnectivityLegacy(): Boolean {
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    /**
     * Check if device is connected to WiFi
     */
    fun isConnectedToWifi(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.type == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected
        }
    }

    /**
     * Check if device is connected to mobile data
     */
    fun isConnectedToMobile(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.type == ConnectivityManager.TYPE_MOBILE && networkInfo.isConnected
        }
    }

    /**
     * Observe network connectivity state changes
     * Emits true when connected, false when disconnected
     */
    fun observeConnectivity(): Flow<Boolean> = callbackFlow {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    val capabilities = connectivityManager.getNetworkCapabilities(network)
                    val isConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                                      capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    trySend(isConnected)
                }

                override fun onLost(network: Network) {
                    trySend(false)
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    val isConnected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                      networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    trySend(isConnected)
                }
            }

            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                .build()

            connectivityManager.registerNetworkCallback(request, callback)

            // Emit initial state
            trySend(isConnected())

            awaitClose {
                connectivityManager.unregisterNetworkCallback(callback)
            }
        } else {
            // For legacy Android, emit initial state
            trySend(isConnected())
            awaitClose { }
        }
    }.distinctUntilChanged()
}

