package com.management.subscription.subscriptionlist

import com.management.subscription.data.BillingCycle
import com.management.subscription.data.ScheduledSubscription

data class SubscriptionCycleListUiState(
    val cycle: BillingCycle = BillingCycle.MONTHLY,
    val schedules: List<ScheduledSubscription> = emptyList()
) {
    val count: Int
        get() = schedules.size
}
