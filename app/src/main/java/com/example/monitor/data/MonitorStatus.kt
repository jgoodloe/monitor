package com.example.monitor.data

import java.util.Date

sealed class MonitorItem {
    data class Header(val title: String) : MonitorItem()
    data class Status(
        val name: String, 
        val isUp: Boolean, 
        val errorMessage: String? = null,
        val validityPeriodStart: Date? = null,
        val validityPeriodEnd: Date? = null,
        val itemType: ItemType
    ) : MonitorItem()
    
    enum class ItemType {
        URL, DNS, CRL
    }
}

// Keep the old data class for backward compatibility in ViewModel
data class MonitorStatus(
    val name: String, 
    val isUp: Boolean, 
    val errorMessage: String? = null,
    val validityPeriodStart: Date? = null,
    val validityPeriodEnd: Date? = null
)
