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
        // DB에 저장된 리소스 ID는 빌드마다 바뀔 수 있어 쓰지 않고, id로 매번 다시 고른다.
        accentColorRes = SubscriptionAccentPalette.pick(id)
    )
}
