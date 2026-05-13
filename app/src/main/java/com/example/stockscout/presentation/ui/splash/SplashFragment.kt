package com.example.stockscout.presentation.ui.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.stockscout.R
import com.example.stockscout.databinding.FragmentSplashBinding
import com.example.stockscout.presentation.viewmodel.SplashViewModel
import com.example.stockscout.utils.Resource
import com.example.stockscout.utils.gone
import com.example.stockscout.utils.visible
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SplashViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeSync()
        viewModel.syncItems()
    }

    private fun observeSync() {
        viewModel.syncState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Loading -> {
                    binding.progressBar.visible()
                    binding.tvStatus.text = getString(R.string.syncing)
                }
                is Resource.Success -> navigateToHome()
                is Resource.Error -> {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.sync_error, state.message),
                        Toast.LENGTH_SHORT
                    ).show()
                    navigateToHome()
                }
            }
        }
    }

    private fun navigateToHome() {
        findNavController().navigate(R.id.action_splash_to_home)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
