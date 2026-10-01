package com.management.subscription.data

import com.management.subscription.data.local.SubscriptionEntity
import com.management.subscription.services.SubscriptionServiceCatalog

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
        // 카테고리가 있으면 그 색, 없으면 id로 고른 색. 이니셜 배지·캘린더 점에 쓴다.
        accentColorRes = resolvedCategory()?.colorRes
            ?: SubscriptionAccentPalette.pick(id),
        category = resolvedCategory(),
        paymentMethod = PaymentMethod.fromStored(paymentMethod),
        memo = memo?.takeIf { it.isNotBlank() }
    )
}

/** 저장된 카테고리, 없으면(v3 이전에 등록한 구독 등) 카탈로그 서비스의 기본 카테고리 */
private fun SubscriptionEntity.resolvedCategory(): SubscriptionCategory? {
    return SubscriptionCategory.fromStored(category)
        ?: SubscriptionServiceCatalog.defaultCategory(serviceKey)
}
