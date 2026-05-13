package com.example.stockscout.presentation.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.stockscout.R
import com.example.stockscout.databinding.FragmentDetailBinding
import com.example.stockscout.domain.model.Item
import com.example.stockscout.domain.model.PendingPick
import com.example.stockscout.domain.model.SyncStatus
import com.example.stockscout.presentation.viewmodel.DetailViewModel
import com.example.stockscout.utils.Resource
import com.example.stockscout.utils.gone
import com.example.stockscout.utils.visible
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ItemDetailFragment : Fragment() {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DetailViewModel by viewModels()
    private val args: ItemDetailFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        observeItem()
        observePickState()
        observePendingPicks()
        viewModel.resolveInput(args.input)
        viewModel.loadPendingPicks()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
    }

    private fun observeItem() {
        viewModel.item.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Loading -> {
                    binding.progressBar.visible()
                    binding.contentGroup.gone()
                    binding.tvNotFound.gone()
                }
                is Resource.Success -> {
                    binding.progressBar.gone()
                    val item = state.data
                    if (item == null) {
                        binding.tvNotFound.visible()
                        binding.contentGroup.gone()
                    } else {
                        binding.tvNotFound.gone()
                        binding.contentGroup.visible()
                        bindItem(item)
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.gone()
                    binding.tvNotFound.visible()
                    binding.tvNotFound.text = getString(R.string.item_not_found)
                    binding.contentGroup.gone()
                }
            }
        }
    }

    private fun bindItem(item: Item) {
        binding.tvItemCode.text = item.itemCode
        binding.tvItemName.text = item.name
        binding.tvUom.text = getString(R.string.uom_label, item.unitOfMeasure)
        binding.tvQty.text = getString(R.string.qty_label, item.onHandQuantity)

        binding.chipGroupAliases.removeAllViews()
        item.aliases.forEach { alias ->
            val chip = Chip(requireContext()).apply {
                text = "${alias.type.name}: ${alias.value}"
                isClickable = false
            }
            binding.chipGroupAliases.addView(chip)
        }

        updatePickButton(inStock = item.onHandQuantity > 0)

        binding.btnPick.setOnClickListener {
            // Disable immediately so a rapid second tap can't enqueue a second pick
            // before the use-case mutex grabs the lock. Re-enabled by observePickState.
            binding.btnPick.isEnabled = false
            viewModel.pick()
        }
    }

    private fun updatePickButton(inStock: Boolean) {
        if (inStock) {
            binding.btnPick.isEnabled = true
            binding.btnPick.alpha = 1.0f
            binding.btnPick.text = getString(R.string.pick_action)
        } else {
            binding.btnPick.isEnabled = false
            binding.btnPick.alpha = 0.5f
            binding.btnPick.text = getString(R.string.out_of_stock)
        }
    }

    private fun observePickState() {
        viewModel.pickState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Loading -> {
                    // Button stays disabled — already set by the click handler.
                }
                is Resource.Success -> {
                    // The fresh item LiveData will fire next and updatePickButton()
                    // will re-evaluate the enabled state from the new qty.
                    Toast.makeText(requireContext(), getString(R.string.pick_success), Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> {
                    // Pick was rejected — re-enable so user can try again with a different item.
                    // updatePickButton runs again on next item emission anyway.
                    val qty = (viewModel.item.value as? Resource.Success)?.data?.onHandQuantity ?: 0
                    updatePickButton(inStock = qty > 0)
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun observePendingPicks() {
        viewModel.pendingPicks.observe(viewLifecycleOwner) { picks ->
            val latestPick = picks.maxByOrNull { it.timestamp }
            if (latestPick != null) {
                binding.chipSyncStatus.visible()
                when (latestPick.status) {
                    SyncStatus.PENDING, SyncStatus.IN_PROGRESS -> {
                        binding.chipSyncStatus.text = getString(R.string.sync_pending)
                        binding.chipSyncStatus.setChipBackgroundColorResource(R.color.sync_pending)
                    }
                    SyncStatus.SYNCED -> {
                        binding.chipSyncStatus.text = getString(R.string.sync_synced)
                        binding.chipSyncStatus.setChipBackgroundColorResource(R.color.sync_synced)
                    }
                    SyncStatus.FAILED -> {
                        binding.chipSyncStatus.text = getString(R.string.sync_failed)
                        binding.chipSyncStatus.setChipBackgroundColorResource(R.color.sync_failed)
                    }
                }
            } else {
                binding.chipSyncStatus.gone()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
