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
        return nextPaymentDate(
            cycle = subscription.billingCycle,
            billingDay = subscription.billingDay,
            annualMonth = subscription.annualMonth,
            fromDate = fromDate
        )
    }

    fun nextPaymentDate(
        cycle: BillingCycle,
        billingDay: Int,
        annualMonth: Int?,
        fromDate: LocalDate
    ): LocalDate {
        return when (cycle) {
            BillingCycle.MONTHLY -> {
                val currentMonthDate = safeDate(YearMonth.from(fromDate), billingDay)
                if (!currentMonthDate.isBefore(fromDate)) currentMonthDate
                else safeDate(YearMonth.from(fromDate).plusMonths(1), billingDay)
            }

            BillingCycle.ANNUAL -> {
                val month = checkNotNull(annualMonth)
                val thisYearDate = safeDate(YearMonth.of(fromDate.year, month), billingDay)
                if (!thisYearDate.isBefore(fromDate)) thisYearDate
                else safeDate(YearMonth.of(fromDate.year + 1, month), billingDay)
            }
        }
    }

    /**
     * 알림이 실제로 나가는 날. 오늘 이후의 결제 회차 중 (결제일 - n일)이 오늘 이후인 첫 회차 기준.
     * 등록 화면 미리보기에 쓴다.
     */
    fun nextReminderDate(
        cycle: BillingCycle,
        billingDay: Int,
        annualMonth: Int?,
        reminderDaysBefore: Int,
        today: LocalDate
    ): Pair<LocalDate, LocalDate> {
        var payment = nextPaymentDate(cycle, billingDay, annualMonth, today)
        var reminder = payment.minusDays(reminderDaysBefore.toLong())
        while (reminder.isBefore(today)) {
            payment = nextPaymentDate(cycle, billingDay, annualMonth, payment.plusDays(1))
            reminder = payment.minusDays(reminderDaysBefore.toLong())
        }
        return payment to reminder
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
            // n일 전 알림: 오늘로부터 n일 뒤가 결제일이면 오늘 알린다. 월간 구독에 20일 전처럼
            // 결제 주기와 비슷하게 긴 값을 넣어도 다음 회차가 아니라 그다음 회차까지 계산된다.
            val paymentDate = today.plusDays(subscription.reminderDaysBefore.coerceAtLeast(0).toLong())
            if (nextPaymentDate(subscription, paymentDate) == paymentDate) {
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
