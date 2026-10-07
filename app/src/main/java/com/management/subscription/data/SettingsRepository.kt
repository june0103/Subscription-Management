package com.management.subscription.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.settingsDataStore by preferencesDataStore(name = "subscription_settings")

class SettingsRepository private constructor(
    private val appContext: Context
) {

    fun observeSettings(): Flow<UserSettings> {
        return appContext.settingsDataStore.data.map(::toUserSettings)
    }

    suspend fun getSettings(): UserSettings {
        return observeSettings().first()
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        appContext.settingsDataStore.edit { preferences ->
            preferences[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun markNotificationPromptShown() {
        appContext.settingsDataStore.edit { preferences ->
            preferences[KEY_NOTIFICATION_PROMPT_SHOWN] = true
        }
    }

    suspend fun updateReminderTime(hour: Int, minute: Int) {
        appContext.settingsDataStore.edit { preferences ->
            preferences[KEY_REMINDER_HOUR] = hour
            preferences[KEY_REMINDER_MINUTE] = minute
        }
    }

    suspend fun updateLastDispatchDate(date: LocalDate?) {
        appContext.settingsDataStore.edit { preferences ->
            if (date == null) {
                preferences.remove(KEY_LAST_DISPATCH_DATE)
            } else {
                preferences[KEY_LAST_DISPATCH_DATE] = date.toString()
            }
        }
    }

    private fun toUserSettings(preferences: Preferences): UserSettings {
        val lastDispatchDate = preferences[KEY_LAST_DISPATCH_DATE]?.let { stored ->
            runCatching { LocalDate.parse(stored) }.getOrNull()
        }

        return UserSettings(
            notificationsEnabled = preferences[KEY_NOTIFICATIONS_ENABLED] ?: false,
            reminderHour = preferences[KEY_REMINDER_HOUR] ?: 9,
            reminderMinute = preferences[KEY_REMINDER_MINUTE] ?: 0,
            lastDispatchDate = lastDispatchDate,
            notificationPromptShown = preferences[KEY_NOTIFICATION_PROMPT_SHOWN] ?: false
        )
    }

    companion object {
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val KEY_REMINDER_HOUR = intPreferencesKey("reminder_hour")
        private val KEY_REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        private val KEY_LAST_DISPATCH_DATE = stringPreferencesKey("last_dispatch_date")
        private val KEY_NOTIFICATION_PROMPT_SHOWN = booleanPreferencesKey("notification_prompt_shown")

        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
