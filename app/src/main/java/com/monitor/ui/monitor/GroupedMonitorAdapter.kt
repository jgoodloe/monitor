package com.monitor.ui.monitor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.monitor.R
import com.monitor.data.MonitorItem
import java.text.SimpleDateFormat
import java.util.Locale

class GroupedMonitorAdapter(
    private val onItemClick: (String) -> Unit
) : ListAdapter<MonitorItem, RecyclerView.ViewHolder>(MonitorItemDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_STATUS = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is MonitorItem.Header -> VIEW_TYPE_HEADER
            is MonitorItem.Status -> VIEW_TYPE_STATUS
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_monitor_header, parent, false)
                HeaderViewHolder(view)
            }
            VIEW_TYPE_STATUS -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_monitor_status, parent, false)
                StatusViewHolder(view, onItemClick)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is MonitorItem.Header -> (holder as HeaderViewHolder).bind(item)
            is MonitorItem.Status -> (holder as StatusViewHolder).bind(item)
        }
    }

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val headerTextView: TextView = itemView as TextView
        
        init {
            // Headers are not clickable
            itemView.isClickable = false
            itemView.isFocusable = false
        }
        
        fun bind(header: MonitorItem.Header) {
            headerTextView.text = header.title
        }
    }

    class StatusViewHolder(
        itemView: View,
        private val onItemClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val urlTextView: TextView = itemView.findViewById(R.id.text_view_url)
        private val statusTextView: TextView = itemView.findViewById(R.id.text_view_status)
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = (bindingAdapter as? GroupedMonitorAdapter)?.getItem(position)
                    if (item is MonitorItem.Status) {
                        onItemClick(item.name)
                    }
                }
            }
        }

        fun bind(status: MonitorItem.Status) {
            urlTextView.text = status.name
            
            // Adjust padding based on item type (less padding for DNS)
            val verticalPadding = when (status.itemType) {
                MonitorItem.ItemType.DNS -> 8 // Less padding for DNS (8dp)
                else -> 16 // Normal padding for URLs and CRLs (16dp)
            }
            // Keep horizontal padding as defined in XML (16dp)
            itemView.setPaddingRelative(
                itemView.paddingStart,
                verticalPadding,
                itemView.paddingEnd,
                verticalPadding
            )
            
            // Check if this is a CRL or HTTPS URL with validity period
            val isCRL = status.itemType == MonitorItem.ItemType.CRL
            val isHTTPS = status.itemType == MonitorItem.ItemType.URL && 
                         status.name.startsWith("https://", ignoreCase = true)
            
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

class MonitorItemDiffCallback : DiffUtil.ItemCallback<MonitorItem>() {
    override fun areItemsTheSame(oldItem: MonitorItem, newItem: MonitorItem): Boolean {
        return when {
            oldItem is MonitorItem.Header && newItem is MonitorItem.Header ->
                oldItem.title == newItem.title
            oldItem is MonitorItem.Status && newItem is MonitorItem.Status ->
                oldItem.name == newItem.name
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: MonitorItem, newItem: MonitorItem): Boolean {
        return oldItem == newItem
    }
}

