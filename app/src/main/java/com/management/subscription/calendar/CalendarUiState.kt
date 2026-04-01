package com.management.subscription.calendar

import com.management.subscription.data.CurrencyTotal
import com.management.subscription.data.ScheduledSubscription
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val hasSubscriptions: Boolean = false,
    val displayedMonth: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val monthSchedules: List<ScheduledSubscription> = emptyList(),
    val selectedSchedules: List<ScheduledSubscription> = emptyList(),
    val monthCurrencyTotals: List<CurrencyTotal> = emptyList(),
    val selectedCurrencyTotals: List<CurrencyTotal> = emptyList(),
    val dayItems: List<CalendarDayUiModel> = emptyList()
)
