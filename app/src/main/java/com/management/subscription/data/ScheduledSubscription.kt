package com.management.subscription.data

import java.time.LocalDate

data class ScheduledSubscription(
    val subscription: SubscriptionPreview,
    val paymentDate: LocalDate,
    val dDay: Int
)
