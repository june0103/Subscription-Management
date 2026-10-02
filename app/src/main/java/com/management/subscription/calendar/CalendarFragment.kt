package com.management.subscription.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.management.subscription.MainActivity
import com.management.subscription.R
import com.management.subscription.data.SubscriptionRepository
import com.management.subscription.databinding.FragmentCalendarBinding
import com.management.subscription.subscriptionlist.ScheduledSubscriptionAdapter
import com.management.subscription.ui.DueViews
import com.management.subscription.ui.LineDividerDecoration
import com.management.subscription.util.SubscriptionFormatters
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

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
        (activity as? MainActivity)?.openEditor(item.id, entry = "calendar")
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
            addItemDecoration(LineDividerDecoration(requireContext()))
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
        binding.tvMonthSummary.text = if (state.monthSchedules.isEmpty()) {
            getString(R.string.calendar_month_empty)
        } else {
            getString(
                R.string.count_with_total_format,
                state.monthSchedules.size,
                SubscriptionFormatters.totals(state.monthCurrencyTotals)
            )
        }

        val dDay = ChronoUnit.DAYS.between(LocalDate.now(), state.selectedDate).toInt()
        DueViews.bindDateTile(binding.selectedDateTile, state.selectedDate, dDay)
        binding.tvSelectedDate.text = SubscriptionFormatters.dateWithWeekday(state.selectedDate)
        binding.tvSelectedDue.isVisible = state.selectedSchedules.isNotEmpty()
        if (state.selectedSchedules.isNotEmpty()) DueViews.bindDueLabel(binding.tvSelectedDue, dDay)
        binding.tvSelectedMeta.text = if (state.selectedSchedules.isEmpty()) {
            getString(R.string.calendar_empty_title)
        } else {
            getString(
                R.string.count_with_total_format,
                state.selectedSchedules.size,
                SubscriptionFormatters.totals(state.selectedCurrencyTotals)
            )
        }

        dayAdapter.submitList(state.dayItems)
        binding.cardEmptyState.isVisible = state.selectedSchedules.isEmpty()
        binding.recyclerSelectedSubscriptions.isVisible = state.selectedSchedules.isNotEmpty()
        selectedAdapter.submitList(
            state.selectedSchedules.map {
                ScheduledSubscriptionAdapter.toUiModel(requireContext(), it, showDue = false)
            }
        )
    }
}
