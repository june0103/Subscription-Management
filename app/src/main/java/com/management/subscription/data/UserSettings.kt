package com.management.subscription.data

import java.time.LocalDate

data class UserSettings(
    val notificationsEnabled: Boolean = false,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val lastDispatchDate: LocalDate? = null,
    /** 저장 직후 "결제 전에 알려 드릴까요?" 안내를 이미 보여 줬는지. 한 번만 묻는다. */
    val notificationPromptShown: Boolean = false
)
