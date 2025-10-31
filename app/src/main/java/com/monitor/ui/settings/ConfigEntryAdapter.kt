package com.monitor.ui.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.monitor.R

class ConfigEntryAdapter(
    private val onEditClick: (String, Int) -> Unit,
    private val onDeleteClick: (Int) -> Unit
) : ListAdapter<String, ConfigEntryAdapter.EntryViewHolder>(EntryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_config_entry, parent, false)
        return EntryViewHolder(view, onEditClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    class EntryViewHolder(
        itemView: View,
        private val onEditClick: (String, Int) -> Unit,
        private val onDeleteClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val valueTextView: TextView = itemView.findViewById(R.id.text_entry_value)
        private val editButton: ImageButton = itemView.findViewById(R.id.button_edit)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.button_delete)

        fun bind(value: String, position: Int) {
            valueTextView.text = value
            
            editButton.setOnClickListener {
                onEditClick(value, position)
            }
            
            deleteButton.setOnClickListener {
                onDeleteClick(position)
            }
        }
    }

    class EntryDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}

