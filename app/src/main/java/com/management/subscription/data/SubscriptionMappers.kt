package com.management.subscription.data

import com.management.subscription.data.local.SubscriptionEntity

fun SubscriptionEntity.toPreview(): SubscriptionPreview {
    return SubscriptionPreview(
        id = id,
        badge = name.trim().split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }.joinToString("")
            .take(2)
            .ifBlank { name.take(1).uppercase() },
        name = name,
        amountMinor = amountMinor,
        currencyCode = currencyCode,
        serviceKey = serviceKey,
        linkedPackageName = linkedPackageName,
        billingCycle = billingCycle,
        billingDay = billingDay,
        annualMonth = annualMonth,
        reminderDaysBefore = reminderDaysBefore,
        accentColorRes = accentColorRes
    )
}
