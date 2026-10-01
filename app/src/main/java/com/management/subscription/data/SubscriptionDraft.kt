package com.management.subscription.data

data class SubscriptionDraft(
    val name: String,
    val amountMinor: Long,
    val currencyCode: String,
    val serviceKey: String?,
    val linkedPackageName: String?,
    val billingCycle: BillingCycle,
    val billingDay: Int,
    val annualMonth: Int?,
    val reminderDaysBefore: Int,
    val category: SubscriptionCategory? = null,
    val paymentMethod: PaymentMethod? = null,
    val memo: String? = null
)
