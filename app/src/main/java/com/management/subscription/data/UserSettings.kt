package com.management.subscription.data

import java.time.LocalDate

data class UserSettings(
    val notificationsEnabled: Boolean = false,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val lastDispatchDate: LocalDate? = null,
    /** 사용 통계 보내기. null이면 아직 고르지 않은 것으로 보고 빌드 기본값을 쓴다. */
    val analyticsEnabled: Boolean? = null
)
