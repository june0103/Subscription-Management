package com.management.subscription.settings

data class SettingsUiState(
    val notificationsEnabled: Boolean = false,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val analyticsEnabled: Boolean = false
)
