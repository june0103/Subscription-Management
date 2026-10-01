package com.management.subscription.ui

import android.content.Context
import com.management.subscription.R
import com.management.subscription.data.BillingCycle
import com.management.subscription.data.SubscriptionPreview
import com.management.subscription.util.DueFormatter

/** 목록 메타 줄 문구: "매월 15일", "매년 3월 2일 · 3일 전 알림" */
object SubscriptionLabels {

    fun cycle(context: Context, subscription: SubscriptionPreview): String {
        return cycle(context, subscription.billingCycle, subscription.annualMonth ?: 1, subscription.billingDay)
    }

    fun cycle(context: Context, cycle: BillingCycle, annualMonth: Int, billingDay: Int): String {
        return when (cycle) {
            BillingCycle.MONTHLY -> context.getString(R.string.cycle_monthly_format, billingDay)
            BillingCycle.ANNUAL -> context.getString(R.string.cycle_annual_format, annualMonth, billingDay)
        }
    }

    fun cycleWithReminder(context: Context, subscription: SubscriptionPreview): String {
        return context.getString(
            R.string.cycle_with_reminder_format,
            cycle(context, subscription),
            DueFormatter.reminder(subscription.reminderDaysBefore)
        )
    }
}
