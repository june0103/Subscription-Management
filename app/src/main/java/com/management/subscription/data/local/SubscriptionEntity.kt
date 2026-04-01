package com.management.subscription.data.local

import androidx.annotation.ColorRes
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.management.subscription.data.BillingCycle

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val amountMinor: Long,
    val currencyCode: String,
    val serviceKey: String?,
    val linkedPackageName: String?,
    val billingCycle: BillingCycle,
    val billingDay: Int,
    val annualMonth: Int?,
    val reminderDaysBefore: Int,
    @ColorRes val accentColorRes: Int,
    val createdAt: Long,
    val updatedAt: Long
)
