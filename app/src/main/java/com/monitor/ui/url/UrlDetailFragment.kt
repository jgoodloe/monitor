package com.monitor.ui.url

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.monitor.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class UrlDetailFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_url_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val urlString = arguments?.getString("urlString") ?: return

        val textUrl = view.findViewById<TextView>(R.id.text_url)
        val containerIps = view.findViewById<LinearLayout>(R.id.container_ips)
        val containerResults = view.findViewById<LinearLayout>(R.id.container_results)
        val textLogs = view.findViewById<TextView>(R.id.text_logs)

        textUrl.text = urlString

        lifecycleScope.launch {
            val logs = mutableListOf<String>()
            try {
                val parsed = Uri.parse(urlString)
                val host = parsed.host ?: run {
                    logs.add("Invalid URL: missing host")
                    textLogs.text = logs.joinToString("\n")
                    return@launch
                }
                logs.add("Resolving host: $host")

                val ips = withContext(Dispatchers.IO) {
                    try { InetAddress.getAllByName(host).map { it.hostAddress } } catch (e: Exception) { logs.add("DNS error: ${e.message}"); emptyList() }
                }
                if (ips.isEmpty()) logs.add("No IPs resolved for $host") else logs.add("IPs: ${ips.joinToString()}")

                containerIps.removeAllViews()
                if (ips.isEmpty()) {
                    containerIps.addView(TextView(requireContext()).apply { text = "(no IPs)" })
                } else {
                    ips.forEach { ip -> containerIps.addView(TextView(requireContext()).apply { text = ip }) }
                }

                containerResults.removeAllViews()
                val scheme = parsed.scheme ?: "https"
                val pathAndQuery = urlString.substringAfter(host)

                val results = withContext(Dispatchers.IO) {
                    ips.map { ip ->
                        val ipUrlString = "$scheme://$ip$pathAndQuery"
                        val r = fetchViaIp(ipUrlString, host, logs)
                        Triple(ip, r.success, r.code ?: -1) to r.log
                    }
                }

                results.forEach { (info, log) ->
                    val (ip, success, code) = info
                    containerResults.addView(TextView(requireContext()).apply {
                        text = "$ip -> ${if (success) "HTTP $code" else "FAILED"}"
                        setTextColor(requireContext().getColor(if (success) R.color.status_up else R.color.status_down))
                    })
                    logs.add(log)
                }
            } catch (e: Exception) {
                logs.add("Unhandled error: ${e.message}")
            } finally {
                textLogs.text = logs.joinToString("\n")
            }
        }
    }

    private data class FetchResult(val success: Boolean, val code: Int?, val error: String?, val log: String)

    private fun fetchViaIp(ipUrlString: String, hostHeader: String, logs: MutableList<String>): FetchResult {
        val sb = StringBuilder()
        return try {
            val url = URL(ipUrlString)
            val conn = url.openConnection() as HttpURLConnection
            if (conn is HttpsURLConnection) {
                val sslContext = createPermissiveSSLContext()
                val factory: SSLSocketFactory = sslContext.socketFactory
                conn.sslSocketFactory = factory
                conn.hostnameVerifier = HostnameVerifier { _, _ -> true }
                sb.append("SSL: permissive context set\n")
            }
            conn.setRequestProperty("Host", hostHeader)
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            sb.append("GET ").append(ipUrlString).append('\n')
            conn.requestMethod = "GET"
            conn.connect()
            val code = conn.responseCode
            sb.append("Response: ").append(code)
            FetchResult(true, code, null, sb.toString())
        } catch (e: Exception) {
            sb.append("Error: ").append(e.message)
            FetchResult(false, null, e.message, sb.toString())
        }
    }

    private fun createPermissiveSSLContext(): SSLContext {
        val trustAll = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = emptyArray()
        }
        return SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(trustAll), java.security.SecureRandom())
        }
    }
}
