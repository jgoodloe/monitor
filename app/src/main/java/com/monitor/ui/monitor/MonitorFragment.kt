package com.monitor.ui.monitor

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.monitor.R
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

        monitorViewModel.items.observe(viewLifecycleOwner) { items ->
            Log.i("MonitorFragment", "Received ${items?.size ?: 0} items from ViewModel")
            items?.forEachIndexed { index, item ->
                Log.d("MonitorFragment", "Item $index: ${item.javaClass.simpleName}")
            }
            // Submit a new list instance to ensure diffing runs
            groupedAdapter.submitList(items?.toList())
            // Request layout in case the RecyclerView needs to redraw
            binding.recyclerViewMonitor.post { groupedAdapter.notifyDataSetChanged() }
        }

        monitorViewModel.lastTestTime.observe(viewLifecycleOwner) { testTime ->
            binding.textLastTestTime.text = "Last test: $testTime"
        }

        monitorViewModel.startMonitoring()

        return root
    }

    private fun setupRecyclerView() {
        monitorAdapter = MonitorAdapter { status ->
            monitorViewModel.retestItem(status.name)
            binding.swipeRefreshLayout.isRefreshing = true
        }

        groupedAdapter = GroupedMonitorAdapter(
            onItemClick = { itemName ->
                monitorViewModel.retestItem(itemName)
                binding.swipeRefreshLayout.isRefreshing = true
            },
            onDnsStatusClick = { hostname ->
                Log.d("MonitorFragment", "DNS detail clicked for: $hostname")
                val args = Bundle().apply { putString("hostname", hostname) }
                findNavController().navigate(R.id.nav_dns_detail, args)
            },
            onUrlStatusClick = { urlString ->
                Log.d("MonitorFragment", "URL detail clicked for: $urlString")
                val args = Bundle().apply { putString("urlString", urlString) }
                findNavController().navigate(R.id.nav_url_detail, args)
            },
            onCrlStatusClick = { crlUrl ->
                Log.d("MonitorFragment", "CRL detail clicked for: $crlUrl")
                val args = Bundle().apply { putString("crlUrl", crlUrl) }
                findNavController().navigate(R.id.nav_crl_detail, args)
            }
        )

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
