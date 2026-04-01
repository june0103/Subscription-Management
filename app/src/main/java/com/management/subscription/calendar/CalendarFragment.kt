package com.management.subscription.calendar

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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.management.subscription.MainActivity
import com.management.subscription.R
import com.management.subscription.data.BillingCycle
import com.management.subscription.data.CurrencyTotal
import com.management.subscription.data.ScheduledSubscription
import com.management.subscription.data.SubscriptionRepository
import com.management.subscription.databinding.FragmentCalendarBinding
import com.management.subscription.databinding.ItemCurrencyTotalBinding
import com.management.subscription.subscriptionlist.ScheduledSubscriptionAdapter
import com.management.subscription.subscriptionlist.ScheduledSubscriptionItemUiModel
import com.management.subscription.services.ServiceIconResolver
import com.management.subscription.util.DdayFormatter
import com.management.subscription.util.SubscriptionFormatters
import kotlinx.coroutines.launch
import java.time.LocalDate

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = checkNotNull(_binding)

    private val repository by lazy(LazyThreadSafetyMode.NONE) {
        SubscriptionRepository.getInstance(requireContext().applicationContext)
    }
    private val viewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProvider(this, CalendarViewModel.factory(repository))[CalendarViewModel::class.java]
    }

    private val dayAdapter = CalendarDayAdapter { item ->
        viewModel.selectDate(item.date)
    }
    private val selectedAdapter = ScheduledSubscriptionAdapter { item ->
        (activity as? MainActivity)?.openEditor(item.id)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerCalendarDays.apply {
            layoutManager = GridLayoutManager(requireContext(), 7)
            adapter = dayAdapter
            itemAnimator = null
        }
        binding.recyclerSelectedSubscriptions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = selectedAdapter
            itemAnimator = null
        }

        binding.buttonPrevMonth.setOnClickListener { viewModel.moveMonthBy(-1) }
        binding.buttonNextMonth.setOnClickListener { viewModel.moveMonthBy(1) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::renderCalendar) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val requestedDate = (activity as? MainActivity)?.consumePendingCalendarFocusDate()
        if (requestedDate != null) {
            viewModel.focusDate(requestedDate)
        }
    }

    override fun onDestroyView() {
        binding.recyclerCalendarDays.adapter = null
        binding.recyclerSelectedSubscriptions.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private fun renderCalendar(state: CalendarUiState) {
        binding.tvMonthTitle.text = SubscriptionFormatters.monthTitle(state.displayedMonth)
        binding.tvMonthSummary.text =
            getString(R.string.calendar_month_summary_format, state.monthSchedules.size)
        binding.tvSelectedDate.text = SubscriptionFormatters.fullDate(state.selectedDate)
        binding.tvSelectedMeta.text =
            getString(R.string.calendar_selected_meta_format, state.selectedSchedules.size)

        renderCurrencyTotals(
            container = binding.layoutMonthTotals,
            totals = state.monthCurrencyTotals
        )
        renderCurrencyTotals(
            container = binding.layoutSelectedTotals,
            totals = state.selectedCurrencyTotals
        )

        dayAdapter.submitList(state.dayItems)
        binding.cardEmptyState.isVisible = state.selectedSchedules.isEmpty()
        binding.recyclerSelectedSubscriptions.isVisible = state.selectedSchedules.isNotEmpty()
        selectedAdapter.submitList(state.selectedSchedules.map(::toUiModel))
    }

    private fun renderCurrencyTotals(
        container: LinearLayout,
        totals: List<CurrencyTotal>
    ) {
        container.removeAllViews()
        if (totals.isEmpty()) {
            val rowBinding = ItemCurrencyTotalBinding.inflate(layoutInflater, container, false)
            rowBinding.tvCurrencyCode.text = ""
            rowBinding.tvCurrencyAmount.text = getString(R.string.summary_empty_amount)
            rowBinding.tvCurrencyCode.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.home_text_secondary)
            )
            rowBinding.tvCurrencyAmount.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.home_text_primary)
            )
            container.addView(rowBinding.root)
            return
        }

        totals.forEach { total ->
            val rowBinding = ItemCurrencyTotalBinding.inflate(layoutInflater, container, false)
            rowBinding.tvCurrencyCode.text = total.currencyCode
            rowBinding.tvCurrencyAmount.text = total.formattedAmount
            rowBinding.tvCurrencyCode.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.home_text_secondary)
            )
            rowBinding.tvCurrencyAmount.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.home_text_primary)
            )
            container.addView(rowBinding.root)
        }
    }

    private fun toUiModel(schedule: ScheduledSubscription): ScheduledSubscriptionItemUiModel {
        return ScheduledSubscriptionItemUiModel(
            id = schedule.subscription.id,
            serviceIconModel = ServiceIconResolver.resolve(requireContext(), schedule.subscription),
            title = schedule.subscription.name,
            subtitle = getString(
                R.string.calendar_item_subtitle_format,
                getCycleLabel(schedule.subscription.billingCycle),
                getString(
                    R.string.editor_reminder_days_value_format,
                    schedule.subscription.reminderDaysBefore
                )
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
