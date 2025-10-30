package com.monitor.ui.monitor

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.monitor.databinding.FragmentMonitorBinding

class MonitorFragment : Fragment() {

    private var _binding: FragmentMonitorBinding? = null
    private val binding get() = _binding!!

    private lateinit var monitorViewModel: MonitorViewModel
    private lateinit var monitorAdapter: MonitorAdapter
    private lateinit var groupedAdapter: GroupedMonitorAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        monitorViewModel = ViewModelProvider(this).get(MonitorViewModel::class.java)

        _binding = FragmentMonitorBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupRecyclerView()
        setupSwipeToRefresh()

        monitorViewModel.statuses.observe(viewLifecycleOwner) {
            monitorAdapter.submitList(it)
            binding.swipeRefreshLayout.isRefreshing = false
        }

        monitorViewModel.items.observe(viewLifecycleOwner) {
            Log.i("MonitorFragment", "Received ${it?.size ?: 0} items from ViewModel")
            it?.forEachIndexed { index, item ->
                Log.d("MonitorFragment", "Item $index: ${item.javaClass.simpleName}")
            }
            groupedAdapter.submitList(it)
        }

        monitorViewModel.lastTestTime.observe(viewLifecycleOwner) { testTime ->
            binding.textLastTestTime.text = "Last test: $testTime"
        }

        monitorViewModel.startMonitoring()

        return root
    }

    private fun setupRecyclerView() {
        monitorAdapter = MonitorAdapter { status ->
            // Retest the clicked item
            monitorViewModel.retestItem(status.name)
            // Show visual feedback
            binding.swipeRefreshLayout.isRefreshing = true
        }
        
        groupedAdapter = GroupedMonitorAdapter { itemName ->
            // Retest the clicked item
            monitorViewModel.retestItem(itemName)
            // Show visual feedback
            binding.swipeRefreshLayout.isRefreshing = true
        }
        
        binding.recyclerViewMonitor.apply {
            adapter = groupedAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            monitorViewModel.startMonitoring()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
