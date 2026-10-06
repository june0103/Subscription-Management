package com.management.subscription

import android.app.Application
import com.management.subscription.analytics.Analytics
import com.management.subscription.analytics.FirebaseAnalyticsTracker

class SubscriptionApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Analytics.init(FirebaseAnalyticsTracker(this))
        // 매니페스트에서 수집은 꺼진 채로 시작하고, 여기서 빌드 값대로 켠다(릴리스는 항상 켬).
        Analytics.setCollectionEnabled(BuildConfig.ANALYTICS_ENABLED)
    }
}
