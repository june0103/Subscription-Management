package com.management.subscription.domain

import com.management.subscription.data.CurrencyTotal
import com.management.subscription.data.ScheduledSubscription
import com.management.subscription.data.SubscriptionCategory

/** 한 카테고리의 이번 달 결제 합계. category가 null이면 카테고리를 고르지 않은 구독이다. */
data class CategorySpend(
    val category: SubscriptionCategory?,
    val count: Int,
    val totals: List<CurrencyTotal>,
    /** 이번 달 전체 중 이 카테고리의 비중(0..1). 막대 길이에만 쓴다. */
    val share: Float
)

object CategorySpendCalculator {

    /**
     * 원화와 달러를 한 막대에 놓기 위한 대략적인 환율.
     * 금액 표시는 통화별로 정확히 하고, 이 값은 막대 길이와 순서에만 쓴다.
     */
    private const val APPROX_KRW_PER_USD = 1_400L

    fun spendByCategory(schedules: List<ScheduledSubscription>): List<CategorySpend> {
        if (schedules.isEmpty()) return emptyList()
        val groups = schedules.groupBy { it.subscription.category }
        val weights = groups.mapValues { (_, items) -> items.sumOf { approxKrw(it) } }
        val totalWeight = weights.values.sum().coerceAtLeast(1L)

        return groups.map { (category, items) ->
            CategorySpend(
                category = category,
                count = items.size,
                totals = SubscriptionScheduleCalculator.currencyTotals(items),
                share = weights.getValue(category).toFloat() / totalWeight
            )
        }.sortedWith(
            // 미분류는 금액과 상관없이 맨 아래에 둔다.
            compareBy<CategorySpend> { it.category == null }
                .thenByDescending { weights.getValue(it.category) }
                .thenBy { it.category?.ordinal ?: Int.MAX_VALUE }
        )
    }

    private fun approxKrw(schedule: ScheduledSubscription): Long {
        val subscription = schedule.subscription
        return when (subscription.currencyCode) {
            "USD" -> subscription.amountMinor * APPROX_KRW_PER_USD / 100
            else -> subscription.amountMinor
        }
    }
}
