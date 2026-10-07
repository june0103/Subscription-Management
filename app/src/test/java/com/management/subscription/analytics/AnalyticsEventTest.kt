package com.management.subscription.analytics

import com.management.subscription.data.BillingCycle
import com.management.subscription.data.PaymentMethod
import com.management.subscription.data.SubscriptionCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsEventTest {

    @Test
    fun savedEvent_sendsCatalogKeyAndBucketsOnly() {
        val event = AnalyticsEvent.SubscriptionSaved(
            isEdit = false,
            source = "discovery",
            serviceKey = "netflix",
            category = SubscriptionCategory.VIDEO,
            paymentMethod = PaymentMethod.KAKAO_PAY,
            cycle = BillingCycle.MONTHLY,
            reminderDays = 10,
            currencyCode = "KRW",
            hasMemo = true
        )

        assertEquals("subscription_saved", event.name)
        assertEquals("netflix", event.params["service"])
        assertEquals("video", event.params["category"])
        assertEquals("kakao_pay", event.params["payment_method"])
        assertEquals(10L, event.params["reminder_days"])
        assertEquals("true", event.params["has_memo"])
    }

    @Test
    fun savedEvent_neverContainsUserTypedValues() {
        val event = AnalyticsEvent.SubscriptionSaved(
            isEdit = true,
            source = "manual",
            serviceKey = null,
            category = null,
            paymentMethod = null,
            cycle = BillingCycle.ANNUAL,
            reminderDays = 0,
            currencyCode = "USD",
            hasMemo = false
        )

        assertEquals("custom", event.params["service"])
        assertEquals("none", event.params["category"])
        listOf("name", "service_name", "amount", "memo", "package", "package_name").forEach { forbidden ->
            assertFalse("must not send $forbidden", forbidden in event.params.keys)
        }
        // 메모는 있고 없음만 보낸다.
        assertTrue(event.params["has_memo"] in setOf("true", "false"))
    }

    @Test
    fun allEvents_fitFirebaseLimits() {
        val events = listOf(
            AnalyticsEvent.SubscriptionAddStart("fab"),
            AnalyticsEvent.SubscriptionOpen("calendar"),
            AnalyticsEvent.DiscoveryOpen,
            AnalyticsEvent.DiscoveryPick(installed = true, serviceKey = "youtube_premium"),
            AnalyticsEvent.SubscriptionSaveError("amount"),
            AnalyticsEvent.SubscriptionDeleted,
            AnalyticsEvent.ReminderCustomOpen,
            AnalyticsEvent.NotificationOpen(2),
            AnalyticsEvent.NotificationsToggled(true)
        )
        events.forEach { event ->
            assertTrue(event.name, event.name.length <= 40 && event.name.matches(Regex("[a-z_]+")))
            assertTrue(event.name, event.params.size <= 25)
            event.params.forEach { (key, value) ->
                assertTrue(key, key.length <= 40)
                if (value is String) assertTrue(key, value.length <= 100)
            }
        }
    }

    @Test
    fun notificationAndNavigationEvents_haveStableNamesAndParams() {
        assertEquals("notification_shown", AnalyticsEvent.NotificationShown(2).name)
        assertEquals(2L, AnalyticsEvent.NotificationShown(2).params["count"])

        val permission = AnalyticsEvent.NotificationPermission(granted = false, source = "after_save")
        assertEquals("notification_permission", permission.name)
        assertEquals("false", permission.params["granted"])
        assertEquals("after_save", permission.params["source"])

        assertEquals("accept", AnalyticsEvent.NotificationPrompt("accept").params["action"])
        assertEquals("today_banner", AnalyticsEvent.CalendarOpen("today_banner").params["entry"])

        // 알림 시각은 시만 보낸다(분 단위 습관까지는 모으지 않는다).
        val time = AnalyticsEvent.ReminderTimeChanged(21)
        assertEquals("reminder_time_changed", time.name)
        assertEquals(mapOf<String, Any>("hour" to 21L), time.params)
    }
}
