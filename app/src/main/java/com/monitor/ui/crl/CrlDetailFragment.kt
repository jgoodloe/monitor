package com.monitor.ui.crl

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.monitor.R
import com.monitor.util.CRLVerifier
import com.monitor.util.DnsResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class CrlDetailFragment : Fragment() {

    private lateinit var crlVerifier: CRLVerifier
    private val dnsResolver = DnsResolver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crlVerifier = CRLVerifier(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_crl_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val crlUrl = arguments?.getString("crlUrl") ?: return

        val textCrlUrl = view.findViewById<TextView>(R.id.text_crl_url)
        val textRevokedCount = view.findViewById<TextView>(R.id.text_revoked_count)
        val textThisUpdate = view.findViewById<TextView>(R.id.text_this_update)
        val textNextUpdate = view.findViewById<TextView>(R.id.text_next_update)
        val textTimeRemaining = view.findViewById<TextView>(R.id.text_time_remaining)
        val containerDns = view.findViewById<LinearLayout>(R.id.container_dns)
        val textLogs = view.findViewById<TextView>(R.id.text_logs)

        textCrlUrl.text = crlUrl

        lifecycleScope.launch {
            val logs = mutableListOf<String>()
            logs.add("Starting CRL detail check for: $crlUrl")
            
            try {
                // Verify CRL
                logs.add("Verifying CRL...")
                val crlResult = withContext(Dispatchers.IO) {
                    crlVerifier.verifyCRL(crlUrl)
                }
                logs.add("CRL verification complete: canDownload=${crlResult.canDownload}, isValid=${crlResult.isValid}")

                // Check if fragment is still attached before accessing context
                if (!isAdded) return@launch

                val context = view.context
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                // Display revoked certificate count
                if (crlResult.revokedCertificateCount != null) {
                    textRevokedCount.text = "CRL contains ${crlResult.revokedCertificateCount} revoked certificates"
                } else {
                    textRevokedCount.text = "CRL revoked certificate count: unknown"
                }

                // Display validity period
                if (crlResult.thisUpdate != null) {
                    textThisUpdate.text = "thisUpdate: ${dateFormat.format(crlResult.thisUpdate)}"
                } else {
                    textThisUpdate.text = "thisUpdate: unknown"
                }

                if (crlResult.nextUpdate != null) {
                    textNextUpdate.text = "nextUpdate: ${dateFormat.format(crlResult.nextUpdate)}"
                    
                    // Calculate time remaining
                    val currentTime = System.currentTimeMillis()
                    val timeRemaining = crlResult.nextUpdate.time - currentTime
                    if (timeRemaining > 0) {
                        val hours = TimeUnit.MILLISECONDS.toHours(timeRemaining)
                        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeRemaining) % 60
                        textTimeRemaining.text = "Time until nextUpdate: ${hours}h ${minutes}m"
                        logs.add("CRL expires in ${hours}h ${minutes}m")
                    } else {
                        textTimeRemaining.text = "CRL has EXPIRED"
                        textTimeRemaining.setTextColor(context.getColor(R.color.status_down))
                        logs.add("CRL has expired")
                    }
                } else {
                    textNextUpdate.text = "nextUpdate: unknown"
                    textTimeRemaining.text = "Time until nextUpdate: unknown"
                }

                // Perform DNS lookup for CRL hostname
                try {
                    val url = URL(crlUrl)
                    val hostname = url.host
                    logs.add("Performing DNS lookup for: $hostname")
                    
                    val dnsResult = withContext(Dispatchers.IO) {
                        dnsResolver.resolveWithPing(hostname)
                    }
                    logs.addAll(dnsResult.logs)

                    if (!isAdded) return@launch

                    containerDns.removeAllViews()
                    if (dnsResult.ipAddresses.isEmpty()) {
                        containerDns.addView(TextView(context).apply { 
                            text = "No IP addresses resolved"
                            setTextColor(context.getColor(R.color.status_down))
                        })
                    } else {
                        dnsResult.ipAddresses.forEach { ip ->
                            containerDns.addView(TextView(context).apply { text = "• $ip" })
                        }
                    }

                    textLogs.text = logs.joinToString("\n")
                } catch (e: Exception) {
                    logs.add("DNS lookup error: ${e.message}")
                    if (isAdded) {
                        containerDns.removeAllViews()
                        containerDns.addView(TextView(view.context).apply {
                            text = "DNS lookup failed: ${e.message}"
                            setTextColor(view.context.getColor(R.color.status_down))
                        })
                    }
                }

                textLogs.text = logs.joinToString("\n")
            } catch (e: Exception) {
                logs.add("Error: ${e.message}")
                if (isAdded) {
                    textLogs.text = logs.joinToString("\n")
                    val context = view.context
                    textRevokedCount.text = "Error loading revoked count"
                    textThisUpdate.text = "Error loading validity period"
                    textNextUpdate.text = "Error loading nextUpdate"
                    textTimeRemaining.text = "Error calculating time remaining"
                }
            }
        }
    }
}




