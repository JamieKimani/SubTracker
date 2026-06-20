package com.example.trackifyv1

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.trackifyv1.data.repo.TrackifyRepository

class TrackifyApp : Application() {

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            // Connection just came back — drain anything queued while we were offline.
            TrackifyRepository.getInstance(applicationContext).requestSync()
        }
    }

    override fun onCreate() {
        super.onCreate()
        val cm = getSystemService(ConnectivityManager::class.java)
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm?.registerNetworkCallback(request, networkCallback)
    }
}
