package com.management.subscription.subscriptionlist

import com.management.subscription.services.ServiceIconModel

data class ScheduledSubscriptionItemUiModel(
    val id: String,
    val serviceIconModel: ServiceIconModel,
    val title: String,
    val subtitle: String,
    val cycleLabel: String,
    val amountLabel: String,
    val trailingLabel: String
)
