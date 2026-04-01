package com.management.subscription.data

import android.content.Context
import com.management.subscription.data.local.SubscriptionDao
import com.management.subscription.data.local.SubscriptionDatabase
import com.management.subscription.data.local.SubscriptionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class SubscriptionRepository(
    private val subscriptionDao: SubscriptionDao
) {

    fun observeSubscriptions(): Flow<List<SubscriptionPreview>> {
        return subscriptionDao.observeAll()
            .map { entities -> entities.map(SubscriptionEntity::toPreview) }
    }

    suspend fun getSubscription(subscriptionId: String): SubscriptionPreview? {
        return subscriptionDao.getById(subscriptionId)?.toPreview()
    }

    suspend fun getSubscriptionsSnapshot(): List<SubscriptionPreview> {
        return subscriptionDao.getAll().map(SubscriptionEntity::toPreview)
    }

    suspend fun createSubscription(draft: SubscriptionDraft) {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        subscriptionDao.insert(
            SubscriptionEntity(
                id = id,
                name = draft.name,
                amountMinor = draft.amountMinor,
                currencyCode = draft.currencyCode,
                serviceKey = draft.serviceKey,
                linkedPackageName = draft.linkedPackageName,
                billingCycle = draft.billingCycle,
                billingDay = draft.billingDay,
                annualMonth = draft.annualMonth,
                reminderDaysBefore = draft.reminderDaysBefore,
                accentColorRes = SubscriptionAccentPalette.pick(id),
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun updateSubscription(subscriptionId: String, draft: SubscriptionDraft) {
        val current = subscriptionDao.getById(subscriptionId) ?: return
        subscriptionDao.update(
            current.copy(
                name = draft.name,
                amountMinor = draft.amountMinor,
                currencyCode = draft.currencyCode,
                serviceKey = draft.serviceKey,
                linkedPackageName = draft.linkedPackageName,
                billingCycle = draft.billingCycle,
                billingDay = draft.billingDay,
                annualMonth = draft.annualMonth,
                reminderDaysBefore = draft.reminderDaysBefore,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteSubscription(subscriptionId: String) {
        subscriptionDao.deleteById(subscriptionId)
    }

    companion object {
        @Volatile
        private var instance: SubscriptionRepository? = null

        fun getInstance(context: Context): SubscriptionRepository {
            return instance ?: synchronized(this) {
                instance ?: SubscriptionRepository(
                    SubscriptionDatabase.getInstance(context).subscriptionDao()
                ).also { instance = it }
            }
        }
    }
}
