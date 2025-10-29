package com.example.monitor.ui.monitor

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.monitor.data.MonitorStatus
import com.example.monitor.util.DnsResolver
import com.example.monitor.util.NetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MonitorViewModel : ViewModel() {

    private val networkMonitor = NetworkMonitor()
    private val dnsResolver = DnsResolver()

    private val _statuses = MutableLiveData<List<MonitorStatus>>()
    val statuses: LiveData<List<MonitorStatus>> = _statuses

    private val urlsToMonitor = listOf(
        "https://pivi.xcloud.authentx.com/portal/index.html",
        "https://piv.xcloud.authentx.com/portal/index.html"
    )

    private val hostsToMonitor = listOf(
        "piv.xcloud.authentx.com",
        "pivi.xcloud.authentx.com",
        "ocsp.xca.xpki.com",
        "crl.xca.xpki.com",
        "aia.xca.xpki.com"
    )

    fun startMonitoring() {
        viewModelScope.launch {
            val results = mutableListOf<MonitorStatus>()
            withContext(Dispatchers.IO) {
                urlsToMonitor.forEach { url ->
                    val (isUp, errorMessage) = networkMonitor.checkUrl(url)
                    results.add(MonitorStatus(url, isUp, errorMessage))
                }
                hostsToMonitor.forEach { host ->
                    val (isUp, errorMessage) = dnsResolver.resolve(host)
                    results.add(MonitorStatus(host, isUp, errorMessage))
                }
            }
            _statuses.postValue(results)
        }
    }
}
