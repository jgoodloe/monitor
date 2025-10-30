package com.monitor.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.security.cert.X509CRL
import java.security.cert.CertificateFactory
import java.io.InputStream
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.net.ssl.*

/**
 * CRL (Certificate Revocation List) Verifier
 * 
 * Based on concepts from https://github.com/jgoodloe/OCSPTesting
 * Verifies CRL files can be downloaded and checks validity periods
 */
class CRLVerifier(private val context: Context? = null) {

    private val prefs: SharedPreferences? = context?.getSharedPreferences("MonitorPrefs", Context.MODE_PRIVATE)
    
    // Default warning threshold: 3 hours before nextUpdate
    private val defaultWarningThresholdHours = 3
    private val warningThresholdKey = "crl_warning_threshold_hours"

    /**
     * Get the configurable warning threshold in hours
     */
    fun getWarningThresholdHours(): Int {
        return prefs?.getInt(warningThresholdKey, defaultWarningThresholdHours) ?: defaultWarningThresholdHours
    }

    /**
     * Set the warning threshold in hours
     */
    fun setWarningThresholdHours(hours: Int) {
        prefs?.edit()?.putInt(warningThresholdKey, hours)?.apply()
        Log.i("CRLVerifier", "Warning threshold updated to $hours hours")
    }

    /**
     * Data class for CRL verification result
     */
    data class CRLVerificationResult(
        val canDownload: Boolean,
        val isValid: Boolean,
        val warningMessage: String?,
        val thisUpdate: Date? = null,
        val nextUpdate: Date? = null,
        val revokedCertificateCount: Int? = null
    )

    /**
     * Verify a CRL URL
     * @param crlUrl The URL to the CRL file
     * @return CRLVerificationResult with download status, validity, warnings, and validity period
     */
    fun verifyCRL(crlUrl: String): CRLVerificationResult {
        Log.i("CRLVerifier", "=== Starting CRL verification: $crlUrl ===")
        
        var canDownload = false
        var isValid = false
        var warningMessage: String? = null
        
        try {
            Log.d("CRLVerifier", "Creating URL connection for: $crlUrl")
            val url = URL(crlUrl)
            val connection = url.openConnection() as HttpURLConnection
            Log.d("CRLVerifier", "Connection created, protocol: ${url.protocol}")
            
            // Apply SSL context for HTTPS connections (reuse permissive SSL for certificate warnings)
            if (url.protocol == "https") {
                (connection as? HttpsURLConnection)?.let { httpsConnection ->
                    try {
                        val sslContext = createPermissiveSSLContext()
                        httpsConnection.sslSocketFactory = sslContext.socketFactory
                        httpsConnection.hostnameVerifier = PermissiveHostnameVerifier()
                    } catch (e: Exception) {
                        Log.w("CRLVerifier", "Failed to set SSL context for $crlUrl: ${e.message}")
                    }
                }
            }
            
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            Log.d("CRLVerifier", "Connecting to $crlUrl...")
            connection.connect()
            
            val responseCode = connection.responseCode
            Log.d("CRLVerifier", "Response code: $responseCode")
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                canDownload = true
                Log.i("CRLVerifier", "Successfully downloaded CRL from $crlUrl")
                
                try {
                    // Parse and validate CRL
                    val inputStream: InputStream = connection.inputStream
                    val crlSize = connection.contentLength
                    Log.d("CRLVerifier", "CRL size: ${if (crlSize > 0) "$crlSize bytes" else "unknown"}")
                    
                    val crlResult = parseAndValidateCRL(inputStream)
                    isValid = crlResult.isValid
                    warningMessage = crlResult.warningMessage
                    val thisUpdate = crlResult.thisUpdate
                    val nextUpdate = crlResult.nextUpdate
                    val revokedCertCount = crlResult.revokedCertificateCount
                    
                    Log.i("CRLVerifier", "CRL parsing result: isValid=$isValid, warningMessage=$warningMessage")
                    Log.i("CRLVerifier", "CRL validity: thisUpdate=$thisUpdate, nextUpdate=$nextUpdate")
                    inputStream.close()
                    
                    return CRLVerificationResult(
                        canDownload = true,
                        isValid = isValid,
                        warningMessage = warningMessage,
                        thisUpdate = thisUpdate,
                        nextUpdate = nextUpdate,
                        revokedCertificateCount = revokedCertCount
                    )
                } catch (e: Exception) {
                    Log.e("CRLVerifier", "Error parsing downloaded CRL", e)
                    isValid = false
                    warningMessage = "Parse error: ${e.message}"
                    return CRLVerificationResult(canDownload = true, isValid = false, warningMessage = warningMessage)
                }
            } else {
                val errorStream = try {
                    connection.errorStream?.readBytes()?.toString(Charsets.UTF_8)
                } catch (e: Exception) {
                    null
                }
                Log.w("CRLVerifier", "Failed to download CRL from $crlUrl, Response Code: $responseCode, Error: $errorStream")
                return CRLVerificationResult(false, false, "HTTP Error: $responseCode")
            }
        } catch (e: SSLException) {
            Log.w("CRLVerifier", "SSL error downloading CRL from $crlUrl: ${e.message}")
            // Retry with permissive SSL
            try {
                val url = URL(crlUrl)
                val connection = url.openConnection() as HttpURLConnection
                if (url.protocol == "https") {
                    (connection as? HttpsURLConnection)?.let { httpsConnection ->
                        val sslContext = createPermissiveSSLContext()
                        httpsConnection.sslSocketFactory = sslContext.socketFactory
                        httpsConnection.hostnameVerifier = PermissiveHostnameVerifier()
                    }
                }
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.connect()
                
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    canDownload = true
                    val inputStream: InputStream = connection.inputStream
                    val crlResult = parseAndValidateCRL(inputStream)
                    isValid = crlResult.isValid
                    warningMessage = "SSL warning - ${crlResult.warningMessage ?: e.message}"
                    inputStream.close()
                    return CRLVerificationResult(
                        canDownload = true,
                        isValid = isValid,
                        warningMessage = warningMessage,
                        thisUpdate = crlResult.thisUpdate,
                        nextUpdate = crlResult.nextUpdate,
                        revokedCertificateCount = crlResult.revokedCertificateCount
                    )
                } else {
                    return CRLVerificationResult(false, false, "SSL error and retry failed: ${e.message}")
                }
            } catch (retryException: Exception) {
                Log.e("CRLVerifier", "Failed to download CRL after SSL error retry", retryException)
                return CRLVerificationResult(false, false, "SSL error: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e("CRLVerifier", "Error verifying CRL: $crlUrl", e)
            Log.e("CRLVerifier", "Exception type: ${e.javaClass.name}, Message: ${e.message}")
            e.printStackTrace()
            return CRLVerificationResult(false, false, "Error: ${e.message ?: e.javaClass.simpleName}")
        }
        
        val resultSummary = "Download: $canDownload, Valid: $isValid, Warning: ${warningMessage != null}"
        Log.i("CRLVerifier", "=== CRL verification complete for $crlUrl ===")
        Log.i("CRLVerifier", "Result: $resultSummary")
        
        // This should not be reached due to early returns, but keep for safety
        return CRLVerificationResult(canDownload, isValid, warningMessage)
    }

    /**
     * Data class for CRL parsing result
     */
    private data class CRLParsingResult(
        val isValid: Boolean,
        val warningMessage: String?,
        val thisUpdate: Date?,
        val nextUpdate: Date?,
        val revokedCertificateCount: Int? = null
    )

    /**
     * Parse CRL and validate time periods
     */
    private fun parseAndValidateCRL(inputStream: InputStream): CRLParsingResult {
        return try {
            Log.d("CRLVerifier", "Creating CertificateFactory for X.509")
            val certificateFactory = CertificateFactory.getInstance("X.509")
            Log.d("CRLVerifier", "Generating CRL from input stream...")
            val crl = certificateFactory.generateCRL(inputStream) as X509CRL
            Log.i("CRLVerifier", "Successfully parsed CRL")
            
            val thisUpdate = crl.thisUpdate
            val nextUpdate = crl.nextUpdate
            val currentTime = Date()
            
            Log.d("CRLVerifier", "CRL thisUpdate: $thisUpdate")
            Log.d("CRLVerifier", "CRL nextUpdate: $nextUpdate")
            Log.d("CRLVerifier", "Current time: $currentTime")
            
            // Check if CRL is currently valid (current time is between thisUpdate and nextUpdate)
            val isCurrentlyValid = currentTime.after(thisUpdate) && currentTime.before(nextUpdate)
            
            // Get revoked certificate count early
            val revokedCertCount = crl.revokedCertificates?.size ?: 0
            Log.i("CRLVerifier", "CRL contains $revokedCertCount revoked certificates")
            
            if (!isCurrentlyValid) {
                if (currentTime.before(thisUpdate)) {
                    val warning = "CRL not yet valid. thisUpdate: $thisUpdate, Current: $currentTime"
                    Log.w("CRLVerifier", warning)
                    return CRLParsingResult(false, warning, thisUpdate, nextUpdate, revokedCertCount)
                } else {
                    val warning = "CRL has expired. nextUpdate: $nextUpdate, Current: $currentTime"
                    Log.w("CRLVerifier", warning)
                    return CRLParsingResult(false, warning, thisUpdate, nextUpdate, revokedCertCount)
                }
            }
            
            // Check if nextUpdate is approaching (within warning threshold)
            val timeUntilNextUpdate = nextUpdate.time - currentTime.time
            val hoursUntilNextUpdate = TimeUnit.MILLISECONDS.toHours(timeUntilNextUpdate)
            val warningThreshold = getWarningThresholdHours()
            
            val warnings = mutableListOf<String>()
            
            if (hoursUntilNextUpdate < warningThreshold) {
                val warning = "CRL nextUpdate is within ${hoursUntilNextUpdate}h (threshold: ${warningThreshold}h). Next update: $nextUpdate"
                Log.w("CRLVerifier", warning)
                warnings.add(warning)
            }
            
            CRLParsingResult(
                isValid = true,
                warningMessage = if (warnings.isNotEmpty()) warnings.joinToString("; ") else null,
                thisUpdate = thisUpdate,
                nextUpdate = nextUpdate,
                revokedCertificateCount = revokedCertCount
            )
        } catch (e: Exception) {
            Log.e("CRLVerifier", "Error parsing CRL", e)
            Log.e("CRLVerifier", "Parse exception type: ${e.javaClass.name}, Message: ${e.message}")
            e.printStackTrace()
            CRLParsingResult(false, "Failed to parse CRL: ${e.message ?: e.javaClass.simpleName}", null, null)
        }
    }

    /**
     * Create permissive SSL context (reused from NetworkMonitor pattern)
     */
    private fun createPermissiveSSLContext(): SSLContext {
        return try {
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(
                null,
                arrayOf<TrustManager>(PermissiveTrustManager()),
                java.security.SecureRandom()
            )
            sslContext
        } catch (e: Exception) {
            throw RuntimeException("Failed to create SSL context", e)
        }
    }

    /**
     * Permissive TrustManager (same pattern as NetworkMonitor)
     */
    private class PermissiveTrustManager : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {
            // Accept all client certificates
        }

        override fun checkServerTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {
            chain?.let {
                try {
                    it.forEachIndexed { index, cert ->
                        Log.d("CRLVerifier", "Certificate[$index]: Subject=${cert.subjectDN}, Issuer=${cert.issuerDN}")
                        val currentTime = System.currentTimeMillis()
                        if (cert.notAfter.time < currentTime) {
                            Log.w("CRLVerifier", "Certificate expired: ${cert.subjectDN}, Expired: ${cert.notAfter}")
                        } else if (cert.notBefore.time > currentTime) {
                            Log.w("CRLVerifier", "Certificate not yet valid: ${cert.subjectDN}, Valid from: ${cert.notBefore}")
                        }
                    }
                } catch (e: Exception) {
                    Log.w("CRLVerifier", "Error checking certificate chain: ${e.message}")
                }
            }
        }

        override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
            return arrayOf()
        }
    }

    /**
     * Permissive HostnameVerifier (same pattern as NetworkMonitor)
     */
    private class PermissiveHostnameVerifier : HostnameVerifier {
        override fun verify(hostname: String?, session: SSLSession?): Boolean {
            session?.let {
                try {
                    val peerHostname = it.peerPrincipal?.name
                    if (peerHostname != null && hostname != null) {
                        val certHostname = peerHostname.removePrefix("CN=")
                        if (!certHostname.equals(hostname, ignoreCase = true) &&
                            !certHostname.contains(hostname, ignoreCase = true)) {
                            Log.w("CRLVerifier", "Hostname mismatch: Expected=$hostname, Certificate=$certHostname")
                        }
                    }
                } catch (e: Exception) {
                    Log.w("CRLVerifier", "Error verifying hostname for $hostname: ${e.message}")
                }
            }
            return true
        }
    }
}
