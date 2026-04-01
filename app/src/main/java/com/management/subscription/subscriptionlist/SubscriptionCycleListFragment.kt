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
import com.management.subscription.data.ScheduledSubscription
import com.management.subscription.data.SubscriptionRepository
import com.management.subscription.databinding.FragmentSubscriptionCycleListBinding
import com.management.subscription.services.ServiceIconResolver
import com.management.subscription.util.DdayFormatter
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
        (activity as? MainActivity)?.openEditor(item.id)
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
        binding.tvSubscriptionCount.text =
            getString(R.string.subscription_list_count_format, state.count)
        binding.recyclerSubscriptions.isVisible = state.schedules.isNotEmpty()
        binding.cardEmptyState.isVisible = state.schedules.isEmpty()
        binding.tvEmptyTitle.text = when (state.cycle) {
            BillingCycle.ANNUAL -> getString(R.string.subscription_list_empty_annual)
            BillingCycle.MONTHLY -> getString(R.string.subscription_list_empty_monthly)
        }

        adapter.submitList(state.schedules.map(::toUiModel))
    }

    private fun toUiModel(schedule: ScheduledSubscription): ScheduledSubscriptionItemUiModel {
        return ScheduledSubscriptionItemUiModel(
            id = schedule.subscription.id,
            serviceIconModel = ServiceIconResolver.resolve(requireContext(), schedule.subscription),
            title = schedule.subscription.name,
            subtitle = getString(
                R.string.home_payment_date_format,
                SubscriptionFormatters.shortDate(schedule.paymentDate)
            ),
            cycleLabel = when (schedule.subscription.billingCycle) {
                BillingCycle.MONTHLY -> getString(R.string.home_cycle_monthly)
                BillingCycle.ANNUAL -> getString(R.string.home_cycle_annual)
            },
            amountLabel = SubscriptionFormatters.currency(
                schedule.subscription.amountMinor,
                schedule.subscription.currencyCode
            ),
            trailingLabel = DdayFormatter.format(schedule.dDay)
        )
    }
}

object SubscriptionCycleListArgs {
    const val KEY_BILLING_CYCLE = "billingCycle"
}
