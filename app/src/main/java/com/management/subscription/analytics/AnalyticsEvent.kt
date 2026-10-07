package com.management.subscription.analytics

import com.management.subscription.data.BillingCycle
import com.management.subscription.data.PaymentMethod
import com.management.subscription.data.SubscriptionCategory

/**
 * 앱이 보내는 사용 이벤트 전체 목록. 이벤트 이름과 파라미터는 이 파일에서만 정의한다.
 *
 * 보내지 않는 것: 설치된 앱 목록, 사용자가 직접 입력한 서비스명, 메모 내용, 정확한 금액.
 * 서비스는 카탈로그에 있는 키(netflix 등)만 보내고, 직접 입력한 서비스는 "custom"으로 묶는다.
 * Firebase 제한: 이벤트·파라미터 이름 40자, 문자열 값 100자, 이벤트당 파라미터 25개.
 */
sealed class AnalyticsEvent(
    val name: String,
    val params: Map<String, Any> = emptyMap()
) {

    /** 화면 진입. Firebase의 screen_view로 보낸다. */
    data class ScreenView(val screenName: String) : AnalyticsEvent("screen_view")

    /** 구독 추가 시작. entry: fab / empty_state */
    data class SubscriptionAddStart(val entry: String) :
        AnalyticsEvent("subscription_add_start", mapOf("entry" to entry))

    /** 기존 구독 열기. entry: home / calendar / list */
    data class SubscriptionOpen(val entry: String) :
        AnalyticsEvent("subscription_open", mapOf("entry" to entry))

    data object DiscoveryOpen : AnalyticsEvent("discovery_open")

    data class DiscoveryPick(val installed: Boolean, val serviceKey: String?) :
        AnalyticsEvent(
            "discovery_pick",
            mapOf("installed" to installed.asParam(), "service" to serviceParam(serviceKey))
        )

    /** source: discovery / suggestion / manual / existing */
    data class SubscriptionSaved(
        val isEdit: Boolean,
        val source: String,
        val serviceKey: String?,
        val category: SubscriptionCategory?,
        val paymentMethod: PaymentMethod?,
        val cycle: BillingCycle,
        val reminderDays: Int,
        val currencyCode: String,
        val hasMemo: Boolean
    ) : AnalyticsEvent(
        "subscription_saved",
        mapOf(
            "is_edit" to isEdit.asParam(),
            "source" to source,
            "service" to serviceParam(serviceKey),
            "category" to (category?.name?.lowercase() ?: "none"),
            "payment_method" to (paymentMethod?.name?.lowercase() ?: "none"),
            "cycle" to cycle.name.lowercase(),
            "reminder_days" to reminderDays.toLong(),
            // "currency"는 Analytics 예약 매개변수라 맞춤 측정기준으로 쓸 수 없다.
            "currency_code" to currencyCode,
            "has_memo" to hasMemo.asParam()
        )
    )

    /** 저장 실패. field: name / amount */
    data class SubscriptionSaveError(val field: String) :
        AnalyticsEvent("subscription_save_error", mapOf("field" to field))

    data object SubscriptionDeleted : AnalyticsEvent("subscription_deleted")

    data object ReminderCustomOpen : AnalyticsEvent("reminder_custom_open")

    /** 결제 알림을 눌러 앱을 연 경우. count: 알림에 담긴 결제 건수 */
    data class NotificationOpen(val count: Int) :
        AnalyticsEvent("notification_open", mapOf("count" to count.toLong()))

    data class NotificationsToggled(val enabled: Boolean) :
        AnalyticsEvent("notifications_toggled", mapOf("enabled" to enabled.asParam()))

    /** 결제 알림을 실제로 띄운 경우. notification_open과 나눠 열람률을 본다. count: 알림에 담긴 결제 건수 */
    data class NotificationShown(val count: Int) :
        AnalyticsEvent("notification_shown", mapOf("count" to count.toLong()))

    /** 시스템 알림 권한 요청 결과. source: settings / after_save */
    data class NotificationPermission(val granted: Boolean, val source: String) :
        AnalyticsEvent(
            "notification_permission",
            mapOf("granted" to granted.asParam(), "source" to source)
        )

    /** 첫 저장 뒤 "결제 전에 알려 드릴까요?" 안내에 대한 선택. action: accept / later */
    data class NotificationPrompt(val action: String) :
        AnalyticsEvent("notification_prompt", mapOf("action" to action))

    /** 캘린더 탭으로 들어온 경로. entry: today_banner / view_all / tab */
    data class CalendarOpen(val entry: String) :
        AnalyticsEvent("calendar_open", mapOf("entry" to entry))

    /** 설정에서 알림 시각을 바꾼 경우. 분은 보내지 않고 시(0~23)만 보낸다. */
    data class ReminderTimeChanged(val hour: Int) :
        AnalyticsEvent("reminder_time_changed", mapOf("hour" to hour.toLong()))

    private companion object {
        fun Boolean.asParam(): String = if (this) "true" else "false"

        fun serviceParam(serviceKey: String?): String = serviceKey ?: "custom"
    }
}
