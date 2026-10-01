package com.management.subscription.util

import androidx.annotation.ColorRes
import com.management.subscription.R

/** 결제까지 남은 날을 한국어 문구와 색 단계로 바꾼다. 디자인 시스템 DueLabel 규칙. */
enum class DueTone(
    @ColorRes val backgroundRes: Int,
    @ColorRes val textRes: Int
) {
    TODAY(R.color.due_today, R.color.on_due_today),
    SOON(R.color.due_soon_soft, R.color.due_soon_ink),
    LATER(R.color.due_later_soft, R.color.due_later_ink)
}

object DueFormatter {

    fun label(dDay: Int): String {
        return when {
            dDay < 0 -> "지난 결제"
            dDay == 0 -> "오늘 결제"
            dDay == 1 -> "내일 결제"
            else -> "${dDay}일 후"
        }
    }

    /** 문장 속에 들어가는 형태: "오늘", "내일", "3일 후" */
    fun relative(dDay: Int): String {
        return when {
            dDay <= 0 -> "오늘"
            dDay == 1 -> "내일"
            else -> "${dDay}일 후"
        }
    }

    fun tone(dDay: Int): DueTone {
        return when {
            dDay == 0 -> DueTone.TODAY
            dDay in 1..3 -> DueTone.SOON
            else -> DueTone.LATER
        }
    }

    /** 알림 시점 표기: 0 → "당일", n → "n일 전" */
    fun reminder(daysBefore: Int): String {
        return if (daysBefore <= 0) "당일" else "${daysBefore}일 전"
    }
}
