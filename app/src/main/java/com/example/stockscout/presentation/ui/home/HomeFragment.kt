package com.example.stockscout.presentation.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.stockscout.R
import com.example.stockscout.databinding.FragmentHomeBinding
import com.example.stockscout.presentation.adapter.ItemAdapter
import com.example.stockscout.presentation.viewmodel.HomeViewModel
import com.example.stockscout.utils.Resource
import com.example.stockscout.utils.gone
import com.example.stockscout.utils.visible
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var adapter: ItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearch()
        setupSwipeRefresh()
        setupFab()
        observeItems()
        observePendingCount()
        observeRefreshing()
        handleScanResult()
    }

    private fun setupRecyclerView() {
        adapter = ItemAdapter { item -> navigateToDetail(item.itemCode) }
        binding.recyclerView.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            viewModel.onSearchQuery(text?.toString() ?: "")
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
    }

    private fun setupFab() {
        binding.fabScan.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_scanner)
        }
    }

    private fun observeItems() {
        viewModel.items.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Loading -> {
                    // Initial load only — pull-to-refresh uses isRefreshing
                }
                is Resource.Success -> {
                    val items = state.data
                    if (items.isEmpty()) {
                        binding.recyclerView.gone()
                        binding.tvEmpty.visible()
                        binding.tvEmpty.text = getString(R.string.no_items)
                    } else {
                        binding.tvEmpty.gone()
                        binding.recyclerView.visible()
                        adapter.submitList(items)
                    }
                }
                is Resource.Error -> {
                    binding.recyclerView.gone()
                    binding.tvEmpty.visible()
                    binding.tvEmpty.text = state.message
                }
            }
        }
    }

    private fun observePendingCount() {
        viewModel.pendingCount.observe(viewLifecycleOwner) { count ->
            if (count > 0) {
                binding.chipPending.visible()
                binding.chipPending.text = getString(R.string.pending_count, count)
            } else {
                binding.chipPending.gone()
            }
        }
    }

    private fun observeRefreshing() {
        viewModel.isRefreshing.observe(viewLifecycleOwner) { isRefreshing ->
            binding.swipeRefresh.isRefreshing = isRefreshing
        }
    }

    /** Receives barcode scan result from ScannerFragment via SafeArgs. */
    private fun handleScanResult() {
        val savedState = findNavController().currentBackStackEntry?.savedStateHandle
        savedState?.getLiveData<String>("scanned_barcode")?.observe(viewLifecycleOwner) { barcode ->
            if (!barcode.isNullOrBlank()) {
                navigateToDetail(barcode)
                savedState.remove<String>("scanned_barcode")
            }
        }
    }

    private fun navigateToDetail(input: String) {
        val action = HomeFragmentDirections.actionHomeToDetail(input)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
