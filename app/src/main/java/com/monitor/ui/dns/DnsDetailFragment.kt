package com.monitor.ui.dns

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.monitor.R
import com.monitor.util.DnsResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DnsDetailFragment : Fragment() {

    private val dnsResolver = DnsResolver()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dns_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val hostname = arguments?.getString("hostname") ?: return

        val textHostname = view.findViewById<TextView>(R.id.text_hostname)
        val containerIps = view.findViewById<LinearLayout>(R.id.container_ips)
        val containerPings = view.findViewById<LinearLayout>(R.id.container_pings)
        val textLogs = view.findViewById<TextView>(R.id.text_logs)

        textHostname.text = hostname

        lifecycleScope.launch {
            val logs = mutableListOf<String>()
            try {
                val result = withContext(Dispatchers.IO) {
                    dnsResolver.resolveWithPing(hostname)
                }

                // Check if fragment is still attached before accessing context
                if (!isAdded) return@launch

                val context = view.context
                containerIps.removeAllViews()
                if (result.ipAddresses.isEmpty()) {
                    containerIps.addView(TextView(context).apply { text = "(no IPs)" })
                } else {
                    result.ipAddresses.forEach { ip ->
                        containerIps.addView(TextView(context).apply { text = ip })
                    }
                }

                containerPings.removeAllViews()
                if (result.pingResults.isEmpty()) {
                    containerPings.addView(TextView(context).apply { text = "(no ping results)" })
                } else {
                    result.pingResults.forEach { ping ->
                        containerPings.addView(TextView(context).apply {
                            val status = if (ping.success) "OK" else "FAIL"
                            val latency = ping.latencyMs?.toString() ?: "-"
                            text = "${ping.ipAddress}: ${status} (${latency}ms)"
                            setTextColor(context.getColor(if (ping.success) R.color.status_up else R.color.status_down))
                        })
                    }
                }

                textLogs.text = result.logs.joinToString("\n")
            } catch (e: Exception) {
                logs.add("Error: ${e.message}")
                textLogs.text = logs.joinToString("\n")
                // show placeholders to avoid empty screen
                if (isAdded) {
                    val context = view.context
                    containerIps.removeAllViews()
                    containerIps.addView(TextView(context).apply { text = "(no IPs)" })
                    containerPings.removeAllViews()
                    containerPings.addView(TextView(context).apply { text = "(no ping results)" })
                }
            }
        }
    }
}
