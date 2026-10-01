package com.management.subscription.data

import androidx.annotation.ColorRes

data class SubscriptionPreview(
    val id: String,
    val badge: String,
    val name: String,
    val amountMinor: Long,
    val currencyCode: String,
    val serviceKey: String?,
    val linkedPackageName: String?,
    val billingCycle: BillingCycle,
    val billingDay: Int,
    val annualMonth: Int? = null,
    val reminderDaysBefore: Int,
    @ColorRes val accentColorRes: Int,
    val category: SubscriptionCategory? = null,
    val paymentMethod: PaymentMethod? = null,
    val memo: String? = null
)
