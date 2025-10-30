package com.monitor.util

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.IDN
import java.net.InetAddress
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class DnsResolver {

    companion object {
        private const val DNS_TIMEOUT_MS = 5000L
    }

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

    /** Normalize a hostname:
     * - If a URL is passed, extract host
     * - Trim whitespace
     * - Remove trailing dot
     * - Convert to ASCII using IDN (punycode) for IDNs
     */
    private fun normalizeHostname(input: String): String {
        var h = input.trim()
        // If looks like a URL, parse and extract host
        try {
            if (h.contains("://")) {
                val uri = URI(h)
                uri.host?.let { h = it }
            }
        } catch (_: Exception) {}
        if (h.endsWith('.')) h = h.dropLast(1)
        return try { IDN.toASCII(h) } catch (_: Exception) { h }
    }

    /**
     * Resolve a hostname to all IP addresses and attempt to ping each one.
     * Also captures step-by-step logs for display.
     */
    fun resolveWithPing(hostname: String): DnsResolution {
        val logs = mutableListOf<String>()
        val normalized = normalizeHostname(hostname)
        logs.add("Resolving hostname: '$hostname' -> '$normalized'")
        return try {
            // Use CountDownLatch to timeout DNS resolution
            val latch = CountDownLatch(1)
            var addresses: Array<InetAddress>? = null
            var dnsException: Exception? = null
            
            val resolveThread = Thread {
                try {
                    addresses = InetAddress.getAllByName(normalized)
                } catch (e: Exception) {
                    dnsException = e
                } finally {
                    latch.countDown()
                }
            }
            resolveThread.start()
            
            val timedOut = !latch.await(DNS_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            if (timedOut) {
                logs.add("DNS resolution timeout after ${DNS_TIMEOUT_MS}ms for '$normalized'")
                return DnsResolution(
                    isUp = false,
                    errorMessage = "DNS resolution timeout",
                    ipAddresses = emptyList(),
                    pingResults = emptyList(),
                    logs = logs
                )
            }
            
            if (dnsException != null) {
                logs.add("DNS resolution failed for '$normalized': ${dnsException!!.message}")
                Log.e("DnsResolver", "Error resolving hostname: $normalized (original: $hostname)", dnsException)
                return DnsResolution(
                    isUp = false,
                    errorMessage = dnsException!!.message,
                    ipAddresses = emptyList(),
                    pingResults = emptyList(),
                    logs = logs
                )
            }
            
            val ips = addresses!!.mapNotNull { it.hostAddress }
            logs.add("Resolved ${ips.size} IP(s): ${ips.joinToString()}")

            val pings = ips.map { ip -> pingIp(ip, logs) }
            val anyReachable = pings.any { it.success }
            val error = if (ips.isEmpty()) "No IP addresses returned for $normalized" else null

            DnsResolution(
                isUp = anyReachable || ips.isNotEmpty(),
                errorMessage = error,
                ipAddresses = ips,
                pingResults = pings,
                logs = logs
            )
        } catch (e: Exception) {
            logs.add("DNS resolution failed for '$normalized': ${e.message}")
            Log.e("DnsResolver", "Error resolving hostname: $normalized (original: $hostname)", e)
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
        // Prefer ICMP echo via system ping without specifying any port
        val candidates = listOf(
            arrayOf("/system/bin/ping", "-c", "1", "-w", "3", ip), // -w overall timeout
            arrayOf("/system/bin/ping", "-c", "1", ip),
            arrayOf("ping", "-c", "1", "-w", "3", ip),
            arrayOf("ping", "-c", "1", ip)
        )
        for (cmd in candidates) {
            logs.add("Pinging $ip with: ${cmd.joinToString(" ")}")
            try {
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
                if (success) return PingResult(ip, true, elapsedMs, output.toString())
                // else fall through to try next method
            } catch (e: Exception) {
                logs.add("Ping command error for $ip: ${e.message}")
            }
        }

        // Fallback: InetAddress.isReachable (no explicit port), may use ICMP or TCP echo internally
        return try {
            logs.add("Fallback isReachable() for $ip (3s)...")
            val start = System.nanoTime()
            val reachable = InetAddress.getByName(ip).isReachable(3000)
            val end = System.nanoTime()
            val elapsedMs = (end - start) / 1_000_000
            logs.add("isReachable result for $ip: $reachable (${elapsedMs}ms)")
            PingResult(ip, reachable, elapsedMs, "isReachable=$reachable")
        } catch (e: Exception) {
            logs.add("isReachable error for $ip: ${e.message}")
            PingResult(ip, false, null, e.message ?: "Ping failed")
        }
    }
}
