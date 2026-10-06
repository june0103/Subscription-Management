package com.management.subscription.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.management.subscription.data.BillingCycle
import com.management.subscription.data.SubscriptionRepository
import com.management.subscription.domain.CategorySpendCalculator
import com.management.subscription.domain.SubscriptionScheduleCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

class HomeViewModel(
    repository: SubscriptionRepository
) : ViewModel() {

    private val today = LocalDate.now()

    val uiState = repository.observeSubscriptions()
        .map { subscriptions ->
            val monthlySchedules = SubscriptionScheduleCalculator.subscriptionsForMonth(
                subscriptions,
                YearMonth.from(today),
                today
            )
            val upcoming = SubscriptionScheduleCalculator.upcomingSubscriptions(subscriptions, today)
            val groups = upcoming.groupBy { it.paymentDate }
                .map { (date, schedules) ->
                    TimelineGroup(
                        date = date,
                        dDay = schedules.first().dDay,
                        schedules = schedules,
                        totals = SubscriptionScheduleCalculator.currencyTotals(schedules)
                    )
                }
            val todayGroup = groups.firstOrNull { it.dDay == 0 }
            val nextGroup = groups.firstOrNull { it.dDay > 0 }

            HomeUiState(
                today = today,
                hasSubscriptions = subscriptions.isNotEmpty(),
                todaySchedules = todayGroup?.schedules.orEmpty(),
                todayTotals = todayGroup?.totals.orEmpty(),
                nextSchedules = nextGroup?.schedules.orEmpty(),
                nextTotals = nextGroup?.totals.orEmpty(),
                dueThisWeekCount = upcoming.count { it.dDay in 0..7 },
                monthlyCount = subscriptions.count { it.billingCycle == BillingCycle.MONTHLY },
                annualCount = subscriptions.count { it.billingCycle == BillingCycle.ANNUAL },
                monthTotals = SubscriptionScheduleCalculator.currencyTotals(monthlySchedules),
                categorySpend = CategorySpendCalculator.spendByCategory(monthlySchedules),
                timeline = groups.take(TIMELINE_GROUP_COUNT)
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
        )

    companion object {
        private const val TIMELINE_GROUP_COUNT = 4

        fun factory(repository: SubscriptionRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(repository) as T
                }
            }
        }
    }
}
