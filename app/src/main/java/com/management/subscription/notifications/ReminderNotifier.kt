package com.management.subscription.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.management.subscription.MainActivity
import com.management.subscription.R
import com.management.subscription.data.ScheduledSubscription
import com.management.subscription.util.DueFormatter
import com.management.subscription.util.SubscriptionFormatters

object ReminderNotifier {

    const val CHANNEL_ID = "subscription_reminders"
    private const val CHANNEL_NAME = "subscription-reminders"
    private const val NOTIFICATION_ID = 1001
    const val EXTRA_NOTIFICATION_COUNT = "notification_count"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    fun notifySchedules(
        context: Context,
        schedules: List<ScheduledSubscription>
    ): Boolean {
        if (schedules.isEmpty() || !NotificationPermissionHelper.hasNotificationPermission(context)) {
            return false
        }

        ensureChannel(context)

        val notificationManager = NotificationManagerCompat.from(context)
        val notification = if (schedules.size == 1) {
            buildSingleNotification(context, schedules.first())
        } else {
            buildSummaryNotification(context, schedules)
        }

        // 위에서 권한을 확인했지만, 그 직후 사용자가 설정에서 알림을 끄면 여기서 예외가 난다.
        // 그때는 이번 알림만 건너뛴다.
        return try {
            notificationManager.notify(CHANNEL_NAME, NOTIFICATION_ID, notification)
            true
        } catch (e: SecurityException) {
            false
        }
    }

    private fun buildSingleNotification(
        context: Context,
        schedule: ScheduledSubscription
    ) = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification_subscription)
        .setContentTitle(
            context.getString(
                R.string.notification_single_title,
                schedule.subscription.name,
                DueFormatter.label(schedule.dDay)
            )
        )
        .setContentText(
            context.getString(
                R.string.notification_single_body,
                SubscriptionFormatters.currency(
                    schedule.subscription.amountMinor,
                    schedule.subscription.currencyCode
                ),
                SubscriptionFormatters.dateWithWeekday(schedule.paymentDate)
            )
        )
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .setContentIntent(mainPendingIntent(context, count = 1))
        .build()

    private fun buildSummaryNotification(
        context: Context,
        schedules: List<ScheduledSubscription>
    ): android.app.Notification {
        val inboxStyle = NotificationCompat.InboxStyle()
            .setSummaryText(context.getString(R.string.notification_summary_body))

        schedules.forEach { schedule ->
            inboxStyle.addLine(
                context.getString(
                    R.string.notification_summary_line,
                    schedule.subscription.name,
                    SubscriptionFormatters.currency(
                        schedule.subscription.amountMinor,
                        schedule.subscription.currencyCode
                    ),
                    DueFormatter.label(schedule.dDay)
                )
            )
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_subscription)
            .setContentTitle(
                context.getString(
                    R.string.notification_summary_title,
                    schedules.size
                )
            )
            .setContentText(context.getString(R.string.notification_summary_body))
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent(context, count = schedules.size))
            .build()
    }

    private fun mainPendingIntent(context: Context, count: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_COUNT, count)
        }
        return PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
