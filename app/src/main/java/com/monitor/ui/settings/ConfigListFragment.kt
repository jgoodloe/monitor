package com.monitor.ui.settings

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputEditText
import com.monitor.R
import com.monitor.databinding.FragmentConfigListBinding
import com.monitor.databinding.DialogEditEntryBinding
import com.monitor.util.ConfigurationManager

class ConfigListFragment : Fragment() {
    
    private var _binding: FragmentConfigListBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var configManager: ConfigurationManager
    private lateinit var adapter: ConfigEntryAdapter
    private var entries = mutableListOf<String>()
    private var configType: ConfigType? = null
    
    enum class ConfigType {
        URL, DNS, CRL
    }
    
    companion object {
        private const val ARG_CONFIG_TYPE = "config_type"
        
        fun newInstance(type: ConfigType): ConfigListFragment {
            val fragment = ConfigListFragment()
            val args = Bundle()
            args.putString(ARG_CONFIG_TYPE, type.name)
            fragment.arguments = args
            return fragment
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configType = arguments?.getString(ARG_CONFIG_TYPE)?.let { ConfigType.valueOf(it) }
        configManager = ConfigurationManager(requireContext())
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConfigListBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        loadEntries()
        
        adapter = ConfigEntryAdapter(
            onEditClick = { value, position ->
                showEditDialog(value, position)
            },
            onDeleteClick = { position ->
                deleteEntry(position)
            }
        )
        
        binding.recyclerViewEntries.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewEntries.adapter = adapter
        adapter.submitList(entries)
        
        binding.fabAdd.setOnClickListener {
            showAddDialog()
        }
    }
    
    private fun loadEntries() {
        entries = when (configType) {
            ConfigType.URL -> configManager.getUrls().toMutableList()
            ConfigType.DNS -> configManager.getDnsHosts().toMutableList()
            ConfigType.CRL -> configManager.getCrlUrls().toMutableList()
            null -> mutableListOf()
        }
    }
    
    private fun saveEntries() {
        when (configType) {
            ConfigType.URL -> configManager.saveUrls(entries)
            ConfigType.DNS -> configManager.saveDnsHosts(entries)
            ConfigType.CRL -> configManager.saveCrlUrls(entries)
            null -> {}
        }
    }
    
    private fun showAddDialog() {
        showEditDialog("", -1)
    }
    
    private fun showEditDialog(currentValue: String, position: Int) {
        val dialogBinding = DialogEditEntryBinding.inflate(layoutInflater)
        dialogBinding.editTextEntry.setText(currentValue)
        
        val isEdit = position >= 0
        dialogBinding.textDialogTitle.text = if (isEdit) "Edit Entry" else "Add Entry"
        
        // Hint is already set in XML, so we don't need to set it here
        
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton(if (isEdit) "Save" else "Add") { _, _ ->
                val newValue = dialogBinding.editTextEntry.text?.toString()?.trim()
                if (!TextUtils.isEmpty(newValue)) {
                    if (isEdit && position < entries.size) {
                        entries[position] = newValue!!
                        adapter.submitList(entries.toList())
                    } else {
                        entries.add(newValue!!)
                        adapter.submitList(entries.toList())
                    }
                    saveEntries()
                } else {
                    Toast.makeText(context, "Value cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        
        dialog.show()
    }
    
    private fun deleteEntry(position: Int) {
        if (position < entries.size) {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Entry")
                .setMessage("Are you sure you want to delete this entry?")
                .setPositiveButton("Delete") { _, _ ->
                    entries.removeAt(position)
                    adapter.submitList(entries.toList())
                    saveEntries()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

