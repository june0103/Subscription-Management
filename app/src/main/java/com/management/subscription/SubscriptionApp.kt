package com.management.subscription

import android.app.Application
import com.management.subscription.analytics.Analytics
import com.management.subscription.analytics.FirebaseAnalyticsTracker
import com.management.subscription.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SubscriptionApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        Analytics.init(FirebaseAnalyticsTracker(this))

        // 매니페스트에서 수집은 꺼진 채로 시작한다. 사용자의 선택(없으면 빌드 기본값)을 적용한다.
        appScope.launch {
            val settings = SettingsRepository.getInstance(this@SubscriptionApp).getSettings()
            Analytics.setCollectionEnabled(
                settings.analyticsEnabled ?: BuildConfig.ANALYTICS_DEFAULT_ENABLED
            )
        }
    }
}
