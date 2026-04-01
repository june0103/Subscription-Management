package com.management.subscription.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.management.subscription.data.SettingsRepository
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class ReminderScheduler private constructor(
    private val appContext: Context,
    private val settingsRepository: SettingsRepository
) {

    suspend fun sync() {
        val settings = settingsRepository.getSettings()
        if (!settings.notificationsEnabled || !NotificationPermissionHelper.hasNotificationPermission(appContext)) {
            cancel()
            return
        }

        ReminderNotifier.ensureChannel(appContext)

        val initialDelayMillis = computeInitialDelayMillis(
            hour = settings.reminderHour,
            minute = settings.reminderMinute
        )

        val request = OneTimeWorkRequestBuilder<ReminderDispatchWorker>()
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(appContext).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel() {
        WorkManager.getInstance(appContext).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    private fun computeInitialDelayMillis(hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        val todayReminderTime = now.toLocalDate().atTime(LocalTime.of(hour, minute))
        val target = if (now.isAfter(todayReminderTime)) {
            todayReminderTime.plusDays(1)
        } else {
            todayReminderTime
        }
        return Duration.between(now, target).toMillis().coerceAtLeast(0L)
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "subscription_reminder_dispatch"

        @Volatile
        private var instance: ReminderScheduler? = null

        fun getInstance(context: Context): ReminderScheduler {
            return instance ?: synchronized(this) {
                instance ?: ReminderScheduler(
                    context.applicationContext,
                    SettingsRepository.getInstance(context.applicationContext)
                ).also { instance = it }
            }
        }
    }
}
