package com.management.subscription.subscriptionlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.management.subscription.MainActivity
import com.management.subscription.R
import com.management.subscription.data.BillingCycle
import com.management.subscription.data.SubscriptionRepository
import com.management.subscription.databinding.FragmentSubscriptionCycleListBinding
import com.management.subscription.domain.SubscriptionScheduleCalculator
import com.management.subscription.ui.LineDividerDecoration
import com.management.subscription.util.SubscriptionFormatters
import kotlinx.coroutines.launch

class SubscriptionCycleListFragment : Fragment() {

    private var _binding: FragmentSubscriptionCycleListBinding? = null
    private val binding get() = checkNotNull(_binding)

    private val repository by lazy(LazyThreadSafetyMode.NONE) {
        SubscriptionRepository.getInstance(requireContext().applicationContext)
    }
    private val cycle by lazy(LazyThreadSafetyMode.NONE) {
        BillingCycle.valueOf(
            requireArguments().getString(SubscriptionCycleListArgs.KEY_BILLING_CYCLE)
                ?: BillingCycle.MONTHLY.name
        )
    }
    private val viewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProvider(
            this,
            SubscriptionCycleListViewModel.factory(repository, cycle)
        )[SubscriptionCycleListViewModel::class.java]
    }

    private val adapter = ScheduledSubscriptionAdapter { item ->
        (activity as? MainActivity)?.openEditor(item.id, entry = "list")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubscriptionCycleListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbarSubscriptionList.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
        binding.recyclerSubscriptions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SubscriptionCycleListFragment.adapter
            itemAnimator = null
            addItemDecoration(LineDividerDecoration(requireContext()))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::renderState)
            }
        }
    }

    override fun onDestroyView() {
        binding.recyclerSubscriptions.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private fun renderState(state: SubscriptionCycleListUiState) {
        binding.toolbarSubscriptionList.title = when (state.cycle) {
            BillingCycle.ANNUAL -> getString(R.string.subscription_list_title_annual)
            BillingCycle.MONTHLY -> getString(R.string.subscription_list_title_monthly)
        }
        val totals = SubscriptionScheduleCalculator.currencyTotals(state.schedules)
        binding.tvSubscriptionCount.text = if (state.schedules.isEmpty()) {
            getString(R.string.subscription_list_count_format, 0)
        } else {
            getString(
                when (state.cycle) {
                    BillingCycle.ANNUAL -> R.string.subscription_list_summary_annual_format
                    BillingCycle.MONTHLY -> R.string.subscription_list_summary_monthly_format
                },
                state.count,
                SubscriptionFormatters.totals(totals)
            )
        }
        binding.recyclerSubscriptions.isVisible = state.schedules.isNotEmpty()
        binding.cardEmptyState.isVisible = state.schedules.isEmpty()
        binding.tvEmptyTitle.text = when (state.cycle) {
            BillingCycle.ANNUAL -> getString(R.string.subscription_list_empty_annual)
            BillingCycle.MONTHLY -> getString(R.string.subscription_list_empty_monthly)
        }

        adapter.submitList(
            state.schedules.map { ScheduledSubscriptionAdapter.toUiModel(requireContext(), it) }
        )
    }
}

object SubscriptionCycleListArgs {
    const val KEY_BILLING_CYCLE = "billingCycle"
}
