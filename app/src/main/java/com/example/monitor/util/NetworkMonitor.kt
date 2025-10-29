package com.example.monitor.util

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL

class NetworkMonitor {

    fun checkUrl(urlString: String): Pair<Boolean, String?> {
        Log.i("NetworkMonitor", "Checking URL: $urlString")
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                Log.i("NetworkMonitor", "URL: $urlString, Response Code: $responseCode, Result: true")
                Pair(true, null)
            } else {
                val errorMessage = "HTTP Error: $responseCode"
                Log.w("NetworkMonitor", "URL: $urlString, Result: false, Error: $errorMessage")
                Pair(false, errorMessage)
            }
        } catch (e: Exception) {
            Log.e("NetworkMonitor", "Error checking URL: $urlString", e)
            Pair(false, e.message)
        }
    }
}
