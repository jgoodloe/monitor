package com.example.monitor.ui.monitor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.monitor.R
import com.example.monitor.data.MonitorStatus

class MonitorAdapter : ListAdapter<MonitorStatus, MonitorAdapter.MonitorViewHolder>(MonitorDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonitorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_monitor_status, parent, false)
        return MonitorViewHolder(view)
    }

    override fun onBindViewHolder(holder: MonitorViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MonitorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val urlTextView: TextView = itemView.findViewById(R.id.text_view_url)
        private val statusTextView: TextView = itemView.findViewById(R.id.text_view_status)

        fun bind(status: MonitorStatus) {
            urlTextView.text = status.name
            if (status.isUp) {
                statusTextView.text = "Up"
                statusTextView.setTextColor(itemView.context.getColor(R.color.status_up))
            } else {
                statusTextView.text = "Down: ${status.errorMessage}"
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
