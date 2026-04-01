package com.management.subscription.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.management.subscription.MainActivity
import com.management.subscription.R
import com.management.subscription.data.BillingCycle
import com.management.subscription.data.CurrencyTotal
import com.management.subscription.data.ScheduledSubscription
import com.management.subscription.data.SubscriptionRepository
import com.management.subscription.databinding.FragmentHomeBinding
import com.management.subscription.databinding.ItemCurrencyTotalBinding
import com.management.subscription.domain.SubscriptionScheduleCalculator
import com.management.subscription.subscriptionlist.ScheduledSubscriptionAdapter
import com.management.subscription.subscriptionlist.ScheduledSubscriptionItemUiModel
import com.management.subscription.services.ServiceIconResolver
import com.management.subscription.util.DdayFormatter
import com.management.subscription.util.SubscriptionFormatters
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = checkNotNull(_binding)

    private val repository by lazy(LazyThreadSafetyMode.NONE) {
        SubscriptionRepository.getInstance(requireContext().applicationContext)
    }
    private val viewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProvider(this, HomeViewModel.factory(repository))[HomeViewModel::class.java]
    }

    private val subscriptionAdapter = ScheduledSubscriptionAdapter { item ->
        (activity as? MainActivity)?.openEditor(item.id)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerUpcoming.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = subscriptionAdapter
            itemAnimator = null
        }

        binding.fabAddSubscription.setOnClickListener {
            (activity as? MainActivity)?.openEditor()
        }
        binding.tvViewAll.setOnClickListener {
            (activity as? MainActivity)?.openCalendarTab(LocalDate.now())
        }
        binding.cardAnnualSubscriptions.setOnClickListener {
            (activity as? MainActivity)?.openSubscriptionList(BillingCycle.ANNUAL)
        }
        binding.cardMonthlySubscriptions.setOnClickListener {
            (activity as? MainActivity)?.openSubscriptionList(BillingCycle.MONTHLY)
        }

        val today = LocalDate.now()
        binding.tvMonthBadge.text = getString(R.string.home_month_badge_format, today.monthValue)
        binding.tvMonthlyLabel.text =
            getString(R.string.home_month_total_format, getString(R.string.this_month_label))

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::renderHome)
            }
        }
    }

    override fun onDestroyView() {
        binding.recyclerUpcoming.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private fun renderHome(state: HomeUiState) {
        binding.tvDueThisWeekValue.text =
            getString(R.string.home_due_this_week_value, state.dueThisWeekCount)
        binding.tvNextDueValue.text = state.nextDueLabel
        binding.tvAnnualCountValue.text = getString(R.string.home_count_format, state.annualCount)
        binding.tvMonthlyCountValue.text = getString(R.string.home_count_format, state.monthlyCount)

        renderCurrencyTotals(
            container = binding.layoutMonthlyTotals,
            totals = state.currencyTotals,
            amountColor = R.color.white,
            codeColor = R.color.home_sand,
            emptyTextColor = R.color.white
        )

        binding.recyclerUpcoming.isVisible = state.hasSubscriptions
        binding.cardHomeEmptyState.isVisible = !state.hasSubscriptions
        subscriptionAdapter.submitList(state.upcomingSchedules.map(::toUiModel))
    }

    private fun renderCurrencyTotals(
        container: LinearLayout,
        totals: List<CurrencyTotal>,
        amountColor: Int,
        codeColor: Int,
        emptyTextColor: Int
    ) {
        container.removeAllViews()
        if (totals.isEmpty()) {
            val rowBinding = ItemCurrencyTotalBinding.inflate(layoutInflater, container, false)
            rowBinding.tvCurrencyCode.text = ""
            rowBinding.tvCurrencyAmount.text = getString(R.string.summary_empty_amount)
            rowBinding.tvCurrencyAmount.setTextColor(
                ContextCompat.getColor(requireContext(), emptyTextColor)
            )
            container.addView(rowBinding.root)
            return
        }

        totals.forEach { total ->
            val rowBinding = ItemCurrencyTotalBinding.inflate(layoutInflater, container, false)
            rowBinding.tvCurrencyCode.text = total.currencyCode
            rowBinding.tvCurrencyAmount.text = total.formattedAmount
            rowBinding.tvCurrencyCode.setTextColor(ContextCompat.getColor(requireContext(), codeColor))
            rowBinding.tvCurrencyAmount.setTextColor(ContextCompat.getColor(requireContext(), amountColor))
            container.addView(rowBinding.root)
        }
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
            cycleLabel = getCycleLabel(schedule.subscription.billingCycle),
            amountLabel = SubscriptionFormatters.currency(
                schedule.subscription.amountMinor,
                schedule.subscription.currencyCode
            ),
            trailingLabel = DdayFormatter.format(schedule.dDay)
        )
    }

    private fun getCycleLabel(cycle: BillingCycle): String {
        return when (cycle) {
            BillingCycle.MONTHLY -> getString(R.string.home_cycle_monthly)
            BillingCycle.ANNUAL -> getString(R.string.home_cycle_annual)
        }
    }
}
