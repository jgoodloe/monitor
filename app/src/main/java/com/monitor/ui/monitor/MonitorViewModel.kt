package com.monitor.ui.monitor

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.monitor.data.MonitorItem
import com.monitor.data.MonitorStatus
import com.monitor.util.CRLVerifier
import com.monitor.util.DnsResolver
import com.monitor.util.NetworkMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MonitorViewModel(application: Application) : AndroidViewModel(application) {

    private val networkMonitor = NetworkMonitor()
    private val dnsResolver = DnsResolver()
    private val crlVerifier = CRLVerifier(application.applicationContext)

    private val _statuses = MutableLiveData<List<MonitorStatus>>()
    val statuses: LiveData<List<MonitorStatus>> = _statuses

    private val _items = MutableLiveData<List<MonitorItem>>()
    val items: LiveData<List<MonitorItem>> = _items

    private val _lastTestTime = MutableLiveData<String>()
    val lastTestTime: LiveData<String> = _lastTestTime

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

    private val crlUrlsToMonitor = listOf(
        "http://crl.xca.xpki.com/CRLs/XTec_PIVI_CA1.crl",
        "http://66.165.167.225/CRLs/XTec_PIVI_CA1.crl",
        "http://152.186.38.46/CRLs/XTec_PIVI_CA1.crl"
    )

    fun startMonitoring() {
        viewModelScope.launch {
            val results = mutableListOf<MonitorStatus>()
            withContext(Dispatchers.IO) {
                // Monitor URLs
                urlsToMonitor.forEach { url ->
                    val urlResult = networkMonitor.checkUrl(url)
                    val errorMsg = buildString {
                        if (urlResult.errorMessage != null) {
                            append(urlResult.errorMessage)
                        }
                        if (urlResult.certificateExpiryWarning != null) {
                            if (isNotEmpty()) append(" | ")
                            append(urlResult.certificateExpiryWarning)
                        }
                    }.ifEmpty { null }
                    results.add(MonitorStatus(
                        url, 
                        urlResult.isUp, 
                        errorMsg,
                        urlResult.certificateNotBefore,
                        urlResult.certificateNotAfter
                    ))
                }
                
                // Monitor DNS hosts
                hostsToMonitor.forEach { host ->
                    val (isUp, errorMessage) = dnsResolver.resolve(host)
                    results.add(MonitorStatus(host, isUp, errorMessage))
                }
                
                // Monitor CRLs
                Log.i("MonitorViewModel", "Starting CRL monitoring for ${crlUrlsToMonitor.size} CRLs")
                crlUrlsToMonitor.forEachIndexed { index, crlUrl ->
                    Log.d("MonitorViewModel", "Processing CRL ${index + 1}/${crlUrlsToMonitor.size}: $crlUrl")
                    try {
                        val crlResult = crlVerifier.verifyCRL(crlUrl)
                        val isUp = crlResult.canDownload && crlResult.isValid
                        
                        // Build status message with validity period info
                        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                        val errorMsg = buildString {
                            when {
                                !crlResult.canDownload -> append("Failed to download CRL")
                                !crlResult.isValid -> append(crlResult.warningMessage ?: "CRL validation failed")
                                crlResult.warningMessage != null -> append("Warning: ${crlResult.warningMessage}")
                            }
                            
                            // Always add validity period if available (even if no warnings)
                            if (crlResult.thisUpdate != null && crlResult.nextUpdate != null) {
                                if (isNotEmpty()) append(" | ")
                                append("Valid: ${dateFormat.format(crlResult.thisUpdate)} - ${dateFormat.format(crlResult.nextUpdate)}")
                            } else if (isEmpty() && crlResult.canDownload) {
                                // If we have no message but downloaded successfully, show basic status
                                append("CRL downloaded")
                            }
                        }.ifEmpty { null }
                        
                        val status = MonitorStatus(
                            crlUrl, 
                            isUp, 
                            errorMsg,
                            crlResult.thisUpdate,
                            crlResult.nextUpdate
                        )
                        results.add(status)
                        Log.i("MonitorViewModel", "Added CRL status: $crlUrl (Up: $isUp)")
                    } catch (e: Exception) {
                        Log.e("MonitorViewModel", "Error processing CRL $crlUrl", e)
                        results.add(MonitorStatus(crlUrl, false, "Error: ${e.message}"))
                    }
                }
                Log.i("MonitorViewModel", "Completed CRL monitoring. Total results: ${results.size}")
            }
            
            // Update last test time
            val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            _lastTestTime.postValue(timeFormat.format(Date()))
            
            _statuses.postValue(results)
            
            // Create grouped items with headers
            updateGroupedItems(results)
        }
    }

    /**
     * Retest a specific item by name/URL
     */
    fun retestItem(itemName: String) {
        viewModelScope.launch {
            val currentStatuses = _statuses.value?.toMutableList() ?: mutableListOf()
            val itemIndex = currentStatuses.indexOfFirst { it.name == itemName }
            
            if (itemIndex == -1) {
                Log.w("MonitorViewModel", "Item not found for retest: $itemName")
                return@launch
            }
            
            withContext(Dispatchers.IO) {
                val updatedStatus = when {
                    // Check if it's a URL
                    itemName.startsWith("http://") || itemName.startsWith("https://") -> {
                        if (itemName.endsWith(".crl", ignoreCase = true)) {
                            // It's a CRL
                            val crlResult = crlVerifier.verifyCRL(itemName)
                            val isUp = crlResult.canDownload && crlResult.isValid
                            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                            val errorMsg = buildString {
                                when {
                                    !crlResult.canDownload -> append("Failed to download CRL")
                                    !crlResult.isValid -> append(crlResult.warningMessage ?: "CRL validation failed")
                                    crlResult.warningMessage != null -> append("Warning: ${crlResult.warningMessage}")
                                }
                                if (crlResult.thisUpdate != null && crlResult.nextUpdate != null) {
                                    if (isNotEmpty()) append(" | ")
                                    append("Valid: ${dateFormat.format(crlResult.thisUpdate)} - ${dateFormat.format(crlResult.nextUpdate)}")
                                }
                            }.ifEmpty { null }
                            MonitorStatus(itemName, isUp, errorMsg, crlResult.thisUpdate, crlResult.nextUpdate)
                        } else {
                            // It's a regular URL
                            val urlResult = networkMonitor.checkUrl(itemName)
                            val errorMsg = buildString {
                                if (urlResult.errorMessage != null) append(urlResult.errorMessage)
                                if (urlResult.certificateExpiryWarning != null) {
                                    if (isNotEmpty()) append(" | ")
                                    append(urlResult.certificateExpiryWarning)
                                }
                            }.ifEmpty { null }
                            MonitorStatus(itemName, urlResult.isUp, errorMsg, urlResult.certificateNotBefore, urlResult.certificateNotAfter)
                        }
                    }
                    // Check if it's a hostname (DNS)
                    hostsToMonitor.contains(itemName) -> {
                        val (isUp, errorMessage) = dnsResolver.resolve(itemName)
                        MonitorStatus(itemName, isUp, errorMessage)
                    }
                    else -> {
                        Log.w("MonitorViewModel", "Unknown item type for retest: $itemName")
                        null // Cannot retest unknown item type
                    }
                }
                
                if (updatedStatus != null && itemIndex != -1) {
                    currentStatuses[itemIndex] = updatedStatus
                    // Update last test time for retest
                    val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    _lastTestTime.postValue(timeFormat.format(Date()))
                    _statuses.postValue(currentStatuses)
                    
                    // Update grouped items
                    updateGroupedItems(currentStatuses)
                }
            }
        }
    }

    /**
     * Set the CRL warning threshold in hours
     */
    fun setCRLWarningThresholdHours(hours: Int) {
        crlVerifier.setWarningThresholdHours(hours)
    }

    /**
     * Get the current CRL warning threshold in hours
     */
    fun getCRLWarningThresholdHours(): Int {
        return crlVerifier.getWarningThresholdHours()
    }

    /**
     * Update grouped items from status list
     */
    private fun updateGroupedItems(results: List<MonitorStatus>) {
        val groupedItems = mutableListOf<MonitorItem>()
        
        // Add URL section
        if (urlsToMonitor.isNotEmpty()) {
            groupedItems.add(MonitorItem.Header("URLs"))
            urlsToMonitor.forEach { url ->
                val status = results.find { it.name == url }
                status?.let {
                    groupedItems.add(MonitorItem.Status(
                        it.name, it.isUp, it.errorMessage,
                        it.validityPeriodStart, it.validityPeriodEnd,
                        MonitorItem.ItemType.URL
                    ))
                }
            }
        }
        
        // Add DNS section
        if (hostsToMonitor.isNotEmpty()) {
            groupedItems.add(MonitorItem.Header("DNS Hosts"))
            hostsToMonitor.forEach { host ->
                val status = results.find { it.name == host }
                status?.let {
                    groupedItems.add(MonitorItem.Status(
                        it.name, it.isUp, it.errorMessage,
                        it.validityPeriodStart, it.validityPeriodEnd,
                        MonitorItem.ItemType.DNS
                    ))
                }
            }
        }
        
        // Add CRL section
        if (crlUrlsToMonitor.isNotEmpty()) {
            groupedItems.add(MonitorItem.Header("CRLs"))
            crlUrlsToMonitor.forEach { crlUrl ->
                val status = results.find { it.name == crlUrl }
                status?.let {
                    groupedItems.add(MonitorItem.Status(
                        it.name, it.isUp, it.errorMessage,
                        it.validityPeriodStart, it.validityPeriodEnd,
                        MonitorItem.ItemType.CRL
                    ))
                }
            }
        }
        
        _items.postValue(groupedItems)
    }
}
