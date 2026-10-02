package com.management.subscription.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.perf.FirebasePerformance

/** 사용 이벤트를 보내는 곳. 화면 코드는 Firebase 대신 이 인터페이스만 안다. */
interface AnalyticsTracker {
    fun log(event: AnalyticsEvent)

    /** 사용 통계·비정상 종료·성능 수집을 한 번에 켜고 끈다. */
    fun setCollectionEnabled(enabled: Boolean)
}

/** 테스트용, 그리고 init 전에 불려도 죽지 않게 하는 기본값 */
object NoOpAnalyticsTracker : AnalyticsTracker {
    override fun log(event: AnalyticsEvent) = Unit
    override fun setCollectionEnabled(enabled: Boolean) = Unit
}

class FirebaseAnalyticsTracker(context: Context) : AnalyticsTracker {

    private val analytics = FirebaseAnalytics.getInstance(context.applicationContext)

    override fun log(event: AnalyticsEvent) {
        if (event is AnalyticsEvent.ScreenView) {
            analytics.logEvent(
                FirebaseAnalytics.Event.SCREEN_VIEW,
                Bundle().apply {
                    putString(FirebaseAnalytics.Param.SCREEN_NAME, event.screenName)
                    putString(FirebaseAnalytics.Param.SCREEN_CLASS, event.screenName)
                }
            )
            return
        }
        analytics.logEvent(event.name, event.params.toBundle())
    }

    override fun setCollectionEnabled(enabled: Boolean) {
        analytics.setAnalyticsCollectionEnabled(enabled)
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = enabled
        FirebasePerformance.getInstance().isPerformanceCollectionEnabled = enabled
        if (!enabled) {
            // 끄기 전에 쌓인 비정상 종료 보고서도 보내지 않는다.
            FirebaseCrashlytics.getInstance().deleteUnsentReports()
        }
    }

    private fun Map<String, Any>.toBundle(): Bundle {
        val bundle = Bundle()
        forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Long -> bundle.putLong(key, value)
                is Int -> bundle.putLong(key, value.toLong())
                is Double -> bundle.putDouble(key, value)
                is Boolean -> bundle.putString(key, value.toString())
                else -> bundle.putString(key, value.toString())
            }
        }
        return bundle
    }
}

/** 앱 어디서나 Analytics.log(...)로 부른다. SubscriptionApp에서 init 한다. */
object Analytics {

    @Volatile
    var tracker: AnalyticsTracker = NoOpAnalyticsTracker
        private set

    /**
     * 앱 시작 직후에는 사용자의 수집 설정을 아직 읽지 못했다. 그 사이 이벤트(첫 화면 조회 등)는
     * 여기에 잠깐 모았다가, 설정이 정해지면 보내거나 버린다.
     */
    private val pending = mutableListOf<AnalyticsEvent>()
    private var collectionDecided = false

    fun init(tracker: AnalyticsTracker) {
        this.tracker = tracker
    }

    fun log(event: AnalyticsEvent) {
        synchronized(pending) {
            if (!collectionDecided) {
                if (pending.size < MAX_PENDING) pending += event
                return
            }
        }
        send(event)
    }

    fun setCollectionEnabled(enabled: Boolean) {
        runCatching { tracker.setCollectionEnabled(enabled) }
            .onFailure { Log.w(TAG, "Failed to apply collection=$enabled", it) }
        val queued = synchronized(pending) {
            collectionDecided = true
            pending.toList().also { pending.clear() }
        }
        if (enabled) queued.forEach(::send)
    }

    @androidx.annotation.VisibleForTesting
    internal fun resetForTest(tracker: AnalyticsTracker) {
        synchronized(pending) {
            pending.clear()
            collectionDecided = false
        }
        this.tracker = tracker
    }

    private fun send(event: AnalyticsEvent) {
        runCatching { tracker.log(event) }
            .onFailure { Log.w(TAG, "Failed to log ${event.name}", it) }
    }

    /** Performance Monitoring 사용자 정의 구간. 콘솔에서 p50·p95 소요 시간을 본다. */
    inline fun <T> trace(name: String, block: () -> T): T {
        val trace = runCatching { FirebasePerformance.getInstance().newTrace(name).also { it.start() } }
            .getOrNull()
        try {
            return block()
        } finally {
            runCatching { trace?.stop() }
        }
    }

    private const val TAG = "Analytics"
    private const val MAX_PENDING = 50
}
