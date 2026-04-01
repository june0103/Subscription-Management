package com.management.subscription.subscriptionlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.management.subscription.data.BillingCycle
import com.management.subscription.data.SubscriptionRepository
import com.management.subscription.domain.SubscriptionScheduleCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

class SubscriptionCycleListViewModel(
    repository: SubscriptionRepository,
    private val cycle: BillingCycle
) : ViewModel() {

    private val today = LocalDate.now()

    val uiState = repository.observeSubscriptions()
        .map { subscriptions ->
            val filtered = subscriptions.filter { it.billingCycle == cycle }
            SubscriptionCycleListUiState(
                cycle = cycle,
                schedules = SubscriptionScheduleCalculator.upcomingSubscriptions(filtered, today)
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SubscriptionCycleListUiState(cycle = cycle)
        )

    companion object {
        fun factory(
            repository: SubscriptionRepository,
            cycle: BillingCycle
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SubscriptionCycleListViewModel(repository, cycle) as T
                }
            }
        }
    }
}
