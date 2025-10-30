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
        private val certInfoTextView: TextView = itemView.findViewById(R.id.text_view_cert_info)
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
            
            // Build details (cert info / validity / failure reason) under URL
            val details = buildString {
                // For HTTPS URLs, show only expiry date
                if (isHTTPS && status.validityPeriodEnd != null) {
                    append("Cert Expires: ${dateFormat.format(status.validityPeriodEnd)}")
                }
                // For CRLs, show validity period (start and end)
                if (isCRL && status.validityPeriodStart != null && status.validityPeriodEnd != null) {
                    if (isNotEmpty()) append('\n')
                    append("Valid: ${dateFormat.format(status.validityPeriodStart)}")
                    append(" to: ${dateFormat.format(status.validityPeriodEnd)}")
                }
                // Always show failure/warning messages here if present
                status.errorMessage?.let { msg ->
                    if (isNotEmpty()) append('\n')
                    append(msg)
                }
            }
            if (details.isNotEmpty()) {
                certInfoTextView.text = details
                certInfoTextView.visibility = View.VISIBLE
            } else {
                certInfoTextView.visibility = View.GONE
            }
            
            // Status text shows only Up/Down, color-coded
            if (status.isUp) {
                statusTextView.text = "Up"
                statusTextView.setTextColor(itemView.context.getColor(R.color.status_up))
            } else {
                statusTextView.text = "Down"
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

