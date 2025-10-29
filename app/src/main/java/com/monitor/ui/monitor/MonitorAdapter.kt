package com.monitor.ui.monitor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.monitor.R
import com.monitor.data.MonitorStatus
import java.text.SimpleDateFormat
import java.util.Locale

class MonitorAdapter(
    private val onItemClick: (MonitorStatus) -> Unit
) : ListAdapter<MonitorStatus, MonitorAdapter.MonitorViewHolder>(MonitorDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonitorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_monitor_status, parent, false)
        return MonitorViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: MonitorViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MonitorViewHolder(
        itemView: View,
        private val onItemClick: (MonitorStatus) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val urlTextView: TextView = itemView.findViewById(R.id.text_view_url)
        private val statusTextView: TextView = itemView.findViewById(R.id.text_view_status)
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val status = (bindingAdapter as? MonitorAdapter)?.getItem(position)
                    status?.let { onItemClick(it) }
                }
            }
        }

        fun bind(status: MonitorStatus) {
            urlTextView.text = status.name
            
            // Check if this is a CRL or HTTPS URL with validity period
            val isCRL = status.name.contains(".crl", ignoreCase = true)
            val isHTTPS = status.name.startsWith("https://", ignoreCase = true)
            
            if (status.isUp) {
                val statusText = buildString {
                    append("Up")
                    
                    // For HTTPS URLs, show only expiry date
                    if (isHTTPS && status.validityPeriodEnd != null) {
                        append("\nCert Expires: ${dateFormat.format(status.validityPeriodEnd)}")
                    }
                    
                    // For CRLs, show validity period (start and end)
                    if (isCRL && status.validityPeriodStart != null && status.validityPeriodEnd != null) {
                        append("\nValid: ${dateFormat.format(status.validityPeriodStart)}")
                        append("\nto: ${dateFormat.format(status.validityPeriodEnd)}")
                    }
                    
                    // Add any error/warning messages (including certificate expiry warnings)
                    if (status.errorMessage != null) {
                        val errorMsg = status.errorMessage
                        // For HTTPS, always show certificate warnings even if validity period is shown
                        val shouldShow = if (isHTTPS) {
                            // Always show HTTPS messages (they may contain expiry warnings)
                            true
                        } else if (isCRL) {
                            // For CRLs, don't duplicate validity period info
                            val alreadyHasValidity = errorMsg.contains("Valid:", ignoreCase = true)
                            !alreadyHasValidity
                        } else {
                            true
                        }
                        if (shouldShow) {
                            append(if (isCRL || isHTTPS) "\n$errorMsg" else ": $errorMsg")
                        }
                    }
                }
                statusTextView.text = statusText
                statusTextView.setTextColor(itemView.context.getColor(R.color.status_up))
            } else {
                val statusText = buildString {
                    append("Down")
                    
                    // For HTTPS URLs, show only expiry date even if down
                    if (isHTTPS && status.validityPeriodEnd != null) {
                        append("\nCert Expires: ${dateFormat.format(status.validityPeriodEnd)}")
                    }
                    
                    // For CRLs, show validity period even if down
                    if (isCRL && status.validityPeriodStart != null && status.validityPeriodEnd != null) {
                        append("\nValid: ${dateFormat.format(status.validityPeriodStart)}")
                        append("\nto: ${dateFormat.format(status.validityPeriodEnd)}")
                    }
                    
                    // Add error message
                    val errorMsg = status.errorMessage ?: "Unknown error"
                    append(if (isCRL || isHTTPS) "\n$errorMsg" else ": $errorMsg")
                }
                statusTextView.text = statusText
                statusTextView.setTextColor(itemView.context.getColor(R.color.status_down))
            }
        }
    }
}

class MonitorDiffCallback : DiffUtil.ItemCallback<MonitorStatus>() {
    override fun areItemsTheSame(oldItem: MonitorStatus, newItem: MonitorStatus): Boolean {
        return oldItem.name == newItem.name
    }

    override fun areContentsTheSame(oldItem: MonitorStatus, newItem: MonitorStatus): Boolean {
        return oldItem == newItem
    }
}
