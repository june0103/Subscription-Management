package com.management.subscription.domain

import com.management.subscription.R
import com.management.subscription.data.BillingCycle
import com.management.subscription.data.ScheduledSubscription
import com.management.subscription.data.SubscriptionCategory
import com.management.subscription.data.SubscriptionPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CategorySpendCalculatorTest {

    @Test
    fun emptySchedules_giveNoRows() {
        assertTrue(CategorySpendCalculator.spendByCategory(emptyList()).isEmpty())
    }

    @Test
    fun groupsByCategory_sortsByAmount_keepsCurrenciesApart() {
        val rows = CategorySpendCalculator.spendByCategory(
            listOf(
                schedule("1", 17_000, "KRW", SubscriptionCategory.VIDEO),
                schedule("2", 14_900, "KRW", SubscriptionCategory.VIDEO),
                schedule("3", 2_000, "USD", SubscriptionCategory.AI),
                schedule("4", 2_000, "USD", SubscriptionCategory.AI),
                schedule("5", 7_890, "KRW", SubscriptionCategory.LIFE)
            )
        )

        // AI $40 ≈ 56,000원 > 영상 31,900원 > 멤버십 7,890원
        assertEquals(
            listOf(SubscriptionCategory.AI, SubscriptionCategory.VIDEO, SubscriptionCategory.LIFE),
            rows.map { it.category }
        )
        assertEquals(2, rows[0].count)
        assertEquals(listOf("USD"), rows[0].totals.map { it.currencyCode })
        assertEquals(4_000L, rows[0].totals.single().amountMinor)
        assertEquals(31_900L, rows[1].totals.single().amountMinor)
        assertEquals(1f, rows.map { it.share }.sum(), 0.001f)
    }

    @Test
    fun uncategorized_isAlwaysLast() {
        val rows = CategorySpendCalculator.spendByCategory(
            listOf(
                schedule("1", 50_000, "KRW", null),
                schedule("2", 4_900, "KRW", SubscriptionCategory.MUSIC)
            )
        )

        assertEquals(listOf(SubscriptionCategory.MUSIC, null), rows.map { it.category })
    }

    @Test
    fun mixedCurrenciesInOneCategory_listKrwFirst() {
        val rows = CategorySpendCalculator.spendByCategory(
            listOf(
                schedule("1", 1_099, "USD", SubscriptionCategory.MUSIC),
                schedule("2", 10_900, "KRW", SubscriptionCategory.MUSIC)
            )
        )

        assertEquals(listOf("KRW", "USD"), rows.single().totals.map { it.currencyCode })
    }

    private fun schedule(
        id: String,
        amountMinor: Long,
        currencyCode: String,
        category: SubscriptionCategory?
    ) = ScheduledSubscription(
        subscription = SubscriptionPreview(
            id = id,
            badge = "S",
            name = "Service $id",
            amountMinor = amountMinor,
            currencyCode = currencyCode,
            serviceKey = null,
            linkedPackageName = null,
            billingCycle = BillingCycle.MONTHLY,
            billingDay = 1,
            reminderDaysBefore = 1,
            accentColorRes = R.color.cat_etc,
            category = category
        ),
        paymentDate = LocalDate.of(2026, 10, 1),
        dDay = 0
    )
}
