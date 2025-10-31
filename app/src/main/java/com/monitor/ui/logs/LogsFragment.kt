package com.monitor.ui.logs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Process
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.monitor.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LogsFragment : Fragment() {

    private var autoRefreshJob: Job? = null
    private var shouldAutoScroll = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_logs, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val swipe = view.findViewById<SwipeRefreshLayout>(R.id.swipe_logs)
        val text = view.findViewById<TextView>(R.id.text_logs)
        val scrollView = view.findViewById<ScrollView>(R.id.scroll_logs)

        val refresh: () -> Unit = {
            lifecycleScope.launch {
                val logs = withContext(Dispatchers.IO) { dumpOwnLogs() }
                val wasAtBottom = shouldAutoScroll
                text.text = logs
                
                // Auto-scroll to bottom if user was already at the bottom
                if (wasAtBottom) {
                    scrollView.post {
                        scrollView.fullScroll(View.FOCUS_DOWN)
                    }
                }
            }
        }

        // Enable copying the entire log on long press
        text.setOnLongClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("App Logs", text.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "Logs copied to clipboard", Toast.LENGTH_SHORT).show()
            true
        }

        // Track user scrolling to disable auto-scroll
        scrollView.setOnScrollChangeListener { _, _, _, _, _ ->
            val atBottom = scrollView.getChildAt(0).bottom <= scrollView.height + scrollView.scrollY
            shouldAutoScroll = atBottom
        }

        swipe.setOnRefreshListener { refresh() }
        refresh()
        
        // Start auto-refresh every 2 seconds
        startAutoRefresh(refresh)
    }

    override fun onPause() {
        super.onPause()
        stopAutoRefresh()
    }

    override fun onResume() {
        super.onResume()
        val swipe = view?.findViewById<SwipeRefreshLayout>(R.id.swipe_logs)
        if (swipe != null) {
            val refresh: () -> Unit = {
                val scrollView = view?.findViewById<ScrollView>(R.id.scroll_logs)
                lifecycleScope.launch {
                    val text = view?.findViewById<TextView>(R.id.text_logs)
                    val logs = withContext(Dispatchers.IO) { dumpOwnLogs() }
                    text?.text = logs
                    if (shouldAutoScroll) {
                        scrollView?.post {
                            scrollView.fullScroll(View.FOCUS_DOWN)
                        }
                    }
                }
            }
            startAutoRefresh(refresh)
        }
    }

    private fun startAutoRefresh(refresh: () -> Unit) {
        stopAutoRefresh()
        autoRefreshJob = lifecycleScope.launch {
            while (isActive) {
                delay(2000) // Update every 2 seconds
                refresh()
            }
        }
    }

    private fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    private fun dumpOwnLogs(): String {
        val pid = Process.myPid().toString()
        val cmd = arrayOf("logcat", "-d", "-v", "time", "--pid", pid)
        return try {
            val proc = Runtime.getRuntime().exec(cmd)
            proc.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "Failed to read logs: ${e.message}"
        }
    }
}
