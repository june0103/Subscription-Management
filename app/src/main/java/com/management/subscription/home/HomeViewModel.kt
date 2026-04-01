package com.management.subscription.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.management.subscription.data.BillingCycle
import com.management.subscription.data.SubscriptionRepository
import com.management.subscription.domain.SubscriptionScheduleCalculator
import com.management.subscription.util.DdayFormatter
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
            val currentMonth = YearMonth.from(today)
            val monthlySchedules =
                SubscriptionScheduleCalculator.subscriptionsForMonth(subscriptions, currentMonth, today)
            val upcomingSchedules =
                SubscriptionScheduleCalculator.upcomingSubscriptions(subscriptions, today)

            HomeUiState(
                hasSubscriptions = subscriptions.isNotEmpty(),
                dueThisWeekCount = upcomingSchedules.count { it.dDay in 0..7 },
                monthlyCount = subscriptions.count { it.billingCycle == BillingCycle.MONTHLY },
                annualCount = subscriptions.count { it.billingCycle == BillingCycle.ANNUAL },
                nextDueLabel = upcomingSchedules.firstOrNull()?.let { DdayFormatter.format(it.dDay) } ?: "--",
                currencyTotals = SubscriptionScheduleCalculator.currencyTotals(monthlySchedules),
                upcomingSchedules = upcomingSchedules.take(5)
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
        )

    companion object {
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
