package com.management.subscription.util

import kotlin.math.abs

object DdayFormatter {
    fun format(dDay: Int): String {
        return when {
            dDay == 0 -> "D-Day"
            dDay > 0 -> "D-$dDay"
            else -> "D+${abs(dDay)}"
        }
    }
}
