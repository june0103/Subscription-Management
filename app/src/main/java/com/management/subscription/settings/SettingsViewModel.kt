package com.management.subscription.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.management.subscription.analytics.Analytics
import com.management.subscription.analytics.AnalyticsEvent
import com.management.subscription.data.SettingsRepository
import com.management.subscription.notifications.ReminderScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    val uiState = settingsRepository.observeSettings()
        .map { settings ->
            SettingsUiState(
                notificationsEnabled = settings.notificationsEnabled,
                reminderHour = settings.reminderHour,
                reminderMinute = settings.reminderMinute
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState()
        )

    fun setNotificationsEnabled(enabled: Boolean, canSchedule: Boolean) {
        Analytics.log(AnalyticsEvent.NotificationsToggled(enabled))
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
            if (enabled && canSchedule) {
                reminderScheduler.sync()
            } else {
                reminderScheduler.cancel()
            }
        }
    }

    fun updateReminderTime(hour: Int, minute: Int, canSchedule: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateReminderTime(hour, minute)
            if (canSchedule) {
                reminderScheduler.sync()
            }
        }
    }

    fun syncScheduling(canSchedule: Boolean) {
        viewModelScope.launch {
            if (canSchedule) {
                reminderScheduler.sync()
            } else {
                reminderScheduler.cancel()
            }
        }
    }

    companion object {
        fun factory(
            settingsRepository: SettingsRepository,
            reminderScheduler: ReminderScheduler
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(
                        settingsRepository,
                        reminderScheduler
                    ) as T
                }
            }
        }
    }
}
