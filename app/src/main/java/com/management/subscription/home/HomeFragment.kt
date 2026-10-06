package com.management.subscription.home

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import android.text.Spanned
import android.text.style.ForegroundColorSpan
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
import com.management.subscription.data.SubscriptionRepository
import com.management.subscription.databinding.FragmentHomeBinding
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

    private val timelineAdapter = TimelineAdapter { subscriptionId ->
        (activity as? MainActivity)?.openEditor(subscriptionId, entry = "home")
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
            adapter = timelineAdapter
            itemAnimator = null
        }

        binding.fabAddSubscription.setOnClickListener {
            (activity as? MainActivity)?.openEditor(entry = "fab")
        }
        binding.buttonEmptyAdd.setOnClickListener {
            (activity as? MainActivity)?.openEditor(entry = "empty_state")
        }

        val openCalendar = View.OnClickListener {
            (activity as? MainActivity)?.openCalendarTab(LocalDate.now())
        }
        binding.tvViewAll.setOnClickListener(openCalendar)
        binding.cardTodayBanner.setOnClickListener(openCalendar)
        binding.cardAnnualSubscriptions.setOnClickListener {
            (activity as? MainActivity)?.openSubscriptionList(BillingCycle.ANNUAL)
        }
        binding.cardMonthlySubscriptions.setOnClickListener {
            (activity as? MainActivity)?.openSubscriptionList(BillingCycle.MONTHLY)
        }

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
        binding.tvTodayDate.text = SubscriptionFormatters.headerDate(state.today)
        renderHeadline(state)
        renderTodayBanner(state)

        binding.tvMonthlyLabel.text =
            getString(R.string.home_month_total_format, state.today.monthValue)
        renderHeroAmount(state.monthTotals)
        binding.tvMonthlyCountValue.text = getString(R.string.home_count_format, state.monthlyCount)
        binding.tvAnnualCountValue.text = getString(R.string.home_count_format, state.annualCount)
        binding.tvDueThisWeekValue.text =
            getString(R.string.home_due_this_week_value, state.dueThisWeekCount)

        binding.recyclerUpcoming.isVisible = state.hasSubscriptions
        binding.cardHomeEmptyState.isVisible = !state.hasSubscriptions
        timelineAdapter.submitList(state.timeline)
    }

    private fun renderHeadline(state: HomeUiState) {
        when {
            !state.hasSubscriptions -> {
                binding.tvHeadline.text = getString(R.string.home_headline_empty)
                binding.tvHeadlineSub.text = getString(R.string.home_headline_empty_sub)
            }

            state.todaySchedules.isNotEmpty() -> {
                val count = getString(R.string.home_count_format, state.todaySchedules.size)
                val sentence = getString(R.string.home_headline_today_format, count)
                val start = sentence.indexOf(count)
                binding.tvHeadline.text = SpannableStringBuilder(sentence).apply {
                    if (start >= 0) {
                        setSpan(
                            ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.due_today_ink)),
                            start,
                            start + count.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }
                binding.tvHeadlineSub.text = getString(
                    R.string.home_headline_sub_format,
                    joinNames(state.todaySchedules.map { it.subscription.name }),
                    SubscriptionFormatters.totals(state.todayTotals)
                )
            }

            else -> {
                val next = state.nextSchedules.firstOrNull()
                if (next == null) {
                    binding.tvHeadline.text = getString(R.string.home_headline_none)
                    binding.tvHeadlineSub.text = ""
                } else {
                    val ending = if (next.dDay == 1) {
                        getString(R.string.home_headline_tomorrow)
                    } else {
                        getString(R.string.home_headline_days_format, next.dDay)
                    }
                    binding.tvHeadline.text = getString(R.string.home_headline_next_format, ending)
                    binding.tvHeadlineSub.text = getString(
                        R.string.home_headline_next_sub_format,
                        SubscriptionFormatters.dateWithWeekday(next.paymentDate),
                        joinNames(state.nextSchedules.map { it.subscription.name }),
                        SubscriptionFormatters.totals(state.nextTotals)
                    )
                }
            }
        }
        binding.tvHeadlineSub.isVisible = binding.tvHeadlineSub.text.isNotEmpty()
    }

    private fun renderTodayBanner(state: HomeUiState) {
        binding.cardTodayBanner.isVisible = state.todaySchedules.isNotEmpty()
        if (state.todaySchedules.isEmpty()) return
        binding.tvTodayTitle.text = getString(
            R.string.home_today_banner_format,
            SubscriptionFormatters.totals(state.todayTotals)
        )
        binding.tvTodayNames.text = state.todaySchedules.joinToString(" · ") { it.subscription.name }
    }

    /** 원화 합계를 크게, 다른 통화는 아래 줄에 "+ $30.99"로. 서로 더하지 않는다. */
    private fun renderHeroAmount(totals: List<CurrencyTotal>) {
        val primary = totals.firstOrNull()
        binding.tvHeroAmount.text = primary?.formattedAmount ?: getString(R.string.home_hero_zero)
        val rest = totals.drop(1)
        binding.tvHeroSub.isVisible = rest.isNotEmpty()
        binding.tvHeroSub.text = rest.joinToString(" ") { "+ " + it.formattedAmount }
    }

    private fun joinNames(names: List<String>): String {
        return when (names.size) {
            0 -> ""
            1, 2 -> names.joinToString(", ")
            else -> getString(R.string.names_and_more_format, names.first(), names.size - 1)
        }
    }
}
