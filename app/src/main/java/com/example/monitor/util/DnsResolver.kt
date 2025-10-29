package com.example.monitor.util

import android.util.Log
import java.net.InetAddress

class DnsResolver {

    fun resolve(hostname: String): Pair<Boolean, String?> {
        Log.i("DnsResolver", "Resolving hostname: $hostname")
        return try {
            InetAddress.getByName(hostname)
            Log.i("DnsResolver", "Successfully resolved hostname: $hostname")
            Pair(true, null)
        } catch (e: Exception) {
            Log.e("DnsResolver", "Error resolving hostname: $hostname", e)
            Pair(false, e.message)
        }
    }
}
