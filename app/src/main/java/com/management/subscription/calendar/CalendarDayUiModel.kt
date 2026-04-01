package com.management.subscription.calendar

import androidx.annotation.ColorRes
import java.time.LocalDate

data class CalendarDayUiModel(
    val date: LocalDate,
    val dayLabel: String,
    val isInCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    @ColorRes val dotColors: List<Int>
)
