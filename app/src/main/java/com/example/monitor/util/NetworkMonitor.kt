package com.example.monitor.util

import android.util.Log
import java.net.HttpURLConnection
import java.net.URL
import java.security.cert.X509Certificate
import javax.net.ssl.*
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.Date
import java.util.concurrent.TimeUnit

data class URLCheckResult(
    val isUp: Boolean,
    val errorMessage: String?,
    val certificateNotBefore: Date? = null,
    val certificateNotAfter: Date? = null,
    val certificateExpiryWarning: String? = null
)

class NetworkMonitor {

    private val certificateWarnings = mutableListOf<String>()
    private val certificateExpiryDaysThreshold = 30
    private val storedCertificates = mutableMapOf<String, X509Certificate?>()

    init {
        setupTrustAllCertificates()
    }

    fun checkUrl(urlString: String): URLCheckResult {
        Log.i("NetworkMonitor", "Checking URL: $urlString")
        certificateWarnings.clear()
        var certificate: X509Certificate? = null
        
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            
            // Apply SSL context for HTTPS connections
            if (url.protocol == "https") {
                (connection as? HttpsURLConnection)?.let { httpsConnection ->
                    try {
                        val sslContext = createPermissiveSSLContext(urlString)
                        httpsConnection.sslSocketFactory = sslContext.socketFactory
                        httpsConnection.hostnameVerifier = PermissiveHostnameVerifier()
                    } catch (e: Exception) {
                        Log.w("NetworkMonitor", "Failed to set SSL context for $urlString: ${e.message}")
                        certificateWarnings.add("SSL context setup failed: ${e.message}")
                    }
                }
            }
            
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()
            
            // Log any certificate warnings encountered
            if (certificateWarnings.isNotEmpty()) {
                certificateWarnings.forEach { warning ->
                    Log.w("NetworkMonitor", "Certificate warning for $urlString: $warning")
                }
            }
            
            // Extract certificate information for validation (must be done after connect)
            var certNotBefore: Date? = null
            var certNotAfter: Date? = null
            var certExpiryWarning: String? = null
            
            if (url.protocol == "https") {
                try {
                    // Get certificate that was stored during SSL handshake
                    val storedCert = storedCertificates[urlString]
                    if (storedCert != null) {
                        certNotBefore = Date(storedCert.notBefore.time)
                        certNotAfter = Date(storedCert.notAfter.time)
                        certExpiryWarning = checkCertificateExpiry(storedCert, urlString)
                        Log.i("NetworkMonitor", "Certificate for $urlString: Valid from ${storedCert.notBefore} to ${storedCert.notAfter}")
                    } else {
                        Log.w("NetworkMonitor", "No certificate stored for $urlString")
                    }
                } catch (e: Exception) {
                    Log.w("NetworkMonitor", "Error extracting certificate info for $urlString: ${e.message}")
                }
            }
            
            // Clear stored certificate for this URL after use
            storedCertificates.remove(urlString)
            
            val responseCode = connection.responseCode
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val warningsInfo = if (certificateWarnings.isNotEmpty()) {
                    " (Warnings: ${certificateWarnings.size})"
                } else ""
                Log.i("NetworkMonitor", "URL: $urlString, Response Code: $responseCode, Result: true$warningsInfo")
                URLCheckResult(true, certExpiryWarning, certNotBefore, certNotAfter, certExpiryWarning)
            } else {
                val errorMessage = "HTTP Error: $responseCode"
                Log.w("NetworkMonitor", "URL: $urlString, Result: false, Error: $errorMessage")
                URLCheckResult(false, errorMessage, certNotBefore, certNotAfter, certExpiryWarning)
            }
        } catch (e: SSLException) {
            val warning = "SSL/Certificate error: ${e.message}"
            certificateWarnings.add(warning)
            Log.w("NetworkMonitor", "SSL error for $urlString: ${e.message}", e)
            // Try to continue despite SSL error
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                var certNotBefore: Date? = null
                var certNotAfter: Date? = null
                var certExpiryWarning: String? = null
                
                if (url.protocol == "https") {
                    (connection as? HttpsURLConnection)?.let { httpsConnection ->
                        val sslContext = createPermissiveSSLContext(urlString)
                        httpsConnection.sslSocketFactory = sslContext.socketFactory
                        httpsConnection.hostnameVerifier = PermissiveHostnameVerifier()
                        try {
                            // Get certificate that was stored during SSL handshake
                            val storedCert = storedCertificates[urlString]
                            if (storedCert != null) {
                                certNotBefore = Date(storedCert.notBefore.time)
                                certNotAfter = Date(storedCert.notAfter.time)
                                certExpiryWarning = checkCertificateExpiry(storedCert, urlString)
                            }
                        } catch (certEx: Exception) {
                            Log.w("NetworkMonitor", "Error extracting certificate after SSL retry: ${certEx.message}")
                        }
                    }
                }
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.connect()
                val responseCode = connection.responseCode
                Log.w("NetworkMonitor", "URL: $urlString succeeded after SSL warning, Response Code: $responseCode")
                val errorMsg = if (responseCode != HttpURLConnection.HTTP_OK) "HTTP Error: $responseCode" else certExpiryWarning
                URLCheckResult(responseCode == HttpURLConnection.HTTP_OK, errorMsg, certNotBefore, certNotAfter, certExpiryWarning)
            } catch (retryException: Exception) {
                Log.e("NetworkMonitor", "Failed to connect to $urlString after SSL error retry", retryException)
                URLCheckResult(false, retryException.message)
            }
        } catch (e: Exception) {
            Log.e("NetworkMonitor", "Error checking URL: $urlString", e)
            URLCheckResult(false, e.message)
        }
    }

    /**
     * Check certificate expiry and return warning message if expired or expiring soon
     */
    private fun checkCertificateExpiry(cert: X509Certificate, url: String): String? {
        val currentTime = System.currentTimeMillis()
        val notBefore = cert.notBefore.time
        val notAfter = cert.notAfter.time
        
        // Check if expired
        if (notAfter < currentTime) {
            val expiredDays = TimeUnit.MILLISECONDS.toDays(currentTime - notAfter)
            val warning = "Certificate EXPIRED $expiredDays days ago (expired: ${cert.notAfter})"
            Log.w("NetworkMonitor", "Certificate expiry warning for $url: $warning")
            return warning
        }
        
        // Check if expires within threshold
        val daysUntilExpiry = TimeUnit.MILLISECONDS.toDays(notAfter - currentTime)
        if (daysUntilExpiry <= certificateExpiryDaysThreshold) {
            val warning = "Certificate expires in $daysUntilExpiry days (expires: ${cert.notAfter})"
            Log.w("NetworkMonitor", "Certificate expiry warning for $url: $warning")
            return warning
        }
        
        // Check if not yet valid
        if (notBefore > currentTime) {
            val warning = "Certificate not yet valid (valid from: ${cert.notBefore})"
            Log.w("NetworkMonitor", "Certificate validity warning for $url: $warning")
            return warning
        }
        
        return null
    }

    private fun setupTrustAllCertificates() {
        // Note: We create SSL contexts per-connection now to store certificates
        // So we don't set default here
    }

    private fun createPermissiveSSLContext(urlString: String? = null): SSLContext {
        return try {
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(
                null,
                arrayOf<TrustManager>(PermissiveTrustManager(urlString)),
                SecureRandom()
            )
            sslContext
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Failed to create SSL context", e)
        } catch (e: KeyManagementException) {
            throw RuntimeException("Failed to initialize SSL context", e)
        }
    }

    /**
     * TrustManager that logs certificate warnings but accepts all certificates
     * Stores certificates for later retrieval
     */
    private inner class PermissiveTrustManager(private val urlString: String?) : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            // Accept all client certificates
        }

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
            // Store server certificate for later retrieval
            if (urlString != null && chain != null && chain.isNotEmpty()) {
                storedCertificates[urlString] = chain[0]
            }
            
            // Log certificate information but accept all
            chain?.let {
                try {
                    it.forEachIndexed { index, cert ->
                        Log.d("NetworkMonitor", "Certificate[$index]: Subject=${cert.subjectDN}, Issuer=${cert.issuerDN}")
                        // Check for potential issues and log warnings
                        val currentTime = System.currentTimeMillis()
                        if (cert.notAfter.time < currentTime) {
                            Log.w("NetworkMonitor", "Certificate expired: ${cert.subjectDN}, Expired: ${cert.notAfter}")
                        } else if (cert.notBefore.time > currentTime) {
                            Log.w("NetworkMonitor", "Certificate not yet valid: ${cert.subjectDN}, Valid from: ${cert.notBefore}")
                        }
                    }
                } catch (e: Exception) {
                    Log.w("NetworkMonitor", "Error checking certificate chain: ${e.message}")
                }
            }
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return arrayOf()
        }
    }

    /**
     * HostnameVerifier that logs mismatches but accepts all hostnames
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
                            Log.w("NetworkMonitor", "Hostname mismatch: Expected=$hostname, Certificate=$certHostname")
                        }
                    }
                } catch (e: Exception) {
                    Log.w("NetworkMonitor", "Error verifying hostname for $hostname: ${e.message}")
                }
            }
            // Always return true to accept all hostnames
            return true
        }
    }
}
