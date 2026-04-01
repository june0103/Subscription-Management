package com.management.subscription.data

import java.time.LocalDate

data class UserSettings(
    val notificationsEnabled: Boolean = false,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val lastDispatchDate: LocalDate? = null
)
