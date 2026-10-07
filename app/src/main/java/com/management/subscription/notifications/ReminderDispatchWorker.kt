package com.management.subscription.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.management.subscription.analytics.Analytics
import com.management.subscription.analytics.AnalyticsEvent
import com.management.subscription.data.SettingsRepository
import com.management.subscription.data.SubscriptionRepository
import com.management.subscription.domain.SubscriptionScheduleCalculator
import java.time.LocalDate

class ReminderDispatchWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val settingsRepository = SettingsRepository.getInstance(applicationContext)
        val reminderScheduler = ReminderScheduler.getInstance(applicationContext)
        val settings = settingsRepository.getSettings()

        if (!settings.notificationsEnabled || !NotificationPermissionHelper.hasNotificationPermission(applicationContext)) {
            reminderScheduler.cancel()
            return Result.success()
        }

        val today = LocalDate.now()
        if (settings.lastDispatchDate == today) {
            reminderScheduler.sync()
            return Result.success()
        }

        val subscriptions = SubscriptionRepository.getInstance(applicationContext)
            .getSubscriptionsSnapshot()
        val schedules = SubscriptionScheduleCalculator.reminderSchedules(subscriptions, today)

        val hasDeliveredNotification = ReminderNotifier.notifySchedules(applicationContext, schedules)
        if (hasDeliveredNotification) {
            settingsRepository.updateLastDispatchDate(today)
            Analytics.log(AnalyticsEvent.NotificationShown(schedules.size))
        }

        reminderScheduler.sync()
        return Result.success()
    }
}
