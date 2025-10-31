package com.monitor.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray

/**
 * Manages configuration for URLs, CRLs, and DNS hosts to monitor
 * Stores values in SharedPreferences as JSON arrays
 */
class ConfigurationManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("MonitorConfig", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_URLS = "monitor_urls"
        private const val KEY_DNS_HOSTS = "monitor_dns_hosts"
        private const val KEY_CRL_URLS = "monitor_crl_urls"
        
        // Default values if none are saved
        private val DEFAULT_URLS = listOf(
            "https://pivi.xcloud.authentx.com/portal/index.html",
            "https://piv.xcloud.authentx.com/portal/index.html"
        )
        
        private val DEFAULT_DNS_HOSTS = listOf(
            "piv.xcloud.authentx.com",
            "pivi.xcloud.authentx.com",
            "ocsp.xca.xpki.com",
            "crl.xca.xpki.com",
            "aia.xca.xpki.com"
        )
        
        private val DEFAULT_CRL_URLS = listOf(
            "http://crl.xca.xpki.com/CRLs/XTec_PIVI_CA1.crl",
            "http://66.165.167.225/CRLs/XTec_PIVI_CA1.crl",
            "http://152.186.38.46/CRLs/XTec_PIVI_CA1.crl"
        )
    }
    
    /**
     * Save a list to SharedPreferences as JSON
     */
    private fun saveList(key: String, list: List<String>) {
        val jsonArray = JSONArray(list)
        prefs.edit().putString(key, jsonArray.toString()).apply()
        Log.d("ConfigurationManager", "Saved $key: ${list.size} items")
    }
    
    /**
     * Load a list from SharedPreferences, or return default if not found
     */
    private fun loadList(key: String, default: List<String>): List<String> {
        val jsonString = prefs.getString(key, null)
        return if (jsonString != null) {
            try {
                val jsonArray = JSONArray(jsonString)
                val list = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    list.add(jsonArray.getString(i))
                }
                Log.d("ConfigurationManager", "Loaded $key: ${list.size} items")
                if (list.isEmpty()) {
                    // Fallback to defaults if an empty list was stored
                    Log.w("ConfigurationManager", "Empty list for $key; using defaults (${default.size})")
                    default
                } else {
                    list
                }
            } catch (e: Exception) {
                Log.e("ConfigurationManager", "Error loading $key, using defaults", e)
                default
            }
        } else {
            // First time - save defaults
            saveList(key, default)
            default
        }
    }
    
    /**
     * Get URLs to monitor
     */
    fun getUrls(): List<String> {
        return loadList(KEY_URLS, DEFAULT_URLS)
    }
    
    /**
     * Save URLs to monitor
     */
    fun saveUrls(urls: List<String>) {
        saveList(KEY_URLS, urls)
    }
    
    /**
     * Get DNS hosts to monitor
     */
    fun getDnsHosts(): List<String> {
        return loadList(KEY_DNS_HOSTS, DEFAULT_DNS_HOSTS)
    }
    
    /**
     * Save DNS hosts to monitor
     */
    fun saveDnsHosts(hosts: List<String>) {
        saveList(KEY_DNS_HOSTS, hosts)
    }
    
    /**
     * Get CRL URLs to monitor
     */
    fun getCrlUrls(): List<String> {
        return loadList(KEY_CRL_URLS, DEFAULT_CRL_URLS)
    }
    
    /**
     * Save CRL URLs to monitor
     */
    fun saveCrlUrls(urls: List<String>) {
        saveList(KEY_CRL_URLS, urls)
    }
}

