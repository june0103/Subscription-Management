package com.management.subscription.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.management.subscription.data.SubscriptionRepository
import com.management.subscription.domain.SubscriptionScheduleCalculator
import com.management.subscription.util.SubscriptionFormatters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.min

class CalendarViewModel(
    repository: SubscriptionRepository
) : ViewModel() {

    private val displayedMonth = MutableStateFlow(YearMonth.now())
    private val selectedDate = MutableStateFlow(LocalDate.now())

    val uiState = combine(
        repository.observeSubscriptions(),
        displayedMonth,
        selectedDate
    ) { subscriptions, month, selected ->
        val today = LocalDate.now()
        val normalizedSelectedDate = if (YearMonth.from(selected) == month) {
            selected
        } else {
            month.atDay(min(selected.dayOfMonth, month.lengthOfMonth()))
        }

        val monthSchedules =
            SubscriptionScheduleCalculator.subscriptionsForMonth(subscriptions, month, today)
        val selectedSchedules =
            SubscriptionScheduleCalculator.subscriptionsForDate(subscriptions, normalizedSelectedDate, today)
        val visibleDates = buildVisibleDates(month)

        CalendarUiState(
            hasSubscriptions = subscriptions.isNotEmpty(),
            displayedMonth = month,
            selectedDate = normalizedSelectedDate,
            monthSchedules = monthSchedules,
            selectedSchedules = selectedSchedules,
            monthCurrencyTotals = SubscriptionScheduleCalculator.currencyTotals(monthSchedules),
            selectedCurrencyTotals = SubscriptionScheduleCalculator.currencyTotals(selectedSchedules),
            dayItems = visibleDates.map { date ->
                val dotColors = SubscriptionScheduleCalculator.subscriptionsForDate(
                    subscriptions = subscriptions,
                    date = date,
                    referenceDate = today
                ).map { it.subscription.accentColorRes }
                    .distinct()
                    .take(3)

                CalendarDayUiModel(
                    date = date,
                    dayLabel = date.dayOfMonth.toString(),
                    isInCurrentMonth = YearMonth.from(date) == month,
                    isToday = date == today,
                    isSelected = date == normalizedSelectedDate,
                    dotColors = dotColors,
                    contentDescription = SubscriptionFormatters.dateWithWeekday(date) +
                        if (dotColors.isEmpty()) "" else ", 결제 있음"
                )
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CalendarUiState()
    )

    fun moveMonthBy(offset: Long) {
        val targetMonth = displayedMonth.value.plusMonths(offset)
        displayedMonth.value = targetMonth
        selectedDate.value = targetMonth.atDay(min(selectedDate.value.dayOfMonth, targetMonth.lengthOfMonth()))
    }

    fun selectDate(date: LocalDate) {
        if (YearMonth.from(date) != displayedMonth.value) {
            displayedMonth.value = YearMonth.from(date)
        }
        selectedDate.value = date
    }

    fun focusDate(date: LocalDate) {
        displayedMonth.value = YearMonth.from(date)
        selectedDate.value = date
    }

    private fun buildVisibleDates(month: YearMonth): List<LocalDate> {
        val firstDay = month.atDay(1)
        val offset = if (firstDay.dayOfWeek == DayOfWeek.SUNDAY) 0 else firstDay.dayOfWeek.value
        val gridStart = firstDay.minusDays(offset.toLong())
        // 그 달에 필요한 주만 그린다(4~6주).
        val cellCount = ((offset + month.lengthOfMonth() + 6) / 7) * 7
        return List(cellCount) { index -> gridStart.plusDays(index.toLong()) }
    }

    companion object {
        fun factory(repository: SubscriptionRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CalendarViewModel(repository) as T
                }
            }
        }
    }
}
