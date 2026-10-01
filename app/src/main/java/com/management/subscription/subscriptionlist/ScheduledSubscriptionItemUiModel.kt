package com.management.subscription.subscriptionlist

import com.management.subscription.services.ServiceIconModel

data class ScheduledSubscriptionItemUiModel(
    val id: String,
    val serviceIconModel: ServiceIconModel,
    val title: String,
    val subtitle: String,
    val amountLabel: String,
    /** 결제까지 남은 날. null이면 DueLabel을 숨긴다. */
    val dDay: Int?
)
