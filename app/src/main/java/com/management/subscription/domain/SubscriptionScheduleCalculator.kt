package com.management.subscription.domain

import com.management.subscription.data.BillingCycle
import com.management.subscription.data.CurrencyTotal
import com.management.subscription.data.ScheduledSubscription
import com.management.subscription.data.SubscriptionPreview
import com.management.subscription.util.SubscriptionFormatters
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.min

object SubscriptionScheduleCalculator {

    fun nextPaymentDate(subscription: SubscriptionPreview, fromDate: LocalDate): LocalDate {
        return when (subscription.billingCycle) {
            BillingCycle.MONTHLY -> {
                val currentMonthDate = safeDate(YearMonth.from(fromDate), subscription.billingDay)
                if (!currentMonthDate.isBefore(fromDate)) currentMonthDate
                else safeDate(YearMonth.from(fromDate).plusMonths(1), subscription.billingDay)
            }

            BillingCycle.ANNUAL -> {
                val annualMonth = checkNotNull(subscription.annualMonth)
                val thisYearDate = safeDate(YearMonth.of(fromDate.year, annualMonth), subscription.billingDay)
                if (!thisYearDate.isBefore(fromDate)) thisYearDate
                else safeDate(YearMonth.of(fromDate.year + 1, annualMonth), subscription.billingDay)
            }
        }
    }

    fun upcomingSubscriptions(
        subscriptions: List<SubscriptionPreview>,
        fromDate: LocalDate
    ): List<ScheduledSubscription> {
        return subscriptions.map { subscription ->
            val paymentDate = nextPaymentDate(subscription, fromDate)
            ScheduledSubscription(
                subscription = subscription,
                paymentDate = paymentDate,
                dDay = ChronoUnit.DAYS.between(fromDate, paymentDate).toInt()
            )
        }.sortedWith(compareBy<ScheduledSubscription> { it.paymentDate }.thenBy { it.subscription.name })
    }

    fun subscriptionsForMonth(
        subscriptions: List<SubscriptionPreview>,
        month: YearMonth,
        referenceDate: LocalDate = LocalDate.now()
    ): List<ScheduledSubscription> {
        return subscriptions.mapNotNull { subscription ->
            scheduledForMonth(subscription, month)?.let { paymentDate ->
                ScheduledSubscription(
                    subscription = subscription,
                    paymentDate = paymentDate,
                    dDay = ChronoUnit.DAYS.between(referenceDate, paymentDate).toInt()
                )
            }
        }.sortedWith(compareBy<ScheduledSubscription> { it.paymentDate }.thenBy { it.subscription.name })
    }

    fun subscriptionsForDate(
        subscriptions: List<SubscriptionPreview>,
        date: LocalDate,
        referenceDate: LocalDate = LocalDate.now()
    ): List<ScheduledSubscription> {
        return subscriptionsForMonth(subscriptions, YearMonth.from(date), referenceDate)
            .filter { it.paymentDate == date }
    }

    fun reminderSchedules(
        subscriptions: List<SubscriptionPreview>,
        today: LocalDate
    ): List<ScheduledSubscription> {
        return subscriptions.mapNotNull { subscription ->
            val paymentDate = nextPaymentDate(subscription, today)
            val reminderDate = paymentDate.minusDays(subscription.reminderDaysBefore.toLong())
            if (reminderDate == today) {
                ScheduledSubscription(
                    subscription = subscription,
                    paymentDate = paymentDate,
                    dDay = ChronoUnit.DAYS.between(today, paymentDate).toInt()
                )
            } else {
                null
            }
        }.sortedWith(compareBy<ScheduledSubscription> { it.paymentDate }.thenBy { it.subscription.name })
    }

    fun currencyTotals(schedules: List<ScheduledSubscription>): List<CurrencyTotal> {
        return schedules.groupBy { it.subscription.currencyCode }
            .map { (currencyCode, items) ->
                val total = items.sumOf { it.subscription.amountMinor }
                CurrencyTotal(
                    currencyCode = currencyCode,
                    amountMinor = total,
                    formattedAmount = SubscriptionFormatters.currency(total, currencyCode)
                )
            }
            .sortedBy { currencySortOrder(it.currencyCode) }
    }

    private fun currencySortOrder(currencyCode: String): Int {
        return when (currencyCode) {
            "KRW" -> 0
            "USD" -> 1
            else -> 9
        }
    }

    private fun scheduledForMonth(subscription: SubscriptionPreview, month: YearMonth): LocalDate? {
        return when (subscription.billingCycle) {
            BillingCycle.MONTHLY -> safeDate(month, subscription.billingDay)
            BillingCycle.ANNUAL -> {
                if (subscription.annualMonth == month.monthValue) safeDate(month, subscription.billingDay)
                else null
            }
        }
    }

    private fun safeDate(month: YearMonth, dayOfMonth: Int): LocalDate {
        return month.atDay(min(dayOfMonth, month.lengthOfMonth()))
    }
}
