package com.example.monitor.ui.monitor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.monitor.databinding.FragmentMonitorBinding

class MonitorFragment : Fragment() {

    private var _binding: FragmentMonitorBinding? = null
    private val binding get() = _binding!!

    private lateinit var monitorViewModel: MonitorViewModel
    private lateinit var monitorAdapter: MonitorAdapter

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

        monitorViewModel.startMonitoring()

        return root
    }

    private fun setupRecyclerView() {
        monitorAdapter = MonitorAdapter()
        binding.recyclerViewMonitor.apply {
            adapter = monitorAdapter
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
