package com.management.subscription.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.management.subscription.databinding.FragmentSubscriptionDiscoveryBinding
import com.management.subscription.services.ServiceSuggestionRepository
import kotlinx.coroutines.launch

class SubscriptionDiscoveryFragment : Fragment() {

    private var _binding: FragmentSubscriptionDiscoveryBinding? = null
    private val binding get() = checkNotNull(_binding)

    private val suggestionRepository by lazy(LazyThreadSafetyMode.NONE) {
        ServiceSuggestionRepository.getInstance(requireContext().applicationContext)
    }
    private val viewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProvider(
            this,
            SubscriptionDiscoveryViewModel.factory(suggestionRepository)
        )[SubscriptionDiscoveryViewModel::class.java]
    }
    private val suggestionAdapter by lazy(LazyThreadSafetyMode.NONE) {
        ServiceSuggestionAdapter(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubscriptionDiscoveryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbarDiscovery.title = "\uC790\uB3D9 \uCC3E\uAE30"
        binding.toolbarDiscovery.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.listSuggestions.adapter = suggestionAdapter
        binding.listSuggestions.setOnItemClickListener { _, _, position, _ ->
            val suggestion = suggestionAdapter.getItemOrNull(position) ?: return@setOnItemClickListener
            parentFragmentManager.setFragmentResult(
                SubscriptionEditorFragment.REQUEST_KEY_DISCOVERY_RESULT,
                bundleOf(
                    SubscriptionEditorFragment.RESULT_DISPLAY_NAME to suggestion.displayName,
                    SubscriptionEditorFragment.RESULT_SERVICE_KEY to suggestion.serviceKey,
                    SubscriptionEditorFragment.RESULT_LINKED_PACKAGE to suggestion.linkedPackageName
                )
            )
            findNavController().popBackStack()
        }
        binding.editSearch.doAfterTextChanged { text ->
            viewModel.onQueryChanged(text?.toString().orEmpty())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::renderState)
            }
        }
    }

    override fun onDestroyView() {
        binding.listSuggestions.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private fun renderState(state: SubscriptionDiscoveryUiState) {
        suggestionAdapter.submitList(state.suggestions)
        binding.progressDiscovery.isVisible = state.isLoading
        binding.listSuggestions.isVisible = !state.isLoading && state.suggestions.isNotEmpty()
        binding.layoutEmpty.isVisible = !state.isLoading && state.suggestions.isEmpty()
    }
}
