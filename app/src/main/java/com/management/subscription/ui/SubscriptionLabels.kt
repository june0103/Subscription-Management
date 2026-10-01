package com.management.subscription.ui

import android.content.Context
import android.content.res.ColorStateList
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.management.subscription.R
import com.management.subscription.data.BillingCycle
import com.management.subscription.data.SubscriptionCategory
import com.management.subscription.data.SubscriptionPreview
import com.management.subscription.util.DueFormatter

/** 목록 메타 줄 문구: "매월 15일", "매년 3월 2일 · 3일 전 알림" */
object SubscriptionLabels {

    fun cycle(context: Context, subscription: SubscriptionPreview): String {
        return cycle(context, subscription.billingCycle, subscription.annualMonth ?: 1, subscription.billingDay)
    }

    fun cycle(context: Context, cycle: BillingCycle, annualMonth: Int, billingDay: Int): String {
        return when (cycle) {
            BillingCycle.MONTHLY -> context.getString(R.string.cycle_monthly_format, billingDay)
            BillingCycle.ANNUAL -> context.getString(R.string.cycle_annual_format, annualMonth, billingDay)
        }
    }

    /** 카테고리 · 결제수단. 둘 다 없으면 null */
    fun categoryAndPayment(subscription: SubscriptionPreview): String? {
        return listOfNotNull(subscription.category?.label, subscription.paymentMethod?.label)
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" · ")
    }

    /** 타임라인 행: 날짜는 타일이 말하므로 "영상 · 카카오페이", 없으면 "매월 15일 · 3일 전 알림" */
    fun timelineMeta(context: Context, subscription: SubscriptionPreview): String {
        return categoryAndPayment(subscription) ?: cycleWithReminder(context, subscription)
    }

    /** 목록 행: "매월 15일 · 영상 · 카카오페이", 없으면 "매월 15일 · 3일 전 알림" */
    fun listMeta(context: Context, subscription: SubscriptionPreview): String {
        val extra = categoryAndPayment(subscription) ?: return cycleWithReminder(context, subscription)
        return cycle(context, subscription) + " · " + extra
    }

    /** 메타 줄 앞에 카테고리 색 점(8dp)을 붙인다. 카테고리가 없으면 점도 없다. */
    fun bindMeta(view: TextView, text: String, category: SubscriptionCategory?) {
        view.text = text
        if (category == null) {
            view.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null)
            return
        }
        val dot = ContextCompat.getDrawable(view.context, R.drawable.bg_dot)?.mutate()
        dot?.setTintList(ColorStateList.valueOf(ContextCompat.getColor(view.context, category.colorRes)))
        view.setCompoundDrawablesRelativeWithIntrinsicBounds(dot, null, null, null)
        view.compoundDrawablePadding = (view.resources.displayMetrics.density * 4).toInt()
    }

    fun cycleWithReminder(context: Context, subscription: SubscriptionPreview): String {
        return context.getString(
            R.string.cycle_with_reminder_format,
            cycle(context, subscription),
            DueFormatter.reminder(subscription.reminderDaysBefore)
        )
    }
}
