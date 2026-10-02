package com.management.subscription.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsBufferTest {

    private class FakeTracker : AnalyticsTracker {
        val logged = mutableListOf<AnalyticsEvent>()
        var enabled: Boolean? = null
        override fun log(event: AnalyticsEvent) {
            logged += event
        }
        override fun setCollectionEnabled(enabled: Boolean) {
            this.enabled = enabled
        }
    }

    @Test
    fun eventsBeforeDecision_areSentOnceCollectionIsEnabled() {
        val fake = FakeTracker()
        Analytics.resetForTest(fake)

        Analytics.log(AnalyticsEvent.ScreenView("home"))
        assertTrue(fake.logged.isEmpty())

        Analytics.setCollectionEnabled(true)
        assertEquals(listOf<AnalyticsEvent>(AnalyticsEvent.ScreenView("home")), fake.logged)

        Analytics.log(AnalyticsEvent.DiscoveryOpen)
        assertEquals(2, fake.logged.size)
    }

    @Test
    fun eventsBeforeDecision_areDroppedWhenUserOptedOut() {
        val fake = FakeTracker()
        Analytics.resetForTest(fake)

        Analytics.log(AnalyticsEvent.ScreenView("home"))
        Analytics.setCollectionEnabled(false)

        assertEquals(false, fake.enabled)
        assertTrue(fake.logged.isEmpty())
    }
}
