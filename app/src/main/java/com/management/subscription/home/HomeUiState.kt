package com.management.subscription.home

import com.management.subscription.data.CurrencyTotal
import com.management.subscription.data.ScheduledSubscription

data class HomeUiState(
    val hasSubscriptions: Boolean = false,
    val dueThisWeekCount: Int = 0,
    val monthlyCount: Int = 0,
    val annualCount: Int = 0,
    val nextDueLabel: String = "--",
    val currencyTotals: List<CurrencyTotal> = emptyList(),
    val upcomingSchedules: List<ScheduledSubscription> = emptyList()
)
