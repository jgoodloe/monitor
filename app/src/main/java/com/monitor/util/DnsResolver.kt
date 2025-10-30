package com.monitor.util

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetAddress

class DnsResolver {

    data class PingResult(
        val ipAddress: String,
        val success: Boolean,
        val latencyMs: Long?,
        val rawOutput: String
    )

    data class DnsResolution(
        val isUp: Boolean,
        val errorMessage: String?,
        val ipAddresses: List<String>,
        val pingResults: List<PingResult>,
        val logs: List<String>
    )

    /**
     * Backward-compatible simple resolve used by existing code
     */
    fun resolve(hostname: String): Pair<Boolean, String?> {
        val result = resolveWithPing(hostname)
        return Pair(result.isUp, result.errorMessage)
    }

    /**
     * Resolve a hostname to all IP addresses and attempt to ping each one.
     * Also captures step-by-step logs for display.
     */
    fun resolveWithPing(hostname: String): DnsResolution {
        val logs = mutableListOf<String>()
        logs.add("Resolving hostname: $hostname")
        return try {
            val addresses = InetAddress.getAllByName(hostname)
            val ips = addresses.mapNotNull { it.hostAddress }
            logs.add("Resolved ${ips.size} IP(s): ${ips.joinToString()}")

            val pings = ips.map { ip -> pingIp(ip, logs) }
            val anyReachable = pings.any { it.success }
            val error = if (ips.isEmpty()) "No IP addresses returned" else null

            DnsResolution(
                isUp = anyReachable || ips.isNotEmpty(),
                errorMessage = error,
                ipAddresses = ips,
                pingResults = pings,
                logs = logs
            )
        } catch (e: Exception) {
            logs.add("DNS resolution failed: ${e.message}")
            Log.e("DnsResolver", "Error resolving hostname: $hostname", e)
            DnsResolution(
                isUp = false,
                errorMessage = e.message,
                ipAddresses = emptyList(),
                pingResults = emptyList(),
                logs = logs
            )
        }
    }

    /**
     * Execute system ping for a single IP (1 echo, 2s timeout) and capture output/latency.
     */
    private fun pingIp(ip: String, logs: MutableList<String>): PingResult {
        val cmd = arrayOf("/system/bin/ping", "-c", "1", "-W", "2", ip)
        logs.add("Pinging $ip ...")
        return try {
            val start = System.nanoTime()
            val process = Runtime.getRuntime().exec(cmd)
            val exitCode = process.waitFor()
            val end = System.nanoTime()
            val elapsedMs = (end - start) / 1_000_000

            val output = StringBuilder()
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    output.append(line).append('\n')
                    line = reader.readLine()
                }
            }
            BufferedReader(InputStreamReader(process.errorStream)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    output.append(line).append('\n')
                    line = reader.readLine()
                }
            }

            val success = (exitCode == 0)
            logs.add("Ping ${if (success) "succeeded" else "failed"} for $ip (exit=$exitCode, ${elapsedMs}ms)")
            PingResult(ip, success, elapsedMs, output.toString())
        } catch (e: Exception) {
            logs.add("Ping error for $ip: ${e.message}")
            PingResult(ip, false, null, e.message ?: "Ping failed")
        }
    }
}
