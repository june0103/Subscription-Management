package com.management.subscription.home

import com.management.subscription.data.CurrencyTotal
import com.management.subscription.data.ScheduledSubscription
import java.time.LocalDate

data class HomeUiState(
    val today: LocalDate = LocalDate.now(),
    val hasSubscriptions: Boolean = false,
    /** 오늘 결제되는 구독. 비어 있으면 오늘 배너를 숨긴다. */
    val todaySchedules: List<ScheduledSubscription> = emptyList(),
    val todayTotals: List<CurrencyTotal> = emptyList(),
    /** 오늘 이후 가장 가까운 결제일의 구독들 */
    val nextSchedules: List<ScheduledSubscription> = emptyList(),
    val nextTotals: List<CurrencyTotal> = emptyList(),
    val dueThisWeekCount: Int = 0,
    val monthlyCount: Int = 0,
    val annualCount: Int = 0,
    val monthTotals: List<CurrencyTotal> = emptyList(),
    val timeline: List<TimelineGroup> = emptyList()
)

/** 같은 날짜에 결제되는 구독 묶음 */
data class TimelineGroup(
    val date: LocalDate,
    val dDay: Int,
    val schedules: List<ScheduledSubscription>,
    val totals: List<CurrencyTotal>
)
